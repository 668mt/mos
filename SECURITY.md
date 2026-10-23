# 安全策略

## 支持的版本

| 版本 | 状态 |
|---|---|
| 3.0（当前开发分支） | ✅ 接收安全修复 |
| 2.1.x | ⚠️ 仅接收关键安全修复 |
| < 2.1 | ❌ 不再维护 |

## 上报漏洞

**请不要通过公开 Issue 上报安全漏洞。**

请通过 GitHub 私有漏洞报告功能提交：

<https://github.com/668mt/mos/security/advisories/new>

或直接联系维护者：`765936728@qq.com`

请尽量提供：

- 受影响版本与模块（`mos-server` / `mos-client` / `mos-sdk`）
- 漏洞类型（越权、注入、路径穿越、鉴权绕过等）
- 复现步骤或 PoC
- 影响范围评估

## 响应

维护者会在 **7 个工作日** 内确认收到并给出初步评估。修复完成后会在 advisory 中致谢，并建议你在合适版本发布后升级。

## 部署建议

MOS 默认**不启用 HTTPS**，且签名密钥由使用者自行配置。生产环境请务必：

- 为服务端启用 HTTPS，或在反向代理（Nginx 等）终结 TLS
- 使用强随机 `secretKey`，定期轮换
- 修改默认管理员账号密码（`mos.server.admin-username` / `admin-password`）
- 为 MySQL / Redis 设置强密码，并限制监听地址
- 存储节点与服务端之间处于可信内网，或开启 `mos.client.regist-pwd` 注册密码
- 定期清理 `mos.server.archive.*` 与 `mos.server.clear.*` 归档任务覆盖的日志数据
