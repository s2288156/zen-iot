# ecs-service 决策与坑

[`ecs-service/README.md`](../../ecs-service/README.md) 只放事实与结论，本文放**为什么这样选、否掉了什么、代价在哪**。跨模块必须同时成立的约定在 [`docs/architecture.md`](../architecture.md)，这里不重复。

## 决策

### 身份来源是 `gateway-header`，本服务不持密钥

- 背景：`zen.security.context-source` 有 `jwt` 与 `gateway-header` 两种取值。ECS 不是签发方，也不该为了读身份再拿一份密钥。
- 选项：自己验签（密钥扩散到每个服务）/ 信任网关覆写后的 `X-User-*`。
- 结论：后者。伪造入口由网关的可信头剥离堵死，这一条是跨模块契约，口径见 [`docs/architecture.md`](../architecture.md)。
- 代价：脱离网关直连本服务时，任何请求都能自报身份。这是内网边界的既设前提，不是 ECS 的选择。

### 在线态放 Redis，不放 MySQL 的一张表

- 背景：设备秒级上报心跳，同时需要一个「多久没报就算掉线」的判据。
- 选项：`t_device` 上加 `last_heartbeat_time` 由 SQL 比较 / Redis 键 + TTL。
- 结论：Redis。高频写不进关系表；键的自然过期本身就是「最近一次心跳」的载体，`RedisDevicePresenceStore` 的键值用 ISO-8601 文本，`redis-cli` 里能直接用眼看。
- 代价：在线事实分散在两处——Redis 是判据、`t_device.online` 是判定结果。所以两者必须同源于一个 `Clock`，见下条。

### 时间一律读注入的 `Clock`，并用门禁禁掉直接取时钟

- 背景：「超时自动离线」的阈值是 30s，测试若只能真等，就不会有人写这条用例。
- 结论：`ZenEcsConfiguration#clock` 是唯一时间源，`HeartbeatService` 与 `CommandDispatchService` 都注入它；`neverReadsTheSystemClockDirectly` 禁 `LocalDateTime.now()` / `Instant.now()` / `System.currentTimeMillis()`，`neverConstructsJavaUtilDate` 禁 `new Date()`。
- 理由：只禁 `new Date()` 是不够的——`Instant.now()` 看起来是「正确的 java.time」，同样隐式取了系统时钟。两条合起来才封死。

### `DevicePresenceStore` 抽一层，接受它落在四层之外

- 背景：超时判定要能测。真依赖 Redis 就得给单测起容器。
- 结论：接口在 `presence` 包，实现 `RedisDevicePresenceStore`；单测里换一个进程内假存储，拨时钟就断言离线。
- 代价：`protocol` / `presence` / `listener` / `scheduler` 都不在 `controller → service → repository → entity` 里，分层规则带 `.consideringOnlyDependenciesInLayers()`，对它们不做判定（见「坑」）。

### 逻辑删除，但编码占用按全表查重

- 背景：`t_device_event` / `t_device_command` 按 `deviceId` 引用档案，物理删行会让历史查不到主语；同时 `uk_device_code` 不区分 `deleted`。
- 选项：留给数据库抛约束冲突（500）/ service 按全表 `countBy…IncludingDeleted` 查重并抛业务码。
- 结论：后者，且文案明说「已删除的档案同样占位」。分组同理。
- 代价：复用已删编码这条路是堵死的。要放开，得同时改唯一索引（把 `deleted` 纳进去）与查重口径，只改一侧会出现「查重放行、插入撞键」。

### `deviceCode` / `groupCode` 建后不可改

- 背景：MQ 指令按 `deviceCode` 定位设备，分组编码是档案引用分组的方式。
- 结论：`DeviceUpdateRequest` / `DeviceGroupUpdateRequest` 里就没有这个字段，不靠 service 忽略它。
- 理由：入口的形状就是契约的形状。参数存在却被静默丢弃，比不给这个参数更难查。

### 协议适配是三层插槽，适配器是有状态实例

- 背景：Phase 4 要接 Modbus TCP / OPC UA，本阶段只有 `loopback` 回环实现。
- 结论：`ProtocolSupport`（认领协议 + 打开适配器）→ `DeviceAdapterFactory` → `DeviceAdapter`（`connect` / `status` / `read` / `write` / `close`）。新增协议只写一个 `ProtocolSupport` 实现注册成 Bean，既有代码不动。
- 细节：工厂由 `ZenEcsConfiguration#deviceAdapterFactory` 装配而不是标 `@Component`，让协议实现只依赖接口；构造期 `List.copyOf` 固化候选集，既免得给 `EI_EXPOSE_REP2` 开豁免，也让「没有适配器」在启动后是稳定事实。
- 代价：适配器按次创建、调用方负责 `close()`，本阶段不做连接复用。为假协议先建一层连接池不值当，Phase 4 真连设备时再补。

### 指令中转不标 `@Transactional`

- 背景：一次中转最坏要等 `max-attempts × timeout`，默认 15 秒。
- 结论：`CommandDispatchService#dispatch` 不开事务，落库拆成「插 `PENDING` 行 → 执行 → 写终态」三个独立短事务。
- 代价：进程在执行中途崩溃，会留下一条永远 `PENDING` 的行。这条留痕换来的是「哪条指令停在哪个阶段」可查，比回滚掉一个正在被设备执行的指令更可接受。

### 幂等以唯一索引为裁判，不靠先查后插

- 背景：MQ 至少一次投递，重复投递不得再碰设备第二次。
- 结论：`register` 先 `findByCommandNo` 再插，但真正的裁判是 `uk_command_no`——捕获 `DataIntegrityViolationException` 同样按「已中转过」跳过。
- 理由：先查后插挡不住两个实例同时消费同一条重投消息。查一次只是省掉注定失败的 insert。

### 设备不存在只留痕，不抛异常

- 背景：`dispatch` 拿 `deviceCode` 查档案，查不到。
- 选项：抛出去（消息重投）/ 写一条 `deviceId` 为空的 `FAILED` 留痕。
- 结论：后者。抛出去会让 broker 重投，一条指向不存在设备的消息会把消费者永久卡住。
- 代价：这类消息不会进死信，排查靠 `t_device_command` 与日志。Phase 9 定全链路 MQ 拓扑时若要 DLQ，得在这一层显式加。

### 消费者吞掉 `RuntimeException`

- 背景：`RcsCommandListener#onCommand` 重抛等于 requeue。
- 结论：一律在此吞掉并记 error，留痕由中转负责。代价：消息丢了就是丢了，没有兜底队列——这与上一条是同一个取舍的两端。

### 重试只针对「没拿到结论」，设备拒绝不重试

- 背景：`CommandOutcome.accepted=false` 是设备明确说不；连接超时/中断是另一回事。
- 结论：`CommandAttemptException` 专门表达「这一轮没拿到结论」，只有它触发下一轮；`accepted=false` 直接终态 `FAILED`。
- 理由：重试只是把同一条被拒的指令再发一遍。

### 执行线程是字段里的虚拟线程池，不注册成 Bean

- 背景：等待方要能用 `Future.get(timeout)` 判超时并中断执行线程，每次执行必须在独立线程里。
- 结论：`Executors.newVirtualThreadPerTaskExecutor()` 作为 `final` 字段，`@PreDestroy` 关掉。
- 理由：Boot 的 `applicationTaskExecutor` 带 `@ConditionalOnMissingBean({Executor.class, ExecutorService.class})`，本模块一暴露这类 Bean 就把它顶掉——一个协议执行的线程池不该顺带接管全应用的异步执行。

### 指令回读只有 GET

- 背景：`/command/{commandNo}` 要不要同时提供 POST 下发。
- 结论：不提供。指令的唯一入口是 RCS 经 MQ 的下发，HTTP 再开一条等于给同一条指令两种语义，而 HTTP 那条没有幂等键的来源。
- 口径：排查看 `t_device_command` 的落库结果，不是重放。

### `RcsCommandMessage` 不带 Bean Validation 注解

- 背景：同模块的 HTTP 请求 DTO 全都带 `@NotBlank` / `@Size`。
- 结论：MQ 消息不带。这条链路的入口是队列，校验失败没有响应可回。字段非法的去向分三种——缺 `commandNo` 在 `dispatch` 开头直接丢弃（只记 error 日志，幂等键都没有，无处留痕）；`deviceCode` 查无落 `deviceId` 为空的 `FAILED` 行；`commandType` 为 null 撞 `command_type NOT NULL`，被 `register` 的唯一键捕获误判成「并发重复投递」静默跳过。
- 理由：给一个不会抛 `ConstraintViolationException` 的入口标校验注解，是写给人看的假保证。

### 接口口径目前没有机器来源

- 背景：admin-service 接了 springdoc，路由与 schema 由 `/v3/api-docs` 发布；ECS 的依赖与配置里没有 springdoc。
- 现状：README 的接口表是唯一口径，正确性靠 `DeviceControllerTest` 一类入口测试。
- 代价与删除条件：本服务一旦接 springdoc（含 `operationId` 服务名前缀，与 admin 同规则），这张表就该换成指向 `/v3/api-docs` 的链接，不留第二份清单。

## 坑

- **`zen.security.context-source` 不配就是启动失败**：缺省值是 `jwt`，而 `jwt` 分支要 `JwtTokenVerifier`，后者以 `zen.jwt.secret` 为装配开关。ECS 刻意不配密钥，于是这一行成了启动前置条件——装配期抛 `IllegalStateException`，不是运行期 401。
- **分层规则对绕层链路全绿**：`.consideringOnlyDependenciesInLayers()` 只判定四层之间的边。已实测：插一条 `controller → listener → repository` 的违规探针，其余三条分层规则同时红，这一条绿。所以「门禁绿」不等于「消费者没跨层」，Phase 5/7 别拿它当证据。删除条件：给 `listener` / `scheduler` / `protocol` / `presence` 各写一条显式依赖方向断言。
- **MQ 反序列化的 `__TypeId__` 必须限定包**：`ZenEcsMessagingConfiguration` 里 `JacksonJsonMessageConverter` 只放行 `com.zen.ecs.dto` 这一个包。放开等于把「反序列化任意类」的攻击面交给任何能往队列写消息的一方。
- **`online` 列是 `Byte` 不是 `Integer`**：DDL 是 `TINYINT`，改成 `Integer` 会被 `ddl-auto: validate` 报列类型不符。这条只有动实体或改表时才会撞上。
- **两个重试计数口径不同**：`zen.ecs.command.max-attempts` **含首次**（`1` 即不重试），`t_device_command.retry_count` **不含首次**。写反的表现是「配置说重试 3 次，库里记着 2」，看着像少执行了一轮。
- **TTL 必须等于超时阈值**：`HeartbeatService#report` 用 `timeout` 当 `markAlive` 的 TTL。判离线另有「键不存在」与「键还在但太旧」两条路，两者阈值不一致时结论会互相矛盾。
- **判离线后必须清键**：`detectTimeouts` 写完 `OFFLINE` 事件要 `presenceStore.clear()`，否则设备持续掉线会让每个探测周期都重复触发同一条事件。
- **`error_message` 落库前截断到 255**：异常文本可能远超列宽，MySQL 严格模式会把「写终态」这一步本身打成失败——表现为一条永远停在 `PENDING` 的指令，真正的原因却在截断那条 SQL 里。
- **测试配置不许关 Flyway**：`application-test.yml` 刻意不关迁移、不把 `ddl-auto` 降到 `none`。关掉等于把「迁移脚本与实体映射不符」留给线上首次启动去发现。
- **入口测试不用 `@WebMvcTest`**：common-core 的安全自动配置会在切片上下文里装配拦截器，未带身份的测试请求直接 401，那是 `AuthInterceptor` 的行为、不是被测 controller 的。改用 standalone MockMvc 只注册 controller 与 `GlobalExceptionHandler`，鉴权与模块校验留给经网关的端到端。
- **`@Scheduled` 用 `fixedDelay` 不是 `fixedRate`**：探测要扫在线设备并写事件，慢一轮不该让下一轮叠上来。
- **`GET /device/{id}/events` 要先确认设备存在**：直接查事件表，空列表会让调用方分不清「这台设备没有事件」和「设备 ID 写错了」。
- **新建档案不是在线**：`create` 里显式 `markOnline(false)`。省掉这一行时 `online` 的初值来自列默认，读代码的人无法从 service 判断新设备算不算在线。
- **`recentEvents` 的条数不开放**：`RECENT_EVENT_LIMIT = 20` 写死。事件表只增不改，把 `size` 交给调用方等于允许一次拉走整张表。
- **排序字段是白名单**：`PageQuery.orderBy` 是客户端传来的字符串，直接交给 `Sort` 等于把列名与查询形状交出去，而未映射的属性名会让整条查询报错。不在名单里明确 400，不静默回落默认排序。
- **tracing 依赖不在本模块声明**：traceId 的装配随 `common-core` 的 `implementation` / `runtimeOnly` 到运行时类路径。它是一条隐式能力——从 `common-core` 删掉不会有任何编译错误，只会让日志里的 traceId 消失，而 ECS 这一侧没有网关那样的 `MUST_BE_PRESENT` 哨兵。
- **`commandType` 为 null 的坏消息被误判成重复投递。** 现象：`command_type` 列 `NOT NULL`，`register` 插入时撞 `DataIntegrityViolationException`，而该异常按「已中转过」处理——消息既不执行也无 `FAILED` 留痕，日志里还写着「并发重复投递，跳过执行」。删除条件：`dispatch` 入口显式校验 `commandType` 非空（同 `commandNo`），或 `register` 把 NOT NULL 冲突与 `uk_command_no` 冲突区分开。
