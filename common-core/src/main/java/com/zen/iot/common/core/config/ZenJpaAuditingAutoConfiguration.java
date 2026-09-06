package com.zen.iot.common.core.config;

import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计自动配置:启用 Auditing 并注册默认审计人 "system"。
 *
 * <p>业务服务(如接入 Spring Security 后)定义自己的 {@code AuditorAware<String>} Bean 即可覆盖默认实现 — 自动配置在用户 Bean
 * 之后评估,默认 Bean 会自动退让。
 */
@AutoConfiguration
@ConditionalOnClass({EnableJpaAuditing.class, AuditingEntityListener.class})
@EnableJpaAuditing
public class ZenJpaAuditingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AuditorAware.class)
  public AuditorAware<String> zenAuditorAware() {
    return () -> Optional.of("system");
  }
}
