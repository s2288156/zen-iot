package com.zen.common.core.jwt;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT 签发与校验配置,前缀 {@code zen.jwt}。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zen.jwt")
public class JwtProperties {

  /** HS256 密钥原文。未配置时 JWT 相关 Bean 整体不注册,不解析 Token 的服务无需持有密钥。 */
  private String secret;

  private Duration accessTtl = Duration.ofMinutes(30);

  private Duration refreshTtl = Duration.ofDays(7);
}
