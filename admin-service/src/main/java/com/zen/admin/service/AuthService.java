package com.zen.admin.service;

import com.zen.admin.dto.ChangePasswordRequest;
import com.zen.admin.dto.LoginRequest;
import com.zen.admin.dto.UserProfileView;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.entity.UserEntity;
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
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录、登出。授权判定不在这里,由 {@code com.zen.common.security.auth.RequireModule} 拦截器按 Token 里的模块快照执行。 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer tokenIssuer;
    private final JwtTokenVerifier tokenVerifier;
    private final TokenRevocationChecker revocationChecker;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenIssuer tokenIssuer,
            JwtTokenVerifier tokenVerifier,
            TokenRevocationChecker revocationChecker) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.tokenVerifier = tokenVerifier;
        this.revocationChecker = revocationChecker;
    }

    /** 用户不存在与密码错误返回同一个 401,不透露账号是否存在。 */
    public TokenPair login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username()).orElseThrow(AuthService::badCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw badCredentials();
        }
        if (!user.isEnabled()) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "账号已停用");
        }
        return tokenIssuer.issue(principalOf(user));
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
     * <p>改密成功<b>不</b>吊销既有会话与 Token——撤销基建在 Phase 5,已知取舍是旧 Token 仍有效至自然过期。
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserEntity user = requireCurrentUser();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "旧口令不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
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
}
