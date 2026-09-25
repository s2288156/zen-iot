package com.zen.admin.service;

import com.zen.admin.dto.ChangePasswordRequest;
import com.zen.admin.dto.LoginRequest;
import com.zen.admin.dto.UserProfileView;
import com.zen.admin.entity.LoginLogEntity;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.entity.UserEntity;
import com.zen.admin.repository.LoginLogRepository;
import com.zen.admin.repository.UserRepository;
import com.zen.common.security.auth.TokenRevocationChecker;
import com.zen.common.security.auth.UserContext;
import com.zen.common.security.auth.UserPrincipal;
import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import com.zen.common.security.jwt.JwtTokenIssuer;
import com.zen.common.security.jwt.JwtTokenVerifier;
import com.zen.common.security.jwt.TokenPair;
import com.zen.common.security.jwt.TokenPrincipal;
import com.zen.common.security.jwt.TokenType;
import com.zen.common.security.jwt.VerifiedToken;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录（含失败锁定与审计日志）、登出。授权判定不在这里,由 {@code com.zen.common.security.auth.RequireModule} 拦截器按 Token 里的模块快照执行。 */
@Slf4j
@Service
public class AuthService {

    /** 登录日志 reason 词表（对外契约,V4 建表注释同步）。 */
    static final String REASON_SUCCESS = "SUCCESS";

    static final String REASON_BAD_CREDENTIALS = "BAD_CREDENTIALS";
    static final String REASON_LOCKED = "LOCKED";
    static final String REASON_DISABLED = "DISABLED";

    /** 与 t_login_log.user_agent 的 VARCHAR(256) 耦合：超长 UA 在落库前截断,避免整条日志因宽度写不进。 */
    static final int USER_AGENT_MAX_LENGTH = 256;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer tokenIssuer;
    private final JwtTokenVerifier tokenVerifier;
    private final TokenRevocationChecker revocationChecker;
    private final LoginAttemptStore attemptStore;
    private final LoginLogRepository loginLogRepository;
    private final SessionService sessionService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenIssuer tokenIssuer,
            JwtTokenVerifier tokenVerifier,
            TokenRevocationChecker revocationChecker,
            LoginAttemptStore attemptStore,
            LoginLogRepository loginLogRepository,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.tokenVerifier = tokenVerifier;
        this.revocationChecker = revocationChecker;
        this.attemptStore = attemptStore;
        this.loginLogRepository = loginLogRepository;
        this.sessionService = sessionService;
    }

    /**
     * 登录：先判锁定,再验凭据,成功清计数。用户不存在与密码错误返回同一个 401,不透露账号是否存在。
     *
     * <p>锁定口径：锁定期内的尝试一律 429、不计数、不续期；达到阈值的那一次凭据失败按 BAD_CREDENTIALS
     * 记日志、但响应给 429（G4-口径）——锁定生效的事实由响应表达,日志忠实记录该次尝试本身。
     */
    public TokenPair login(LoginRequest request, String ip, String userAgent) {
        String username = request.username();
        if (attemptStore.isLocked(username)) {
            writeLoginLog(username, false, REASON_LOCKED, ip, userAgent);
            throw new BusinessException(GlobalErrorCode.TOO_MANY_REQUESTS, "账号已锁定,请稍后重试");
        }
        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            boolean lockTriggered = attemptStore.recordFailure(username);
            writeLoginLog(username, false, REASON_BAD_CREDENTIALS, ip, userAgent);
            if (lockTriggered) {
                throw new BusinessException(GlobalErrorCode.TOO_MANY_REQUESTS, "账号已锁定,请稍后重试");
            }
            throw badCredentials();
        }
        if (!user.isEnabled()) {
            writeLoginLog(username, false, REASON_DISABLED, ip, userAgent);
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "账号已停用");
        }
        attemptStore.reset(username);
        writeLoginLog(username, true, REASON_SUCCESS, ip, userAgent);
        TokenPair issued = tokenIssuer.issue(principalOf(user));
        sessionService.recordLogin(
                user.getId(), user.getUsername(), ip, truncate(userAgent, USER_AGENT_MAX_LENGTH), issued);
        return issued;
    }

    /**
     * 换新并轮转:旧 refresh Token 立即进黑名单,重放即 401。
     *
     * <p>{@link JwtTokenVerifier} 只校验签名/过期/{@code typ},不查黑名单,所以这里必须自己查——否则被登出的 refresh Token 仍能换新。
     */
    public TokenPair refresh(String refreshToken) {
        VerifiedToken refresh = tokenVerifier.verify(refreshToken, TokenType.REFRESH);
        if (revocationChecker.isRevoked(refresh.jti())) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        TokenPair issued = tokenIssuer.issue(
                new TokenPrincipal(refresh.userId(), refresh.username(), refresh.roles(), refresh.modules()));
        revoke(refresh.jti(), refresh.remainingTtl());
        sessionService.rotate(refresh.jti(), issued);
        return issued;
    }

    /** 同时撤销本次请求携带的 access Token 与入参里的 refresh Token,缺一不可,否则登出后仍能换新 Token。 */
    public void logout(String refreshToken) {
        VerifiedToken refresh = tokenVerifier.verify(refreshToken, TokenType.REFRESH);
        UserPrincipal current = UserContext.get();
        if (current != null) {
            revoke(current.jti(), current.remainingTtl());
        }
        revoke(refresh.jti(), refresh.remainingTtl());
        sessionService.removeOnLogout(refresh.jti());
    }

    /**
     * 个人中心:按 {@link UserContext} 里的身份回查库内最新资料,而不是回显 Token 快照——
     * 角色改过后 Token 里是旧值,资料接口必须反映当前状态。
     */
    @Transactional(readOnly = true)
    public UserProfileView me() {
        return toProfileView(requireCurrentUser());
    }

    /**
     * 自助改密:旧口令 BCrypt 比对,不符返回 400,不透露 stored 口令任何信息。
     *
     * <p>改密成功后连带吊销该用户除当前会话外的全部会话(G5-2):旧谱系在新口令生效前不该还能续期;
     * 当前会话按本次请求的 access jti 匹配保留,调用人不会被自己的改密踢下线。吊销失败上抛回滚改密事务
     * (决策 (c):吊销不了就不改密),不再维持 Phase 4 的「改密不吊销」取舍。
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserEntity user = requireCurrentUser();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "旧口令不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        UserPrincipal current = UserContext.get();
        sessionService.revokeAllForUserExcept(user.getId(), current == null ? null : current.jti());
    }

    /**
     * 身份缺失或用户查不到一律 401 默认文案:身份缺失只可能出现在拦截器之外的调用路径;
     * 被逻辑删的用户 {@code @SQLRestriction} 让 findById 直接返回 empty,等价于"登录已失效",也不透露账号是否存在。
     */
    private UserEntity requireCurrentUser() {
        UserPrincipal current = UserContext.get();
        if (current == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return userRepository
                .findById(current.userId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.UNAUTHORIZED));
    }

    private static UserProfileView toProfileView(UserEntity user) {
        List<Long> roleIds =
                user.getRoles().stream().map(RoleEntity::getId).sorted().toList();
        return new UserProfileView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getStatus() == null ? null : Integer.valueOf(user.getStatus()),
                roleIds);
    }

    private void revoke(String jti, Duration ttl) {
        if (jti != null && ttl != null && !ttl.isZero()) {
            revocationChecker.revoke(jti, ttl);
        }
    }

    static TokenPrincipal principalOf(UserEntity user) {
        List<String> roles =
                user.getRoles().stream().map(RoleEntity::getRoleCode).sorted().toList();
        List<String> modules = user.getRoles().stream()
                .flatMap(role -> role.getModules().stream())
                .distinct()
                .sorted()
                .toList();
        return new TokenPrincipal(user.getId(), user.getUsername(), roles, modules);
    }

    private static BusinessException badCredentials() {
        return new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }

    /** 同步 best-effort 写登录日志：库故障只记 ERROR,绝不让审计能力反过来阻断认证主流程。 */
    private void writeLoginLog(String username, boolean success, String reason, String ip, String userAgent) {
        LoginLogEntity entry = new LoginLogEntity();
        entry.setUsername(username);
        entry.setSuccess(success ? LoginLogEntity.RESULT_SUCCESS : LoginLogEntity.RESULT_FAILURE);
        entry.setReason(reason);
        entry.setIp(ip);
        entry.setUserAgent(truncate(userAgent, USER_AGENT_MAX_LENGTH));
        entry.setLoginTime(LocalDateTime.now());
        try {
            loginLogRepository.save(entry);
        } catch (DataAccessException e) {
            log.error("登录日志写入失败,不阻断登录流程: username={}, reason={}", username, reason, e);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
