package com.zen.common.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.common.core.security.AuthInterceptor;
import com.zen.common.core.security.GatewayHeaderUserContextResolver;
import com.zen.common.core.security.JwtUserContextResolver;
import com.zen.common.core.security.ModuleAuthInterceptor;
import com.zen.common.core.security.TokenRevocationChecker;
import com.zen.common.core.security.UserContextResolver;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class ZenSecurityAutoConfigurationTest {

    private static final String SECRET = "zen-iot-phase-1-hs256-secret-key-32-bytes!";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(ZenJwtAutoConfiguration.class, ZenSecurityAutoConfiguration.class));

    @Test
    void jwtSourceIsTheDefaultAndNeedsASecret() {
        runner.withPropertyValues("zen.jwt.secret=" + SECRET).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AuthInterceptor.class);
            assertThat(context).hasSingleBean(ModuleAuthInterceptor.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
            assertThat(context.getBean(UserContextResolver.class)).isInstanceOf(JwtUserContextResolver.class);
        });
    }

    @Test
    void gatewayHeaderSourceStartsWithoutAnySecret() {
        runner.withPropertyValues("zen.security.context-source=gateway-header").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(UserContextResolver.class)).isInstanceOf(GatewayHeaderUserContextResolver.class);
        });
    }

    @Test
    void jwtSourceWithoutSecretFailsAtStartupWithActionableMessage() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("zen.jwt.secret")
                    .hasMessageContaining("gateway-header");
        });
    }

    @Test
    void disablingSecurityRemovesTheInterceptor() {
        runner.withPropertyValues("zen.security.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AuthInterceptor.class);
        });
    }

    @Test
    void serviceProvidedRevocationCheckerOverridesTheDisabledFallback() {
        runner.withPropertyValues("zen.jwt.secret=" + SECRET)
                .withUserConfiguration(RevocationCheckerConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TokenRevocationChecker.class);
                    assertThat(context.getBean(TokenRevocationChecker.class)).isInstanceOf(RecordingChecker.class);
                });
    }

    @Test
    void fallbackRevocationCheckerNeverRejects() {
        runner.withPropertyValues("zen.jwt.secret=" + SECRET).run(context -> {
            TokenRevocationChecker checker = context.getBean(TokenRevocationChecker.class);
            checker.revoke("jti", Duration.ofMinutes(5));
            assertThat(checker.isRevoked("jti")).isFalse();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RevocationCheckerConfig {

        @Bean
        TokenRevocationChecker redisLikeChecker() {
            return new RecordingChecker();
        }
    }

    static class RecordingChecker implements TokenRevocationChecker {

        @Override
        public void revoke(String jti, Duration ttl) {}

        @Override
        public boolean isRevoked(String jti) {
            return true;
        }
    }
}
