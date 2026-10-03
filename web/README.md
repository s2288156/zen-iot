# web

`zen-iot` 管理控制台前端，独立于 Gradle 的 pnpm 工程：Vite 8 + React 19 + TypeScript strict + Ant Design 6，经网关以 `/api/admin/**` 访问 admin-service。

## 能力与接口口径

- 业务接口一律带 `/api/admin` 前缀（网关 `StripPrefix=2`），dev 由 Vite proxy 转发到 `localhost:28080`。
- 响应信封为 `{ code, message, data }`，`code` 是 int 且成功码为 `200`；`code != 200` 一律抛 `ApiError{code, message}`。
- 可匿名调用的业务接口只有 `/auth/login` 与 `/auth/refresh`，其余全部要求 Bearer。
- `GET /auth/me` 返回 `UserProfileView`，含 `roleIds` 但**不含** modules；模块权限由逐个 `GET /roles/{id}` 的 `modules` 求并集得出。
- 分页响应为 `PageResult{list,total,pageNum,pageSize,pages}`；请求参数为 `PageQuery{pageNum,pageSize,orderBy,orderDirection}`，`pageSize` 默认 10、上限 200，`orderDirection` 取 `asc|desc` 且默认 `asc`。
- 列表接口口径不一致：`/users/page` 与 `/roles/page` 支持过滤条件和排序白名单，`/sessions` 路径不含 `/page` 且不接受任何过滤参数。

## 关键实现

### HTTP 客户端（`src/api/http.ts`）

- `ApiResponse` 解包集中在 `unwrap`，非 JSON 信封的裸 5xx 按 HTTP 状态码归一化为 `ApiError`。
- 超时用 `AbortSignal.timeout`，与断网分别归一化为 `NetworkError{ timedOut: true | false }`，默认 10s。
- `authAction` 标记认证动作类请求：`/auth/login` 的凭证错误同样是 HTTP 401，不排除就会被误判为会话过期而丢失错误文案。

### Token 生命周期（`src/api/tokenStore.ts`）

- 后端 `TokenPair` 不返回 `expiresIn`，因此不做提前续期，只在收到 401 时轮转。
- 轮转单飞：并发 401 共享同一个在途 `/auth/refresh`，成功后各自重放原请求一次；重放仍 401 不再二次轮转。
- 旧 refreshToken 一经使用立即进黑名单，故轮转成功 MUST 整体替换两个 Token；旧 accessToken 不被轮转吊销，在剩余 TTL（30m）内仍可用，因此多标签页各自续期互不影响。
- 轮转失败即清 Token 并通知会话过期；跳转 `/login` 由 `RequireAuth` 响应式完成，api 层不做命令式导航。

### Token 持久化与安全权衡

- accessToken 只驻内存（模块变量），不落任何持久化存储：刷新页面即丢，由 refreshToken 静默轮转重建。
- refreshToken 落 localStorage，键名 `zen.admin.refresh-token`，以支撑刷新与多标签页保持会话。
- 代价是 refreshToken 暴露在 XSS 之下；后端不发 httpOnly Cookie 且本任务不改 Java，localStorage 是现有契约下唯一可行的持久化位置。
- 因此 refreshToken 只用于 `/auth/refresh`，任何业务请求都不携带它；泄露的可利用窗口等于 `refresh-ttl`，撤销依赖 `/auth/logout` 拉黑。
- 部署侧 MUST 配 CSP 收敛注入面；本模块不引入任何 `dangerouslySetInnerHTML`。

### 会话与权限（`src/stores/auth.ts`、`src/routes/guards.tsx`）

- auth store 状态机为 `idle → bootstrapping → authenticated | anonymous`；`bootstrap` 先同步占位，保证 StrictMode 双跑只装配一次。
- `logout` 无论服务端撤销是否成功都清本地 Token 并回落 `anonymous`，撤销失败作为异常抛给调用方。
- `RequireAuth` 未登录时重定向 `/login`，并把 `pathname + search` 存进 `location.state.from` 供登录后回跳。
- `RequireModule` 无权限时渲染 403 状态页而非重定向；`DefaultHome` 让不含 admin 的账号落到 `/profile`，避免首屏即 403。

### 视觉基调口径（`src/theme/`）

- 色系：科技蓝。完整色阶集中在 `palette`（blue1 最浅 → blue10 最深），`colorPrimary` 取 blue6 `#1677ff`；中性灰阶在 `neutrals`；success/warning/danger 沿用 AntD 语义默认值，不覆写。
- 布局：左侧可收起 Sider 展开 224 / 折叠 64（`layout.siderWidth` / `siderCollapsedWidth`），顶栏白底、内容区灰底，圆角 8、字号 14、控件高 32。菜单只定制 `item*` 系（`itemSelectedBg` = blue1、`itemSelectedColor` = blue7、`activeBarBorderWidth: 0`），Sider 与 Menu 统一 light。
- 暗色预留：本轮不做切换控件、不做双态校验。`theme/index.tsx` 注明接入点——把 `algorithm: theme.darkAlgorithm` 并入 `antdTheme` 即可，`token` 仍作浅色 seed、派生交给 algorithm。
- 自检路由：`/theme-self-check` 用 Button/Table/Form 合成页验证 token 贯通，仅经 `import.meta.env.DEV` 挂载，不进生产路由。

### 布局与导航（`src/components/AppLayout.tsx`、`src/routes/navigation.tsx`）

- Sider 与 Menu 统一 light 基调，折叠宽度取 `theme/tokens.ts` 的 `layout.siderCollapsedWidth`；折叠状态由顶栏按钮控制，Sider 自带 trigger 关掉。
- 菜单项集中在 `NAV_ITEMS` 声明并按 `auth.modules` 过滤，`module: null` 的项不受权限约束；`handle.fullScreen` 是路由级隐藏侧栏开关，当前无页面挂载它。

### 列表页数据约定（TanStack Query）

- 列表统一走 `PageTable`（`src/components/PageTable.tsx`）：服务端分页 + 查询表单 + loading/error/empty 三态，queryKey 由封装内部生成为 `[domain, 'page', params]`，页面不手写 key。
- 翻页与排序由 `Table.onChange` 的 `extra.action` 分支归一到 `PageQuery`；跨页保留数据用 `placeholderData: keepPreviousData`，据此以 `isPlaceholderData` 区分新旧数据。
- 写操作后按 domain 前缀失效（`api/queryKeys.ts` 的 `invalidateDomain`），不精确到 `page` 段。

### 错误处理两层（`src/routes/errorElement.tsx`、`src/components/ErrorBoundary.tsx`）

- 路由层 `errorElement` 为主，抓 lazy 装载失败与 loader/action rejection；渲染期异常由 `ErrorBoundary` 兜底，两层渲染同一套 `StatusPage`。
- 404 由 catch-all `path: '*'` 指向 `NotFound`；提示层经 AntD `App` 上下文取用（`main.tsx` 顺序为 `ThemeProvider > QueryClientProvider > App`），`logout` 服务端撤销失败以 `message.warning` 明示「本地已登出、服务端未撤销」后照常回 `/login`。

## 架构约束

- 色值唯一事实源是 `src/theme/tokens.ts`，业务代码不得硬编码色值。
- 路由模块必须具名导出 `Component`；dev-only 路由用 `import.meta.env.DEV` 条件挂载。
- api 层不得依赖 store 与 router，跨层通知通过 `setSessionExpiredHandler` 反向注册。
- 公共组件留在 `src/components`，不拆独立组件库包；列表页不得绕过 `PageTable` 直连 `useQuery`。

## 主要依赖

`antd@6.6.5`、`@ant-design/icons@6.3.4`、`react-router@8`、`@tanstack/react-query@5`、`zustand@5`；测试为 `vitest` + `@testing-library/react` + jsdom。

## 命令

| 任务     | 命令                                            |
| -------- | ----------------------------------------------- |
| 开发     | `pnpm dev`                                      |
| 类型检查 | `pnpm typecheck`                                |
| Lint     | `pnpm lint`                                     |
| 单元测试 | `pnpm test`                                     |
| 构建     | `pnpm build`                                    |
| 联调前置 | 网关与 admin-service 已启动在 `localhost:28080` |
