package com.zen.common.core.security;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/** 解析身份写入 {@link UserContext},并保证线程归还前清除。 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final String USER_ID_MDC_KEY = "userId";

    private final UserContextResolver resolver;

    public AuthInterceptor(UserContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserPrincipal principal = resolver.resolve(request);
        if (principal == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        UserContext.set(principal);
        MDC.put(USER_ID_MDC_KEY, Long.toString(principal.userId()));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
        MDC.remove(USER_ID_MDC_KEY);
    }
}
