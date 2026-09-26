# 跨模块契约

这里收拢**必须多个模块同时成立才有效**的约定。单模块内部的实现与取舍写在 `docs/modules/<module>.md`，硬性门禁写在 [`AGENTS.md`](../AGENTS.md)。

判据只有一条：一条约束如果「只在一侧成立就会静默失效」，它就属于本文件，不属于任何单个模块的 README。

可交互架构图（请求主链路 / 共享内核 / 设备 MQ 三个视图）：[`docs/architecture/v2/zen-iot-architecture.html`](architecture/v2/zen-iot-architecture.html)。**它是某一时刻的快照，不与代码同步维护**，与本文冲突时以本文和代码为准。

## 请求链路与前缀所有权

浏览器 → 网关 `28080` `/api/{admin,wcs,rcs,ecs}/**` → Nacos 发现 → `lb://{service}` → `StripPrefix=2` 剥掉两段前缀 → 服务。

- `/api/{service}` 前缀**只存在于网关侧**。服务内一律写裸路径（`/auth/login`，不是 `/api/admin/auth/login`）。
- OpenAPI 文档里同时声明两条 server：直连与经网关（`zen.api-docs.direct-url` / `gateway-url`）。
- 设备侧不走 HTTP，由 `ecs-service` 的协议适配器接入。

## 鉴权：身份来源与两套白名单

### 身份来源（`zen.security.context-source`）

| 取值             | 含义                                                       | 谁在用                 |
| ---------------- | ---------------------------------------------------------- | ---------------------- |
| `jwt`            | 本服务自己用 `zen.jwt.secret` 验签 `Authorization: Bearer` | `admin-service`        |
| `gateway-header` | 信任网关透传的 `X-User-*` 头，本服务不持有密钥             | 未来不持密钥的业务服务 |

判定入口是 `common-security` 的 `SecurityProperties`，两种来源共用同一批拦截器。

### 密钥必须一致

`zen.jwt.secret`（HS256，UTF-8 ≥ 32 字节，生产由 `ZEN_JWT_SECRET` 覆盖）在 `admin-service` 与 `gateway-service` 必须**完全相同**：签发在 admin、验签在网关，不一致的后果是网关把所有 Token 判为非法。

### 两套白名单，不是同一份配置

| 位置   | 配置键                       | 路径形态   | 例子                    |
| ------ | ---------------------------- | ---------- | ----------------------- |
| 网关   | `zen.gateway.auth.whitelist` | **带前缀** | `/api/admin/auth/login` |
| 服务内 | `zen.security.whitelist`     | **裸路径** | `/auth/login`           |

新增公开接口必须同时改两处：只改服务内，网关会先把它拦掉；只改网关，服务内仍会返回 401。照抄另一侧的字符串是最常见的错法——两边路径形态不同。

## 令牌生命周期与撤销链

`zen.jwt.access-ttl: 30m`、`refresh-ttl: 7d`。access 只用于调用，refresh 只用于 `/auth/refresh`，`typ` 声明区分两者。

### 撤销的写侧与读侧

- **写侧**（`admin-service`）：登出、refresh 轮转被换下的旧 refresh Token、会话吊销，都把 `jti` 按剩余有效期写进 Redis。
- **读侧**（`gateway-service`）：每个请求查 `auth:blacklist:{jti}`。
- **键名单点定义**：`common-security` 的 `TokenRevocationChecker.blacklistKey(jti)`。写侧与读侧都调它，不各自拼字符串——键名写岔不报错，只会让撤销静默失效。
- **TTL = Token 剩余有效期**，到期自然消失，因此不需要任何清理任务。

### 撤销动作的口径

| 场景                 | 撤销范围                                                                |
| -------------------- | ----------------------------------------------------------------------- |
| `POST /auth/logout`  | 本次请求携带的 access `jti` + 入参 refresh `jti`，缺一不可              |
| `POST /auth/refresh` | 旧 refresh `jti` 当场进黑名单，重放即 401（轮转式）                     |
| 强制下线 / 连带吊销  | 会话谱系的 `currentRefreshJti` + `lastAccessJti` **成对**拉黑，再删登记 |

只拉黑 access 会被 `/auth/refresh` 复活；只拉黑 refresh 则已签出的 access 仍可用到自然过期。

`JwtTokenVerifier` 只校验签名/过期/`typ`，**不查黑名单**，所以 `AuthService.refresh` 必须自己查一次——否则被登出的 refresh Token 仍能换新。

### 会话 = refresh 令牌谱系

会话由登录建立、随 refresh 轮转迁移、登出自删。轮转时 `sessionId` 不变，双 `jti`、登记键 TTL、索引 score、`expireTime` 随新 refresh 有效期一并迁移，`issueTime` 保持首次登录时刻。

Redis 三处布局（`RedisSessionRegistry`）：

| 键                                  | 形态                            | 说明                                                     |
| ----------------------------------- | ------------------------------- | -------------------------------------------------------- |
| `auth:session:{sessionId}`          | String(JSON)                    | TTL = refresh 剩余有效期                                 |
| `auth:session:index`                | ZSET，score = `expireTime` 毫秒 | 死项只在读路径 `ZREMRANGEBYSCORE` 顺手清理，不建后台任务 |
| `auth:session:refresh:{refreshJti}` | String → `sessionId`            | 轮转与登出靠它定位会话                                   |

对外响应（`SessionView`）刻意不含 `jti`：列表拿到的标识只用于下线路由，令牌本身不应经接口外泄。

## 模块权限判定

授权依据是 **Token 里的 `modules` 快照**，不落库回查：`@RequireModule(ModuleCode.X)` 判定缺模块即 403。因此改角色授权不会影响已签出的 Token，需要 403 变成 200 就得重新登录或刷新。

`ModuleCode` 枚举 `admin` / `wcs` / `rcs` / `ecs`；`t_role_module.module_code` 存的就是这四个字符串。样例接口 `DemoController`（`/demo/admin`、`/demo/ecs`）存在的唯一目的是验证这条判定链与网关透传身份接得上。

## 身份头透传与伪造防护

网关验签成功后覆写四个头，头名以 `common-security` 的 `TrustedHeaders` 为唯一来源（`X-User-Id` / `X-Username` / `X-User-Roles` / `X-User-Modules`）。

**入站同名头一律先剥离，白名单路径也剥**。不剥离的话，任何人自带 `X-User-Id: 1` 就能冒充超管——这是整套透传设计成立的前提。头名写岔（`X-User-Module` vs `X-User-Modules`）同样不报错，只会让下游判成「未认证」，故收敛到单点常量。

## 真实客户端 IP 的可信边界

`t_login_log.ip` 的「真实客户端 IP」语义**依赖网关 `JwtAuthGlobalFilter` 对 `X-Forwarded-For` 的剥离 + 以连接对端重建**。服务侧取 XFF 的最后一个值即网关认定的客户端。

不经网关直达服务的请求（本机集成用例、内部直连、运维 curl）没有可信 XFF，回落到 `getRemoteAddr()`，记的是**连接对端而非浏览器**。排查时不要把这类值当真实来源。

登录失败的锁定计数按 `username` 维度，与 IP 无关。

## 登录失败锁定口径

配置前缀 `zen.security.login`（`LoginSecurityProperties`）：`max-fail-attempts` 默认 5、`lockout-ttl` 默认 15m、`failure-window-ttl` 默认 15m。

- **固定窗口而非滑动窗口**：TTL 只在首次 `INCR` 设置，后续失败不续期。
- 达到阈值那一次响应 429；锁定期内的尝试一律 429、不计数、不续期，到期自动解锁。
- **Redis 故障 fail-open**（`RedisLoginAttemptStore` 内部降级）：可用性优先于「锁定绝对严格」，否则 Redis 挂了无人能登录。

## 审计：两类日志都不反噬主流程

| 日志     | 表                | 写入方式                                     | 边界                                                    |
| -------- | ----------------- | -------------------------------------------- | ------------------------------------------------------- |
| 登录日志 | `t_login_log`     | `AuthService.writeLoginLog` 同步 best-effort | 落库失败只记 ERROR，绝不阻断认证                        |
| 操作审计 | `t_operation_log` | `OperationLogInterceptor.afterCompletion`    | 同上；且落库在 service 事务之外，业务回滚场景审计行仍在 |

操作审计的 `module` 列取同方法 `@RequireModule` 的模块码——缺该注解则跳过落库并记 ERROR；`result_code` 记响应 HTTP 状态码（含 500 异常路径）。认证失败（401/403）在拦截器链更前抛出，天然不触发操作审计。

会话簿记与吊销的异常边界是**分两界**的：簿记三动作（登录登记 / 轮转迁移 / 登出清理）Redis 失败只记 WARN；吊销动作 fail-closed 上抛，因为静默吞掉意味着旧谱系在改密事务提交后仍能续期，等于安全承诺落空。

## 统一响应与错误码口径

响应统一包在 `ApiResponse`（`code` / `message` / `data`），且 `code` 与 HTTP 状态码同源。

- 用户不存在、密码错误、Token 缺失、签名不合法、已被逻辑删的用户一律 **401 同码同体**，不透露账号是否存在。
- 账号停用为 403；登录锁定为 429。
- 唯一性冲突为 409（`username`、`roleCode`、仍被引用的角色）；非法枚举/排序字段为 400。
- 网关侧不能用 `common-core` 的 `GlobalExceptionHandler`（Servlet 专属），由 `GatewayErrorWebExceptionHandler` 承接路由级错误，鉴权失败由过滤器直接写出同一个 `ApiResponse` 形状。
