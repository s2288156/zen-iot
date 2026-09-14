# 计划 5（P4）：CI Workflow 剩余缺陷修复

## 背景与现状

计划 1/2/3 已合入 main（或正在走 PR），核心门禁 `./gradlew check` + `./gradlew check -PintegrationTests` 全部 PASS。但 `.github/workflows/ci.yml` 中两个安全扫描 job 在 **PR #8（chore/plan3）** 上反复失败，根因已定位，修复 commit 也已推到分支上，但 CI run 对新 commit 的触发滞后，需要在 main 上单独验证确认。

## 两个待修复 job

### Job 1：gitleaks（密钥扫描）

**当前状态**：run 34799258974 报错 `FTL unable to load gitleaks config, err: While parsing config: toml: incomplete number`

**已定位根因**（两次迭代）：

1. **第一次迭代（commit 37af251）**：修复了 `gitleaks-action@v2` 不接受 `with.args` 输入、只能用 env 变量的问题。改用 `env.GITLEAKS_CONFIG: ".gitleaks.toml"`。CI 报错变了，但还是 fail。
2. **第二次迭代（commit a1c9aef）**：根因在 `.gitleaks.toml` 文件本身。原文件用 TOML literal string `'''regex'''` 写 BCrypt 密文（形如 `\$2a\$10\$Kw/dU22ig8...`），TOML literal string 不做转义但 **$ 后跟数字**（如 `$2a`）会被 TOML 解析器误判为 "incomplete number"（TOML 标准中`$` 用于 prefix 特殊 number 类型如 `0x...` / `0o...` / `0b...`）。
   - 修复：改用 TOML **basic string `"""..."""`** + 正确转义：TOML `\\$` → `\$` → 传给 gitleaks regex 引擎为 `$`（字面值）
   - 当前 `.gitleaks.toml` 内容见 `a1c9aef`

**当前 commit 状态**：`a1c9aef` 已推到 PR #8 分支，但 run 34799258974 用的是旧 commit 触发的（GitHub Actions 对连续 push 有时触发滞后）。**合并到 main 后需要单独观察 CI 是否 PASS**。

**风险**：gitleaks TOML 转义逻辑需要再验证一次 basic string 的转义规则正确——TOML basic string 里 `\\` → 单个 `\`，所以 `\\$` 传入 gitleaks 是 `\$`（正则转义字面量 $）。BCrypt 密文正则里还有 `\.`，需要确认转义后仍然匹配 `.`（非捕获）还是字面 `.`（捕获）。

### Job 2：OSV scanner（依赖漏洞扫描）

**当前状态**：run 34799258974 报错 `Unable to resolve action google/osv-scanner-action@v2, unable to find version v2`

**已定位根因**：action 路径/版本号多次写错：

1. 原计划 2 写的是 `google/osv-scanner-action/osv-scanner-action@v1.8.1` —— 嵌套路径 + 版本号都是错的。v1.8.x 路径结构不同。
2. commit `a1c9aef` 改到 `google/osv-scanner-action@v2.5.0` —— 这个仓库的根路径是 reusable workflow（`.github/workflows/osv-scanner-reusable.yml`），不是 action entrypoint。v2.5.0 tag 下根目录没有 `action.yml`，所以 action runner 找不到。
3. commit `32febfe`（当前 HEAD）改到 `google/osv-scanner-action/osv-scanner-action@v2` —— 子路径 `/osv-scanner-action` 是正确的 action 入口，但 tag 仍然是 `v2`（不是 `v2.5.0` 这种具体版本号）。需要查最新实际 tag。

**当前 commit 状态**：`32febfe` 已推。run 34799258974 还没等它（新 run 触发可能滞后）。

**需要进一步验证**：

- 查 `google/osv-scanner-action` 仓库的最新 release tag（可能是 `v2.5.0`，但 `/osv-scanner-action@v2.5.0` 是否存在？）
- 查 action.yml 的 `inputs` schema（`scan-args` 是否存在？之前旧版本用的是 `args`）
- SARIF 输出格式路径（`--output osv-results.sarif` 是否在 action upload-artifact 范围内）

## 修复步骤（按此顺序）

### Step 1：合并 PR #8，观察 gitleaks

PR #8 的核心 `check` + `integration` 已经两次 PASS，gitleaks/OSV scan 是独立的 workflow bug，不阻塞代码质量。合并后 main 上 CI 会重新跑。

### Step 2：gitleaks 验证与可能的 TOML 微调

合并后观察 main 分支 CI run：

- 如果 gitleaks PASS → 完成，无需进一步动作
- 如果继续 `incomplete number` → TOML basic string 转义仍不对，需要手动测最小 TOML 文件：

```bash
# 本地跑 gitleaks 验证 TOML
gitleaks detect --config .gitleaks.toml --path admin-service/src/main/resources/application.yml -v
# 或在线 sandbox：https://toml.io/en/v1/tool?version=v1.0.0
# 确认 basic string 里 \\$ 被解析为 \$（单字符）
```

可能的 TOML 正确写法（逐一试）：

```toml
# 方案 A：basic string，每个 $ 转义
regex = "(\\$2a\\$10\\$...|...)"
# 方案 B：basic string + \u0024 转义
regex = "(\u00242a\u002410\u0024...|...)"
# 方案 C：改用 allowlist.path（整个文件放行，最低效但不会错）
[[allowlists]]
path = "admin-service/src/main/resources/application.yml"
description = "dev placeholder secrets in Flyway placeholders"
```

### Step 3：OSV scanner 修复

**已知事实**（来自 WebSearch + run 失败日志）：

1. `google/osv-scanner-action@v2.5.0`（根路径）是 **reusable workflow**，不是 action entrypoint
2. `/osv-scanner-action` 子路径是 **action entrypoint**，但 tag 写法需确认
3. v1.8.x 的输入字段（`scan-mode` / `results-file-path` / `exit-code`）在新版已全部变化

**需要查的**：

```bash
# 查可用 tag
gh api repos/google/osv-scanner-action/tags --jq '.[].name'
# 查 v2.x 最新 tag 下 /osv-scanner-action/action.yml 里的 inputs
curl -sL https://raw.githubusercontent.com/google/osv-scanner-action/v2.5.0/osv-scanner-action/action.yml | head -30
```

**预期正确写法**（候选）：

```yaml
# 候选 1
- uses: google/osv-scanner-action/osv-scanner-action@v2.5.0
  with:
    scan-args: --format sarif --output osv-results.sarif --recursive .
# 候选 2：如果 v2 还不稳定，回退到直接用 osv-scanner CLI（通过 actions/setup-osv-scanner）
- uses: google/osv-scanner-action/setup-osv-scanner@v1.2.0
- run: osv-scanner scan --format sarif --output osv-results.sarif --recursive .
- uses: actions/upload-artifact@v4
  with:
    name: osv-scan-results
    path: osv-results.sarif
```

### Step 4：两者合并验证

两个 job 都修复后，合并到 main（可以单独 commit 或新 PR），确认 main 分支的 **4 个 CI job 全绿**。

## 验收标准

- [ ] gitleaks PASS：没有 TOML 解析错误；扫描结果不把 dev BCrypt 密文 / 默认 JWT secret 报为泄露
- [ ] OSV scanner PASS：正确解析 scan-args；能识别 Java Gradle 项目的依赖清单（pom.xml 风格）；输出 SARIF 或 artifact 能上传
- [ ] 4 个 CI job（check / integration / gitleaks / osv-scan）在 main 分支上全部 PASS

## 影响范围

- **文件**：`.github/workflows/ci.yml`、`.gitleaks.toml`
- **不影响**：业务代码、Gradle 构建配置、其他 CI job
- **风险**：两个 fix 都是 workflow YAML + TOML 纯配置变更，没有代码逻辑风险
