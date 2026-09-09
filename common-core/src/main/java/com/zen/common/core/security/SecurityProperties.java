package com.zen.common.core.security;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 认证拦截配置,前缀 {@code zen.security}。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zen.security")
public class SecurityProperties {

  /** 关闭后两个拦截器都不注册,鉴权与模块校验一并跳过,仅供本地开发与测试。 */
  private boolean enabled = true;

  /** 身份来源。 */
  private ContextSource contextSource = ContextSource.JWT;

  /** 免鉴权路径(Ant 风格),由拦截器注册处的 excludePathPatterns 消费。 */
  private List<String> whitelist = List.of();

  /** 身份来源类型。 */
  public enum ContextSource {

    /** 自行用 {@code zen.jwt.secret} 解析 {@code Authorization: Bearer}。 */
    JWT,

    /** 信任网关透传的 {@code X-User-*} 头,本服务不持有密钥。 */
    GATEWAY_HEADER
  }
}
