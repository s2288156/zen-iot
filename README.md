# Zen IoT

基于 Spring Boot 4.1 + Gradle 多模块架构的物联网管理平台。

## 项目结构

```text
zen-iot/
├── admin-service/     # 后台管理服务
├── common-core/       # 公共模块（统一响应、异常、审计、分页）
└── build.gradle.kts   # 根项目配置
```

## 环境要求

- **Java**: JDK 21
- **Gradle**: 9.7.1（已包含 wrapper）
- **Node.js**: 20 及以上，Markdown 格式化与 markdownlint 通过 `npx` 调用（Spotless 自动管理依赖，首次运行需联网）

## Gradle 命令参考

### 代码格式化

| 命令                          | 说明                                                                                   |
| ----------------------------- | -------------------------------------------------------------------------------------- |
| `./gradlew spotlessApply`     | 格式化所有 Java + Markdown + Gradle 脚本 + YAML 文件，并自动修复 markdownlint 规范问题 |
| `./gradlew spotlessCheck`     | 检查格式是否正确（不修改文件）                                                         |
| `./gradlew spotlessJavaApply` | 仅格式化 Java 文件                                                                     |
| `./gradlew spotlessJavaCheck` | 仅检查 Java 格式                                                                       |
| `./gradlew lintMarkdown`      | 使用 markdownlint 检查 Markdown 规范                                                   |
| `./gradlew lintMarkdownFix`   | 自动修复 Markdown 规范问题（markdownlint --fix）                                       |

### 构建与编译

| 命令                    | 说明                               |
| ----------------------- | ---------------------------------- |
| `./gradlew build`       | 构建所有模块（编译 + 测试 + 打包） |
| `./gradlew assemble`    | 仅打包，不运行测试                 |
| `./gradlew clean`       | 清理所有模块的 build 目录          |
| `./gradlew classes`     | 编译所有模块的主代码               |
| `./gradlew testClasses` | 编译所有模块的测试代码             |

### 测试

| 命令                                | 说明                                                                          |
| ----------------------------------- | ----------------------------------------------------------------------------- |
| `./gradlew test`                    | 运行所有模块的测试（默认排除 `@Tag("integration")`，无需本机中间件）          |
| `./gradlew test -PintegrationTests` | 连同依赖本机 MySQL/Nacos/Redis 的集成测试一起跑                               |
| `./gradlew architectureTest`        | 只跑 ArchUnit 架构约束（秒级，不启动 Spring 上下文）                          |
| `./gradlew spotbugsMain`            | SpotBugs 缺陷扫描（主代码）                                                   |
| `./gradlew check`                   | 全量门禁：格式 + Markdown 规范 + 编译零告警 + SpotBugs + 架构约束 + 测试      |
| `./gradlew check -x test`           | 运行检查但跳过测试                                                            |
| `./gradlew prePushCheck`            | 推送门禁：格式 + Markdown 规范 + 编译零告警 + SpotBugs + 架构约束（不跑测试） |

### 运行应用

| 命令                                   | 说明                         |
| -------------------------------------- | ---------------------------- |
| `./gradlew :admin-service:bootRun`     | 启动 admin-service           |
| `./gradlew :admin-service:bootTestRun` | 以测试配置启动 admin-service |

### 打包部署

| 命令                                      | 说明                              |
| ----------------------------------------- | --------------------------------- |
| `./gradlew :admin-service:bootJar`        | 打包 admin-service 为可执行 JAR   |
| `./gradlew :admin-service:bootBuildImage` | 构建 admin-service 的 Docker 镜像 |

### 依赖管理

| 命令                                                             | 说明                      |
| ---------------------------------------------------------------- | ------------------------- |
| `./gradlew dependencies`                                         | 查看根项目依赖树          |
| `./gradlew :admin-service:dependencies`                          | 查看 admin-service 依赖树 |
| `./gradlew :admin-service:dependencyInsight --dependency <name>` | 查看特定依赖的详细信息    |

### 辅助命令

| 命令                         | 说明                         |
| ---------------------------- | ---------------------------- |
| `./gradlew tasks`            | 列出所有可用任务             |
| `./gradlew tasks --all`      | 列出所有任务（包括隐藏任务） |
| `./gradlew help`             | 显示帮助信息                 |
| `./gradlew spotlessDiagnose` | 诊断 Spotless 配置问题       |

## 模块说明

### admin-service

后台管理服务，提供用户管理、权限控制、系统配置等功能。

**主要依赖：**

- Spring Boot Web
- Spring Data JPA
- Spring Security
- Spring Data Redis
- MySQL Connector
- Lombok

### common-core

各业务服务的公共模块。依赖后即自动装配（通过 `AutoConfiguration.imports`，无需修改启动类）：

- **统一 API 响应**：`ApiResponse<T>`（code/message/data，成功码 200）
- **错误码与业务异常**：`ErrorCode` 接口 + `GlobalErrorCode` 通用枚举 + `BusinessException`，业务服务可自建枚举实现 `ErrorCode` 扩展码段
- **全局异常处理器**：`GlobalExceptionHandler`（`@RestControllerAdvice`，业务服务可注册自己的 Bean 接管）
- **DAO 基础实体**：`BaseEntity`（`id` + `create_time`/`update_time`/`creator`/`updater` 审计字段）
- **分页工具**：`PageQuery`（转 Spring Data `Pageable`）/ `PageResult<T>`（由 `Page<T>` 构建）
- **JPA 审计配置**：自动启用 `@EnableJpaAuditing`，默认审计人为 `"system"`，业务服务定义自己的 `AuditorAware<String>` Bean 即可覆盖

**主要依赖：** Spring Boot Web、Validation、Data JPA、Lombok

## 开发规范

### 格式化

- Java 代码使用 palantir-java-format 格式化（版本见 `build.gradle.kts` 的 Spotless 配置）
- Markdown 文档使用 Prettier 格式化，并按 markdownlint 校验
- `*.gradle.kts` 只校「行尾空白 + 文件结尾换行」，刻意不引入 ktlint，避免把既有 tab 缩进整体重排
- `*.yml` 走 Prettier；Flyway 的 `db/migration/*.sql` **不纳入自动格式化**，迁移脚本是历史记录，重排会让 diff 无法审查
- `.editorconfig` 与 palantir / Prettier 的实际产出对齐（Java 4 空格 120 列、YAML 2 空格、kts 沿用 tab、仓库统一 LF）
- 提交前运行 `./gradlew spotlessApply` 确保代码格式正确
- 提交前运行 `./gradlew lintMarkdown` 确保文档规范

### 静态检查与架构约束

| 层面 | 规则与开关                                                                                                                              |
| ---- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 编译 | `-Xlint:deprecation,unchecked -Werror`：告警即失败。不用 `-Xlint:all`，否则 Lombok 的 `No processor claimed...` 会直接炸构建            |
| 缺陷 | SpotBugs 4.10.4，`Effort.DEFAULT` + `Confidence.MEDIUM`，`ignoreFailures=false`；报告在 `<模块>/build/reports/spotbugs/main.{html,xml}` |
| 架构 | ArchUnit 1.5.0，各模块 `ArchitectureTest`（`@Tag("architecture")`），无需 Spring 上下文                                                 |
| 豁免 | 只允许改 `gradle/spotbugs/exclude.xml`，且必须写理由、范围收窄到「单条规则 + 单个包」；能用代码修掉的优先改代码                         |

现有架构约束（违反即 `architectureTest` 失败）：

- `admin-service`：`controller → service → repository → entity` 单向；controller 不得依赖持久化实体；`@Transactional` 只出现在 `service` 层
- `common-core`：不得依赖任何业务服务包；`com.zen.common.core.jwt` 不得引用 `jakarta.servlet`（Phase 2 网关要复用同一份 JWT 代码）
- 全局：禁止使用 Spring Framework 7 已废弃的 `org.springframework.lang.Nullable` / `NonNull`，需要标注可空性时用 `org.jspecify.annotations.*`

依赖本机中间件的测试必须打 `@Tag("integration")`，`test`/`check` 默认排除它们，因此 `./gradlew check` 在干净机器上也是可执行的真门禁；容器起来后用 `./gradlew test -PintegrationTests` 补跑。

### Git 钩子

克隆仓库后执行一次任意 Gradle 任务（如 `./gradlew help`），`org.danilopianini.gradle-pre-commit-git-hooks` 插件会按 `settings.gradle.kts` 里的 `gitHooks` 声明生成 `.git/hooks` 下的三个钩子：

| 钩子         | 执行内容                                                                                                        |
| ------------ | --------------------------------------------------------------------------------------------------------------- |
| `pre-commit` | `./gradlew spotlessCheck`，Java/Markdown 格式不合规即拒绝提交（daemon 热时约 2 秒）                             |
| `commit-msg` | Conventional Commits 校验，type 限 `fix/feat/build/chore/ci/docs/perf/refactor/revert/style/test`，scope 可省略 |
| `pre-push`   | `./gradlew prePushCheck`，格式 + Markdown 规范 + 全模块编译（`-Werror`）+ SpotBugs(main) + 架构约束，不跑测试   |

- 钩子文件由构建脚本生成，不要手工编辑：每次 Gradle 调用都会按脚本声明覆盖回去。
- 全量门禁仍是 `./gradlew check`（格式 + 编译 + SpotBugs + 架构约束 + 非集成测试）。pre-push 不跑测试只为省时间；`admin-service` 依赖本机 MySQL 8 / Nacos / Redis 的 `@SpringBootTest` 已打 `@Tag("integration")`，容器没起时也不会误报。
- `commit-msg` 的 scope 只接受字母、数字、空格与 `/ + -`，`.ai`、`common_core` 这类写法会被拒。
- `.git` 不可写的环境（agent 沙箱、源码包）用 `./gradlew <task> -PskipGitHooks` 跳过钩子安装。
- 插件只生成上述三个钩子，第三方工具已有的 `post-commit` / `post-checkout` 不受影响。

## 许可证

MIT License
