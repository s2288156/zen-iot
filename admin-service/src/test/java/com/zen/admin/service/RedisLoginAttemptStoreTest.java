package com.zen.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zen.admin.config.LoginSecurityProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link RedisLoginAttemptStore} 的纯单元测试：mock {@link StringRedisTemplate}，
 * 钉死键语义（{@code auth:fail:*} 固定窗口、达阈值写 {@code auth:lock:*} 并删计数键）与 fail-open 降级。
 *
 * <p>真 Redis 行为（TTL 计时、自动解锁）在 {@code LoginAttemptStoreIntegrationTest} 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class RedisLoginAttemptStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginSecurityProperties properties;
    private RedisLoginAttemptStore store;

    @BeforeEach
    void setUp() {
        properties = new LoginSecurityProperties(); // 默认：5 次 / 锁定 15m / 窗口 15m
        store = new RedisLoginAttemptStore(redisTemplate, properties);
    }

    // ---------- isLocked ----------

    @Test
    void isLockedReturnsTrueWhenLockKeyExists() {
        when(redisTemplate.hasKey("auth:lock:admin")).thenReturn(true);

        assertThat(store.isLocked("admin")).isTrue();
    }

    @Test
    void isLockedReturnsFalseWhenLockKeyMissing() {
        when(redisTemplate.hasKey("auth:lock:admin")).thenReturn(false);

        assertThat(store.isLocked("admin")).isFalse();
    }

    // ---------- recordFailure ----------

    @Test
    void firstFailureSetsWindowTtlOnCounter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:fail:admin")).thenReturn(1L);

        assertThat(store.recordFailure("admin")).isFalse();

        // 仅首次 INCR 设 TTL——固定窗口，不随后续失败续期（G1-A）
        verify(redisTemplate).expire("auth:fail:admin", Duration.ofMinutes(15));
    }

    @Test
    void subsequentFailureBelowThresholdDoesNotResetTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:fail:admin")).thenReturn(3L);

        assertThat(store.recordFailure("admin")).isFalse();

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void failureAtThresholdWritesLockAndDeletesCounter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:fail:admin")).thenReturn(5L);

        assertThat(store.recordFailure("admin")).isTrue();

        // 达阈值：写锁标记（TTL=lockout-ttl）并同步删计数键（G4-2），避免解锁后残留历史计数
        verify(valueOperations).set("auth:lock:admin", "1", Duration.ofMinutes(15));
        verify(redisTemplate).delete("auth:fail:admin");
    }

    @Test
    void nullIncrementResultIsTreatedAsNoLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:fail:admin")).thenReturn(null);

        assertThat(store.recordFailure("admin")).isFalse();

        verify(redisTemplate, never()).delete(anyString());
    }

    // ---------- reset ----------

    @Test
    void resetDeletesCounter() {
        store.reset("admin");

        verify(redisTemplate).delete("auth:fail:admin");
    }

    // ---------- fail-open（G4-1）----------

    @Test
    void isLockedFailsOpenWhenRedisIsDown() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new QueryTimeoutException("redis down"));

        assertThat(store.isLocked("admin")).isFalse();
    }

    @Test
    void recordFailureFailsOpenWhenRedisIsDown() {
        when(redisTemplate.opsForValue()).thenThrow(new QueryTimeoutException("redis down"));

        assertThat(store.recordFailure("admin")).isFalse();
    }

    @Test
    void resetSwallowsRedisErrors() {
        when(redisTemplate.delete(anyString())).thenThrow(new QueryTimeoutException("redis down"));

        assertThatCode(() -> store.reset("admin")).doesNotThrowAnyException();
    }
}
