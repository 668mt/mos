# 更新日志

本文件记录 MOS 各版本的主要变更。

## [3.0] — 开发中

分支：`3.0` ｜ 尚未发布发行包

### 新增

- **短链接**：开放接口 `POST /s` 创建短码，`GET /s/{shortCode}` 302 跳转到带签名的直链。Redis 存储 + TTL + `SETNX` 防重。Java SDK 与 `mos-cli` 均已支持（`--short`）
- **内网/公网差异化重定向**：`/mos/{bucket}/**` 在内网访问时自动 302 到内网地址，节省公网带宽。通过 `mos.server.lan-redirect.*` 配置，默认关闭
- **`mos-cli` 命令行工具**：TypeScript 实现服务端开放接口，已发布到 npm（`@668mt/mos-cli`）。支持分片并发上传、秒传、断点续传、递归目录、多地址故障转移、多 profile 配置，并附带 skill 供 AI 助手使用
- **在线处理能力**：图片/视频缩略图、m3u8 分片重签名、图集、Markdown/TXT 渲染

### 变更

- **升级到 Java 17 / Spring Boot 3.0.2**（2.x 使用 JDK 8，**不可混用**）
- 存储节点与服务的通信改为心跳注册 + 权重调度，支持多副本
- 引入 `mt-spring-generator-starter`，服务端可按实体注解自动建表
- 集成 Prometheus 指标与 OpenAPI（knife4j）文档

### 修复

- 存储节点健康检查优化
- 上传流程优化

## [2.1.1]

发行包：[Releases/tag/2.1.1](https://github.com/668mt/mos/releases/tag/2.1.1)

### 新增

- 审计信息收集，主页图表展示
- 跨桶复制
- 修改文件夹名称与合并文件夹
- 文件访问量显示

### 变更

- 客户端 `client_id` 改为 `id`

## [2.1]

发行包：[Releases/tag/v2.1](https://github.com/668mt/mos/releases/tag/v2.1)

### 新增

- 自动生成缩略图

## 升级指南

`assembly/sqls/` 下的脚本为**增量升级**脚本，需按版本顺序依次执行。

**2.1 → 2.1.1**

```bash
mysql -uroot -p mos < assembly/sqls/mos_ddl-2.1.1-RELEASE.sql
```

**2.1.1 → 2.1.2**

```bash
mysql -uroot -p mos < assembly/sqls/mos_ddl-2.1.2-RELEASE.sql
```

> 首次部署无需执行任何脚本 —— 可开启代码生成器（`mt.config.generator.enabled=true`）由实体类自动建表。

## 关联产物

| 组件 | 版本 | 位置 |
|---|---|---|
| `mos-sdk` | 2.1.2 | [Maven Central](https://central.sonatype.com/artifact/com.github.668mt.mos/mos-sdk) |
| `mos-sdk-spring-boot-starter` | 2.1.2 | [Maven Central](https://central.sonatype.com/artifact/com.github.668mt.mos/mos-sdk-spring-boot-starter) |
| `@668mt/mos-cli` | 1.0.0 | [npm](https://www.npmjs.com/package/@668mt/mos-cli) |
