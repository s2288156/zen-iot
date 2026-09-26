# ecs-service

外部设备服务（端口 `28084`，库 `zen_ecs`）：管 AMR 之外的设备——PLC / 光栅 / 自动门 / 充电桩。设备档案与分组、心跳在线态、协议适配抽象层、RCS 指令中转。

本文只写事实与结论。**为什么这样选、否掉了什么、踩过什么坑**见 [`docs/modules/ecs-service.md`](../docs/modules/ecs-service.md)；跨模块必须同时成立的约定（前缀所有权、身份头、两套白名单、业务码与 HTTP 状态码的映射）见 [`docs/architecture.md`](../docs/architecture.md)。

## 能力与接口口径

路径都是**裸路径**：`/api/ecs` 前缀只存在于网关侧，`StripPrefix=2` 剥掉后才进本服务。除 `/actuator/**` 外，每个接口都要求 `@RequireModule(ModuleCode.ECS)`，身份取自网关透传头。

### HTTP 接口

| 方法 & 路径                      | 说明                                                                                         | 业务错误码                |
| -------------------------------- | -------------------------------------------------------------------------------------------- | ------------------------- |
| `GET /device`                    | 分页；`groupId` 缺省即不过滤                                                                 | 400（排序字段不在白名单） |
| `GET /device/{deviceId}`         | 单台档案                                                                                     | 1001                      |
| `POST /device`                   | 建档案；`protocolType` 只存字符串，此处不校验可否连通                                        | 1002、1003                |
| `PUT /device/{deviceId}`         | 改档案；**不含 `deviceCode`**，编码建后不可改                                                | 1001、1003                |
| `DELETE /device/{deviceId}`      | 逻辑删除                                                                                     | 1001                      |
| `GET /device/{deviceId}/events`  | 最近 20 条上下线事件，条数不开放给调用方                                                     | 1001                      |
| `GET /device-group`              | 全量列表（分组数量有限，不分页）                                                             | —                         |
| `POST /device-group`             | 建分组                                                                                       | 1004                      |
| `PUT /device-group/{groupId}`    | 改名称与描述；**不含 `groupCode`**                                                           | 1003                      |
| `DELETE /device-group/{groupId}` | 组内仍有设备时拒绝                                                                           | 1003、1005                |
| `POST /heartbeat/{deviceCode}`   | 记一次心跳：Redis 在线键 + `last_heartbeat_time`；离线转在线留 `ONLINE` 事件。不返回在线判定 | 1001                      |
| `GET /command/{commandNo}`       | 指令中转结果回读；**没有 POST**，指令只能经 MQ 进来                                          | 1007                      |

- 分页排序字段是白名单：`id` / `deviceCode` / `deviceName` / `createTime` / `updateTime`，其余 `orderBy` 一律 400。
- 编码重复按**全表**查重（含软删行），因为 `uk_device_code` / `uk_group_code` 不区分 `deleted`；已删除的档案同样占位。
- `GET /device/{deviceId}/events` 先确认设备存在再查事件，避免调用方分不清「没有事件」和「设备 ID 写错」。

### MQ 契约

| 项          | 值                  | 口径                                                    |
| ----------- | ------------------- | ------------------------------------------------------- |
| Exchange    | `zen.rcs.command`   | topic；拓扑由本模块声明，RabbitAdmin 在连接建立时自动建 |
| Queue       | `ecs.rcs.command`   | 消费方唯一入口                                          |
| Routing key | `rcs.command.ecs`   | 按 `rcs.command.<目标模块>` 走，本服务只绑自己那一条    |
| 消息体      | `RcsCommandMessage` | `commandNo` / `deviceCode` / `commandType` / `payload`  |

- `commandNo` 由 RCS 生成且全局唯一，是消费侧幂等键；MQ 至少一次投递，缺了它就无从证明「重复投递不执行第二次」。
- 消息**不带 Bean Validation 注解**：入口是 MQ，校验失败没有响应可回。缺 `commandNo` 直接丢弃（无留痕）；`deviceCode` 查无落 `FAILED`；`commandType` 为 null 会被误判成重复投递静默跳过（见 docs 坑清单）。
- 消费者吞掉一切 `RuntimeException`，不重抛——重抛会让 broker requeue，一条注定失败的消息会把消费者卡死。

### 配置键

| 键                                 | 当前值           | 口径                                                        |
| ---------------------------------- | ---------------- | ----------------------------------------------------------- |
| `zen.security.context-source`      | `gateway-header` | **必须显式配**；缺省是 `jwt`，而本服务不配 `zen.jwt.secret` |
| `zen.security.whitelist`           | `/actuator/**`   | 服务内白名单，与网关那套是两套字符串                        |
| `zen.ecs.heartbeat.timeout`        | `30s`            | 超时阈值；同时是 Redis 键的 TTL                             |
| `zen.ecs.heartbeat.probe-interval` | `10s`            | `@Scheduled` 探测周期（`fixedDelay`）                       |
| `zen.ecs.command.timeout`          | `5s`             | 单次下发的等待上限                                          |
| `zen.ecs.command.max-attempts`     | `3`              | **含首次**，故 `1` 即不重试                                 |
| `zen.ecs.messaging.*`              | 见上表           | 交换器 / 队列 / 绑定键，改一处即断链                        |

`application-test.yml` 不关 Flyway、不把 `ddl-auto` 降到 `none`，只关 Nacos 注册与配置导入、端口取 `0`。

## 关键实现

- **表结构**：Flyway `V1__init_ecs.sql` 建 `t_device` / `t_device_group` / `t_device_event` / `t_device_command`；`ddl-auto: validate`，Hibernate 只校验不建表。
- **在线态与档案分离**：`/heartbeat` 往 `DevicePresenceStore` 记「活着」（Redis 键 `ecs:heartbeat:{deviceId}`，值 ISO-8601 文本，TTL 等于超时阈值），同时更新 `last_heartbeat_time`（MySQL，仅展示）；离线转在线时写标记并留 `ONLINE` 事件。`HeartbeatTimeoutTask` 按 `probe-interval` 跑 `HeartbeatService#detectTimeouts`，判离线才写 `OFFLINE` 事件并清键——置离线只发生在探测里，一次上报只证明那一刻活着。
- **时间源统一注入 `java.time.Clock`**：超时判定与事件时刻都读它，拨时钟就能测「超时自动离线」；直接调 `LocalDateTime.now()` / `Instant.now()` / `System.currentTimeMillis()` 由 `neverReadsTheSystemClockDirectly` 拦下。
- **协议适配是可插拔插槽**：`ProtocolSupport`（认领 `protocolType` + 打开适配器）→ `DeviceAdapterFactory` → `DeviceAdapter`（`connect` / `status` / `read` / `write` / `close`）。当前只有 `loopback`；无人认领的协议类型抛 1006，不静默丢指令。
- **指令中转**：`CommandDispatchService#dispatch` 先插 `PENDING` 行再执行，结果写终态 `SUCCESS` / `FAILED`；重试只针对「没拿到结论」的轮次（连不上、超时、被中断），设备明确拒绝直接终态；`retry_count` 不含首次，`error_message` 截断到 255。
- **新建档案即离线**：`markOnline(false)`，只有心跳能置起在线标记。
- **审计人**覆盖 common-core 默认的 `system`，取 `UserContext` 里的用户名（与 admin-service 同口径）。

## 架构约束

`./gradlew :ecs-service:architectureTest`，规则清单以该测试类为准：11 条规则，参数化展开后 14 个用例。分层四条（四层单向、实体不出入口层、`@Transactional` 只在 service、角色后缀各归其包 ×4）与通用四条（构造器注入、禁标准流、禁 `new Date()`、包切片无环）合计 11 个用例，其余 3 个只属于本模块：

- **导入类数非零哨兵**（`importedClassesAreNeverEmpty`）：包根改名或目录被挪走时规则会拿到零个类而安静通过，这条把「覆盖面缩水」变成明确断言。
- **禁止直接读系统时钟**（`neverReadsTheSystemClockDirectly`）：时间源必须是注入的 `Clock`。
- **不得引用其它业务服务或网关**（`neverDependsOnOtherServicesOrGateway`）：编译期只允许 `common-core` / `common-security`。

本模块未套用 JSpecify 可空性条（`neverUsesDeprecatedSpringNullabilityAnnotations`）——它只在 `common-core` / `common-security` / `gateway-service` 三个模块断言，`admin-service` 与 `ecs-service` 尚未纳入。

> **分层规则的盲区**：`layersOnlyDependDownwards` 带 `.consideringOnlyDependenciesInLayers()`，而 `protocol` / `presence` / `listener` / `scheduler` 四个包都在四层之外。因此 `listener → service` 不被判跨层，反面同样成立——`controller → listener → repository` 这种绕层链路也不会变红。「消费者只调 service」是约定而非门禁。

## 主要依赖

`common-core`（以 `api` 透传 `common-security`）、Spring Boot Web / Validation / Data JPA / Data Redis / AMQP / Flyway + `flyway-mysql`、Nacos Discovery、actuator + prometheus registry、MySQL Connector、Lombok。**没有 tracing 依赖**，也**不声明 `common-security`**。

## 命令

| 命令                                             | 说明                                                          |
| ------------------------------------------------ | ------------------------------------------------------------- |
| `./gradlew :ecs-service:bootRun`                 | 启动服务（28084），需本机 MySQL / Redis / RabbitMQ / Nacos    |
| `./gradlew :ecs-service:bootJar`                 | 产出 `ecs-service.jar`                                        |
| `./gradlew :ecs-service:test`                    | 只跑单测（standalone MockMvc + 假 presence 存储），不碰中间件 |
| `./gradlew :ecs-service:test -PintegrationTests` | 连同 `@Tag("integration")` 的 `@SpringBootTest` 一起跑        |
| `./gradlew :ecs-service:architectureTest`        | 只跑本模块架构约束                                            |
