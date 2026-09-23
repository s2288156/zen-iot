package com.zen.admin.config;

import com.zen.common.security.auth.RequireModule;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * OpenAPI 文档的鉴权、来源与错误响应声明。
 *
 * <p>{@code /v3/api-docs} 是前端、Apifox 与 SDK 生成器唯一的接口事实来源，所以文档必须把「要带 Bearer Token」这件事
 * 说出来。此前它不说（{@code securitySchemes} 与逐接口的 {@code security} 皆空）：身份判定落在
 * {@code AuthInterceptor} 与 {@link RequireModule} 上，springdoc 从 controller 签名里看不到。
 *
 * <p>刻意不声明全局 {@code security}：文档侧逐接口标注
 * {@link io.swagger.v3.oas.annotations.security.SecurityRequirement} 才能顺手把 login/refresh 表达成匿名入口，
 * 而「全局声明 + 逐接口覆盖」要依赖 springdoc 的覆盖语义，不如直接写「谁要鉴权」。
 *
 * <p>错误响应统一由 {@link #apiErrorResponses()} 补，而不是在方法上写 {@code @ApiResponses}：实测后者会让
 * springdoc 不再自动生成成功响应，连带 {@code ApiResponse*} 模型从 {@code components.schemas} 消失、文档里的
 * {@code $ref} 悬空。按「有请求体→400、有鉴权要求→401、有 {@link RequireModule}→403」推导，既绕开那个坑，
 * 又让文档口径直接来自驱动运行期行为的同一批注解，不会两处各写一份而漂移。悬空 {@code $ref} 由
 * {@code OpenApiDocContractTest} 断住。
 *
 * <p>{@link #namespacedOperationIds(String)} 给 operationId 加服务名前缀：springdoc 取的是裸方法名（{@code admin}、
 * {@code ecs}、{@code list}…），把 admin/ecs/wcs/rcs 的文档并进同一个 Apifox 项目或同一份 SDK 时必然撞名。
 */
@Configuration(proxyBeanMethods = false)
public class ZenAdminOpenApiConfiguration {

    /**
     * Bearer Token 安全方案名，controller 的 {@code @SecurityRequirement(name = ...)} 按此名引用。
     *
     * <p>名字写岔不会报错，只会让文档引用一个未定义的方案（Apifox 侧表现为「鉴权又没了」），属于静默失效，故收敛到单点。
     * 它是编译期常量，会被内联进注解，controller 因此在字节码上不依赖本 config 包。
     */
    public static final String BEARER_JWT_SCHEME = "bearerJwt";

    /**
     * 错误响应体与 {@code ApiResponse<Void>} 同构，即登出接口成功响应生成的那个组件。
     *
     * <p>组件名由 springdoc 按泛型展开推导，改登出接口的返回类型会让它消失；{@code OpenApiDocContractTest} 断言
     * 文档里每个 {@code $ref} 都能解析，正是为了把这种漂移变成构建失败而不是悬空引用。
     */
    private static final String ERROR_ENVELOPE_REF = "#/components/schemas/ApiResponseVoid";

    /** 各状态码的真实响应文案（{@code GlobalErrorCode} 默认消息；400 是字段错误的实际形态）。 */
    private static final Map<String, String> EXAMPLE_MESSAGES = Map.of(
            "400", "username must not be blank",
            "401", "未认证或登录已失效",
            "403", "无访问权限");

    private static final String DESCRIPTION = """
            认证与授权基线服务。响应统一包在 `ApiResponse` 里：`code` / `message` / `data`，\
            且 `code` 与 HTTP 状态码同源（401 的响应体里 `code` 就是 401）。

            - 免鉴权入口只有 `POST /auth/login` 与 `POST /auth/refresh`；带 `bearerJwt` 标注的接口必须\
            `Authorization: Bearer <accessToken>`。经网关访问时另有一份带 `/api/admin` 前缀的白名单，两处口径要同步改。
            - 用户不存在、密码错误、Token 缺失、签名不合法一律 401 同码同体，不透露账号是否存在。
            - `POST /auth/refresh` 是轮转式的：旧 refresh Token 当场进黑名单，重放即 401。\
            `POST /auth/logout` 同时撤销本次请求携带的 access Token 与入参里的 refresh Token。
            - 路径为裸路径（不含 `/api/admin`），该前缀只存在于网关。
            """;

    @Bean
    public OpenAPI adminOpenApi(
            @Value("${zen.api-docs.direct-url:http://localhost:28081}") String directUrl,
            @Value("${zen.api-docs.gateway-url:http://localhost:28080/api/admin}") String gatewayUrl,
            @Value("${zen.api-docs.version:0.0.1}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("zen-iot admin-service API")
                        .version(version)
                        .description(DESCRIPTION))
                // springdoc 默认按「当前请求」推导 server：谁在哪个地址访问就写成哪个，多环境下不稳定。
                // 这里显式列直连与经网关两条；工具（Apifox / Postman）仍以自身环境变量为准。
                .servers(List.of(
                        new Server().url(directUrl).description("直连 admin-service（裸路径）"),
                        new Server().url(gatewayUrl).description("经 gateway-service：StripPrefix=2 剥掉 /api/admin 前缀")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_JWT_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("登录响应的 data.accessToken；access 过期用 /auth/refresh 换新")));
    }

    @Bean
    public OperationCustomizer apiErrorResponses() {
        return (operation, handlerMethod) -> {
            if (operation.getRequestBody() != null) {
                addErrorResponse(operation, "400", "请求体校验失败（如 LoginRequest 的 @NotBlank），message 里带字段名");
            }
            boolean requiresIdentity =
                    operation.getSecurity() != null && !operation.getSecurity().isEmpty();
            addErrorResponse(
                    operation,
                    "401",
                    requiresIdentity
                            ? "未带 Authorization，或 Token 签名/过期校验失败、已被登出与轮转撤销"
                            : "凭证或 Token 校验失败：免鉴权入口也要过各自那一关，响应体与「未登录」同形");
            // RequireModule 的 @Target 只有 METHOD，所以只看方法，不必回看类
            if (handlerMethod.getMethodAnnotation(RequireModule.class) != null) {
                addErrorResponse(operation, "403", "身份有效，但 Token 的 modules 快照缺这个接口要求的模块——不是登录失效，重新拿 Token 也没用");
            }
            return operation;
        };
    }

    /** swagger 模型只在 {@code ApiResponses} 上有 add 方法，Operation 这边只有 get/set。 */
    private static void addErrorResponse(Operation operation, String code, String description) {
        ApiResponses responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
        responses.addApiResponse(code, errorResponse(code, description));
        operation.setResponses(responses);
    }

    /**
     * 错误响应的固定形状：{@code application/json} + 指向统一信封的 {@code $ref} + 一个能直接抄进断言的 example。
     *
     * <p>example 文案取自 {@code GlobalErrorCode} 与 {@code GlobalExceptionHandler} 的实际输出（本机逐条验过），
     * 不是编的示例值——Apifox 的用例断言与 mock 会直接依赖它，写错比不写更糟。
     */
    private static ApiResponse errorResponse(String code, String description) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("code", Integer.parseInt(code));
        example.put("message", EXAMPLE_MESSAGES.getOrDefault(code, ""));
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType(
                                "application/json",
                                new MediaType()
                                        .schema(new Schema<>().$ref(ERROR_ENVELOPE_REF))
                                        .example(example)));
    }

    /**
     * 给每个 operationId 加服务名前缀。
     *
     * <p>必须是 {@link GlobalOpenApiCustomizer} 而不是 {@link OperationCustomizer}：springdoc 在全局定制阶段才把
     * 裸方法名写成 operationId，逐接口阶段拿到的是 null——实测前缀在那种写法下根本不生效。
     */
    @Bean
    public GlobalOpenApiCustomizer namespacedOperationIds(@Value("${spring.application.name}") String applicationName) {
        return new NamespacedOperationIds(applicationName + "-");
    }

    /** 前缀幂等：已带前缀不再叠加；operationId 缺失时按「方法 + 路径」补一个确定值。 */
    private static final class NamespacedOperationIds implements GlobalOpenApiCustomizer, Ordered {

        private final String prefix;

        private NamespacedOperationIds(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void customise(OpenAPI openApi) {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths()
                    .forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
                        String fallback = method.name().toLowerCase() + path.replaceAll("[^A-Za-z0-9]", "");
                        String current = operation.getOperationId() == null ? fallback : operation.getOperationId();
                        if (!current.startsWith(prefix)) {
                            operation.setOperationId(prefix + current);
                        }
                    }));
        }

        /** 排在 springdoc 自己的 OperationIdCustomizer 之后，否则前缀会被它写回的裸方法名覆盖。 */
        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }
}
