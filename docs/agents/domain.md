# 领域文档

工程技能在探索代码库时如何消费本仓库的领域文档。

本仓库是 **single-context**：根上一个 `GLOSSARY.md`，加 `docs/adr/`。判据是没有 monorepo 信号——无
`pnpm-workspace.yaml`、无 `packages/*`。`web/` 分开的是工具链与门禁（见 `docs/modules/web.md`），不是领域词汇，
因此不构成独立上下文。

## 探索前先读

- 根目录的 **`GLOSSARY.md`**，或
- 根目录的 **`GLOSSARY-MAP.md`**（若存在）：它按上下文指向各自的 `GLOSSARY.md`，只读与主题相关的那些。
- **`docs/adr/`**：读与接下来要动的区域有关的 ADR。multi-context 仓库还要看 `src/<context>/docs/adr/` 里按上下文
  范围记录的决定。

任一文件不存在就直接往下做，不必声张缺失，也不要主动提议创建。`/domain-modeling`（经 `/grill-with-docs` 与
`/improve-codebase-architecture` 进入）会在术语或决策真正定下来时按需创建它们。

## 文件结构

single-context（本仓库，也是绝大多数仓库）：

```text
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

multi-context（判据是根目录存在 `GLOSSARY-MAP.md`）：

```text
/
├── GLOSSARY-MAP.md
├── docs/adr/                          ← 系统级决策
└── src/
    ├── ordering/
    │   ├── GLOSSARY.md
    │   └── docs/adr/                  ← 上下文内的决策
    └── billing/
        ├── GLOSSARY.md
        └── docs/adr/
```

## 用 glossary 里的词汇

输出里给某个领域概念命名时（issue 标题、重构提案、假设、测试名），用 `GLOSSARY.md` 里定义的术语，不要漂移到 glossary
明确避开的同义词。

**术语的定义只在 `GLOSSARY.md` 一处**：`docs/architecture.md` 与各 `<module>/README.md` 的「能力与接口口径」只引用词、
不定义词，所以它们不是第二份词表，不要从那里取定义。代价是定义离开了赋予它意义的契约，因此 glossary 条目要带指针
（例如 `会话` 指向 `docs/architecture.md` 的令牌生命周期与撤销链），机制本身留在契约文档里。

若需要的概念还不在 glossary 里，那是一个信号：要么你在发明项目不用的说法（应当重新考虑），要么确实存在缺口（记下来交给
`/domain-modeling`）。

## ADR 冲突要明说

输出与既有 ADR 矛盾时，显式指出来，而不是静默覆盖：

> _与 ADR-0007（event-sourced orders）矛盾，但值得重开，因为……_
