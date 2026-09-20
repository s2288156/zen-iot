# gateway-service

API 网关（端口 28080，WebFlux 反应式）：统一路由、JWT 鉴权与身份透传。下游服务不必自己验签，`/api/{service}` 前缀也只存在于网关这一侧。

## 关键实现

- **路由**：`/api/{admin,wcs,rcs,ecs}/**` → `lb://{service}`（Nacos 发现），`StripPrefix=2` 剥掉两段前缀，所以服务内一律写裸路径。当前只有 `admin-service` 的 URI 允许用 `GATEWAY_ADMIN_URI` 覆盖，其余为纯 `lb://`。
- **鉴权**：`JwtAuthGlobalFilter`（`Ordered.HIGHEST_PRECEDENCE`）按「白名单 → 取 Bearer Token → 验签/过期/`typ=access` → Redis 黑名单 `auth:blacklist:{jti}` → 覆写 `X-User-Id`/`X-Username`/`X-User-Roles`/`X-User-Modules`」执行。入站同名身份头一律**先剥离**，白名单路径也剥——否则任何人自带 `X-User-Id: 1` 就能冒充超管。
- **白名单**：`zen.gateway.auth.whitelist` 匹配**带前缀**的入站路径（如 `/api/admin/auth/login`），照抄服务内的裸路径会全部漏放。它与各服务自己的 `zen.security.whitelist` 是两套字符串，新增公开接口要同时改两处。
- **跨域**：`GatewayCorsWebFilter`（`CorsWebFilter` 子类 + 显式 order，由 `GatewayCorsConfiguration` 注册），预检请求在鉴权之前短路。刻意不使用网关的 `globalcors`，避免两处同时写 `Access-Control-*` 头。
- **统一异常**：鉴权失败由过滤器直接写出 `ApiResponse` JSON；路由级错误（无匹配路由、下游无实例 503）由 `GatewayErrorWebExceptionHandler` 承接——`common-core` 的 `GlobalExceptionHandler` 是 Servlet 专属，网关用不了。
- **链路**：`spring.reactor.context-propagation: auto` + 本模块自带的 tracing bridge，使 traceId 进网关日志并由 SCG 注入 `traceparent` 到下游请求。
- **密钥**：`zen.jwt.secret` 必须与 admin-service 完全一致（生产由 `ZEN_JWT_SECRET` 覆盖），否则 Phase 1 签发的 Token 在这里一律验签失败。

## 架构约束

`ArchitectureTest` 与 `GatewayDependencyIsolationTest`（从消费方视角复核依赖）把守：

- 禁引用 Servlet / `spring-webmvc` / `jakarta.persistence` / Hibernate——网关是反应式应用。
- 禁引用 `common-core` 的 Servlet 专属件（`AuthInterceptor` / `ModuleAuthInterceptor` / `UserContext` / `web` 包），并由 `ArchitectureTest.neverDependsOnCommonCore()` 整体钉住不依赖 `common-core`。
- 禁命令式 `StringRedisTemplate` / `RedisTemplate`：黑名单读取只走 reactive Redis。
- 禁在任何地方调用 `Mono#block` / `Flux#blockFirst` / `blockLast`：Netty EventLoop 上阻塞会静默堵死事件循环。

## 主要依赖

`common-security`（唯一项目依赖）、Spring Cloud Gateway（`...-gateway-server-webflux` 坐标，Cloud 2025.1 起网关拆成 webflux/webmvc 两套）、Spring Cloud LoadBalancer（`lb://` 需要它，网关 starter 不传递）、Spring Data Redis Reactive、Nacos Discovery、actuator + prometheus registry、micrometer-tracing（OTel bridge）。

> 该模块**不依赖 `common-core`**。拆模块前这里要逐条 `exclude` `common-core` 以 `api` 暴露的 `spring-boot-starter-web`/`-validation`/`-data-jpa`（漏一条网关就起不来：Servlet 判定或无数据源），现在内核本身不带这些依赖，一条 `implementation` 就够。边界由两侧断言把守：生产方 `common-security` 的 `moduleStaysFreeOfWebAndPersistence()`，消费方本模块的两个测试。

## 命令

| 命令                                          | 说明                       |
| --------------------------------------------- | -------------------------- |
| `./gradlew :gateway-service:bootRun`          | 启动网关（28080）          |
| `./gradlew :gateway-service:bootJar`          | 产出 `gateway-service.jar` |
| `./gradlew :gateway-service:architectureTest` | 只跑本模块架构约束         |
