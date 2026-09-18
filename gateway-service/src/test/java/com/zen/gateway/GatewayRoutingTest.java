package com.zen.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.zen.common.security.auth.TrustedHeaders;
import com.zen.common.security.jwt.JwtTokenIssuer;
import com.zen.common.security.jwt.TokenPrincipal;
import com.zen.gateway.auth.TokenBlocklist;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

/**
 * 经网关的端到端路由与鉴权测试：真起 Netty 网关 + 一个 JDK 内置 HTTP 桩后端，不碰 MySQL/Nacos/Redis。
 *
 * <p>为什么不打 {@code @Tag("integration")}：剥前缀路由、身份头覆写、统一 JSON 就是本阶段的全部交付物，把它们留在
 * {@code check} 里才有门禁价值。做法是把 {@code admin-service} 那条路由的 {@code uri} 指向桩后端（绕过 {@code lb://}），
 * 并把黑名单换成 mock（绕过 Redis）。
 *
 * <p>用 {@link HttpServer} 而不是 WireMock/MockWebServer：本阶段不为测试引入新依赖，P3-2 定案后再补契约测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            // 不连 Nacos：既避开「干净机器上没有 Nacos」，也让被测 routes 只来自本地配置
            "spring.config.import=",
            "spring.cloud.nacos.discovery.enabled=false"
        })
class GatewayRoutingTest {

    private static final AtomicReference<String> BACKEND_PATH = new AtomicReference<>();

    /** 桩后端收到的请求头，键统一小写（JDK HttpServer 会把头名规范化成 {@code X-user-id} 这种形式）。 */
    private static final Map<String, String> BACKEND_HEADERS = new ConcurrentHashMap<>();

    private static final AtomicInteger BACKEND_CALLS = new AtomicInteger();

    private static final HttpServer BACKEND = startBackend();

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private JwtTokenIssuer tokenIssuer;

    @MockitoBean
    private TokenBlocklist tokenBlocklist;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @DynamicPropertySource
    static void adminRoutePointsAtStubBackend(DynamicPropertyRegistry registry) {
        // 只换路由的目标地址：predicates（Path=/api/admin/**）与 filters（StripPrefix=2）仍来自 application.yml，
        // 于是「经网关剥两段前缀」这条契约本身也是被测对象。（列表按属性源整体覆盖，不能只塞 routes[0].uri。）
        registry.add(
                "GATEWAY_ADMIN_URI",
                () -> "http://localhost:" + BACKEND.getAddress().getPort());
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.stop(0);
    }

    @BeforeEach
    void resetStubStateAndAllowValidTokens() {
        BACKEND_PATH.set(null);
        BACKEND_HEADERS.clear();
        BACKEND_CALLS.set(0);
        when(tokenBlocklist.isBlocked(any())).thenReturn(Mono.just(Boolean.FALSE));
    }

    @Test
    void loginIsRoutedWithPrefixStrippedAndWithoutToken() throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"demo\",\"password\":\"x\"}")));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(BACKEND_PATH.get()).isEqualTo("/auth/login");
    }

    @Test
    void anonymousRequestToProtectedRouteIsRejectedBeforeReachingBackend() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/api/admin/demo/admin")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.body()).contains("\"code\":401");
        assertThat(BACKEND_CALLS.get()).isZero();
    }

    /** 伪造头 + 普通用户 Token 经网关：下游只能看到网关写入的身份，网关覆写不可被绕过（Phase 2 验收标准第 3 条）。 */
    @Test
    void gatewayOwnsTrustedIdentityHeaders() throws IOException, InterruptedException {
        String token = tokenIssuer
                .issue(new TokenPrincipal(7L, "demo", List.of("user"), List.of("ecs", "rcs")))
                .accessToken();

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/demo/admin"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(TrustedHeaders.USER_ID, "1")
                .header(TrustedHeaders.USERNAME, "attacker")
                .header(TrustedHeaders.USER_MODULES, "admin")
                .GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(BACKEND_HEADERS.get(TrustedHeaders.USER_ID.toLowerCase())).isEqualTo("7");
        assertThat(BACKEND_HEADERS.get(TrustedHeaders.USERNAME.toLowerCase())).isEqualTo("demo");
        assertThat(BACKEND_HEADERS.get(TrustedHeaders.USER_MODULES.toLowerCase()))
                .isEqualTo("ecs,rcs");
    }

    @Test
    void revokedTokenIsRejectedEvenThoughSignatureIsValid() throws IOException, InterruptedException {
        when(tokenBlocklist.isBlocked(any())).thenReturn(Mono.just(Boolean.TRUE));
        String token = tokenIssuer
                .issue(new TokenPrincipal(7L, "demo", List.of("user"), List.of("ecs")))
                .accessToken();

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/demo/admin"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(BACKEND_CALLS.get()).isZero();
    }

    /** 路由命中但下游没有任何实例（wcs-service 未注册）：透 503，不压成 500。 */
    @Test
    void missingDownstreamInstanceIsServiceUnavailableNot500() throws IOException, InterruptedException {
        String token = tokenIssuer
                .issue(new TokenPrincipal(7L, "demo", List.of("user"), List.of("wcs")))
                .accessToken();

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/wcs/devices"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.body()).contains("\"code\":503");
    }

    /** 未匹配任何路由的路径也要拿统一 JSON，而不是 whitelabel/ProblemDetail——顺带证明自定义异常处理器确实抢在 Boot 之前。 */
    @Test
    void unmatchedRouteStillGetsUnifiedJson() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(uri("/api/nosuch/things")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(""))
                .contains("application/json");
        assertThat(response.body()).contains("\"code\":404");
    }

    /** 预检不带 Token：跨域必须在鉴权之前短路，否则浏览器一律拿 401。 */
    @Test
    void corsPreflightPassesWithoutToken() throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/auth/login"))
                .header(HttpHeaders.ORIGIN, "http://console.local")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin").orElse(""))
                .isEqualTo("http://console.local");
    }

    // ---------- helpers ----------

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + gatewayPort + path);
    }

    private static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                BACKEND_CALLS.incrementAndGet();
                BACKEND_PATH.set(exchange.getRequestURI().getPath());
                exchange.getRequestHeaders()
                        .forEach((name, values) -> BACKEND_HEADERS.put(name.toLowerCase(), String.join(",", values)));
                // 读完请求体再回：留给 JDK 的 HttpServer 自己丢弃会让 keep-alive 连接上的响应不稳定
                exchange.getRequestBody().readAllBytes();
                byte[] body = "{\"code\":200,\"message\":\"成功\",\"data\":\"stub\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
                exchange.sendResponseHeaders(HttpStatus.OK.value(), body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("测试桩后端启动失败", e);
        }
    }
}
