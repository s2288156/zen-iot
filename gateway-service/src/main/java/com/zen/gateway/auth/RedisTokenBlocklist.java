package com.zen.gateway.auth;

import com.zen.common.security.auth.TokenRevocationChecker;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * 黑名单落在 Redis：Key 与 admin-service 的写侧共用 {@link TokenRevocationChecker#blacklistKey(String)}，
 * 条目按 Token 剩余有效期自动过期。
 *
 * <p>只读不写：撤销是 admin-service 的职责，网关同时持有写能力会让「谁有权作废 Token」变成两处代码。
 */
public class RedisTokenBlocklist implements TokenBlocklist {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisTokenBlocklist(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> isBlocked(String jti) {
        return redisTemplate.hasKey(TokenRevocationChecker.blacklistKey(jti)).defaultIfEmpty(Boolean.FALSE);
    }
}
