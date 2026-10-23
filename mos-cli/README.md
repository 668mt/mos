<div align="center">

# mos-cli

**MOS 对象存储命令行工具**

[![npm](https://img.shields.io/badge/npm-@668mt/mos--cli-cb3837?logo=npm&logoColor=white)](https://www.npmjs.com/package/@668mt/mos-cli)
[![Node](https://img.shields.io/badge/node-%3E%3D18-green?logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[English](#) | 简体中文

</div>

---

用 TypeScript 实现服务端 `controller/open` 下的开放接口，核心是**上传**与**下载**。命令名为 `mos`。

签名算法与 URL 编码均与 Java `mos-sdk` **字节级一致**，可直接使用同一套 `secretKey` / `openId`。

## 功能

- **上传**：分片并发、秒传、断点续传、递归目录、stdin 管道
- **下载**：流式下载、原子替换、还原 mtime、限速、递归目录
- **管理**：`ls` / `info` / `rm` / `mv` / `exists` / `url`（含短链接）
- **多地址**：逗号分隔多个服务地址，权重选择 + 健康检查 + 故障转移
- **多档案**：`--profile` 切换不同环境的连接信息
- **AI 友好**：自带 skill，`mos install` 一键安装到 `~/.agents/skills/`


## 安装

```bash
# 全局安装（推荐）
npm install -g @668mt/mos-cli

# 或直接用 npx
npx @668mt/mos-cli --help
```

需要 Node.js >= 18。

从源码开发：

```bash
cd mos-cli
npm install
npm run build
npm link        # 可选：把 mos 注册到全局
```

### 配置

```bash
mos config set --host http://localhost:9700 --bucket default --secret-key <秘钥>
```

配置默认写入 `~/.mos/config.json`。

支持**逐项设置**，只更新传入的字段，其余保持不变：

```bash
mos config set --host http://localhost:9700     # 只改 host
mos config set --bucket default                 # 只改 bucket
mos config set --secret-key <秘钥>               # 只改秘钥
```

### 多地址（hosts）

`host` 可以配多个地址，用逗号分隔。**协议可以省略，默认按 `http://` 处理**：

```bash
mos config set --host "10.0.0.1:4100,10.0.0.2:4100"
mos config set --host "http://a.com, https://b.com"
```

每个地址还可以带**权重**后缀 `(N)`，缺省 100，权重越高被选中的概率越大：

```bash
mos config set --host "192.168.0.174:4100(50),192.168.0.2:4100(0)"
```

多地址时的行为：

- **故障转移**：请求失败会自动换下一个地址重试；上传、下载等操作会依次尝试，直到成功。
- **健康检查**：按 `/index.html` 探测可用性（结果缓存 10 秒），不可用的地址不参与选择。
- **优先复用**：同一进程内会优先使用上次成功的地址，避免每次都重新探测。
- 全部地址都失败时，会逐个列出每个地址的失败原因。

也可以用环境变量 `MOS_HOST` 传多个地址（逗号分隔）。

也可以完全用环境变量，不落盘：

| 环境变量 | 含义 |
| --- | --- |
| `MOS_HOST` | 服务地址（多个用逗号分隔） |
| `MOS_BUCKET` | 桶名 |
| `MOS_SECRET_KEY` | 秘钥 |
| `MOS_OPEN_ID` | 开放平台 id（`<=0` 表示管理员） |
| `MOS_PROFILE` | 配置档案名 |
| `MOS_CONFIG` | 配置文件路径 |

优先级：**命令行参数 > 环境变量 > 配置文件**。

### 多套配置（profile）

用 `--profile` 管理多套连接信息，不传时默认使用 `default`：

```bash
mos config set --profile dev --host http://dev:9700 --bucket dev-bucket --secret-key <key>
mos config set --profile dev --bucket other-bucket   # 只改 dev 档案的 bucket
mos config profiles                                  # 列出所有档案（* 为默认）
mos config use dev                                   # 把 dev 设为默认档案
mos config unset bucket --profile dev                # 清空某一项
```

所有数据命令都支持 `--profile`，前置后置写法都可以：

```bash
mos ls / --profile dev
mos --profile dev ls /
```

查看当前生效的配置：

```bash
mos config show
mos config show --profile dev
```

## 命令一览

| 命令 | 说明 |
| --- | --- |
| `mos config set/show/unset/use/profiles` | 配置管理 |
| `mos install` | 安装 mos-cli 的 skill 到 `~/.agents/skills/` |
| `mos upload <path>` | 上传文件或目录 |
| `mos download <pathname>` | 下载文件或目录 |
| `mos ls [pathname]` | 列出目录内容 |
| `mos info <pathname>` | 查看文件信息 |
| `mos rm <pathname>` | 删除文件或目录 |
| `mos mv <src> <dst>` | 重命名 / 移动 |
| `mos url <pathname>` | 生成访问链接（含短链接） |
| `mos exists <pathname>` | 判断是否存在 |

## 安装 skill

`mos-cli` 自带一个描述其用法的 skill，安装后 AI 助手即可自动学会 `mos` 命令，无需每次手写说明。

```bash
mos install                     # 安装到 ~/.agents/skills/mos-cli
mos install --path D:/my/skills # 安装到自定义目录
```

默认即覆盖安装：目标已存在时会直接更新（并清理旧版本的残留文件）。

实现要点：

- 目标目录默认 `~/.agents/skills/mos-cli`，也可用 `--path` 或环境变量 `AGENTS_SKILLS_PATH` 覆盖。
- 先装到临时目录并校验 `SKILL.md` 合法，再原子切换，中途失败不会留下坏目录。
- 不触碰 `~/.agents/skills/.skills_store_lock.json`（该文件属于 Skillhub 官方安装器）。

## 上传

```bash
# 单文件（默认以文件名作为远端路径）
mos upload ./a.txt

# 指定远端路径、并发数、覆盖
mos upload ./a.txt -r /docs/a.txt -p 10 --cover

# 上传整个目录（递归，保持目录结构）
mos upload ./dist -r /dist

# 管道上传
cat a.txt | mos upload --stdin -r /from-stdin.txt

# 强制重新上传，跳过秒传
mos upload ./big.bin --force
```

要点：

- **秒传**：先用整文件 MD5 + 大小询问服务端，命中则直接完成，不产生流量。
- **断点续传**：服务端返回已上传的分片索引，只补传缺失分片。
- **分片策略**与 Java SDK 一致：`partSize = ceil(totalSize / 50)`，再 clamp 到 `[5MB, 20MB]`。
- 自动按扩展名推断 `contentType`，可用 `--content-type` 覆盖。

## 下载

```bash
# 下载到当前目录
mos download /docs/a.txt

# 指定保存位置（文件或目录都可以）
mos download /docs/a.txt -o ./out/a.txt
mos download /docs/a.txt -o ./out/        # 视为目录

# 覆盖已存在文件
mos download /docs/a.txt -o ./a.txt --cover

# 限速 512 KB/s
mos download /docs/big.bin --limit-speed 512

# 输出到 stdout，便于管道
mos download /docs/a.txt --stdout | less

# 递归下载整个目录
mos download /docs -r -o ./docs
```

要点：

- 先写 `*.mos-tmp`，成功后原子替换，避免中断留下半截文件。
- 下载完成后还原远端的 `lastModified`，本地时间戳保持一致。

## 其它

```bash
mos ls /                      # 列出目录
mos ls /docs -k 关键字        # 按关键字过滤
mos info /docs/a.txt          # 查看 md5 / 大小 / contentType
mos rm /docs/a.txt            # 删除文件（交互确认）
mos rm /docs/a.txt --yes      # 跳过确认
mos rm /docs/ -r --yes        # 递归删除目录
mos url /docs/a.txt           # 生成 2 小时有效的链接
mos url /docs/a.txt --render --thumb            # 渲染 / 缩略图链接

# 短链接：访问 /s/<短码> 会 302 跳到带签名的直链
mos url /docs/a.txt --short --expire 3600                        # 基本用法
mos url /docs/a.txt --short --expire 3600 --domain https://cdn.x.com  # 指定对外域名
mos url /docs/a.txt --short --expire 3600 --render --thumb        # 携带查询参数

mos exists /docs/a.txt        # 存在返回 0，不存在返回 1
```

短链接的有效期必须 **≥ 60 秒**（与服务端一致），不支持永久。多地址配置下短链使用实际选中的那个地址；面向外部用户时建议用 `--domain` 指定对外域名。

## 与 Java SDK 的兼容性

服务端校验签名用的是 `mt.spring.mos.sdk.utils.MosEncrypt`，其 AES 密钥派生方式是：

```java
SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
random.setSeed(key.getBytes());
kgen.init(128, random);
```

这里的 `SHA1PRNG` 在现代 JDK（JDK9+）中实际映射到 `sun.security.provider.SecureRandom`，**并不是**简单的「key 做一次 SHA-1」，而是一个带进位加法的 SHA-1 计数器型生成器。`src/core/javaAes.ts` 完整复刻了它的 `engineSetSeed` / `engineNextBytes` / `updateState`，以保证签名能被服务端接受。

签名串结构同样与 Java 一致：

```
rChar(1) + s1(random) + lengthChar(1) + openIdEncoded + s2
```

### 验证

**1. 字节级对齐 Java 实现**

`npm test` 中的 `src/test/crypto.test.ts` 用 Java 生成的基准数据（`ref.txt`，36 组）反向校验：

- AES 加密结果与 Java **逐字节一致**
- Java 生成的签名可被正确解析
- 随机切分点不落在非法区间 `(25, 32)`
- Unicode / 特殊字符路径可往返

**2. 双向交叉验证（需要本机 JDK）**

```bash
bash scripts/verify-java-compat.sh
```

该脚本会用 mos-cli 生成签名，再调用**真实的 Java SDK**（`MosEncrypt.decrypt`）解析，覆盖中文路径、空格、`|` 等特殊字符：

```
==> 用真实 Java SDK 校验 mos-cli 生成的签名
  ✔ [1] /a.txt
  ✔ [2] /dir/sub/file name.png
  ✔ [3] /中文/文件.png
  ...
全部通过：7/7 个签名均可被 Java SDK 正确解析
```

脚本依赖 `mos-base` / `mos-sdk` 已编译：

```bash
cd .. && (cd mos-base && mvn -q -DskipTests compile) && (cd mos-sdk && mvn -q -DskipTests compile)
```

脚本会自动探测本地 Maven 仓库（`~/.m2/repository`、`D:/Maven/repository` 等）。若在自定义位置，用环境变量指定：

```bash
M2_REPO=/path/to/repository bash scripts/verify-java-compat.sh
```

> 脚本兼容 Windows（Git Bash）、Linux、macOS。Git Bash 下需 JDK 已在 `PATH` 中。

## 测试

```bash
npm test
```

覆盖以下方面：

- `crypto.test.ts` — 与 Java 的字节级兼容
- `util.test.ts` — 路径规范化、MD5、并发控制
- `config.test.ts` — profile 隔离、参数优先级、`openId 0` 边界
- `hosts.test.ts` — 多地址解析、协议默认、权重、健康检查
- `install.test.ts` — SKILL.md frontmatter 解析（含 `|` 块标量、CRLF）与安装流程
- `integration.test.ts` — 起一个模拟服务端（`mockServer.ts`），跑通上传 / 秒传 / 断点续传 / 多分片 / 下载 / 目录递归 / 删除 / 重命名 / 签名拒绝等全流程

## 结构

```
src/
├── index.ts              # 命令行入口
├── core/
│   ├── javaAes.ts        # Java 兼容的 AES + SHA1PRNG 密钥派生
│   ├── sign.ts           # MosEncrypt 签名生成 / 解析
│   ├── hosts.ts          # 多地址解析、权重选择、健康检查、故障转移
│   ├── client.ts         # 开放接口 HTTP 客户端（含 ResResult 信封解析）
│   ├── config.ts         # 配置加载与优先级
│   ├── upload.ts         # 分片、秒传、断点续传
│   └── download.ts       # 流式下载、临时文件原子替换、限速
├── commands/             # 各子命令
├── skills/mos-cli/       # 随包分发的 skill（mos install 会安装它）
└── test/                 # 测试与模拟服务端
```

## 注意事项

- **Git Bash / MSYS** 会把以 `/` 开头的参数自动改写成 Windows 路径（`/a.txt` → `C:/Program Files/Git/a.txt`）。若在 Git Bash 下传绝对远端路径，请加 `MSYS_NO_PATHCONV=1`，或改用相对路径。PowerShell / CMD 无此问题。
- HTTPS 使用自签名或内网证书时不做证书校验，与 Java SDK 行为一致。

### 服务端的已知限制

以下是实测中服务端侧的既有行为，CLI 已做相应处理：

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 同名文件上传报「已存在」 | 服务端在秒传判定**之前**就检查重名 | 需显式加 `--cover` 允许覆盖 |
| `--force` 无法真正重传 | 服务端按 `md5 + size` 去重，跳过秒传后会报 `uploadFile不存在` | `--force` 仅跳过秒传提示，不保证重传 |
| 根目录带关键字过滤返回空 | 服务端 `ResourceMapper.xml` 中根目录查询条件为 `and 1 = 2` | 改用子目录查询，或不带 `-k` |
| 短链接有效期必须 ≥ 60 秒 | 服务端校验 | CLI 已在参数层校验 |

## 相关

- [MOS 主仓库](https://github.com/668mt/mos/tree/3.0) —— Java 实现的分布式对象存储服务
- [发布指南](PUBLISH.md) —— 如何发新版本到 npm

