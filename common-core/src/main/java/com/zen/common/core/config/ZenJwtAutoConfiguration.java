package com.zen.common.core.config;

import com.zen.common.core.jwt.JwtProperties;
import com.zen.common.core.jwt.JwtTokenIssuer;
import com.zen.common.core.jwt.JwtTokenVerifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * JWT 签发与校验的自动配置。Servlet 与 Reactive 应用通用,所以 {@code com.zen.common.core.jwt} 不引用 Servlet API——Phase
 * 2 的 WebFlux 网关复用同一份代码。
 *
 * <p>以 {@code zen.jwt.secret} 是否存在为开关:只标注解、不解析 Token 的业务服务（WCS/RCS/ECS）不必持有密钥,也不会因缺配置而启动失败。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zen.jwt", name = "secret")
@EnableConfigurationProperties(JwtProperties.class)
public class ZenJwtAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public JwtTokenIssuer jwtTokenIssuer(JwtProperties properties) {
    return new JwtTokenIssuer(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public JwtTokenVerifier jwtTokenVerifier(JwtProperties properties) {
    return new JwtTokenVerifier(properties);
  }
}
