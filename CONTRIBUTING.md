# Contributing to MOS

感谢你参与 MOS 的开发。

## 开发环境

| 组件 | 版本 |
|---|---|
| JDK | **17+**（3.0 分支基于 Spring Boot 3.0.2） |
| MySQL | 8+ |
| Redis | 5+ |
| Node.js | 18+（仅开发 `mos-cli` 需要） |

## 构建与测试

本项目是 Maven 多模块结构，**改动某个模块时只编译对应模块**，不要在根目录跑全量 `mvn compile`。

```bash
# 改了 mos-server
cd mos-server && mvn -q -DskipTests compile

# 改了 mos-client
cd mos-client && mvn -q -DskipTests compile

# 全量打包
mvn clean package -DskipTests -pl assembly -am
```

`mos-cli` 单独构建与测试：

```bash
cd mos-cli
npm install
npm test          # 98 项测试
npm run typecheck
```

### Java 兼容性验证

`mos-cli` 的签名算法必须与 Java SDK **字节级一致**。修改签名相关代码后，必须跑交叉验证：

```bash
cd mos-cli && bash scripts/verify-java-compat.sh
```

该脚本会用 mos-cli 生成签名，再调用真实的 Java SDK（`MosEncrypt.decrypt`）解析。

## 提交信息

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat: 新增短链接接口
fix: 修复分片合并时的并发问题
docs: 完善配置项说明
refactor: 抽出客户端选择策略
chore: 升级依赖版本
```

## 提 Issue

请附上：

- MOS 版本（分支 / tag）
- JDK、MySQL、Redis 版本
- 复现步骤
- 日志片段（`logs/mos-server.log`）

## 认领功能

`openspec/` 目录用 [OpenSpec](https://github.com/Fission-AI/OpenSpec) 管理变更提案。新功能请先在 `openspec/changes/` 下建一份 proposal，再动手实现。

## 许可

提交代码即表示同意以 [Apache License 2.0](LICENSE) 授权。
