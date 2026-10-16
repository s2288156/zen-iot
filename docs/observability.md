# traceId 可观测性

> **这篇文档放在哪**：链路每一环的真源是代码与配置——网关的 `application.yml`、各模块 `build.gradle.kts` 里的 tracing 依赖、`common-core` 的 `AuthInterceptor`。本文不抄那份清单，只回答三个问题：**一个 traceId 怎么从入站请求走到某条服务日志、中间哪一段现在是断的、actuator 端点里能看到什么与看不到什么**。「为什么只做 A 层、否掉了什么」归 [`docs/modules/common-core.md`](modules/common-core.md) 与 [`docs/modules/gateway-service.md`](modules/gateway-service.md)；本文只写这条链路落地后的形态与它的前提。术语定义见 [`GLOSSARY.md`](../GLOSSARY.md) 的「A 层追踪」。

## 1. 链路终点在哪里

A 层做到「traceId 进日志 + 向下游注入 `traceparent`」，**span 不外发**。终点是确定的，而且不是配置漏了一半：类路径上没有 `SpanExporter` 实现（刻意不用 `spring-boot-starter-opentelemetry`，它连带 otlp exporter），但 `OpenTelemetryTracingAutoConfiguration#otelSpanProcessor` 照样装出一个 `BatchSpanProcessor`，它包的 `CompositeSpanExporter` 导出器列表为空——**span 在进程内被丢弃**。

于是排查手段只有两种：按 traceId 查日志，读 `/actuator/prometheus` 里的 exemplar。「打开追踪后端看这条请求的瀑布图」在 Phase 10 接上 collector 之前不成立。

## 2. 网关侧：trace 从哪来

1. **入站先落到网关自己的 server observation**。`propagatingReceiverTracingObservationHandler` 从 `traceparent` 提取远端上下文：带就续用同一个 traceId 并原样保留末段采样标志，不带就新起一条。实测入站 `00-aaaa…-bbbb-01` → 下游收到 `00-aaaa…-<新 spanId>-01`；入站 `-00` 同样续用、末段仍是 `-00`。
2. **鉴权与 tracing 无关**。`JwtAuthGlobalFilter` 只做可信头剥离与覆写，代码里没有任何 MDC 或 span 操作；401、503、500 的日志行一样带 traceId。
3. **转发前 SCG 建一个 sender observation**。`ObservedRequestHttpHeadersFilter` 的父 observation 取自 Reactor Context——键 `micrometer.observation`，那份 `ContextView` 经 exchange 属性 `GATEWAY_REACTOR_CONTEXT_ATTR` 传给它。
4. **`GatewayPropagatingSenderTracingObservationHandler#onStart` 先删后写**：把 propagator 声明的字段（`traceparent` / `tracestate` / `b3` 等）从出站头里移除，再由 `propagator.inject` 写入网关这条 trace 的子上下文。入站哪怕带了伪造的 `traceparent` 也不会被透传——它只决定「续用哪个 traceId」，头本身被换掉。
5. **MDC 由 bridge 写**。`micrometer-tracing-bridge-otel` 的 `Slf4JEventListener` 在 observation 的 scope 开/关时写/清 MDC 的 `traceId` 与 `spanId`。这是 §3 那条前提唯一真正作用的地方。

## 3. 一条前提：`spring.reactor.context-propagation`

Boot 4 默认 `limited`，网关在 `gateway-service/src/main/resources/application.yml` 显式设 `auto`。两者只差一件事：`limited` 只在 `tap` / `handle` 等少数算子上把上下文恢复到 ThreadLocal，`auto` 调 `Hooks.enableAutomaticContextPropagation()` 逐算子恢复。

而 §2 的第 3 步与第 5 步取的**不是同一个载体**，所以受影响的程度也不同：

| 环节                     | 取父上下文的途径                                                                                                                                                                | 受 `limited` 影响 |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- |
| 向下游注入 `traceparent` | Reactor Context：`getParentObservation` 直接读 `ContextView`，`TracingObservationHandler#getParentSpan` 优先用父 observation 的 `TracingContext`，`Tracer#currentSpan` 只是兜底 | **不受**          |
| 网关日志里出现 traceId   | ThreadLocal：`Slf4JEventListener` 只在 scope 打开的那个线程上写 MDC，日志语句常常跑在别的线程上                                                                                 | **受影响**        |

实测（网关真启动 + JDK `HttpServer` 桩后端，`TokenBlocklist` 返回 `Mono.delay(...)` 以制造 scheduler 跳，把 `org.springframework.cloud.gateway.filter.headers.observation` 开到 DEBUG 才有可读的行）：四个请求跑完，同一份结构化日志累计 44 行。`limited` 下 **0** 行带 traceId；`auto` 下 16 行带——每请求 4 行，发送侧过滤器 2 行、响应侧过滤器 2 行。带身份那次请求的发送侧 2 行落在 `parallel-1` 线程上，正是那次 scheduler 跳之后；免鉴权路径的 4 行都留在 netty 线程上。两种模式注入给下游的 `traceparent` 都对。

**所以症状是「下游跟得上、网关查不到」，不是「网关有 trace 列、admin 另起一条」**。按 id grep 网关日志查不到这条请求时，先确认这个键，再怀疑日志级别。删除条件：Boot 把默认值改回 `auto`，或网关不再依赖 ThreadLocal 写 MDC。

## 4. 下游侧：同一个 traceId 怎么落到日志行

Servlet 服务不需要 §3 那条前提——请求全程跑在容器线程上，observation 的 scope 就地打开、就地关闭。

1. Boot 的 `WebMvcObservationAutoConfiguration#webMvcObservationFilter` 把 `ServerHttpObservationFilter` 以 `FilterRegistrationBean` 注册进去，order `HIGHEST_PRECEDENCE + 1`，dispatcher 覆盖 `REQUEST` 与 `ASYNC`；它建 server observation 并打开 scope。
2. 同一个 receiver handler 从 `traceparent` 取回网关那条 trace，`Slf4JEventListener` 写 MDC。
3. 实测：给一个 MVC 服务发 `traceparent: 00-1111…-2222…-01`，业务日志行的 `traceId` 就是 `1111…`，`spanId` 是本服务新建的子 span。**两侧 traceId 相同、spanId 不同**是正常形态，不是断链的信号。

日志形态由 profile 决定，两套读的都是同一个 MDC：

| profile     | 配置                                                                                                                 | 落地样子                                                                                                                                                             |
| ----------- | -------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 默认（dev） | 不配 `logging.structured`                                                                                            | Boot 的 `%correlationId` 占位，默认 spec `traceId(32),spanId(16)`，渲染成级别之后那段 `[<32 位 hex> <16 位 hex>]`；没有 trace 的行（启动期、定时任务）该段是等宽空白 |
| prod        | 三个服务的 `application-prod.yml`：`logging.structured.format.*` = `logstash`，`logging.structured.json.add.service` | 内置 logstash formatter 把 MDC 键**自动提升为 JSON 顶层字段**，`traceId` / `spanId` / `userId` / `service` 平级，采集端按字段检索                                    |

`userId` 不是 tracing 给的：`common-core` 的 `AuthInterceptor` 在 `preHandle` 写 MDC `userId`、`afterCompletion` 连同 `UserContext` 一起清掉。所以「有 traceId、没 userId」是合法组合（白名单路径、非 HTTP 入口），反过来不会成立。

跨服务串联要求**两侧都是 prod 形态**：dev 的文本与 prod 的 JSON 混在一起时，能 grep 到 id，不能按字段过滤。

## 5. actuator 里能观察什么

| 位置                                                           | 能观察到                                                                                           | 观察不到                                                                 |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `/actuator/prometheus`，`Accept: application/openmetrics-text` | 计数与直方图行下方的 exemplar `# {span_id="…",trace_id="…"}`：某个样本值属于哪条 trace 的哪个 span | 未被采样的 trace；任何「按 traceId 反查这条请求」的能力                  |
| `/actuator/health`、`/info`、`/metrics`                        | 与 traceId 无关                                                                                    | —                                                                        |
| 响应头                                                         | —                                                                                                  | 经网关的响应不回显 `traceparent`（实测响应头里没有），前端拿不到 traceId |

- exemplar 只对**被采样**的 trace 产生。默认 sampler 是 `PARENT_BASED_TRACE_ID_RATIO`，`management.tracing.sampling.probability` 默认 `0.10`：父级的决定优先，本地新起的 trace 约十分之一被采样。「prometheus 里查不到这个 trace_id」先按未采样解释，不等于链路断了。
- 采样与日志无关：**未采样的 trace 一样进日志**。A 层的可观测性建立在日志上，不建立在采样上；要每条都有 exemplar 就把 probability 抬到 `1`，日志侧不需要动。
- 不会有 `/actuator/traces` 这类端点，它需要一个 trace 后端。
- 端点可达性口径见 [`docs/architecture.md`](architecture.md) 的 actuator 段：经网关时只有 `health` 免鉴权，`prometheus` 要带身份；直连服务端口一律匿名可读。

## 6. 断点清单

traceId 现在走不到的位置。「同进程内线程断」与「跨进程没有载体」是两类问题，修法不同。

| 位置               | 代码                                                                                 | 为什么断                                                  | 接上要做什么                                                                        |
| ------------------ | ------------------------------------------------------------------------------------ | --------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 定时任务           | `ecs-service` 的 `HeartbeatTimeoutTask`（`@Scheduled`）                              | 没有入站请求就没有 observation，scope 没打开，MDC 是空的  | 自己起根 span：包一层 `Observation` 或 `Tracer#nextSpan()`，任务体内显式开 scope    |
| 命令下发的虚拟线程 | `CommandDispatchService` 的 `Executors.newVirtualThreadPerTaskExecutor()` + `submit` | ThreadLocal 不跨 `ExecutorService`，scope 跟不进子任务    | 用 `io.micrometer:context-propagation` 的 `ContextExecutorService` 包装该执行器     |
| MQ 这一腿          | `RcsCommandListener`（`@RabbitListener`）                                            | 跨 broker 不靠 HTTP 头传上下文，且生产者 RCS 服务还不存在 | producer 把 `traceparent` 写进消息属性、consumer 提取后当父上下文；RCS 落地时一并做 |
| WCS / RCS 路由     | 网关预留的 `/api/wcs`、`/api/rcs` 前缀                                               | 没有注册实例，请求 503 就返回了                           | 不需要处理，服务注册后自动接上                                                      |
| 前端               | `web/`                                                                               | 经网关的响应不回显 traceId，用户报障时无法引用它          | 在网关回传或写进错误响应体——是产品决策，不是配置项                                  |
| 直连服务端口       | 绕过网关的任何调用（本机用例、内部直连、运维 curl）                                  | 服务自己起一条新 trace，与网关那条不同 id                 | 属预期行为；排障时按 `service` 字段分别查                                           |

共同规律只有一句：**observation 跟着「被 instrumented 的入口」走**。入口之外的线程切换与进程跳转，都要显式传上下文，没人替你做。
