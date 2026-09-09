package com.zen.common.core.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 校验通过的 Token 内容:签名有效、未过期、{@code typ} 与期望一致才可能构造出来。
 *
 * @param jti Token 唯一 ID,黑名单与撤销的粒度
 * @param userId 用户 ID
 * @param username 用户名
 * @param roles 角色编码集合
 * @param modules 模块编码集合
 * @param type Token 用途
 * @param expiresAt 过期时刻
 */
public record VerifiedToken(
        String jti,
        long userId,
        String username,
        List<String> roles,
        List<String> modules,
        TokenType type,
        Instant expiresAt) {

    /** 剩余有效期,即黑名单条目的 TTL;已过期返回零。 */
    public Duration remainingTtl() {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
