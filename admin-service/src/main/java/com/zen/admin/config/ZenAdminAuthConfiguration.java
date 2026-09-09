package com.zen.admin.config;

import com.zen.common.core.security.UserContext;
import com.zen.common.core.security.UserPrincipal;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证所需的两个基础 Bean。
 *
 * <p>只声明 {@link PasswordEncoder} 而非引入 {@code spring-boot-starter-security}：后者会装配默认过滤器链，接管 401/403
 * 并绕过 {@code GlobalExceptionHandler}。
 */
@Configuration(proxyBeanMethods = false)
public class ZenAdminAuthConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 覆盖 common-core 的默认审计人 {@code system}；自动配置在用户 Bean 之后评估，会自动退让。 */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(UserContext.get()).map(UserPrincipal::username);
    }
}
