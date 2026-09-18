package com.zen.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.common.core.jwt.JwtProperties;
import com.zen.common.core.jwt.JwtTokenIssuer;
import com.zen.common.core.jwt.JwtTokenVerifier;
import com.zen.common.core.jwt.TokenPair;
import com.zen.common.core.jwt.TokenPrincipal;
import com.zen.common.core.security.TrustedHeaders;
import com.zen.gateway.auth.TokenBlocklist;
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

    /** 换掉签名最后一个字符：长度不变但签名校验必失败。 */
    private String tamper(String token) {
        int cut = token.length() - 1;
        char replacement = token.charAt(cut) == 'a' ? 'b' : 'a';
        return token.substring(0, cut) + replacement;
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
