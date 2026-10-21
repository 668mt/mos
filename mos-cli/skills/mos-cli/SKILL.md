---
name: mos-cli
description: |
  mos 对象存储命令行工具（TypeScript CLI）。用于在终端上传、下载、列举、删除、重命名文件，以及管理 mos 配置和生成访问链接。
  支持分片并发上传、秒传、断点续传、递归目录上传下载。当用户提到「mos」「mos-cli」「上传文件到 mos」「从 mos 下载」
  「对象存储」「上传下载文件」「配置 mos」「生成文件链接」时使用本 skill。
---

# mos CLI

`mos` 是 MOS 对象存储服务的命令行工具，把服务端开放接口（上传、下载、资源管理）压成几条命令。

## 何时使用

- 用户要在终端把文件/目录上传到 mos
- 用户要从 mos 下载文件或整个目录
- 用户要查看 mos 上的文件列表、文件信息（md5/大小/类型）
- 用户要删除或重命名 mos 上的文件
- 用户要生成 mos 文件的访问链接（含短链接）
- 用户要配置 mos 的服务地址、桶名、秘钥

## 前置条件

需要先配置好连接信息，否则任何命令都会报「配置不完整」：

```bash
mos config set --host http://localhost:9700 --bucket default --secret-key <秘钥>
```

支持逐项设置，只更新传入的字段：

```bash
mos config set --host http://localhost:9700     # 只改 host
mos config set --bucket default                 # 只改 bucket
mos config set --secret-key <秘钥>               # 只改秘钥
```

配置写入 `~/.mos/config.json`。也可用环境变量 `MOS_HOST` / `MOS_BUCKET` / `MOS_SECRET_KEY` / `MOS_OPEN_ID`。

优先级：**命令行参数 > 环境变量 > 配置文件**。

### 多地址（hosts）

`host` 可配多个地址，逗号分隔；**协议可省略，默认 `http://`**；可用 `(N)` 后缀指定权重：

```bash
mos config set --host "10.0.0.1:4100,10.0.0.2:4100"
mos config set --host "192.168.0.174:4100(50),192.168.0.2:4100(0)"
```

多地址会自动做健康检查与故障转移：请求失败自动换下一个地址重试，不可用的地址不参与选择。

### 多套配置（profile）

`--profile` 默认是 `default`，所有数据命令都支持：

```bash
mos config set --profile dev --host http://dev:9700 --bucket dev-bucket --secret-key <key>
mos config profiles                    # 列出所有档案（* 为默认）
mos config use dev                     # 设为默认档案
mos ls / --profile dev                  # 用 dev 档案执行
mos --profile dev ls /                 # 前置写法同样支持
```

## 常用命令

### 配置

```bash
mos config set --host <地址> --bucket <桶名> --secret-key <秘钥>   # 保存配置
mos config show                                                  # 查看当前生效配置
mos config profiles                                              # 列出所有配置档案
```

### 上传

```bash
mos upload ./a.txt                          # 单文件，远端路径默认取文件名
mos upload ./a.txt -r /docs/a.txt           # 指定远端路径
mos upload ./a.txt -p 10 --cover            # 10 并发 + 覆盖同名文件
mos upload ./dist -r /dist                  # 上传整个目录（递归，保持结构）
cat a.txt | mos upload --stdin -r /x.txt    # 管道上传
mos upload ./big.bin --force                # 强制重传，跳过秒传
```

上传会自动：算整文件 MD5 → 询问服务端是否可**秒传** → 不可则**分片并发上传**（默认 50 片，5–20MB/片）→ 已存在的分片自动跳过（**断点续传**）→ 合并。

### 下载

```bash
mos download /docs/a.txt                    # 下载到当前目录
mos download /docs/a.txt -o ./out/a.txt     # 指定文件路径
mos download /docs/a.txt -o ./out/          # 指定目录
mos download /docs/a.txt --cover            # 覆盖本地已有文件
mos download /docs/big.bin --limit-speed 512  # 限速 512 KB/s
mos download /docs/a.txt --stdout | less    # 输出到标准输出
mos download /docs -r -o ./docs             # 递归下载整个目录
```

下载先写临时文件、成功后原子替换，并还原远端 `lastModified`，不会留下半截文件。

### 查询与管理

```bash
mos ls /                    # 列出目录
mos ls /docs -k 关键字       # 按关键字过滤
mos info /docs/a.txt        # 查看 md5 / 大小 / contentType / 修改时间
mos exists /docs/a.txt      # 存在返回 0，不存在返回 1（适合脚本判断）
mos rm /docs/a.txt --yes    # 删除文件
mos rm /docs/ -r --yes      # 递归删除目录
mos mv /docs/a.txt /docs/b.txt   # 重命名 / 移动
```

### 生成访问链接

```bash
mos url /docs/a.txt                              # 2 小时有效的直链
mos url /docs/a.txt --expire 3600                # 自定义有效期（-1 为永久）
mos url /docs/a.txt --thumb                       # 缩略图链接
mos url /docs/a.txt --short --expire 3600         # 短链接
```

## 注意

- **Git Bash / MSYS** 会把以 `/` 开头的远端路径参数改写成 Windows 路径（`/a.txt` → `C:/Program Files/Git/a.txt`）。在 Git Bash 下传绝对路径请加 `MSYS_NO_PATHCONV=1`，或用相对路径。PowerShell / CMD 无此问题。
- `rm` 交互式会要求确认，非交互终端下需显式加 `--yes`。
- 全局选项可加在任意子命令前：`--host`、`--bucket`、`--secret-key`、`--open-id`、`--profile`。

## 安装本 skill

```bash
mos install            # 安装到 ~/.agents/skills/mos-cli（已存在则覆盖）
mos install --path D:/somewhere/skills   # 安装到自定义目录
```
