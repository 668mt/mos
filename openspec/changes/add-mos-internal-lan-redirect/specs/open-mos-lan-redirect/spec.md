## Purpose

在 `GET /mos/{bucketName}/**` 公开资源接口的入口处，根据"用户是否通过域名访问 + 是否与本服务同局域网"做差异化重定向：同局域网用户直达内网资源地址以节省公网回源带宽，外网用户直达公网资源地址以让客户端按自身网络拓扑选择最优链路；IP 直连访问维持原有行为不变。

## ADDED Requirements

### Requirement: 域名访问且同局域网时必须返回 302 重定向到内网地址

当用户通过已配置的域名访问 `GET /mos/{bucketName}/**` 资源，且系统判定该用户与本服务处于同一局域网时，系统 MUST 返回 `302 Found` 响应，`Location` 头指向内网资源地址 `http://{internalIp}:{serverPort}/mos/{pathname}`，并保留原始请求的查询参数。

其中 `{internalIp}` MUST 复用既有配置项 `mos.server.current-ip` 的值；`{serverPort}` MUST 直接使用 `server.port`；不再为本次变更新增内网 IP/端口的配置项。

#### Scenario: 域名访问且同局域网命中重定向
- **WHEN** 用户使用已配置的域名（如 `rs.668mt.cn`）访问 `/mos/{bucketName}/**` 且系统判定该客户端与本服务处于同一局域网
- **THEN** 系统返回 `302 Found`，`Location` 头为 `http://{mos.server.currentIp}:{server.port}/mos/{pathname}`，查询参数与原请求一致

#### Scenario: 重定向保留 thumb/render/gallary 等查询参数
- **WHEN** 用户访问 `/mos/{bucketName}/**?thumb=true&render=true&gallary=false` 且命中重定向策略
- **THEN** `Location` 头 MUST 保留 `thumb`、`render`、`gallary` 等查询参数

#### Scenario: 重定向保留原始 pathname 与 bucketName
- **WHEN** 用户访问 `/mos/myBucket/dir/photo.jpg` 且命中重定向策略
- **THEN** `Location` 头为 `http://{internalIp}:{server.port}/mos/myBucket/dir/photo.jpg`

### Requirement: 域名访问但不同局域网时必须返回 302 重定向到公网地址

当用户通过域名访问、但与本服务不在同一局域网时，系统 MUST 返回 `302 Found`，`Location` 头指向公网资源地址 `http://{publicIp}:{publicPort}/mos/{pathname}`，并保留原始请求的查询参数。

#### Scenario: 域名访问且跨网段重定向到公网地址
- **WHEN** 用户使用已配置的域名访问 `/mos/{bucketName}/**` 且系统判定该客户端与本服务不在同一局域网
- **THEN** 系统返回 `302 Found`，`Location` 头为 `http://{publicIp}:{publicPort}/mos/{pathname}`，查询参数与原请求一致

#### Scenario: 公网重定向保留 thumb/render/gallary 等查询参数
- **WHEN** 用户访问 `/mos/{bucketName}/**?thumb=true&render=true` 且命中公网重定向策略
- **THEN** `Location` 头 MUST 保留 `thumb`、`render` 等查询参数

#### Scenario: 公网重定向保留原始 pathname 与 bucketName
- **WHEN** 用户访问 `/mos/myBucket/dir/photo.jpg` 且命中公网重定向策略
- **THEN** `Location` 头为 `http://{publicIp}:{publicPort}/mos/myBucket/dir/photo.jpg`

### Requirement: 同局域网判定基于客户端公网 IP 与服务端公网 IP 相等比较

系统 MUST 通过以下方式判定"是否同局域网"：
- 解析客户端公网 IP：优先取 `X-Forwarded-For` 请求头的第一段（nginx `$proxy_add_x_forwarded_for` 注入的真实客户端 IP），回退 `X-Real-IP`，最后回退 `HttpServletRequest.getRemoteAddr()`；
- 取服务端公网 IP（来自配置 `mos.server.lan-redirect.public-ip` 或懒加载端点 `http://668mt.cn:2345/ip`）；
- 当两者字符串相等时判定为"同局域网"，否则判定为"不同局域网"。

任一侧 IP 为空或格式非法时 MUST 视为"不同局域网"以走公网重定向分支；任何异常 MUST 被捕获并降级为"不重定向"，不得抛 5xx。

#### Scenario: 客户端公网 IP 与服务端公网 IP 相等时判定为同局域网
- **WHEN** `X-Forwarded-For` 为 `1.2.3.4, 10.0.0.1` 且服务端公网 IP 解析结果为 `1.2.3.4`
- **THEN** 系统 MUST 判定为同局域网，并按"内网重定向"分支处理

#### Scenario: 客户端公网 IP 与服务端公网 IP 不等时判定为不同局域网
- **WHEN** `X-Forwarded-For` 为 `5.6.7.8, 10.0.0.1` 且服务端公网 IP 解析结果为 `1.2.3.4`
- **THEN** 系统 MUST 判定为不同局域网，并按"公网重定向"分支处理

#### Scenario: 缺失 X-Forwarded-For 时回退 X-Real-IP
- **WHEN** 请求未携带 `X-Forwarded-For` 但携带 `X-Real-IP: 1.2.3.4`，且服务端公网 IP 解析结果为 `1.2.3.4`
- **THEN** 系统 MUST 通过 `X-Real-IP` 判定为同局域网

#### Scenario: 缺失代理头时回退 getRemoteAddr
- **WHEN** 请求未携带 `X-Forwarded-For` 与 `X-Real-IP`，`getRemoteAddr()` 为 `1.2.3.4`，且服务端公网 IP 解析结果为 `1.2.3.4`
- **THEN** 系统 MUST 判定为同局域网

#### Scenario: 服务端公网 IP 解析失败时不抛 5xx
- **WHEN** 公网 IP 端点不可达或返回异常
- **THEN** 系统 MUST 捕获异常、打印 warn 日志，并按"不做重定向、放行原请求"处理

### Requirement: 公网 IP 必须可配置且支持自动获取

`{publicIp}` MUST 可通过配置项 `mos.server.lan-redirect.public-ip` 直接指定；当未配置时，系统 MUST 按需懒加载调用 `http://668mt.cn:2345/ip` 获取当前服务端的公网 IP，并缓存结果以避免每次请求都做远程调用。`{publicPort}` MUST 可通过配置项 `mos.server.lan-redirect.public-port` 指定；当未配置时 MUST 退化为 `server.port`。

#### Scenario: 配置公网 IP 时直接使用配置值
- **WHEN** `mos.server.lan-redirect.public-ip` 已配置为 `1.2.3.4`
- **THEN** 系统 MUST 使用 `1.2.3.4` 作为重定向目标地址，禁止发起远程 IP 查询

#### Scenario: 未配置公网 IP 时自动获取并缓存
- **WHEN** `mos.server.lan-redirect.public-ip` 未配置
- **THEN** 系统 MUST 在首次需要时调用 `http://668mt.cn:2345/ip` 获取公网 IP，并对该值进行缓存以避免后续每次请求重复调用

#### Scenario: 公网端口未配置时使用 server.port
- **WHEN** `mos.server.lan-redirect.public-port` 未配置
- **THEN** 系统 MUST 使用 `server.port` 作为公网重定向端口

### Requirement: 通过 IP 直接访问时必须保持原资源返回行为

当用户通过 IP（而非域名）直接访问 `GET /mos/{bucketName}/**` 时，系统 MUST 不触发任何重定向，直接按照原有逻辑返回资源。

#### Scenario: 使用 IP 直连不重定向
- **WHEN** 用户通过 IP（如 `http://10.0.0.1:8080/mos/{bucketName}/**`）访问资源
- **THEN** 系统 MUST 不返回 302，而 MUST 直接走既有的资源返回流程

#### Scenario: IP 直连访问返回既有资源
- **WHEN** 用户通过 IP 直接访问一个真实存在的资源
- **THEN** 系统 MUST 返回既有的资源响应（200/相应资源内容），行为与变更前一致

### Requirement: 域名白名单未配置时退化为原行为

启用重定向策略的域名白名单 MUST 通过配置项 `mos.server.lan-redirect.domains` 声明；当该白名单为空或未配置时，重定向 MUST 不会触发，系统 MUST 退化为原有行为。

#### Scenario: 未配置域名白名单时退化为原行为
- **WHEN** 配置中未声明任何启用重定向的域名
- **THEN** 系统 MUST 不进行任何重定向，所有访问均按原有逻辑返回资源

#### Scenario: 仅配置的域名才会触发重定向判定
- **WHEN** 用户访问的域名不在白名单内
- **THEN** 系统 MUST 跳过域名+同局域网判定逻辑，直接按原有逻辑处理

### Requirement: 重定向响应不得破坏 CORS 头

当命中重定向策略时，响应 MUST 保留现有的 CORS 相关响应头（`Access-Control-Allow-Origin`、`Access-Control-Allow-Credentials`、`Access-Control-Expose-Headers`、`Access-Control-Allow-Headers`）。

#### Scenario: 302 响应携带 CORS 头
- **WHEN** 命中域名+同局域网重定向策略
- **THEN** 302 响应 MUST 携带与原 200 资源响应一致的 CORS 头
