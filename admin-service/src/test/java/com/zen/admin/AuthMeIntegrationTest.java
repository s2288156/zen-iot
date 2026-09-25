package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zen.admin.entity.RoleEntity;
import com.zen.admin.entity.UserEntity;
import com.zen.admin.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * {@code /auth/me} 与 {@code /auth/change-password} 的端到端契约：走真实拦截器与真实库，验证「只要求登录、
 * 无模块权限」的鉴权档位与改密语义。
 *
 * <p>取 {@code Admin@123} 作种子口令：它就是 {@code application.yml} 里 Flyway 开发占位符的明文，本测试与
 * {@code OpenApiDocContractTest} 同样只面向本机容器环境（共享库被生产口令覆盖时不适用）。改密用例会改 admin 的
 * 口令，finally 里把 hash 原样写回，避免污染共享开发库；已知取舍——改密不吊销旧 Token，用例顺带断言旧 access
 * Token 改密后仍可用（撤销基建在 Phase 5）。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.nacos.discovery.enabled=false")
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class AuthMeIntegrationTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_DEV_PASSWORD = "Admin@123";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Test
    void meReturnsLatestProfileForLoggedInAdmin() throws Exception {
        UserEntity admin = userRepository.findByUsername(ADMIN).orElseThrow();
        List<Long> expectedRoleIds =
                admin.getRoles().stream().map(RoleEntity::getId).sorted().toList();

        String accessToken = login(ADMIN, ADMIN_DEV_PASSWORD);
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/auth/me")).GET(), accessToken);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = toJson(response).path("data");
        assertThat(data.path("id").asLong()).isEqualTo(admin.getId());
        assertThat(data.path("username").asText()).isEqualTo(ADMIN);
        assertThat(data.path("status").asInt()).isEqualTo(1);
        assertThat(roleIdsOf(data)).containsExactlyElementsOf(expectedRoleIds);
        // 个人中心只回资料：口令与 modules 都不该出现
        assertThat(data.has("password")).isFalse();
        assertThat(data.has("modules")).isFalse();
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/auth/me")).GET(), null);

        assertThat(response.statusCode()).isEqualTo(401);
        JsonNode body = toJson(response);
        assertThat(body.path("code").asInt()).isEqualTo(401);
        assertThat(body.path("message").asText()).isEqualTo("未认证或登录已失效");
    }

    @Test
    void changePasswordReplacesHashAndKeepsOldTokenUsable() throws Exception {
        String originalHash = userRepository.findByUsername(ADMIN).orElseThrow().getPassword();
        String oldToken = login(ADMIN, ADMIN_DEV_PASSWORD);
        String newPwd = "changed-by-self-8";
        try {
            HttpResponse<String> ok = changePassword(oldToken, ADMIN_DEV_PASSWORD, newPwd);
            assertThat(ok.statusCode()).isEqualTo(200);

            // 新口令可登录、旧口令即失效——证明 hash 真的换掉了
            assertThat(login(ADMIN, newPwd)).isNotBlank();
            HttpResponse<String> stale = send(
                    HttpRequest.newBuilder(uri("/auth/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"username\":\"admin\",\"password\":\"" + ADMIN_DEV_PASSWORD + "\"}",
                                    StandardCharsets.UTF_8)),
                    null);
            assertThat(stale.statusCode()).isEqualTo(401);

            // 已知取舍：改密不吊销既有 access Token，它仍有效至自然过期
            HttpResponse<String> stillValid =
                    send(HttpRequest.newBuilder(uri("/auth/me")).GET(), oldToken);
            assertThat(stillValid.statusCode()).isEqualTo(200);

            // 旧口令比对不通过 → 400 固定文案，不透露任何 stored 侧信息
            HttpResponse<String> wrongOld = changePassword(oldToken, "wrong-old-pwd", newPwd);
            assertThat(wrongOld.statusCode()).isEqualTo(400);
            assertThat(toJson(wrongOld).path("message").asText()).isEqualTo("旧口令不正确");
        } finally {
            userRepository.findByUsername(ADMIN).ifPresent(user -> {
                user.setPassword(originalHash);
                userRepository.save(user);
            });
        }
    }

    // ---------- HTTP helpers（Stand-alone 之外还要过真实拦截器，故全走 HTTP） ----------

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = send(
                HttpRequest.newBuilder(uri("/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}",
                                StandardCharsets.UTF_8)),
                null);
        assertThat(response.statusCode()).isEqualTo(200);
        return toJson(response).path("data").path("accessToken").asText();
    }

    private HttpResponse<String> changePassword(String bearer, String oldPassword, String newPassword)
            throws Exception {
        return send(
                HttpRequest.newBuilder(uri("/auth/change-password"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"oldPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword + "\"}",
                                StandardCharsets.UTF_8)),
                bearer);
    }

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

    private List<Long> roleIdsOf(JsonNode data) {
        List<Long> ids = new ArrayList<>();
        data.path("roleIds").forEach(node -> ids.add(node.asLong()));
        return ids;
    }
}
