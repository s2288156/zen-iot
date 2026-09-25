package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zen.admin.service.SessionInfo;
import com.zen.admin.service.SessionRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 在线会话登记与强制下线的端到端契约（Phase 5，G5-2）：走真实 HTTP + 真实 Redis，验证
 * 登录写入 / 轮转迁移（决策 (b)：sessionId 不变、双 jti 与映射键一并迁移）/ 登出清理 /
 * 踢人后谱系双 jti 命中黑名单且 refresh 重放 401 / 自踢 400。
 *
 * <p>会话键结构（{@code RedisSessionRegistry} 的常量是包私有，这里按契约重述）：登记键
 * {@code auth:session:{sessionId}}、ZSET 索引 {@code auth:session:index}（score = expireTime 毫秒）、
 * 反向映射 {@code auth:session:refresh:{refreshJti}}；黑名单键见 {@code TokenRevocationChecker.blacklistKey}。
 *
 * <p>共享 Redis 里可能残留其他用例（如 {@code AuthMeIntegrationTest}）泄漏的 admin 会话，
 * 所以所有断言只针对本用例自己造出的 sessionId / jti（经自己登录的 refresh jti 反查定位），
 * 绝不断言全局会话计数。清理纪律按 README 已知坑：finally 清 {@code auth:fail:{username}}，
 * 兜底删本用例产生的黑名单键与会话残留。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.nacos.discovery.enabled=false")
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class SessionRegistryIntegrationTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_DEV_PASSWORD = "Admin@123";
    private static final String TEMP_PASSWORD = "Temp@12345";

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String SESSION_INDEX_KEY = "auth:session:index";
    private static final String REFRESH_MAPPING_PREFIX = "auth:session:refresh:";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** 本用例产生的清理项：黑名单 jti、会话 id、临时用户名、待删登录失败计数键。 */
    private final List<String> blacklistJtis = new ArrayList<>();

    private final List<String> sessionIdsToPurge = new ArrayList<>();
    private final List<String> usernamesToPurge = new ArrayList<>();

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SessionRegistry registry;

    @AfterEach
    void purgeRedisTraces() {
        usernamesToPurge.forEach(name -> {
            redisTemplate.delete("auth:fail:" + name);
            redisTemplate.delete("auth:lock:" + name);
        });
        blacklistJtis.forEach(jti -> redisTemplate.delete("auth:blacklist:" + jti));
        sessionIdsToPurge.forEach(id -> registry.find(id).ifPresent(registry::remove));
    }

    @Test
    void loginRegistersSessionAndRefreshRotationMovesItAndLogoutClearsIt() throws Exception {
        String adminToken = login(ADMIN, ADMIN_DEV_PASSWORD).accessToken();
        String username = tempUsername();
        long userId = createUser(adminToken, username);
        usernamesToPurge.add(username);
        try {
            Tokens tokens = login(username, TEMP_PASSWORD);
            String refreshJti = jtiOf(tokens.refreshToken());
            blacklistJtis.add(refreshJti);
            blacklistJtis.add(jtiOf(tokens.accessToken()));
            String sessionId = registry.findSessionIdByRefreshJti(refreshJti).orElseThrow();
            sessionIdsToPurge.add(sessionId);

            // 登记键存在且 TTL 跟着 refresh 的 7 天有效期（留 1 分钟余量给时钟与执行耗时）
            assertThat(redisTemplate.hasKey(SESSION_KEY_PREFIX + sessionId)).isTrue();
            Long ttlSeconds = redisTemplate.getExpire(SESSION_KEY_PREFIX + sessionId);
            assertThat(ttlSeconds)
                    .isBetween(
                            Duration.ofDays(7).toSeconds() - 60,
                            Duration.ofDays(7).toSeconds());
            // ZSET score = expireTime 毫秒——尚未过期
            Double score = redisTemplate.opsForZSet().score(SESSION_INDEX_KEY, sessionId);
            assertThat(score).isNotNull().isGreaterThan(System.currentTimeMillis());

            // 分页列表能看到这条会话（只看自己造的，不断言 total）
            HttpResponse<String> listed = send(
                    HttpRequest.newBuilder(uri("/sessions?pageNum=1&pageSize=200"))
                            .GET(),
                    adminToken);
            assertThat(listed.statusCode()).isEqualTo(200);
            List<String> listedIds = new ArrayList<>();
            toJson(listed)
                    .path("data")
                    .path("list")
                    .forEach(node -> listedIds.add(node.path("sessionId").asText()));
            assertThat(listedIds).contains(sessionId);

            // 轮转（决策 (b)）：sessionId 不变，双 jti 与映射一并迁移，旧映射消失
            Tokens rotated = refresh(tokens.refreshToken());
            String newRefreshJti = jtiOf(rotated.refreshToken());
            String newAccessJti = jtiOf(rotated.accessToken());
            blacklistJtis.add(newRefreshJti);
            blacklistJtis.add(newAccessJti);
            assertThat(registry.findSessionIdByRefreshJti(newRefreshJti)).contains(sessionId);
            SessionInfo after = registry.find(sessionId).orElseThrow();
            assertThat(after.currentRefreshJti()).isEqualTo(newRefreshJti);
            assertThat(after.lastAccessJti()).isEqualTo(newAccessJti);
            assertThat(after.issueTime()).isNotNull();
            assertThat(redisTemplate.hasKey(REFRESH_MAPPING_PREFIX + refreshJti))
                    .isFalse();
            assertThat(redisTemplate.hasKey(REFRESH_MAPPING_PREFIX + newRefreshJti))
                    .isTrue();

            // 登出：登记、索引、映射三处全清
            HttpResponse<String> logout = send(
                    HttpRequest.newBuilder(uri("/auth/logout"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"refreshToken\":\"" + rotated.refreshToken() + "\"}", StandardCharsets.UTF_8)),
                    rotated.accessToken());
            assertThat(logout.statusCode()).isEqualTo(200);
            assertThat(redisTemplate.hasKey(SESSION_KEY_PREFIX + sessionId)).isFalse();
            assertThat(redisTemplate.opsForZSet().score(SESSION_INDEX_KEY, sessionId))
                    .isNull();
            assertThat(redisTemplate.hasKey(REFRESH_MAPPING_PREFIX + newRefreshJti))
                    .isFalse();
        } finally {
            deleteUser(adminToken, userId);
        }
    }

    @Test
    void kickoutBlacklistsWholeLineageAndRejectsRefreshReplayAndSecondKick() throws Exception {
        String adminToken = login(ADMIN, ADMIN_DEV_PASSWORD).accessToken();
        String username = tempUsername();
        long userId = createUser(adminToken, username);
        usernamesToPurge.add(username);
        try {
            Tokens tokens = login(username, TEMP_PASSWORD);
            String accessJti = jtiOf(tokens.accessToken());
            String refreshJti = jtiOf(tokens.refreshToken());
            String sessionId = registry.findSessionIdByRefreshJti(refreshJti).orElseThrow();

            HttpResponse<String> kicked =
                    send(HttpRequest.newBuilder(uri("/sessions/" + sessionId)).DELETE(), adminToken);
            assertThat(kicked.statusCode()).isEqualTo(200);

            // 谱系成对拉黑：只拉 access 会被 refresh 复活，只拉 refresh 则旧 access 还能用 30 分钟
            assertThat(redisTemplate.hasKey("auth:blacklist:" + accessJti)).isTrue();
            assertThat(redisTemplate.hasKey("auth:blacklist:" + refreshJti)).isTrue();
            assertThat(redisTemplate.hasKey(SESSION_KEY_PREFIX + sessionId)).isFalse();

            // 重放被轮转前的 refresh → 401（jti 已在黑名单）
            HttpResponse<String> replay = send(
                    HttpRequest.newBuilder(uri("/auth/refresh"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"refreshToken\":\"" + tokens.refreshToken() + "\"}", StandardCharsets.UTF_8)),
                    null);
            assertThat(replay.statusCode()).isEqualTo(401);

            // 登记已删，二次踢 → 404
            HttpResponse<String> again =
                    send(HttpRequest.newBuilder(uri("/sessions/" + sessionId)).DELETE(), adminToken);
            assertThat(again.statusCode()).isEqualTo(404);
        } finally {
            deleteUser(adminToken, userId);
        }
    }

    @Test
    void kickoutOwnSessionReturns400AndKeepsSession() throws Exception {
        Tokens admin = login(ADMIN, ADMIN_DEV_PASSWORD);
        usernamesToPurge.add(ADMIN);
        String refreshJti = jtiOf(admin.refreshToken());
        blacklistJtis.add(refreshJti);
        blacklistJtis.add(jtiOf(admin.accessToken()));
        String sessionId = registry.findSessionIdByRefreshJti(refreshJti).orElseThrow();
        sessionIdsToPurge.add(sessionId);

        HttpResponse<String> selfKick =
                send(HttpRequest.newBuilder(uri("/sessions/" + sessionId)).DELETE(), admin.accessToken());
        assertThat(selfKick.statusCode()).isEqualTo(400);
        assertThat(toJson(selfKick).path("message").asText()).isEqualTo("不能强制下线自己的会话");

        // 自踢被拒后登记原样保留——防自锁不能以「半踢」形态破坏状态
        assertThat(redisTemplate.hasKey(SESSION_KEY_PREFIX + sessionId)).isTrue();
    }

    // ---------- 业务 helpers ----------

    private record Tokens(String accessToken, String refreshToken) {}

    private String tempUsername() {
        return "p5-" + System.nanoTime();
    }

    private Tokens login(String username, String password) throws Exception {
        HttpResponse<String> response = send(
                HttpRequest.newBuilder(uri("/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}",
                                StandardCharsets.UTF_8)),
                null);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = toJson(response).path("data");
        return new Tokens(
                data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private Tokens refresh(String refreshToken) throws Exception {
        HttpResponse<String> response = send(
                HttpRequest.newBuilder(uri("/auth/refresh"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"refreshToken\":\"" + refreshToken + "\"}", StandardCharsets.UTF_8)),
                null);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = toJson(response).path("data");
        return new Tokens(
                data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private long createUser(String bearer, String username) throws Exception {
        HttpResponse<String> response = send(
                HttpRequest.newBuilder(uri("/users"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"username\":\"" + username + "\",\"password\":\"" + TEMP_PASSWORD + "\","
                                        + "\"nickname\":\"phase5 临时\"}",
                                StandardCharsets.UTF_8)),
                bearer);
        assertThat(response.statusCode()).isEqualTo(200);
        return toJson(response).path("data").path("id").asLong();
    }

    private void deleteUser(String bearer, long userId) throws Exception {
        send(HttpRequest.newBuilder(uri("/users/" + userId)).DELETE(), bearer);
    }

    /** 解 JWT 载荷段的 jti：会话登记里的双 jti 与令牌本身同源，测试端只能这样回取。 */
    private String jtiOf(String token) throws Exception {
        String payload = token.split("\\.")[1];
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(payload));
        return claims.path("jti").asText();
    }

    // ---------- HTTP helpers（全走真实 HTTP，与 AuthMeIntegrationTest 同一策略） ----------

    private HttpResponse<String> send(HttpRequest.Builder builder, String bearer) throws Exception {
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return client.send(
                builder.timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private JsonNode toJson(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }
}
