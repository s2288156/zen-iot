# Zen IoT

面向仓储 / 工厂场景的物联网管理平台：统一网关鉴权、认证与授权基线、外部设备（PLC / 光栅 / 自动门 / 充电桩）的档案、在线态与指令中转。

技术栈为 **Spring Boot 4.1 + Gradle 多模块**。网关是反应式应用（Spring Cloud Gateway / WebFlux），业务服务是 Servlet 栈（Spring MVC + JPA），两侧靠同一份 Web 无关的安全内核复用验签代码。中间件：MySQL 8（每服务一库）、Redis（令牌黑名单与设备心跳）、RabbitMQ 4（跨服务指令）、Nacos（服务注册）、Flyway（建库）。

> 当前落地到 Phase 3（骨架 → 认证 → 网关 → ECS 设备管理）。WCS / RCS 尚未建模块，网关路由已为它们预留前缀。进度事实源：`.ai/dev-plan/实施进度.md`。

## 模块

| 模块              | 端口  | 职责                                            | 细节                                |
| ----------------- | ----- | ----------------------------------------------- | ----------------------------------- |
| `gateway-service` | 28080 | API 网关：路由、JWT 鉴权、身份透传              | [README](gateway-service/README.md) |
| `admin-service`   | 28081 | 认证与授权基线（库 `zen_admin`）                | [README](admin-service/README.md)   |
| `ecs-service`     | 28084 | 外部设备管理与协议适配（库 `zen_ecs`）          | [README](ecs-service/README.md)     |
| `common-core`     | —     | Servlet 业务服务的公共模块                      | [README](common-core/README.md)     |
| `common-security` | —     | Web 无关的安全内核（JWT、响应契约、身份头契约） | [README](common-security/README.md) |

```text
zen-iot/
├── {gateway,admin,ecs}-service/   # 可运行应用，各自 README 说明实现与约束
├── common-{core,security}/        # 库模块
├── docker/                        # 本地中间件 compose + initdb 建库脚本
├── gradle/                        # wrapper、SpotBugs 豁免、commit-msg 脚本
├── build.gradle.kts               # 跨模块公共配置与门禁任务
└── AGENTS.md                      # 门禁矩阵与硬性约定（唯一事实源）
```

## 请求链路

浏览器 → 网关 `28080` 的 `/api/{admin,wcs,rcs,ecs}/**` → Nacos 发现 → `lb://{service}`，`StripPrefix=2` 剥掉两段前缀，因此**服务内只写裸路径**，`/api/{service}` 前缀只存在于网关。网关验签后覆写 `X-User-Id`/`X-Username`/`X-User-Roles`/`X-User-Modules`，入站同名头一律先剥离。设备侧不走 HTTP，由 `ecs-service` 的协议适配器接入。

## 环境要求

- **Java**：JDK 21（`JAVA_HOME`）
- **Gradle**：9.7.1（已包含 wrapper）
- **Node.js**：20 及以上——Markdown 格式化与 markdownlint 通过 `npx` 调用（Spotless 管理依赖，首次运行需联网）
- **Docker**：跑本地中间件

## 本地运行

1. 起中间件：`docker compose -f docker/docker-compose.dev.yml up -d`
   MySQL `3306`（root/root，建 `zen_admin`）、Redis `6379`、Nacos `8848`/`9848`、RabbitMQ `5672`（管理台 `15672`，zen/zen）。
   `initdb/*.sql` 只在 MySQL 数据卷为空时执行，已有卷的机器需手工补一次 `zen_ecs` 建库，命令见该文件头注释。
2. 装 Git 钩子：执行一次任意 Gradle 任务（如 `./gradlew help`）即按 `settings.gradle.kts` 生成 `.git/hooks`。
3. 起服务：`./gradlew :admin-service:bootRun`、`:ecs-service:bootRun`、`:gateway-service:bootRun`。表结构与种子数据由 Flyway 自动灌（种子账号见 `admin-service/src/main/resources/db/migration/V2__seed_auth.sql`）。
4. 冒烟：经网关 `POST /api/admin/auth/login` 拿 Token，带 `Authorization: Bearer <token>` 访问 `GET /api/admin/demo/admin`；服务自己的 OpenAPI 文档在 `http://localhost:28081/swagger-ui.html`（不经网关）。

## 常用命令

| 命令                         | 说明                                                            |
| ---------------------------- | --------------------------------------------------------------- |
| `./gradlew spotlessApply`    | 格式化 Java + Markdown + kts + YAML，并自动修 markdownlint 问题 |
| `./gradlew test`             | 跑不依赖本机中间件的测试（`@Tag("integration")` 默认排除）      |
| `./gradlew architectureTest` | 只跑 ArchUnit 架构约束（秒级，不启动 Spring 上下文）            |
| `./gradlew check`            | 全量门禁：格式 + Markdown + 编译零告警 + SpotBugs + 架构 + 测试 |
| `./gradlew prePushCheck`     | 推送门禁：同上但不跑测试（pre-push 钩子执行它）                 |

完整命令与门禁矩阵见 [`AGENTS.md`](AGENTS.md)；打包、集成测试等模块级命令见各模块 README。

## 开发规范

- 硬性约定与门禁（零告警编译、jspecify 可空性、分层与编码卫生、SpotBugs 豁免政策、集成测试打 tag、Conventional Commits）以 [`AGENTS.md`](AGENTS.md) 为唯一事实源，这里不重复。
- 各模块的实现细节与架构约束写在**该模块目录的 `README.md`**，与本文件分工：这里回答「项目是什么、怎么跑起来」，模块文档回答「这个模块内部怎么实现、什么被禁」。
- 依赖本机中间件的测试一律打 `@Tag("integration")`，否则 `./gradlew check` 在干净机器上就是红的。

## 安全

漏洞上报与密钥泄漏处置见 [`SECURITY.md`](SECURITY.md)。

## 许可证

MIT License
