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
- **Node.js**: 用于 Markdown 格式化（Spotless 自动管理）

## Gradle 命令参考

### 代码格式化

| 命令                          | 说明                                                              |
| ----------------------------- | ----------------------------------------------------------------- |
| `./gradlew spotlessApply`     | 格式化所有 Java + Markdown 文件，并自动修复 markdownlint 规范问题 |
| `./gradlew spotlessCheck`     | 检查格式是否正确（不修改文件）                                    |
| `./gradlew spotlessJavaApply` | 仅格式化 Java 文件                                                |
| `./gradlew spotlessJavaCheck` | 仅检查 Java 格式                                                  |
| `./gradlew lintMarkdown`      | 使用 markdownlint 检查 Markdown 规范                              |
| `./gradlew lintMarkdownFix`   | 自动修复 Markdown 规范问题（markdownlint --fix）                  |

### 构建与编译

| 命令                    | 说明                               |
| ----------------------- | ---------------------------------- |
| `./gradlew build`       | 构建所有模块（编译 + 测试 + 打包） |
| `./gradlew assemble`    | 仅打包，不运行测试                 |
| `./gradlew clean`       | 清理所有模块的 build 目录          |
| `./gradlew classes`     | 编译所有模块的主代码               |
| `./gradlew testClasses` | 编译所有模块的测试代码             |

### 测试

| 命令                      | 说明                                                |
| ------------------------- | --------------------------------------------------- |
| `./gradlew test`          | 运行所有模块的测试                                  |
| `./gradlew check`         | 运行所有检查（spotlessCheck + lintMarkdown + test） |
| `./gradlew check -x test` | 运行检查但跳过测试                                  |

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

- Java 代码使用 google-java-format 格式化
- Markdown 文档使用 Prettier 格式化
- 提交前运行 `./gradlew spotlessApply` 确保代码格式正确
- 提交前运行 `./gradlew lintMarkdown` 确保文档规范

## 许可证

MIT License
