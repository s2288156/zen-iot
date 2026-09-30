package com.zen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zen.admin.doc.DocError;
import com.zen.common.security.auth.RequireModule;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@code /v3/api-docs} 的文档契约：它是 Apifox、前端与 SDK 生成器唯一的事实来源，所以「文档说的」必须与
 * 「代码做的」一致。三组断言各对应一个实测踩过的失效模式：
 *
 * <ul>
 *   <li>在方法上写 {@code @ApiResponses} 会让 springdoc 不再自动生成成功响应，{@code ApiResponse*} 模型随之从
 *       {@code components.schemas} 消失、错误响应的 {@code $ref} 悬空——所以既断 200 还在，也断每个 {@code $ref} 可解析；
 *   <li>{@code securitySchemes} 与逐接口 {@code security} 缺失会把必须鉴权的接口说成匿名；
 *   <li>响应类型退化成 {@code *}{@code /}* 时工具拿不到 json 语义（本服务只产 json）。
 * </ul>
 *
 * <p>接口集合、403 路径集与 tag 目录三处清单不手写，而是从 {@link RequestMappingHandlerMapping} 注册的方法与代码
 * 注解（{@code @RequestMapping} / {@code @RequireModule} / {@code @SecurityRequirement} / {@code @Tag} /
 * {@code @DocError}）推导——
 * 与 springdoc builder 读同一批注解，新增 controller 不再需要改本测试。{@code @RequireModule} 的判定刻意与
 * {@code ZenAdminOpenApiConfiguration.apiErrorResponses()} 同口径（只看方法注解）。
 *
 * <p>取文档的方式是真 HTTP 而不是 MockMvc：Boot 4 把 {@code @AutoConfigureMockMvc} 拆进了
 * {@code spring-boot-webmvc-test}，不为一个测试给模块加依赖（{@code GatewayRoutingTest} 同一取向）。
 * 完整上下文要 JPA + 数据源，所以与 {@code JpaAuditingIntegrationTest} 同标 integration。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.nacos.discovery.enabled=false")
@Tag("integration") // 依赖本机 MySQL/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class OpenApiDocContractTest {

    private static final String ERROR_ENVELOPE = "ApiResponseVoid";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    /** {@code GlobalErrorCode.BAD_REQUEST} 的默认消息，即请求体没有 @NotBlank 时 400 的实际文案。 */
    private static final String BAD_REQUEST_MESSAGE = "请求参数有误";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    /**
     * 期望清单的推导源：springdoc 与本测试读的注解都挂在它注册的处理器方法上。字段注入是测试类的既有惯例。
     *
     * <p>必须按名注入：actuator 会再注册一个 {@code RequestMappingHandlerMapping} 子类
     * （{@code controllerEndpointHandlerMapping}），按类型注入会因两个候选而失败。
     */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void successResponsesSurviveAndEveryRefResolves() throws Exception {
        JsonNode doc = loadDoc();

        // 双向全等：注册了的接口必须都在文档里，文档里也不许多出没注册的操作
        Map<String, JsonNode> operations = operations(doc);
        assertThat(operations.keySet()).as("文档接口集合与处理器映射不一致").isEqualTo(expectedKeys());
        operations.forEach(
                (key, operation) -> assertThat(operation.at("/responses/200").isMissingNode())
                        .as("%s 的 200 响应必须存在（被方法上的 @ApiResponses 顶掉就是回归）", key)
                        .isFalse());
        assertThat(danglingRefs(doc))
                .as("文档里悬空的 $ref：组件名由 springdoc 推导，改返回类型就会漂移")
                .isEmpty();
    }

    @Test
    void authRequirementsMatchTheRuntimeWhitelist() throws Exception {
        JsonNode doc = loadDoc();

        assertThat(doc.at("/components/securitySchemes/bearerJwt/type").asText())
                .isEqualTo("http");
        assertThat(doc.at("/components/securitySchemes/bearerJwt/scheme").asText())
                .isEqualTo("bearer");
        assertThat(doc.at("/components/securitySchemes/bearerJwt/bearerFormat").asText())
                .isEqualTo("JWT");

        // 匿名/需鉴权不再手写名单：逐接口按代码上的 @SecurityRequirement 派生（与运行时白名单同口径的
        // 「只有 login/refresh 匿名」由注解位置本身表达，文档与注解不一致即失败）
        Map<String, JsonNode> operations = operations(doc);
        assertThat(operations.keySet()).isEqualTo(expectedKeys());
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            Set<String> expectedSchemes = securitySchemes(handler);
            handlerKeys(mapping).stream()
                    .filter(key -> !isDocInfrastructure(key.substring(key.indexOf(' ') + 1)))
                    .forEach(key -> {
                        assertThat(operations).containsKey(key);
                        JsonNode security = operations.get(key).path("security");
                        if (expectedSchemes.isEmpty()) {
                            assertThat(security.isMissingNode())
                                    .as("%s 应匿名（代码未标 @SecurityRequirement）", key)
                                    .isTrue();
                        } else {
                            assertThat(documentedSchemes(security))
                                    .as("%s 的 security 应与 @SecurityRequirement 一致", key)
                                    .isEqualTo(expectedSchemes);
                        }
                    });
        });
    }

    @Test
    void errorResponsesAndJsonMediaTypesAreDocumented() throws Exception {
        JsonNode doc = loadDoc();
        Map<String, JsonNode> operations = operations(doc);

        // 400 只属于有请求体的接口；401 所有接口都有；403 只属于方法上带 @RequireModule 的接口
        // （派生口径与 ZenAdminOpenApiConfiguration.apiErrorResponses() 逐字一致：只看方法注解）
        Set<String> moduleGated = moduleGatedKeys();
        Set<String> forbiddenExpected = new TreeSet<>(moduleGated);
        forbiddenExpected.addAll(keysDeclaringStatus(403));
        operations.forEach((key, operation) -> {
            assertThat(operation.at("/responses/401").isMissingNode()).as(key).isFalse();
            boolean hasBody = !operation.path("requestBody").isMissingNode();
            assertThat(!operation.at("/responses/400").isMissingNode()).as(key).isEqualTo(hasBody);
            assertThat(!operation.at("/responses/403").isMissingNode())
                    .as(key)
                    .isEqualTo(forbiddenExpected.contains(key));
            for (Map.Entry<String, JsonNode> response :
                    operation.at("/responses").properties()) {
                assertThat(response.getValue()
                                .at("/content")
                                .path("application/json")
                                .isMissingNode())
                        .as("%s 的 %s 响应应为 application/json", key, response.getKey())
                        .isFalse();
            }
        });

        // @DocError 声明的错误码必须真进文档，且 example.message 与实际输出逐字一致。
        // 登录缺 403/429 就是这条断言要拦的形态：README 与代码都有，文档没有。
        for (DeclaredError declared : declaredErrors()) {
            JsonNode example = operations
                    .get(declared.key())
                    .at("/responses/" + declared.status() + "/content")
                    .path("application/json")
                    .path("example");
            assertThat(example.isMissingNode())
                    .as("%s 缺 %d 响应", declared.key(), declared.status())
                    .isFalse();
            assertThat(example.at("/message").asText())
                    .as("%s 的 %d 示例文案", declared.key(), declared.status())
                    .isEqualTo(declared.message());
        }
        assertThat(operations).containsKeys("POST /auth/login");
        assertThat(operations.get("POST /auth/login").at("/responses").has("429"))
                .isTrue();

        // 信封模型必须在，否则上面那些 $ref 全是悬空的
        assertThat(doc.at("/components/schemas/" + ERROR_ENVELOPE).isMissingNode())
                .isFalse();
    }

    @Test
    void docIdentifiesItselfAndBothServers() throws Exception {
        JsonNode doc = loadDoc();

        assertThat(doc.at("/info/title").asText()).isEqualTo("zen-iot admin-service API");
        assertThat(doc.at("/info/version").asText()).isNotBlank();
        assertThat(doc.at("/servers").findValuesAsText("url"))
                .containsExactly("http://localhost:28081", "http://localhost:28080/api/admin");
    }

    @Test
    void operationIdsAreNamespacedAndTagsStayReadable() throws Exception {
        JsonNode doc = loadDoc();

        List<String> operationIds = operations(doc).values().stream()
                .map(operation -> operation.at("/operationId").asText())
                .toList();
        assertThat(operationIds).allMatch(id -> id.startsWith("admin-service-")).doesNotHaveDuplicates();

        // 目录名来自 controller 类上的 @Tag：auth-controller / demo-controller 这种派生名在 Apifox 树里读不出含义。
        // 期望集合 = 处理器 bean 类上实际声明的 @Tag 名，新增/改名 controller 只需要同步注解本身
        Set<String> expectedTags = tagCatalogue();
        assertThat(doc.at("/tags").findValuesAsText("name")).containsExactlyInAnyOrderElementsOf(expectedTags);
        operations(doc).forEach((key, operation) -> {
            List<String> tags = new ArrayList<>();
            operation.path("tags").forEach(tag -> tags.add(tag.asText()));
            assertThat(tags).as(key).hasSize(1);
            assertThat(expectedTags).as(key).contains(tags.getFirst());
        });
    }

    @Test
    void fieldDocsAndErrorExamplesAreRealAndLeakNothing() throws Exception {
        JsonNode doc = loadDoc();

        JsonNode loginRequest = doc.at("/components/schemas/LoginRequest/properties");
        assertThat(loginRequest.at("/password/format").asText())
                .as("format=password 才会在客户端里遮罩")
                .isEqualTo("password");
        assertThat(loginRequest.at("/username/description").asText()).isNotBlank();
        assertThat(loginRequest.at("/username/example").asText()).isEqualTo("admin");
        assertThat(doc.at("/components/schemas/RefreshRequest/properties/refreshToken/description")
                        .asText())
                .isNotBlank();

        // 错误 example 必须与 GlobalErrorCode 的实际文案一致，断言抄错比不写更糟
        JsonNode forbidden = operations(doc)
                .get("GET /demo/admin")
                .at("/responses/403/content")
                .path("application/json")
                .path("example");
        assertThat(forbidden.at("/code").asInt()).isEqualTo(403);
        assertThat(forbidden.at("/message").asText()).isEqualTo("无访问权限");
        JsonNode unauthorized = operations(doc)
                .get("POST /auth/logout")
                .at("/responses/401/content")
                .path("application/json")
                .path("example");
        assertThat(unauthorized.at("/message").asText()).isEqualTo("未认证或登录已失效");

        // 400 的示例 message 必须落在该接口自己的字段上。改动前是全服务共用一串 username，
        // 表现就是 refresh/logout 在文档里说一个自己根本没有的字段；期望值不写清单，
        // 从该接口 requestBody 指向的 schema 的 properties/required 推导。
        List<String> blankExamples = new ArrayList<>();
        operations(doc).forEach((key, operation) -> {
            // 状态码挂在 responses 下，直接对 operation 调 has("400") 恒为 false
            if (!operation.path("responses").has("400")) {
                return;
            }
            String message = operation
                    .at("/responses/400/content")
                    .path("application/json")
                    .path("example")
                    .path("message")
                    .asText();
            assertThat(message).as("%s 的 400 示例文案", key).isIn(acceptableBlankMessages(operation, doc));
            blankExamples.add(message);
        });
        // 只断「字段属于该接口」堵不住另一种塌法：@NotBlank 全取不到时 12 个接口集体退回
        // 「请求参数有误」，逐条看依然成立。按字段推导必然留下多个不同值，故加指纹断言。
        assertThat(blankExamples.stream().distinct().count())
                .as("400 示例文案去重数：全体同一条就说明没按字段推导")
                .isGreaterThan(1);

        // /v3/api-docs 匿名可读：开发口令不能出现在文档里
        assertThat(doc.toString()).doesNotContain("Admin@123").doesNotContain("Demo@123");
    }

    /**
     * 该接口 400 示例可接受的文案集合。
     *
     * <p>改动前是全服务共用一串 {@code username}，于是 refresh/logout 在文档里说一个自己根本没有的字段；
     * 这条断言拦的就是那种形态：字段名必须出自该接口请求体模型自己的 {@code required ∩ properties}。
     * 同时允许「请求体确实没有 @NotBlank」的兜底——{@code UserStatusRequest} 只有 {@code @NotNull}、
     * {@code UserUpdateRequest} 全部可空、覆盖式授权的请求体是字符串数组，那三种 Bean Validation
     * 给不出字段错误，实际输出就是 {@code BAD_REQUEST} 的默认文案。
     */
    private static List<String> acceptableBlankMessages(JsonNode operation, JsonNode doc) {
        List<String> accepted = new ArrayList<>();
        accepted.add(BAD_REQUEST_MESSAGE);
        String ref = operation
                .at("/requestBody/content")
                .path("application/json")
                .path("schema")
                .path("$ref")
                .asText("");
        if (ref.isEmpty()) {
            return accepted;
        }
        String modelName = ref.substring(ref.lastIndexOf('/') + 1);
        JsonNode schema = doc.at("/components/schemas/" + modelName);
        JsonNode properties = schema.path("properties");
        for (JsonNode field : schema.path("required")) {
            if (properties.has(field.asText())) {
                accepted.add(field.asText() + " must not be blank");
            }
        }
        return accepted;
    }

    private JsonNode loadDoc() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    /** 按 {@code METHOD path} 收集全部操作，便于断言时一眼看出是哪个接口挂了。 */
    private Map<String, JsonNode> operations(JsonNode doc) {
        Map<String, JsonNode> byKey = new TreeMap<>();
        for (Map.Entry<String, JsonNode> path : doc.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> method : path.getValue().properties()) {
                byKey.put(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue());
            }
        }
        return byKey;
    }

    /** 期望接口集合：处理器映射里注册的 (方法, 路径)，剔除 springdoc 基础设施端点。 */
    private Set<String> expectedKeys() {
        Set<String> keys = new TreeSet<>();
        handlerMapping.getHandlerMethods().keySet().forEach(mapping -> {
            handlerKeys(mapping).stream()
                    .filter(key -> !isDocInfrastructure(key.substring(key.indexOf(' ') + 1)))
                    .forEach(keys::add);
        });
        return keys;
    }

    /**
     * 非业务清单的框架端点：{@code /v3/api-docs*} 与 {@code /swagger-ui*} 是文档工具自身，
     * {@code /error} 由 {@code BasicErrorController} 提供、springdoc 默认（{@code remove-broken-refs} 之外的
     * {@code springdoc.default-produces-media-type} 路径过滤）不写进文档。
     */
    private static boolean isDocInfrastructure(String path) {
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/error");
    }

    /** 与 {@link #operations(JsonNode)} 同格式的 {@code METHOD path} 键；多方法/多路径逐一展开，缺省按 ANY 展开。 */
    private static List<String> handlerKeys(RequestMappingInfo mapping) {
        // Boot 4 默认 PathPatternParser，旧 getPatternsCondition() 已废弃且恒为 null
        var patterns = mapping.getPathPatternsCondition();
        List<String> paths = patterns == null
                ? List.of("/")
                : patterns.getPatternValues().stream().sorted().toList();
        var methodsCondition = mapping.getMethodsCondition();
        List<String> methods = methodsCondition.getMethods().isEmpty()
                ? List.of("ANY")
                : methodsCondition.getMethods().stream()
                        .map(Enum::name)
                        .sorted()
                        .toList();
        List<String> keys = new ArrayList<>();
        methods.forEach(method -> paths.forEach(path -> keys.add(method + " " + path)));
        return keys;
    }

    /** 方法 + 类两级 {@code @SecurityRequirement} 的并集；本项目无混用，与 springdoc 逐接口展开结果一致。 */
    private static Set<String> securitySchemes(HandlerMethod handler) {
        return Stream.concat(
                        Stream.of(handler.getMethod().getAnnotationsByType(SecurityRequirement.class)),
                        Stream.of(handler.getBeanType().getAnnotationsByType(SecurityRequirement.class)))
                .map(SecurityRequirement::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** 文档里某操作的 security 引用的方案名集合（OR 列表取并集；本服务每接口只声明单方案）。 */
    private static Set<String> documentedSchemes(JsonNode security) {
        // security 形如 [{"bearerJwt": []}]：scheme 名是 requirement 对象的键，
        // 值只是 scope 列表（本项目恒为空），不能拿去当名字遍历
        Set<String> names = new TreeSet<>();
        security.forEach(requirement -> requirement.fieldNames().forEachRemaining(names::add));
        return names;
    }

    /** 带 403 的接口集合：只看方法上的 {@code @RequireModule}，与文档 builder 同一口径。 */
    private Set<String> moduleGatedKeys() {
        Set<String> gated = new TreeSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (handler.getMethodAnnotation(RequireModule.class) != null) {
                gated.addAll(handlerKeys(mapping));
            }
        });
        return gated;
    }

    /** tag 目录：处理器 bean 类上声明的 {@code @Tag} 名去重集。 */
    /** 方法上声明了某个错误码的全部接口键（{@code METHOD path}）。 */
    private Set<String> keysDeclaringStatus(int status) {
        Set<String> keys = new TreeSet<>();
        declaredErrors().stream()
                .filter(declared -> declared.status() == status)
                .map(DeclaredError::key)
                .forEach(keys::add);
        return keys;
    }

    /** 处理器方法上的 {@code @DocError} 展开成 (接口键, 状态码, 期望文案)；新增声明自动进断言，不手写清单。 */
    private List<DeclaredError> declaredErrors() {
        List<DeclaredError> declared = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            // 与 ZenAdminOpenApiConfiguration 读同一份声明，两边不会各说一遍
            DocError[] annotations = handler.getMethod().getAnnotationsByType(DocError.class);
            for (String key : handlerKeys(mapping)) {
                for (DocError annotation : annotations) {
                    declared.add(new DeclaredError(key, annotation.status(), annotation.message()));
                }
            }
        });
        return declared;
    }

    /** 一条错误声明落在哪个接口上。 */
    private record DeclaredError(String key, int status, String message) {}

    private Set<String> tagCatalogue() {
        Set<String> tags = new LinkedHashSet<>();
        handlerMapping.getHandlerMethods().values().forEach(handler -> {
            // 全限定名：简单名 Tag 已被类上的 JUnit @Tag 占用
            var tag = AnnotatedElementUtils.findMergedAnnotation(
                    handler.getBeanType(), io.swagger.v3.oas.annotations.tags.Tag.class);
            if (tag != null) {
                tags.add(tag.name());
            }
        });
        return tags;
    }

    /** 递归收集无法解析的 {@code $ref}：指向的组件不在 {@code components/schemas} 里就被列出来。 */
    private List<String> danglingRefs(JsonNode doc) {
        List<String> dangling = new ArrayList<>();
        collectRefs(doc, doc, dangling);
        return dangling;
    }

    private void collectRefs(JsonNode doc, JsonNode node, List<String> sink) {
        if (node.isContainerNode()) {
            node.forEach(child -> collectRefs(doc, child, sink));
            return;
        }
        if (!node.isTextual() || !node.asText().startsWith(SCHEMA_REF_PREFIX)) {
            return;
        }
        String name = node.asText().substring(SCHEMA_REF_PREFIX.length());
        if (doc.at("/components/schemas/" + name).isMissingNode()) {
            sink.add(node.asText());
        }
    }
}
