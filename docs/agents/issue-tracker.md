# Issue tracker：GitHub

本仓库的 issue 与 spec 以 GitHub issue 的形式存在，所有读写操作走 `gh` CLI。

## 约定

- **创建 issue**：`gh issue create --title "..." --body "..."`，多行正文用 heredoc。
- **读 issue**：`gh issue view <number> --json number,title,body,labels,comments`。
- **列 issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，按需加
  `--label` 与 `--state` 过滤。
- **把 issue 设为子 issue**：`gh issue create --parent <parent> ...`，或事后
  `gh issue edit <parent> --add-sub-issue <child>`（`gh` 2.94+）。更早的 `gh` 用
  `gh api --method POST repos/<owner>/<repo>/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>`，其中是
  database id（见下方 **Blocking**）。没有 sub-issue 时，把 `Part of #<parent>` 写在子 issue 正文首行。
- **评论**：`gh issue comment <number> --body "..."`
- **加减标签**：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **关闭**：`gh issue close <number> --comment "..."`

仓库归属由 `git remote -v` 推断；在克隆内运行时 `gh` 自己会做这件事。

## 把 pull request 当作需求入口

**PRs as a request surface: no.** _(本仓库若把外部 PR 当需求入口，把这句改成 `yes`；`/triage` 读这个开关。)_

## 当技能说「发布到 issue tracker」

创建一个 GitHub issue。

## 当技能说「取回相关 ticket」

按上文 **读 issue** 的方式读取。

## Wayfinding 操作

供 `/wayfinder` 使用：**map** 是一个 issue，**child** 是挂在它下面的决策 ticket。

- **Map**：单个带 `wayfinder:map` 标签的 issue，正文装 Notes / Decisions-so-far / Fog。命令：
  `gh issue create --label wayfinder:map`。
- **Child ticket**：以 GitHub sub-issue 挂在 map 下（见上文 **把 issue 设为子 issue**）。sub-issue 未启用时，把子项
  追加进 map 正文的任务清单，并在子 issue 正文首行写 `Part of #<map>`。标签取 `wayfinder:<type>`，即
  `wayfinder:research` / `wayfinder:prototype` / `wayfinder:grilling` / `wayfinder:task`。被认领后 assignee
  是主导的 dev。
- **Blocking**：GitHub 原生 issue dependencies，是唯一在 UI 上可见的表示法。加一条边用
  `gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`，
  其中 `<blocker-db-id>` 是阻塞方的数字 database id（`gh api repos/<owner>/<repo>/issues/<n> --jq .id`，_不是_
  `#number`，也 _不是_ `node_id`）。GitHub 用 `issue_dependencies_summary.blocked_by` 报告结果，只含未关闭的阻塞方，
  这就是真正的门禁。dependencies 不可用时，退回子 issue 正文首行的 `Blocked by: #<n>, #<n>`。所有阻塞方都关闭，该
  ticket 才算解除阻塞。
- **Frontier 查询**：列出 map 的未关闭子项（`gh issue list --state open`，范围限定在 map 的 sub-issue 或任务清单），
  剔除仍有开放阻塞方（`issue_dependencies_summary.blocked_by > 0`，或 `Blocked by` 行里有未关闭 issue）或已有
  assignee 的项，按 map 顺序取第一条。
- **Claim**：`gh issue edit <n> --add-assignee @me`，一个 session 的第一次写操作。
- **Resolve**：`gh issue comment <n> --body "<answer>"`，然后 `gh issue close <n>`，最后把 context pointer（要点加
  链接）追加到 map 的 Decisions-so-far。
