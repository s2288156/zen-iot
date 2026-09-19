package com.zen.ecs.presence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 心跳存储与真 Redis 的契约：键存在、值可解析回同一时刻、**TTL 确实挂上了**、清除后判定为缺席。
 *
 * <p>TTL 是这里唯一必须用真客户端验证的部分——单元测试里的假存储无法表达「键自行过期」，而它正是超时判据的另一半：
 * 键不过期，设备被拆除后 {@code lastSeen} 会永远返回一个旧时刻，离线事件就只在第一次探测时触发一次。
 *
 * <p>用的是一个不可能与 {@code t_device.id}（自增）相撞的合成 ID：本服务 {@code @EnableScheduling}，
 * 超时探测每 {@code zen.ecs.heartbeat.probe-interval} 跑一次并会 {@code clear} 已判离线设备的键。
 * 若拿真实在线设备做样本，测试刚写入的键可能被那个后台任务清掉——红得毫无道理，且只在时序不巧时红。
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration") // 依赖本机 Redis
class RedisDevicePresenceStoreIntegrationTest {

    /** 与 {@code RedisDevicePresenceStore} 的 KEY_PREFIX 一致：查 TTL 必须自己拼键名，改前缀时这里要同步。 */
    private static final String KEY_PREFIX = "ecs:heartbeat:";

    private static final Duration TTL = Duration.ofSeconds(30);

    @Autowired
    private DevicePresenceStore presenceStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private long deviceId;

    @BeforeEach
    void pickSyntheticDeviceId() {
        deviceId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
    }

    @AfterEach
    void removeProbeKey() {
        presenceStore.clear(deviceId);
    }

    @Test
    void markAliveIsVisibleAndBoundedByTtl() {
        Instant seenAt = Instant.parse("2026-03-05T08:30:00Z");

        presenceStore.markAlive(deviceId, seenAt, TTL);

        assertThat(presenceStore.lastSeen(deviceId)).contains(seenAt);
        // 键的剩余生存期（秒）：TTL 是超时判据的另一半——键不过期，设备被拆除后 lastSeen 会永远返回旧时刻
        assertThat(redisTemplate.getExpire(KEY_PREFIX + deviceId)).isBetween(1L, TTL.toSeconds());
    }

    @Test
    void clearMakesTheDeviceLookAbsent() {
        presenceStore.markAlive(deviceId, Instant.now(), TTL);

        presenceStore.clear(deviceId);

        assertThat(presenceStore.lastSeen(deviceId)).isEmpty();
    }

    @Test
    void neverReportedDeviceIsAbsent() {
        assertThat(presenceStore.lastSeen(deviceId)).isEmpty();
    }
}
