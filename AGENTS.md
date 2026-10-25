# AGENTS.md

## 语言

回复必须使用中文来回答。

## 项目概览

- **MOS** —— Java 编写的分布式对象存储服务，服务端（`mos-server`）与存储节点（`mos-client`）分离
- **JDK 17 / Spring Boot 3.0.2**（2.x 分支为 JDK 8，不要混用）
- **mos-cli** —— TypeScript 命令行工具，npm 包 `@668mt/mos-cli`，**不属于 Maven 构建**，单独用 npm 构建
- **mos-web** —— Web 控制台（React 18 + TS + antd5 + UmiJS 4），**不属于 Maven 构建**，单独用 npm 构建；详见 `mos-web/AGENTS.md`

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
- `mos-sdk/src/main/java/**` → `mos-sdk`
- `mos-base/src/main/java/**` → `mos-base`
- `mos-sdk-spring-boot-starter/src/main/java/**` → `mos-sdk-spring-boot-starter`
- `mos-cli/**` → 走 npm，不走 Maven
- 跨模块改动（如公共 entity）→ 受影响的模块都要编译

### 编译参数

- `-q` 静默模式，只输出错误信息
- `-DskipTests` 跳过测试，编译阶段无需跑单测
- 不要加 `-am`（also-make）参数，除非确实需要构建依赖模块

## mos-cli 开发

```bash
cd mos-cli
npm run typecheck    # 类型检查
npm test             # 98 项测试
npm run build        # 构建到 dist/
npm pack --dry-run   # 检查打包内容
```

### 签名兼容性（重要）

`mos-cli` 的签名算法必须与 Java SDK **字节级一致**，改动 `src/core/javaAes.ts`、`src/core/sign.ts`、`src/util/util.ts` 后**必须**跑交叉验证：

```bash
# 先编译 Java SDK
cd .. && (cd mos-base && mvn -q -DskipTests compile) && (cd mos-sdk && mvn -q -DskipTests compile)
cd mos-cli && bash scripts/verify-java-compat.sh
```

预期输出 `全部通过：7/7 个签名均可被 Java SDK 正确解析`。

> 服务端校验签名用的是 `MosEncrypt`，其 AES 密钥派生依赖 `sun.security.provider.SecureRandom`（现代 JDK 中 `SHA1PRNG` 实际映射到该类），**不是**简单的「key 做一次 SHA-1」。改动密钥派生逻辑会直接导致服务端拒绝请求。

## 服务端契约注意

以下为实测确认的服务端既有行为，改动前请留意：

- 同名上传必须 `--cover`（重名检查在秒传判定**之前**）
- 服务端按 `md5 + size` 去重，跳过秒传会报 `uploadFile不存在`
- 根目录 + `keyWord` 过滤返回空（`ResourceMapper.xml` 目录查询条件为 `and 1 = 2`）
- 短链有效期必须 ≥ 60 秒
- 缩略图生成由 `ResourceService` 的 `afterCommit` → `ResourceMetaService.calculateMeta` 触发（`ThumbCron` 定时任务已被注释掉）
- `enable-auto-import` 只是配置项，导入实现已被注释，暂不可用
- `mos_file_house.encode` 是**文件头标记**（固定密钥 AES 加密的路径前缀），不是内容加密

## 安全红线（禁止事项）

- **禁止尝试密码破解 / 暴力猜解密码**。包括但不限于：
  - 批量试常见弱口令去撞登录接口
  - 拿 BCrypt 等哈希离线跑字典比对
  - 反复试错把账号试到锁定
- **禁止用生产环境的真实账号反复试登录**。本项目账号有失败计数与锁定机制（连续失败会锁定账号、触发验证码），试错会造成真实影响。
- 需要账号时**直接问用户要**：用户名、密码，或让用户自己解锁 / 重置。
- **不要为了验证而编写、启动 mock / 假后端服务**（不要新增 `mock-server.cjs` 之类的文件，也不要把 dev 代理指向自造的假接口）。验证优先用**真实后端**：
  - 不需要登录的接口直接 `curl` 打真实后端，让服务端当 oracle 回显真实行为；
  - 需要登录的**直接问用户要账号**。
- 禁止尝试绕过验证码等鉴权校验（如利用空值/类型强转等实现缺陷），发现此类缺陷应**如实报告给用户**，由用户决定是否修复。

> 2026-10-25 教训：调试 `mos-web` 登录流程时，用 `admin/admin` 连续试错，
> 把生产库 `mos_prd.mos_user` 的 admin 账号试到 `locked=1 / failures=5`，
> 需要用户手动恢复。**任何账号只允许试一次。**

> 2026-10-25 补充：验证前端时**不要起 mock 服务**。mock 会掩盖真实的请求形态问题 ——
> 本次「上传报 `bucket不存在:default,default`」的根因就是「前端把 `bucketName`
> 同时放进 query 和 body」，mock 若照抄前端写法就复现不出来。


