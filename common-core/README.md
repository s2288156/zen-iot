# common-core

Servlet 业务服务的公共模块。以 `api` 透传 `common-security`，消费方（`admin-service`、`ecs-service`）无需重复声明；依赖即通过 `AutoConfiguration.imports` 自动装配，不用改启动类。

## 自动装配的三件事

- **`ZenWebAutoConfiguration`**：注册 `GlobalExceptionHandler`（`@RestControllerAdvice`，统一写出 `ApiResponse`），业务服务可定义自己的 Bean 接管（`@ConditionalOnMissingBean`）。
- **`ZenJpaAuditingAutoConfiguration`**：启用 `@EnableJpaAuditing` 并注册默认审计人 `"system"`；业务服务定义自己的 `AuditorAware<String>` 即覆盖。`zen.jpa.auditing.enabled=false` 可整体关闭。
- **`ZenSecurityAutoConfiguration`**：注册 `AuthInterceptor` 与 `ModuleAuthInterceptor`，把 `zen.security.whitelist` 交给 `excludePathPatterns`，并按 `zen.security.context-source` 选身份来源——`jwt`（缺省，走 `JwtUserContextResolver`，需要 `zen.jwt.secret`）或 `gateway-header`（走 `GatewayHeaderUserContextResolver` 读 `X-User-*`）。整个鉴权可被 `zen.security.enabled=false` 关掉，只供本地与测试。

## 其余内容

- **`BaseEntity`**：`id` + `create_time`/`update_time`/`creator`/`updater` 审计字段。
- **`PageQuery` / `PageResult<T>`**：请求参数转 Spring Data `Pageable`，`Page<T>` 转响应体。
- **链路追踪 A 层**：`traceId` 生成/透传/写 MDC，日志可串联但 span 不上报。刻意不用 `spring-boot-starter-opentelemetry`——它连带 otlp exporter，会真的开始外发数据；Phase 10 部署追踪后端时再换。

> 装配类必须 `@ConditionalOnWebApplication(type = SERVLET)`：反应式网关依赖的是拆出去的 `common-security`，本模块的类一旦落到它 classpath 上，Servlet 拦截器与 `WebMvcConfigurer` 就会漏进反应式应用，启动直接失败。

## 架构约束

`ArchitectureTest`：不得依赖任何业务服务或网关包（`com.zen.{admin,ecs,gateway,rcs,wcs}`）；包切片之间无循环依赖。

## 主要依赖

以 `api` 暴露的 `common-security` 与 `spring-boot-starter-web`/`-validation`/`-data-jpa`（类型出现在公共签名里），micrometer-tracing（OTel bridge）、Lombok。

## 命令

| 命令                                      | 说明               |
| ----------------------------------------- | ------------------ |
| `./gradlew :common-core:architectureTest` | 只跑本模块架构约束 |
| `./gradlew :common-core:test`             | 本模块单元测试     |
