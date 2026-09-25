package com.zen.admin.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zen.admin.entity.OperationLogEntity;
import com.zen.admin.repository.OperationLogRepository;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import com.zen.common.security.auth.UserContext;
import com.zen.common.security.auth.UserPrincipal;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link OperationLogInterceptor} 的纯单元测试：直接调用 {@code afterCompletion}，不起 Web 上下文。
 *
 * <p>钉死三件事：成功与失败（{@code ex} 非空）两条路径都以当时的响应状态码落库；落库炸了只记
 * ERROR 不外抛（决策 (c) 的 best-effort）；缺注解、非 Controller 方法、缺 {@code @RequireModule}、
 * 无登录身份四种装配外场景一律跳过落库而非写半截数据。
 */
@ExtendWith(MockitoExtension.class)
class OperationLogInterceptorTest {

    @Mock
    private OperationLogRepository operationLogRepository;

    private OperationLogInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new OperationLogInterceptor(operationLogRepository);
        request = new MockHttpServletRequest("POST", "/users/42");
        response = new MockHttpServletResponse();
        UserContext.set(new UserPrincipal(
                1L,
                "admin",
                List.of("admin"),
                List.of("ADMIN"),
                "jti-access",
                Instant.now().plus(Duration.ofMinutes(10))));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void successPathPersistsFullAuditRow() throws Exception {
        response.setStatus(200);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "42"));

        interceptor.afterCompletion(request, response, handler("update"), null);

        OperationLogEntity row = savedRow();
        assertThat(row.getOperator()).isEqualTo("admin");
        // module 不手填：由方法上的 @RequireModule 推导为模块码小写值
        assertThat(row.getModule()).isEqualTo(ModuleCode.ADMIN.getCode());
        assertThat(row.getAction()).isEqualTo(OperationAction.UPDATE.name());
        assertThat(row.getTargetType()).isEqualTo(OperationTargetType.USER.name());
        assertThat(row.getTargetId()).isEqualTo("42");
        assertThat(row.getResultCode()).isEqualTo(200);
        assertThat(row.getOpTime()).isNotNull();
    }

    @Test
    void failurePathWithExceptionStillRecordsResponseStatus() throws Exception {
        // GlobalExceptionHandler 已把 BusinessException 解析为响应：ex 非空路径同样只认状态码
        response.setStatus(500);

        interceptor.afterCompletion(request, response, handler("update"), new IllegalStateException("boom"));

        OperationLogEntity row = savedRow();
        assertThat(row.getResultCode()).isEqualTo(500);
        // 创建类接口无路径变量：target_id 自然留空，落库即定不回填
        assertThat(row.getTargetId()).isNull();
    }

    @Test
    void persistenceFailureIsSwallowed() throws Exception {
        response.setStatus(200);
        when(operationLogRepository.save(any(OperationLogEntity.class)))
                .thenThrow(new DataIntegrityViolationException("audit down"));

        assertThatCode(() -> interceptor.afterCompletion(request, response, handler("update"), null))
                .doesNotThrowAnyException();
    }

    @Test
    void handlerWithoutAnnotationIsIgnored() throws Exception {
        interceptor.afterCompletion(request, response, handler("readWithoutAnnotation"), null);

        verifyNoInteractions(operationLogRepository);
    }

    @Test
    void nonHandlerMethodTargetIsIgnored() throws Exception {
        // 静态资源等最终落到 ResourceHttpRequestHandler 上，不是 HandlerMethod
        interceptor.afterCompletion(request, response, new Object(), null);

        verifyNoInteractions(operationLogRepository);
    }

    @Test
    void annotationWithoutRequireModuleSkipsWrite() throws Exception {
        // module 列无从推导属于装配错误：记 ERROR 跳过，绝不写猜测值
        interceptor.afterCompletion(request, response, handler("updateWithoutRequireModule"), null);

        verifyNoInteractions(operationLogRepository);
    }

    @Test
    void missingUserContextSkipsWrite() throws Exception {
        UserContext.clear();

        interceptor.afterCompletion(request, response, handler("update"), null);

        verifyNoInteractions(operationLogRepository);
    }

    // ---------- fixtures ----------

    private OperationLogEntity savedRow() {
        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static HandlerMethod handler(String methodName) throws Exception {
        Method method = SampleController.class.getMethod(methodName);
        return new HandlerMethod(new SampleController(), method);
    }

    /** 注解组合的最小样例：与真实 controller 一样只声明注解，方法体不会被执行。 */
    static class SampleController {

        @RequireModule(ModuleCode.ADMIN)
        @OperationLog(action = OperationAction.UPDATE, targetType = OperationTargetType.USER)
        public void update() {}

        @RequireModule(ModuleCode.ADMIN)
        public void readWithoutAnnotation() {}

        @OperationLog(action = OperationAction.UPDATE, targetType = OperationTargetType.USER)
        public void updateWithoutRequireModule() {}
    }
}
