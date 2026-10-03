# web 决策与坑

[`web/README.md`](../../web/README.md) 只放事实与结论，本文放**为什么这样选、否掉了什么、代价在哪**。跨模块必须同时成立的约定在 [`docs/architecture.md`](../architecture.md)，这里不重复。

## 决策

### 独立 pnpm 工程，不进 Gradle

- 背景：仓库其余部分是 Gradle 多模块 Java，门禁由 JDK 25 toolchain、Spotless、ArchUnit、SpotBugs、`prePushCheck` 组成；前端工具链是 Node + pnpm，失败模式与缓存都不同。
- 选项：把 `web/` 注册为 Gradle 模块（靠 node 插件拉前端任务）/ 保持独立 pnpm 工程、CI 另开 workflow。
- 结论：后者。`settings.gradle.kts` 不含 `web`，前端门禁落在 `.github/workflows/web-ci.yml`，与 `ci.yml` 靠 `paths` 过滤器互不触发。
- 理由：把 Node 工具链塞进 Gradle 会让 `./gradlew check` 的语义变成「还依赖 Node 与网络」；独立后只改 Java 的 PR 完全不跑前端 job。
- 边界：`./gradlew devUp` 会拉起 Vite 进程，但它只在本机跑、不被任何门禁依赖——这条决策约束的是工具链归属与 `check` 语义，不是「Gradle 不许碰前端进程」。
- 代价：门禁入口有两个，`./gradlew check` 不再覆盖前端——改 `web/**` 必须另跑四件套（见 `web/README.md` 命令表）。

### AntD 6 + TanStack Query 管服务端状态 + Zustand 管会话

- 背景：管理台是表格/表单密集型，既要组件库，也要服务端分页缓存与全局会话。
- 选项：AntD + Query + Zustand / Redux Toolkit 一把梭 / 只用 Zustand 全包。
- 结论：AntD 6 作组件库；服务端状态（列表分页、缓存、失效）归 TanStack Query；只有会话与权限进 Zustand 的 `auth` store。
- 理由：服务端状态交给 Query 后，loading/error/empty/`keepPreviousData`/失效都是现成的，页面只声明 `PageTable`；会话是纯客户端状态，进 Query 反而别扭。
- 代价：两套状态心智并存，边界靠约定与评审——列表一律走 `PageTable`，不绕过 `useQuery`；列表数据不进 store。
- 否决：Redux Toolkit（样板重，服务端状态仍要自建缓存层）；把列表塞进 Zustand（等于重写分页与失效）。

### Design Tokens 定制，不用 AntD 默认皮

- 结论：色值集中在 `src/theme/tokens.ts`（`palette` / `neutrals` / `layout`），经 `ConfigProvider` 注入；业务代码零硬编码色值。
- 理由：改主色只改一处并全站即时生效；违规也可评审——grep 十六进制色值即可发现。
- 代价：`tokens.ts` 是唯一事实源，任何「就地写个色值更省事」都是退化；antd CLI 不检查色值，只能靠评审兜住。

### 错误处理分两层：路由 `errorElement` 为主，`ErrorBoundary` 兜渲染期

- 结论：lazy 装载失败与 loader/action rejection 由路由 `errorElement` 抓，渲染期异常由 `ErrorBoundary` 兜底，两层渲染同一套 `StatusPage`。
- 理由：React 的渲染错误不会冒泡到 router 的 `errorElement`，只做一层必有盲区。
- 代价：两个入口要维持同一套视觉，收敛到同一 `StatusPage` 正是为此；新增状态页时两层同时受益。

### `antd lint` 从「只报告」改为 CI 阻断

- 背景：spec 把「组件属性一律写 v6 口径、不用 v5 废弃别名」定为硬约束，此前仅靠评审把关。
- 选项：只报告（输出 GitHub `::warning` annotation、不进 required check）/ 只加 ESLint 规则 / 外层包一层转阻断。
- 结论：外层包一层（`web/scripts/antd-lint-gate.mjs`）直接转阻断，未落中间态——Task 5 收口时基线即为 0 issue，无历史问题要背，中间态纯属空转。
- 理由：CLI 退出码恒 0，本身不构成门禁，阻断只能由外层按摘要计数补；同一层顺带校验 CLI 与 antd 同版，否则检查器与被检对象错位。
- 代价：门禁语义是「不许新增废弃别名」，但 CLI 对 `Tabs.TabPane` 与 `size="default"` 漏检，这两条仍靠评审，不因转阻断而视为已覆盖。

## 坑

- **`@ant-design/cli` 的退出码恒为 0。** 现象：注入违规后仍 `exit 0`，只有 stdout 多出 `Summary: N deprecated, …` 行；任何直接把它当 CI 步骤的写法都是假绿。做法：外层 `antd-lint-gate.mjs` 解析该摘要、按四类计数求和判非零，并对「既无 `No issues found` 也无 `Summary`」按失败处理。**删除条件**：CLI 自身改为非零退出码后。

- **Menu 的 `dark*` 与 `item*` 是两套独立命名空间。** 现象：Task 2 只定制了 `item*` 系（`itemSelectedBg` / `itemSelectedColor`），深色菜单下选中态仍吃 dark 默认值，定制看起来没生效。做法：Task 4 把 Sider/Menu 统一转 light 后成立；改回深色菜单时须同时补 `dark*` 系。

- **antd v6 已改名的属性写回 v5 口径不报错。** 现象：`Space.direction`（应为 `orientation`）、`Alert.message`（应为 `title`）在编译期与运行期都不报错，只是语义错位。做法：以 `antd info <Component>` 与 `antd lint` 为准，不凭记忆写属性名。

- **jsdom 缺 `matchMedia` / `ResizeObserver`。** 现象：组件测试直接抛错——AntD 的响应式与 rc-trigger 弹层都依赖它们。做法：在 `src/test-setup.ts` 统一补 stub。

- **登录失败与会话过期同为 HTTP 401。** 现象：`/auth/login` 的凭证错误也是 401，若不排除会被 HTTP 层误判成会话过期，触发轮转并丢掉错误文案。做法：用 `authAction` 标记认证动作类请求，跳过轮转。

- **`/sessions` 的响应是 `data.list`，且忽略排序参数。** 现象：与 `/users/page`、`/roles/page` 的 `PageResult` 同形但字段名不同（不是 `data.records`）；列表恒按 Redis ZSET 的过期时刻升序，`orderBy` / `orderDirection` 传了也不生效。做法：会话页不提供排序交互，避免给出无效控件。

- **`sessionId` 与 JWT `jti` 无对应关系。** 现象：`SessionService#recordLogin` 生成的 `sessionId` 是独立 UUID，后端也刻意不下发 `jti`，前端无法从会话列表认出「哪条是自己当前会话」；而 `SessionService#kickout` 的自锁按 `userId` 判，下线自己名下**任何**一条都回 400。做法：会话页整行按 `userId` 禁用操作并说明原因。

- **`PUT /users/{id}/roles`、`PUT /roles/{id}/modules` 的请求体是裸 JSON 数组。** 现象：包一层对象会被 Jackson 拒成 400。做法：前端按裸数组发送，不按常见「`{ ids: [...] }`」写法。

- **`pnpm-lock.yaml` 会被根 Spotless 的 yaml 步骤重排。** 现象：包管理器产物被 Prettier 重排后，每次 install 都产生无意义 churn。做法：已把它排除出根 `build.gradle.kts` 的 `format("yaml")`。

- **行内操作用 `Typography.Link` 渲染成裸 `<a>`。** 现象：无 `href`/`role`/`tabindex`，键盘不可聚焦，是真实 a11y 缺口。做法：属 antd 惯用写法且当前无键盘可达性要求，本次未改；需要时统一换成可聚焦的 `Button type="link"`。

- **会话过期只有状态回落，没有全局提示。** 现象：`stores/auth.ts` 的 `setSessionExpiredHandler` 仅 `setState(anonymous)`，随后 `RequireAuth` 静默跳 `/login`，用户看不出是被强制下线。做法：已知缺口，需另开改动补全局提示。
