# admin-service 决策与坑

[`admin-service/README.md`](../../admin-service/README.md) 只放事实与结论，本文放**为什么这样选、否掉了什么、代价在哪**。跨模块必须同时成立的约定在 [`docs/architecture.md`](../architecture.md)，这里不重复。

## 决策

### 身份来源用 `jwt`，不信任透传头

- 背景：`zen.security.context-source` 有 `jwt` 与 `gateway-header` 两种。作为签发方，admin 是链路上唯一持有密钥的服务。
- 选项：自己验签（`jwt`）/ 信任网关透传的 `X-User-*`（`gateway-header`）。
- 结论：`jwt`。登出与 `/auth/me` 需要 `jti` 与剩余有效期，这些信息在透传头里没有；且基线服务必须能在不经网关时自证。
- 代价：与网关共用同一把 `zen.jwt.secret`，两边不一致就全线验签失败（口径见 [`docs/architecture.md`](../architecture.md)）。

### 只引 `spring-security-crypto`，不引 starter

- 背景：需要 BCrypt 比对口令。
- 选项：`spring-boot-starter-security` / 只有 `spring-security-crypto`。
- 结论：后者，`ZenAdminAuthConfiguration` 只声明一个 `PasswordEncoder` Bean。
- 代价：过滤器链、密码策略、CSRF 等都得自己判断要不要。收益是 401/403 仍由 `GlobalExceptionHandler` 统一输出，不被 starter 的默认链接管。

### 不声明全局 `security`，逐接口标 `@SecurityRequirement`

- 背景：文档必须说出「要带 Bearer Token」，否则前端与 SDK 生成器只能猜。
- 选项：全局 `security` + 逐接口覆盖 / 只逐接口标注。
- 结论：只逐接口。全局写法要依赖 springdoc 的覆盖语义才能把 login/refresh 表达成匿名入口，逐接口直接把「谁要鉴权」写清楚。
- 代价：新增接口漏标就是文档口径错，`OpenApiDocContractTest` 以集合差集拦住。

### 错误响应集中推导，方法上不写 `@ApiResponses`

- 背景：400/401/403 需要在文档里可见。
- 选项：逐方法 `@ApiResponses` / `ZenAdminOpenApiConfiguration.apiErrorResponses()` 按注解推导。
- 结论：推导。规则是「有请求体 →400、有鉴权要求 →401、有 `@RequireModule`→403」。
- 实测代价：逐方法写 `@ApiResponses` 会让 springdoc **不再自动生成成功响应**，`ApiResponse*` 模型连带从 `components.schemas` 消失，文档里的 `$ref` 悬空。这条是踩出来的，不是设计偏好。
- 配套：错误响应体指向 `#/components/schemas/ApiResponseVoid`，组件名由 springdoc 按泛型展开推导——把登出接口的返回类型改掉就会让它消失，靠契约测试把这种漂移变成构建失败。

### 错误响应的 example 取真实输出

- 背景：example 是 Apifox 用例断言与 mock 的直接依赖。
- 结论：文案取自 `GlobalErrorCode` 与 `GlobalExceptionHandler` 的实际输出并逐条本机验过，不是编的示例值。
- 理由：写错的 example 比不写更糟——它会被人当真相复制。

### `operationId` 加服务名前缀

- 背景：springdoc 取的是裸方法名（`list`、`detail`、`admin`…），admin/ecs/wcs/rcs 的文档并进同一个 Apifox 项目或同一份 SDK 时必然撞名。
- 结论：`namespacedOperationIds` 以 `${spring.application.name}-` 为前缀，且**幂等**（已带前缀不叠加）、`operationId` 缺失时按「方法 + 路径」补确定值。
- 关键实现约束：必须是 `GlobalOpenApiCustomizer` 而非 `OperationCustomizer`——springdoc 在全局阶段才写 `operationId`，逐接口阶段拿到的是 null，前缀在那种写法下完全不生效；同时要 `Ordered.LOWEST_PRECEDENCE` 排在 springdoc 自己的 OperationIdCustomizer 之后，否则会被它写回的裸方法名覆盖。

### 显式声明两条 server

- 背景：springdoc 默认按「当前请求」推导 server——谁在哪个地址访问就写成哪个，多环境下不稳定。
- 结论：显式列直连与经网关两条，可用 `zen.api-docs.direct-url` / `gateway-url` / `version` 覆盖。
- 代价：端口改了要记得同步默认值。

### `default-produces-media-type` 与不开 `use-fqn`

- 背景：文档要能被 Apifox 定时同步与 SDK 生成器消费。
- 结论：`default-produces-media-type: application/json`——默认 `*/*` 让工具拿不到 json 语义，而接口实际只产 json；`writer-with-order-by-keys: true` 让文档 JSON 成为可 diff 的稳定输入；刻意不开 `use-fqn`，否则模型名变成 `com.zen.admin.dto.LoginRequest` 这种长串，每个工具里都要重读一遍。
- 代价：跨服务并档只靠 `operationId` 前缀防撞；真撞上同名 schema，在涉事 DTO 上写 `@Schema(name = ...)` 定点改，不全局切换命名策略。
- 配套：`packages-to-scan: com.zen.admin`，防止 `common-core` 的 `@RestControllerAdvice` 与实体被当成本服务的接口模型。

### 认证失败一律 401 同码同体

- 背景：用户名不存在与密码错误如果响应不同，等于开放了一个账号枚举接口。
- 结论：`username` 不存在、密码错误、Token 缺失/签名非法/已过期都是 401 默认文案。`/auth/me` 遇到已被逻辑删的用户同样 401——`@SQLRestriction` 让 `findById` 直接返回 empty，等价于「登录已失效」，不透露账号是否存在。
- 代价：排查时分不清「没登录」和「被删了」，要看 `t_login_log`。

### 登录锁定：达阈值那次响应 429，日志仍记 `BAD_CREDENTIALS`

- 背景：第 5 次失败既是「一次凭据错误」又是「锁定生效的时刻」，响应和审计各自要表达哪个？
- 结论：响应给 429「账号已锁定,请稍后重试」（G4-口径），日志忠实记录该次尝试本身即 `BAD_CREDENTIALS`。锁定生效由响应表达，审计不失真。
- 理由：如果日志记 `LOCKED`，运维看到的就是「用户没输错过密码却被锁」，归因错。

### 达阈值时同步删除计数键

- 背景：锁标记与失败计数是两个键（`auth:lock:{username}` / `auth:fail:{username}`）。
- 结论：写锁的同时 `DELETE` 计数键（G4-2）。
- 否则：解锁后的第一个窗口会残留历史计数，一次失败即再次锁定，表现为「用户被永久间歇性锁定」。

### 锁定对 Redis 故障 fail-open

- 选项：Redis 异常时拒绝登录（安全优先）/ 放行（可用性优先）。
- 结论：`RedisLoginAttemptStore` 内部降级——判锁返回 `false`（放行）、计失败返回 `false`（不触发锁定）、`reset` 静默。
- 理由：否则 Redis 一挂无人能登录，把锁定的可用性变成登录的可用性。这与网关黑名单读取的 fail-closed 方向相反，是刻意的：**撤销失败不能放行，锁定失败可以让行**。

### refresh 路径必须自己查黑名单

- 背景：`JwtTokenVerifier` 只校验签名/过期/`typ`，不查黑名单（它属于无状态的 `common-security`，不依赖 Redis）。
- 结论：`AuthService.refresh` 显式调 `revocationChecker.isRevoked(...)`，命中即 401。
- 否则：被登出的 refresh Token 仍能换新，等于登出无效。

### 会话 = refresh 令牌谱系，而非 access

- 背景：一个浏览器标签在 7 天内会持有一串轮换过的 refresh Token 和很多个 access Token。「会话」该绑哪个标识？
- 选项：绑 access `jti`（每次换新就变）/ 绑自造的浏览器 ID（客户端可信度存疑）/ 绑 refresh 谱系（登录时生成 `sessionId`，轮转时迁移不变）。
- 结论：第三种。`sessionId` 由服务端在登录时生成，轮转只迁移登记，谱系断掉（refresh 过期或被撤销）即会话结束。
- 关键推论：**强制下线与连带吊销必须成对拉黑 `currentRefreshJti` + `lastAccessJti`**。只拉黑 access 会被 `/auth/refresh` 复活；只拉黑 refresh 则已签出的 access 仍可用到过期。

### 轮转迁移保持 `sessionId` 与 `issueTime`（决策 (b)）

- 背景：轮转后如果 `issueTime` 也跟着刷新，「这次登录是什么时候发生的」就永久丢失，会话列表只剩最后一次续期时间。
- 结论：登记键 TTL、ZSET score、`expireTime` 三者随新 refresh 有效期一并迁移；`sessionId` 与 `issueTime` 不动。
- 代价：会话可能连续存在 7 天以上（每次续 7 天），`issueTime` 会变成「首次登录」而非「最近活跃」。当前不做最后活跃时间——那要每次请求都写 Redis。
- 配套：access 的拉黑 TTL 以会话 `expireTime` 剩余为上界，不在 `SessionInfo` 里存 access 自身的 `exp`（30m 的 access 过期时刻对吊销决策没意义，上界才是）。

### 簿记降级、吊销 fail-closed（决策 (c)）

- 背景：Redis 抖动时，会话簿记和吊销失败应该阻断认证主流程吗？两类动作的答案相反。
- 选项：两类都上抛 / 都降级 / 分界。
- 结论：分界。登录登记、轮转迁移、登出清理三个**簿记**动作 catch `RuntimeException` 只记 WARN；强制下线与连带吊销**一律不 catch**，失败上抛回滚外层 `@Transactional`。
- 理由：簿记缺失只影响可观测性，靠 TTL 自然过期兜底，不该反噬登录；而静默吞掉吊销失败意味着旧谱系在改密事务提交后仍能续期，等于安全承诺落空——「吊销不了就不改密」。
- 实现细节：这三个簿记方法只在认证成功之后执行，所以 catch `RuntimeException` 不会吞掉任何业务异常，这是它能安全降级的前提。

### 死会话只在读路径清理，不建后台任务

- 背景：ZSET 索引里的成员过期后会成为死项。
- 选项：定时任务 `ZREMRANGEBYSCORE` / 每次读之前顺手清。
- 结论：`RedisSessionRegistry.purgeExpired()` 在读路径执行，不建后台任务。
- 理由：登记键自身带 TTL，索引剔除后残留 JSON 会自然过期，两侧不需要强一致；多一个后台组件就多一处失败模式。
- 配套竞态处理：登记键先于索引项过期时 `multiGet` 会拿到 null payload，`load` 跳过该条；登记键先到期而反向映射残留时，`rotate` 顺手 `unbindRefresh` 解除悬挂。

### 不建「按用户检索会话」的第三索引

- 背景：`revokeAllForUser*` 需要按用户找会话，当前是扫 ZSET 活成员逐条过滤。
- 结论：不加 `auth:session:user:{userId}` 这类索引。
- 理由：在线会话是 admin 后台量级；多一层键就多一处与主登记的一致性负担，而这套登记的簿记路径是允许降级丢数据的——索引一旦可能滞后，就不能用来做安全判定。

### 自助改密连带吊销，但保留当前会话

- 背景：改密后旧谱系不该还能续期。但如果把调用人的会话也吊销，用户改完密码立刻被踢，表现为「改密失败」。
- 结论：吊销该用户除当前会话外的全部会话（`revokeAllForUserExcept(userId, currentAccessJti)`）；管理员重置口令则吊销**全部**（`revokeAllForUser`）。
- 这条取代了 Phase 4 的「改密不吊销」取舍——撤销基建在 Phase 5 才具备，早期那句承诺是能力缺失的包装。`AuthController.changePassword` 的 `@Operation` 描述已同步为轮转后口径。

### 自助改密不校验旧口令的长度下限

- 背景：新口令口径 8-72 字符，与建号/重置一致。
- 结论：旧口令字段**不设**长度下限。
- 理由：历史短口令用户不该被挡在改密入口外——校验旧口令的长度对安全毫无增益（攻击者本来就不读校验注解），却会制造「改不掉旧口令」的死锁。

### `/auth/me` 回查库，不回显 Token 快照

- 背景：Token 里的 roles/modules 是签发时刻的快照。
- 结论：按 `UserContext` 身份回查库内最新资料，返回 `UserProfileView`（资料 + 升序 `roleIds`）。
- 理由：资料接口必须反映当前状态，角色刚被改过时回显 Token 就是错的。
- 边界：**不含 `modules`** —— 前端菜单授权不在本接口范围（modules 的真相只在 Token 快照里，两者语义不同，混在一个响应里迟早有人拿错的那个）。也不含 `createTime` 等审计列。

### 启停是单接口 `PATCH /users/{id}/status`

- 背景：可选 `POST /enable` + `POST /disable`，或一个 `PATCH` 带 `{"status":0|1}`。
- 结论：单接口。状态只有两态，拆两个路由没有额外信息，反而多一份白名单与审计口径要对齐。
- 代价：合法性要靠 body 校验（`UserStatusRequest`）而不是路由保证。

### 删用户有意不做角色反向引用校验

- 背景：删角色时「仍被有效用户引用」返回 409，删用户时却不校验角色，两侧不对称。
- 结论：不对称是刻意的。
- 理由：删用户不产生悬空引用——`t_user_role` 的关联行随用户的 `deleted` 标记一起失去可见性（`@SQLRestriction` 让两侧 JOIN 都查不到），不需要反向清理；而删角色若被引用，会留下「用户指向一个不存在的角色」的真悬空态，两者风险不同。
- 代价：读代码的人会把这当成漏实现，因此在 controller 的 Javadoc 与本文各留一句。

### 口令只进不出

- 背景：`UserView` 与 `UserProfileView` 都没有 password 组件。
- 结论：BCrypt 密文只落库，任何查询接口都不回传；`PUT /users/{id}/password` 是管理员重置，不校验旧口令。
- 理由：密文外泄即离线爆破风险，与权限高低无关。
- 代价：管理员无法「查看」口令，只能重置——这本来就不该是能力。

### `deleted` 列不映射到实体

- 背景：逻辑删除需要一个开关列。
- 选项：实体上映射 `deleted` 字段 + service 里手写过滤 / 不映射，实体只挂 `@SQLRestriction("deleted = 0")` 过滤，删除走 native `UPDATE ... SET deleted = 1`。
- 结论：不映射。
- 理由：一旦映射，任何一次 `save` 都可能把内存里的 `deleted = 0` 写回去，「误恢复」是静默的；不映射则实体层根本没有把它改回来的能力。
- 代价：`UserEntity` / `RoleEntity` 上看不见这个列，读实体的人要回 DDL 才知道有软删（见 `V1__init_auth.sql` 头注释）；删除接口不能走 `repository.delete()`，得留一条 native update。

### 审计日志表不挂软删

- 背景：`t_login_log` / `t_operation_log` 都继承 `BaseEntity`（有 `creator`/`updater` 等公共列）。
- 结论：两张表**没有** `deleted` 列，实体也不挂 `@SQLRestriction`。
- 理由：日志是追加写的审计流水，能被逻辑删除的审计等于没有审计。
- 代价：与业务表的惯例不一致，DDL 头注释各留一句说明，避免被「补上」。

### 操作审计用 `HandlerInterceptor`，不引 AOP

- 背景：需要记录写接口的结果码。
- 选项：AOP 切面 / `HandlerInterceptor.afterCompletion`。
- 结论：`OperationLogInterceptor`，与 `ModuleAuthInterceptor` 同构，不新增 AOP 依赖与切点表达式。
- 关键约束：**注册顺序必须排在安全拦截器之后（order 更大）**。`afterCompletion` 按注册逆序回调，逆序后它最先执行，此时 `UserContext` 尚未被 `AuthInterceptor` 清除，才拿得到操作人。顺序写反不报错，只会让审计行的 operator 为空。
- 边界：不吞业务异常——业务异常在进 `afterCompletion` 前已由 `GlobalExceptionHandler` 解析为响应，这里只观察状态码。

### `module` 列由 `@RequireModule` 推导，缺注解即跳过

- 背景：审计表要记模块。
- 选项：`@OperationLog(module = ...)` 再写一遍 / 从同方法的 `@RequireModule` 取 `getCode()`。
- 结论：推导。两处各写一份必然漂移，而模块权限本来就是这个接口存在的运行期事实。
- 代价：`@OperationLog` 隐含要求同方法有 `@RequireModule`，缺了会跳过落库并记 ERROR——这个错只在运行期暴露，靠日志而非编译发现。

### `user_agent` 落库前按列宽截断

- 背景：`t_login_log.user_agent` 是 `VARCHAR(256)`，浏览器 UA 可以远超这个长度（部分插件会拼出 1KB 以上）。
- 结论：`AuthService.USER_AGENT_MAX_LENGTH = 256`，写库前截断。
- 理由：否则整条登录日志因字段宽度写不进，一次超长 UA 就丢掉一条认证审计。
- 代价：常量与 DDL 是**手工耦合**，改列宽要同步改常量，见本文坑清单。

### 真实客户端 IP 取 XFF 最后一个值

- 背景：`X-Forwarded-For` 是客户端可写的头，链路上每一跳都可能追加值。
- 结论：取最后一个值——网关 `JwtAuthGlobalFilter` 已剥离入站伪造并以连接对端重建，因此最后一个值是网关认定的客户端。
- 边界：不经网关直达时没有可信 XFF，回落 `getRemoteAddr()`，记的是连接对端。完整口径见 [`docs/architecture.md`](../architecture.md)。

### 契约测试的期望从注解推导

- 背景：`OpenApiDocContractTest` 要保证文档与实现一致。
- 选项：测试里手维护一份接口清单 / 从运行期注解推导。
- 结论：推导——接口集合取 `RequestMappingHandlerMapping`，403 集取方法上的 `@RequireModule`，security 取两级 `@SecurityRequirement`，tags 取类上 `@Tag`。新增 controller 不需要手改测试。
- 收益仍在：漏标 `@SecurityRequirement`/`@Tag`、或 springdoc 升级改了生成口径，都会以集合差集的形式失败。框架端点（`/v3/api-docs*`、`/swagger-ui*`、`/error`）由 `isDocInfrastructure` 剔除。
- 代价：它需要完整 Spring 上下文，因此标 `@Tag("integration")`——本机 `check` 不跑，CI 的 integration job 跑。文档一致性的守护是异步的。

### 覆盖式写关联，空数组即收回全部

- 背景：`PUT /roles/{id}/modules` 与 `PUT /users/{id}/roles` 要改关联集合。
- 结论：整体替换（覆盖式写 `t_role_module` / `t_user_role`），空数组是合法输入且语义明确 = 收回全部。
- 理由：PATCH 式的增删差异要引入两套语义，对后台配置类接口是净负担。
- 代价：并发覆盖不可检测（后写者胜），当前没有乐观锁。

### 审计人覆盖为真实登录用户

- 背景：`common-core` 提供默认审计人 `system`。
- 结论：`ZenAdminAuthConfiguration.auditorAware()` 从 `UserContext` 取当前用户名；自动配置在用户 Bean 之后评估，会自动退让，不需要 `@Primary`。

### 种子口令走 Flyway placeholder

- 背景：种子账号需要 BCrypt 密文，而开发口令是公开的两个值。
- 结论：`application.yml` 的 `spring.flyway.placeholders` 存开发密文，生产由 `SPRING_FLYWAY_PLACEHOLDERS_ADMINPASSWORD` / `..._DEMOPASSWORD` 覆盖，不改文件。
- 理由：迁移脚本里不出现任何明文口令，也不为了换口令去改已应用的脚本。
- 代价：placeholder 值一旦变更，已建好的库不会重跑 `V2`，改种子口令要清库或补一条新迁移。

## 坑

- **`Pageable.withSort` 在本项目 Spring Data 版本不存在。** 做法：排序兜底照 `RoleService` 走 `query.setOrderBy`，不要写 `withSort`（编译期才发现）。

- **Mockito 打桩 `findAll(Specification, Pageable)` 有重载歧义。** 做法：`ArgumentMatchers.<Specification<XxxEntity>>any()` 显式消解类型参数，否则 stub 命中到别的重载上，测试假绿。

- **`ddl-auto: validate` 让实体与迁移脚本强耦合。** 现象：实体加字段忘了写迁移，`@SpringBootTest` **整片**挂掉，不是只挂新测试。做法：加列必须同步新增 Flyway 迁移，列名/长度/可空性与实体完全一致。

- **Gradle 的 `-P` 属性不构成任务输入。** 现象：改过代码后 `./gradlew :admin-service:test -PintegrationTests` 可能整片 `UP-TO-DATE`——属性变了不等于输入变了，任务缓存判定只看声明的输入输出。做法：要确认 integration 真跑过就加 `--rerun-tasks`。

- **真实登录的集成用例必须在 `finally` 清理 `auth:fail:{username}`。** 现象：15 分钟窗口内累计 5 次失败会锁死共享开发库的 `admin`，殃及后续所有登录用例，且表现是 429 而非断言失败，很容易被误读成代码坏了。做法：凡走真实登录流程、或改密/吊销路径可能产生失败尝试的用例（`AuthMeIntegrationTest`、`SessionRegistryIntegrationTest`）收尾时 `DELETE` 该键。

- **注入的 `ObjectMapper` 必须是 Jackson 3（`tools.jackson`）。** 现象：注入 `com.fasterxml.jackson.databind.ObjectMapper` 直接 `No qualifying bean`——Boot 4 自动装配的是 `tools.jackson` 那个，Jackson 2 只为 jjwt-jackson 留在类路径。做法：`RedisSessionRegistry` 的注入类型即为准；Jackson 3 异常是 unchecked，`writeValueAsString`/`readValue` 不需要 try/catch，`Instant` 由核心原生支持。gateway 的 `ApiJsonResponses` 有同款实测记录。

- **`BEARER_JWT_SCHEME` 写岔不报错。** 现象：字符串写岔只会让文档引用一个未定义的安全方案，Apifox 侧表现为「鉴权又没了」，属静默失效。做法：名字收敛成 `ZenAdminOpenApiConfiguration` 的常量，controller 一律引用常量而非字面量。它是编译期常量，会被内联进注解，因此 controller 在字节码上不依赖 config 包。

- **`ERROR_ENVELOPE_REF` 依赖登出接口的返回类型。** 现象：`#/components/schemas/ApiResponseVoid` 的组件名由 springdoc 按泛型展开推导，把 `logout` 的返回类型从 `ApiResponse<Void>` 改掉就会让它消失，文档里所有错误响应 `$ref` 悬空。做法：`OpenApiDocContractTest` 断言每个 `$ref` 可解析，把这种漂移变成构建失败。

- **方法上加 `@ApiResponses` 会吃掉成功响应。** 现象：文档里 200 消失、`ApiResponse*` 模型连带不见、`$ref` 悬空，且 springdoc 不告警。做法：错误响应只由 `ZenAdminOpenApiConfiguration` 集中推导（见上文决策），逐方法注解这条路已验证走不通。

- **IDE 自带 Java formatter 与仓库 palantir 不一致。** 现象：刷出大片纯格式 diff，掩盖真实改动。做法：一律以 `./gradlew spotlessApply` 为准；工作区出现意外格式噪音先归一再判断。

- **JPA auditing 断言在单测里观察不到。** 现象：「`creator`/`updater` 随 `UserContext` 更新」这类断言，mock 仓储永远看不到——它只在真实 flush 时生效。做法：单测断言业务字段，审计字段放 `@Tag("integration")` 仓储测试兜底。

- **`UserStatusRequest` 之类的校验只挡 HTTP 入口。** 现象：`PATCH /users/{id}/status` 的 `0|1` 合法性来自 Bean Validation，service 内部直接调用不经过校验。做法：新增写路径时不要假设实体状态已被注解保证。
