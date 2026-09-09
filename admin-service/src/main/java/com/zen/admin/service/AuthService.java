package com.zen.admin.service;

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
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 登录、登出。授权判定不在这里,由 {@code @RequireModule} 拦截器按 Token 里的模块快照执行。 */
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
    UserEntity user =
        userRepository.findByUsername(request.username()).orElseThrow(AuthService::badCredentials);
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
    TokenPair issued =
        tokenIssuer.issue(
            new TokenPrincipal(
                refresh.userId(), refresh.username(), refresh.roles(), refresh.modules()));
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

  private void revoke(String jti, Duration ttl) {
    if (jti != null && ttl != null && !ttl.isZero()) {
      revocationChecker.revoke(jti, ttl);
    }
  }

  static TokenPrincipal principalOf(UserEntity user) {
    List<String> roles = user.getRoles().stream().map(RoleEntity::getRoleCode).sorted().toList();
    List<String> modules =
        user.getRoles().stream()
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
