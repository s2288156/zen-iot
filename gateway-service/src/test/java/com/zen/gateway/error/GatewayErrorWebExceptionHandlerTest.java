package com.zen.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.common.security.error.BusinessException;
import com.zen.common.security.error.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

/** {@link GatewayErrorWebExceptionHandler} 的单元测试：只关心 code↔状态码↔响应体三者的对应关系，不需要 Web 服务器。 */
class GatewayErrorWebExceptionHandlerTest {

    private final GatewayErrorWebExceptionHandler handler =
            new GatewayErrorWebExceptionHandler(JsonMapper.builder().build());

    @Test
    void businessExceptionKeepsItsOwnCodeAndMessage() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handler.handle(exchange, new BusinessException(GlobalErrorCode.FORBIDDEN))
                        .then(Mono.defer(exchange.getResponse()::getBodyAsString)))
                .expectNextMatches(json -> json.contains("\"code\":403") && json.contains("无访问权限"))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** 框架异常只透出状态码对应的固定文案，不透出 reason，避免泄露内部路径。 */
    @Test
    void responseStatusExceptionIsMappedToUnifiedNotFoundWithoutLeakingReason() {
        MockServerWebExchange exchange = exchange();
        ResponseStatusException notFound = new ResponseStatusException(HttpStatus.NOT_FOUND, "No static resource /x/y");

        StepVerifier.create(
                        handler.handle(exchange, notFound).then(Mono.defer(exchange.getResponse()::getBodyAsString)))
                .expectNextMatches(json -> json.contains("\"code\":404") && !json.contains("No static resource"))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unexpectedExceptionBecomesBare500() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handler.handle(exchange, new IllegalStateException("内部细节"))
                        .then(Mono.defer(exchange.getResponse()::getBodyAsString)))
                .expectNextMatches(json -> json.contains("\"code\":500") && !json.contains("内部细节"))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** 下游无可用实例：SCG 抛的 NotFoundException 带 503，必须原样透出，压成 500 会让重试与探针判断失真。 */
    @Test
    void serviceUnavailableKeepsItsOwnStatus() {
        MockServerWebExchange exchange = exchange();
        ResponseStatusException unavailable = org.springframework.cloud.gateway.support.NotFoundException.create(
                false, "Unable to find instance for wcs-service");

        StepVerifier.create(
                        handler.handle(exchange, unavailable).then(Mono.defer(exchange.getResponse()::getBodyAsString)))
                .expectNextMatches(json -> json.contains("\"code\":503") && json.contains("服务暂不可用"))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** 响应已提交时不能再改状态码，必须把原错误继续往上抛。 */
    @Test
    void committedResponsePropagatesTheOriginalError() {
        MockServerWebExchange exchange = exchange();
        exchange.getResponse().setComplete();

        StepVerifier.create(handler.handle(exchange, new BusinessException(GlobalErrorCode.UNAUTHORIZED)))
                .verifyError(BusinessException.class);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/demo/admin"));
    }
}
