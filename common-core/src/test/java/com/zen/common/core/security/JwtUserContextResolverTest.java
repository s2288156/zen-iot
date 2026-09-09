package com.zen.common.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.jwt.JwtProperties;
import com.zen.common.core.jwt.JwtTokenIssuer;
import com.zen.common.core.jwt.JwtTokenVerifier;
import com.zen.common.core.jwt.TokenPair;
import com.zen.common.core.jwt.TokenPrincipal;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtUserContextResolverTest {

  private static final String SECRET = "zen-iot-phase-1-hs256-secret-key-32-bytes!";

  private final Set<String> revoked = new HashSet<>();
  private JwtTokenIssuer issuer;
  private JwtUserContextResolver resolver;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret(SECRET);
    issuer = new JwtTokenIssuer(properties);
    resolver = new JwtUserContextResolver(new JwtTokenVerifier(properties), new InMemoryChecker());
  }

  @Test
  void resolvesPrincipalFromBearerToken() {
    TokenPair pair = issuePair();

    UserPrincipal principal = resolver.resolve(bearer(pair.accessToken()));

    assertThat(principal)
        .extracting(
            UserPrincipal::userId,
            UserPrincipal::username,
            UserPrincipal::roles,
            UserPrincipal::modules)
        .containsExactly(1L, "admin", List.of("admin"), List.of("ADMIN"));
    assertThat(principal.jti()).isNotBlank();
    assertThat(principal.remainingTtl()).isPositive();
  }

  @Test
  void missingOrBlankBearerHeaderMeansUnauthenticated() {
    assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
    assertThat(resolver.resolve(bearer("   "))).isNull();
  }

  @Test
  void forgedIdentityHeadersAreIgnoredInJwtMode() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(GatewayHeaderUserContextResolver.USER_ID_HEADER, "1");
    request.addHeader(GatewayHeaderUserContextResolver.MODULES_HEADER, "ADMIN");

    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  void refreshTokenCannotBeUsedAsAccessToken() {
    TokenPair pair = issuePair();

    assertThatThrownBy(() -> resolver.resolve(bearer(pair.refreshToken())))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void revokedTokenIsRejected() {
    TokenPair pair = issuePair();
    revoked.add(resolver.resolve(bearer(pair.accessToken())).jti());

    assertThatThrownBy(() -> resolver.resolve(bearer(pair.accessToken())))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void tamperedTokenIsRejected() {
    TokenPair pair = issuePair();

    assertThatThrownBy(() -> resolver.resolve(bearer(pair.accessToken() + "tampered")))
        .isInstanceOf(BusinessException.class);
  }

  private TokenPair issuePair() {
    return issuer.issue(new TokenPrincipal(1L, "admin", List.of("admin"), List.of("ADMIN")));
  }

  private MockHttpServletRequest bearer(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }

  private final class InMemoryChecker implements TokenRevocationChecker {

    @Override
    public void revoke(String jti, Duration ttl) {
      revoked.add(jti);
    }

    @Override
    public boolean isRevoked(String jti) {
      return revoked.contains(jti);
    }
  }
}
