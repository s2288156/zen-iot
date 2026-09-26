# admin-service

认证与授权基线服务（端口 `28081`，库 `zen_admin`）：登录 / 刷新 / 登出、令牌签发与撤销、模块权限判定、在线会话与两类审计。

本文只写事实与结论。**为什么这样选、否掉了什么、踩过什么坑**见 [`docs/modules/admin-service.md`](../docs/modules/admin-service.md)；跨模块必须同时成立的约定（密钥一致、两套白名单、撤销链、可信 IP）见 [`docs/architecture.md`](../docs/architecture.md)。

## 能力与接口口径

路径为**裸路径**，`/api/admin` 前缀只存在于网关侧；`zen.security.whitelist` 与网关的 `zen.gateway.auth.whitelist` 是两份配置，新增公开接口要同时改两处。

| 接口                                            | 鉴权要求                   | 关键错误码                           |
| ----------------------------------------------- | -------------------------- | ------------------------------------ |
| `POST /auth/login`                              | 免鉴权                     | 401 凭据错 / 403 已停用 / 429 已锁定 |
| `POST /auth/refresh`                            | 免鉴权，Token 自身要过校验 | 401 签名、过期或已撤销               |
| `POST /auth/logout`                             | 登录                       | 401                                  |
| `GET /auth/me`                                  | 登录                       | 401（含用户已被逻辑删）              |
| `POST /auth/change-password`                    | 登录                       | 400 旧口令不正确 / 401               |
| `POST` `PUT` `DELETE /roles/**`                 | `admin` 模块               | 400 非法模块或排序字段 / 404 / 409   |
| `GET /roles/page`、`GET /roles/{id}`            | 登录                       | 400 / 404                            |
| `POST` `PUT` `PATCH` `DELETE /users/**`         | `admin` 模块               | 400 / 404 / 409 用户名重复           |
| `GET /users/page`、`GET /users/{id}`            | 登录                       | 400 / 404                            |
| `GET /sessions`、`DELETE /sessions/{sessionId}` | `admin` 模块               | 400 踢自己 / 404 会话不存在          |
| `GET /demo/admin`、`GET /demo/ecs`              | 对应模块                   | 401 / 403，用于自证鉴权闭环          |

`DemoController` 不是业务功能，只用来验证 `@RequireModule` 的判定与网关透传身份接得上。

- 免鉴权白名单：`/auth/login`、`/auth/refresh`、`/actuator/**`、`/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html`。
- 错误码口径：401 一律同码同体，不透露账号是否存在；唯一性冲突与被引用的删除一律 409；非法枚举与非法排序字段 400。
- 口令只进不出：BCrypt 密文落库，任何视图都不回传；改密与重置的会话吊销范围见下表。

| 会话动作       | 撤销范围                                                      |
| -------------- | ------------------------------------------------------------- |
| 登出           | 本次 access `jti` + 入参 refresh `jti`                        |
| 自助改密       | 该用户除当前会话外的全部会话                                  |
| 管理员重置口令 | 该用户全部会话                                                |
| 强制下线       | 目标会话谱系的双 `jti`（成对，否则会被 `/auth/refresh` 复活） |

### OpenAPI 即对外契约

`/v3/api-docs` 由 `ZenAdminOpenApiConfiguration` 补 `bearerJwt` 安全方案、直连与经网关两条 server、按注解推导的 400/401/403，以及带服务名前缀的 `operationId`。

- **方法上不写 `@ApiResponses`**：实测那会让 springdoc 不再自动生成成功响应、`ApiResponse*` 模型连带消失、文档里的 `$ref` 悬空。
- `OpenApiDocContractTest` 的期望从注解自动推导，新增接口不需手改测试，但会拦住漏标 `@SecurityRequirement` / `@Tag`。它需要完整上下文，标 `@Tag("integration")`：本机 `check` 不跑，CI 的 integration job 跑。
- 文档的 server 与版本可用 `zen.api-docs.direct-url` / `gateway-url` / `version` 覆盖。

## 关键实现

- **身份来源是 `jwt`**：本服务自己验签，与网关共用同一把 `zen.jwt.secret`（生产由 `ZEN_JWT_SECRET` 覆盖，UTF-8 ≥ 32 字节）。`access-ttl: 30m` / `refresh-ttl: 7d`。
- **不引 `spring-boot-starter-security`**：只用 `spring-security-crypto` 的 `BCryptPasswordEncoder`，401/403 仍由 `GlobalExceptionHandler` 统一输出。
- **撤销的写侧在这里**：`RedisTokenRevocationChecker` 按剩余有效期把 `jti` 写进 Redis，读侧是网关；键名由 `common-security` 的 `TokenRevocationChecker.blacklistKey(jti)` 单点定义。
- **登录失败锁定**：`zen.security.login` 控制阈值与时长（默认窗口 15m 内 5 次失败锁定 15m），达阈值那次与锁定期内的尝试一律 429；Redis 故障 fail-open 放行。
- **每次登录尝试都记 `t_login_log`**：reason 词表 `SUCCESS` / `BAD_CREDENTIALS` / `LOCKED` / `DISABLED`，落库失败只记 ERROR 不阻断认证。
- **会话 = refresh 令牌谱系**：`SessionService.recordLogin` 建登记、`rotate` 迁移、`removeOnLogout` 清理；Redis 三处布局与轮转语义见 [`docs/architecture.md`](../docs/architecture.md)。
- **簿记可降级、吊销 fail-closed**：登录登记 / 轮转迁移 / 登出清理失败只记 WARN；强制下线与连带吊销失败上抛并回滚改密事务。
- **`/auth/me` 回查库内最新资料**，返回 `UserProfileView`（资料 + 升序 `roleIds`，不含 modules 与审计列）；菜单授权不在本接口范围。
- **自助改密的新口令口径与用户侧一致**（8-72 字符），旧口令字段不设长度下限。
- **启停是单接口** `PATCH /users/{id}/status`（body `{"status":0|1}`）；删除是逻辑删除，有意不做「仍持有角色」的反向引用校验。
- **`deleted` 列不映射到实体**：删除走 native `UPDATE ... SET deleted = 1`，实体只靠 `@SQLRestriction` 过滤（见 `V1__init_auth.sql` 头注释）。
- **操作审计用 `HandlerInterceptor` 而非 AOP**：`OperationLogInterceptor` order 排在安全拦截器之后，`afterCompletion` 逆序最先回调、此时 `UserContext` 尚未清除；`module` 列取同方法 `@RequireModule` 推导，缺该注解则跳过落库并记 ERROR。
- **表结构归 Flyway**：`V1` 建表、`V2` 灌种子、`V3` 加资料列、`V4` 建 `t_login_log`、`V5` 建 `t_operation_log`；两张日志表只写不读、无 `deleted` 列。

## 架构约束

`./gradlew :admin-service:architectureTest`，规则清单见 `admin-service/src/test/java/**/ArchitectureTest.java`：

- `controller → service → repository → entity` 单向；controller 不得依赖持久化实体。
- `@Transactional` 只出现在 `service` 层。
- `*Controller` / `*Service` / `*Repository` / `*Entity` 各归其包；包切片之间无循环依赖。
- 模块级通用条（构造器注入、禁 `System.out`/`printStackTrace`、禁 `new Date()`）见 [`AGENTS.md`](../AGENTS.md)。

## 主要依赖

`common-core`（以 `api` 透传 `common-security`）、Spring Boot Web / Validation / Data JPA / Data Redis、Flyway + `flyway-mysql`、Nacos Discovery、actuator + prometheus registry、`springdoc-openapi-starter-webmvc-ui`、`spring-security-crypto`、MySQL Connector、Lombok。

## 命令

| 命令                                               | 说明                                                       |
| -------------------------------------------------- | ---------------------------------------------------------- |
| `./gradlew :admin-service:bootRun`                 | 启动服务（28081）                                          |
| `./gradlew :admin-service:bootTestRun`             | 以 `application-test.yml` 启动                             |
| `./gradlew :admin-service:bootJar`                 | 产出 `admin-service.jar`                                   |
| `./gradlew :admin-service:bootBuildImage`          | 构建 OCI 镜像                                              |
| `./gradlew :admin-service:test -PintegrationTests` | 连同依赖本机 MySQL/Redis/Nacos 的 `@SpringBootTest` 一起跑 |
| `./gradlew :admin-service:architectureTest`        | 只跑本模块架构约束                                         |
