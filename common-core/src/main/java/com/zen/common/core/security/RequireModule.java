package com.zen.common.core.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口所属业务模块，由 {@link ModuleAuthInterceptor} 在进 Controller 前比对 {@link UserPrincipal#modules()}。
 *
 * <p>只支持方法级：授权跟接口走，同一 Controller 内不同方法可以属于不同模块。缺注解即不校验模块，仍要求已登录。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireModule {

    ModuleCode value();
}
