package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.admin.service.LoginAttemptStore;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link LoginAttemptStore} 对真 Redis 的行为契约：固定窗口计数、达阈值写锁并删计数键、锁 TTL 到期自动解锁。
 *
 * <p>锁 TTL 压到 3 秒让「自动解锁」可在测试内验证；用户名带纳秒后缀隔离并发与历史残留，finally 清理两个键。
 * Mockito 单测（{@code RedisLoginAttemptStoreTest}）钉的是交互，本测试钉的是 Redis 侧真实语义。
 */
@SpringBootTest(properties = {"spring.cloud.nacos.discovery.enabled=false", "zen.security.login.lockout-ttl=3s"})
@Tag("integration") // 依赖本机 Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class LoginAttemptStoreIntegrationTest {

    @Autowired
    private LoginAttemptStore attemptStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void failuresAccumulateInFixedWindowAndFifthFailureLocksThenAutoUnlocks() throws Exception {
        String username = "it-lock-" + System.nanoTime();
        String failKey = "auth:fail:" + username;
        String lockKey = "auth:lock:" + username;
        try {
            // 前 4 次只计数，不锁定
            for (int i = 0; i < 4; i++) {
                assertThat(attemptStore.recordFailure(username)).isFalse();
                assertThat(attemptStore.isLocked(username)).isFalse();
            }
            assertThat(redisTemplate.opsForValue().get(failKey)).isEqualTo("4");
            // 固定窗口：TTL 只在首次 INCR 设置，剩余量仍在窗口内（默认 15m，容 1 分钟下限防误判）
            Long windowTtl = redisTemplate.getExpire(failKey, TimeUnit.SECONDS);
            assertThat(windowTtl).isBetween(14L * 60, 15L * 60);

            // 第 5 次触发锁定：写锁标记 + 删除计数键
            assertThat(attemptStore.recordFailure(username)).isTrue();
            assertThat(attemptStore.isLocked(username)).isTrue();
            assertThat(redisTemplate.hasKey(failKey)).isFalse();
            assertThat(redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)).isBetween(0L, 3L);

            // 锁 TTL 到期自动解锁，无需清理任务
            Thread.sleep(3_200L);
            assertThat(attemptStore.isLocked(username)).isFalse();
        } finally {
            redisTemplate.delete(failKey);
            redisTemplate.delete(lockKey);
        }
    }

    @Test
    void resetClearsFailureCounter() {
        String username = "it-reset-" + System.nanoTime();
        String failKey = "auth:fail:" + username;
        try {
            assertThat(attemptStore.recordFailure(username)).isFalse();
            assertThat(redisTemplate.hasKey(failKey)).isTrue();

            attemptStore.reset(username);

            assertThat(redisTemplate.hasKey(failKey)).isFalse();
        } finally {
            redisTemplate.delete(failKey);
        }
    }
}
