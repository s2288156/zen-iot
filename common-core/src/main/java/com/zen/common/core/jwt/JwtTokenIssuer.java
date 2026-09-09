package com.zen.common.core.jwt;

import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

/** 以 HS256 签发 access / refresh 双 Token,两者各自持有独立 {@code jti}。 */
public class JwtTokenIssuer {

  private final SecretKey key;
  private final Duration accessTtl;
  private final Duration refreshTtl;

  public JwtTokenIssuer(JwtProperties properties) {
    this.key = JwtKeys.hmacKey(properties);
    this.accessTtl = properties.getAccessTtl();
    this.refreshTtl = properties.getRefreshTtl();
  }

  public TokenPair issue(TokenPrincipal principal) {
    Instant issuedAt = Instant.now();
    return new TokenPair(
        build(principal, issuedAt, TokenType.ACCESS, accessTtl),
        build(principal, issuedAt, TokenType.REFRESH, refreshTtl));
  }

  private String build(TokenPrincipal principal, Instant issuedAt, TokenType type, Duration ttl) {
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(String.valueOf(principal.userId()))
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(issuedAt.plus(ttl)))
        .claim(JwtClaimNames.USERNAME, principal.username())
        .claim(JwtClaimNames.TYPE, type.claimValue())
        .claim(JwtClaimNames.ROLES, principal.roles())
        .claim(JwtClaimNames.MODULES, principal.modules())
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }
}
