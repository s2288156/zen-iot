package com.zen.admin.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注写接口的操作审计语义，由 {@link OperationLogInterceptor} 在 {@code afterCompletion} 落一行
 * {@code t_operation_log}。只支持方法级，与 {@code RequireModule} 同一判定时机。
 *
 * <p>{@code operator} 与 {@code result_code} 无需声明：前者取自 {@code UserContext}，后者取响应的
 * HTTP 状态码（已拍板）。{@code module} 不手填——拦截器从同方法上的 {@code @RequireModule} 推导，
 * 缺该注解视为装配错误、跳过落库并记 ERROR，避免同一事实两处维护。
 *
 * <p>刻意不含 {@code module} 属性：见 tasks.md 决策 (a)。注解不引入事务，落库发生在 service 事务外，
 * 业务回滚不丢审计行。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作动作，见 {@link OperationAction} 词表。 */
    OperationAction action();

    /** 目标类型，见 {@link OperationTargetType} 词表。 */
    OperationTargetType targetType();

    /** 取哪个 URI 路径变量作 {@code target_id}；创建类无路径变量时留空不取。 */
    String targetIdParam() default "id";
}
