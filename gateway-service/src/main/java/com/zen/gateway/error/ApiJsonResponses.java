package com.zen.gateway.error;

import com.zen.common.security.api.ApiResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 网关侧的统一 JSON 响应体写出。
 *
 * <p>这里的 {@code ObjectMapper} 是 {@code tools.jackson}（Jackson 3）：Boot 4 默认换到 Jackson 3，自动装配出来的 Bean
 * 就是 {@code tools.jackson.databind.ObjectMapper}，注入 {@code com.fasterxml.jackson.databind.ObjectMapper} 会拿不到
 * Bean（实测 {@code No qualifying bean of type ...}）。Jackson 2 仍在类路径上，只为 jjwt-jackson 服务。
 *
 * <p>与 {@code common-core} 的 {@code GlobalExceptionHandler} 同一形状（{@link ApiResponse}）、同一 code↔HTTP 映射规则，
 * 但那份处理器带 {@code @ConditionalOnWebApplication(SERVLET)}，网关用不了，故鉴权失败与网关级错误都由本类落笔。
 */
@Slf4j
public final class ApiJsonResponses {

    /** 序列化失败时的兜底响应体： {@link ApiResponse} 只有 int/String 字段，正常路径不会走到这里。 */
    private static final byte[] FALLBACK_BODY = "{\"code\":500,\"message\":\"系统异常\"}".getBytes(StandardCharsets.UTF_8);

    private ApiJsonResponses() {}

    /**
     * @param code 业务/HTTP 语义码，写进响应体 {@code code} 字段
     * @param message 面向调用方的说明
     */
    public static Mono<Void> write(ServerHttpResponse response, ObjectMapper objectMapper, int code, String message) {
        response.setStatusCode(statusFor(code));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.failure(code, message));
        } catch (JacksonException e) {
            log.error("统一响应序列化失败，回落到固定 500 响应体", e);
            body = FALLBACK_BODY;
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    /** code 是合法 HTTP 错误码时状态码与之一致，否则回落 200 由 body 的 code 表达错误（与 Servlet 侧同规则）。 */
    public static HttpStatus statusFor(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null && status.isError() ? status : HttpStatus.OK;
    }
}
