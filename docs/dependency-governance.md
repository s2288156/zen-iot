# 依赖与版本治理

> **这篇文档放在哪**：依赖版本只在根 `build.gradle.kts` 的 `configure<DependencyManagementExtension>` 与 `settings.gradle.kts` 的 `dependencyResolutionManagement` 两处声明——某个坐标用哪个版本，以这两份脚本为准。本文不把清单再抄一遍，只说明背后的规则；它回答三个问题：新加一个依赖该写在哪一层、`spring-cloud-alibaba` 与 Boot 插件之间有哪些互相牵制、Dependabot 能自动把版本升到哪一步。
>
> 门禁怎么跑、报告去哪看属于门禁文档的范围，见 [`docs/quality-gates.md`](quality-gates.md) §2.7；某个模块为什么这样取舍，见 `docs/modules/<module>.md`。

## 1. 版本只在根声明一次

依赖共有三种声明方式，区别只在「版本号由谁提供」：

| 声明方式                     | 适用情况                                       | 写在哪里                                    |
| ---------------------------- | ---------------------------------------------- | ------------------------------------------- |
| import 一份 `mavenBom`       | 整条技术栈有官方 BOM                           | 根 `build.gradle.kts` 的 `imports { }`      |
| 单独写一条 `dependency(...)` | 该坐标不在任何 BOM 里（ArchUnit 就是这种情况） | 根 `build.gradle.kts` 的 `dependencies { }` |
| 只写坐标，不写版本           | 版本已由上面两种声明之一提供                   | 模块 `build.gradle.kts`                     |

新加依赖时按下面三步判断，就能确定该写在哪：

1. 能在已 import 的 BOM 里查到该坐标 → 模块脚本只写坐标，不写版本号；
2. 它是自带官方 BOM 的新技术栈 → 在根部 `imports { }` 里追加一条 `mavenBom`；
3. 以上两条都不满足 → 在根部 `dependencies { }` 里加一条 `dependency(...)` 约束。

**无论哪种情况，模块脚本都不写版本号。**

四个 import 覆盖的坐标基本互不重叠，所以一般不存在「谁覆盖谁」的问题；真正会冲突的只有 `spring-cloud-alibaba` 直接写明版本的那些坐标，处理方式见 §2。

这样做有两条硬理由：

- **写在模块里的 BOM 和约束只对该模块生效，不会传给使用它的模块。** `common-security` 用 `api` 把 jjwt 的类型暴露给了别人，所以 jjwt 的版本必须在根部统一管；写在自己模块里，只有它自己看得见，别的模块仍会解析到另一个版本。
- **版本号多出第二个声明处，`crossModuleVersionCheck` 的比对就失去意义。** 这个门禁比的是「同一个坐标在各个模块里解析出的版本是否一致」；而只给组内一个构件钉死版本、不管 BOM 里其余同组构件，正是当初把 SpotBugs 拆成两个版本的起因（`#15`、`#20`，经过见 [`docs/quality-gates.md`](quality-gates.md) §2.7）。

另外两条相关约定：仓库地址统一声明在 `settings.gradle.kts` 的 `dependencyResolutionManagement`，并设了 `RepositoriesMode.PREFER_SETTINGS`，所以子项目不再各自写 `repositories`；插件版本同样一律直接写在脚本里（`settings.gradle.kts` 与根脚本的 `plugins` 块），没有使用版本目录。

## 2. `spring-cloud-alibaba` 与 Boot 插件

**冲突从哪来**：`com.alibaba.cloud` 的 BOM 和 `spring-boot-dependencies`、`spring-cloud-dependencies` 是并列 import 的，但它的 BOM 里有直接写明版本的坐标。如果某个模块**只挂了 `io.spring.dependency-management`**，这些直接写明的版本就会盖过 Boot BOM 提供的版本。

**项目怎么处理**：每个模块都挂 `org.springframework.boot` 插件，连 `java-library` 的 `common-core`、`common-security` 也不例外——挂上之后，SCA 直接写明的版本就不再生效。库模块要为此多付一点代价：显式关掉 `bootJar`、重新启用 `jar`、并把 classifier 复位。

实测数据与这份代价见 [`docs/modules/common-core.md`](modules/common-core.md)，`common-security` 做法相同；把这条反直觉的行为变成可自动检查的断言，机制见 [`docs/quality-gates.md`](quality-gates.md) §2.7。

升级任何一侧时，有三处会互相牵制：

1. `org.springframework.boot` 插件版本必须与 `spring-boot-dependencies` 那条 `mavenBom` 的版本一致（现在同为 `4.1.1`）。两者各带一套依赖管理，只动其中一个，等于让版本不再来自同一份 BOM。
2. `spring-cloud-dependencies` 与 `spring-cloud-alibaba-dependencies` 的版本前缀必须落在同一条 release train 上（现在同为 `2025.1`）。要升 Cloud 的 train，先确认 SCA 有对应的 train；小数点之后的修订交给 SCA 自己走。
3. 改完跑一次 `./gradlew crossModuleVersionCheck`——同一坐标出现多个版本时它会先报错，不必等到服务启动才发现。

## 3. Dependabot 的边界

Dependabot 只覆盖三个位置，`directory` 指到哪里就扫到哪里：

| 生态           | directory | 它能看到什么                                          |
| -------------- | --------- | ----------------------------------------------------- |
| gradle         | `/`       | 根脚本与 `settings.gradle.kts` 里手写的坐标与插件版本 |
| npm            | `/web`    | 前端独立的 `package.json` 与 `pnpm-lock.yaml`         |
| github-actions | `/`       | workflow 里用到的 action 版本                         |

- **gradle 只写 `/` 不是漏写**：按 §1 的约定，模块脚本里没有版本号，能升的坐标本来就都在根——这正是「版本只在根声明一次」带来的好处。
- **分组怎么分的**：gradle 与 npm 都把 patch 和 minor 合成一个 PR，major 单独开（有破坏风险）。npm 另外有一组 `antd` 三件套，它**必须排在 `patch-and-minor` 之前**：一个依赖同时命中多个分组时，只会归入最先匹配的那一组，顺序就决定归属。这三件必须一起升，因为 `@ant-design/cli` 的检查规则和 `@ant-design/icons` 的 major 都对应某个 antd 版本，拆开升会让检查的一方和被检查的一方对不上。github-actions 不分组，因为节奏不同（月更，而依赖是周更）。
- **没有任何依赖被刻意排除**：文件里没有一条 `ignore`。它的边界靠覆盖范围和 CI 校验来划，不靠排除名单——每个 PR 都会自动触发 CI（gradle 与 npm 的 PR 分别走 `ci.yml` 与 `web-ci.yml`），且不会自动合并。
- **Node 版本同源这条靠门禁，不靠评审**：`web/package.json` 的 `engines.node`、`web-ci.yml` 的 `node-version` 与 `@types/node` 解析到的版本必须指向同一个 Node 大版本，由 `web/scripts/node-types-gate.mjs` 在 `web-ci.yml` 里断言（脚本跑在 `node-version` 给的运行时上，所以只需比对另两处）。这条 Dependabot 看不见：npm 生态只抬 `package.json`，`node-version` 不是任何生态的依赖项——上一条说的「靠覆盖范围划边界」在这里正好留了个洞，靠脚本补。
- **`apifox-sync.yml` 的 Node 22 不属于这条同源约束**：那里的 `setup-node` 只为 `npm i -g apifox-cli` 服务，与前端运行时无关。别为了「全仓统一」把它一起抬——那等于把一个第三方 CLI 的兼容矩阵绑进前端的升级节奏。
- **代价**：这套自动化换来的是耦合——插件版本动一格，可能五个模块一起受影响。[`docs/quality-gates.md`](quality-gates.md) §3 记录的就是这笔成本。
