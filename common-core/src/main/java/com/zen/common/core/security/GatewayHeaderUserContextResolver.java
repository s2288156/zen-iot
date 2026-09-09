package com.zen.common.core.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 信任网关透传身份头的模式:本服务不持有 JWT 密钥,也不校验签名,前提是流量只能来自网关。
 *
 * <p>缺少 {@code X-User-Id} 即视为未认证。直连本服务而绕过网关的请求会因此拿到 401,而不是被伪装的头部放行。
 */
public class GatewayHeaderUserContextResolver implements UserContextResolver {

  public static final String USER_ID_HEADER = "X-User-Id";
  public static final String USERNAME_HEADER = "X-Username";
  public static final String ROLES_HEADER = "X-User-Roles";
  public static final String MODULES_HEADER = "X-User-Modules";

  @Override
  public UserPrincipal resolve(HttpServletRequest request) {
    long userId = parseUserId(request.getHeader(USER_ID_HEADER));
    if (userId < 0) {
      return null;
    }
    return new UserPrincipal(
        userId,
        request.getHeader(USERNAME_HEADER),
        csv(request.getHeader(ROLES_HEADER)),
        csv(request.getHeader(MODULES_HEADER)),
        null,
        null);
  }

  private long parseUserId(String value) {
    if (value == null || value.isBlank()) {
      return -1;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private List<String> csv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }
}
