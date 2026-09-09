package com.zen.admin.service;

import com.zen.common.core.security.TokenRevocationChecker;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 黑名单落在 Redis:Key 为 {@code auth:blacklist:{jti}},TTL 取 Token 剩余有效期,到期自动清除。 */
@Service
public class RedisTokenRevocationChecker implements TokenRevocationChecker {

  private static final String KEY_PREFIX = "auth:blacklist:";

  private final StringRedisTemplate redisTemplate;

  public RedisTokenRevocationChecker(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void revoke(String jti, Duration ttl) {
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      return;
    }
    redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
  }

  @Override
  public boolean isRevoked(String jti) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
  }
}
