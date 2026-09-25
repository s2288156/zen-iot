# admin-service

认证与授权基线服务（端口 28081，库 `zen_admin`）：登录 / 刷新 / 登出、Token 签发与撤销、模块权限判定示例。

## 关键实现

- **身份来源是 `jwt`**：`zen.security.context-source: jwt`，本服务自己验签，因此它与网关共用同一把 `zen.jwt.secret`（生产由 `ZEN_JWT_SECRET` 覆盖，UTF-8 ≥ 32 字节）。`access-ttl: 30m` / `refresh-ttl: 7d`。
- **撤销的写侧在这里**：登出（以及刷新时被换下的旧 refresh Token）由 `RedisTokenRevocationChecker` 按剩余有效期把 `jti` 写进 Redis 黑名单，读侧是网关的 `RedisTokenBlocklist`；键名 `auth:blacklist:{jti}` 由 `common-security` 的 `TokenRevocationChecker.blacklistKey(jti)` 单点定义，两侧不会各写一份字符串。
- **不引 `spring-boot-starter-security`**：只用 `spring-security-crypto` 的 `BCryptPasswordEncoder`。装 starter 会拉起默认过滤器链，接管 401/403 并绕过 `GlobalExceptionHandler` 的统一响应。
- **服务内白名单是裸路径**：`/auth/login`、`/auth/refresh`、`/actuator/**`、`/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html`。经网关访问时要在 `zen.gateway.auth.whitelist` 里另写带 `/api/admin` 前缀的一份。
- **OpenAPI 文档即对外契约**：`/v3/api-docs` 由 `ZenAdminOpenApiConfiguration` 补 `bearerJwt` 安全方案、直连与经网关两条 server、以及 400/401/403 响应；错误响应按「有请求体 / 有鉴权要求 / 有 `@RequireModule`」推导，**不在方法上写 `@ApiResponses`**——实测那会让 springdoc 不再自动生成成功响应、`ApiResponse*` 模型连带消失、文档里的 `$ref` 悬空。`OpenApiDocContractTest` 断言 200 仍在、每个 `$ref` 可解析、鉴权口径与上面的服务内白名单一致（它要完整上下文，标 `integration`：本机 `check` 不跑，CI 的 integration job 跑）。文档里的 server 与版本可用 `zen.api-docs.direct-url` / `gateway-url` / `version` 覆盖。
- **接口**：`AuthController`（`/auth/login`、`/auth/refresh`、`/auth/logout`）、`RoleController`（`POST /roles`、`PUT /roles/{id}`、`DELETE /roles/{id}`、`GET /roles/page`、`GET /roles/{id}`、`PUT /roles/{id}/modules`）、`UserController`（`POST /users`、`PUT /users/{id}`、`PATCH /users/{id}/status`、`DELETE /users/{id}`、`PUT /users/{id}/password`、`PUT /users/{id}/roles`、`GET /users/page`、`GET /users/{id}`）与 `DemoController`（`/demo/admin`、`/demo/ecs`）。`RoleController` / `UserController` 的写接口标 `@RequireModule(ModuleCode.ADMIN)`（403 由文档推导规则同步），读接口登录即可。角色侧：`roleCode` 重复与删除仍被有效用户引用的角色都是 409，非法模块编码 / 排序字段是 400。用户侧：`username` 重复 409，未知 roleId / 非法排序字段 400，用户不存在 404；启停是单接口 `PATCH /users/{id}/status`（body `{"status":0|1}`，状态只有两态，拆两个路由没有额外信息）；删除是逻辑删除且**有意不做**「仍持有角色」的反向引用校验（与删角色的 409 不对称：删用户不产生悬空引用，`t_user_role` 关联行随 `deleted` 标记保留）；口令只进不出——BCrypt 密文落库，`UserView` 没有 password 组件。两者的删除都走 native `UPDATE ... SET deleted = 1`（`deleted` 列故意不映射到实体，见 `V1__init_auth.sql`）。`DemoController` 不是业务功能，用来验证 `@RequireModule` 的模块权限判定与网关透传身份接得上不上。
- **表与种子数据**：Flyway `V1__init_auth.sql` 建表、`V2__seed_auth.sql` 灌角色/用户/`t_role_module` 授权、`V3__user_profile.sql` 给 `t_user` 加资料列（昵称/邮箱/手机号/头像，全可空），测试与本机起服务都靠它，不手建库。

## 架构约束

`ArchitectureTest`（`./gradlew :admin-service:architectureTest`）：

- `controller → service → repository → entity` 单向；controller 不得依赖持久化实体。
- `@Transactional` 只出现在 `service` 层。
- `*Controller` / `*Service` / `*Repository` / `*Entity` 各归其包；包切片之间无循环依赖。
- 模块级通用条（构造器注入、禁 `System.out`/`printStackTrace`、禁 `new Date()`）见 `AGENTS.md`。

## 主要依赖

`common-core`（以 `api` 透传 `common-security`）、Spring Boot Web / Validation / Data JPA / Data Redis、Flyway + `flyway-mysql`、Nacos Discovery、actuator + prometheus registry、`springdoc-openapi-starter-webmvc-ui`、`spring-security-crypto`、MySQL Connector、Lombok。

## 已知坑

- 本项目 Spring Data 版本没有 `Pageable.withSort`，排序兜底照 `RoleService` 走 `query.setOrderBy`。
- Mockito 打桩 `findAll(Specification, Pageable)` 需 `ArgumentMatchers.<Specification<XxxEntity>>any()` 消解重载歧义。
- IDE 自带 Java formatter 与仓库 palantir 不一致，会刷出纯格式 diff；一律以 `./gradlew spotlessApply` 为准，工作区出现意外格式噪音先归一再判断。
- `ddl-auto: validate` 意味着实体加字段必须同步新增 Flyway 迁移，列名/长度/可空性要和实体完全一致，否则上下文起不来（`@SpringBootTest` 全挂而不是只挂新测试）。
- 新增 controller 会打破 `OpenApiDocContractTest` 的硬编码断言：`containsKeys`、`tags` 目录、`moduleGated` 路径前缀三处都要同步加，本地不跑 integration 也会在 CI 挂。
- 「updater/creator 随 `UserContext` 更新」这类 JPA auditing 断言 mock 仓储观察不到（只在真实 flush 时生效），单测断言业务字段、审计字段放 `@Tag("integration")` 仓储测试兜底。

## 命令

| 命令                                               | 说明                                                       |
| -------------------------------------------------- | ---------------------------------------------------------- |
| `./gradlew :admin-service:bootRun`                 | 启动服务（28081）                                          |
| `./gradlew :admin-service:bootTestRun`             | 以 `application-test.yml` 启动                             |
| `./gradlew :admin-service:bootJar`                 | 产出 `admin-service.jar`                                   |
| `./gradlew :admin-service:bootBuildImage`          | 构建 OCI 镜像                                              |
| `./gradlew :admin-service:test -PintegrationTests` | 连同依赖本机 MySQL/Redis/Nacos 的 `@SpringBootTest` 一起跑 |
