package com.zen.common.core.jwt;

import java.util.List;

/**
 * 待签发 Token 所承载的身份。
 *
 * @param userId 用户 ID,落在 {@code sub} claim
 * @param username 用户名
 * @param roles 角色编码集合
 * @param modules 模块编码集合,授权判定依据
 */
public record TokenPrincipal(long userId, String username, List<String> roles, List<String> modules) {

    public TokenPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
