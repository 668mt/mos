## Why

当前 `OpenMosController` 仅暴露资源访问入口 `/mos/{bucketName}/**`，缺乏"为某条资源生成可短期分享的临时链接"的能力。在运营/客服/审核等场景下，运营需要把一条比较长的 `pathname`（含 bucket 前缀）分享给临时用户时，直接贴原 URL 既不直观又容易泄露完整路径结构；调用方需要一个能在指定秒数后失效的短链接生成接口，传入目标 `pathname` 与有效期，返回一个可由公开访问入口解析回原 `pathname` 的短码。

## What Changes

- 在 `OpenMosController` 中新增一个用于创建短链接的开放接口（路径形如 `/s`），使用已有的 `@OpenApi` 注解做权限校验；
- 入参：`pathname`（必填，目标资源路径，与 `OpenMosService` 解析 `pathname` 的语义一致）、`expireSeconds`（必填，单位秒，正整数）；
- 短码存储使用 Redis：key 设计为 `mos:short:{shortCode}`，value 为 `pathname`，并设置 TTL = `expireSeconds`；
- 短码生成策略：使用 `SecureRandom` / NanoID 等进程内随机串，配合 Redis 的 `SET key value NX EX seconds` 保证同名不冲突且自动过期；
- 返回体包含 `shortCode` 与完整短链 URL（由 `request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/s/" + shortCode` 拼出，便于调用方直接复制）；
- 新增一个 `GET /s/{shortCode}` 的解析端点（同 Controller 或新 Controller 均可，遵循"开放接口"语义）：命中 Redis 则 302 重定向到 `/mos/{pathname}`（保留原查询参数），未命中或短码不存在则返回 404；
- 不修改既有 `GET /mos/{bucketName}/**` 的任何行为；不引入新的第三方依赖（仅复用既有的 Spring Data Redis 能力）。

## Capabilities

### New Capabilities

- `open-mos-short-url`: 在 `OpenMosController` 中开放"创建短链接"与"短链接解析跳转"两个端点，短链接存储于 Redis 并按入参有效期自动过期，未命中返回 404，命中返回 302 跳转到对应 `/mos/{pathname}` 资源入口。

### Modified Capabilities

（无现有 spec 需要修改；本变更为新增能力，不修改既有需求的语义。）

## Impact

- 受影响代码：
  - `mos-server/src/main/java/mt/spring/mos/server/controller/open/OpenMosController.java`（新增 `createShortUrl` 与 `resolveShortUrl` 两个端点）；
  - `mos-server/src/main/java/mt/spring/mos/server/service/OpenMosService.java`（新增 `createShortUrl(pathname, expireSeconds)` 与 `resolveShortUrl(shortCode)` 业务方法，内部使用 `StringRedisTemplate` 操作 Redis）；
  - 复用既有 `@OpenApi` 注解做权限校验（创建端点需要调用方具备相应权限，解析跳转端点可设为免权限或挂在同一权限下，按项目惯例决定）。
- 受影响 API：
  - 新增 `POST /s` 入参 `{pathname, expireSeconds}` → 返回 `{shortCode, shortUrl, expireSeconds}`；
  - 新增 `GET /s/{shortCode}` → 命中返回 `302` 跳 `/mos/{pathname}`，未命中返回 `404`。
- 受影响依赖/配置：仅依赖项目既有 Spring Data Redis（已在 `mos-server` 中），不引入新依赖；Redis 连接复用既有配置。
- 兼容性：纯新增接口，不影响既有 `/mos/{bucketName}/**` 与管理端接口的行为。
