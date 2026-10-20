# 开发线必须严格领先于最近一个 tag

发一个版本的顺序是「先打 tag，再抬开发线」，而抬开发线那一步在 [release-versioning.md](../release-versioning.md) §2 里只是一条纪律，没有任何检查会红：漏掉的后果是开发中的制品顶着已发布的版本号跑，别的门禁一个都不会亮。于是加一条 `releaseLineCheck`（根 `build.gradle.kts`）：取 Gradle 版本的前三段数字，与 `git tag --list 'v*' --sort=-v:refname` 里最新的 `vX.Y.Z` 相比，必须**严格大于**，相等同样判失败。

## 状态

accepted

## 否决过的方案

- **只在 main 上比「相等」**：判据换成「main 的开发线不等于任何已发布的 tag」，比法更宽松，但它会放行「落后于 tag」的形态——开发线停在比最近发布更旧的版本号上，而那同样是漏抬。
- **显式豁免标记**（commit trailer 或 PR label）：把「本次提交允许先不动开发线」写成一处可读的记号。豁免得有人记得加，也得有人记得清，等于把纪律从机器判定降回评审经验，还多引入一份会漂移的状态。
- **把打 tag 与抬版绑成一次脚本执行**：一次动作里同时 tag 与抬版，纪律就不再需要门禁。反对理由见 [release-versioning.md](../release-versioning.md) §2：两步各有独立判据（tag 钉在 release commit 上、抬版是又一个 PR），合并之后任一步出错都得回退整个动作，而 tag 一旦推出去，补救本来就无法脚本化。
- **不做门禁，靠一份不 squash 的 release PR 携带两个 commit**：纪律依然只活在流程文档里，与「没有门禁」等价。

## 后果

- 这条只在 main 上跑（`.github/workflows/ci.yml` 的 `release-line` job）：PR 分支上去比一份所有人共享的 tag 没有意义，所以 PR 永不受影响。
- 接受一个假红面：release commit 与抬版 PR 之间若有别的提交落到 main、或周一的定时任务正好落在那个窗口，main 会红一次，直到抬版合入。那正是它想说的话——那一刻的开发线确实还停在已发布值上。
- 它不进 `check` 也不进 `prePushCheck`：本地也会撞上同一个窗口，而一次改代码的推送不该被一次发版时机的遗留问题拦住。发版人可以在打完 tag 之后手动跑一次自检。
- 边界：git 不可用（源码归档、沙箱）时跳过，与 `lintMarkdown` 的降级口径一致；非 `vX.Y.Z` 的 tag 直接忽略；tag 不必是 HEAD 的祖先，因为它的语义是「这棵树 = 这个版本」而非「最新提交」。
