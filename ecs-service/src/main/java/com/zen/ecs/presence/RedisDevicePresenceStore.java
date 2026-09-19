package com.zen.ecs.presence;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis 实现：一个设备一个键，靠键 TTL 自然淘汰。值用 ISO-8601 字符串，便于 redis-cli 直接眼看。 */
@Component
public class RedisDevicePresenceStore implements DevicePresenceStore {

    private static final String KEY_PREFIX = "ecs:heartbeat:";

    private final StringRedisTemplate redisTemplate;

    public RedisDevicePresenceStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void markAlive(long deviceId, Instant seenAt, Duration ttl) {
        redisTemplate.opsForValue().set(key(deviceId), seenAt.toString(), ttl);
    }

    @Override
    public Optional<Instant> lastSeen(long deviceId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(deviceId)))
                .map(Instant::parse);
    }

    @Override
    public void clear(long deviceId) {
        redisTemplate.delete(key(deviceId));
    }

    private static String key(long deviceId) {
        return KEY_PREFIX + deviceId;
    }
}
