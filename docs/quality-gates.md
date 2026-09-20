# 代码质量与测试门禁

> **这篇文档的位置**：`AGENTS.md` 是门禁规则的唯一事实源（哪些规则、什么约定），各模块 `README.md` 是该模块约束的清单。本文不重复那份清单，只回答另外三个问题——**这些技术本身是什么、本项目是怎么把它们接进去的、接上之后前后有什么不同，以及跑完去哪看结果**。凡是引用实现细节，都给 `文件:行号`，方便对着源码读。
>
> 阅读前提：本文假设你从没接触过 SpotBugs / ArchUnit / Spotless 这些东西。

---

## 1. 全景：一次改动要过几道闸

门禁不是"一个 CI 任务"，而是按**反馈速度**分层的四道闸。越靠左的闸越快、拦得越浅，越靠右越慢、拦得越深。

| 关口                 | 触发时机              | 执行内容                                                                             | 单进程实测耗时      | 失败后果               |
| -------------------- | --------------------- | ------------------------------------------------------------------------------------ | ------------------- | ---------------------- |
| `pre-commit`         | 每次 `git commit`     | `spotlessCheck`（Java/MD/kts/YAML 格式）                                             | 约 2s（热守护进程） | 提交被拒               |
| `commit-msg`         | 提交信息写完之后      | Conventional Commits 校验（`gradle/git-hooks/commit-msg.sh`）                        | 毫秒                | 提交被拒               |
| `pre-push`           | 每次 `git push`       | `prePushCheck`：格式 + Markdown 规范 + 编译 `-Werror` + SpotBugs + 架构 + 版本一致性 | 见 §6               | 推送被拒，**不跑测试** |
| CI（GitHub Actions） | PR / push main / 定时 | `check`（含测试）、集成测试 + 真中间件、gitleaks、OSV                                | 见 §6               | PR 红灯                |

对应源码：`build.gradle.kts:266`（`prePushCheck`）、`settings.gradle.kts:24`（钩子声明）、`.github/workflows/ci.yml`。

关键设计取舍一句话：**推送闸不跑测试**。`build.gradle.kts:265` 的注释写着"不跑测试；全量门禁仍是 check"。理由是上面这六项覆盖了绝大多数"低级但会污染主干"的问题，而测试要连数据库、启动 Spring 上下文，放进 `pre-push` 会让人开始用 `--no-verify`。闸一旦被人绕过，它就比没有闸更糟。

三个钩子都由 `settings.gradle.kts` 的 `gitHooks { }` 块**生成**，不是手写文件：

```bash
./gradlew help          # 任意一次 Gradle 调用都会重新生成 .git/hooks/{pre-commit,commit-msg,pre-push}
```

所以本地手改钩子文件是无效的（下次调用就被覆盖），这也是 `AGENTS.md` 禁止手改和禁止 `--no-verify` 的技术原因。`.git` 不可写时（沙箱、源码包）加 `-PskipGitHooks`（`settings.gradle.kts:22`）。

---

## 2. 逐项技术讲解

每项按同一个结构：**是什么 → 本项目怎么接 → 接入前后 → 产物**。

### 2.1 Spotless —— 格式化即门禁

**是什么**：一个"格式化即构建步骤"的 Gradle 插件。它不配置编辑器，而是自己持有 formatter（Java 用 palantir-java-format，Markdown/YAML 用 Prettier），把"格式对不对"变成一个可执行断言。`spotlessApply` 改文件，`spotlessCheck` 只判定。

**本项目怎么接**（`build.gradle.kts:161`）：四种语言各一条 target，注意每条都 `targetExclude("**/build/**")`：

- `java`：palantir-java-format 2.97.0 + `removeUnusedImports()` + `toggleOffOn()`（允许用 `// @formatter:off` 局部逃生，生成的代码、对齐的矩阵才用得上）。
- `markdown`：Prettier，`printWidth: 120`、`singleQuote`、`proseWrap: preserve`。
- `gradleScripts`：**只**定尾随空白与文件结尾换行，注释写明了原因——不引入 ktlint，否则既有 tab 缩进的构建脚本会被整体重排，一个改动淹没在几百行 diff 里。
- `yaml`：Prettier。
- 刻意**没有** SQL target：`build.gradle.kts:200` 那行注释说 Flyway 脚本是历史记录，重排会让迁移 diff 无法审查。

**接入前后**：

- 接之前，"缩进风格"是 code review 里反复出现的口水战，`.editorconfig` 只是建议——IDE 不装插件就不生效，成员换编辑器就漂移。
- 接之后 diff 里再没有空白噪音。但它有两个真实的坑，本项目都踩过：
  1. **Spotless 的 `**` 会匹配以点开头的路径**。`.ai/` 是 gitignore 的临时笔记目录，于是**未跟踪文件**反过来卡住了每一次提交——`check`会因为一个根本不在版本库里的 Markdown 文件而红。修法是把`**/.ai/**`加进`targetExclude`（`build.gradle.kts:172`），历史见 `2cbd5f8 fix(build): exclude gitignored .ai scratch docs from spotless markdown`。
  2. 它管不了"内容是否还准确"。格式化通过 ≠ 文档没过期。

**产物**：**没有报告文件**。失败信息只有标准输出里的"哪个文件哪一处不符合格式"，以及一行 `Run 'gradlew spotlessApply' to fix`。这是有意的——格式问题的正确修复动作是"应用"，不是"读报告"。

### 2.2 markdownlint —— 格式的下一层是规范

**是什么**：Spotless 保证"看起来一致"，markdownlint 保证"写得规范"（标题层级不跳级、列表缩进、ATX 风格标题等）。它是 Node 工具，Gradle 通过 `Exec` 任务调 `npx markdownlint-cli`。

**本项目怎么接**：`build.gradle.kts:209` 注册 `lintMarkdown`，`:215` 注册 `lintMarkdownFix`（`--fix`），并把前者挂到 `check` 与 `prePushCheck`、后者挂成 `spotlessApply` 的 `finalizedBy`（`:274`）。这个"Apply 之后自动 fix"的接法意味着：一条 `spotlessApply` 就同时满足 Prettier 和 markdownlint，不需要记两条命令。规则集在 `.markdownlint.json`（`default: true` 起步，逐条放宽：`MD013` 行长关闭、`MD033` 允许内联 HTML、`MD041` 允许首行非标题）。

**接入前后**：这条最直观的前后对比就是本文档——手写 Markdown 的表格列宽是乱的，`spotlessApply` 之后所有表格对齐（本文所有表格都是这么生成的）。另外 `lintMarkdown` 依赖 `npx`，**首次运行需要联网**，这也是 `AGENTS.md` 把 Node.js 20+ 列为前置条件的原因。

**产物**：无文件产物，结果在标准输出。

### 2.3 编译零告警（`-Werror`）—— 把警告当成错误

**是什么**：`javac -Xlint:deprecation,unchecked -Werror`。警告默认不阻断构建，于是废弃 API 会一辈子留在代码里；`-Werror` 让"用了 Spring 7 已废弃的东西"当场编译失败。

**本项目怎么接**：`build.gradle.kts:62` 对**所有**模块的 `JavaCompile` 统一加三个参数，因此五个模块一条不落。

这里有一条"选了 A 而不是 B"的重要记录，值得单独学：

> **为什么不是 `-Xlint:all`？** Lombok 会发出 `No processor claimed any of these annotations` 这条警告，而 `-Werror` 把它变成构建失败——**稳定必红**。所以只取 `deprecation,unchecked`，而这两条已经足够拦住 Spring 7 的废弃 API（注释在 `build.gradle.kts:63`；同一条也写进了 `AGENTS.md`）。

**接入前后**：接之前 `org.springframework.lang.Nullable` 这种"能用但已废弃"的注解会无声扩散；接之后它与一条 ArchUnit 规则形成双保险（`@Tag("architecture")` 的 `neverUsesDeprecatedSpringNullabilityAnnotations`），必须改用 `org.jspecify.annotations.*`。

**产物**：无文件产物，警告/错误在编译输出里。（Gradle 9 另外会把问题归因成一份 HTML 报告，但它只在构建记录到问题时出现，见 §4.6。）

### 2.4 SpotBugs —— 字节码层面的缺陷模式扫描

**是什么**：Checkstyle/PMD 读源码，SpotBugs 读**编译后的字节码**，因此能看出源码里看不见的问题（可变对象被暴露、返回值可空却未检查、格式化参数不匹配等）。规则以 bug pattern 编码，如 `EI_EXPOSE_REP2`。

**本项目怎么接**（`build.gradle.kts:91`）：

- 版本只在根声明：SpotBugs 引擎 `toolVersion 4.10.4`，插件 `com.github.spotbugs 6.5.11`。
- `effort = DEFAULT` + `reportLevel = Confidence.MEDIUM`，且 `ignoreFailures = false` —— 命中即构建失败。
- 豁免走单一集中文件 `gradle/spotbugs/exclude.xml`，全项目共用。

**接入前后**，三个真实案例：

1. **报告默认根本不生成**。6.5.x 不注册任何报告，失败时终端只有 `exit code 1`——一个"会拦人但不给理由"的门禁是不可维权的。所以 `build.gradle.kts:89` 显式建了 HTML + XML 两类报告。这行配置本身就是它自己的前后对比。
2. **能用代码修的绝不写豁免**。`VerifiedToken` 原本命中 4 条 `EI_EXPOSE_REP/EI_EXPOSE_REP2`（构造器存下了外部 `List`）。修法不是豁免而是加一个 `List.copyOf` 的紧凑构造器，4 条 finding 自动消失（`7bbd4b9 refactor(common-core): 构造期用 List.copyOf 固化 Token 的角色与模块列表`）。`exclude.xml` 的头注释把这条政策写死在文件里。
3. **但有些东西确实只能豁免**。`EI_EXPOSE_REP2` 对"构造参数是 Spring 注入的共享单例"这个形状不成立：拷贝 `ObjectMapper` 会丢掉 `spring.jackson` 配置，而且这些 Bean 本来就由容器唯一持有。`exclude.xml` 为此做了三条窄豁免，每条只覆盖**一个 pattern + 一组具名包**，并写明理由；其中一条还记录了"更精确的 `<Class annotation="...Service"/>` 在 4.10.4 实测不生效，所以按包名收敛"。

第 3 个案例里最有信息量的一句：SpotBugs 的不可变性推断在 `HeartbeatService` 上成立、在 `DeviceService` 上不成立（因为 `@Transactional` 需要 CGLIB 代理，service 不能 `final`）——**同一个代码形状两种结果**。这条观察决定了豁免只能按包收敛，不该指望"把代码写漂亮"去绕开静态分析。

**产物**（每个模块两份）：

```text
<module>/build/reports/spotbugs/main.{html,xml}   # 主源码 → spotbugsMain（进 pre-push）
<module>/build/reports/spotbugs/test.{html,xml}   # 测试源码 → spotbugsTest（只进 check）
```

### 2.5 ArchUnit —— 把架构约束写成可执行断言

**是什么**：一个把"依赖关系"变成 JUnit 测试的库。它把 class 文件读成一张依赖图，然后用 fluent API 断言图的模式。价值在于**架构规则通常只存在于某人的脑子里和一份没人读的文档里**，ArchUnit 让它变成 CI 上一行红。

**本项目怎么接**：每个模块一个 `ArchitectureTest`，共 50 个用例（`admin 11 / common-core 6 / common-security 9 / ecs 14 / gateway 10`），全部打 `@Tag("architecture")`，并且：

- 根任务 `architectureTest` 用 `includeTags("architecture")` 单独跑这些（`build.gradle.kts:78`）。注释点明了动机：**秒级、不碰中间件、不启动 Spring 上下文**，因此可以安全放进 `pre-push`。
- 内置规则直接用：`layeredArchitecture()`、`slices().matching("<root>.(*)..").beFreeOfCycles()`、`NO_CLASSES_SHOULD_USE_FIELD_INJECTION`、`NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`。
- 自定义规则用 `noClasses().should().dependOnClassesThat()...` 与 `.callConstructor(...)` / `.callMethod(...)`。

**接入前后**，四个案例：

1. **分层规则有明确盲区，无环规则补上**。commit message 里记着实测：把 `config ↔ controller` 造个环，分层规则**仍然全绿**（它只约束 controller/service/repository/entity 四层之间的方向，config 不在任何层里），是 `packageSlicesAreFreeOfCycles()` 单独报的红灯（`c935dc2`）。这是一次典型的"规则之间不重叠"验证。
2. **防"静默失效"的哨兵**。`noClasses()` 在**一个类都没导入**时也判通过。所以包根改名、目录误删、`importPackages` 写错，会让整个模块的架构规则一起变成零断言绿。`common-security` 与 `ecs-service` 各有一条 `importedClassesAreNeverEmpty()` 断言导入类数下限（`common-security/.../ArchitectureTest.java:36`），专门用来抓这种失效。**这条值得每个新项目抄**——它是唯一一条防守"门禁本身坏掉"的规则。
3. **把模块的存在理由写成断言**。`common-security` 的全部意义是"Web 无关，所以反应式网关能直接复用同一份验签代码"。破坏方式只需要"有人往这个模块加了个用 `HttpServletRequest` 的类"，而编译和 IDE 都不会提醒。于是 `moduleStaysFreeOfWebAndPersistence()` 把这条存在理由变成测试（`common-security/.../ArchitectureTest.java:47`）。
4. **一条规则被拆成两半**。拆模块之前，`common-core` 有一条只盯 `jakarta.servlet` 的 `jwtPackageStaysServletFree()`，网关侧靠 `build.gradle.kts` 里三个 `exclude` 维持隔离。拆出 `common-security` 之后：生产方按包禁全（Servlet / Spring Web / JPA / Spring Data / 事务 / validation），消费方网关再加一道 `GatewayDependencyIsolationTest`。于是同一条边界两端各有一次断言，**一边漏风也会被抓住**。

`GatewayDependencyIsolationTest`（`gateway-service/src/test/java/com/zen/gateway/GatewayDependencyIsolationTest.java`）是 ArchUnit 之外另一种值得学的思路：它不用 ArchUnit，而用 `Class.forName` 从**消费方运行时视角**断言。理由写在类注释里——ArchUnit 看的是编译期依赖，而"某个 `starter-web` 被 Dependabot 抬版本时重新带进来"这种情况编译照样过、IDE 不报警，要等真启动才炸（Boot 一看 Servlet 在类路径上就把应用判成 Servlet 类型，WebFlux 网关启动即失败）。它甚至直接断言 `WebApplicationType.deduce() == REACTIVE`，把 Boot 那个"最容易因一个依赖整体翻车的开关"钉住。

**产物**：ArchUnit 失败就是 JUnit 失败，所以产物是**测试报告**（§4.3），失败消息里含 ArchUnit 打印的违规依赖清单，形如：

```text
Architecture Violation [Priority: MEDIUM] - Rule '...' was violated (3 times):
Method <com.zen.ecs.controller.DeviceController.list()> calls method <...> in (DeviceController.java:42)
```

### 2.6 JaCoCo —— 覆盖率只测量，不设门槛

**是什么**：字节码插桩统计"哪些行被测试执行过"。

**本项目怎么接**：`build.gradle.kts:152` 对所有 `JacocoReport` 打开 XML + HTML，`:158` 用 `tasks.named("test") { finalizedBy("jacocoTestReport") }` 让报告随每次 `test` 自动生成。

**接入前后 / 为什么没有门槛**：注释里那句"不设阈值门禁；数据由 test 任务产出"是这条配置的全部设计意图。百分比阈值是个可被博弈的数字（想达标就写断言弱的测试），而"看不到覆盖率"才是真问题——所以本项目选择只出报告不做门禁。**这意味着覆盖率红灯不会挡你，需要人去看**（§4.4 给了一行命令读 XML）。这是本文里最该被后来者知道的一个"故意留白"。

**产物**：

```text
<module>/build/reports/jacoco/test/jacocoTestReport.xml
<module>/build/reports/jacoco/test/html/index.html      # 逐包/逐类/逐行标色
```

### 2.7 版本一致性 —— 自建的两级门禁

**是什么**：不是插件，是自己写的两个 Gradle 任务。它解决的问题很具体：**同一份源码在不同模块里解析出不同版本的同一个库**。

**本项目怎么接**（`build.gradle.kts:109` 与 `:235`）：

- `versionCoherenceCheck`（每模块）：断言"同一 group 的构件在本模块各配置内同版本"，并把已解析版本写成一份报告。
- `crossModuleVersionCheck`（根）：读各模块那份报告做跨模块比较，同一坐标出现多版本即失败。
- 拆成两步是 Gradle 的硬约束逼出来的：`org.gradle.parallel=true`（`gradle.properties`）下根任务不能去解析别人的配置（"attempted without an exclusive lock"），所以只能让每个模块写自己的报告。

监控的配置列表在 `build.gradle.kts:24`，其中 `testRuntimeClasspath` 是最有信号的一条——**库模块自测所用的一套依赖和真正部署的那套不是一套**时，CI 绿着也能藏住序列化行为差异。这正是本条门禁的由来：`common-core` 的 `testRuntimeClasspath` 曾解析到 Jackson `2.12.7.1`，而 `admin-service` 运行时是 `2.21.5`（注释在 `build.gradle.kts:231`）。

**接入前后**：根因是托管优先级——`spring-cloud-alibaba-dependencies` 直接声明的坐标只在**没挂 Boot 插件**的模块里生效。这条门禁把这个反直觉的行为变成了机器可查的断言；而它的触发源是 Dependabot：每周一它会抬"单条钉住组内一个构件"的约束，却不抬 BOM 管的兄弟构件，历史上把 SpotBugs 的配置拆成 `core 2.26.1` + `api 2.25.5` **两次**（`#15`、`#20`）。现在根脚本里不留这种约束，门禁守的是**将来再有人加**。

一个值得注意的实现细节：这个任务**故意不声明 `outputs`**（`build.gradle.kts:107`）。理由是依赖版本变了但任务被判定 `UP-TO-DATE` 会留下过期报告——那比"门禁空转"更糟，因为过期报告会让跨模块比对基于错误数据得出绿。

**产物**：`<module>/build/reports/dependency-versions.txt`，格式 `配置|坐标|版本`，每模块一份（`common-core` 那份实测 343 行）。这份报告同时是**排障资料**：`.github/workflows/ci.yml:146` 就引用它来确定 Flyway CLI 镜像版本必须和运行时 `flyway-core` 对齐（CI 里原先钉 `flyway/flyway:11` 比运行时低一个 major，12.x 语法的迁移脚本会"本机过、CI 红"或反向假绿）。

### 2.8 测试分层 —— `@Tag` 让门禁在干净机器上可执行

**是什么**：JUnit 5 的标签 + `useJUnitPlatform { includeTags/excludeTags }`。

**本项目怎么接**（`build.gradle.kts:12`）：任何依赖本机 MySQL/Nacos/Redis 的测试打 `@Tag("integration")`，默认从 `test`/`check` **排除**；容器就绪时用 `./gradlew test -PintegrationTests` 纳入。架构测试打 `@Tag("architecture")`，为的是让 `architectureTest` 能单独捞出来跑。

**接入前后**：这是全套门禁**能不能落地**的前提。如果 `check` 需要本机起中间件，那么任何人换台机器、任何 agent 沙箱里跑一次都是红的，门禁立刻退化成"某个人的机器上才是绿的"。两个 tag 各解决一层：`architecture` 让"架构检查"能进 pre-push（秒级），`integration` 让"需要真环境"的测试既能在 CI 上跑、又不绑架本地 `check`。CI 因此拆成两个 job：`check`（无中间件）和 `check -PintegrationTests`（起 MySQL/Redis/RabbitMQ 容器 + 手工拉 Nacos，`.github/workflows/ci.yml:54`）。

**产物**：与普通测试同一份报告，按 tag 看不出来——要区分"哪条测试属于集成层"，看源码上的 `@Tag`。

---

## 3. 前后对比：这套门禁整体换来了什么

把 §2 的散点收拢成一句可检验的话：**这套门禁拦住的不是"写得丑的代码"，而是"编译和测试都发现不了的失效模式"**。逐条对照：

| 维度       | 没有门禁时会怎样                                                      | 本项目现在的机制                                                     |
| ---------- | --------------------------------------------------------------------- | -------------------------------------------------------------------- |
| 发现时机   | 坏味道活到生产或活到启动期：一个 `starter-web` 上类路径要等真启动才炸 | 同一个失效模式在 `pre-push` 被 `GatewayDependencyIsolationTest` 拦住 |
| 谁能发现   | 只有读过设计文档、知道"网关不能有 Servlet"的那个人                    | 决策写进代码；换人、换 IDE、Dependabot 抬版本都照样挡                |
| 谁不会被挡 | ——                                                                    | 常规重构（改名、抽方法）不碰边界就不会红，所以闸不会被绕过           |
| 反馈成本   | code review 里靠人眼，评论还会漂移                                    | 秒级到分钟级的机器判定，规则只有一处事实源                           |
| 腐坏方向   | 规则散在文档/IDE 配置/口头约定里，随人员变动失效                      | 豁免必须写理由且窄（`exclude.xml`）；新增约束带 `文件:行号` 可回溯   |

同时要说清**代价与拦不住的**，否则这份对比就成了推销：

- **它不保证正确性**。上面所有闸跑完之后，产品行为可以依然是错的。这套东西管的是"结构不再悄悄变坏"。
- **它买来了耦合**。格式化、SpotBugs 引擎、ArchUnit、依赖版本策略现在都必须在根脚本对齐，抬一个插件版本可能同时牵动五个模块（Dependabot 的 PR 历史就是这部分成本的真实账单）。
- **它有假绿面**。§2.5 第 2 条那个 `noClasses()` 空导入即通过的坑是这类工具的共同性质：**断言不存在的时候，它也是"通过"的**。所以覆盖率不设阈值、豁免文件必须逐条写理由，都是承认这一点之后的补偿措施，而不是替代措施。
- **它把成本前移**。写一条 ArchUnit 规则、维护一份 `exclude.xml`、给测试打 tag，都是当场要花时间、收益却在未来某次事故里才显形的支出。

---

## 4. 输出产物一览：在哪、怎么看

**先记一句话**：有报告文件的是 SpotBugs / 测试 / JaCoCo / 版本报告；**没有**报告文件的是 Spotless / markdownlint / `-Werror` / 版本一致性判定（它们的失败信息只在标准输出）。

所有本地产物都在 `<module>/build/` 下，而 `build/` 整个被 gitignore——**产物是本地/CI 的，不进版本库**。

### 4.1 一条命令列出当前所有报告

```bash
find . -path "*/build/reports/*" \( -name "*.html" -o -name "*.xml" -o -name "*.txt" \) -not -path "*/jacoco/test/html/*" | sort
```

### 4.2 SpotBugs 报告（最值得点开看的一份）

```bash
xdg-open admin-service/build/reports/spotbugs/main.html   # Linux；WSL 用 wslview
```

`main.html` 是单文件，直接开就行。零 finding 时页面显示 "No bugs found"，但**这不代表门禁没跑**——同一份 `main.xml` 的 `<Project>` 里列着本次真正分析过的 class 清单：

```bash
for m in */; do echo -n "${m%/}: "; grep -c "<Jar>" "$m/build/reports/spotbugs/main.xml" 2>/dev/null || echo "-"; done
```

要看命中了什么（当前为零）：`grep -o 'type="[A-Z_0-9]*"' */build/reports/spotbugs/main.xml | sort -u`。

### 4.3 测试报告（普通测试 / 架构测试各一份）

```text
<module>/build/reports/tests/test/index.html              # ./gradlew test 或 check
<module>/build/reports/tests/architectureTest/index.html  # 只在单独跑 architectureTest 时生成
```

这里有个容易看漏的点：`architectureTest` **不是** `check` 的依赖（`build.gradle.kts` 里只把它挂进了 `prePushCheck` 的 `compileGate`）。但那些架构用例仍然在 `check` 里跑了——因为 `test` 只 `excludeTags("integration")`，`@Tag("architecture")` 的用例本来就是它的普通成员。所以：

- 找 `check` 之后的架构测试结果 → 看 `tests/test/index.html`（`tests/architectureTest/` 那时可能不存在）。
- 想只要架构那份视图 → `./gradlew architectureTest`，然后看 `tests/architectureTest/index.html`。
- JUnit 原始 XML 在 `<module>/build/test-results/{test,architectureTest}/`，适合 `grep '<failure'` 定位。

### 4.4 JaCoCo 覆盖率

```bash
xdg-open admin-service/build/reports/jacoco/test/html/index.html   # 逐行标色，要看细节必须走 HTML
```

HTML 是多文件站点，`file://` 直接开通常没问题；若样式异常就起个本地服务：

```bash
python3 -m http.server -d admin-service/build/reports/jacoco/test/html 8000   # http://localhost:8000
```

只想要数字（脚本 / CI 日志友好）：

```bash
grep -o '<counter type="\(LINE\|BRANCH\)"[^/]*>' admin-service/build/reports/jacoco/test/jacocoTestReport.xml | tail -2
```

记住 §2.6：**这个数字没有门禁**。没有阈值会替你判断"够不够"。

### 4.5 已解析依赖版本报告

```bash
column -s'|' -t ecs-service/build/reports/dependency-versions.txt | grep jackson-databind
```

跨模块找差异（`crossModuleVersionCheck` 做的同样的事，只是给人看）：

```bash
cat */build/reports/dependency-versions.txt | awk -F'|' '{print $2, $3}' | sort -u | awk '{c[$1]++; v[$1]=v[$1]" "$2} END {for (k in c) if (c[k]>1) print k, v[k]}'
```

### 4.6 Gradle 问题报告（只在有问题时出现）

Gradle 9 会把构建期的问题（编译告警、依赖解析、插件弃用）归因成一份 HTML。**它不是一次成功 `check` 的固定产物**——干净通过的构建之后 `build/reports/problems/` 可以不存在，`clean` 也会抹掉它。所以把它当成"红灯时去找的东西"，而不是"每次都要去翻的东西"：

```bash
find . -name "problems-report.html" -not -path "./.git/*"   # 有就有，没有就说明这次没记录到问题
```

### 4.7 CI 上的产物

本地 `build/` 会被下一次 `clean` 抹掉，CI 那份才是留档。两个 job 都无条件上传（`if: always()`，保留 7 天）：

| Artifact 名           | 来自 job      | 内容                                            |
| --------------------- | ------------- | ----------------------------------------------- |
| `build-reports`       | `check`       | `**/build/reports/{spotbugs,tests,jacoco}/`     |
| `integration-reports` | `integration` | 同上，但那次跑包含 `@Tag("integration")` 的测试 |

网页：PR → Checks → CI 的 `check` job → 右下角 **Artifacts**。命令行（若装了 `gh`）：

```bash
gh run list --workflow=ci.yml --limit 10
gh run download <run-id> --name build-reports          # 解出 reports/ 目录树
```

CI **不**上传版本一致性报告和 `problems-report.html`（`.github/workflows/ci.yml:46` 的 path 列表里没有）。要在 CI 上确认这两项，只能读 job 日志，或者本地跑 §4.5 / §4.6。

### 4.8 一次全绿的干净环境长什么样

```bash
./gradlew clean check            # 全量
./gradlew prePushCheck           # 推送闸（不跑测试）
./gradlew test -PintegrationTests  # 需要本机 MySQL/Nacos/Redis/RabbitMQ
```

---

## 5. 红灯定位：先判断是哪道闸

| 症状                                        | 大概率原因                                                               | 下一步                                                           |
| ------------------------------------------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| `git commit` 就被拒，几秒                   | 格式（pre-commit）                                                       | `./gradlew spotlessApply`                                        |
| 提交信息被拒但代码没动                      | Conventional Commits                                                     | 看 `gradle/git-hooks/commit-msg.sh` 允许的 type 列表             |
| `push` 被拒，没有测试相关字样               | `prePushCheck` 六项之一：SpotBugs / 架构 / `-Werror` / 版本一致性 / 格式 | 看终端首行任务名；SpotBugs 去开 `main.html`，架构去开测试报告    |
| 编译期报 `use of deprecated... is an error` | `-Werror` + `deprecation`                                                | 换 jspecify / 换非废弃 API，别去关 lint                          |
| 本地一切正常，CI 的 `check` 红              | 缓存或脏 `build/`；或某个 `@Tag("integration")` 忘了打 tag               | `./gradlew --no-build-cache clean check` 复现                    |
| CI 的 `integration` 红、`check` 绿          | 真中间件相关（Flyway 脚本、RabbitMQ 契约、Nacos 注册）                   | 看 `integration-reports` 产物                                    |
| 架构测试"通过"但你刚改过包结构              | 可能踩了 §2.5 的空导入静默失效                                           | 看 `importedClassesAreNeverEmpty()` 在不在该模块，跑一次确认类数 |

---

## 6. 实测数据

本文写作时在本机（WSL2，Gradle 9.7.1 热守护进程、构建缓存开启、工作树干净）实测：

| 命令                                                | 耗时  | 说明                                                    |
| --------------------------------------------------- | ----- | ------------------------------------------------------- |
| `./gradlew architectureTest`（5 个模块，50 个用例） | 12.9s | 印证"秒级、可进 pre-push"                               |
| `./gradlew spotlessCheck`                           | 2.2s  | pre-commit 的实际开销                                   |
| `./gradlew lintMarkdown`                            | 1.9s  | 含 `npx` 冷启动                                         |
| `./gradlew clean check`（缓存命中）                 | 7.8s  | 全部输出可从构建缓存重放                                |
| `./gradlew clean check --no-build-cache`            | 75.9s | 全量门禁的真实成本：5 模块编译 + SpotBugs + 架构 + 测试 |

那对 8 秒 / 76 秒的差值本身就是结论：**日常反馈几乎免费，全量成本只在缓存失效时付**（`org.gradle.caching=true`，`gradle.properties`）。CI 上通过 `gradle/actions/setup-gradle` 复用同一套缓存，`timeout-minutes: 20` 是给冷启动留的余量。

## 7. 后续文档计划

本文件夹按主题展开，每篇一个对象，不与 `AGENTS.md` 抢事实源。候选：

- 依赖与版本治理：BOM 优先级、`spring-cloud-alibaba` 与 Boot 插件的交互、Dependabot 的边界。
- 反应式网关与 Servlet 业务服务共存：`common-security` 拆分史、`WebApplicationType.deduce` 那条启动期开关。
- 可观测性：traceId 如何从网关穿到日志与 actuator（P3-1）。
