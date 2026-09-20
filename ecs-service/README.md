# ecs-service

外部设备服务（端口 28084，库 `zen_ecs`）：管 AMR 之外的设备——PLC / 光栅 / 自动门 / 充电桩。设备档案与分组、心跳在线态、协议适配抽象层、RCS 指令中转。

## 关键实现

- **身份来源是网关透传头**：`zen.security.context-source: gateway-header`，本服务**不配** `zen.jwt.secret`。这不是风格声明而是启动前置条件——缺省值是 `jwt`，而 `jwt` 分支拿不到 `JwtTokenVerifier`（它以 `zen.jwt.secret` 为装配开关）会在装配期抛 `IllegalStateException`。
- **服务内白名单只放 `/actuator/**`**：ECS 的接口全部要身份，设备侧上报走协议适配器而非 HTTP 匿名入口。它与网关的 `zen.gateway.auth.whitelist`是两套字符串（裸路径 / 带`/api/ecs` 前缀）。
- **设备档案与分组**：`/device`、`/device-group`，都是逻辑删除（实体上的 `@SQLDelete` + `@SQLRestriction`）。但唯一索引不区分 `deleted`，因此新建按全表查重（`countBy…IncludingDeleted`）并报业务码，而不是留给数据库抛 500。
- **心跳与上下线**：`/heartbeat` 上报只写 Redis（键 `ecs:heartbeat:{deviceId}`，TTL 就等于超时阈值 `zen.ecs.heartbeat.timeout: 30s`），超时探测由 `@Scheduled`（`probe-interval: 10s`）周期扫在线设备。判定一律读注入的 `java.time.Clock`，所以「超时自动离线」在单元测试里拨时钟就能复现，不必真等 30 秒。
- **协议适配抽象层**：`ProtocolSupport`（按协议类型认领 + 打开适配器）→ `DeviceAdapterFactory` → `DeviceAdapter`（`connect`/`read`/`write`/`close`）。当前只有 `loopback` 这一个回环实现，Modbus TCP / OPC UA 随 Phase 4 追加，插槽形态不变。
- **指令中转**：RCS 经 RabbitMQ topic 交换器 `zen.rcs.command` 投递，ECS 用绑定键 `rcs.command.ecs` 把队列 `ecs.rcs.command` 挂上去。`RcsCommandListener` 消费后由 `CommandDispatchService` 落到设备：`commandNo` 是幂等键（`t_device_command` 的 `uk_command_no` 即判重），单次等待 `5s`、最多 `3` 次尝试，结果回写该表供 `GET /command/{commandNo}` 回读。
- **表结构**：Flyway `V1__init_ecs.sql` 建 `t_device` / `t_device_group` / `t_device_event` / `t_device_command`。

> MQ 消息反序列化限定在 `com.zen.ecs.dto` 这一个受信包：`JacksonJsonMessageConverter` 按 `__TypeId__` 头选类，放开等于把「反序列化任意类」的攻击面交给任何能往队列写消息的一方。

## 架构约束

`ArchitectureTest`（`./gradlew :ecs-service:architectureTest`）逐条照搬 `admin-service` 的分层规则（四层单向、实体不出入口层、`@Transactional` 只在 service、角色各归其包、切片无环，外加构造器注入 / 禁标准流 / 禁 `new Date()`），另加三条：

- **导入类数非零哨兵**（`importedClassesAreNeverEmpty`）：包根改名或目录被挪走时，规则会拿到零个类而「按自己的判定风格」安静通过；这条把「覆盖面缩水」变成明确断言。
- **禁止直接读系统时钟**（`neverReadsTheSystemClockDirectly`）：`LocalDateTime.now()` / `Instant.now()` / `System.currentTimeMillis()` 一律不许，心跳超时判定与事件时刻必须取注入的 `Clock`，否则「超时自动离线」只能靠真等阈值来测。
- 不得引用其它业务服务或网关。

> **边界警告**：分层规则带 `.consideringOnlyDependenciesInLayers()`，而协议适配（`protocol`）、心跳存储（`presence`）、MQ 消费者（`listener`）、定时探测（`scheduler`）都在四层之外。所以 listener/scheduler 调 service 不被判跨层——反面同样成立：`controller → listener → repository` 这种绕层链路也不会变红（已实测：违规探针三条规则同时红，这条绕过全绿）。「消费者只调 service」是约定而非门禁，Phase 5/7 别把「门禁绿」当「没跨层」。

## 主要依赖

`common-core`（以 `api` 透传 `common-security`，在这里重复声明 `common-security` 会让跨模块版本比对的输入变模糊）、Spring Boot Web / Validation / Data JPA / Data Redis / AMQP、Flyway + `flyway-mysql`、Nacos Discovery、actuator + prometheus registry、MySQL Connector、Lombok。

## 命令

| 命令                                             | 说明                                                                |
| ------------------------------------------------ | ------------------------------------------------------------------- |
| `./gradlew :ecs-service:bootRun`                 | 启动服务（28084）                                                   |
| `./gradlew :ecs-service:architectureTest`        | 只跑本模块架构约束                                                  |
| `./gradlew :ecs-service:test -PintegrationTests` | 连同依赖本机 MySQL/Redis/RabbitMQ/Nacos 的 `@SpringBootTest` 一起跑 |
| `./gradlew :ecs-service:bootJar`                 | 产出 `ecs-service.jar`                                              |
