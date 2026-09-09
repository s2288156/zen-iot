package com.zen.common.core.security;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import com.zen.common.core.jwt.JwtTokenVerifier;
import com.zen.common.core.jwt.TokenType;
import com.zen.common.core.jwt.VerifiedToken;
import jakarta.servlet.http.HttpServletRequest;

/** 自解析 {@code Authorization: Bearer} 的 JWT 模式:服务持有密钥,不信任任何身份请求头。 */
public class JwtUserContextResolver implements UserContextResolver {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenVerifier verifier;
  private final TokenRevocationChecker revocationChecker;

  public JwtUserContextResolver(
      JwtTokenVerifier verifier, TokenRevocationChecker revocationChecker) {
    this.verifier = verifier;
    this.revocationChecker = revocationChecker;
  }

  @Override
  public UserPrincipal resolve(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      return null;
    }
    String token = header.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      return null;
    }
    VerifiedToken verified = verifier.verify(token, TokenType.ACCESS);
    if (revocationChecker.isRevoked(verified.jti())) {
      throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
    }
    return new UserPrincipal(
        verified.userId(),
        verified.username(),
        verified.roles(),
        verified.modules(),
        verified.jti(),
        verified.expiresAt());
  }
}
