package com.zen.admin.service;

import com.zen.admin.config.LoginSecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 固定窗口计数（键语义即对外契约）：
 *
 * <ul>
 *   <li>{@code auth:fail:{username}} — 失败计数。仅首次 INCR 设置 TTL（=failure-window-ttl，G1-A），
 *       不随后续失败续期，因此是固定窗口而非滑动窗口。
 *   <li>{@code auth:lock:{username}} — 锁标记，TTL = lockout-ttl。锁定期内的尝试既不计数也不续期，
 *       到期自动解锁，无需任何清理任务。
 * </ul>
 *
 * <p>达阈值时写锁并<b>同步删除</b>计数键（G4-2）：否则解锁后的第一个窗口会残留历史计数、一次失败即再次锁定。
 *
 * <p>fail-open（G4-1）：Redis 异常在本类内部 catch 并降级——判锁返回 {@code false}（放行）、计失败返回
 * {@code false}（不触发锁定）、reset 静默。可用性优先于「锁定绝对严格」，避免 Redis 故障时无人能登录。
 */
@Slf4j
@Service
public class RedisLoginAttemptStore implements LoginAttemptStore {

    static final String FAIL_KEY_PREFIX = "auth:fail:";
    static final String LOCK_KEY_PREFIX = "auth:lock:";

    private final StringRedisTemplate redisTemplate;
    private final LoginSecurityProperties properties;

    public RedisLoginAttemptStore(StringRedisTemplate redisTemplate, LoginSecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean isLocked(String username) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + username));
        } catch (DataAccessException e) {
            log.warn("登录锁定状态查询失败，fail-open 放行: username={}", username, e);
            return false;
        }
    }

    @Override
    public boolean recordFailure(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count == null) {
                return false;
            }
            if (count == 1L) {
                redisTemplate.expire(failKey, properties.getFailureWindowTtl());
            }
            if (count >= properties.getMaxFailAttempts()) {
                redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + username, "1", properties.getLockoutTtl());
                redisTemplate.delete(failKey);
                log.warn("登录失败达到阈值，账号锁定: username={}, attempts={}", username, count);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.warn("登录失败计数写入失败，fail-open 不触发锁定: username={}", username, e);
            return false;
        }
    }

    @Override
    public void reset(String username) {
        try {
            redisTemplate.delete(FAIL_KEY_PREFIX + username);
        } catch (DataAccessException e) {
            log.warn("登录失败计数清除失败，fail-open 忽略: username={}", username, e);
        }
    }
}
