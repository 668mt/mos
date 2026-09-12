# AGENTS.md

## 编译效率

本项目是 Maven 多模块结构，改动某个模块的代码时**只编译对应模块**即可，**不要**在根目录跑 `mvn compile` 全量编译。

### 编译命令

```bash
# 改了 mos-server 模块（绝大多数本仓库的代码改动都在这里）
cd mos-server && mvn -q -DskipTests compile

# 改了 mos-client 模块
cd mos-client && mvn -q -DskipTests compile
```

退出码 0 表示编译通过；非 0 表示有错误，按 `mvn` 输出定位修复。

### 判断当前改动属于哪个模块

- `mos-server/src/main/java/**` 或 `mos-server/src/main/resources/**` → `mos-server`
- `mos-client/src/main/java/**` 或 `mos-client/src/main/resources/**` → `mos-client`
- 跨模块改动（如公共 entity）→ 两个模块都要编译

### 编译参数

- `-q` 静默模式，只输出错误信息
- `-DskipTests` 跳过测试，编译阶段无需跑单测
- 不要加 `-am`（also-make）参数，除非确实需要构建依赖模块
