package com.zen.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zen.admin.dto.LoginRequest;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.entity.UserEntity;
import com.zen.admin.repository.UserRepository;
import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import com.zen.common.core.jwt.JwtTokenIssuer;
import com.zen.common.core.jwt.JwtTokenVerifier;
import com.zen.common.core.jwt.TokenPair;
import com.zen.common.core.jwt.TokenPrincipal;
import com.zen.common.core.jwt.TokenType;
import com.zen.common.core.jwt.VerifiedToken;
import com.zen.common.core.security.TokenRevocationChecker;
import com.zen.common.core.security.UserContext;
import com.zen.common.core.security.UserPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link AuthService} 的纯单元测试：全部协作用 Mockito mock，不启动 Spring 上下文，不依赖任何中间件。
 *
 * <p>覆盖安全敏感行为：登录账号不泄露、refresh 轮转 + 旧 Token 黑名单重放拒绝、登出吊销 access 与 refresh。
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

        TokenPair result = authService.login(new LoginRequest("admin", "pwd123"));

        assertThat(result).isSameAs(expected);
        // principal 的 roles 按 roleCode 排序，modules 去重后排序
        verify(tokenIssuer)
                .issue(argThat(p -> p instanceof TokenPrincipal tp
                        && tp.userId() == 1L
                        && tp.username().equals("admin")
                        && tp.roles().equals(List.of("admin"))
                        && tp.modules().equals(List.of("ADMIN"))));
    }

    @Test
    void userNotFoundThrowsSameErrorAsBadPassword() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "anything")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED);
                    assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
                });
        // 密码校验不应被调用——用户不存在就直接拒绝
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void wrongPasswordThrowsUnauthorized() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 1, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.UNAUTHORIZED);
                    assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
                });
    }

    @Test
    void disabledAccountThrowsForbidden() {
        UserEntity user = userEntity(1L, "admin", "$2a$10$hash", (byte) 0, "admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "$2a$10$hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "pwd")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(GlobalErrorCode.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo("账号已停用");
                });
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

    // ---------- fixtures ----------

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
