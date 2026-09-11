package com.zen.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link RedisTokenRevocationChecker} 的纯单元测试：mock {@link StringRedisTemplate}，不连真实 Redis。
 *
 * <p>覆盖：revoke 的 Key 拼装与 TTL 传递、TTL 为零/负/空时跳过、isRevoked 的 true/false 判定。
 */
@ExtendWith(MockitoExtension.class)
class RedisTokenRevocationCheckerTest {

    private static final String KEY_PREFIX = "auth:blacklist:";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisTokenRevocationChecker checker;

    @Test
    void revokeSetsKeyWithPrefixAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        checker.revoke("jti-123", Duration.ofMinutes(30));

        verify(valueOps).set(eq(KEY_PREFIX + "jti-123"), eq("1"), eq(Duration.ofMinutes(30)));
    }

    @Test
    void revokeWithZeroTtlDoesNothing() {
        checker.revoke("jti-123", Duration.ZERO);

        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void revokeWithNegativeTtlDoesNothing() {
        checker.revoke("jti-123", Duration.ofMinutes(-1));

        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void revokeWithNullTtlDoesNothing() {
        checker.revoke("jti-123", null);

        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void isRevokedReturnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey(KEY_PREFIX + "jti-123")).thenReturn(true);

        assertThat(checker.isRevoked("jti-123")).isTrue();
    }

    @Test
    void isRevokedReturnsFalseWhenKeyMissing() {
        when(redisTemplate.hasKey(KEY_PREFIX + "jti-404")).thenReturn(false);

        assertThat(checker.isRevoked("jti-404")).isFalse();
    }

    @Test
    void isRevokedReturnsFalseWhenRedisReturnsNull() {
        when(redisTemplate.hasKey(KEY_PREFIX + "jti-null")).thenReturn(null);

        assertThat(checker.isRevoked("jti-null")).isFalse();
    }
}
