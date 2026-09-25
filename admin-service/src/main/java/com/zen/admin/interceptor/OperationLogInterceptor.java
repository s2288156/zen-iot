package com.zen.admin.interceptor;

import com.zen.admin.entity.OperationLogEntity;
import com.zen.admin.repository.OperationLogRepository;
import com.zen.common.security.auth.RequireModule;
import com.zen.common.security.auth.UserContext;
import com.zen.common.security.auth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 操作审计拦截器：与 {@code ModuleAuthInterceptor} 同构的 {@link HandlerInterceptor}（G5-1：不引 AOP）。
 *
 * <p>注册顺序必须排在安全拦截器之后（order 更大），{@code afterCompletion} 按注册逆序回调即最先执行，
 * 此时 {@link UserContext} 尚未被 {@code AuthInterceptor} 清除。
 *
 * <p>两条边界：一是不吞业务异常——业务异常在进 {@code afterCompletion} 前已由
 * {@code GlobalExceptionHandler} 解析为响应，这里只观察状态码，不截获也不重抛任何业务异常；
 * 二是落库 best-effort——审计写失败只记 ERROR 不外抛（决策 (c)），业务操作不因审计缺失而反噬。
 * 落库发生在 service 事务之外：{@code save} 自带事务且此刻业务事务已结束，回滚场景审计行仍在。
 */
@Slf4j
public class OperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogRepository operationLogRepository;

    public OperationLogInterceptor(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        OperationLog annotation = handlerMethod.getMethodAnnotation(OperationLog.class);
        if (annotation == null) {
            return;
        }
        try {
            write(request, response, handlerMethod, annotation);
        } catch (DataAccessException e) {
            log.error(
                    "操作审计落库失败，不影响业务响应: action={}, target={}/{}, path={}",
                    annotation.action(),
                    annotation.targetType(),
                    resolveTargetId(request, annotation.targetIdParam()),
                    request.getRequestURI(),
                    e);
        }
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HandlerMethod handlerMethod,
            OperationLog annotation) {
        RequireModule required = handlerMethod.getMethodAnnotation(RequireModule.class);
        if (required == null) {
            log.error(
                    "@OperationLog 缺少同方法 @RequireModule，module 列无从推导，跳过落库: {}",
                    handlerMethod.getBeanType().getSimpleName() + "#"
                            + handlerMethod.getMethod().getName());
            return;
        }
        UserPrincipal principal = UserContext.get();
        if (principal == null) {
            log.warn("@OperationLog 处理时已无登录身份（异步或身份被提前清除），跳过落库: {}", request.getRequestURI());
            return;
        }
        OperationLogEntity entity = new OperationLogEntity();
        entity.setOperator(principal.username());
        entity.setModule(required.value().getCode());
        entity.setAction(annotation.action().name());
        entity.setTargetType(annotation.targetType().name());
        entity.setTargetId(resolveTargetId(request, annotation.targetIdParam()));
        entity.setResultCode(response.getStatus());
        entity.setOpTime(LocalDateTime.now());
        operationLogRepository.save(entity);
    }

    /** 从 Spring MVC 记录的 URI 模板变量里取 target_id；创建类无路径变量，自然返回 {@code null}。 */
    private static String resolveTargetId(HttpServletRequest request, String paramName) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> map) {
            Object value = map.get(paramName);
            return value == null ? null : value.toString();
        }
        return null;
    }
}
