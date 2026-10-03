#!/usr/bin/env node
/**
 * antd v6 用法门禁（Task 6.4）。
 *
 * `@ant-design/cli` 的退出码**恒为 0**：无论检出多少问题都以 0 退出，所以阻断只能由这一层补上——
 * 跑 `antd lint src`，按输出末尾 `Summary: N deprecated, N a11y, N usage, N performance, N skipped`
 * 的四类计数求和，非零即 exit 1。`skipped` 是 CLI 自己跳过的检查，不计入。
 *
 * 顺手校验 CLI 与被检对象同版。检查规则是冲着某个 antd 版本写的，两者分开升级会让
 * 「新规则跑在旧 antd 上」或「旧规则跑在新 antd 上」，检查结论就失去意义；
 * dependabot 那边也把 `antd` / `@ant-design/icons` / `@ant-design/cli` 放在同一组升级。
 *
 * 已知漏检（CLI 版本 6.6.5 实测）：`Tabs.TabPane` 与 `size="default"` 两个废弃写法检不出来，
 * 它们始终靠评审兜底，不因本门禁转阻断而视为已覆盖。
 *
 * 用法：在 `web/` 下执行 `node scripts/antd-lint-gate.mjs [target]`（target 默认 `src`）。
 */
import { spawnSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const webRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const target = process.argv[2] ?? 'src'

const readVersion = (name) => JSON.parse(readFileSync(join(webRoot, 'node_modules', name, 'package.json'), 'utf8')).version

const antdVersion = readVersion('antd')
const cliVersion = readVersion('@ant-design/cli')
if (antdVersion !== cliVersion) {
  console.error(`antd 与 @ant-design/cli 版本不一致：antd@${antdVersion} vs @ant-design/cli@${cliVersion}`)
  console.error('两者必须同版升级（见 .github/dependabot.yml 的 antd 分组），否则检查器与被检对象错位。')
  process.exit(1)
}

const cliEntry = join(webRoot, 'node_modules', '@ant-design', 'cli', 'dist', 'index.js')
const result = spawnSync(process.execPath, [cliEntry, 'lint', target], { cwd: webRoot, encoding: 'utf8' })
if (result.error !== undefined) {
  console.error(`无法执行 antd lint：${result.error.message}`)
  process.exit(1)
}

const output = `${result.stdout ?? ''}${result.stderr ?? ''}`.trimEnd()
console.log(output)
console.log(`（antd@${antdVersion} / @ant-design/cli@${cliVersion}）`)

if (/No issues found\.$/.test(output)) {
  process.exit(0)
}

const summary = output.match(/^Summary:\s*(.+)$/m)
if (summary === null) {
  // 既没有「No issues found」也没有 Summary：CLI 没跑起来或输出格式变了，不能默认放行
  console.error('antd lint 的输出既无「No issues found」也无 Summary 行，按失败处理。')
  process.exit(1)
}

const counts = {}
for (const [, count, category] of summary[1].matchAll(/(\d+)\s+(deprecated|a11y|usage|performance|skipped)/g)) {
  counts[category] = Number(count)
}
const blocking = ['deprecated', 'a11y', 'usage', 'performance'].reduce((sum, key) => sum + (counts[key] ?? 0), 0)

if (blocking === 0) process.exit(0)

console.error(`\nantd lint 检出 ${blocking} 个问题。本门禁语义是「不许新增 antd 废弃别名与不当用法」，基线为 0。`)
console.error('请改代码，不要放宽门禁；`Tabs.TabPane` 与 `size="default"` 检不出来，仍需评审兜底。')
process.exit(1)
