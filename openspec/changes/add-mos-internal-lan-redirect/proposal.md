## Why

当前 OpenMosController 的 `mos` 接口在所有访问场景下都直接由服务器返回资源。当用户通过域名（如 `rs.668mt.cn`）访问时，期望的行为是：

- 同局域网用户 → 直达内网资源（`http://{内网ip}:{端口}/mos/路径`），省去域名解析+公网回源开销；
- 外网用户 → 直达公网资源（`http://{公网ip}:{端口}/mos/路径`），让客户端按自身网络拓扑选择最优链路，避免对端再做无谓的二次回源；
- IP 直连用户 → 维持现状，直接返回资源。

## What Changes

- 新增 `LanRedirectInterceptor`（`HandlerInterceptor`），通过 `WebMvcConfigurer` 注册到 `/mos/{bucketName}/**` 路径，**不修改 `OpenMosController`**：
  - 若用户通过 **域名** 访问（如 `rs.668mt.cn`），且与服务端处于同一局域网，则返回 **302** 重定向到 `http://{mos.server.currentIp}:{server.port}/mos/{pathname}`（保留原始查询参数）。
  - 若用户通过 **域名** 访问但不在同一局域网，则返回 **302** 重定向到 `http://{公网ip}:{端口}/mos/{pathname}`（保留原始查询参数）。
  - 若用户通过 **IP** 直接访问，则放行，直接走既有资源返回流程。
- 新增同局域网判定：取 `request.getRemoteAddr()`（视为客户端公网 IP）与服务端公网 IP（来自 `mos.server.lan-redirect.public-ip` 配置或懒加载）做**字符串相等比较**；相等判定为同局域网，否则为不同局域网。
- 新增公网 IP 获取能力：参考 `D:\work\idea_workspace\springcloud\mukeyuan-video\src\main\java\mt\spring\mukeyuan\utils\IpParser.java` 的做法，请求 `http://668mt.cn:2345/ip` 获取当前服务端的公网 IP，并做进程内缓存以避免每次请求都做远程调用。
- 新增配置项 `mos.server.lan-redirect.domains`（域名白名单）、`mos.server.lan-redirect.public-ip`（可选）、`mos.server.lan-redirect.public-port`（可选）；内网重定向目标 IP 复用既有 `mos.server.current-ip`，端口固定使用 `server.port`，**不再新增内网 IP/端口配置项**。

## Capabilities

### New Capabilities

- `open-mos-lan-redirect`: 当用户通过已配置的域名访问 mos 资源、且与服务端处于同一局域网时，将请求 302 重定向到内网资源链接；其他访问场景保持原行为不变。

### Modified Capabilities

（无现有 spec 需要修改；本变更为新增能力，不修改既有需求的语义。）

## Impact

- 受影响代码：
  - `mos-server/src/main/java/mt/spring/mos/server/intercept/LanRedirectInterceptor.java`（新增，拦截 + 重定向判定）。
  - `mos-server/src/main/java/mt/spring/mos/server/config/WebMvcConfig.java`（或新建）注册新的 `LanRedirectInterceptor`。
  - 新增 `mos-server/.../utils/PublicIpResolver.java`（参考 `IpParser.java`，懒加载+缓存公网 IP）。
  - `mos-server/.../entity/MosServerProperties.java` 新增 `LanRedirectConfig` 内嵌配置类。
  - **`OpenMosController` 与 `OpenMosService` 不修改**（`getPathname` 的语义作为参照，可在新拦截器中复用或独立实现）。
- 受影响 API：`GET /mos/{bucketName}/**` 公共开放接口，新增一种合法响应——`302 Found`，Location 在不同场景下指向内网地址或公网地址。其余行为不变。
- 受影响依赖/配置：新增配置项（域名白名单、可选公网 ip、可选公网端口）；复用既有 `mos.server.current-ip` 与 `server.port` 作为内网重定向目标；需考虑 IPv4 部署与前置代理对 `getRemoteAddr()` 的影响。
- 兼容性：仅对命中策略的请求返回 302，不改变返回 200 资源响应场景下的任何语义，对既有客户端透明。
