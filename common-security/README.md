# common-security

Web 无关的**安全内核**：JWT 签发/验签、统一响应与错误码、身份透传头与令牌撤销契约。它刻意不依赖任何传输栈或持久化栈，因此反应式网关与 Servlet 业务服务复用同一份验签代码时，谁都不需要排依赖。

本模块**不提供 HTTP 接口**，也不含 Servlet/Reactor 类型的签名。所有服务都是它的消费方：网关直接依赖，`admin-service` / `ecs-service` 经 `common-core` 的 `api` 透传拿到。

本文只写事实与结论。**为什么这样选、否掉了什么、踩过什么坑**见 [`docs/modules/common-security.md`](../docs/modules/common-security.md)；跨模块必须同时成立的约定（密钥一致、撤销读写两侧、身份头防伪）见 [`docs/architecture.md`](../docs/architecture.md)。

## 能力与接口口径

配置键（属性类 `JwtProperties`，前缀 `zen.jwt`）：

| 配置键                | 默认  | 口径                                                                   |
| --------------------- | ----- | ---------------------------------------------------------------------- |
| `zen.jwt.secret`      | 无    | HS256 密钥原文，**按 UTF-8 字节数**要求 ≥ 32；缺省则 JWT Bean 全不注册 |
| `zen.jwt.access-ttl`  | `30m` | access Token 有效期，只影响签发方                                      |
| `zen.jwt.refresh-ttl` | `7d`  | refresh Token 有效期                                                   |

Token 声明（`username` / `typ` / `roles` / `modules` 四个字面量集中在包私有 `JwtClaimNames`，签发与解析两侧共用；`sub` / `jti` / `iat` / `exp` 走 jjwt 标准访问器）：

| 声明                | 内容                                             |
| ------------------- | ------------------------------------------------ |
| `sub`               | 用户 ID（字符串形式的 `long`，非数字即验签失败） |
| `jti`               | 每个 Token 独立 `UUID`，access 与 refresh 不共用 |
| `iat` / `exp`       | 签发与过期时刻                                   |
| `username`          | 用户名                                           |
| `typ`               | `access` / `refresh`，用途隔离的依据             |
| `roles` / `modules` | 字符串数组，`modules` 是授权判定依据             |

- **验签只保证签名有效、未过期、`typ` 与期望一致**；不查黑名单——那是持有 Redis 的一侧的事（`JwtTokenVerifier#verify` 恒要求传 `expectedType`）。
- **任何不通过都是同一个结果**：抛 `BusinessException(GlobalErrorCode.UNAUTHORIZED)`，不区分「签名错 / 过期 / 用途错 / 格式错」，也不关心传输层怎么把它变成 401。
- 撤销键名单点定义：`TokenRevocationChecker.blacklistKey(jti)` → `auth:blacklist:{jti}`。写侧（`admin-service`）与读侧（网关）都调它，不各自拼字符串。
- 身份头名唯一来源：`TrustedHeaders` 的 `X-User-Id` / `X-Username` / `X-User-Roles` / `X-User-Modules`。
- `SecurityProperties`（前缀 `zen.security`：`enabled` / `context-source` / `whitelist`）也住本模块，但只在 Servlet 侧由 `common-core` 的自动配置绑定消费；网关不复用它（另立 `zen.gateway.*`）。
- 统一响应 `ApiResponse<T>`：`code` / `message` / `data`，成功码 200；`@JsonInclude(NON_NULL)`——失败响应里 `data` 字段**不出现**，不是 `null`。
- 通用错误码只有 `GlobalErrorCode` 的 11 个 HTTP 语义码（200/400/401/403/404/405/409/429/500/501/503）；业务码段由消费方自建枚举实现 `ErrorCode`。
- 授权最小单位是 `ModuleCode`（`admin` / `wcs` / `rcs` / `ecs`，落库与 claim 均为小写）。`ADMIN` 是「管理后台」模块而非超级权限。

## 关键实现

- **JWT Bean 以 `zen.jwt.secret` 是否存在为开关**：`ZenJwtAutoConfiguration` 只有 `@ConditionalOnProperty(prefix = "zen.jwt", name = "secret")`，不限定 Web 类型——Servlet 与 Reactive 通用。不配该键的服务（如只做透传的下游）不会因缺配置而启动失败。
- **密钥推导集中在包私有 `JwtKeys`**：`secret` 为 null/空白时抛 `IllegalStateException`；短于 32 字节时由 jjwt 的 `Keys.hmacShaKeyFor` 抛 `WeakKeyException`，两者都是启动期失败。
- **契约类型全是 record + 紧凑构造器**：`TokenPrincipal` / `VerifiedToken` / `UserPrincipal` 的 `roles`、`modules` 为 null 时归一成 `List.of()`，否则 `List.copyOf`。这是 SpotBugs 的免豁免样板（`VerifiedToken` 靠它消掉 4 条告警）。
- **两个 `remainingTtl()` 语义不同，别混用**：`VerifiedToken` 的 `expiresAt` 恒非空，已过期返回 `Duration.ZERO`；`UserPrincipal` 在网关透传模式下 `jti` 与 `expiresAt` 为 null，`remainingTtl()` 随之返回 `null`，Javadoc 明确要求「调用方据此跳过撤销」。
- **`UserContext` 是 Servlet 侧的 ThreadLocal 持有者**，约定只能由 `common-core` 的 `AuthInterceptor` 写入并在 `afterCompletion` 清除；在异步线程或定时任务里 `get()` 返回 null。
- **`@RequireModule` 只支持方法级**（`@Target(ElementType.METHOD)`），写在类上是编译错误；缺注解即不校验模块，仍要求已登录。
- **`TokenRevocationChecker.disabled()` 是接口自带的空实现**（`revoke` 空操作、`isRevoked` 恒 false），由 `common-core` 的自动配置在缺实现时兜底。
- **jjwt 的 `Date` 边界是本模块唯一允许 `java.util.Date` 的地方**：`JwtTokenIssuer#build` 用 `Date.from(Instant)` 传 `issuedAt` / `expiration`，门禁只禁 `new Date()` 构造调用。
- **构建侧与 `common-core` 同理**：挂 `org.springframework.boot` 插件只为拿托管版本，显式关 `bootJar` 并把 `jar` 的 classifier 复位，见 `common-security/build.gradle.kts`。

## 架构约束

`./gradlew :common-security:architectureTest`，规则清单见 `common-security/src/test/java/com/zen/common/security/ArchitectureTest.java`。其中 `moduleStaysFreeOfWebAndPersistence` 是「网关可以直接复用它而不必排依赖」的可执行形式，`importedClassesAreNeverEmpty` 是保证前者没被静默架空的哨兵：

- `moduleStaysFreeOfWebAndPersistence`：按包禁全——`jakarta.servlet` / `jakarta.persistence` / `jakarta.validation` / `springframework.web` / `springframework.data` / `springframework.transaction` / `hibernate`。取代了拆模块前 `common-core` 里只盯 `jakarta.servlet` 的那条。
- `importedClassesAreNeverEmpty`：反「静默失效」哨兵，导入类数须 > 20（拆模块时实测 19 的下一档）。`noClasses()` 在一个类都没导入时也判通过，包根改名或误删目录会让全部规则一起变绿。
- `neverDependsOnCommonCore`：依赖方向单向 `common-core → common-security`。
- `neverDependsOnServicesOrGateway`：不得引用 `com.zen.{admin,ecs,gateway,rcs,wcs}`，同 `common-core` 一样是硬编码包名列表。
- 通用五条：禁字段注入、禁标准流、禁 `new Date()` 构造、可空性只用 jspecify、包切片无环，见 [`AGENTS.md`](../AGENTS.md)。

消费方视角还有一道独立复核：`gateway-service` 的 `GatewayDependencyIsolationTest`。同一边界两端各断言一次，一边漏风也会被抓住。

## 主要依赖

`api` 暴露 `spring-boot-starter`（不含任何 Web starter）、`jackson-annotations`（`ApiResponse` 的 `@JsonInclude` 出现在公共签名里）、`jjwt-api`（版本由根的 jjwt BOM 统一管）；`runtimeOnly` `jjwt-impl` 与 `jjwt-jackson`；Lombok `compileOnly` + `annotationProcessor`。

没有任何 starter 带 `data-*` 或 `web`，也没有 Redis 客户端——撤销契约是接口，实现留在持有 Redis 的服务里。

## 命令

| 命令                                          | 说明                                                        |
| --------------------------------------------- | ----------------------------------------------------------- |
| `./gradlew :common-security:test`             | 单元测试：签发/验签往返与拒绝路径、自动装配条件（无中间件） |
| `./gradlew :common-security:architectureTest` | 只跑本模块架构约束                                          |
