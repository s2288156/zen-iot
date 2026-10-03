# Coder 开发规范

做最小且正确的改动，遵循既有模式，并验证结果。只有在请求不清楚、确实存在有意义的设计取舍，或操作具有破坏性时才提问。如果你想为这些文档中的任何规则开例外，先停下来取得明确许可。

正确性优先于附和。不确定就明说不确定，而不是猜；对技术上站不住脚的请求，带着证据反驳。

## 前置条件

- **Java**：JDK 25，由根 `build.gradle.kts` 的 `toolchain` 强制；仓库未配置工具链自动下载，本机没有 25 就直接失败
- **构建工具**：Gradle 9.7.1
- **Node.js**：20 或更新（Spotless 的 Prettier 与 markdownlint-cli 经 `npx` 运行；首次运行需要网络）

## 常用命令

| 任务        | 命令                                | 说明                                                                     |
| ----------- | ----------------------------------- | ------------------------------------------------------------------------ |
| 全量格式化  | `./gradlew spotlessApply`           | 就地格式化 Java + Markdown + kts + YAML，随后自动修复 Markdown lint 问题 |
| 检查格式    | `./gradlew spotlessCheck`           | 只校验格式，不修改文件                                                   |
| 格式化 Java | `./gradlew spotlessJavaApply`       | 仅格式化 Java 文件                                                       |
| Lint 文档   | `./gradlew lintMarkdown`            | 用 markdownlint 检查 Markdown 规范（不在 `spotlessCheck` 内）            |
| 单元测试    | `./gradlew test`                    | 运行不依赖本地中间件的测试（集成测试被标记排除）                         |
| 集成测试    | `./gradlew test -PintegrationTests` | 包含 `@Tag("integration")` 测试；需要本地 MySQL/Nacos/Redis 容器         |
| 架构约束    | `./gradlew architectureTest`        | ArchUnit 分层与依赖方向规则，不启动 Spring 上下文                        |
| 缺陷扫描    | `./gradlew spotbugsMain`            | 对 main 源码跑 SpotBugs                                                  |
| 全量检查    | `./gradlew check`                   | 全量门禁，含测试                                                         |
| 推送门禁    | `./gradlew prePushCheck`            | 推送闸，**不跑测试**                                                     |
| 起本机栈    | `./gradlew devUp`                   | docker 中间件 + gateway/admin/ecs + 前端，已在运行的跳过                 |
| 停本机栈    | `./gradlew devDown`                 | 只停 devUp 拉起的应用进程，不动容器；没在跑时是 no-op                    |
| 看本机栈    | `./gradlew devStatus`               | 各目标进程/端口状态与中间件健康，日志在 `build/dev/logs/`                |

各门禁跑到哪一步、包含哪几项，以 `./gradlew tasks` 输出的任务 description 与根 `build.gradle.kts` 为准，本表不重述。

## 强制约定

每条规则都注明执行方式（门禁 / 评审）：走门禁的，绕过的表现是推送失败，而不是评审意见。

- **零编译警告**：每个模块都启用 `-Xlint:deprecation,unchecked -Werror`。不要改成
  `-Xlint:all` —— Lombok 会发出 `No processor claimed any of these annotations`，`-Werror` 直接把它变成构建失败。
  只允许带理由的抑制。
- **空安全**：`org.springframework.lang.Nullable`/`NonNull` 在 Spring Framework 7 中已废弃。需要注解时改用
  `org.jspecify.annotations.*`；`-Werror` 和一条 ArchUnit 规则都会拒绝 Spring 那两个。
- **分层**（ArchUnit，`architectureTest`）：`controller → service → repository → entity` 单向，controller
  绝不接触持久化实体，`@Transactional` 只出现在 `service`，每个 `*Controller`/`*Service`/`*Repository`/`*Entity`
  各居自己的包，包切片保持无环，`common-core` 绝不依赖业务服务或网关；`common-security` 不得引入任何
  传输层或持久化栈（被禁包清单见其 README 的 `moduleStaysFreeOfWebAndPersistence`），也绝不反向指向
  `common-core`。
- **编码卫生**（ArchUnit，按模块断言）：只允许构造器注入，因此字段上绝不出现 `@Autowired`/`@Resource`/`@Inject`；
  不使用 `System.out`/`System.err`/`printStackTrace`，因为只有日志框架会传递 traceId；不使用 `new Date()`，
  而 `java.util.Date` 本身仍然合法，供 jjwt 的 `issuedAt`/`expiration` 使用。
- **SpotBugs**：默认级别即阻断（绕过的表现同样是推送失败）。优先改代码；新的豁免写进
  `gradle/spotbugs/exclude.xml` 并附注释说明理由，且必须保持狭窄（规则 + 包，绝不整类豁免；是否狭窄靠评审）。
  级别配置与报告产物位置见 `docs/quality-gates.md`。
- **测试**：任何需要本地 MySQL/Nacos/Redis 的测试都带 `@Tag("integration")`（漏打不会被拦，靠评审），让
  `test`/`check` 在干净机器上依然可跑。
- **格式化**：`*.java`（palantir）、`*.md`（Prettier + markdownlint）、`*.gradle.kts`、`*.yml`；范围与
  `db/migration/*.sql` 的豁免见根 `build.gradle.kts` 的 `spotless` 块，编辑器行为见 `.editorconfig`。

## 文档地图

本文件是入口：上面的命令表和强制约定在其他任何地方都不存在。其余每份文档只承担一件事，而写在两处的事实一定会漂移。

| 文档                       | 回答什么问题                           | 绝不包含                   |
| -------------------------- | -------------------------------------- | -------------------------- |
| `README.md`（根）          | 平台是什么、模块全景、如何运行         | 进度、计划、日期、门禁规则 |
| `AGENTS.md`（本文件）      | 命令、门禁、硬性约定、这张路由表       | 模块内部实现、设计沿革     |
| `docs/architecture.md`     | 某一侧一动就会静默失效的跨模块契约     | 单模块实现细节             |
| `<module>/README.md`       | 单个模块的事实与结论                   | 理由、被否决的方案、坑     |
| `docs/modules/<module>.md` | 为什么这样选、否决过什么、哪里坑过我们 | 别处已经写过的任何内容     |
| `docs/quality-gates.md`    | 某项门禁技术是什么、如何接进来         | 规则清单                   |
| `SECURITY.md`              | 漏洞上报与密钥处置                     | 架构细节                   |

- **模块 README 骨架** —— 开头一段交代模块职责，随后是 `能力与接口口径` / `关键实现` /
  `架构约束` / `主要依赖` / `命令`。只写事实与结论，每条至多两行；任何需要写「因为」的内容进
  `docs/modules/<module>.md`。
- **不做第二份清单**：绝不手抄代码已经发布的列表（路由与 schema 来自 `/v3/api-docs`，规则列表来自该模块的
  `ArchitectureTest`）。
- **每条坑自带删除条件**：一旦某个门禁或测试让这个错误不可能发生，就删掉这条，而不是任其变质。
- **不引用行号**：写 `路径 + class#method` 或配置键。行号在下一次编辑时就会腐烂。
- **命名与排版**：正文用中文，文件名用英文 kebab-case，一个主题一份文档 —— 不建目录的 `v2` 副本，不在成套的文档
  之上再加索引文件。
- **私有工作区**（`.ai/`、`.trae/`）不纳入版本控制，提交文档也绝不引用它们。

## 工作流

- 框架更换、大规模重构、系统设计这类架构决策，要先讨论再实现。常规修复和意图明确的实现不需要讨论。
- 被提问时，回答问题，而不是直接上手改代码。
- 每次克隆安装一次仓库 Git 钩子：任意 Gradle 调用（例如 `./gradlew help`）都会依据 `settings.gradle.kts` 的
  `gitHooks` 块生成 `pre-commit`（`spotlessCheck`）、`commit-msg`（conventional commits）与 `pre-push`（`prePushCheck`）。
  这三个文件每次都从脚本声明重建，所以绝不手工编辑，也绝不用 `--no-verify` 绕过；`.git` 不可写时（agent 沙箱、
  源码归档）传 `-PskipGitHooks`。
- 迭代期间优先跑针对性的测试与检查。交付前，跑受影响领域所要求的更大范围检查。
- 除非明确要求，不要 force-push。
- Commit 与 PR 标题使用 `type(scope): message`。scope 必须是真实路径，并且包含全部被改动的文件。跨领域改动用更大的
  scope 或不写 scope。
