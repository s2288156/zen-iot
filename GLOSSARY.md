# Zen IoT 术语表

本平台术语的**唯一出处**。模块 README 的「能力与接口口径」与 [`docs/architecture.md`](docs/architecture.md) 只引用这里的词、不重述含义；具体口径（配置键名、字段名、上限常量）留在各自文档里。有对应跨模块契约的词条，附一个指向 [`docs/architecture.md`](docs/architecture.md) 的指针，机制留在那边。

## 模块与授权

### 前缀所有权

对外路径的前缀归网关所有：网关暴露 `/api/{service}/**`，剥掉前缀后再转发；服务内只认裸路径。因此同一个接口的对外路径与服务内路径**不是同一个字符串**。

契约见 [`docs/architecture.md`](docs/architecture.md) 的「请求链路与前缀所有权」。

### 两套白名单

免鉴权路径在网关与服务内各有一份配置，且匹配的字符串形态不同，无法合并成一处。

契约见 [`docs/architecture.md`](docs/architecture.md) 的「鉴权：身份来源与两套白名单」。

### ModuleCode

授权判定的最小单位，取值 `admin` / `wcs` / `rcs`。`ADMIN` 指「管理后台」这个模块，不是超级权限。`wcs` / `rcs` 已在枚举里预留取值、网关亦预留 `/api/wcs`、`/api/rcs` 前缀，但当前都没有对应模块。

契约见 [`docs/architecture.md`](docs/architecture.md) 的「模块权限判定」。

### @RequireModule

方法级注解，声明接口属于哪个 `ModuleCode`。判定依据是 Token 里的 `modules` 快照，缺该模块即 403。只支持方法级，写在类上是编译错误；缺注解即不校验模块，但仍要求已登录。

契约见 [`docs/architecture.md`](docs/architecture.md) 的「模块权限判定」。

## 身份与传输

### 身份透传头

网关验签成功后写入的可信请求头。`gateway-header` 模式的下游据此建立身份，自己不需要持有密钥；入站同名头必须先剥离，否则任何调用方都能伪造身份。

契约见 [`docs/architecture.md`](docs/architecture.md) 的「身份头透传与伪造防护」。

## 查询

### 排序字段白名单

分页查询的排序字段由客户端指定；服务端不内建允许清单，校验与否由各调用方自己决定。

## 可观测性

### A 层追踪

追踪只做到这一层：traceId 进日志、跨服务向下游注入 `traceparent`；span 不外发，也没有追踪后端可查。日志与指标 exemplar 因此是唯一的查询面。

链路的走法、前提与断点见 [`docs/observability.md`](docs/observability.md)；为什么停在这一层见 `docs/modules/common-core.md`。

## 边界未确认

设备侧的域边界尚未定案。下列三条只占位、不含定义，定案前不得据此做实现决策；结论见 [#67](https://github.com/s2288156/zen-iot/issues/67)。

### RCS

边界未确认。

### WCS

边界未确认。

### AMR

边界未确认。
