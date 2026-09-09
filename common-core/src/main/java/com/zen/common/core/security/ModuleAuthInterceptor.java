package com.zen.common.core.security;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 按 {@link RequireModule} 比对当前身份的模块快照。必须排在 {@link AuthInterceptor} 之后，否则读不到 {@link UserContext}。
 */
public class ModuleAuthInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }
    RequireModule required = handlerMethod.getMethodAnnotation(RequireModule.class);
    if (required == null) {
      return true;
    }
    UserPrincipal principal = UserContext.get();
    if (principal == null) {
      throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
    }
    if (!principal.modules().contains(required.value().getCode())) {
      throw new BusinessException(GlobalErrorCode.FORBIDDEN);
    }
    return true;
  }
}
