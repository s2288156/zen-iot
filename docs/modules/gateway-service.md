# gateway-service 决策与坑

[`gateway-service/README.md`](../../gateway-service/README.md) 只放事实与结论，本文放**为什么这样选、否掉了什么、代价在哪**。跨模块必须同时成立的约定在 [`docs/architecture.md`](../architecture.md)，这里不重复。

## 决策

### 只依赖 `common-security`，不依赖 `common-core`

- 背景：网关是 WebFlux 应用，`common-core` 里剩下的每一类都绑定 Servlet——拦截器、`WebMvcConfigurer` 自动配置、JPA 实体基类、`GlobalExceptionHandler`。
- 选项：① 引 `common-core` 并逐条 `exclude` 它以 `api` 暴露的 `spring-boot-starter-web` / `-validation` / `-data-jpa`；② 把 Web 无关的安全内核拆成 `common-security`，网关只引它。
- 结论：②。现在一条 `implementation(":common-security")` 就够，构建脚本里没有任何 `exclude`。
- 理由：`exclude` 是逐条维护的隐式契约，加一个 starter 就要补一条，漏一条的表现是启动期整体翻车（Servlet 判定或无数据源）。
- 代价：多一个模块，两侧的规则清单要各维护一份。

### 依赖隔离做双向断言，且必须用 `Class.forName`

- 背景：拆完之后，「`common-security` 不带 Web/JPA」这件事有两个方向会漏风——生产方偷偷加依赖，或被消费方某条新依赖重新带进来。
- 结论：生产方 `common-security/ArchitectureTest.moduleStaysFreeOfWebAndPersistence()` 按包禁；消费方 `GatewayDependencyIsolationTest` 从运行时类路径断言。
- 关键实现约束：必须用 `Class.forName` 而不是看编译期依赖——换依赖、加 starter、Dependabot 抬版本都会把坐标重新带进来，那种情况下编译照过、IDE 不报警，直到真启动才炸。
- 边界：`GatewayDependencyIsolationTest` 只断言「Servlet/MVC/JPA 三类整包依赖不存在」；按类名拦不住的情况（`StringRedisTemplate` 与 `ReactiveStringRedisTemplate` 同在 `spring-data-redis` 一个 jar 里）交给 `ArchitectureTest` 按编译期依赖精确拦。
- 附带断言：`MUST_BE_PRESENT` 一半是防「反向的静默失效」——`lb://` 所需的 `ServiceInstanceListSupplier`、jjwt 的 `runtimeOnly` 实现类、tracing 的 `Tracer` 缺了都是等真启动或首次调用才炸。

### 不套用四层架构门禁

- 背景：Phase 0 的「新模块接入门禁清单」要求 `controller → service → repository → entity` 单向。
- 结论：网关没有 JPA 层，不套用；通用五条（禁字段注入、禁标准流、禁 `new Date()`、可空性只用 JSpecify、包切片无环）原样保留，另加五条反应式专属约束。
- 代价：网关没有「分层」这一说，包结构（`filter` / `auth` / `error` / `config`）只按职责切，靠切片无环兜住。

### 可信头剥离写在 `GlobalFilter` 里，不用路由的 `default-filters`

- 背景：入站可能自带伪造的 `X-User-Id: 1`。SCG 本身提供 `RemoveRequestHeader` 这种声明式过滤器。
- 选项：路由 `default-filters: RemoveRequestHeader` / 在 `JwtAuthGlobalFilter` 内先剥离。
- 结论：过滤器内剥离。两类过滤器合并后按 order 排序，声明式那条排在鉴权之后就会把网关刚写好的头再删一遍——剥离必须**先于**白名单分流。
- 关键实现约束：白名单路径同样要剥，否则 `/auth/logout` 会收到伪造的 `X-User-Id: 1`。
- 代价：剥离逻辑不再能从路由配置里一眼读出，只能看代码。

### `X-Forwarded-For` 与身份头同一步覆写

- 背景：Phase 4 的登录日志要记客户端 IP。
- 结论：剥离入站 `XFF`，改写为网关连接的 remoteAddr；remoteAddr 缺失时只剥离不追加，宁缺毋伪。
- 前提：拓扑已确认客户端直达网关，remoteAddr 即真实客户端 IP，下游取 XFF 最后一个值可信。
- 代价：一旦网关前面多了任何一层代理（LB、CDN），这条语义立刻失效，且失效方式是把所有请求记成同一个 IP。上代理时必须改这里，不能只加配置。

### CORS 用 `CorsWebFilter`，不用网关的 `globalcors`

- 背景：预检请求（`OPTIONS`）不带 `Authorization`。
- 选项：SCG 的 `globalcors` 配置 / 注册一个 `WebFilter`。
- 结论：`WebFilter`，顺序 `HIGHEST_PRECEDENCE`。预检在鉴权之前就被短路，不进过滤器链。
- 理由补充：两者同时配会重复写 `Access-Control-*` 头，浏览器反而判定跨域失败。
- 代价：跨域配置从网关的 yml 语义挪进 `zen.gateway.cors.*`，读配置的人不一定找得到。

### 鉴权失败由过滤器直接写出响应体

- 背景：全局过滤器抛出的错误能否走到 `ErrorWebExceptionHandler` 取决于框架的过滤器链实现。
- 结论：`JwtAuthGlobalFilter#reject` 直接调 `ApiJsonResponses.write`，不依赖错误传播路径。
- 收益：行为可测（`JwtAuthGlobalFilterTest` 是纯单元测试，不起上下文），且不必和 Boot 的错误处理器抢顺序。
- 代价：写出路径有两处（过滤器直写 + 异常处理器），code↔HTTP 映射规则必须两边共用同一个 `ApiJsonResponses.statusFor`，否则同一个码在两条路上给出不同状态。

### 网关侧另立 `TokenBlocklist`，不复用 `TokenRevocationChecker`

- 背景：撤销的读侧就是查一个 Redis 键，`common-security` 已有 `TokenRevocationChecker` 接口与键名单点。
- 选项：实现那个接口 / 定义一个返回 `Mono<Boolean>` 的网关侧接口。
- 结论：另立接口，但**键名仍复用 `TokenRevocationChecker.blacklistKey(jti)`**——接口分开、字符串单点。
- 理由：那个接口是命令式的，在 Netty EventLoop 上调用它就是 P3-2 BlockHound 要拦的第一类问题。
- 关键实现约束：实现必须返回不会空完成的 `Mono<Boolean>`，空完成会让请求既不放行也不拒绝，直接挂住。`RedisTokenBlocklist` 以 `defaultIfEmpty(FALSE)` 兜住。

### 黑名单读取失败 fail-closed

- 背景：Redis 不可用时是放行还是拒绝。
- 结论：以错误信号透出，由异常处理器兜成 5xx。宁可拒绝也不在校验缺位时放行。
- 代价：Redis 故障期间全站不可用。与 admin 侧登录失败锁定的 fail-open 是**相反的**取向，理由是这两处失败的后果不对称（多锁一会儿 vs. 伪造身份）。

### 不提供「关闭网关鉴权」的配置开关

- 背景：排障时常常想临时关掉鉴权。
- 结论：`GatewayAuthConfiguration` 里不放 `@ConditionalOnProperty`。网关是唯一身份来源，能被配置关掉就等于没有鉴权。
- 替代手段：`GATEWAY_ADMIN_URI` 可以把下游钉到桩后端，但 predicates/filters 仍生效，覆盖的是「打到哪里」而不是「要不要验」。

### 白名单配置不复用 `zen.security.whitelist`

- 背景：`common-security` 已有 `SecurityProperties`（前缀 `zen.security`）承载白名单。
- 结论：网关另立 `ZenGatewayProperties`（前缀 `zen.gateway`）。
- 两条独立理由：① 那份配置由带 `@ConditionalOnWebApplication(SERVLET)` 的自动配置绑定，反应式网关里根本不装配；② 两侧匹配的是**不同字符串**——网关匹配带前缀的入站路径，服务匹配剥掉前缀后的裸路径，合并成一个配置项只会让人以为改一处就够。
- 代价：这就是「两套白名单」这条跨模块契约的由来（见 [`docs/architecture.md`](../architecture.md)）。

### 网关级错误保留下游语义

- 背景：SCG 抛的 `NotFoundException` 带着 `SERVICE_UNAVAILABLE`，无匹配路由抛的也是 `ResponseStatusException`。
- 结论：`BusinessException` 与 `ResponseStatusException` 按自身状态码透出（下游无实例是 503 不是 500），其余才压成 500。
- 理由：压成 500 会让调用方的重试判断与 Phase 10 的探针判断全部失真。
- 配套：`messageFor` 优先取 `GlobalErrorCode` 的中文文案，不透出框架原始 reason，避免泄露内部路径与实现细节。

### tracing 按「谁运行谁声明」自带，且不用 `spring-boot-starter-opentelemetry`

- 背景：拆 `common-core` 之前，tracing 依赖随它的 `implementation` / `runtimeOnly` 传递进来；拆完之后网关得自己声明。
- 选项：`spring-boot-starter-opentelemetry` / `spring-boot-micrometer-tracing-opentelemetry` + `runtimeOnly` OTel bridge。
- 结论：后者。starter 连带 otlp exporter，会真的外发 span，而 P3-1 只做 A 层（traceId 进日志 + 注入 `traceparent`，不外发），Phase 10 接后端时再换。
- 代价：这条依赖链少一行就会静默降级——日志 trace 列为空、下游收不到 `traceparent`，编译与启动都不报错，故由 `GatewayDependencyIsolationTest` 的 `MUST_BE_PRESENT` 拦下。

### 端到端路由测试不引入中间件，也不打 `@Tag("integration")`

- 背景：真实路由依赖 Nacos（服务发现）与 Redis（黑名单），本机干净环境两者都没有。
- 结论：`spring.cloud.nacos.discovery.enabled=false` + `GATEWAY_ADMIN_URI` 指向 JDK 内置 `HttpServer` 桩后端 + `@MockitoBean TokenBlocklist`。
- 理由：剥前缀路由、身份头覆写、统一 JSON 就是本阶段的交付物，把它们留在 `check` 里才有门禁价值。
- 关键实现约束：只换 `uri`，predicates 与 filters 仍来自 `application.yml`——于是「经网关剥两段前缀」这条契约本身也是被测对象，而不是测试自己重写一遍配置。
- 桩后端用 `HttpServer` 而非 WireMock/MockWebServer：本阶段不为测试引入新依赖，P3-2 定案后再补契约测试。
- 顺带利用：`missingDownstreamInstanceIsServiceUnavailableNot500` 走 `wcs-service` 这条没注册实例的路由，白拿一个 503 场景。

## 坑

- **Boot 4 里 `spring.reactor.context-propagation` 默认是 `limited`，网关链路会静默断 trace。** 现象：网关日志有 trace 列，admin 却是另一条 trace。原因：`Tracer.currentSpan()` 在这条链上取不到 span，SCG 的 `observedRequestHttpHeadersFilter` 无 trace 可注入。做法：`application.yml` 显式设 `auto`。删除条件：Boot 把默认值改回 `auto`，或链路追踪改用显式的 `Context` 写入。

- **`@Order` 写在 `@Bean` 方法上对 `CorsWebFilter` 无效。** 现象：跨域预检回 401，浏览器只报「CORS 失败」，看起来像 CORS 配置写错。原因：`WebHttpHandlerBuilder` 收集 `WebFilter` Bean 后用 `AnnotationAwareOrderComparator` 排序，而 `CorsWebFilter` 既没实现 `Ordered` 也没有类级 `@Order`，方法上的注解不在这条排序路径上，它会落到 `LOWEST_PRECEDENCE` 排到路由转发之后。做法：`GatewayCorsWebFilter` 继承它并显式实现 `Ordered`。删除条件：上游给 `CorsWebFilter` 加上 `Ordered`。

- **`ObjectMapper` 必须是 `tools.jackson`（Jackson 3）。** 现象：注入 `com.fasterxml.jackson.databind.ObjectMapper` 起不来，报 `No qualifying bean of type`。原因：Boot 4 默认换到 Jackson 3，自动装配出来的 Bean 就是 `tools.jackson.databind.ObjectMapper`；Jackson 2 仍在类路径上，但只为 jjwt-jackson 服务。做法：网关一律注入 `tools.jackson` 的那把。这条对从旧文档抄代码的人尤其容易踩。

- **Cloud 2025.1 起路由配置前缀整体迁移。** 现象：写 `spring.cloud.gateway.httpclient.*` / 顶层 `routes` 不报错也不生效。原因：SCG 5.0.x 把配置挪到 `spring.cloud.gateway.server.webflux.*`，旧前缀已移除。做法：改路由配置先确认这个前缀。删除条件：又一次大版本迁移——所以本条不会自动消失，改 yml 时始终先看一眼。

- **`lb://` 需要额外引 loadbalancer starter。** 现象：路由配上了一律 503。原因：网关 starter 不传递 `spring-cloud-starter-loadbalancer`。做法：保留这条 `implementation`，它已被 `GatewayDependencyIsolationTest.MUST_BE_PRESENT` 钉住。

- **`micrometer-registry-prometheus` 是 `runtimeOnly`，缺了完全静默。** 现象：Boot 只少暴露一个端点，日志里表现为「Exposing 1 endpoint」，没有任何失败。做法：以 `/actuator/prometheus` 是否真存在为验收，不看启动日志。

- **动态属性只能整体覆盖路由列表。** 现象：想只改 `routes[0].uri` 却发现整段 predicates/filters 都没了。原因：`List` 绑定的属性源按整体覆盖，不做元素级合并。做法：走 `GATEWAY_ADMIN_URI` 这种「URI 用占位符，其余留在 yml」的路子。

- **`Mono<Boolean>` 空完成会把请求挂住。** 现象：既不放行也不拒绝，客户端超时。做法：`RedisTokenBlocklist#isBlocked` 以 `defaultIfEmpty(FALSE)` 保证一定有值；任何自研的 `TokenBlocklist` 实现都要遵守这条，接口 Javadoc 里写着。

- **自定义异常处理器必须抢在 Boot 之前。** 原因：`DefaultErrorWebExceptionHandler` 的 order 是 `-1`，晚于它的处理器不会生效，响应变成 whitelabel 页面。做法：`GatewayErrorWebExceptionHandler` 取 `HIGHEST_PRECEDENCE`。`unmatchedRouteStillGetsUnifiedJson` 就是这个顺序的探针。

- **响应已提交时改不了状态码与 body。** 现象：偶发的 200 + 半截 JSON 后面跟着一个错误日志。做法：`handle` 里先判 `response.isCommitted()` 并 `Mono.error(ex)` 交回上层，不要试图补写。

- **白名单里的 `/actuator/health` 两条其实不经全局过滤器。** 现象：删掉它们行为不变。原因：网关自身的 actuator 不是路由请求，`GlobalFilter` 根本不介入；它们留在 yml 只作意图声明。删除条件：`GatewayRouteContractTest.whitelistUsesGatewayPrefixedInboundPaths` 或新增用例真正断言这两条的存在，否则它们会一直给人一种「改这里能关掉健康检查匿名访问」的错觉。

- **`GatewayRouteContractTest` 的 Javadoc 与方法名仍按拆模块前的立论描述。** 现象：`jwtVerifierIsAutoConfiguredDespiteCommonCoreExcludes` 与「common-core 的 JWT 自动装配要在排掉三个 starter 之后仍然生效」，而事实是 JWT 自动装配在 `common-security`（`ZenJwtAutoConfiguration`）且构建脚本里已无 `exclude`。做法：改名与改注释时一并修；断言本身（`JwtTokenVerifier` 在网关上下文里可用、且上下文里没有 `DataSource`）不需要动。
