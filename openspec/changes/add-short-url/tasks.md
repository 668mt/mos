## 1. Service 层业务逻辑

- [x] 1.1 在 `OpenMosService` 中新增 `String createShortUrl(String bucketName, String pathname, String sign, String queryString, long expireSeconds)`：使用 `SecureRandom` 生成 8 位 Base62 短码；循环（上限 5 次）调用 `stringRedisTemplate.opsForValue().setIfAbsent("mos:short:" + shortCode, bucketName + "|" + pathname + "|" + sign [+ "|" + queryString], expireSeconds, TimeUnit.SECONDS)`，冲突时重试；返回生成的 `shortCode`。
- [x] 1.2 在 `OpenMosService` 中新增入参校验：`bucketName` 非空、`pathname` 非空 + 以 `/` 开头 + 不含 `..` 且不含 `|`、`sign` 非空且不含 `|`、`queryString` 不含 `|`、`expireSeconds > 0`；任一校验失败抛 `IllegalArgumentException`。
- [x] 1.3 在 `OpenMosService` 中新增 `ShortUrlTarget resolveShortUrl(String shortCode)`：从 Redis 读取 `mos:short:{shortCode}` 对应 value；用 `value.split("\\|", 4)` 切出 `bucketName` / `pathname` / `sign` / `queryString`，封装为 `ShortUrlTarget(bucketName, pathname, sign, queryString)`；未命中或 value 为空/切分结果不合法（如 sign 段为空）返回 `null`。
- [x] 1.4 在 `OpenMosService` 同文件新增 `static class ShortUrlTarget { String bucketName; String pathname; String sign; String queryString; }`（含 `@Data` 与全参构造器），承载 resolve 返回的四个字段。

## 2. Controller 层端点

- [x] 2.1 在 `OpenMosController` 中新增 `POST /s` 端点 `createShortUrl(@RequestParam String bucketName, @RequestParam String pathname, @RequestParam String sign, @RequestBody ShortUrlCreateRequest request)`：调用 `OpenMosService.createShortUrl(bucketName, pathname, sign, request.getQueryString(), request.getExpireSeconds())`；返回 `ShortUrlResponse{shortCode}`。`bucketName` / `pathname` / `sign` 走 query 以便 `OpenApiAspect` 直接命中 method args / `request.getParameter`；`expireSeconds` / `queryString` 走 JSON body。
- [x] 2.2 在 `OpenMosController` 中新增 `GET /s/{shortCode}` 端点 `resolveShortUrl(@PathVariable String shortCode, HttpServletResponse response)`：调用 `OpenMosService.resolveShortUrl`；命中则 `response.sendRedirect("/mos/" + target.getBucketName() + target.getPathname() + "?sign=" + target.getSign() + (target.getQueryString() != null ? "&" + target.getQueryString() : ""))` 并返回；未命中返回 404 状态。**不读 `request.getQueryString()`**，queryString 由短链本身携带；sign MUST 在 Location 中透传以通过 `/mos/...` 的 `@OpenApi` 签名校验。
- [x] 2.3 创建端点 MUST 标注 `@OpenApi(perms = BucketPerm.SELECT)`（由既有 `OpenApiAspect` 校验 `sign` / 登录用户 / bucket 归属，从 method args / `request.getParameter` 取 `sign` / `bucketName`）；解析跳转端点不标注 `@OpenApi`（免权限校验）。

## 3. 响应 DTO

- [x] 3.1 新建 `ShortUrlResponse` DTO（`mos-server/.../entity/dto/ShortUrlResponse.java`），仅包含 `shortCode` 一个字段。
- [x] 3.2 新建 `ShortUrlCreateRequest` DTO（`mos-server/.../entity/dto/ShortUrlCreateRequest.java`），仅包含 `expireSeconds` 与 `queryString` 两个字段（`bucketName` / `pathname` / `sign` 走 query）。

## 4. SDK 集成

- [x] 4.1 在 `mt.spring.mos.sdk.entity.params.UrlBuildParams` 中新增 `Boolean useShortUrl` 字段。
- [x] 4.2 在 `mt.spring.mos.sdk.http.ServiceClient` 中新增 `<T> T postJson(String url, Object body, Class<T> type)` 方法（FastJSON 序列化 + `StringEntity(APPLICATION_JSON)` + 复用既有 `HttpClient` / `checkSuccessAndGetResult`）。
- [x] 4.3 在 `mt.spring.mos.sdk.MosSdk#getUrl` 中：当 `useShortUrl=true` 时改为调 `POST {host}/s?bucketName=...&pathname=...&sign=...`（body 仅含 `expireSeconds` / `queryString`），把当前 `render`/`gallery` 拼成 `queryString` 放进 body；返回 `{host}/s/{shortCode}`。`useShortUrl=false/null` 时走原有 `pathnameDefine.getUrl` 路径不变。

## 5. 编译与验证

- [x] 5.1 编译 `mos-server` 模块：`cd mos-server && mvn -q -DskipTests compile`，确认退出码为 0。
- [x] 5.2 编译 `mos-sdk` 模块：`cd mos-sdk && mvn -q -DskipTests compile`，确认退出码为 0。
- [ ] 5.3 启动 `mos-server` 后通过 curl 调用 `POST /s?bucketName=myBucket&pathname=/dir/photo.jpg&sign=<合法签名>` body `{expireSeconds=600, queryString=render=true}`，确认返回 `shortCode` 且 Redis value 为 `myBucket|/dir/photo.jpg|<sign>|render=true`。
- [ ] 5.4 用上一步返回的 `shortCode` 访问 `GET /s/{shortCode}`（不带 queryString），确认 302 跳转到 `/mos/{bucketName}{pathname}?sign=<sign>&{queryString}`，且跳转后 `/mos/...` 接口的 `@OpenApi` 签名校验通过；等待 expireSeconds 后再次访问确认返回 404。
- [ ] 5.5 故意传入 `pathname=`（空）或 `expireSeconds=0` 或 `queryString` 含 `|`，确认返回 400 且 Redis 中无新增 key。
- [ ] 5.6 访问一个不存在的 `shortCode`，确认返回 404。
- [ ] 5.7 SDK 侧 `MosSdk.getUrl(UrlBuildParams.builder(...).useShortUrl(true).build())` 走通整链路，断言返回的 URL 以 `{host}/s/` 开头且 shortCode 在 Redis 中存在。
