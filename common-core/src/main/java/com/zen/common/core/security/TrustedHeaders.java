package com.zen.common.core.security;

/**
 * 网关透传给下游的身份头名，两侧共用同一份字面量。
 *
 * <p>刻意放在不含 Servlet 依赖的位置：网关是 WebFlux，业务服务是 Servlet，两边只读这几个字符串常量即可对齐契约。头名一旦写岔
 * （例如网关写 {@code X-User-Module} 而下游读 {@code X-User-Modules}）不会报错，只会让下游判定成「未认证」，属于静默失效，故收敛到单一来源。
 */
public final class TrustedHeaders {

    public static final String USER_ID = "X-User-Id";

    public static final String USERNAME = "X-Username";

    public static final String USER_ROLES = "X-User-Roles";

    public static final String USER_MODULES = "X-User-Modules";

    private TrustedHeaders() {}
}
