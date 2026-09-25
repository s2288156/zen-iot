package com.zen.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.common.security.auth.TrustedHeaders;
import com.zen.common.security.jwt.JwtProperties;
import com.zen.common.security.jwt.JwtTokenIssuer;
import com.zen.common.security.jwt.JwtTokenVerifier;
import com.zen.common.security.jwt.TokenPair;
import com.zen.common.security.jwt.TokenPrincipal;
import com.zen.gateway.auth.TokenBlocklist;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link JwtAuthGlobalFilter} 的纯单元测试：真实 jjwt 验签（与 admin-service 同一套代码），黑名单用桩，不起 Spring 上下文、不碰任何中间件。
 */
class JwtAuthGlobalFilterTest {

    private static final String SECRET = "zen-test-gateway-hs256-secret-key-32-bytes";

    private static final String PROTECTED_PATH = "/api/admin/demo/admin";

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    private final StubTokenBlocklist blocklist = new StubTokenBlocklist();

    private JwtTokenIssuer tokenIssuer;
    private JwtAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        tokenIssuer = new JwtTokenIssuer(properties);
        filter = new JwtAuthGlobalFilter(
                new JwtTokenVerifier(properties),
                blocklist,
                JsonMapper.builder().build(),
                List.of("/api/*/auth/login", "/api/*/actuator/health/**"));
    }

    @Test
    void whitelistedPathPassesThroughButStillStripsIdentityHeaders() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/admin/auth/login")
                .header(TrustedHeaders.USER_ID, "1")
                .header(TrustedHeaders.USER_MODULES, "admin"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // 白名单不等于「原样透传」：伪造的身份头必须已被剥掉，否则 /auth/logout 会认这个假身份
        assertThat(forwarded.get().getRequest().getHeaders().get(TrustedHeaders.USER_ID))
                .isNull();
        assertThat(forwarded.get().getRequest().getHeaders().get(TrustedHeaders.USER_MODULES))
                .isNull();
    }

    @Test
    void spoofedForwardedForIsReplacedWithTrustedClientIp() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(PROTECTED_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + issueToken())
                .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
                .remoteAddress(new InetSocketAddress("203.0.113.9", 54321)));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // 入站伪造链整体作废：下游只见到网关连接 remoteAddr 这一个可信值
        assertThat(forwarded.get().getRequest().getHeaders().get("X-Forwarded-For"))
                .containsExactly("203.0.113.9");
    }

    @Test
    void whitelistedLoginPathAlsoGetsTrustedForwardedFor() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/admin/auth/login")
                .header("X-Forwarded-For", "1.2.3.4")
                .remoteAddress(new InetSocketAddress("203.0.113.7", 12345)));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // 免鉴权路径同样覆写——登录日志（Phase 4）记的正是这条路径的 IP
        assertThat(forwarded.get().getRequest().getHeaders().get("X-Forwarded-For"))
                .containsExactly("203.0.113.7");
    }

    @Test
    void missingRemoteAddressStripsForwardedForWithoutFabricatingOne() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(PROTECTED_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + issueToken())
                .header("X-Forwarded-For", "1.2.3.4"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // remoteAddr 缺失时宁缺毋伪：剥掉伪造值，也不凭空造一个
        assertThat(forwarded.get().getRequest().getHeaders().get("X-Forwarded-For"))
                .isNull();
    }

    @Test
    void healthCheckUnderPrefixIsWhitelisted() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/admin/actuator/health/readiness"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(forwarded.get()).isNotNull();
    }

    @Test
    void missingTokenIsRejectedWithUnifiedJson() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(PROTECTED_PATH));

        StepVerifier.create(filter.filter(exchange, chain).then(Mono.defer(() -> body(exchange))))
                .expectNextMatches(json -> json.contains("\"code\":401") && json.contains("未携带访问令牌"))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void tamperedTokenIsRejected() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(PROTECTED_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamper(issueToken())));

        StepVerifier.create(filter.filter(exchange, chain).then(Mono.defer(() -> body(exchange))))
                .expectNextMatches(json -> json.contains("\"code\":401"))
                .verifyComplete();

        assertThat(forwarded.get()).isNull();
    }

    @Test
    void refreshTokenCannotAccessBusinessApi() {
        TokenPair pair = tokenIssuer.issue(principal());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(PROTECTED_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.refreshToken()));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void blacklistedTokenIsRejected() {
        blocklist.blocked = true;
        MockServerWebExchange exchange = authorized(issueToken());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void validTokenOverwritesSpoofedIdentityHeaders() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(PROTECTED_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + issueToken())
                .header(TrustedHeaders.USER_ID, "1")
                .header(TrustedHeaders.USERNAME, "attacker")
                .header(TrustedHeaders.USER_ROLES, "admin")
                .header(TrustedHeaders.USER_MODULES, "admin"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst(TrustedHeaders.USER_ID)).isEqualTo("7");
        assertThat(headers.getFirst(TrustedHeaders.USERNAME)).isEqualTo("demo");
        assertThat(headers.getFirst(TrustedHeaders.USER_ROLES)).isEqualTo("user");
        // 下游 GatewayHeaderUserContextResolver 按逗号切分，这里固化两侧共同的编码格式
        assertThat(headers.getFirst(TrustedHeaders.USER_MODULES)).isEqualTo("ecs,rcs");
    }

    @Test
    void blocklistFailureFailsClosedInsteadOfPassingThrough() {
        blocklist.failure = new IllegalStateException("redis down");
        MockServerWebExchange exchange = authorized(issueToken());

        // 校验缺位时宁可让错误信号透出兜成 5xx，也不能放行
        StepVerifier.create(filter.filter(exchange, chain)).verifyError(IllegalStateException.class);
        assertThat(forwarded.get()).isNull();
    }

    // ---------- helpers ----------

    private MockServerWebExchange authorized(String token) {
        return exchange(MockServerHttpRequest.get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String issueToken() {
        return tokenIssuer.issue(principal()).accessToken();
    }

    private TokenPrincipal principal() {
        return new TokenPrincipal(7L, "demo", List.of("user"), List.of("ecs", "rcs"));
    }

    /**
     * 篡改签名首位字符：长度不变、签名必不匹配。
     *
     * <p>刻意不动<b>末位</b>字符——HS256 的 32 字节签名编码成 base64url 后共 43 个字符，最后一个字符有 2 位不属于任何
     * 字节，改它有四分之一概率解出同一个签名，测试于是「偶发通过」（与 {@code ba7c27a} 修掉的 JwtTokenTest 那类失效同源）。
     */
    private String tamper(String token) {
        int head = token.lastIndexOf('.') + 1;
        char original = token.charAt(head);
        char replacement = original == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, head) + replacement + token.substring(head + 1);
        assertThat(tampered).isNotEqualTo(token);
        return tampered;
    }

    private MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request);
    }

    private Mono<String> body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString();
    }

    private static final class StubTokenBlocklist implements TokenBlocklist {

        private boolean blocked;
        private RuntimeException failure;

        @Override
        public Mono<Boolean> isBlocked(String jti) {
            return failure != null ? Mono.error(failure) : Mono.just(blocked);
        }
    }
}
