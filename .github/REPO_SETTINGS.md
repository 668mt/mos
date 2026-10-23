# GitHub 仓库设置

以下设置需要在 GitHub 网页端配置（<https://github.com/668mt/mos/settings>）。

## 基础信息

**Description**（一句话简介）：

```
Java 编写的分布式对象存储服务：分片上传/秒传/断点续传、签名访问、多副本存储节点、在线缩略图与渲染
```

**Website**：

```
https://github.com/668mt/mos
```

**Topics**（最多 20 个，直接复制下面这行）：

```
object-storage
file-storage
s3-alternative
minio-alternative
java
spring-boot
distributed-systems
typescript
cli
sdk
```

> Topics 直接影响 GitHub 站内搜索曝光，建议全部加上。

## 仓库功能开关

在 <https://github.com/668mt/mos/settings/features> 建议开启：

| 功能 | 建议 | 说明 |
|---|---|---|
| Issues | ✅ 开启 | 已配好模板 |
| Discussions | ✅ 开启 | 问答类问题分流，避免 Issue 堆积 |
| Wiki | ❌ 关闭 | 文档在仓库内，Wiki 容易过期 |
| Projects | ❌ 关闭 | 暂无使用场景 |
| Sponsorship | ❌ 关闭 | 暂不开放赞助 |

## 分支保护

建议保护 `master` 与 `3.0` 分支（<https://github.com/668mt/mos/settings/branches>）：

- ✅ Require a pull request before merging
- ✅ Require status checks to pass before merging
  - `Java 构建 (JDK 17) / compile`
  - `Java 构建 (JDK 17) / package`
  - `mos-cli 测试 (Node 18) / typecheck`
  - `mos-cli 测试 (Node 18) / test`
  - `Java SDK 签名兼容性 / 交叉验证签名`
- ✅ Require branches to be up to date before merging
- 建议同时开启 **Automatically delete head branches**

## Actions 权限

在 <https://github.com/668mt/mos/settings/actions>：

- Workflow permissions → **Read and write permissions**（若后续要用 Actions 发布 npm 包则必须）

## 标签（Tags）

仓库现有的 tag 命名不一致（`2.1.1` 无前缀、`v2.1` 有前缀），建议统一为 `v<version>` 格式：

```bash
git tag -d 2.1.1 && git tag v2.1.1 && git push origin v2.1.1
git tag -d 2.1   && git tag v2.1   && git push origin v2.1
```

> 注意：改动已有 tag 会导致旧下载链接失效，请确认后再操作。

## 后续发布 npm 包（可选）

`mos-cli` 已发布到 npm（`@668mt/mos-cli`）。若要用 GitHub Actions 自动发版，走 **npm Trusted Publishing**（免 token）：

1. 先手动发布一次 1.0.0（已完成）
2. 在 <https://www.npmjs.com/package/@668mt/mos-cli/settings/access> 配置 Trusted Publisher
   - Package：`@668mt/mos-cli`
   - Repository owner：`668mt`
   - Repository：`mos`
   - Workflow filename：`release.yml`
3. 新增 `.github/workflows/release.yml`（见 `mos-cli/PUBLISH.md`）

## 常用链接

| 用途 | 链接 |
|---|---|
| 仓库 | <https://github.com/668mt/mos> |
| 3.0 分支 | <https://github.com/668mt/mos/tree/3.0> |
| Issues | <https://github.com/668mt/mos/issues> |
| 新建 Issue（选模板） | <https://github.com/668mt/mos/issues/new/choose> |
| Discussions | <https://github.com/668mt/mos/discussions> |
| Releases | <https://github.com/668mt/mos/releases> |
| 安全漏洞报告 | <https://github.com/668mt/mos/security/advisories/new> |
| npm 包 | <https://www.npmjs.com/package/@668mt/mos-cli> |
