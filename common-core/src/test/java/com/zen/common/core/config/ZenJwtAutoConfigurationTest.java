package com.zen.common.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.common.core.jwt.JwtProperties;
import com.zen.common.core.jwt.JwtTokenIssuer;
import com.zen.common.core.jwt.JwtTokenVerifier;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ZenJwtAutoConfigurationTest {

    private static final String SECRET = "zen-iot-phase-1-hs256-secret-key-32-bytes!";

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ZenJwtAutoConfiguration.class));

    @Test
    void registersIssuerAndVerifierWhenSecretConfigured() {
        runner.withPropertyValues("zen.jwt.secret=" + SECRET).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtProperties.class);
            assertThat(context).hasSingleBean(JwtTokenIssuer.class);
            assertThat(context).hasSingleBean(JwtTokenVerifier.class);
        });
    }

    @Test
    void shortSecretFailsStartup() {
        runner.withPropertyValues("zen.jwt.secret=only-8-bytes")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void jwtBeansAreSkippedWithoutSecret() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(JwtTokenIssuer.class);
            assertThat(context).doesNotHaveBean(JwtTokenVerifier.class);
        });
    }

    @Test
    void defaultTtlMatchesContract() {
        JwtProperties properties = new JwtProperties();

        assertThat(properties.getAccessTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.getRefreshTtl()).isEqualTo(Duration.ofDays(7));
    }
}
