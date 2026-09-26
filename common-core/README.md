# common-core

Servlet 业务服务的公共模块：统一异常出口、鉴权拦截的自动装配、分页与审计基类。消费方是 `admin-service` 与 `ecs-service`；反应式网关不依赖本模块，只用拆出去的 `common-security`。

本模块**不提供 HTTP 接口**，对外表面是三样：自动装配的 Bean、公共类型（`BaseEntity` / `PageQuery` / `PageResult` / `GlobalExceptionHandler`）、配置键。装配入口是 `common-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，消费方不改启动类、不需 `@Import`。

本文只写事实与结论。**为什么这样选、否掉了什么、踩过什么坑**见 [`docs/modules/common-core.md`](../docs/modules/common-core.md)；跨模块必须同时成立的约定（密钥一致、两套白名单、可信身份头）见 [`docs/architecture.md`](../docs/architecture.md)。

## 能力与接口口径

配置键及其缺省行为（`zen.security.*` 的属性类是 `common-security` 的 `SecurityProperties`、由本模块的自动配置绑定；`zen.jpa.auditing.enabled` 由本模块自己读取）：

| 配置键                        | 默认   | 口径                                                                                         |
| ----------------------------- | ------ | -------------------------------------------------------------------------------------------- |
| `zen.security.enabled`        | `true` | `false` 时两个拦截器都不注册，鉴权与模块校验一并跳过；只供本地与测试                         |
| `zen.security.context-source` | `jwt`  | `jwt` 自行验签 `Authorization: Bearer`；`gateway-header` 只读 `TrustedHeaders` 的 4 个透传头 |
| `zen.security.whitelist`      | **空** | Ant 风格免鉴权路径，公开接口必须逐服务显式列出                                               |
| `zen.jpa.auditing.enabled`    | `true` | 关掉则 `@EnableJpaAuditing` 不生效，审计列不再自动填充                                       |

`context-source=jwt` 还要求 `zen.jwt.secret`（UTF-8 ≥ 32 字节），该键由 `common-security` 消费。

异常到响应的映射由 `GlobalExceptionHandler` 独占，业务服务不再各写一套：

| 异常                                                             | 结果                                         |
| ---------------------------------------------------------------- | -------------------------------------------- |
| `BusinessException`                                              | 携带的 `errorCode`，HTTP 按状态码口径对齐    |
| `BindException`（Spring 7 含 `MethodArgumentNotValidException`） | 400，`<field> <message>`，只取第一条字段错误 |
| `ConstraintViolationException`                                   | 400，只取第一条 violation                    |
| `MissingRequestValueException`                                   | 400（缺 param / header / cookie）            |
| `HttpRequestMethodNotSupportedException`                         | 405                                          |
| `NoResourceFoundException`、`NoHandlerFoundException`            | 404                                          |
| 其余 `Exception`                                                 | 500，不回显异常信息                          |

- **HTTP 状态只在 code 能解析成 4xx/5xx 时对齐**；业务码段（如 1xxx）回 HTTP 200，错误由响应体 `code` 表达。见 `GlobalExceptionHandler#build`。
- 白名单是**路径级排除**，同时作用于两个拦截器：命中的路径既不建立身份，也不校验 `@RequireModule`。
- `ModuleAuthInterceptor` 对非 `HandlerMethod`（静态资源、`/error`）直接放行；注解只支持方法级，缺注解即不校验模块、仍要求已登录。
- 分页入参 1-based：`pageNum` 默认 1、`pageSize` 默认 10 且上限 `PageQuery.MAX_PAGE_SIZE = 200`；`orderBy` 取自客户端，**属性白名单校验是调用方的责任**。
- `BaseEntity` 提供 `id`（IDENTITY）与 `create_time` / `update_time` / `creator` / `updater`，`create_time` 标了 `updatable = false`。

## 关键实现

- **两个 Web 侧自动配置限定 `@ConditionalOnWebApplication(type = SERVLET)`**，反应式应用即使误引也会被条件跳过，不会漏进 Servlet 拦截器；`ZenJpaAuditingAutoConfiguration` 不限定 Web 类型，按 `@ConditionalOnClass` 与 `zen.jpa.auditing.enabled` 生效。
- **六个可接管点都是 `@ConditionalOnMissingBean`**：`GlobalExceptionHandler`、`TokenRevocationChecker`、`UserContextResolver`、`AuthInterceptor`、`ModuleAuthInterceptor`、`AuditorAware`。消费方定义同类型 Bean 即覆盖，不需要 `@Primary`；注册拦截器的 `zenSecurityWebMvcConfigurer` 不在此列——消费方的 `WebMvcConfigurer` 与之并存而非覆盖。
- **拦截器注册顺序即执行顺序**：`AuthInterceptor` 先写 `UserContext`，`ModuleAuthInterceptor` 后读，见 `ZenSecurityAutoConfiguration#zenSecurityWebMvcConfigurer`。
- **缺 `JwtTokenVerifier` 时启动即失败**：`ZenSecurityAutoConfiguration#jwtUserContextResolver` 抛 `IllegalStateException`，文案直接给出两条出路（补 `zen.jwt.secret` 或改 `context-source=gateway-header`）。
- **`TokenRevocationChecker` 的缺省实现是 `disabled()`**：消费方不自定义时撤销检查恒为「未撤销」。admin 侧的 `RedisTokenRevocationChecker` 因该退让机制而生效。
- **透传模式的 principal 不完整**：`GatewayHeaderUserContextResolver` 的 `jti` 与 `expiresAt` 为 null，`UserPrincipal#remainingTtl()` 随之返回 null；缺或非数字 `X-User-Id` 即无身份（401），`roles` / `modules` 按 CSV trim 解析。
- **MDC 只加 `userId`**，由 `AuthInterceptor` 写入、`afterCompletion` 连同 `UserContext` 一起清理；`traceId` 来自 micrometer-tracing，不是本模块写的。
- **链路追踪只做 A 层**：`spring-boot-micrometer-tracing-opentelemetry` + `micrometer-tracing-bridge-otel`，traceId 进日志可串联，span 不上报。
- **两个 Bean 之外的公共类型不可漏**：`PageResult` 是不可变普通类（私有构造 + 只读访问器），工厂只有 `of(Page)` / `of(Page, mappedList)` / `empty()`，`pageNum` 由 `page.getNumber() + 1` 还原成 1-based。
- **构建侧**：`common-core/build.gradle.kts` 刻意挂了 `org.springframework.boot` 插件只为拿托管版本，同时关掉 `bootJar` 并把 `jar` 的 classifier 复位成默认产物名。

## 架构约束

`./gradlew :common-core:architectureTest`，规则清单见 `common-core/src/test/java/com/zen/common/core/ArchitectureTest.java`：

- `commonCoreNeverDependsOnBusinessServices`：不得依赖 `com.zen.{admin,ecs,gateway,rcs,wcs}`。**该包名列表逐模块硬编码**，新建模块必须在这里补一条。
- `packageSlicesAreFreeOfCycles`：`com.zen.common.core.(*)..` 包切片无循环依赖。
- `neverUsesDeprecatedSpringNullabilityAnnotations`：可空性只用 `org.jspecify.annotations.*`。
- 编码卫生三条：禁字段注入、禁标准流输出、禁 `new Date()`（`java.util.Date` 本身仍合法，jjwt 边界要用）。

不启动 Spring 上下文，只导入主代码，因此无中间件也能跑。

## 主要依赖

`api` 透传 `common-security` 与 `spring-boot-starter-web` / `-validation` / `-data-jpa`（类型出现在公共签名里）；`implementation` `spring-boot-micrometer-tracing-opentelemetry`、`runtimeOnly` `micrometer-tracing-bridge-otel`；Lombok `compileOnly` + `annotationProcessor`。

追踪依赖用 `implementation` 声明，靠运行时传递性落到消费方的 classpath——admin/ecs 都不用自己引就拿到 traceId。反过来，`common-security` 不需消费方重复声明，但 web/validation/jpa 三个 starter 在 `admin-service/build.gradle.kts` 与 `ecs-service/build.gradle.kts` 里仍各自显式声明了一次，属既有的冗余写法。

## 命令

| 命令                                      | 说明                                                     |
| ----------------------------------------- | -------------------------------------------------------- |
| `./gradlew :common-core:test`             | 本模块单元测试（MockMvc `standaloneSetup`，不需中间件）  |
| `./gradlew :common-core:architectureTest` | 只跑本模块架构约束                                       |
| `./gradlew :admin-service:bootRun`        | 想看自动装配的真实效果，跑一个消费方；配置键改动在此验证 |
