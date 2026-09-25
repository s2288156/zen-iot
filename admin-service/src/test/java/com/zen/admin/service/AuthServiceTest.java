package com.zen.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link AuthService} 的纯单元测试：全部协作用 Mockito mock，不启动 Spring 上下文，不依赖任何中间件。
 *
 * <p>覆盖安全敏感行为：登录账号不泄露、refresh 轮转 + 旧 Token 黑名单重放拒绝、登出吊销 access 与 refresh，
 * 以及登录锁定口径（锁定期 429 不计数、达阈值那次记 BAD_CREDENTIALS 但回 429）与审计日志的 best-effort 语义。
 * Redis fail-open 不在本类断言——收敛在 store 内部，见 {@link RedisLoginAttemptStoreTest}。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenIssuer tokenIssuer;

    @Mock
    private JwtTokenVerifier tokenVerifier;

    @Mock
    private TokenRevocationChecker revocationChecker;

    @Mock
    private LoginAttemptStore attemptStore;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private AuthService authService;

    // ---------- login ----------

    @Test
    void loginSuccessReturnsTokenPairWithPrincipalFromUser() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd123", "$2a$10$hash")).thenReturn(true);
        TokenPair expected = new TokenPair("access-xyz", "refresh-xyz");
        when(tokenIssuer.issue(any(TokenPrincipal.class))).thenReturn(expected);

        TokenPair result = authService.login(new LoginRequest("admin", "pwd123"), "203.0.113.7", "junit-ua");

        assertThat(result).isSameAs(expected);
        // principal 的 roles 按 roleCode 排序，modules 去重后排序
        verify(tokenIssuer)
                .issue(argThat(p -> p instanceof TokenPrincipal tp
                        && tp.userId() == 1L
                        && tp.username().equals("admin")
                        && tp.roles().equals(List.of("admin"))
                        && tp.modules().equals(List.of("ADMIN"))));
        // 成功登录必须清零失败计数
        verify(attemptStore).reset("admin");

        LoginLogEntity logEntry = capturedLoginLog();
        assertThat(logEntry.getUsername()).isEqualTo("admin");
        assertThat(logEntry.getSuccess()).isEqualTo(LoginLogEntity.RESULT_SUCCESS);
        assertThat(logEntry.getReason()).isEqualTo(AuthService.REASON_SUCCESS);
        assertThat(logEntry.getIp()).isEqualTo("203.0.113.7");
        assertThat(logEntry.getUserAgent()).isEqualTo("junit-ua");
        assertThat(logEntry.getLoginTime()).isNotNull();

        // 登录成功要拿刚签发的 Token 对建立会话登记（G5-2）
        verify(sessionService).recordLogin(1L, "admin", "203.0.113.7", "junit-ua", expected);
    }

    @Test
    void overlongUserAgentIsTruncatedBeforePersist() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd123", "$2a$10$hash")).thenReturn(true);
        when(tokenIssuer.issue(any(TokenPrincipal.class))).thenReturn(new TokenPair("a", "r"));

        authService.login(new LoginRequest("admin", "pwd123"), "203.0.113.7", "u".repeat(300));

        // 截断在 service 落库前完成，长度恰好等于列宽
        assertThat(capturedLoginLog().getUserAgent()).hasSize(AuthService.USER_AGENT_MAX_LENGTH);
    }

    @Test
    void userNotFoundThrowsSameErrorAsBadPasswordAndCountsFailure() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "anything"), "203.0.113.7", "junit-ua"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED);
                    assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
                });
        // 密码校验不应被调用——用户不存在就直接拒绝；但失败照常计数（防用户名枚举的探测计入锁定）
        verify(passwordEncoder, never()).matches(any(), any());
        verify(attemptStore).recordFailure("ghost");

        LoginLogEntity logEntry = capturedLoginLog();
        assertThat(logEntry.getSuccess()).isEqualTo(LoginLogEntity.RESULT_FAILURE);
        assertThat(logEntry.getReason()).isEqualTo(AuthService.REASON_BAD_CREDENTIALS);
    }

    @Test
    void wrongPasswordThrowsUnauthorizedAndCountsFailure() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong"), "203.0.113.7", "junit-ua"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED);
                    assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
                });
        verify(attemptStore).recordFailure("admin");

        LoginLogEntity logEntry = capturedLoginLog();
        assertThat(logEntry.getSuccess()).isEqualTo(LoginLogEntity.RESULT_FAILURE);
        assertThat(logEntry.getReason()).isEqualTo(AuthService.REASON_BAD_CREDENTIALS);
    }

    @Test
    void failureAtThresholdReturnsTooManyRequestsButLogsBadCredentials() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);
        // 这次失败恰好触发锁定
        when(attemptStore.recordFailure("admin")).thenReturn(true);

        // 钉死口径：达阈值那次凭据确实错了，日志记 BAD_CREDENTIALS；响应却是 429（锁定已生效）
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong"), "203.0.113.7", "junit-ua"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.TOO_MANY_REQUESTS);
                    assertThat(ex.getMessage()).isEqualTo("账号已锁定,请稍后重试");
                });

        LoginLogEntity logEntry = capturedLoginLog();
        assertThat(logEntry.getSuccess()).isEqualTo(LoginLogEntity.RESULT_FAILURE);
        assertThat(logEntry.getReason()).isEqualTo(AuthService.REASON_BAD_CREDENTIALS);
    }

    @Test
    void lockedAttemptThrowsTooManyRequestsWithoutTouchingCredentialsOrCounter() {
        when(attemptStore.isLocked("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "pwd123"), "203.0.113.7", "junit-ua"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.TOO_MANY_REQUESTS);
                    assertThat(ex.getMessage()).isEqualTo("账号已锁定,请稍后重试");
                });
        // 锁定期内不查库、不校验口令、不累计失败——否则攻击者可借锁定续期或探测口令
        verify(userRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(attemptStore, never()).recordFailure(any());

        LoginLogEntity logEntry = capturedLoginLog();
        assertThat(logEntry.getReason()).isEqualTo(AuthService.REASON_LOCKED);
    }

    @Test
    void disabledAccountThrowsForbiddenAndDoesNotCountFailure() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 0, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "$2a$10$hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "pwd"), "203.0.113.7", "junit-ua"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo("账号已停用");
                });
        // 口令是对的，停用不是凭据失败——不应计入锁定计数
        verify(attemptStore, never()).recordFailure(any());

        LoginLogEntity logEntry = capturedLoginLog();
        assertThat(logEntry.getReason()).isEqualTo(AuthService.REASON_DISABLED);
    }

    @Test
    void loginLogWriteFailureDoesNotBlockLogin() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd123", "$2a$10$hash")).thenReturn(true);
        TokenPair expected = new TokenPair("access-xyz", "refresh-xyz");
        when(tokenIssuer.issue(any(TokenPrincipal.class))).thenReturn(expected);
        // 审计日志 best-effort：落库炸了也只记 error，不影响认证结果
        when(loginLogRepository.save(any(LoginLogEntity.class)))
                .thenThrow(new DataIntegrityViolationException("login log down"));

        TokenPair result = authService.login(new LoginRequest("admin", "pwd123"), "203.0.113.7", "junit-ua");

        assertThat(result).isSameAs(expected);
    }

    // ---------- refresh ----------

    @Test
    void refreshIssuesNewPairAndRevokesOldToken() {
        Instant expiresAt = Instant.now().plus(Duration.ofDays(1));
        VerifiedToken refreshToken = new VerifiedToken(
                "jti-old", 1L, "admin", List.of("admin"), List.of("ADMIN"), TokenType.REFRESH, expiresAt);
        when(tokenVerifier.verify("old-refresh", TokenType.REFRESH)).thenReturn(refreshToken);
        when(revocationChecker.isRevoked("jti-old")).thenReturn(false);
        TokenPair expected = new TokenPair("access-new", "refresh-new");
        when(tokenIssuer.issue(any(TokenPrincipal.class))).thenReturn(expected);

        TokenPair result = authService.refresh("old-refresh");

        assertThat(result).isSameAs(expected);
        // 旧 refresh Token 的 jti 应被吊销，TTL 取自其剩余有效期
        verify(revocationChecker).revoke(eq("jti-old"), any(Duration.class));
        // 轮转后要迁移会话登记：新 jti 接棒，否则踢下线会被旧谱系复活
        verify(sessionService).rotate("jti-old", expected);
    }

    @Test
    void refreshReplayOfRevokedTokenThrowsUnauthorized() {
        Instant expiresAt = Instant.now().plus(Duration.ofDays(1));
        VerifiedToken refreshToken = new VerifiedToken(
                "jti-revoked", 1L, "admin", List.of("admin"), List.of("ADMIN"), TokenType.REFRESH, expiresAt);
        when(tokenVerifier.verify("revoked-refresh", TokenType.REFRESH)).thenReturn(refreshToken);
        when(revocationChecker.isRevoked("jti-revoked")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh("revoked-refresh"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED));
        // 已吊销的 Token 不应再签发新对
        verify(tokenIssuer, never()).issue(any());
    }

    // ---------- logout ----------

    @Test
    void logoutRevokesAccessAndRefreshTokens() {
        Instant accessExpiry = Instant.now().plus(Duration.ofMinutes(10));
        Instant refreshExpiry = Instant.now().plus(Duration.ofDays(1));
        UserPrincipal principal =
                new UserPrincipal(1L, "admin", List.of("admin"), List.of("ADMIN"), "jti-access", accessExpiry);
        UserContext.set(principal);

        VerifiedToken refreshToken = new VerifiedToken(
                "jti-refresh", 1L, "admin", List.of("admin"), List.of("ADMIN"), TokenType.REFRESH, refreshExpiry);
        when(tokenVerifier.verify("refresh-token", TokenType.REFRESH)).thenReturn(refreshToken);

        authService.logout("refresh-token");

        // access Token 的 jti 来自 UserContext，refresh Token 的 jti 来自参数
        verify(revocationChecker).revoke(eq("jti-access"), any(Duration.class));
        verify(revocationChecker).revoke(eq("jti-refresh"), any(Duration.class));
        // 双 jti 拉黑后还要删会话登记，否则列表里挂着一条永远踢不掉的死行
        verify(sessionService).removeOnLogout("jti-refresh");
    }

    @Test
    void logoutWithoutUserContextStillRevokesRefreshToken() {
        UserContext.clear(); // 无登录上下文（如网关透传模式）

        Instant refreshExpiry = Instant.now().plus(Duration.ofDays(1));
        VerifiedToken refreshToken = new VerifiedToken(
                "jti-refresh", 1L, "admin", List.of("admin"), List.of("ADMIN"), TokenType.REFRESH, refreshExpiry);
        when(tokenVerifier.verify("refresh-token", TokenType.REFRESH)).thenReturn(refreshToken);

        authService.logout("refresh-token");

        // 只有 refresh Token 被吊销
        verify(revocationChecker).revoke(eq("jti-refresh"), any(Duration.class));
        verify(revocationChecker, never()).revoke(eq("jti-access"), any(Duration.class));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ---------- me ----------

    @Test
    void meReturnsLatestProfileWithSortedRoleIds() {
        UserEntity user = profileUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserContext.set(principal());

        UserProfileView view = authService.me();

        assertThat(view.id()).isEqualTo(1L);
        assertThat(view.username()).isEqualTo("admin");
        assertThat(view.nickname()).isEqualTo("平台管理员");
        assertThat(view.email()).isEqualTo("admin@zen.local");
        assertThat(view.phone()).isEqualTo("13800000000");
        assertThat(view.avatar()).isEqualTo("https://cdn/avatar.png");
        assertThat(view.status()).isEqualTo(1);
        // 两个角色乱序挂上，视图里必须升序
        assertThat(view.roleIds()).containsExactly(2L, 9L);
    }

    @Test
    void meWithoutUserContextThrowsUnauthorizedWithoutHittingRepository() {
        UserContext.clear(); // 身份缺失只可能出现在拦截器之外的调用路径

        assertThatThrownBy(() -> authService.me())
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void meForSoftDeletedUserThrowsUnauthorized() {
        // @SQLRestriction 让已删用户的 findById 返回 empty：等价于"登录已失效"，不透露账号是否存在
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        UserContext.set(principal());

        assertThatThrownBy(() -> authService.me()).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED);
            assertThat(ex.getMessage()).isEqualTo("未认证或登录已失效");
        });
    }

    // ---------- change-password ----------

    @Test
    void changePasswordWithCorrectOldPasswordStoresNewHash() {
        UserEntity user = profileUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pwd", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("$2a$10$newhash");
        UserContext.set(principal());

        authService.changePassword(new ChangePasswordRequest("old-pwd", "new-password"));

        // 脏检查落库前，实体上的口令必须已被替换为新 hash
        assertThat(user.getPassword()).isEqualTo("$2a$10$newhash");
        // 连带吊销除当前会话（本次请求的 access jti）外的全部会话（G5-2）
        verify(sessionService).revokeAllForUserExcept(1L, "jti-access");
    }

    @Test
    void changePasswordRevokeFailurePropagatesAndRollsBack() {
        UserEntity user = profileUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pwd", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("$2a$10$newhash");
        UserContext.set(principal());
        // 决策 (c)：连带吊销失败上抛回滚改密事务——「吊销不了就不改密」
        doThrow(new IllegalStateException("redis down")).when(sessionService).revokeAllForUserExcept(1L, "jti-access");

        assertThatThrownBy(() -> authService.changePassword(new ChangePasswordRequest("old-pwd", "new-password")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis down");
    }

    @Test
    void changePasswordWithWrongOldPasswordThrowsBadRequestAndKeepsHash() {
        UserEntity user = profileUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-old", "$2a$10$hash")).thenReturn(false);
        UserContext.set(principal());

        assertThatThrownBy(() -> authService.changePassword(new ChangePasswordRequest("wrong-old", "new-password")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("旧口令不正确");
                });
        verify(passwordEncoder, never()).encode(any());
        assertThat(user.getPassword()).isEqualTo("$2a$10$hash");
    }

    // ---------- fixtures ----------

    /** 捕获唯一一次落库的登录日志实体。 */
    private LoginLogEntity capturedLoginLog() {
        ArgumentCaptor<LoginLogEntity> captor = ArgumentCaptor.forClass(LoginLogEntity.class);
        verify(loginLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static UserPrincipal principal() {
        return new UserPrincipal(
                1L,
                "admin",
                List.of("admin"),
                List.of("ADMIN"),
                "jti-access",
                Instant.now().plus(Duration.ofMinutes(10)));
    }

    /** 带完整资料与两个乱序角色（id 9、2）的用户，口令 hash 与 {@link #principal()} 的 userId 对齐。 */
    private static UserEntity profileUser() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        user.setNickname("平台管理员");
        user.setEmail("admin@zen.local");
        user.setPhone("13800000000");
        user.setAvatar("https://cdn/avatar.png");
        RoleEntity operatorRole = new RoleEntity();
        operatorRole.setId(2L);
        operatorRole.setRoleCode("operator");
        user.getRoles().add(operatorRole);
        user.getRoles().iterator().next().setId(9L);
        return user;
    }

    private static UserEntity userEntity(
            long id, String username, String password, byte status, String roleCode, String... modules) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setStatus(status);
        RoleEntity role = new RoleEntity();
        role.setRoleCode(roleCode);
        role.setModules(new LinkedHashSet<>(Set.of(modules)));
        user.setRoles(new LinkedHashSet<>(Set.of(role)));
        return user;
    }
}
