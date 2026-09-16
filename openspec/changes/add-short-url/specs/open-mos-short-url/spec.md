## Purpose

在 `OpenMosController` 中开放"创建短链接"与"短链接解析跳转"两个端点。短链接以 `shortCode` 形式由 Redis 持久化并按入参有效期自动过期，访问短链接时会 302 跳转到对应 `/mos/{pathname}` 资源入口；未命中或已过期则返回 404。

## ADDED Requirements

### Requirement: 必须提供创建短链接的开放接口

系统 MUST 在 `OpenMosController` 中开放 `POST /s` 端点。**`bucketName` / `pathname` / `sign` MUST 通过 query/form 参数传入**（与项目内其它 OpenApi 接口一致，便于 `OpenApiAspect` 从 method args / `request.getParameter` 直接拿到，避免走反射遍历 arg 的兜底分支）；`expireSeconds` / `queryString` 通过 JSON body `ShortUrlCreateRequest{expireSeconds, queryString}` 传入。

入参约束：
- `bucketName`（必填，query）：目标资源所属 bucket；
- `pathname`（必填，query）：目标资源路径，必须以 `/` 开头、不能包含 `..` 或 `|`；
- `sign`（必填，query）：签名串，与项目内既有 `OpenApi` 签名机制一致；
- `expireSeconds`（必填，body）：单位秒，必须为正整数；
- `queryString`（可选，body）：访问短链时要透传到 `/mos/...` 的查询字符串（例如 `render=true&gallary=true`），不得包含 `|`。

系统 MUST 生成一个全局唯一的 `shortCode` 作为短链接标识，并将 `bucketName|pathname|sign[|queryString]` 写入 Redis 的 key `mos:short:{shortCode}`，TTL = `expireSeconds`。**`sign` MUST 一并存进 Redis**，因为访问短链 302 跳转到 `/mos/{bucketName}{pathname}` 时会进入 `OpenMosController#mos`（带 `@OpenApi`），跳转 Location MUST 携带 `sign` 否则签名校验会失败。

响应 MUST 仅返回 `shortCode` 一个字段（短码标识）。`shortUrl` 由调用方自行按 `{scheme}://{host}:{port}/s/{shortCode}` 规则拼出，避免在响应里携带与调用方实际访问 host/port 不一致的拼接结果。

#### Scenario: 合法入参（无 queryString）时成功创建短链接
- **WHEN** 调用方 POST `/s?bucketName=myBucket&pathname=/dir/photo.jpg&sign=<合法签名>` body `{expireSeconds=600}`
- **THEN** 系统 MUST 返回 `200 OK`，响应体仅包含 `shortCode`（非空字符串），并在 Redis 中写入 `mos:short:{shortCode} -> myBucket|/dir/photo.jpg`，TTL = 600 秒

#### Scenario: 合法入参（含 queryString）时成功创建短链接
- **WHEN** 调用方 POST `/s?bucketName=myBucket&pathname=/dir/photo.jpg&sign=<合法签名>` body `{expireSeconds=600, queryString=render=true&gallary=true}`
- **THEN** 系统 MUST 返回 `200 OK`，响应体仅包含 `shortCode`，并在 Redis 中写入 `mos:short:{shortCode} -> myBucket|/dir/photo.jpg|render=true&gallary=true`，TTL = 600 秒

#### Scenario: pathname 为空或非法时拒绝创建
- **WHEN** 调用方传入的 `pathname` 为空字符串、null、包含 `..`、包含 `|` 或不以 `/` 开头
- **THEN** 系统 MUST 返回 `400 Bad Request`，响应体包含明确的参数校验错误信息，且 MUST NOT 在 Redis 中写入任何 key

#### Scenario: queryString 包含 | 时拒绝创建
- **WHEN** 调用方传入的 `queryString` 包含 `|`（会与存储分隔符冲突）
- **THEN** 系统 MUST 返回 `400 Bad Request`，且 MUST NOT 在 Redis 中写入任何 key

#### Scenario: expireSeconds 非正整数时拒绝创建
- **WHEN** 调用方传入 `expireSeconds <= 0` 或非整数
- **THEN** 系统 MUST 返回 `400 Bad Request`，响应体包含明确的参数校验错误信息，且 MUST NOT 在 Redis 中写入任何 key

#### Scenario: 同 pathname 多次创建应得到不同 shortCode
- **WHEN** 调用方在短时间内用相同 `pathname` 与 `expireSeconds` 多次调用创建接口
- **THEN** 每次调用 MUST 返回不同的 `shortCode`（除非发生极端哈希冲突，否则不会出现重复）

### Requirement: 必须提供短链接解析跳转接口

系统 MUST 提供一个 `GET /s/{shortCode}` 解析端点。命中 Redis 时 MUST 返回 `302 Found`，`Location` 头指向 `/mos/{bucketName}{pathname}?sign={sign}[&{queryString}]`（bucketName、pathname、sign、queryString 均来自 Redis 存储值；`sign` 在前，`queryString` 在后），以便目标 `/mos/...` 接口的 `@OpenApi` 签名校验通过。未命中（短码不存在或已过期，或存储值格式非法）时 MUST 返回 `404 Not Found`。调用方访问 `GET /s/{shortCode}` 时**不应**在 URL 自带 queryString（queryString 由短链本身携带并由后端在 302 Location 中透传）。

#### Scenario: 短链接有效时 302 跳转到对应资源入口
- **WHEN** 调用方访问 `GET /s/{shortCode}`，且 Redis 中存在 `mos:short:{shortCode}` 且未过期，其存储值为 `myBucket|/dir/photo.jpg|<sign>`
- **THEN** 系统 MUST 返回 `302 Found`，`Location` 头 MUST 为 `/mos/myBucket/dir/photo.jpg?sign=<sign>`

#### Scenario: 短链接携带 queryString 时一并透传
- **WHEN** 调用方访问 `GET /s/{shortCode}`（不携带 queryString），且 Redis 中存在 `mos:short:{shortCode}` 且存储值为 `myBucket|/dir/photo.jpg|<sign>|render=true&gallary=true`
- **THEN** 系统 MUST 返回 `302 Found`，`Location` 头 MUST 为 `/mos/myBucket/dir/photo.jpg?sign=<sign>&render=true&gallary=true`

#### Scenario: 短链接存储值缺少 sign 时按 404 处理
- **WHEN** Redis 中存在 `mos:short:{shortCode}`，但其值分段不足（缺 sign 段）
- **THEN** 系统 MUST 视为无效短链接，返回 `404 Not Found`，并打印 warn 日志（避免后续跳转因缺 sign 而触发 `@OpenApi` 校验失败）

#### Scenario: 短链接不存在或已过期时返回 404
- **WHEN** 调用方访问 `GET /s/{shortCode}`，且 Redis 中不存在 `mos:short:{shortCode}`（或 key 已过期被自动删除）
- **THEN** 系统 MUST 返回 `404 Not Found`，且 MUST NOT 抛出 5xx

#### Scenario: 短链接存储值格式非法时按 404 处理
- **WHEN** Redis 中存在 `mos:short:{shortCode}`，但其值分隔符位置非法（如 `sign` 段为空或 pathname 为空）
- **THEN** 系统 MUST 视为无效短链接，返回 `404 Not Found`，并打印 warn 日志

### Requirement: 短码生成与冲突处理必须安全可靠

`shortCode` MUST 由进程内的安全随机源生成（不得使用时间戳/自增 ID 等可被枚举的形式），长度 MUST 不少于 6 位以避免在合理 QPS 下出现可枚举攻击。

#### Scenario: 短码随机生成且不可枚举
- **WHEN** 调用方连续创建 N 个短链接
- **THEN** 返回的 `shortCode` MUST 来自 `SecureRandom` 等加密安全随机源，且 MUST 不出现可被外部推断的递增规律

#### Scenario: 写入 Redis 时使用 SET NX EX 避免冲突
- **WHEN** 系统尝试将 `mos:short:{shortCode}` 写入 Redis
- **THEN** MUST 使用 `SET key value NX EX expireSeconds`；若返回失败（极小概率 hash 冲突），系统 MUST 重新生成 `shortCode` 并重试，直到成功为止，且对调用方不暴露重试过程

### Requirement: SDK 必须支持 useShortUrl 参数生成短链接

`mt.spring.mos.sdk.entity.params.UrlBuildParams` MUST 新增 `Boolean useShortUrl` 字段。当 `useShortUrl=true` 时，`MosSdk.getUrl` MUST 改为调用 mos-server 的 `POST /s`（JSON body 含 `bucketName`/`pathname`/`sign`/`expireSeconds`/`queryString`，由 SDK 收集 `render`/`gallery` 等查询参数拼成 `queryString`），拿到 `shortCode` 后拼成 `{host}/s/{shortCode}` 返回。

#### Scenario: useShortUrl=false 走原签名 URL 拼装
- **WHEN** 调用方使用 `UrlBuildParams` 且 `useShortUrl=null` 或 `false`
- **THEN** SDK MUST 走原有 `pathnameDefine.getUrl(host, bucketName, sign)` 路径，不调用 `POST /s`，返回完整签名 URL

#### Scenario: useShortUrl=true 走 POST /s 拿 shortCode
- **WHEN** 调用方设置 `useShortUrl=true` 与 `render=true&gallery=true`
- **THEN** SDK MUST 调用 `POST {host}/s?bucketName=...&pathname=...&sign=...` JSON body `{expireSeconds, queryString="render=true&gallary=true"}`，返回 `{host}/s/{shortCode}`；该短链访问时由后端 302 跳转到 `/mos/{bucketName}{pathname}?render=true&gallary=true`

### Requirement: 短链接接口必须经过 OpenApi 权限校验

创建端点 MUST 标注 `@OpenApi(perms = BucketPerm.SELECT)`（或当前项目惯例的对应权限），由既有 `OpenApiAspect` 完成签名校验、登录用户校验与 bucket 归属校验（要求调用方传入 `bucketName` 以便切面定位目标 bucket）；解析跳转端点 MUST 不标注 `@OpenApi`（免权限校验），仅依赖短码本身的不可枚举性保证安全。

#### Scenario: 未通过 OpenApi 校验时创建接口返回 401/403
- **WHEN** 调用方调用创建接口但 `sign` 校验失败、`bucketName` 不存在或调用方对该 bucket 无 SELECT 权限
- **THEN** 系统 MUST 返回 `401/403`，且 MUST NOT 在 Redis 中写入任何 key

#### Scenario: 解析跳转端点免权限校验
- **WHEN** 任意调用方访问 `GET /s/{shortCode}`（无论是否携带凭证）
- **THEN** 系统 MUST 直接走短码解析逻辑；命中返回 302，未命中返回 404，不因权限校验失败返回 401/403
