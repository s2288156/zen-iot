package com.zen.common.core.security;

import jakarta.servlet.http.HttpServletRequest;

/** 从请求中还原登录身份,是 {@code zen.security.context-source} 两种取值的共同抽象。 */
public interface UserContextResolver {

  /**
   * @return 未携带凭证时返回 {@code null},由 {@code AuthInterceptor} 统一转成 401;凭证存在但无效则直接抛 {@code
   *     BusinessException(UNAUTHORIZED)},以便区分「没登录」与「登录已失效」
   */
  UserPrincipal resolve(HttpServletRequest request);
}
