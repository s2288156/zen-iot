package com.zen.admin.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明某个接口会返回、但 springdoc 从方法签名推不出来的错误状态码。
 *
 * <p>文档侧的错误响应由 {@code ZenAdminOpenApiConfiguration#apiErrorResponses()} 推导：有请求体→400、要身份→401、
 * 方法带 {@code @RequireModule}→403。登录接口的 403「账号已停用」与 429「账号已锁定」是 service 抛
 * {@code BusinessException} 带出来的，签名上看不到，于是文档里少了这两个码——而 {@code admin-service/README.md}
 * 与实际行为都有它们。本注解把这最后一类也变成从代码推导，不再依赖有人在文档里手抄。
 *
 * <p>刻意不用方法级 {@code @ApiResponses}：实测它会让 springdoc 不再生成成功响应，连带 {@code ApiResponse*}
 * 模型从 {@code components.schemas} 消失、错误响应的 {@code $ref} 悬空。
 *
 * <p>{@link #message()} 必须是实际输出的文案：Apifox 的用例断言与 Mock 直接依赖文档里的 example，写错比不写更糟。
 * 注意 {@code GlobalErrorCode} 的默认消息与部分抛点自定义的消息不同（429 实际是「账号已锁定,请稍后重试」，
 * 不是枚举里那句「请求过于频繁,请稍后重试」），所以这里显式带上而不是复用枚举。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(DocErrors.class)
public @interface DocError {

    /** HTTP 状态码，同时也是统一信封里的 {@code code}（两者同源）。 */
    int status();

    /** 该接口实际返回的 {@code message}，作为响应示例进文档。 */
    String message();

    /** 什么情况下会拿到这个码，写给调用方看。 */
    String description();
}
