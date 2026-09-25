package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * {@code /v3/api-docs} 的文档契约：它是 Apifox、前端与 SDK 生成器唯一的事实来源，所以「文档说的」必须与
 * 「代码做的」一致。三组断言各对应一个实测踩过的失效模式：
 *
 * <ul>
 *   <li>在方法上写 {@code @ApiResponses} 会让 springdoc 不再自动生成成功响应，{@code ApiResponse*} 模型随之从
 *       {@code components.schemas} 消失、错误响应的 {@code $ref} 悬空——所以既断 200 还在，也断每个 {@code $ref} 可解析；
 *   <li>{@code securitySchemes} 与逐接口 {@code security} 缺失会把必须鉴权的接口说成匿名；
 *   <li>响应类型退化成 {@code *}{@code /}* 时工具拿不到 json 语义（本服务只产 json）。
 * </ul>
 *
 * <p>取文档的方式是真 HTTP 而不是 MockMvc：Boot 4 把 {@code @AutoConfigureMockMvc} 拆进了
 * {@code spring-boot-webmvc-test}，不为一个测试给模块加依赖（{@code GatewayRoutingTest} 同一取向）。
 * 完整上下文要 JPA + 数据源，所以与 {@code JpaAuditingIntegrationTest} 同标 integration。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.nacos.discovery.enabled=false")
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class OpenApiDocContractTest {

    private static final String ERROR_ENVELOPE = "ApiResponseVoid";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Test
    void successResponsesSurviveAndEveryRefResolves() throws Exception {
        JsonNode doc = loadDoc();

        Map<String, JsonNode> operations = operations(doc);
        assertThat(operations)
                .containsKeys(
                        "POST /auth/login",
                        "POST /auth/refresh",
                        "POST /auth/logout",
                        "GET /auth/me",
                        "POST /auth/change-password",
                        "GET /demo/admin",
                        "GET /demo/ecs",
                        "POST /roles",
                        "PUT /roles/{id}",
                        "DELETE /roles/{id}",
                        "PUT /roles/{id}/modules",
                        "GET /roles/page",
                        "GET /roles/{id}",
                        "POST /users",
                        "PUT /users/{id}",
                        "PATCH /users/{id}/status",
                        "DELETE /users/{id}",
                        "PUT /users/{id}/password",
                        "PUT /users/{id}/roles",
                        "GET /users/page",
                        "GET /users/{id}",
                        "GET /sessions",
                        "DELETE /sessions/{sessionId}");
        operations.forEach(
                (key, operation) -> assertThat(operation.at("/responses/200").isMissingNode())
                        .as("%s 的 200 响应必须存在（被方法上的 @ApiResponses 顶掉就是回归）", key)
                        .isFalse());
        assertThat(danglingRefs(doc))
                .as("文档里悬空的 $ref：组件名由 springdoc 推导，改返回类型就会漂移")
                .isEmpty();
    }

    @Test
    void authRequirementsMatchTheRuntimeWhitelist() throws Exception {
        JsonNode doc = loadDoc();

        assertThat(doc.at("/components/securitySchemes/bearerJwt/type").asText())
                .isEqualTo("http");
        assertThat(doc.at("/components/securitySchemes/bearerJwt/scheme").asText())
                .isEqualTo("bearer");
        assertThat(doc.at("/components/securitySchemes/bearerJwt/bearerFormat").asText())
                .isEqualTo("JWT");

        // 与服务内 zen.security.whitelist 同口径：只有 login/refresh 匿名，logout/me/change-password 都要身份
        Map<String, JsonNode> operations = operations(doc);
        assertThat(operations.get("POST /auth/login").path("security").isMissingNode())
                .isTrue();
        assertThat(operations.get("POST /auth/refresh").path("security").isMissingNode())
                .isTrue();
        for (String authed : List.of(
                "POST /auth/logout",
                "GET /auth/me",
                "POST /auth/change-password",
                "GET /demo/admin",
                "GET /demo/ecs",
                "GET /sessions",
                "DELETE /sessions/{sessionId}")) {
            assertThat(operations.get(authed).path("security").toString())
                    .as(authed)
                    .contains("bearerJwt");
        }
    }

    @Test
    void errorResponsesAndJsonMediaTypesAreDocumented() throws Exception {
        JsonNode doc = loadDoc();
        Map<String, JsonNode> operations = operations(doc);

        // 400 只属于有请求体的接口；401 所有接口都有；403 只属于带 @RequireModule 的接口（demo 样例 + 角色/用户写接口）
        operations.forEach((key, operation) -> {
            assertThat(operation.at("/responses/401").isMissingNode()).as(key).isFalse();
            boolean hasBody = !operation.path("requestBody").isMissingNode();
            assertThat(!operation.at("/responses/400").isMissingNode()).as(key).isEqualTo(hasBody);
            boolean moduleGated = key.startsWith("GET /demo/")
                    || key.startsWith("POST /roles")
                    || key.startsWith("PUT /roles")
                    || key.startsWith("DELETE /roles")
                    || key.startsWith("POST /users")
                    || key.startsWith("PUT /users")
                    || key.startsWith("PATCH /users")
                    || key.startsWith("DELETE /users")
                    || key.startsWith("GET /sessions")
                    || key.startsWith("DELETE /sessions");
            assertThat(!operation.at("/responses/403").isMissingNode()).as(key).isEqualTo(moduleGated);
            for (Map.Entry<String, JsonNode> response :
                    operation.at("/responses").properties()) {
                assertThat(response.getValue()
                                .at("/content")
                                .path("application/json")
                                .isMissingNode())
                        .as("%s 的 %s 响应应为 application/json", key, response.getKey())
                        .isFalse();
            }
        });

        // 信封模型必须在，否则上面那些 $ref 全是悬空的
        assertThat(doc.at("/components/schemas/" + ERROR_ENVELOPE).isMissingNode())
                .isFalse();
    }

    @Test
    void docIdentifiesItselfAndBothServers() throws Exception {
        JsonNode doc = loadDoc();

        assertThat(doc.at("/info/title").asText()).isEqualTo("zen-iot admin-service API");
        assertThat(doc.at("/info/version").asText()).isNotBlank();
        assertThat(doc.at("/servers").findValuesAsText("url"))
                .containsExactly("http://localhost:28081", "http://localhost:28080/api/admin");
    }

    @Test
    void operationIdsAreNamespacedAndTagsStayReadable() throws Exception {
        JsonNode doc = loadDoc();

        List<String> operationIds = operations(doc).values().stream()
                .map(operation -> operation.at("/operationId").asText())
                .toList();
        assertThat(operationIds).allMatch(id -> id.startsWith("admin-service-")).doesNotHaveDuplicates();

        // 目录名来自 @Tag：auth-controller / demo-controller 这种派生名在 Apifox 树里读不出含义
        assertThat(doc.at("/tags").findValuesAsText("name"))
                .containsExactlyInAnyOrder("认证", "模块鉴权样例", "角色管理", "用户管理", "在线会话");
        operations(doc).forEach((key, operation) -> {
            List<String> tags = new ArrayList<>();
            operation.path("tags").forEach(tag -> tags.add(tag.asText()));
            assertThat(tags).as(key).hasSize(1);
            assertThat(List.of("认证", "模块鉴权样例", "角色管理", "用户管理", "在线会话")).as(key).contains(tags.getFirst());
        });
    }

    @Test
    void fieldDocsAndErrorExamplesAreRealAndLeakNothing() throws Exception {
        JsonNode doc = loadDoc();

        JsonNode loginRequest = doc.at("/components/schemas/LoginRequest/properties");
        assertThat(loginRequest.at("/password/format").asText())
                .as("format=password 才会在客户端里遮罩")
                .isEqualTo("password");
        assertThat(loginRequest.at("/username/description").asText()).isNotBlank();
        assertThat(loginRequest.at("/username/example").asText()).isEqualTo("admin");
        assertThat(doc.at("/components/schemas/RefreshRequest/properties/refreshToken/description")
                        .asText())
                .isNotBlank();

        // 错误 example 必须与 GlobalErrorCode 的实际文案一致，断言抄错比不写更糟
        JsonNode forbidden = operations(doc)
                .get("GET /demo/admin")
                .at("/responses/403/content")
                .path("application/json")
                .path("example");
        assertThat(forbidden.at("/code").asInt()).isEqualTo(403);
        assertThat(forbidden.at("/message").asText()).isEqualTo("无访问权限");
        JsonNode unauthorized = operations(doc)
                .get("POST /auth/logout")
                .at("/responses/401/content")
                .path("application/json")
                .path("example");
        assertThat(unauthorized.at("/message").asText()).isEqualTo("未认证或登录已失效");

        // /v3/api-docs 匿名可读：开发口令不能出现在文档里
        assertThat(doc.toString()).doesNotContain("Admin@123").doesNotContain("Demo@123");
    }

    private JsonNode loadDoc() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    /** 按 {@code METHOD path} 收集全部操作，便于断言时一眼看出是哪个接口挂了。 */
    private Map<String, JsonNode> operations(JsonNode doc) {
        Map<String, JsonNode> byKey = new TreeMap<>();
        for (Map.Entry<String, JsonNode> path : doc.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> method : path.getValue().properties()) {
                byKey.put(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue());
            }
        }
        return byKey;
    }

    /** 递归收集无法解析的 {@code $ref}：指向的组件不在 {@code components/schemas} 里就被列出来。 */
    private List<String> danglingRefs(JsonNode doc) {
        List<String> dangling = new ArrayList<>();
        collectRefs(doc, doc, dangling);
        return dangling;
    }

    private void collectRefs(JsonNode doc, JsonNode node, List<String> sink) {
        if (node.isContainerNode()) {
            node.forEach(child -> collectRefs(doc, child, sink));
            return;
        }
        if (!node.isTextual() || !node.asText().startsWith(SCHEMA_REF_PREFIX)) {
            return;
        }
        String name = node.asText().substring(SCHEMA_REF_PREFIX.length());
        if (doc.at("/components/schemas/" + name).isMissingNode()) {
            sink.add(node.asText());
        }
    }
}
