package com.zen.common.core.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 当前请求的登录身份。
 *
 * @param userId 用户 ID
 * @param username 用户名,同时是 JPA 审计的审计人
 * @param roles 角色编码集合
 * @param modules 模块编码集合,{@code @RequireModule} 的判定依据
 * @param jti 本次请求所带 Token 的唯一 ID,登出时按它写黑名单;网关透传模式下为 {@code null}
 * @param expiresAt Token 过期时刻,决定黑名单条目的 TTL;网关透传模式下为 {@code null}
 */
public record UserPrincipal(
        long userId, String username, List<String> roles, List<String> modules, String jti, Instant expiresAt) {

    public UserPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    /** 剩余有效期;无过期时刻(网关透传模式)时返回 {@code null},调用方据此跳过撤销。 */
    public Duration remainingTtl() {
        if (expiresAt == null) {
            return null;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
