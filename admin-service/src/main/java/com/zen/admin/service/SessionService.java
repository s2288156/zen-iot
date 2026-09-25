package com.zen.admin.service;

import com.zen.admin.dto.SessionView;
import com.zen.common.core.page.PageQuery;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.auth.TokenRevocationChecker;
import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import com.zen.common.security.jwt.JwtTokenVerifier;
import com.zen.common.security.jwt.TokenPair;
import com.zen.common.security.jwt.TokenType;
import com.zen.common.security.jwt.VerifiedToken;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 在线会话簿记与吊销（G5-2）。会话 = refresh 令牌谱系：只拉黑 access jti 会被 {@code /auth/refresh} 复活，
 * 因此强制下线与连带吊销都必须成对拉黑 {@code currentRefreshJti + lastAccessJti} 再删登记。
 *
 * <p>异常处理按决策 (c) 分两界——
 * <b>簿记方法</b>（登录登记/轮转迁移/登出清理）catch 全部运行时异常只记 WARN 不外抛：簿记缺失不该反噬
 * 认证主流程（对齐 {@code RedisLoginAttemptStore} 的 fail-open 先例；这三个方法只在认证成功之后执行，
 * catch RuntimeException 不会吞掉任何业务异常）。
 * <b>吊销方法</b>（强制下线/连带吊销）一律不 catch、失败上抛：静默吞掉意味着旧谱系在改密事务提交后
 * 仍能续期，等于安全承诺落空；上抛则回滚外层 {@code @Transactional}，「吊销不了就不改密」，状态自洽。
 *
 * <p>按用户检索扫 ZSET 活成员逐条过滤，不建第三索引：在线会话是 admin 后台量级，多一层键就多一处
 * 与主登记的一致性负担。
 */
@Slf4j
@Service
public class SessionService {

    private final SessionRegistry registry;
    private final TokenRevocationChecker revocationChecker;
    private final JwtTokenVerifier tokenVerifier;

    public SessionService(
            SessionRegistry registry, TokenRevocationChecker revocationChecker, JwtTokenVerifier tokenVerifier) {
        this.registry = registry;
        this.revocationChecker = revocationChecker;
        this.tokenVerifier = tokenVerifier;
    }

    /** 登录成功后建立会话登记。刚签发的 Token 用 {@link JwtTokenVerifier} 回取 jti/过期时刻，common-security 不改。 */
    public void recordLogin(
            long userId, String username, @Nullable String ip, @Nullable String userAgent, TokenPair issued) {
        try {
            VerifiedToken access = tokenVerifier.verify(issued.accessToken(), TokenType.ACCESS);
            VerifiedToken refresh = tokenVerifier.verify(issued.refreshToken(), TokenType.REFRESH);
            registry.save(new SessionInfo(
                    UUID.randomUUID().toString(),
                    userId,
                    username,
                    ip,
                    userAgent,
                    Instant.now(),
                    refresh.expiresAt(),
                    refresh.jti(),
                    access.jti()));
        } catch (RuntimeException e) {
            log.warn("会话登记失败，不影响本次登录: username={}", username, e);
        }
    }

    /**
     * refresh 轮转后迁移登记（决策 (b)：登记键 TTL、ZSET score、expireTime 三者随新 refresh 有效期一并迁移；
     * issueTime 保持首次登录时刻）。经旧 jti 的反向映射定位；映射缺失即旧版会话或登记已丢，跳过。
     */
    public void rotate(String oldRefreshJti, TokenPair issued) {
        try {
            Optional<String> sessionId = registry.findSessionIdByRefreshJti(oldRefreshJti);
            if (sessionId.isEmpty()) {
                return;
            }
            Optional<SessionInfo> existing = registry.find(sessionId.get());
            if (existing.isEmpty()) {
                // 登记键先到期而映射残留：顺手解除，避免悬挂
                registry.unbindRefresh(oldRefreshJti);
                return;
            }
            VerifiedToken access = tokenVerifier.verify(issued.accessToken(), TokenType.ACCESS);
            VerifiedToken refresh = tokenVerifier.verify(issued.refreshToken(), TokenType.REFRESH);
            SessionInfo old = existing.get();
            registry.save(new SessionInfo(
                    old.sessionId(),
                    old.userId(),
                    old.username(),
                    old.ip(),
                    old.userAgent(),
                    old.issueTime(),
                    refresh.expiresAt(),
                    refresh.jti(),
                    access.jti()));
            registry.unbindRefresh(oldRefreshJti);
        } catch (RuntimeException e) {
            log.warn("会话轮转迁移失败，不影响本次换新: jti={}", oldRefreshJti, e);
        }
    }

    /** logout 已把双 jti 拉黑（AuthService 原逻辑），这里只删登记与索引；失败靠 TTL 自然过期兜底。 */
    public void removeOnLogout(String refreshJti) {
        try {
            registry.findSessionIdByRefreshJti(refreshJti)
                    .flatMap(registry::find)
                    .ifPresent(registry::remove);
            registry.unbindRefresh(refreshJti);
        } catch (RuntimeException e) {
            log.warn("会话登记清理失败，不影响本次登出: jti={}", refreshJti, e);
        }
    }

    /**
     * 强制下线：目标会话属主是调用人自己 → 400（防自锁，G5-2）；不存在或已下线 → 404。
     * 失败上抛（fail-closed），显式接口调用方得到 500 而非「以为踢掉了」。
     */
    public void kickout(String sessionId, long callerUserId) {
        SessionInfo session = registry.find(sessionId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "会话不存在或已下线: " + sessionId));
        if (session.userId() == callerUserId) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不能强制下线自己的会话");
        }
        revokeSession(session);
    }

    /** 连带吊销（管理员重置口令）：该用户全部会话。在调用方事务内执行，失败上抛回滚改密。 */
    public void revokeAllForUser(long userId) {
        for (SessionInfo session : registry.listAllActive()) {
            if (session.userId() == userId) {
                revokeSession(session);
            }
        }
    }

    /** 连带吊销（自助改密）：该用户除当前会话（{@code lastAccessJti} 匹配本次请求的 access jti）外全部。 */
    public void revokeAllForUserExcept(long userId, @Nullable String currentAccessJti) {
        for (SessionInfo session : registry.listAllActive()) {
            if (session.userId() == userId && !session.lastAccessJti().equals(currentAccessJti)) {
                revokeSession(session);
            }
        }
    }

    /** 在线会话分页（ZSET 过期时刻升序）：排序字段不开放，jti 不外泄。 */
    public PageResult<SessionView> page(PageQuery query) {
        long total = registry.countActive();
        long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
        List<SessionView> views = registry.listActive(offset, query.getPageSize()).stream()
                .map(SessionService::toView)
                .toList();
        PageRequest page = PageRequest.of(query.getPageNum() - 1, query.getPageSize());
        return PageResult.of(new PageImpl<>(views, page, total));
    }

    /** 拉黑谱系双 jti 并删登记。access 的拉黑 TTL 以会话 expireTime 为上界（决策 (b)：不存 access 自身 exp）。 */
    private void revokeSession(SessionInfo session) {
        Duration ttl = Duration.between(Instant.now(), session.expireTime());
        revocationChecker.revoke(session.currentRefreshJti(), ttl.isNegative() ? Duration.ZERO : ttl);
        revocationChecker.revoke(session.lastAccessJti(), ttl.isNegative() ? Duration.ZERO : ttl);
        registry.remove(session);
    }

    private static SessionView toView(SessionInfo session) {
        return new SessionView(
                session.sessionId(),
                session.userId(),
                session.username(),
                session.ip(),
                session.userAgent(),
                session.issueTime(),
                session.expireTime());
    }
}
