## 1. 配置项扩展

- [x] 1.1 在 `MosServerProperties` 中新增内嵌配置类 `LanRedirectConfig`，仅包含 `domains`（域名白名单）、`publicIp`（可选）、`publicPort`（可选），并加入 `mos.server.lan-redirect.*` 绑定；不再新增 `internalIp` / `internalPort` 字段。
- [x] 1.2 确认 `mos.server.current-ip` 已在现有 `MosServerProperties` 中可用，无需新增；如有需要，在 `bootstrap.properties` 与 `bootstrap-prdtest.properties` 中给出注释说明（默认关闭，仅声明 key，便于运维按需启用）。

## 2. 公网 IP 解析能力

- [x] 2.1 在 `mos-server` 下新增 `PublicIpResolver`（`utils` 或 `service` 包），参考 `mukeyuan-video` 的 `IpParser` 实现：调用 `http://668mt.cn:2345/ip`，解析 JSON 中的 `ip` 字段返回。
- [x] 2.2 在 `PublicIpResolver` 中加入进程内缓存（首次按需懒加载、失败时降级为 `null` 并 warn 日志、不抛异常）。
- [x] 2.3 在 `LanRedirectConfig` 中暴露 `getPublicIp()` / `getPublicPort()` 解析方法：优先返回配置值，否则走懒加载/默认端口。

## 3. 拦截器实现

- [x] 3.1 在 `mos-server/src/main/java/mt/spring/mos/server/intercept/` 下新增 `LanRedirectInterceptor`（`HandlerInterceptor`），在 `preHandle` 中按顺序执行判定：IP 直连 → 放行；域名不在白名单 → 放行；域名在白名单 + 同局域网 → 302 内网；域名在白名单 + 不同局域网 → 302 公网；公网 IP 解析失败 → 放行 + warn。
- [x] 3.2 同局域网判定逻辑：通过 `resolveClientIp(request)` 解析客户端公网 IP（优先 `X-Forwarded-For` 第一段 → `X-Real-IP` → `getRemoteAddr()`），与 `PublicIpResolver` 解析到的服务端公网 IP 做字符串相等比较；任一侧为空/非法时视为"不同局域网"。
- [x] 3.3 内网重定向 URL 拼接：`http://{mos.server.currentIp}:{server.port}/mos/{pathname}?{queryString}`；`{pathname}` 复用 `OpenMosService.getPathname` 的语义（直接 URI 截取或调用 service 方法）；`{queryString}` 使用 `request.getQueryString()` 原样拼回（为空则省略 `?`）。
- [x] 3.4 公网重定向 URL 拼接：`http://{publicIp}:{publicPort}/mos/{pathname}?{queryString}`，规则同上。
- [x] 3.5 在重定向响应中保留现有 CORS 头（与 `OpenMosController#mos` 既有设置一致）。
- [x] 3.6 新增（或复用）`WebMvcConfig` 实现 `WebMvcConfigurer`，将 `LanRedirectInterceptor` 注册到路径 `/mos/{bucketName}/**`。

## 4. 验证

- [ ] 4.1 启动 `mos-server`，通过 `rs.668mt.cn` 域名模拟"客户端公网 IP 等于服务端公网 IP"与"不等"两种访问，确认分别 302 到内网地址与公网地址；通过 IP 直连访问确认无重定向。
- [ ] 4.2 关闭 `domains` 白名单配置，确认所有访问均走既有资源返回流程，行为与变更前一致。
- [ ] 4.3 模拟公网 IP 端点不可达（可通过临时改端点 URL），确认不会抛 5xx，而是降级放行 + warn 日志。
