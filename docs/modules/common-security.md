# common-security 决策与坑

[`common-security/README.md`](../../common-security/README.md) 只放事实与结论，本文放**为什么这样选、否掉了什么、代价在哪**。跨模块必须同时成立的约定在 [`docs/architecture.md`](../architecture.md)，这里不重复。

## 决策

### 把安全内核从 `common-core` 里拆出来

- 背景：Phase 1 只有 Servlet 服务，JWT 与统一响应都住在 `common-core`。Phase 2 的网关是 WebFlux，要复用同一份验签代码，而 `common-core` 的 `api` 把 `spring-boot-starter-web` / `-data-jpa` 一起拖了过去。
- 选项：网关逐条 `exclude`（拆模块前的实际做法：排掉 `starter-web` / `-validation` / `-data-jpa` 三项，现已全部移除）/ 拆一个 Web 无关的内核模块。
- 结论：拆。`common-security` 只放不含传输层依赖的东西，网关直接依赖它，一个 `exclude` 都不需要。
- 代价：多一个模块，且这条边界必须靠门禁维持——`ArchitectureTest` 里的 `moduleStaysFreeOfWebAndPersistence` 就是它的存在理由本身。收益是同一份验签代码两端各跑一次，改一处两边同时生效。

### 按包禁全，而不是只禁 `jakarta.servlet`

- 背景：拆模块前 `common-core` 有一条 `jwtPackageStaysServletFree()`，只盯 `jakarta.servlet`。
- 结论：包独立成模块后规则同步收紧为 `moduleStaysFreeOfWebAndPersistence()`，按包禁 `jakarta.servlet` / `jakarta.persistence` / `jakarta.validation` / `springframework.web` / `springframework.data` / `springframework.transaction` / `hibernate`。
- 理由：只禁 Servlet 挡不住 `spring-webmvc`、JPA 与 validation 混进来，而那三者同样会把 `exclude` 清单还给消费方。
- 配套：消费方视角另有 `gateway-service` 的 `GatewayDependencyIsolationTest`，同一条边界两端各断言一次。

### JWT Bean 以「有没有密钥」为开关，而不是「哪种 Web」

- 背景：网关和 Servlet 服务都要能装配 `JwtTokenVerifier`，但只有签发方需要密钥。
- 选项：按 Web 类型条件化 / 按 `zen.jwt.secret` 是否存在条件化。
- 结论：后者（`ZenJwtAutoConfiguration`）。不配密钥的服务整个配置类不评估，既不会启动失败，也不会拿到一个不能用的 verifier。
- 理由：真正决定「这个服务要不要碰 Token」的是它持不持有密钥，与它是 Servlet 还是 Reactive 无关。
- 代价：条件不满足是静默跳过，见坑清单里「密钥置空与密钥缺失是两条路」。

### 双令牌靠 `typ` claim 区分，共用一套密钥与一个签发器

- 背景：refresh Token 存活 7 天，绝不能被拿去访问业务接口。
- 选项：两套密钥 / 两个签发器 / 同一个签发器打 `typ` 声明并由验签方要求期望值。
- 结论：第三种。`JwtTokenIssuer#issue` 一次产出 `TokenPair`，`JwtTokenVerifier#verify(token, expectedType)` 的 `expectedType` 是必填参数——没有「不检查用途」这条调用路径。
- 代价：`typ` 缺失或值不认识同样归一为 401，与签名错误不可区分（这是刻意的：不向外透露 Token 到底哪一步不对）。

### 密钥推导收进包私有的 `JwtKeys`

- 背景：`JwtTokenIssuer` 与 `JwtTokenVerifier` 各自都要把 `String secret` 变成 `SecretKey`。
- 结论：两处都调 `JwtKeys.hmacKey(properties)`，类本身包私有 + 私有构造。
- 理由：UTF-8 取值、空白判定、长度下限这三件事只要有一处漏了，两侧就会对同一把密钥达成不同结论——表现为「admin 签的 Token 网关验不过」，而排查方向会被引到网络与配置上。

### 撤销契约是接口，键名是唯一来源，记录靠 TTL 自动过期

- 背景：撤销必须能被两端复用——写侧 `admin-service`（有 Redis），读侧网关（反应式 Redis client）。
- 选项：本模块直接引 Redis / 只定义接口。
- 结论：只定义 `TokenRevocationChecker`，`revoke(jti, ttl)` 按剩余有效期写入、记录随 TTL 自然过期，不做清理任务。
- 关键约束：`BLACKLIST_KEY_PREFIX` 与 `blacklistKey(jti)` 是本模块唯一的键名来源。两侧各拼一遍字符串时，写错不会编译失败也不会报错，只会让登出的 Token 在网关侧继续可用——属安全失效，所以收敛到单点（同一个道理见 `TrustedHeaders`）。
- 代价：缺实现时得有个兜底，而这个兜底天然是「不校验撤销」，见坑清单。

### 错误码只放 HTTP 语义码，业务码段留给消费方

- 背景：公共模块要能表达「未认证」「资源不存在」，但不可能预知各业务的失败语义。
- 结论：`GlobalErrorCode` 只有 11 个 HTTP 语义码；业务服务自建枚举实现 `ErrorCode` 扩展自己的码段。
- 代价：没有码段登记表，两个服务撞上同一个业务码值不会被拦，见坑清单。

### 契约类型是 record，集合格用紧凑构造器归一

- 背景：`roles` / `modules` 来自 Token claim 或透传头，可能为 null，也可能是调用方传进来的可变 `List`。
- 选项：`Collections.unmodifiableList` 包装 / 可变 DTO 加 setter / record + `List.copyOf`。
- 结论：record + 紧凑构造器（null → `List.of()`，否则 `List.copyOf`）。对外承诺不可变，且不需要每个构造点自觉。
- 配套：这也是 SpotBugs 的免豁免样板——`VerifiedToken` 用这个写法消掉 4 条 EI_EXPOSE_REP 类告警，而不是去 `gradle/spotbugs/exclude.xml` 申请豁免。SpotBugs 政策本身见 [`AGENTS.md`](../../AGENTS.md)。

### 验签不查黑名单

- 背景：`JwtTokenVerifier` 是纯函数（Token 进、声明出），而撤销状态在 Redis 里。
- 结论：验签只保证签名/过期/`typ`，撤销检查由持有 Redis 的一侧在同一层做（Servlet 侧 `JwtUserContextResolver`，网关侧 `JwtAuthGlobalFilter`）。
- 理由：本模块一旦为撤销而依赖 Redis，就同时依赖了某个具体 client，Web 无关这条存在理由当场破功。
- 代价：「验签通过」不等于「可用」，两处调用点都必须记得串上撤销检查（Servlet 侧 `isRevoked`、网关侧 `TokenBlocklist#isBlocked`），漏一处就是撤销静默失效。

## 坑

- **`noClasses()` 在一个类都没导入时也判通过。** 现象：包根改名、目录误删、`importPackages` 写错，都会让本模块所有边界规则一起变成零断言绿——CI 全绿而约束早已不存在。做法：`importedClassesAreNeverEmpty()` 哨兵断言导入类数 > 20（拆模块时实测 19 的下一档）。目前只有本模块与 `ecs-service` 带哨兵，`common-core` / `gateway-service` / `admin-service` 是已知空白，新模块建议照抄。

- **密钥缺失与密钥置空是两条完全不同的路。** 现象：`ZEN_JWT_SECRET=` 让属性**存在**，条件成立 → `JwtKeys.hmacKey` 抛 `IllegalStateException`，服务启动失败；而完全不写这个键是整个配置类不评估，服务正常启动。做法：不需要 JWT 的服务不要留一个空值的环境变量；排查启动失败时先分清这两种表现。

- **密钥长度按 UTF-8 字节数算，不是字符数。** 现象：非 ASCII 密钥（中文短语）字符数看着够，`secret.getBytes(UTF_8)` 却短于 32，`Keys.hmacShaKeyFor` 抛 `WeakKeyException`，同样是启动期失败。做法：`zen.jwt.secret` 一律用 ASCII，长度以 32 字节为下限（口径见 README）。

- **两个 `remainingTtl()` 同名不同义。** 现象：`VerifiedToken#remainingTtl()` 的 `expiresAt` 恒非空、已过期返回 `Duration.ZERO`；`UserPrincipal#remainingTtl()` 在网关透传模式下返回 `null`。把后者的返回值直接喂给 `TokenRevocationChecker.revoke(jti, ttl)` 不会炸——admin 的实现把 null/零/负 TTL 一律静默跳过，撤销不发生且无日志，比抛 NPE 更难查。做法：拿 `UserPrincipal` 的剩余有效期必须先判 null（该方法的 Javadoc 已写明「调用方据此跳过撤销」）。

- **`jjwt-impl` / `jjwt-jackson` 是 `runtimeOnly`，编译期看不出缺失。** 现象：谁把它们排掉或没带上，代码照样编译通过，第一次 `Jwts.builder()` / `Jwts.parser()` 才在运行期报找不到实现。做法：消费方不要对 jjwt 坐标做任何 `exclude`；网关的 `GatewayDependencyIsolationTest` 是这道边界上唯一能提前发现的门禁。

- **失败响应里没有 `data` 字段。** 现象：`ApiResponse` 标了 `@JsonInclude(NON_NULL)`，`failure(...)` 产出的 JSON 是 `{"code":..,"message":".."}`，`data` 键整个不存在；按「一定有 `data` 且为 null」写的客户端解析会拿不到字段。做法：跨服务契约一律按「`data` 可缺省」表达（admin 侧 OpenAPI 的 example 即真实输出）。

- **`ErrorCode` 扩展点没有码段登记处。** 现象：两个服务各自新增同值的业务码不会有任何冲突提示，前端只能靠 `code` 猜是哪个服务回的。做法：新增业务码前人工核对其他服务的 `*ErrorCode` 枚举。删除条件：有一份码段分配表，或有一条门禁断言各服务业务码落在互不重叠的区间。

- **`sub` 必须是数字串，否则一律 401。** 现象：`JwtTokenIssuer` 把 `userId` 转成字符串写进 `sub`，`JwtTokenVerifier#userId` 又 `Long.parseLong` 回去，非数字主体（设备、服务账号）在两端都被拒——签发侧是 `long` 类型挡死，验签侧是 401 挡死。做法：要支持非用户主体就得先给 claim 布局加一个主体类型维度，不要复用 `sub`。
