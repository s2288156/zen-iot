package com.zen.common.core.security;

/** 请求级登录身份。只能由 {@code AuthInterceptor} 写入并在 {@code afterCompletion} 清除。 */
public final class UserContext {

  private static final ThreadLocal<UserPrincipal> CURRENT = new ThreadLocal<>();

  private UserContext() {}

  public static void set(UserPrincipal principal) {
    CURRENT.set(principal);
  }

  /** 未认证、或在拦截器之外执行(异步线程、定时任务)时返回 {@code null}。 */
  public static UserPrincipal get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
