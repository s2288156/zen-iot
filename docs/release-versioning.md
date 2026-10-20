# 版本与发布策略

> **这篇文档放在哪**：版本号的真源是根 `build.gradle.kts` 顶部那一行（§1），已发布版本的真源是 `git tag` 本身。本文不抄当前版本数字——那种清单下一次发版就腐烂——只写发版那一刻的顺序、tag 的形态，和每条规矩背后的理由。它回答三个问题：一个版本号写在哪一处、发一个版本按什么顺序做、tag 长什么样。
>
> 别和「依赖版本一致性」混谈：那说的是同一个库在不同模块解析出不同版本，规则见 [`docs/dependency-governance.md`](dependency-governance.md)，门禁见 [`docs/quality-gates.md`](quality-gates.md) §2.7。

## 1. 版本号只写在一处

| 写在哪里                               | 谁读它                                                                                                                                         | 发版那一刻         |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ |
| 根 `build.gradle.kts` 顶层的 `version` | 全部子项目（`subprojects` 里是 `version = rootProject.version`）；`admin-service` 的 `bootBuildInfo` 把它写进 `META-INF/build-info.properties` | 改成被发布的版本号 |

对外能看到的版本全都从这一个字面量派生：

| 对外表现                                                     | 派生方式                                                                                                             |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------- |
| `/v3/api-docs` 的 `info.version`（Apifox 与 SDK 生成器读的） | `ZenAdminOpenApiConfiguration` 从 `BuildProperties` 取，没有默认值：`build-info.properties` 缺失时上下文直接起不来   |
| 各模块 `jar` 的归档名                                        | Gradle 默认 `${name}-${version}`；可交付的 `bootJar` 归档名被钉成 `admin-service.jar` 之类，所以版本不出现在制品名里 |

`web/package.json` **没有** `version` 字段：它没有任何读者，留着只是一份会漂移的抄本——`v0.0.1` 之前 Gradle 侧 `0.0.1-SNAPSHOT`、web 侧 `0.0.0` 就是这么分叉的，那次对齐是 PR `#80`。`versionSourceCheck` 断言它缺席，并断言没有任何模块脚本另写一份版本（门禁说明见 [`quality-gates.md`](quality-gates.md) §2.11）。

开发线上 `info.version` 报的是带 `-SNAPSHOT` 的开发线版本。semver 里它就是「尚未发布」的合法表达，比停在最近一个发布值更接近真相；以前靠「发版时不动那个硬编码默认值」来避免谎报新版本，现在由后缀本身表达。

## 2. 发一次版的顺序

1. **发布 PR**：只改根 `build.gradle.kts` 那一行的字面量，其他文件一个不动。标题 `build: 发布 X.Y.Z`。判据：diff 里只有那一个版本字面量。
2. **等三条门禁全绿**：PR 上的 `ci.yml`、`web-ci.yml`、OSV，加上 squash 进 main 之后 main 那一次的同一组。判据是 run 结论为 success，不是本机跑过。
3. **打 tag**（形态见 §3），钉在**那个 release commit** 上。判据：`git for-each-ref refs/tags/vX.Y.Z --format='%(*objectname)'` 得到的短 sha 就是它。
   - 不要求它是 main 尖。Dependabot 随时会把尖推走，而 tag 的语义是「这棵树 = 这个版本」，不是「最新提交」。
   - 打之前确认这棵树上不再带 `-SNAPSHOT`：`-SNAPSHOT` 的语义是「尚未发布」，和一个已发布的 tag 直接矛盾，而 tag message 与 tag 指向的树都改不掉（`#80` 存在的全部理由）。
4. **推 tag**：`git push origin vX.Y.Z`，走本地钩子。
5. **抬开发线**：紧接着又一个独立 PR，把同一处抬到下一个 `-SNAPSHOT`（样板 `#81`）。判据：main 上的开发线版本号带 `-SNAPSHOT`。
6. **不建 GitHub Release**（§5）。

## 3. tag 的形态

- **命名**：`vX.Y.Z`，带 `v` 前缀，semver。
- **类型**：annotated，中文正文，**不签名**（仓库没有既定的 signing key）。
- **写法**：`git tag -a -F <message-file> vX.Y.Z <commit>`。正文用文件给，多段结构才不会塌成一行。
- **正文四段**：subject 一句范围声明 → `范围：` → `不在本版本内：` → 末行「不支持生产使用」。tag message immutable，多写四行比将来考古便宜。样板直接取：`git for-each-ref refs/tags/v0.0.1 --format='%(contents)'`。
- `不在本版本内：` 这段写给未来的读者：它说清这个 tag 覆盖到哪、哪些还只是骨架，于是「能不能拿 `v0.0.1` 做 X」不必重读代码。

## 4. 下一个版本号怎么定

- `-SNAPSHOT` 只属于 Gradle 开发线：它同时是对外报告的那个 `info.version` 的形态，语义就是「尚未发布」。
- 抬 patch 还是 minor，看下一段工作属于哪一档：`v0.0.1` 之后抬的是 `0.1.0-SNAPSHOT`，因为接下来要做的是 dashboard 与设备侧。

## 5. 边界

- 撤销已经推出去的 tag 是破坏性的共享动作：由人执行，且先取得批准；相关门禁红就停下来。
- 没有制品、没有 changelog 时不建 GitHub Release 页面——建了只是空壳。将来要对外交付时，从 tag 补。

## 6. 还没有闸住的一笔

这一笔已立一单跟踪，方向写在这里，别让约定只活在评审经验里。

- **抬版纪律没有门禁**（`#84`）：tag 之后忘记抬 `-SNAPSHOT`，开发中的制品会顶着已发布的版本号跑，而没有任何检查会红。方向是一条可执行检查，比对 Gradle 版本与最近一个 tag；判据要先定，因为发版那一刻的版本号本来就该等于 tag。

这一条被门禁接住之后，就删掉它——和 `AGENTS.md` 的「每条坑自带删除条件」同一个规矩。
