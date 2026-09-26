# common-core 决策与坑

[`common-core/README.md`](../../common-core/README.md) 只放事实与结论，本文放**为什么这样选、否掉了什么、代价在哪**。跨模块必须同时成立的约定在 [`docs/architecture.md`](../architecture.md)，这里不重复。

## 决策

### 只服务 Servlet 侧，反应式形态一律不进

- 背景：网关是 WebFlux/Netty，admin/ecs 是 MVC/JPA，两者要复用的东西不一样。
- 选项：本模块同时提供两套装配 / 只装 Servlet 一套，反应式需要的部分拆走。
- 结论：后者。Web 无关的安全内核拆成 `common-security`，本模块只放拦截器、`WebMvcConfigurer`、`@RestControllerAdvice`、JPA 基类与分页类型（`PageQuery` / `PageResult`）。
- 代价：网关要用的类型只能待在 `common-security`，一旦有人图方便往本模块加个 Servlet 无关的通用工具，网关就得跟着排依赖。方向依赖由两边的 `ArchitectureTest` 各断言一次。

### 装配类靠 `@ConditionalOnWebApplication(SERVLET)` 自保，不靠「消费方不依赖」

- 背景：自动配置类一旦落到反应式 classpath 上就会被注册并参与启动评估，除非自身带条件。
- 结论：Web 侧两个（`ZenWebAutoConfiguration` / `ZenSecurityAutoConfiguration`）限定 SERVLET，条件不满足时整个配置类不评估；`ZenJpaAuditingAutoConfiguration` 刻意不限定——JPA 审计与 Web 类型正交，反应式应用根本不引 JPA，边界由 `@ConditionalOnClass` 把守。
- 理由：模块归属是人能改的（明天就可能有人让网关 `implementation(project(":common-core"))`），条件判断是改不坏的。
- 代价：条件为假时是静默跳过，配错形态不会报错。所以网关侧另有 `GatewayDependencyIsolationTest` 兜住方向。

### 挂 `org.springframework.boot` 插件只为拿托管版本

- 背景：本模块是 `java-library`，产的是普通 jar，却挂了 Boot 插件；只挂 `io.spring.dependency-management` 时实测 `spring-cloud-alibaba-dependencies` 直接声明的坐标会盖过 Boot 的 BOM（jackson-databind 2.12.7.1、lombok 1.18.38、slf4j-api 2.0.17）。
- 结论：挂上 Boot 插件，使本模块自测所用版本与 `admin-service` 运行时完全一致（挂后两模块 2230 项托管版本全等）。
- 代价：Boot 插件不会因为 `java-library` 就自动关 `bootJar`，必须显式关掉 `bootJar`、重新启用 `jar`、并把 Boot 加的 `-plain` classifier 复位成默认产物名，否则 `assemble` 报「Main class name has not been configured」且产物名带 `-plain`。见 `common-core/build.gradle.kts`。

### `api` 只给出现在公共签名里的类型

- 背景：`GlobalExceptionHandler` 与两个拦截器的签名用到 `ApiResponse` / `BusinessException` / `UserPrincipal`，JPA 与 validation 的类型同理。
- 结论：`common-security` 与 web/validation/data-jpa 三个 starter 用 `api`，追踪是 `implementation` `spring-boot-micrometer-tracing-opentelemetry` + `runtimeOnly` `micrometer-tracing-bridge-otel`。
- 理由：消费方（admin/ecs）不该为了用异常处理而重复声明安全类型；追踪是运行期能力，编译期不需要看见。
- 代价：`api` 是对外承诺，把某个 starter 从 `api` 降级成 `implementation` 会让消费方编译失败；这也是跨模块版本比对（`crossModuleVersionCheck`）的输入之一。

### 异常出口集中，但每个 Bean 都可被消费方接管

- 背景：401/403/400/404/405/500 的响应体必须一模一样，否则前端要为每个服务写一套解析。
- 结论：`GlobalExceptionHandler` 由 `ZenWebAutoConfiguration` 装配，条件带 `@ConditionalOnMissingBean`；同理 `TokenRevocationChecker`、`UserContextResolver`、两个拦截器、`AuditorAware`。
- 理由：集中不等于不可变。服务确实需要不同口径时，定义同类型 Bean 即覆盖，不需要 `@Primary` 也不需要排除自动配置。
- 代价：接管点靠约定，忘了 `@ConditionalOnMissingBean` 就会撞 Bean 冲突；方向上，是自动配置在消费方配置之后评估、靠 `@ConditionalOnMissingBean` 退让——admin 的 `RedisTokenRevocationChecker` 正是这样顶掉缺省实现的。

### HTTP 状态只在错误码能解析成 4xx/5xx 时对齐

- 背景：`GlobalErrorCode` 里既有 HTTP 语义码，也有业务码段（1xxx）。
- 结论：`GlobalExceptionHandler#build` 用 `HttpStatus.resolve(code)` + `isError()` 判定，不满足就回 HTTP 200，错误由响应体 `code` 表达。
- 理由：业务码不是 HTTP 状态，硬套会把「余额不足」这类语义塞进一个不存在的状态码里。
- 代价：客户端必须同时看状态与 `code`；只按 HTTP 2xx 判成功的调用方会在业务失败时误判。

### 身份来源做成配置项，而不是每个服务自己选实现

- 背景：网关已经验过签并把身份写进 `X-User-*`，下游再验一次是重复劳动；但下游也可能不经网关直连。
- 结论：`zen.security.context-source` 二选一（`jwt` 默认 / `gateway-header`），两个解析器同一接口 `UserContextResolver`，切换不改代码。
- 代价：`jwt` 模式要求服务持有 `zen.jwt.secret`，缺密钥是启动期失败而非运行期降级；两种模式产出的 principal 完整度不同（见坑清单）。

### 撤销检查的缺省实现是「永不撤销」

- 背景：`JwtUserContextResolver` 需要查撤销，但并非每个服务都接了 Redis。
- 选项：不注册该 Bean（启动即缺依赖失败）/ 注册一个 `TokenRevocationChecker.disabled()`。
- 结论：`ZenSecurityAutoConfiguration#disabledTokenRevocationChecker` 兜一个空实现，让不关心撤销的纯透传服务保持零配置。
- 代价：接了 Redis 的服务必须自己声明实现，否则登出静默失效——这是本模块最需要盯住的一处退化，见坑清单。

### 链路追踪只做 A 层：进日志，不外发

- 背景：Phase 10 才有 OTLP collector，现在把 span 发出去就是往不存在的端点投递。
- 结论：`spring-boot-micrometer-tracing-opentelemetry` + `micrometer-tracing-bridge-otel`，只让 traceId 进日志；刻意不用 `spring-boot-starter-opentelemetry`——它连带 `opentelemetry-exporter-otlp` 与 `micrometer-registry-otlp`，会真的开始外发数据。
- 代价：Boot 4 起自动配置独立成模块，少了前者就连 `Tracer`/traceId 的装配都没有；这条依赖是「隐式能力」，从 `build.gradle.kts` 删掉不会有任何编译错误，只会让日志里的 traceId 消失。

### 审计人给 `"system"`，不给真实用户

- 背景：`BaseEntity` 的 `creator`/`updater` 需要一个 `AuditorAware<String>`，而 `UserContext` 是请求态的。
- 结论：`ZenJpaAuditingAutoConfiguration#zenAuditorAware` 返回固定的 `Optional.of("system")`，带 `@ConditionalOnMissingBean`，业务服务定义自己的 `AuditorAware` 即覆盖（admin 就是这么把审计人换成登录用户名的）。
- 理由：公共模块不能假设「一定有请求上下文」——定时任务、启动期初始化都要能落库。
- 代价：`zen.jpa.auditing.enabled` 默认为真，新服务忘了覆盖就会静默记成 `system`，且不报错。缺省实现恒返回 `Optional.of("system")`；`AuditorAware` 的签名本就是 `Optional<String>`，返回空是消费方实现的选择——审计人缺失时 `create_time` 仍能写入。

### 排序字段白名单交给调用方

- 背景：`PageQuery.orderBy` 直接来自客户端。
- 选项：本模块内置实体属性校验 / 只做 `toPageable()` 并声明责任在调用方。
- 结论：后者。`orderBy` 的 Javadoc 写明「调用方必须先用实体属性白名单校验后再传入」。
- 理由：公共模块不知道每个实体的合法属性集，硬做要么反射扫实体（慢且脆）要么搞一份全局注册表（重）。
- 代价：漏校验就是任意排序属性可达，慢查询与列探测都归调用方。两个消费方的做法一致：非法排序字段直接 400（admin 见 `RoleController` 类 Javadoc，ecs 见其 README 口径）。

## 坑

- **缺省 `TokenRevocationChecker` 会让登出静默失效。** 现象：`context-source=jwt` 的服务如果没声明自己的 `TokenRevocationChecker`，自动配置兜的 `disabled()` 恒返回「未撤销」，登出与踢下线在鉴权链路上完全无感——写侧写进了 Redis，读侧压根不查。做法：接 Redis 的服务必须显式声明实现（admin 的 `RedisTokenRevocationChecker`），并断言撤销链路。**删除条件**：把缺省实现改成「`context-source=jwt` 且无 `TokenRevocationChecker` 即启动失败」，或加一条架构/装配测试拦住这种组合之后。

- **畸形 JSON 请求体回 500，不是 400。** 现象：`GlobalExceptionHandler` 没有 `HttpMessageNotReadableException` 处理器，请求体 JSON 语法错会落到 `Exception` 兜底，返回 500 与「服务器内部错误」，而客户端错明明是 4xx。做法：要么补处理器，要么在文档里承认这个口径。删除条件：`GlobalExceptionHandler` 增加该处理器并有断言 400 的用例（`GlobalExceptionHandlerTest` 目前 5 个用例覆盖 409/400/405/404/500，无此项）。

- **`@RequestBody` 校验失败依赖 Spring 7 的继承关系。** 现象：本模块只写了 `@ExceptionHandler(BindException.class)`，`MethodArgumentNotValidException` 能一起被覆盖是因为它在 Spring 7 里继承 `BindException`（该处注释是唯一的解释）。框架一旦改回平级，`@RequestBody` 的校验错误会静默落到 500 兜底，而现有测试用的是缺 param / 404 / 405，**没有一条走 `@RequestBody` 校验**，改坏了不会红。做法：升级 Spring 大版本前先补 `@RequestBody` + `@Valid` 的 400 用例。

- **白名单是路径级排除，连带关掉模块校验。** 现象：`zen.security.whitelist` 同时传给两个拦截器的 `excludePathPatterns`，把带 `@RequireModule` 的路径误写进白名单，结果不是「匿名可访问」而是「任何人都能访问且不看模块」。做法：改白名单时逐条核对对应方法上没有 `@RequireModule`。删除条件：加一条装配测试断言白名单命中的 handler 方法不带模块注解，或把模块校验从白名单作用范围里摘出来。

- **透传模式的 principal 缺 `jti` 与过期时刻。** 现象：`GatewayHeaderUserContextResolver` 造的 `UserPrincipal` 里 `jti`/`expiresAt` 为 null，`remainingTtl()` 随之返回 null；任何「按剩余有效期写撤销」的逻辑在 `gateway-header` 服务上都是空操作，而编译期毫无提示。做法：调用 `remainingTtl()` 的分支必须显式处理 null（口径见 `UserPrincipal` 的 Javadoc）。

- **`ArchitectureTest` 的反向依赖靠硬编码包名列表。** 现象：`commonCoreNeverDependsOnBusinessServices` 逐模块写死 `com.zen.{admin,ecs,gateway,rcs,wcs}`，新建模块不补这一条，它的反向依赖就无人拦——测试仍然全绿。做法：新建模块时到各模块这类测试的包名列表里补一条（现状 5 个模块逐模块硬编码，无统一清单可改）。删除条件：换成按注解或按 `settings.gradle.kts` 模块列表动态推导之后。

- **自动装配的测试只覆盖到 Bean 层。** 现象：`ZenSecurityAutoConfigurationTest` 用 `WebApplicationContextRunner` 断言 Bean 存在与类型，但拦截器的**注册顺序**与 `excludePathPatterns` 的实际生效只能在真实 MVC 上下文里观察到，本机 `test` 不跑这类断言。做法：改拦截器注册逻辑时，用 `DemoController` 那组自证接口在 `./gradlew :admin-service:bootRun` 上手工验一次顺序效应。
