package com.zen.admin.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 会话登记的 Redis 实现（G5-2）。异常约定：本类不 catch Redis 故障——Spring Data Redis 会包装成
 * {@code DataAccessException} 上抛，由 {@link SessionService} 按决策 (c) 分簿记（降级）与吊销（上抛）两条路处理。
 *
 * <p>死项清理只在读路径顺手 {@code ZREMRANGEBYSCORE}（G5-2：不建后台任务）；登记键自身带 TTL，
 * 索引剔除后残留的 JSON 会自然过期，两侧不需要强一致。
 *
 * <p>ObjectMapper 注入的是 Jackson 3（{@code tools.jackson}）：Boot 4 自动装配的 Bean 就是它，
 * {@code com.fasterxml} 的 Jackson 2 只在类路径上为 jjwt-jackson 服务、没有 Bean（同 gateway 的
 * {@code ApiJsonResponses} 注释）。Instant 序列化由 Jackson 3 核心原生支持，无需额外模块。
 */
@Slf4j
@Service
public class RedisSessionRegistry implements SessionRegistry {

    static final String SESSION_KEY_PREFIX = "auth:session:";
    static final String SESSION_INDEX_KEY = "auth:session:index";
    static final String REFRESH_MAPPING_PREFIX = "auth:session:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSessionRegistry(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(SessionInfo session) {
        Duration ttl = Duration.between(Instant.now(), session.expireTime());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + session.sessionId(), serialize(session), ttl);
        redisTemplate
                .opsForZSet()
                .add(
                        SESSION_INDEX_KEY,
                        session.sessionId(),
                        session.expireTime().toEpochMilli());
        redisTemplate.opsForValue().set(REFRESH_MAPPING_PREFIX + session.currentRefreshJti(), session.sessionId(), ttl);
    }

    @Override
    public Optional<SessionInfo> find(String sessionId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId))
                .map(this::deserialize);
    }

    @Override
    public Optional<String> findSessionIdByRefreshJti(String refreshJti) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_MAPPING_PREFIX + refreshJti));
    }

    @Override
    public void unbindRefresh(String refreshJti) {
        redisTemplate.delete(REFRESH_MAPPING_PREFIX + refreshJti);
    }

    @Override
    public void remove(SessionInfo session) {
        redisTemplate.delete(List.of(
                SESSION_KEY_PREFIX + session.sessionId(), REFRESH_MAPPING_PREFIX + session.currentRefreshJti()));
        redisTemplate.opsForZSet().remove(SESSION_INDEX_KEY, session.sessionId());
    }

    @Override
    public long countActive() {
        purgeExpired();
        Long size = redisTemplate.opsForZSet().zCard(SESSION_INDEX_KEY);
        return size == null ? 0L : size;
    }

    @Override
    public List<SessionInfo> listActive(long offset, long limit) {
        purgeExpired();
        if (limit <= 0) {
            return List.of();
        }
        return load(range(offset, offset + limit - 1));
    }

    @Override
    public List<SessionInfo> listAllActive() {
        purgeExpired();
        return load(range(0, -1));
    }

    private void purgeExpired() {
        redisTemplate
                .opsForZSet()
                .removeRangeByScore(SESSION_INDEX_KEY, 0, Instant.now().toEpochMilli());
    }

    private Set<String> range(long start, long end) {
        Set<String> members = redisTemplate.opsForZSet().range(SESSION_INDEX_KEY, start, end);
        return members == null ? Set.of() : members;
    }

    private List<SessionInfo> load(Set<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        List<String> keys =
                sessionIds.stream().map(id -> SESSION_KEY_PREFIX + id).toList();
        List<String> payloads = redisTemplate.opsForValue().multiGet(keys);
        if (payloads == null) {
            return List.of();
        }
        // 竞态剔除：登记键先于索引项过期时 payload 为 null，跳过本条
        List<SessionInfo> sessions = new ArrayList<>(payloads.size());
        for (String payload : payloads) {
            if (payload != null) {
                sessions.add(deserialize(payload));
            }
        }
        return sessions;
    }

    /** Jackson 3（{@code tools.jackson}）异常是 unchecked，直接上抛由 {@link SessionService} 按决策 (c) 分流。 */
    private String serialize(SessionInfo session) {
        return objectMapper.writeValueAsString(session);
    }

    private SessionInfo deserialize(String payload) {
        return objectMapper.readValue(payload, SessionInfo.class);
    }
}
