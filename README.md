# Zen IoT

面向仓储 / 工厂场景的物联网管理平台：统一网关鉴权、认证与授权基线、外部设备（PLC / 光栅 / 自动门 / 充电桩）的档案、在线态与指令中转。

技术栈为 **Spring Boot 4.1 + Gradle 多模块**。网关是反应式应用（Spring Cloud Gateway / WebFlux），业务服务是 Servlet 栈（Spring MVC + JPA），两侧靠同一份 Web 无关的安全内核复用验签代码。中间件：MySQL 8（每服务一库）、Redis（令牌黑名单、会话、设备心跳）、RabbitMQ 4（跨服务指令）、Nacos（服务注册）、Flyway（建库）。

## 全景

| 模块              | 端口  | 已落地能力                                                                            | 文档                                |
| ----------------- | ----- | ------------------------------------------------------------------------------------- | ----------------------------------- |
| `gateway-service` | 28080 | 路由、JWT 验签与黑名单、身份头透传、CORS、统一异常                                    | [README](gateway-service/README.md) |
| `admin-service`   | 28081 | 登录 / 刷新 / 登出、会话与强制下线、用户与角色、两类审计（库 `zen_admin`）            | [README](admin-service/README.md)   |
| `ecs-service`     | 28084 | 设备档案与分组、心跳在线态、协议适配（当前 `loopback`）、RCS 指令中转（库 `zen_ecs`） | [README](ecs-service/README.md)     |
| `common-core`     | —     | Servlet 业务服务的公共件：响应契约、全局异常、用户上下文、拦截器                      | [README](common-core/README.md)     |
| `common-security` | —     | Web 无关的安全内核：JWT、错误码、`ApiResponse`、`TrustedHeaders`                      | [README](common-security/README.md) |

`WCS` / `RCS` 尚未建模块，网关路由已为它们预留 `/api/wcs`、`/api/rcs` 前缀。

另有 `docs/`（跨模块契约、模块决策与坑、门禁技术笔记）、`docker/`（本地中间件 compose + `initdb` 建库脚本）、`gradle/`（wrapper、SpotBugs 豁免、commit-msg 脚本）；跨模块公共配置与门禁任务在 `build.gradle.kts`。

## 请求链路

浏览器 → 网关 `28080` 的 `/api/{service}/**` → Nacos 发现 → `lb://{service}`，`StripPrefix=2` 剥掉两段前缀，**服务内只写裸路径**。设备侧不走 HTTP，由 `ecs-service` 的协议适配器接入。

链路成立依赖一批「只在一侧改就静默失效」的跨模块约定（密钥一致、两套白名单、撤销链、身份头、可信 IP），集中在 [`docs/architecture.md`](docs/architecture.md)。[架构图](docs/architecture/v2/zen-iot-architecture.html) 是某一时刻的快照，不与代码同步维护。

## 环境要求

JDK 25（`JAVA_HOME`）、Gradle 9.7.1（已含 wrapper）、Node.js 20+（Markdown 格式化经 `npx`，首次运行需联网）、Docker（本地中间件）。

## 本地运行

1. 起中间件：`docker compose -f docker/docker-compose.dev.yml up -d` —— MySQL `3306`（root/root，建 `zen_admin`）、Redis `6379`、Nacos `8848`/`9848`（控制台 `8080`）、RabbitMQ `5672`（管理台 `15672`，zen/zen）。`initdb/*.sql` 只在 MySQL 数据卷为空时执行，已有卷的机器需手工补一次 `zen_ecs` 建库，命令见该文件头注释。
2. 装 Git 钩子：跑一次任意 Gradle 任务（如 `./gradlew help`）即生成 `.git/hooks`。
3. 起服务：`./gradlew :admin-service:bootRun`，同理 `:ecs-service:` / `:gateway-service:`。表结构与种子数据由 Flyway 自动灌，种子账号见 `admin-service/src/main/resources/db/migration/V2__seed_auth.sql`。
4. 冒烟：经网关 `POST /api/admin/auth/login` 拿 Token，带 `Authorization: Bearer <token>` 访问 `GET /api/admin/demo/admin`；服务自己的 OpenAPI 文档在 `http://localhost:28081/swagger-ui.html`（不经网关）。

命令与门禁矩阵（格式化、测试、架构约束、全量 `check`）以 [`AGENTS.md`](AGENTS.md) 为唯一事实源，本文件不重复。

## 文档分工

| 想搞清楚的事                     | 看哪里                     |
| -------------------------------- | -------------------------- |
| 项目是什么、怎么跑起来           | 本文件                     |
| 命令、门禁、硬性约定             | `AGENTS.md`                |
| 跨模块必须同时成立的约定         | `docs/architecture.md`     |
| 某个模块内部怎么实现、什么被禁   | 该模块的 `README.md`       |
| 某个模块为什么这样选、踩过什么坑 | `docs/modules/<module>.md` |
| 某项门禁技术是什么、产物去哪看   | `docs/quality-gates.md`    |

## 安全

漏洞上报与密钥泄漏处置见 [`SECURITY.md`](SECURITY.md)。MIT License。
