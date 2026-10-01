package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 导出 admin-service 的完整 OpenAPI JSON 到 {@code build/api-docs/openapi.json}，供 Apifox 导入与
 * 文档漂移比对消费；顺手把「产物必须是一份真文档」钉成断言。
 *
 * <p>取代此前的骨架实现：它只往空的 {@code paths} 里塞两个 {@code x-controller} 扩展，产物实测 517 字节、
 * {@code paths} 恒为 0，而 javadoc 承诺「CI 将对比生成的 JSON 与 main 分支基线」——没有任何 workflow 引用过它。
 * 真正的危险在于这份产物长得像能用：拿它去导入等于用空集对齐 23 条接口。下面的操作数下限、关键路径与错误信封
 * 三项断言，就是让「骨架冒充文档」重新变成构建失败而不是静默事故。
 *
 * <p>取真 HTTP 而不是 MockMvc，与 {@code OpenApiDocContractTest} 同一取向（Boot 4 把
 * {@code @AutoConfigureMockMvc} 拆进了 {@code spring-boot-webmvc-test}，不为一个测试给模块加依赖）；
 * 完整上下文要 JPA 与数据源，所以同样标 {@code integration}。
 *
 * <p>带 Bearer 拉取：文档端点不属于免鉴权白名单，这里同时验证一次「拿不到身份就导不出文档」。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.nacos.discovery.enabled=false")
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class OpenApiExportTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_DEV_PASSWORD = "Admin@123";

    /** 下限而非精确值：新增接口不该让这里变红，但 0 条的骨架必须被拦下（当前实测 23 个操作）。 */
    private static final int MIN_OPERATIONS = 20;

    private static final Path OUTPUT_DIR = Path.of("build", "api-docs");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Test
    void exportsFullOpenApiJson() throws Exception {
        String accessToken = login();
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET(), accessToken, StandardCharsets.UTF_8);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode doc = objectMapper.readTree(response.body());

        assertThat(operations(doc).size()).as("paths 为空就是骨架，不是文档").isGreaterThanOrEqualTo(MIN_OPERATIONS);
        assertThat(operations(doc)).containsKey("POST /auth/login");
        // 全部错误响应的 $ref 都指向它，缺了就是整篇悬空
        assertThat(doc.at("/components/schemas/ApiResponseVoid").isMissingNode())
                .isFalse();
        // 产物是要外发给 Apifox 与 SDK 生成器的：种子口令不能出现在里面
        assertThat(response.body()).doesNotContain(ADMIN_DEV_PASSWORD).doesNotContain("Demo@123");

        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve("openapi.json");
        Files.writeString(
                out, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc), StandardCharsets.UTF_8);
        assertThat(out).isRegularFile().isNotEmptyFile();
    }

    /** 种子账号登录取 access Token——与 {@code AuthMeIntegrationTest} 同一份 Flyway 开发口令。 */
    private String login() throws Exception {
        String body = "{  \"username\": \"" + ADMIN + "\", \"password\": \"" + ADMIN_DEV_PASSWORD + "\"}";
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/auth/login"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).at("/data/accessToken").asText();
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, String accessToken, Charset charset)
            throws IOException, InterruptedException {
        return client.send(
                builder.timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + accessToken)
                        .build(),
                HttpResponse.BodyHandlers.ofString(charset));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /** 按 {@code METHOD path} 展开全部操作，与 {@code OpenApiDocContractTest} 同一口径。 */
    private Map<String, JsonNode> operations(JsonNode doc) {
        Map<String, JsonNode> byKey = new TreeMap<>();
        for (Map.Entry<String, JsonNode> path : doc.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> method : path.getValue().properties()) {
                byKey.put(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue());
            }
        }
        return byKey;
    }
}
