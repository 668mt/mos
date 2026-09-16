## Context

- `OpenMosController`（`mos-server/src/main/java/mt/spring/mos/server/controller/open/OpenMosController.java:22`）当前仅有一个公开入口 `GET /mos/{bucketName}/**`，由 `OpenMosService` 解析 `pathname` 后走 `requestResource` 返回资源；权限校验通过 `@OpenApi` 注解完成（参考 `mos-server/.../annotation/OpenApi.java`）。
- `mos-server` 已集成 Spring Data Redis（参考既有 `@Autowired StringRedisTemplate` / `RedisTemplate` 使用点），可直接复用作为短码存储。
- 项目里 `OpenMosService`（`mos-server/.../service/OpenMosService.java`）是 Controller 与业务逻辑的中间层，新增端点的业务方法（生成短码、写 Redis、读 Redis）放在 Service 中更利于复用与单测。

本变更**仅在 `OpenMosController` 中新增两个端点**（`POST /s` 创建 + `GET /s/{shortCode}` 解析跳转），对应业务逻辑下沉到 `OpenMosService`，存储层用 Redis；不修改既有 `/mos/{bucketName}/**` 任何行为，也不引入新依赖。

## Goals / Non-Goals

**Goals:**
- 在 `OpenMosController` 中新增 `createShortUrl`（POST `/s`）与 `resolveShortUrl`（GET `/s/{shortCode}`）两个端点；
- 短码存储使用 Redis，key 设计为 `mos:short:{shortCode}`，TTL 由入参 `expireSeconds` 决定；
- 创建端点复用既有 `@OpenApi` 权限校验；解析端点免权限校验；
- 短码生成走 `SecureRandom`，长度不少于 6 位，写入使用 `SET key value NX EX` 保证原子性。

**Non-Goals:**
- 不修改既有 `GET /mos/{bucketName}/**` 的任何行为；
- 不修改 `OpenMosService.requestResource`；
- 不引入新的第三方依赖（短码生成用 JDK `SecureRandom`，存储用既有 `StringRedisTemplate`）；
- 不做短码访问频率限制、IP 黑名单等安全增强（依赖短码本身的不可枚举性 + `SecureRandom` 长度保证基本安全）；
- 不持久化短码到数据库（Redis 过期即清理，符合"临时链接"定位）。

## Decisions

### Decision 1：短码存储统一使用 Redis，key 命名为 `mos:short:{shortCode}`

- **选择**：所有短码存储在 Redis，key 形如 `mos:short:{shortCode}`，value 为 `bucketName + "|" + pathname + "|" + sign + ["|" + queryString]`（固定四段：bucketName / pathname / sign 必填，`queryString` 可选；sign MUST 在 pathname 之后、`queryString` 之前）；TTL = `expireSeconds`。
- **理由**：
  - Redis 自带 TTL 机制，无需额外写定时清理任务，过期 key 自动回收，符合"短链临时"的定位；
  - 单条短码读写都是 O(1)，对 `/s/{shortCode}` 的高并发解析友好；
  - 项目里既有 Redis 接入（Spring Data Redis），无需引入新依赖；
  - **必须把 `sign` 一并存进 Redis**：访问短链 302 跳转到 `/mos/{bucketName}{pathname}` 时会进入 `OpenMosController#mos`（带 `@OpenApi(perms = BucketPerm.SELECT)`），跳转 Location MUST 携带 `sign` 否则签名校验会失败。
- **替代方案**：本地 Caffeine 缓存。**放弃原因**：集群部署下本地缓存无法跨实例共享，且进程重启会丢全部短码；Redis 是更合适的中心化方案。
- **替代方案**：在跳转时让短链客户端重签。**放弃原因**：服务端无法重签（缺少 secretKey），且需要让 SDK 端介入短链跳转流程，破坏了"短链跳转对调用方透明"的定位。

### Decision 2：短码生成使用 `SecureRandom` + Base62，长度 8 位

- **选择**：用 `SecureRandom` 生成 8 字节随机数，转成 64 位整数再编码为 Base62（`[A-Za-z0-9]`），截取前 8 位作为 `shortCode`。
- **理由**：
  - `SecureRandom` 是加密安全随机源，无法被外部推断；
  - 8 位 Base62 的搜索空间 ≈ `62^8 ≈ 2.18e14`，在合理 QPS 下无法被枚举攻击（即使 1000 QPS 也需要 6900 年才遍历完）；
  - Base62 字符集在 URL 中无需转义，可直接放在 path 段。
- **替代方案**：
  - NanoID。**放弃原因**：需要额外引入第三方库；JDK 内置 `SecureRandom` 已能完全覆盖需求。
  - 6 位短码。**放弃原因**：搜索空间 ≈ `5.7e10`，在高并发或长生命周期下存在被枚举攻击风险；选 8 位更稳妥。

### Decision 3：写入 Redis 使用 `SET key value NX EX expireSeconds` 原子操作

- **选择**：在 Service 层用 `stringRedisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS)` 写入。
- **理由**：
  - `SET ... NX EX` 是 Redis 单条原子命令，避免"先 SET 再 EXPIRE"中间崩溃导致永久 key；
  - `NX` 语义保证极小概率的 hash 冲突会被自动检测（返回 `false`），外层循环重试即可。
- **冲突重试策略**：循环上限 5 次；每次重试生成新短码；若仍失败则抛 `BizException` 并返回 500；正常情况下 hash 冲突概率极低，重试几乎不会触发。

### Decision 4：解析跳转端点路径为 `GET /s/{shortCode}`，命中返回 302 `/mos/{bucketName}{pathname}?sign={sign}[&{queryString}]`

- **选择**：路径段 `shortCode` 直接用 `@PathVariable` 接收；命中后从 Redis value 中按 `|` 切出 `bucketName` / `pathname` / `sign` / `queryString` 四段，拼出 `Location = "/mos/" + bucketName + pathname + "?sign=" + sign + (queryString != null ? "&" + queryString : "")` 后 `response.sendRedirect(...)`。
- **理由**：
  - Redis value 已存 `bucketName` / `pathname` / `sign` / `queryString`，解析跳转时无需再次查询 bucket 表或读当前请求的 queryString，单次 Redis GET 即可完成跳转；
  - **跳转 Location MUST 包含 `sign`**：目标 `/mos/...` 接口带 `@OpenApi(perms = BucketPerm.SELECT)`，缺 sign 会导致签名校验失败并返回 401/403；
  - 拼接顺序 `sign` 在前 `queryString` 在后，便于在 URL 上把签名参数放第一个，调试时直观；
  - 使用 302（而非 301）避免被中间代理长期缓存，与项目里既有重定向约定一致（参考 `add-mos-internal-lan-redirect` 的 Decision 5）；
  - **访问端点不再读 `request.getQueryString()`**：queryString 由短链本身携带，调用方访问 `GET /s/{shortCode}` 时**不应**在 URL 自带 queryString；如果带了，后端忽略并使用短链自带的 queryString。
- **`pathname` 切分细节**：用 `value.split("\\|", 4)` 切分（limit=4 保证 queryString 内部的 `|` 不会被切坏，虽然已经校验过 `queryString` 不含 `|`）；长度 < 3 或 prefix/pathname/sign 任一为空视为短码 value 损坏，按 404 处理并 warn 日志。

### Decision 5：创建端点签名参数走 query，业务参数走 body

- **选择**：签名相关字段（`bucketName` / `pathname` / `sign`）走 query/form，与项目内其它 OpenApi 接口（如 `OpenResourceController`、`OpenUploadController`）保持一致；其它业务字段（`expireSeconds` / `queryString`）走 JSON body `ShortUrlCreateRequest{expireSeconds, queryString}`。
- **理由**：
  - **`OpenApiAspect` 优先级**：`getParameter` 先找 method args、再 `request.getParameter`、最后才反射遍历所有 arg（参考 `AbstractAspect.java:25-53`）；把签名字段放 query/form 后切面直接命中第二档，**不必走反射遍历**；
  - **避免语义混淆**：签名参数与业务参数分离，URL 上的字段专注于鉴权 / 路由，body 专注于业务数据；
  - `pathname` / `sign` 校验在 Service 入口处做：`bucketName` 非空、`pathname` 非空 + 以 `/` 开头 + 不含 `..` 且不含 `|`、`sign` 非空且不含 `|`、`queryString` 不含 `|`、`expireSeconds > 0`，校验失败抛 `IllegalArgumentException` 由全局异常处理转 400。
- **替代方案**：全部入参走 `@RequestBody`。**放弃原因**：`@OpenApi` 切面要从 method args 拿 `sign` / `bucketName` 才能完成签名校验；body 作为一个 arg 时切面只能通过反射遍历第三档兜底分支，效率差且语义不清晰。
- **替代方案**：全部入参走 `@RequestParam`。**放弃原因**：与 `POST /s` 创建资源的语义不匹配，且 `expireSeconds` / `queryString` 与 `sign` / `bucketName` 混在 URL 上不直观。

### Decision 6：Service 接口职责最小化，create 只返回 shortCode / resolve 返回 bucketName+pathname+queryString

- **选择**：
  - `OpenMosService.createShortUrl(bucketName, pathname, queryString, expireSeconds)` 返回 `String shortCode`，不返回 `bucketName` / `pathname` / `queryString`（调用方已经在入参里持有，无需回传）；
  - `OpenMosService.resolveShortUrl(shortCode)` 返回 `ShortUrlTarget{bucketName, pathname, queryString}`（包含解析所需的全部字段），不返回 `shortCode`（调用方已经在 path 参数里持有）。
  - `ShortUrlTarget` 包含 `bucketName` + `pathname` + `queryString` 三个字段（静态内部类 + `@Data` + 全参构造器）。
- **响应体 DTO**：`ShortUrlResponse{shortCode}`，仅含短码一个字段。
- **理由**：
  - 最小化接口返回值：调用方在入参里持有的字段不需要在返回值里再回传，避免冗余字段；
  - 解析跳转端点拿 `bucketName` + `pathname` + `queryString` 拼 `/mos/{bucketName}{pathname}[?queryString]` 即可，`shortCode` 在 path 里已存在不需要再回传；
  - Controller 不再拼 `shortUrl` / 不再回 `expireSeconds`：调用方拿 `shortCode` 后自行按 `{scheme}://{host}:{port}/s/{shortCode}` 拼短链更准确（避免 server 端用 `request.getServerPort()` 拼出的端口与调用方实际访问端口不一致，例如反向代理后的端口漂移）；`expireSeconds` 由调用方自己持有，无需回传。
- **删除项**：移除 Controller 中 `HttpServletRequest request` 形参与 `shortUrl` 拼接逻辑；移除 `ShortUrlResponse.shortUrl` 与 `expireSeconds` 字段；移除 `ShortUrlTarget.shortCode` 字段与三参构造器。

## Risks / Trade-offs

- **[Risk] 短码 hash 冲突导致创建失败** → Mitigation：`SET NX` 自动检测冲突 + 外层循环最多 5 次重试，每次重试使用全新 `SecureRandom`；正常情况下 8 位 Base62 冲突概率可忽略。
- **[Risk] Redis 不可用导致创建/解析失败** → Mitigation：依赖 `StringRedisTemplate` 既有异常体系，由全局异常处理器统一转 5xx；不引入额外的降级缓存（保持实现简单）。
- **[Risk] 短码被恶意枚举攻击** → Mitigation：8 位 Base62 的搜索空间 ≈ `2.18e14`，加上 `SecureRandom` 的不可预测性，正常 QPS 下不可枚举；如未来 QPS 极高可考虑增长短码位数（不在本变更范围）。
- **[Risk] Redis TTL 到期但用户访问瞬间短码"看似存在"** → Mitigation：依赖 Redis 自身 TTL 原子语义，TTL 到期后 key 自动消失，下一次 GET 返回 null，自然走 404 分支。
- **[Trade-off] 短码与 `pathname` 是 1:1 关系，但 Redis 中不持久化"谁创建了短码"** → 这是临时链接定位下的有意识取舍；如未来需要审计，再额外持久化创建者即可（不在本变更范围）。
- **[Trade-off] 解析跳转端点免权限校验，意味着短码本身是唯一的鉴权凭据** → 这是临时短链的标准做法；调用方需自行保证短码不外泄到公开场合。

## Migration Plan

1. **代码上线**：发布 `mos-server` 新版本即生效，无需数据迁移（Redis 全新建 key）。
2. **回滚**：将 `OpenMosController` 中新增的两个方法删除即可，Redis 中残留的短码 TTL 到期后自动清理，不影响其它功能。
3. **灰度建议**：先在测试环境创建几条短链验证 302 跳转行为；确认无误后再上生产。

### Decision 9：SDK 侧 `UrlBuildParams.useShortUrl` 与 `ServiceClient.postJson`

- **选择**：
  - `UrlBuildParams` 新增 `Boolean useShortUrl` 字段；`useShortUrl=true` 时 `MosSdk.getUrl` 改走 `POST {host}/s` 拿 `shortCode` 再拼 `{host}/s/{shortCode}`；
  - **签名参数（`bucketName` / `pathname` / `sign`）放进 URL query**，业务参数（`expireSeconds` / `queryString`）放进 JSON body `ShortUrlCreateRequest`；这是为了配合后端 `OpenApiAspect` 从 method args / `request.getParameter` 直接拿到签名字段，避免走反射兜底分支（详见 Decision 5）；
  - `ServiceClient` 新增 `<T> T postJson(String url, Object body, Class<T> type)`，内部用 `JSONObject.toJSONString` + `StringEntity(APPLICATION_JSON)` 发 POST 并 `checkSuccessAndGetResult` 解析 `result` 字段，**复用既有 `HttpClient` / 连接池**；
  - SDK 把 `render` / `gallery` 拼成 `queryString` 放进 body（如 `render=true&gallary=true`）；访问短链时由后端 302 跳转透传，调用方不需要在 `GET /s/{shortCode}` URL 上自带 queryString；
  - `expireSeconds` 转换：把 `UrlBuildParams.expiredTime` × `expiredTimeUnit` 折算为秒后传给 body；若 `expiredTime` 或 `unit` 为空则传 `-1`（由服务端自行处理或拒绝）。
- **理由**：
  - `UrlBuildParams.useShortUrl` 字段名直观，开关语义清晰，不影响 `useShortUrl=false/null` 时的既有行为；
  - `ServiceClient.postJson` 复用既有 HTTP 客户端与 `checkSuccessAndGetResult`，不引入新依赖；
  - 把 `render` / `gallery` 放进 body 的 `queryString`，让后端 302 跳转时直接透传，避免 SDK 拼接短链 URL 时丢失 query（同时短链本身携带业务参数，访问者不带 query 也能命中）；
  - 签名参数走 query 与项目内其它 OpenApi 接口风格一致。
- **替代方案**：SDK 把 `render` / `gallery` 拼到短链 URL 后面（如 `{host}/s/{shortCode}?render=true`）。**放弃原因**：要求访问者必须自己拼 query，无法做到"拿着短链访问就生效"，且短链 URL 不再稳定（同一 `shortCode` 对应不同 query 会得到不同结果）。
- **替代方案**：把签名参数放 header。**放弃原因**：`@OpenApi` 切面只从 method args 或 query/form 取参数，不读 header；改 header 需要同步改切面，工作量大。

## Open Questions

无。短码长度、Redis key 命名、权限策略、重定向语义、冲突重试上限等关键决策均已在本设计中固化，未在 spec 中留下需补需求的空白。
