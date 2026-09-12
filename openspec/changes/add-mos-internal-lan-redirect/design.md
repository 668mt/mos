## Context

- `OpenMosController#mos`（`mos-server/src/main/java/mt/spring/mos/server/controller/open/OpenMosController.java:29`）当前对所有请求一视同仁地走 `OpenMosService.requestResource` 返回资源。
- 配置侧已有 `MosServerProperties`（`mos-server/.../entity/MosServerProperties.java`）作为 `mos.server.*` 配置的承载类，且支持通过 Nacos 远程配置刷新（`@RefreshScope`）。其中 `mos.server.currentIp` 已存在并被用于"任务分片健康检查 / 多网卡识别"。
- 项目里 `668mt.cn:2345/ip` 已经作为"获取当前服务端公网 IP"的稳定端点（参考 `mukeyuan-video` 项目的 `IpParser`）。

本次变更**不修改 `OpenMosController` 的代码**，改为通过新增一个 `HandlerInterceptor` 在 Spring MVC 层拦截 `/mos/{bucketName}/**` 请求，按"域名/IP → 同局域网 → 重定向"策略返回 302 或放行；保持 Controller 与 Service 的纯粹职责不变。

## Goals / Non-Goals

**Goals:**
- 通过 `HandlerInterceptor` 在 `/mos/{bucketName}/**` 入口处新增判定+重定向分支，命中策略时返回 302；不污染 Controller 与 Service。
- 提供可配置项（域名白名单、可选公网 ip），未配置时静默退化为既有行为；公网 IP 未配置时按需懒加载。
- 复用既有 `mos.server.currentIp` 作为内网重定向目标 IP，无需新增 `internalIp`/`internalPort` 配置项。
- 保持现有的 CORS 头、查询参数、`bucketName/pathname` 透传语义不变。

**Non-Goals:**
- 不改动 `OpenMosController`、`OpenMosService.requestResource` 与既有渲染器链。
- 不修改其它接口（如管理端、上传端）的访问行为。
- 不引入新的第三方依赖；优先用 JDK / Spring 内置能力完成 IP 解析、URL 拼接与重定向。
- 不在本次变更里处理"内网多网卡""IPv6 优先解析"等复杂拓扑问题（仅 IPv4 字符串相等判定即可）。

## Decisions

### Decision 1：通过 HandlerInterceptor 实现重定向，不侵入 Controller

- **选择**：新增 `LanRedirectInterceptor`（`HandlerInterceptor`），放在 `mos-server/src/main/java/mt/spring/mos/server/intercept/` 包下，通过 `WebMvcConfigurer` 注册其拦截路径为 `/mos/{bucketName}/**`；命中策略时在 `preHandle` 中直接 `response.sendRedirect(...)` 并返回 `false` 短路后续处理。
- **理由**：
  - 完全不修改 `OpenMosController` 与 `OpenMosService`，符合"非侵入式增强"的工程风格。
  - 拦截路径精确（`/mos/{bucketName}/**`），不会误伤管理端、上传端等其它 `/mos/*` 路径。
  - 与项目里其它横切关注点（如审计、CORS）天然一致地落在拦截器层。
- **替代方案**：在 `OpenMosController#mos` 内联实现。**放弃原因**：在公开接口 Controller 中混入"重定向策略"属于职责污染，不利于后续复用/替换。

### Decision 2：复用既有 `mos.server.currentIp`，不再新增内网 IP/端口配置

- **选择**：内网重定向目标地址 MUST 复用 `mos.server.currentIp` 作为 `{internalIp}`，`{internalPort}` 直接使用 `server.port`；不在 `LanRedirectConfig` 中新增 `internalIp`/`internalPort` 字段。
- **理由**：
  - `mos.server.currentIp` 已经承担"标识本机内网 IP"的语义，运维侧已有维护习惯；不引入含义重复的新配置项。
  - 端口固定为 `server.port`（服务监听的端口就是资源入口端口），无需单独配置。
- **配套处理**：当 `mos.server.currentIp` 为空时，内网重定向 MUST 退化为公网重定向（即按"不同局域网"处理），不抛异常。
- **LanRedirectConfig 最终字段**：
  - `List<String> domains`：启用重定向的域名白名单（如 `rs.668mt.cn`）。
  - `String publicIp`：公网 IP（可选，未配置时按需懒加载）。
  - `Integer publicPort`：公网端口（可选，未配置时退化为 `server.port`）。

### Decision 3：同局域网判定改为"客户端公网 IP vs 服务端公网 IP 相等比较"

- **选择**：将 `request.getRemoteAddr()` 取到的客户端公网 IP 与 `PublicIpResolver` 解析到的服务端公网 IP 进行**字符串相等比较**；相等判定为"同局域网"，否则为"不同局域网"。
- **理由**：
  - 在典型部署下（NAT 出网、无业务层反向代理），`getRemoteAddr()` 实际返回的是客户端**对外**的公网 IP（NAT 出口 IP）；同局域网用户的 NAT 出口就是本机，对应的公网 IP 与服务端公网 IP 相同；不同局域网用户拥有不同的公网 IP，判定天然区分。
  - 比较方式为字符串相等，比"网段前缀匹配"更简单、更准——网段匹配需要处理 `/23`、`/16` 等边界，而字符串相等天然规避了这些问题。
  - 不再需要"枚举本机所有内网网卡"或"前 3 段匹配"。
- **替代方案**：枚举本机网卡 + 同网段匹配。**放弃原因**：跨平台实现复杂、IPv4/IPv6 边界场景多；字符串相等判定已经能覆盖 99% 部署。
- **配套处理**：若任一侧 IP 为空/格式非法，MUST 视为"不同局域网"以保守走公网重定向；任何异常 MUST 被捕获并降级为"不做重定向"，不抛 5xx。

### Decision 4：客户端 IP 优先读 `X-Forwarded-For` 第一段，回退 `X-Real-IP`，最后回退 `getRemoteAddr()`

- **选择**：在拦截器内统一通过 `resolveClientIp(request)` 解析客户端真实公网 IP，优先级为：
  1. `X-Forwarded-For` 第一段非空值（nginx `$proxy_add_x_forwarded_for` 会自动注入真实客户端 IP）；
  2. `X-Real-IP`（nginx `$remote_addr`）；
  3. `request.getRemoteAddr()`（直连场景兜底）。
- **理由**：
  - mos-server 生产部署通常前置 nginx，`getRemoteAddr()` 返回的是 nginx 的内网 IP，与服务端公网 IP 永远不相等，会导致"同局域网"分支永远命中不了——必须读 `X-Forwarded-For`。
  - `X-Forwarded-For` 第一段是真实客户端（最左），后续段是逐级代理追加的，符合业内通用约定。
- **配套要求**：
  - nginx 必须在反代配置里透传 `X-Real-IP $remote_addr;` 和 `X-Forwarded-For $proxy_add_x_forwarded_for;`。
  - 本服务 MUST 仅在受控入口（nginx）后对外暴露；不得让客户端绕过 nginx 直接访问 mos-server，否则攻击者可伪造 `X-Forwarded-For` 绕过重定向判定。
- **风险**：`X-Forwarded-For` 是客户端可控 header，存在伪造风险。Mitigation：通过部署架构保证 mos-server 不暴露公网入口；必要时后续可改为只信任已知代理 IP 段的 XFF（不在本变更范围）。

### Decision 5：重定向 URL 拼接规则

- **选择**：
  - 内网：`http://{mos.server.currentIp}:{server.port}/mos/{pathname}?{queryString}`
  - 公网：`http://{publicIp}:{publicPort}/mos/{pathname}?{queryString}`
- **理由**：
  - `pathname` 与 `bucketName` 的解码/拼接复用 `OpenMosService.getPathname(request, "/mos/" + bucketName)` 的语义（拦截器中可直接按同样规则解析）。
  - 查询参数透传保证 `thumb/render/gallary` 等仍生效。
- **注意**：使用 `302 Found`（非 301）避免被中间代理长期缓存。

### Decision 6：仅在判定为"域名访问"时才触发

- **选择**：用 `request.getServerName()` 是否是 IPv4/IPv6 地址来识别"IP 直连"；如果是 IP，则放行（不重定向）。
- **理由**：与用户诉求"IP 访问 → 直接返回资源"完全对齐；该判定放在拦截器最开始，能让非域名访问零成本通过。

### Decision 7：公网 IP 的获取与缓存

- **选择**：新增 `PublicIpResolver` 服务，按需懒加载调用 `http://668mt.cn:2345/ip`，解析返回 JSON 中的 `ip` 字段；结果在进程内缓存（`volatile String` + 双检锁 / `AtomicReference`），避免每次请求都发起远程调用。
- **理由**：参考 `mukeyuan-video` 项目 `IpParser` 的实现思路，与既有"通过远程端点获取自身公网 IP"惯例保持一致；缓存避免在高 QPS 下重复访问上游服务。
- **失败处理**：当远程端点不可用/解析失败时，MUST 降级为"不做公网重定向"，并打印 warn 日志；不得因获取公网 IP 失败而抛 5xx。

### Decision 8：内网重定向 vs 公网重定向的优先级

- **选择**：判定流程固定为「域名/IP → 域名白名单 → 同局域网 → 内网/公网重定向」。两者 MUST 互斥，每次请求只会命中其中之一或都不命中。
- **理由**：与用户诉求完全对齐；优先级在判定阶段固定，不引入外部配置开关。
- **降级链**：
  - 非域名 → 放行；
  - 域名不在白名单 → 放行；
  - 域名在白名单 + 同局域网（公网 IP 相等）→ 内网 302；
  - 域名在白名单 + 不同局域网 + 公网 IP 可解析 → 公网 302；
  - 域名在白名单 + 不同局域网 + 公网 IP 解析失败 → 放行（warn 日志）。

## Risks / Trade-offs

- **[Risk] 前置反向代理导致 `getRemoteAddr()` 不是真实客户端公网 IP** → Mitigation：拦截器已通过 `X-Forwarded-For` 第一段 → `X-Real-IP` → `getRemoteAddr()` 的优先级解析真实客户端 IP；nginx 必须透传这两个 header。
- **[Risk] `mos.server.currentIp` 未配置导致内网重定向无法进行** → Mitigation：在判定中显式检查，为空时直接走公网分支（视作不同局域网），不抛异常。
- **[Risk] `http://668mt.cn:2345/ip` 上游不可达导致公网 IP 获取失败** → Mitigation：捕获异常并降级为"放行 + warn 日志"，不得向用户暴露 5xx。
- **[Risk] 公网 IP 在进程生命周期内变更** → Mitigation：当前实现只在进程内首次需要时拉取一次；若运维侧需要刷新可通过调整 `public-ip` 配置或重启实例触发。
- **[Risk] 浏览器对 302 的缓存** → Mitigation：使用 `302 Found`（非 301）；如确需禁用缓存可补充 `Cache-Control: no-store`（在 tasks 中按需决定）。
- **[Trade-off] 字符串相等判定依赖 NAT 出口 IP 一致** → 适用于"内网用户通过同一 NAT/网关出口"的典型部署；若内网有多个 NAT 出口指向不同公网 IP，需要运维侧保证 mos-server 与客户端在同一 NAT 出口后，否则会被误判为"不同局域网"。
- **[Trade-off] 重定向到内网 IP 意味着用户浏览器需具备到该内网 IP 的可达性** → 这是本变更的核心目的（同局域网用户直达内网），但需要文档说明"该功能仅对能访问该内网 IP 的客户端生效"。

## Migration Plan

1. **配置上线**：在目标环境的 `mos-server` 配置（bootstrap / Nacos）中追加：
   ```
   mos.server.current-ip=192.168.x.x
   mos.server.lan-redirect.domains[0]=rs.668mt.cn
   # 以下为可选配置；若不配置 public-ip 则由系统按需自动获取
   # mos.server.lan-redirect.public-ip=1.2.3.4
   # mos.server.lan-redirect.public-port=9700
   ```
2. **灰度**：白名单先只放一个域名验证；观察日志中"命中重定向"的请求是否符合预期；同时观察公网 IP 是否被正确懒加载（首次跨网段访问会触发远程调用）。
3. **回滚**：将 `mos.server.lan-redirect.domains` 置空或删除配置项即可，无需重启（依赖 `@RefreshScope`）。
4. **代码上线**：发布 `mos-server` 新版本后即生效；无需数据迁移。

## Open Questions

无。所有"如何判定同局域网 / 用什么配置项 / 何时跳过 / 是否侵入 Controller"的关键决策均已在本设计中固化，未在 spec 中留下需补需求的空白。
