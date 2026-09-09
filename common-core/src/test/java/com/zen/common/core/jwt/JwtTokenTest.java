package com.zen.common.core.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenTest {

    private static final String SECRET = "zen-iot-phase-1-hs256-secret-key-32-bytes!";
    private static final String OTHER_SECRET = "totally-different-zen-iot-hs256-secret-key-32b!";

    private static final TokenPrincipal PRINCIPAL =
            new TokenPrincipal(42L, "admin", List.of("admin"), List.of("admin", "ecs", "wcs", "rcs"));

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(properties(SECRET, Duration.ofMinutes(30)));
    private final JwtTokenVerifier verifier = new JwtTokenVerifier(properties(SECRET, Duration.ofMinutes(30)));

    @Test
    void issuedAccessTokenRoundTripsEveryClaim() {
        TokenPair pair = issuer.issue(PRINCIPAL);

        VerifiedToken verified = verifier.verify(pair.accessToken(), TokenType.ACCESS);

        assertThat(verified.userId()).isEqualTo(42L);
        assertThat(verified.username()).isEqualTo("admin");
        assertThat(verified.roles()).containsExactly("admin");
        assertThat(verified.modules()).containsExactly("admin", "ecs", "wcs", "rcs");
        assertThat(verified.type()).isEqualTo(TokenType.ACCESS);
        assertThat(verified.jti()).isNotBlank();
        assertThat(verified.remainingTtl()).isBetween(Duration.ofSeconds(1), Duration.ofMinutes(30));
    }

    @Test
    void accessAndRefreshTokensGetIndependentJti() {
        TokenPair pair = issuer.issue(PRINCIPAL);

        VerifiedToken access = verifier.verify(pair.accessToken(), TokenType.ACCESS);
        VerifiedToken refresh = verifier.verify(pair.refreshToken(), TokenType.REFRESH);

        assertThat(access.jti()).isNotEqualTo(refresh.jti());
        assertThat(refresh.type()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void refreshTokenCannotPassAsAccessToken() {
        String refresh = issuer.issue(PRINCIPAL).refreshToken();

        assertUnauthorized(() -> verifier.verify(refresh, TokenType.ACCESS));
    }

    @Test
    void tamperedSignatureIsRejected() {
        String token = issuer.issue(PRINCIPAL).accessToken();
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertUnauthorized(() -> verifier.verify(tampered, TokenType.ACCESS));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtTokenIssuer foreignIssuer = new JwtTokenIssuer(properties(OTHER_SECRET, Duration.ofMinutes(30)));

        assertUnauthorized(() -> verifier.verify(foreignIssuer.issue(PRINCIPAL).accessToken(), TokenType.ACCESS));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenIssuer expiredIssuer = new JwtTokenIssuer(properties(SECRET, Duration.ofSeconds(-1)));

        assertUnauthorized(() -> verifier.verify(expiredIssuer.issue(PRINCIPAL).accessToken(), TokenType.ACCESS));
    }

    @Test
    void malformedTokenIsRejected() {
        assertUnauthorized(() -> verifier.verify("not-a-jwt", TokenType.ACCESS));
        assertUnauthorized(() -> verifier.verify("", TokenType.ACCESS));
    }

    private static JwtProperties properties(String secret, Duration accessTtl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setAccessTtl(accessTtl);
        return properties;
    }

    private static void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(GlobalErrorCode.UNAUTHORIZED);
    }
}
