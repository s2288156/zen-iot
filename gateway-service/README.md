# gateway-service

API 网关（端口 `28080`，WebFlux 反应式）：统一路由与剥前缀、JWT 验签与撤销读取、身份头覆写透传、跨域放行、网关级错误的统一 JSON。它是透传身份的唯一签发点——`gateway-header` 模式的下游（如 ecs）不持密钥，绕过网关就拿不到可信身份；`jwt` 模式的下游（admin）自行验签。

本文只写事实与结论。**为什么这样选、否掉了什么、踩过什么坑**见 [`docs/modules/gateway-service.md`](../docs/modules/gateway-service.md)；跨模块必须同时成立的约定（前缀所有权、密钥一致、两套白名单、撤销链、身份头与可信 IP）见 [`docs/architecture.md`](../docs/architecture.md)。

## 能力与接口口径

本模块不提供业务接口，对外表面是**路由表 + 鉴权口径 + 配置键**。

### 路由

`/api/{service}/**` → `lb://{service}`，`StripPrefix=2` 剥掉两段前缀，下游服务内一律写裸路径。

| 入站前缀        | 目标                 | URI 可覆盖          |
| --------------- | -------------------- | ------------------- |
| `/api/admin/**` | `lb://admin-service` | `GATEWAY_ADMIN_URI` |
| `/api/wcs/**`   | `lb://wcs-service`   | —                   |
| `/api/rcs/**`   | `lb://rcs-service`   | —                   |
| `/api/ecs/**`   | `lb://ecs-service`   | —                   |

只有 `admin-service` 一条允许用 `GATEWAY_ADMIN_URI` 钉到固定地址，predicates/filters 仍来自 `application.yml`，于是路由契约本身继续是被测对象（见 `GatewayRoutingTest`）。新增业务服务要同时改 `application.yml` 与 `GatewayRouteContractTest`（四段前缀逐条比对）——少了比对用例，前缀写错不会有任何报警。

### 配置键

| 键                                         | 默认值                                 | 口径                                                     |
| ------------------------------------------ | -------------------------------------- | -------------------------------------------------------- |
| `zen.jwt.secret`                           | dev 常量，生产由 `ZEN_JWT_SECRET` 覆盖 | 必须与 admin-service 完全一致，按 UTF-8 字节数 ≥ 32      |
| `zen.gateway.auth.whitelist`               | `List.of()`（yml 另配 6 条）           | 免鉴权路径，匹配**带前缀的入站路径**，`PathPattern` 风格 |
| `zen.gateway.cors.allowed-origin-patterns` | `*`                                    | 用 patterns 而非 origins                                 |
| `zen.gateway.cors.allowed-methods`         | `GET/POST/PUT/PATCH/DELETE/OPTIONS`    | yml 显式配了同值                                         |
| `zen.gateway.cors.allowed-headers`         | `*`                                    | yml 显式配了同值                                         |
| `zen.gateway.cors.allow-credentials`       | `false`                                | 前端凭 Bearer Token，不带 Cookie；开它必须同时收窄来源   |
| `zen.gateway.cors.max-age`                 | `1h`                                   | 预检结果缓存                                             |
| `spring.reactor.context-propagation`       | `auto`（Boot 默认 `limited`）          | traceId 进日志与 `traceparent` 注入下游的共同前提        |

yml 里的白名单：`/api/admin/auth/login`、`/api/admin/auth/refresh`、`/api/*/actuator/health`、`/api/*/actuator/health/**`，以及 `/actuator/health`、`/actuator/health/**`（后两条只作意图声明——网关自身的 actuator 不是路由请求，不经全局过滤器）。`prometheus` 不放开，指标只走内网。

### 鉴权口径

`JwtAuthGlobalFilter#filter` 按「剥离可信头 → 白名单 → 取 Bearer → 验签（签名 / 过期 / `typ=access`）→ Redis 黑名单 → 覆写身份头」执行，`Ordered.HIGHEST_PRECEDENCE`。

| 情况                                  | 结果                                               |
| ------------------------------------- | -------------------------------------------------- |
| 无 `Authorization` / 非 Bearer / 空串 | 401 `未携带访问令牌`，请求不转发                   |
| 签名不符、过期、refresh 令牌          | 401，错误码与文案由 `common-security` 的验签器给出 |
| `jti` 命中黑名单                      | 401 `登录已失效，请重新登录`                       |
| 黑名单读取失败（Redis 故障）          | 以错误信号透出，兜成 5xx——fail-closed              |
| 无匹配路由                            | 404 统一 JSON                                      |
| 路由命中但下游无实例                  | 503（SCG 的 `NotFoundException` 语义，不压成 500） |
| 未识别异常                            | 500 `系统异常`                                     |

- **身份头**：`X-User-Id` / `X-Username` / `X-User-Roles` / `X-User-Modules`，roles 与 modules 用逗号连接——下游 `GatewayHeaderUserContextResolver` 按逗号切分，这里就是它的解析契约。
- **入站同名头一律先剥离**，白名单路径同样剥。`X-Forwarded-For` 同步处理：删入站值，改写为网关连接的 remoteAddr；缺失时只删不加。
- 免鉴权路径也会写 `X-Forwarded-For`，登录日志记录的 IP 依赖这一语义。

## 关键实现

- **只依赖 `common-security`，不依赖 `common-core`**：验签器 `JwtTokenVerifier` 由 `ZenJwtAutoConfiguration` 自动装配（以 `zen.jwt.secret` 存在为开关），本模块不重复声明。
- **撤销只有读侧**：`RedisTokenBlocklist` 用 `ReactiveStringRedisTemplate` 查 `TokenRevocationChecker.blacklistKey(jti)`，只读不写；写侧在 admin-service。
- **`TokenBlocklist` 刻意不实现 `TokenRevocationChecker`**：后者是命令式接口，在 Netty EventLoop 上调用它就是 BlockHound 要拦的第一类问题。
- **鉴权失败由过滤器直接写出响应体**（`ApiJsonResponses`），不经 `ErrorWebExceptionHandler`；路由级错误才由 `GatewayErrorWebExceptionHandler`（`HIGHEST_PRECEDENCE`，必须早于 Boot 的 `-1`）承接。
- **code↔HTTP 映射与 Servlet 侧同规则**：`ApiJsonResponses.statusFor` 只在 code 是合法 HTTP 错误码时对齐状态码，否则回落 200 由 body 的 `code` 表达错误。
- **CORS 用 `GatewayCorsWebFilter`**（`CorsWebFilter` 子类 + 显式 `Ordered`）而非网关的 `globalcors`，预检在鉴权之前短路。
- **不提供「关闭网关鉴权」的开关**：它是透传身份的唯一签发点，能被配置关掉就等于 `gateway-header` 模式的下游没有鉴权。
- **不复用 `common-security` 的 `SecurityProperties`**（前缀 `zen.security`）：那份由 `@ConditionalOnWebApplication(SERVLET)` 的自动配置绑定，反应式网关里根本不装配，且两侧白名单匹配的字符串不同。
- **白名单在构造期解析成 `PathPattern`**，解析失败直接抛——宁可启动失败也不留一个永不匹配的白名单。
- **追踪只做 A 层**：traceId 生成 / 透传 / 写 MDC + 由 SCG 向下游注入 `traceparent`，不外发 span。

## 架构约束

`./gradlew :gateway-service:architectureTest`，规则清单见 `gateway-service/src/test/java/**/ArchitectureTest.java`（10 条）。本模块不套用 `controller → service → repository → entity` 四层，模块通用五条（禁字段注入、禁标准流、禁 `new Date()`、可空性只用 JSpecify、包切片无环）原样保留，另加五条只属于反应式网关的约束：

- 禁依赖 `jakarta.servlet..` / `jakarta.validation..` / `org.springframework.web.servlet..`。
- 禁依赖 `jakarta.persistence..` / `org.springframework.data.jpa..` / `org.hibernate..`。
- 禁依赖 `com.zen.common.core..`、任何 `*Interceptor`、以及 `com.zen.common.security.auth.UserContext`（ThreadLocal 在反应式链路里会读到上一个请求的用户）。
- 禁按 FQN 使用命令式 `StringRedisTemplate` / `RedisTemplate`（`ReactiveStringRedisTemplate` 同包，故不能整包禁）。
- 禁调用 `Mono` / `Flux` 上任何以 `block` 开头的算子。

`GatewayDependencyIsolationTest` 从消费方视角补一类 ArchUnit 看不见的失效：按 `Class.forName` 断言 Servlet/JPA 栈整包不在类路径、且 `lb://` 与 tracing 所需类型确实在，并断言 `WebApplicationType.deduce()` 判成 `REACTIVE`。

## 主要依赖

`common-security`（唯一项目依赖）、Spring Cloud Gateway（`spring-cloud-starter-gateway-server-webflux`，Cloud 2025.1 起网关拆成 webflux/webmvc 两套）、Spring Cloud LoadBalancer（`lb://` 需要，网关 starter 不传递）、Spring Data Redis Reactive、Nacos Discovery、actuator + prometheus registry、`spring-boot-micrometer-tracing-opentelemetry` + `micrometer-tracing-bridge-otel`、Lombok。

## 命令

| 命令                                          | 说明                                 |
| --------------------------------------------- | ------------------------------------ |
| `./gradlew :gateway-service:bootRun`          | 启动网关（28080）                    |
| `./gradlew :gateway-service:bootJar`          | 产出 `gateway-service.jar`           |
| `./gradlew :gateway-service:test`             | 含路由契约与端到端桩测试，无需中间件 |
| `./gradlew :gateway-service:architectureTest` | 只跑本模块架构约束                   |
