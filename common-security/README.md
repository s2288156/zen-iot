# common-security

Web 无关的**安全内核**：JWT 签发/校验、统一响应与错误码、身份透传头与令牌撤销契约。它刻意不依赖任何传输栈或持久化栈，因此反应式网关与 Servlet 业务服务复用同一份验签代码时，谁都不需要排依赖。

## 内容

- **JWT**（`com.zen.common.security.jwt`）：`JwtTokenIssuer` / `JwtTokenVerifier` / `VerifiedToken` / `TokenPair` / `TokenType`，加上 `JwtProperties`、`JwtClaimNames`、`JwtKeys`、`TokenPrincipal`。HS256，`typ` 声明区分 access/refresh，密钥以 UTF-8 ≥ 32 字节为前置。
- **自动装配**：`ZenJwtAutoConfiguration` 以 `zen.jwt.secret` 为开关（`@ConditionalOnProperty`）——不配这个键就没有 `JwtTokenVerifier`，`context-source: jwt` 的服务会在装配期直接失败，而不是静默放行。
- **统一 API 响应**：`ApiResponse<T>`（code/message/data，成功码 200）。
- **错误码与业务异常**：`ErrorCode` 接口 + `GlobalErrorCode` 通用枚举 + `BusinessException`，业务服务可自建枚举实现 `ErrorCode` 扩展自己的码段。
- **身份与授权契约**：`TrustedHeaders`（网关 → 下游那 4 个头名的唯一来源）、`UserPrincipal`、`UserContext`（Servlet 侧 ThreadLocal 持有者）、`SecurityProperties`（`zen.security`）、`ModuleCode` + `@RequireModule`。
- **撤销契约**：`TokenRevocationChecker` 含 `blacklistKey(jti)`——写侧 `admin-service` 与读侧网关共用这一个键名定义。

## 架构约束

`ArchitectureTest` 把「本模块存在的理由」写成断言：

- `moduleStaysFreeOfWebAndPersistence()`：整个模块不得依赖 Servlet / Spring Web（两种传输栈）/ `jakarta.validation` / JPA / Spring Data / 事务。
- `neverDependsOnCommonCore()`：不得反向依赖 `common-core`。
- `neverDependsOnServicesOrGateway()`：不得引用业务服务或网关。
- `importedClassesAreNeverEmpty()`：导入类数非零哨兵，防「覆盖面缩水」变成静默通过。
- 另有 `new Date()`、字段注入、标准流、jspecify 与包切片无环等通用条，见 `AGENTS.md`。

> 前两条就是「网关可以直接复用它而不必排依赖」的可执行形式。`common-core` 原来那条只禁 `jakarta.servlet` 的 `jwtPackageStaysServletFree()` 已被它们取代。
>
> SpotBugs 政策（豁免须写理由且尽量窄）见 `AGENTS.md`。本模块的样板是 `VerifiedToken`：它用 `List.copyOf` 紧凑构造器消掉 4 条告警，而不是申请豁免。

## 主要依赖

`spring-boot-starter`（不含任何 Web starter）、`jjwt-api`（`api` 暴露）+ `jjwt-impl`/`jjwt-jackson`（`runtimeOnly`）、Jackson annotations、Lombok。

## 命令

| 命令                                          | 说明               |
| --------------------------------------------- | ------------------ |
| `./gradlew :common-security:architectureTest` | 只跑本模块架构约束 |
| `./gradlew :common-security:test`             | 本模块单元测试     |
