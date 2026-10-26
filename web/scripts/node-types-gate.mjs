#!/usr/bin/env node
/**
 * Node 版本同源门禁。
 *
 * web 的 Node 版本口径只有三个值、两个文件：`web-ci.yml` 的 `node-version` 与 `web/package.json` 的
 * `engines.node`、`@types/node`。各改各的都不会报错——`@types/node` 描述的是另一个运行时，而运行时
 * 与 `engines` 的偏差只在某次 vite/vitest 行为差异里才暴露。
 *
 * 本脚本只在 CI 里跑，`process.version` 就是 `node-version` 提供的那个运行时，所以不必解析 workflow YAML：
 * 只比两对——`@types/node` 的 major 与 `engines.node` 下限的 major 都要等于运行时 major。
 * 抬运行时而忘了抬另两处，这里就红。
 *
 * job / step 显示名不写版本号，门禁也不解析 YAML：脚本口径只认会改变执行结果的那三个值。
 */
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const webRoot = dirname(dirname(fileURLToPath(import.meta.url)))

// 版本串或范围串（">=X.0.0"、"X.Y.Z"）都取第一段整数作为 major
const majorOf = (value) => {
  const match = value.match(/\d+/)
  return match === null ? Number.NaN : Number(match[0])
}

const runtime = process.versions.node
const runtimeMajor = majorOf(runtime)

const manifest = JSON.parse(readFileSync(join(webRoot, 'package.json'), 'utf8'))
const enginesRaw = manifest.engines?.node ?? ''
const enginesMajor = majorOf(enginesRaw)

const typesVersion = JSON.parse(
  readFileSync(join(webRoot, 'node_modules', '@types', 'node', 'package.json'), 'utf8'),
).version
const typesMajor = majorOf(typesVersion)

const failures = []
if (enginesMajor !== runtimeMajor) {
  failures.push(`engines.node "${enginesRaw}" (major ${enginesMajor}) vs runtime Node ${runtime}`)
}
if (typesMajor !== runtimeMajor) {
  failures.push(`@types/node ${typesVersion} (major ${typesMajor}) vs runtime Node ${runtime}`)
}

if (failures.length > 0) {
  console.error(
    'Node version coupling check failed. The CI runtime, engines.node and @types/node must name the same Node major.',
  )
  for (const failure of failures) {
    console.error(`  - ${failure}`)
  }
  console.error('Raise them together; see docs/dependency-governance.md.')
  process.exit(1)
}

console.log(`Node version coupling OK: runtime ${runtime}, engines.node "${enginesRaw}", @types/node ${typesVersion}`)
