# 依赖与版本治理

> **这篇文档的位置**：版本的**真源**是根 `build.gradle.kts` 的 `configure<DependencyManagementExtension>` 与 `settings.gradle.kts` 的 `dependencyResolutionManagement`——坐标与版本号以那两份脚本为准，本文不复述那些清单，只给口径。本文只回答三个问题：加一个依赖放哪一层、`spring-cloud-alibaba` 与 Boot 插件怎么互相约束、Dependabot 能自动抬到哪一步。
>
> 门禁怎么跑、报告在哪看是门禁文档的职责，见 [`docs/quality-gates.md`](quality-gates.md) §2.7；模块内部的取舍见 `docs/modules/<module>.md`。

## 1. 版本只在根声明一次

三种声明形状：

| 形状                   | 用在什么坐标上                           | 声明位置                                    |
| ---------------------- | ---------------------------------------- | ------------------------------------------- |
| `mavenBom` import      | 整条栈有官方 BOM                         | 根 `build.gradle.kts` 的 `imports { }`      |
| 单条 `dependency(...)` | 坐标不在任何 BOM 里（ArchUnit 这样进来） | 根 `build.gradle.kts` 的 `dependencies { }` |
| 裸坐标 + scope         | 已被上面某条托管                         | 模块 `build.gradle.kts`                     |

加新依赖按三条判据过一遍：坐标能在已 import 的 BOM 里查到 → 模块脚本只写裸坐标；新栈自带官方 BOM → 在根部追加一条 `mavenBom`；两者都不满足 → 在根部加单条 `dependency(...)` 约束。**模块脚本一律不写版本号。**

四个 import 覆盖的坐标基本不相交，因此通常谈不上「谁覆盖谁」；真正会互相打架的只有 `spring-cloud-alibaba` 那些直接声明的坐标，处理口径见 §2。

两条不能违反的理由：

- **模块局部的 BOM 与约束不会传递给消费方。** `common-security` 以 `api` 暴露 jjwt 类型，所以 jjwt 的版本必须在根统一管——写在模块里只有生产方自己看得见，消费方仍会解析到别的版本。
- **多一个版本真源，`crossModuleVersionCheck` 的比对就失去意义。** 它比的是「同一坐标在各模块解析出的版本」；而只钉住组内一个构件、不动 BOM 管的兄弟构件，正是历史上把 SpotBugs 配置拆成两个版本的成因（`#15`、`#20`，故事在 [`docs/quality-gates.md`](quality-gates.md) §2.7）。

配套事实：仓库声明集中在 `settings.gradle.kts` 的 `dependencyResolutionManagement`，设 `RepositoriesMode.PREFER_SETTINGS`，子项目不再各自声明 `repositories`；插件版本同样一律硬编码（`settings.gradle.kts` 与根脚本的 `plugins` 块），不走版本目录。

## 2. `spring-cloud-alibaba` 与 Boot 插件

- **耦合点**：`com.alibaba.cloud` 的 BOM 与 `spring-boot-dependencies`、`spring-cloud-dependencies` 并列 import，而它内部有**直接声明**的坐标；在**只挂 `io.spring.dependency-management`** 的模块里，这些直接声明会压过 Boot BOM 的托管值。
- **口径**：每个模块都挂 `org.springframework.boot` 插件，`java-library` 的 `common-core` / `common-security` 也不例外——挂上之后 SCA 的直接声明不再生效。库模块的代价是显式关掉 `bootJar`、重新启用 `jar` 并把 classifier 复位。
- 实测数据与代价见 [`docs/modules/common-core.md`](modules/common-core.md)，`common-security` 同口径；把这条反直觉行为变成机器可查断言的机制见 [`docs/quality-gates.md`](quality-gates.md) §2.7。

升级时的三个耦合点：

1. `org.springframework.boot` 插件版本与 `spring-boot-dependencies` 的 `mavenBom` 版本必须同值（现在都是 `4.1.1`）：两者各自带来一套依赖管理，分开动意味着托管来源不是同一份 BOM。
2. `spring-cloud-dependencies` 与 `spring-cloud-alibaba-dependencies` 的版本号前缀必须是同一条 release train（现在都是 `2025.1`）：升 Cloud train 要先确认有对应的 SCA train，小数点后的修订由 SCA 自己走。
3. 改完跑 `./gradlew crossModuleVersionCheck`——同一坐标出现多版本时它会先红，不必等真启动才炸。

## 3. Dependabot 的边界

自动化只覆盖三处，`directory` 就是覆盖面：

| ecosystem      | directory | 它看得见什么                                            |
| -------------- | --------- | ------------------------------------------------------- |
| gradle         | `/`       | 根脚本与 `settings.gradle.kts` 里硬编码的坐标与插件版本 |
| npm            | `/web`    | 前端独立的 `package.json` 与 `pnpm-lock.yaml`           |
| github-actions | `/`       | workflow 里用到的 action 版本                           |

- **gradle 只指 `/` 不是遗漏**：按 §1 的规则，模块脚本不写版本号，所以可抬的坐标都落在根——这是「版本只在根声明一次」换来的直接收益。
- **分组**：gradle 与 npm 都只把 patch + minor 合成一个 PR，major 单独走（破坏性风险）。npm 另有一组 `antd` 三件套，且这一组**必须排在 `patch-and-minor` 之前**——一个依赖同时命中多组时只有最先匹配的那组收走它，顺序即优先级。三件套必须同进同出：`@ant-design/cli` 的检查规则与 `@ant-design/icons` 的 major 都锚在某个 antd 版本上，分开升会让检查器与被检对象错位。github-actions 不分组，节奏也不同（月更，依赖是周更）。
- **没有 `ignore` 名单，也没有被刻意排除的依赖**：文件里一条 `ignore` 都没有——边界靠覆盖面与把关实现，不靠排除清单。把关是每个 PR 自动触发 CI（gradle 与 npm 的 PR 分别走 `ci.yml` 与 `web-ci.yml`）且不自动合并。
- **代价**：它买来了耦合——插件版本抬一格可能同时牵动五个模块，PR 历史就是这笔成本的真实账单（整体口径见 [`docs/quality-gates.md`](quality-gates.md) §3）。
