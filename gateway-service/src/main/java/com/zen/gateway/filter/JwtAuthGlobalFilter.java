package com.zen.gateway.filter;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.ErrorCode;
import com.zen.common.core.exception.GlobalErrorCode;
import com.zen.common.core.jwt.JwtTokenVerifier;
import com.zen.common.core.jwt.TokenType;
import com.zen.common.core.jwt.VerifiedToken;
import com.zen.common.core.security.TrustedHeaders;
import com.zen.gateway.auth.TokenBlocklist;
import com.zen.gateway.error.ApiJsonResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * 统一鉴权全局过滤器：白名单放行 → 取 Token → 签名/过期/{@code typ=access} 校验 → Redis 黑名单校验 → 覆写并透传身份头。
 *
 * <p>顺序取 {@link Ordered#HIGHEST_PRECEDENCE}：鉴权与身份头覆写必须先于任何路由转发过滤器完成。
 *
 * <p><b>入站 {@code X-User-*} 一律先剥离</b>，白名单路径同样要剥（否则 {@code /auth/logout} 会收到伪造的
 * {@code X-User-Id: 1}）。剥离写在过滤器里而不是路由的 {@code default-filters: RemoveRequestHeader}：两类过滤器合并后按
 * order 排序，声明式那条排在鉴权过滤器之后就会把网关刚写好的头再删一遍。
 *
 * <p>验签是纯 CPU 操作，刻意留在 EventLoop 上；黑名单查询走 {@link TokenBlocklist} 的反应式实现，全链路不出现 {@code block()}。
 *
 * <p>响应体由本过滤器直接写出，不经 {@code ErrorWebExceptionHandler}：全局过滤器抛出的错误能否走到异常处理器取决于框架的
 * 过滤器链实现，直接写响应把这个不确定性挡在门外。
 */
@Slf4j
// final 是 CT_CONSTRUCTOR_THROW 的正解：构造期解析白名单失败必须直接抛（宁可启动失败也不留一个「永不匹配的白名单」），
// 而非 final 的类一旦构造器抛异常就可能被 finalizer 拿到半初始化对象。
public final class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> TRUSTED_REQUEST_HEADERS = List.of(
            TrustedHeaders.USER_ID, TrustedHeaders.USERNAME, TrustedHeaders.USER_ROLES, TrustedHeaders.USER_MODULES);

    private final JwtTokenVerifier tokenVerifier;
    private final TokenBlocklist tokenBlocklist;
    private final ObjectMapper objectMapper;
    private final List<PathPattern> whitelist;

    /**
     * @param tokenVerifier 与 admin-service 同一密钥的验签器（{@code common-core} 自动装配）
     * @param tokenBlocklist 反应式黑名单读取，与 Phase 1 共用 {@code auth:blacklist:{jti}}
     * @param whitelistPaths 免鉴权路径，匹配的是<b>带前缀的入站路径</b>（如 {@code /api/admin/auth/login}），与下游服务内的裸路径不是同一套字符串
     */
    public JwtAuthGlobalFilter(
            JwtTokenVerifier tokenVerifier,
            TokenBlocklist tokenBlocklist,
            ObjectMapper objectMapper,
            List<String> whitelistPaths) {
        this.tokenVerifier = tokenVerifier;
        this.tokenBlocklist = tokenBlocklist;
        this.objectMapper = objectMapper;
        PathPatternParser parser = new PathPatternParser();
        this.whitelist = whitelistPaths.stream().map(parser::parse).toList();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = withoutTrustedHeaders(exchange);
        if (isWhitelisted(sanitized)) {
            return chain.filter(sanitized);
        }
        return authorize(sanitized, chain);
    }

    private Mono<Void> authorize(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = bearerToken(exchange.getRequest());
        if (token == null) {
            return reject(exchange, GlobalErrorCode.UNAUTHORIZED, "未携带访问令牌");
        }
        VerifiedToken verified;
        try {
            verified = tokenVerifier.verify(token, TokenType.ACCESS);
        } catch (BusinessException e) {
            return reject(exchange, e.getErrorCode(), e.getMessage());
        }
        // 黑名单读失败时以错误信号透出，兜成 5xx——宁可拒绝也不在校验缺位时放行
        return tokenBlocklist
                .isBlocked(verified.jti())
                .flatMap(blocked -> Boolean.TRUE.equals(blocked)
                        ? reject(exchange, GlobalErrorCode.UNAUTHORIZED, "登录已失效，请重新登录")
                        : chain.filter(withTrustedIdentity(exchange, verified)));
    }

    private boolean isWhitelisted(ServerWebExchange exchange) {
        var path = exchange.getRequest().getPath();
        return whitelist.stream().anyMatch(pattern -> pattern.matches(path));
    }

    /** 剥离客户端可能自带的身份头；{@code mutate()} 只影响后续链路，不改原始请求。 */
    private ServerWebExchange withoutTrustedHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(builder -> builder.headers(headers -> TRUSTED_REQUEST_HEADERS.forEach(headers::remove)))
                .build();
    }

    private ServerWebExchange withTrustedIdentity(ServerWebExchange sanitized, VerifiedToken verified) {
        return sanitized
                .mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.set(TrustedHeaders.USER_ID, Long.toString(verified.userId()));
                    if (verified.username() != null) {
                        headers.set(TrustedHeaders.USERNAME, verified.username());
                    }
                    // 下游 GatewayHeaderUserContextResolver 按逗号切分，这里就是它的解析契约
                    headers.set(TrustedHeaders.USER_ROLES, String.join(",", verified.roles()));
                    headers.set(TrustedHeaders.USER_MODULES, String.join(",", verified.modules()));
                }))
                .build();
    }

    private String bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> reject(ServerWebExchange exchange, ErrorCode errorCode, String message) {
        log.warn(
                "网关鉴权拒绝: path={}, code={}, reason={}",
                exchange.getRequest().getPath().value(),
                errorCode.getCode(),
                message);
        return ApiJsonResponses.write(exchange.getResponse(), objectMapper, errorCode.getCode(), message);
    }
}
