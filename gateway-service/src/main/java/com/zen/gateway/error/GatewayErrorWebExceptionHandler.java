package com.zen.gateway.error;

import com.zen.common.core.exception.BusinessException;
import com.zen.common.core.exception.GlobalErrorCode;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * 网关层统一异常响应：承接路由级错误（无匹配路由 404、下游无可用实例、Redis 故障 500）。
 *
 * <p>鉴权失败不经此处——{@code JwtAuthGlobalFilter} 直接写出响应体，不依赖框架的错误传播路径（行为可测，且不必和 Boot 的
 * 错误处理器抢顺序）。
 *
 * <p>{@link Order} 必须早于 Boot 的 {@code DefaultErrorWebExceptionHandler}（order = -1），否则响应会变成 whitelabel 页面。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    /** 与 Servlet 侧一致的中文错误说明；不透出框架原始 reason，避免泄露内部路径与实现细节。 */
    private static final Map<Integer, String> LOCALIZED_MESSAGES = Arrays.stream(GlobalErrorCode.values())
            .collect(Collectors.toUnmodifiableMap(GlobalErrorCode::getCode, GlobalErrorCode::getMessage));

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            // 响应已经开始写出，状态码与 body 都改不了，只能把错误交回上层
            return Mono.error(ex);
        }
        String path = exchange.getRequest().getURI().getPath();
        // 已识别的异常按自身语义透出：下游无可用实例是 503 而不是 500（SCG 抛的 NotFoundException 带着
        // SERVICE_UNAVAILABLE），把它压成 500 会让调用方与 Phase 10 的重试/探针判断都失真。
        boolean recognized = ex instanceof BusinessException || ex instanceof ResponseStatusException;
        int code = recognized ? codeFor(ex) : GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode();
        String message =
                ex instanceof BusinessException businessException ? businessException.getMessage() : messageFor(code);
        if (code >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("网关请求失败: path={}, code={}", path, code, ex);
        } else {
            log.warn("网关请求失败: path={}, code={}, message={}", path, code, ex.getMessage());
        }
        return ApiJsonResponses.write(response, objectMapper, code, message);
    }

    private int codeFor(Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            return businessException.getErrorCode().getCode();
        }
        if (ex instanceof ResponseStatusException statusException) {
            return statusException.getStatusCode().value();
        }
        return GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode();
    }

    private String messageFor(int code) {
        String localized = LOCALIZED_MESSAGES.get(code);
        if (localized != null) {
            return localized;
        }
        HttpStatus status = HttpStatus.resolve(code);
        return status != null ? status.getReasonPhrase() : GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage();
    }
}
