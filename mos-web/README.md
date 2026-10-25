# mos-web

MOS 对象存储服务的 Web 控制台。

基于 **React 18 + TypeScript + Ant Design 5 + UmiJS 4** 重写自旧版 Vue 2 + ant-design-vue 1.x 实现，
功能与后端接口完全保持一致，同时适配 PC 与移动端浏览器。

---

## 功能

| 模块 | 路径 | 说明 |
| --- | --- | --- |
| 登录 | `/signin` | 用户名密码登录，失败 3 次后自动显示图形验证码；支持「记住我」 |
| 资源管理 | `/resource` | 桶列表（桶的增删改查、秘钥管理、桶授权），点击桶进入桶内 |
| 桶内 · 仪表盘 | `/resource/dashboard?b={bucket}` | 当前桶的今日 / 今月读写请求与流量统计、24 小时 / 30 天趋势图 |
| 桶内 · 资源管理 | `/resource/files?b={bucket}` | 文件浏览、上传（分片 / 秒传 / 断点续传）、新建与重命名文件夹、编辑资源、复制 / 移动、删除、访问链生成、图集 |
| 桶内 · 回收站 | `/resource/trash?b={bucket}` | 彻底删除、还原 |
| 服务器管理 | `/manage/client` | 存储节点状态、容量占用、剔除 / 恢复（仅管理员） |
| 用户管理 | `/manage/user` | 用户增删改查、解锁（仅管理员） |
| 视频查看器 | `/viewer/video` | 同目录视频列表、连续播放 |
| 图库查看器 | `/viewer/gallery` | 目录图片网格、灯箱大图预览 |

> 桶内三个页签通过查询参数 `b` 携带桶名，切换页签时桶不会丢。

---

## 快速开始

### 环境要求

- Node.js >= 18（推荐 20 LTS）
- npm / pnpm / yarn 均可
- 后端 `mos-server` 需运行在 `http://localhost:9700`（默认）

### 安装与启动

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:8000）
npm run dev

# 生产构建，产物在 dist/
npm run build

# 本地预览构建产物
npm run preview
```

开发服务器会把 `/member`、`/admin`、`/open`、`/login`、`/upload`、`/mos` 等路径代理到后端。
后端地址默认取 `config/config.ts` 中的 `API_TARGET`，也可用环境变量覆盖：

```bash
MOS_API_TARGET=http://192.168.1.100:9700 npm run dev
```

> 注意：代理的路径匹配是**精确到路径边界**的。`http-proxy-middleware` 的字符串 context
> 是前缀匹配，`/s` 会误伤 `/signin`、`/static` 等，所以这里改用函数 context。
>
> 另外后端未登录时会返回 `302 -> /signin`，且 `Location` 指向后端自身。
> 开发服务器会把该 `Location` 改写为前端地址，避免浏览器跳离 SPA 落到后端的静态页。

### 类型检查与代码检查

```bash
npm run typecheck   # tsc --noEmit
npm run lint        # eslint
```

---

## 目录结构

```
src/
├── app.tsx                  # Umi 运行时配置（初始状态、布局、请求）
├── global.css               # 全局样式（含 PC / 移动端适配）
├── components/
│   ├── EChart.tsx           # ECharts 折线图封装
│   ├── FileIcon.tsx         # 文件类型图标
│   └── Thumb.tsx            # 缩略图
├── hooks/
│   ├── useResponsive.ts     # 响应式断点
│   └── usePerms.ts          # 桶权限判断
├── layouts/
│   ├── BasicLayout.tsx      # 响应式主布局（PC 侧边栏 / 移动端抽屉）
│   ├── HeaderActions.tsx    # 顶部用户菜单
│   ├── ProfileModal.tsx     # 个人资料
│   └── menuConfig.ts        # 菜单定义
├── models/
│   └── account.ts           # 当前用户、桶、权限
├── pages/
│   ├── Login/               # 登录
│   ├── Resource/            # 资源管理（桶内）
│   │   ├── BucketList.tsx   # 桶列表（桶管理）
│   │   ├── Dashboard.tsx    # 桶内仪表盘
│   │   ├── FileList.tsx     # 桶内资源（文件）列表
│   │   ├── Trash.tsx        # 桶内回收站
│   │   └── components/      # 桶内导航条、上传、文件夹、资源编辑、访问链、复制移动
│   ├── User/                # 用户管理
│   ├── Client/              # 服务器管理
│   ├── Viewer/              # 视频 / 图库查看器
│   └── Exception/           # 403 / 404
├── services/
│   ├── api.ts               # 后端接口封装
│   ├── request.ts           # axios 实例与拦截器
│   └── types.ts             # 接口类型定义
└── utils/
    ├── index.ts             # 通用工具
    └── upload.ts            # 分片与 MD5 计算
```

---

## 与后端的契约

前端严格对齐 `mos-server` 的实际接口实现：

- **响应封装**：绝大多数接口返回 `{ status, message, result }`，`status !== 'ok'` 即业务错误。
  部分接口返回裸 JSON（`/member/resource/file/suffix`、`/member/resource/{b}/file/{t}/{id}`、
  `/member/resource/{b}/files/{t}`、`/member/dir/{b}/detailInfo/{id}`），已在 `services/api.ts` 中分别用
  `get` / `getRaw` 处理。
- **登录**：地址为 `POST /login`（服务端 `loginProcessingUrl`），使用
  `application/x-www-form-urlencoded`；`GET /kaptcha/check/{username}` 判断是否需要验证码。
- **凭证**：所有请求携带 Cookie（`withCredentials: true`），服务端基于 Session 鉴权。
- **会话失效**：服务端未登录时返回 `302 -> /signin`，且 `Location` 指向**后端自身**。
  浏览器会自动跟随该重定向，因此前端不能靠 `status === 302` 判断 ——
  实际拿到的是登录页 HTML（或跳转目标不存在的 404）。
  前端改为按「本该返回 JSON 却返回了 HTML」来判定会话失效，并自行跳转 `/signin`。
  开发服务器同时会把 302 的 `Location` 改写为前端地址，避免浏览器跳离 SPA。
- **上传**：
  1. `POST /member/resource/{bucket}/checkFile/isExists` 重名检查
  2. `POST /upload/{bucket}/init` 初始化（`fileExists` 为 true 即秒传完成）
  3. `POST /upload/{bucket}` 逐片上传，跳过 `existedChunkIndexs` 中已存在的分片
  4. `POST /upload/mergeFiles?wait=true` 合并

  注意 `isPublic` / `cover` / `updateMd5` / `wait` 在服务端是 `@RequestParam`，
  **必须放在 query string**，放在 multipart 表单里不会生效。
- **权限**：`GET /member/bucket/grant/perms/own` 返回当前用户在各桶上的权限
  （`SELECT` / `INSERT` / `UPDATE` / `DELETE`），桶所有者拥有全部权限。
  按钮的可用状态据此控制。
- **短链**：`expireSeconds` 必须 >= 60，服务端会拒绝更小的值。

### 登录后跳转循环的成因与修复

旧实现登录成功后只写 `localStorage`，而 Umi 的 `initialState` 仅在应用启动时读取一次，
导致主布局守卫认为「未登录」而跳回 `/signin`，登录页又跳回 `/home`，形成死循环。

修复要点：

1. 登录成功后 `await setInitialState(s => ({ ...s, currentUser: user }))`，同步全局状态。
2. 主布局读取用户态时用 `initialState?.currentUser ?? readCurrentUser()` 兜底。
3. 会话失效判定改为「期望 JSON 却收到 HTML」，不再依赖 `status === 302`。


---

## PC 与移动端适配

- **断点**：`< 768px` 抽屉式菜单、表格紧凑模式、单列栅格；
  `768px ~ 991px` 平板布局；`>= 992px` 固定侧边栏 + 双列图表。
- 使用 `src/hooks/useResponsive.ts` 统一判断，避免各处散落的媒体查询。
- 表格在窄屏下保留横向滚动，不隐藏列。

---

## 与旧版（Vue）的差异

功能点一一对应，主要变化：

- 路由与状态管理改为 UmiJS 约定式路由 + `useModel`。
- 组件库升级到 Ant Design 5，表单改为受控 `Form.Item name`。
- 图片 / 图库预览改用 `viewerjs` 原生封装。
- 视频播放器改用 `dplayer` 原生封装。
- 图表改用 ECharts 按需引入（`echarts/core`）。
- 权限缓存从全局变量改为 React Context + Hook。
- 修复了旧版若干问题：复制后未刷新表格、目录查询参数索引错位、
  `getReadableSize` 格式化错误等。
