# AGENTS.md — mos-web

> MOS 对象存储服务的 Web 控制台。**React 18 + TypeScript + Ant Design 5 + UmiJS 4**。
> 本文件是给 AI 智能体（及新人）看的开发指南，只记录**约定、坑和契约**，细节以代码为准。
> 概览性介绍见同目录 `README.md`。

---

## 语言

回复必须使用中文来回答

## 1. 定位与边界

- `mos-web` 是**纯前端 SPA**，对应后端 `mos-server`（Spring Boot 3 / JDK 17）。
- **不属于 Maven 构建**。父仓库的 `mvn compile` 完全不碰这个目录；根 `AGENTS.md` 说的「按模块编译」只适用于 Java 模块。
- 改前端 → 只在 `mos-web/` 内跑 npm 脚本；改后端 → 跑对应 Maven 模块。**两者互不影响**。
- 前端**只通过 HTTP 接口**访问后端，没有直连数据库、没有服务端渲染。

---

## 2. 命令速查

```bash
cd mos-web

npm install          # 装依赖（会触发 postinstall -> max setup）
npm run dev          # 开发服务器，http://localhost:8000
npm run build        # 生产构建 -> dist/
npm run preview      # 本地预览 dist/

npm run typecheck    # tsc --noEmit，改完代码至少跑这个
npm run lint         # eslint src --ext .ts,.tsx --max-warnings 0
```

> **没有单元测试脚本**（`npm test` 不存在）。别去找 jest/vitest 配置。
> 验证手段只有 `typecheck` + `lint` + 手动点页面。

> ⚠️ `npm run lint` 当前**有存量报错**（约 12 errors / 10 warnings），主要是
> `@typescript-eslint/no-use-before-define`（函数声明顺序）、
> `no-redeclare`（`Progress` 命名冲突）、`no-unused-vars`（`Space`、`parentDirs`、
> `uploadFileList` 等未使用变量）、`guard-for-in`。
> 这些是**历史遗留**，不是你的锅。**改动时不要顺手大改**，但**自己新增的代码不要引入新报错**。
> 判断自己有没有引入新问题：用 `npm run lint` 的输出对照你改过的文件。

> 📌 2026-10-25 修复：`.eslintrc.js` 原本写的是**裸 JSON 对象字面量**（没有 `module.exports =` 包装），
> ESLint 按 CommonJS 加载时把它当块语句解析，直接报 `SyntaxError: Unexpected token ':'`，
> **导致 lint 完全跑不起来**。已改为 `module.exports = { ... }`。
> 改 ESLint 配置时务必保持 CommonJS 导出形式。

### 后端地址

```bash
MOS_API_TARGET=http://192.168.1.100:9700 npm run dev
```

- 默认值写在 `config/config.ts` 的 `API_TARGET`（当前是 `http://localhost:4100`）。
- 生产环境前端是**同源部署**，直接用相对路径 `/member/...`；如需分离部署，设 `window.__MOS_API_BASE__`。

---

## 3. 目录职责

```
src/
├── app.tsx            # Umi 运行时配置：getInitialState / rootContainer / 主题 / 中文语言包
├── global.css         # 全局样式，所有 class 以 .mos- 前缀
├── typings.d.ts
├── components/        # 跨页面复用的小组件（EChart / FileIcon / Thumb）
├── hooks/             # useResponsive（断点）、usePerms（桶权限）、useDocumentTitle（浏览器标题）
├── layouts/           # BasicLayout（PC 侧栏/移动抽屉）、menuConfig、HeaderActions、ProfileModal
├── models/            # account.ts —— 当前用户的 localStorage 读写（其余字段是死代码，见 8）
├── pages/             # 按路由分目录，每个页面一个 index.tsx + components/ 子目录
├── services/          # api.ts（接口封装）、request.ts（axios 实例 + 拦截器）、types.ts（类型）
└── utils/             # index.ts（通用工具）、upload.ts（分片 + MD5）、antdSafe.ts（antd 全局兜底）
```

**路径别名**：`@/*` → `src/*`（见 `tsconfig.json`）。统一用 `@/` 导入，不要写 `../../..`。

---

## 4. 硬性约定

### 4.1 新增接口的流程

1. 在 `services/types.ts` 加**类型**（接口名与后端 DTO/VO 对齐）。
2. 在 `services/api.ts` 加**函数**，只做「拼 URL + 定参数位置 + 定返回类型」，不含业务逻辑。
3. 页面里 `useCallback` 包一层调 `api.ts` 的函数。

**禁止**在页面组件里直接 `axios.get` / `fetch`。

### 4.2 `get` vs `getRaw`

`services/request.ts` 提供两套：

| 函数 | 返回 | 何时用 |
|---|---|---|
| `get/post/put/del` | 已解包的 `result` 字段 | **绝大多数**接口，返回 `{status, message, result}` |
| `getRaw` | 完整响应体 | 少数返回**裸 JSON** 的接口 |

裸 JSON 接口（务必用 `getRaw`）：
`/member/resource/file/suffix`、`/member/resource/{b}/file/{t}/{id}`、
`/member/resource/{b}/files/{t}`、`/member/dir/{b}/detailInfo/{id}`。

### 4.3 错误提示

**不要在页面里重复弹错误提示**。`request.ts` 的响应拦截器已经统一 `notification.error` 了，页面里只需：

```ts
try {
  await someApi();
} catch {
  // 错误已由拦截器处理
}
```

只有**业务语义**上的提示（如「请先选择文件」）才在页面里 `message.warning`。

#### 4.3.0 别让异常逃逸成 Unhandled Rejection

antd 的 `Modal.onOk` / `Modal.confirm.onOk` / `Input.onPressEnter`
**都不会接管回调返回的 Promise**：

- `Modal.confirm` 更狠 —— `ActionButton` 会把 `onOk` 的 rejection **再抛一次**
  （`antd/es/_util/ActionButton.js` 的 `handlePromiseOnOk`），这个新 promise 无人接管；
- 于是浏览器判定为 **Unhandled Rejection**，umi dev-server 的错误浮层
  （`@umijs/bundler-webpack` 的 `startReportingRuntimeErrors`）会弹出**盖住整页**的浮层，
  把拦截器已经弹出的错误提示一起遮住。

> ⚠️ react-error-overlay 的处理函数**不检查 `event.defaultPrevented`**，
> 所以靠 `window.addEventListener('unhandledrejection')` 是**压不过**它的，
> 只能在 antd 的 `onOk` 边界上把异常吃掉。

本仓库的做法（**两处，不要各写各的**）：

| 场景 | 做法 |
|---|---|
| `Modal.confirm` / `Modal.info` 等**静态方法** | `utils/antdSafe.ts` 在启动时打了全局补丁，**调用点不用改** |
| 普通 `<Modal onOk={...}>` / `onPressEnter={...}` | 用 `utils` 里的 `safeAsync(...)` 包一层 |

```tsx
<Modal onOk={safeAsync(handleOk)} ... />
<Input onPressEnter={safeAsync(handleOk)} />
```

`safeAsync` 的行为：
- 后端业务错误（拦截器已 `notification.error` 提示过）→ 静默吞掉；
- antd 表单校验失败（`validateFields` 抛出 `errorFields`）→ 静默吞掉，antd 已在字段下方标红；
- 其余异常（代码缺陷）→ `console.error` 输出到控制台，便于排查。

> 「已提示」标记由 `utils` 的 `markNotified` / `isNotified` 维护，
> 拦截器 `request.ts` 弹完提示后会打标记，`safeAsync` 据此判断要不要打印日志。

### 4.3.1 删除必须二次确认

**所有破坏性操作（删除类）都必须先弹确认框**，不能点一下就直接删。

已覆盖的入口（都走 `Modal.confirm`）：

| 位置 | 动作 | 确认文案要点 |
|---|---|---|
| `pages/Resource` | 删除文件 / 文件夹 | 显示完整路径；文件夹提示「内容会一并移入回收站」 |
| `pages/Resource` | 批量删除 | 显示所有路径 |
| `pages/Trash` | 彻底删除 | 显示路径 + **不可恢复**警告；按钮文案「彻底删除」 |
| `pages/Trash` | 批量彻底删除 / 还原 | 同上 |
| `pages/Bucket` | 删除桶 | 显示桶名 + 「桶内文件一并删除、不可恢复」 |
| `pages/Bucket` | 删除秘钥 | 显示用途 + 「访问将失效」 |
| `pages/User` | 删除用户 | 显示姓名/用户名 + 「无法登录、授权会被清除」 |

写法约定（**直接用 antd 的静态 `Modal.confirm`**，`onOk` 的异常已全局兜底，见 4.3.0）：

```tsx
import { Modal } from 'antd';

Modal.confirm({
  title: '确定删除这个文件吗?',
  content: (<div>...</div>),
  okText: '确定',
  okType: 'danger',      // 危险操作必须
  cancelText: '取消',
  onOk: async () => {
    await doDelete();
    message.success('删除成功');
    reload();
  },
});
```

- `okType: 'danger'` 必填。
- `content` 里要**说清删的是什么、删了能不能恢复**，不要只写「确定吗?」。
- 确认框里的删除逻辑放进 `onOk`，**不要**在 `onClick` 里直接调删除接口。
- 窄屏「更多」下拉里的删除项也要走同一个确认框。
- **`Modal.confirm` 的 `onOk` 里不用自己 try/catch**：`utils/antdSafe.ts` 在启动时
  给 antd 的静态确认框方法打了全局补丁，`onOk` 的异常会被统一吞掉，
  详见 4.3.0 与 `src/utils/antdSafe.ts` 的注释。

### 4.4 antd 5 注意事项

- **确认框统一用 antd5 的静态 `Modal.confirm`**（见 4.3.1），保持仓库现有写法一致。
  它的 `onOk` 异常已由 `utils/antdSafe.ts` 全局兜底（见 4.3.0），**不要**改成
  `App.useApp()` 的 context 版本 —— 仓库现有写法统一是静态的，保持一致。
- antd5 的静态 `Modal.confirm` 拿不到 context（主题/locale），这是**已知取舍**。
- 表单用**受控 `Form.Item name`**，不要手写 `value`/`onChange`。
- `destroyOnHidden`（不是 `destroyOnClose`，antd5 改名了）。
- 主题色 / 圆角在 `src/app.tsx` 的 `token` 里统一改，别在各处写死颜色。

### 4.5 权限控制

- 桶级权限（`SELECT` / `INSERT` / `UPDATE` / `DELETE`）用 `usePerms()` 的 `hasPerm(bucketName, perm)` 判断，按钮用 `disabled` 控制。
- 桶授权改动后调用 `usePerms()` 返回的 `refresh()`（`pages/Bucket/index.tsx` 有现成用法）。
- 管理员专属页面（服务器管理、用户管理）在 `layouts/menuConfig.tsx` 标 `adminOnly: true`。
- `models/account.ts` 里的 `buckets` / `perms` / `currentBucket` 及 `loadWorkspace` / `refreshPerms`
  **目前是死代码**（无任何页面调用）。权限一律走 `hooks/usePerms.ts`；桶列表各页面自己
  `fetchBuckets()`。改权限逻辑以 `hooks/usePerms.ts` 为准，别去动 model。

### 4.6 响应式

- **统一用 `useResponsive()`**（`src/hooks/useResponsive.ts`），断点与 antd Grid 对齐：`xs 480 / sm 768 / md 992 / lg 1200 / xl 1600`。
  注意本项目 `isMobile` = `xs || sm`（即 **< 768px**），`isTablet` = `md`。
- **不要在组件里写媒体查询**，媒体查询只放在 `global.css` 里，且统一用 `mos-` 前缀类名。
- **表格窄屏用 antd 的 `responsive` 列属性**（取值是 antd 断点 `xs/sm/md/lg/xl/xxl`，语义是「在哪些断点及以上显示」）：
  ```ts
  { title: '访问量', dataIndex: 'visits', width: 90, responsive: ['lg', 'xl', 'xxl'] }
  ```
  规则：**第一列（文件名/名称）和「操作」列永远显示**，次要列按重要性依次从大屏往下放。
  窄屏同时要把 `scroll={{ x: ... }}` 收窄（如 `isMobile ? 360 : 1100`），否则留出大片空白。
- **操作列在窄屏要收进「更多」下拉**（参考 `pages/Resource/index.tsx` 的 `buildRowActions` / `onRowAction`，
  和 `pages/Bucket`、`pages/User` 的写法）：宽屏平铺链接，窄屏 `Dropdown` + `mos-row-more`。
  链接超过 2 个就必须收，否则操作列会挤爆手机屏幕。
- 分页在窄屏要关掉「每页条数」和「总数」：`showSizeChanger: !isMobile`、
  `showTotal: isMobile ? undefined : ...`、`size: isMobile ? 'small' : 'default'`。
- 工具栏在窄屏要让搜索框 / 选择器各占一行：在 `global.css` 里用 `order` + `flex: 1 1 100%`，
  不要在 JSX 里写两套布局。

### 4.7 样式

- 全局样式集中在 `src/global.css`，类名统一 `mos-` 前缀（`.mos-card`、`.mos-toolbar`、`.mos-breadcrumb`…）。
- **不要用 CSS-in-JS / styled-components**，本项目没有这套基建。
- 少量局部布局允许内联 `style={{...}}`（现有代码就是这么写的），但**颜色、圆角、阴影**要走 `global.css` 或 antd token。

### 4.8 注释

- 注释用**中文**，写「**为什么**」而不是「做了什么」。现有代码里凡是出现反直觉行为的，都有一行注释说明原因（见第 5 节）。
- 不要给显而易见的代码加注释。

### 4.9 ESLint 配置

- `.eslintrc.js` 必须是 **CommonJS 导出**：`module.exports = { ... }`。
  写成裸对象字面量（`{ "extends": [...] }`）会让 ESLint 报 `SyntaxError` 直接跑不起来。
- 规则覆盖：`@typescript-eslint/no-explicit-any` 关闭、`no-unused-vars` 降为 warn 且忽略 `_` 前缀参数、`no-console` 关闭。
- **新增代码建议避开** `no-use-before-define`：把函数定义放到调用点**之前**，或改用 `function` 声明提升 / 提到模块顶层。

---

## 5. 后端契约（踩过的坑，改动前必读）

### 5.1 会话失效：不要判断 302

服务端未登录时返回 `302 -> /signin`，且 `Location` 指向**后端自身**。浏览器会自动跟随，前端拿到的可能是登录页 HTML（200）或 404/5xx。

**判定方式**：`request.ts` 里按「**本该返回 JSON 却返回了 HTML**」（`content-type` 含 `text/html`）来判断会话失效，然后跳 `/signin`。
**不要**写 `status === 302` 之类的判断——那永远不成立。

### 5.2 登录跳转循环

登录成功后**必须**同步全局状态，否则主布局认为未登录会跳回 `/signin`：

```ts
writeCurrentUser(user);                          // localStorage
await setInitialState((s) => ({ ...s, currentUser: user }));  // ← 缺这行就会死循环
```

主布局读取用户态时用 `initialState?.currentUser ?? readCurrentUser()` 兜底。

### 5.3 登录接口

- 地址 `POST /login`（服务端 `SecurityConfig.loginProcessingUrl`），**不是** `/signin`。
- 用 `application/x-www-form-urlencoded`（`transformFormData`）。
- 失败 3 次后需要验证码：`GET /kaptcha/check/{username}` 返回是否需要，`GET /kaptcha?t=时间戳` 取图。
- 凭证走 **Cookie / Session**（`withCredentials: true`），前端不存 token。

### 5.4 上传：参数位置有严格要求

服务端 `isPublic` / `cover` / `updateMd5` / `wait` 是 `@RequestParam`，
**必须放 query string**，放进 multipart 表单体里**不生效**（静默失效，最难查）。

上传流程（`api.ts` 的 `initUpload` / `uploadChunk` / `mergeUpload`）：

1. `POST /member/resource/{bucket}/checkFile/isExists` —— 重名检查（**注意不是 `/member/resource/{bucket}/checkFile`**）
2. `POST /upload/{bucket}/init` —— 初始化；返回 `fileExists === true` 即**秒传**完成
3. `POST /upload/{bucket}` —— 逐片上传，跳过 `existedChunkIndexs` 里已有的分片（**断点续传**）
4. `POST /upload/mergeFiles?wait=true` —— 合并

其它约束：

- 服务端按 `md5 + size` 去重；跳过秒传会报 `uploadFile不存在`。
- 同名上传必须开 `--cover`/覆盖开关；重名检查发生在秒传判定**之前**。
- 分片大小：`clamp(ceil(total/100), 2MB, 10MB)`（`utils/upload.ts`）。
- MD5 用 `spark-md5`，分 2MB 读取，避免大文件占内存。

### 5.5 资源列表：`path` 空串 = 所有位置

`GET /member/resource/{bucket}/list` 的 `path`：

- **非空** → 在该路径下搜索（返回真实 `currentDir` + `parentDirs` 面包屑）
- **空串** → **递归搜所有位置**（`currentDir.path = ""`，`parentDirs` 为空）

> ⚠️ 2026-10-25 变更：文件中心的「当前 / 所有」切换器已移除，前端不再传空串，
> 「所有位置」模式目前在 UI 上不可达。服务端该分支**保留未动**。

### 5.6 短链

`expireSeconds` 必须 **≥ 60**，服务端会拒绝更小的值。

### 5.7 根目录 + keyWord 返回空

`ResourceMapper.xml` 的根目录查询条件是 `and 1 = 2`，
所以在根目录（`path=/`）带 `keyWord` 搜索**必然返回空**。这是服务端既有行为，不是前端 bug。

### 5.8 缩略图

由服务端 `ResourceService` 的 `afterCommit` → `ResourceMetaService.calculateMeta` 异步生成，
`ThumbCron` 定时任务已被注释。前端上传后缩略图可能延迟出现，属正常。

### 5.8.1 分片大小 = 100MB

`utils/upload.ts` 的 `PART_SIZE = 100 * 1024 * 1024`（**固定 100MB**，不是按文件大小均分）：

- 文件 < 100MB → 1 个分片
- 220MB → 3 个分片（100 + 100 + 20），最后一个是余数
- 分片数 `chunks` 必须与实际发出的分片个数**严格一致** ——
  服务端 `merge` 会断言 `uploadFile.chunks == 已上传分片数`，对不上就报「还未上传完整」。

**上传相关接口要放宽超时**：全局 axios 超时只有 60s（`request.ts`），
100MB 分片在弱网下必然超时；`mergeFiles` 带 `wait=true` 会阻塞到存储节点合并完成，更久。
`services/api.ts` 的 `initUpload` / `uploadChunk` / `mergeUpload` 都单独传了
`timeout: UPLOAD_TIMEOUT`（30 分钟），**新增上传接口时别忘了**。

服务端限制（都够用）：`mos-server` 的 `spring.servlet.multipart.max-file-size=4GB`；
`mos-client` 是 2GB，且服务端转发分片用的是原始流（`InputStream`），不走 multipart。

### 5.9 同一个参数只能传一次（`bucketName` 重复传参的坑）

**服务端** `OpenUploadController.mergeFiles(String bucketName, ...)` 的 `bucketName` 是
**普通参数**（不是 `@RequestParam`），Spring 绑定时会取 `request.getParameterValues()`：

- 只有 1 个值 → 绑成 `String`
- **有多个值** → 绑成 `String[]`，再由 `ArrayToString` 拼成 `"a,b"`

而 Servlet 会把 **query string 和 body 里的同名参数合并**。
所以只要 `bucketName` 同时出现在 query 和 body 里，服务端拿到的就是 `"default,default"`，
按这个名字查桶必然查不到：

```
bucket不存在:default,default,currentUserId:1
```

**规则：普通参数（`bucketName` / `pathname` / `totalMd5` …）只放一处。**
`mergeUpload` 的约定是 **`bucketName` 放 query，其余放 body**；
`@RequestParam`（`isPublic` / `cover` / `updateMd5` / `wait`）**必须放 query**。

> 2026-10-25 修复：`services/api.ts` 的 `mergeUpload` 之前在 query 和 body 里各放了一次
> `bucketName`，导致上传必然失败。`mos-cli` 的 `client.ts` 只放 query，所以 CLI 一直正常。

**怎么验证（不需要 mock、不需要登录）**：
未登录时请求仍会进 controller，`Assert.notNull(bucket, "bucket不存在:" + bucketName)`
会把服务端绑定出来的桶名原样回显：

```bash
# 正确（只传一次）-> 期望看到 bucket不存在:default
curl -s -X POST "http://localhost:4100/upload/mergeFiles?bucketName=default" \
  -H "Content-Type: application/x-www-form-urlencoded" -d "totalMd5=x&totalSize=1"

# 错误（query + body 各一次）-> 会看到 bucket不存在:default,default
curl -s -X POST "http://localhost:4100/upload/mergeFiles?bucketName=default" \
  -H "Content-Type: application/x-www-form-urlencoded" -d "bucketName=default&totalMd5=x&totalSize=1"
```

---

## 6. 开发代理的坑（`config/config.ts`）

- `http-proxy-middleware` 的**字符串 context 是前缀匹配**：`'/s'` 会误伤 `/signin`、`/static`。
  这里用**函数 context**（`isProxyPath`）精确匹配路径边界。改代理路径时别退化回字符串。
- 代理会**改写 302 的 `Location`**（`/signin` → 前端 `/signin`），否则浏览器会跳到后端的静态页，SPA 直接白屏。
- 代理路径白名单：`/member` `/admin` `/open` `/login` `/mos` `/upload` `/kaptcha` `/render` `/gallary` `/s` `/health`。
  **新增后端接口前缀时，忘了加代理 = 本地开发 404**。

---

## 7. 路由

- 路由定义在 `config/routes.ts`（Umi 约定式）。
- 页面标题在 `layouts/BasicLayout.tsx` 的 `getPageTitle()` 里**手工维护**一份映射 —— **新增页面要同时改路由和这里**。
- 菜单在 `layouts/menuConfig.tsx`，用 `useSelectedKeys`（最长路径匹配）算高亮。
- 免布局页（登录、查看器、403/404）设 `layout: false`。

---

## 8. 状态管理

- 全局状态只有**当前用户**（`currentUser`），走 Umi 的 `initialState`：
  - 读：`useModel('@@initialState')`
  - 写：`useModel('@@initialState')` 的 `setInitialState`
  - localStorage 读写用 `models/account.ts` 的 `readCurrentUser()` / `writeCurrentUser()`
- `models/account.ts` 里的 `buckets` / `perms` / `currentBucket` 字段**目前没有页面在用**
  （桶列表和权限实际是各页面自己 `useState` + `fetchBuckets()` / `usePerms()` 拉取的）。
  改动权限逻辑时以 `hooks/usePerms.ts` 为准。
- 页面级状态用 `useState` / `useReducer`，**不要**为此新建 model。
- 列表页的「当前目录 / 关键字 / 排序 / 分页」等状态会同步到 URL query（`useSearchParams`），
  便于分享链接。别名表在 `pages/Resource/index.tsx` 的 `QUERY_ALIAS`。

### 8.1 浏览器前进 / 后退（`push` vs `replace`）

**核心约定**（`pages/Resource/index.tsx` 的 `syncUrl`）：

| 操作 | 方式 | 原因 |
|---|---|---|
| 切换目录 / 切换桶 | `push` | 浏览器「后退」要能回到上一个目录 |
| 翻页 / 排序 / 改每页条数 | `replace` | 这些操作不该把历史记录刷屏 |

`syncUrl(params, { push: true })` 决定用 `setSearchParams(next, { replace: !push })`。

配套必须做的事 —— **监听 `popstate`**：

- 只改 URL 是不够的：浏览器后退后 URL 变了，但 React 状态还是旧的。
- `pages/Resource/index.tsx` 里监听了 `window.popstate`，从 `window.location.search` 重新解析
  `p / k / b / f / o / n / s`，同步回 state 并重新请求列表。
- **后退时必须 `{ syncUrl: false }`**，否则会再次写 URL，打断浏览器的历史遍历。
- 跨桶后退时，把目标路径记在 `pendingPathRef` 里，由「桶变化」effect 消费，否则路径会被重置成根目录。

> ⚠️ 改这块时务必实测：进入两级目录 → 连按两次后退 → 应逐级回到上一级路径，**而不是退出页面**。

---

## 9. 常见任务怎么做

| 任务 | 做法 |
|---|---|
| 加一个接口 | `types.ts` 加类型 → `api.ts` 加函数 → 页面 `useCallback` 调用 |
| 加一个页面 | `pages/Xxx/index.tsx` → `routes.ts` 注册 → `menuConfig.tsx` 加菜单 → `BasicLayout` 加标题 |
| 改按钮权限 | `hasPerm(bucket, 'UPDATE')` → `disabled` |
| 加一个响应式分支 | `useResponsive()` 的 `isMobile` / `isTablet`，别写媒体查询 |
| 改上传行为 | `api.ts` 的 `initUpload`/`mergeUpload` —— **注意参数必须在 query** |
| 改样式 | `src/global.css`，加 `.mos-` 前缀类 |
| 改图集大图 | `pages/Viewer/Gallery.tsx` —— **自研灯箱**，见第 12 节 |
| 给长列表加懒加载 | `IntersectionObserver` + 占位骨架，见第 12 节 |

---

## 10. 图集（翻页加载 + 懒加载 + 灯箱）

`pages/Viewer/Gallery.tsx`：

### 10.1 翻页加载（无限滚动）

目录可能有上千张图，**不要用一次 `pageSize: 500` 硬拉**（既慢又会截断）。

- `PAGE_SIZE = 60`，按 `pageNum` 翻页，`append=true` 时追加到已有列表。
- 列表底部放**哨兵元素** `<div className="mos-gallery-sentinel" />`，
  用 `IntersectionObserver`（`rootMargin: '600px 0px'`）监听，滚到底部自动拉下一页。
- 「还有没有更多」用两个条件判断：`!pageEmpty && items.length < total`
  - `pageEmpty`：本次拉到的列表为空 —— 拉不到就停，避免死循环
  - `items.length < total`：还没拉完
- 底部提示：加载中显示「正在加载更多…」，拉完了显示「没有更多了」。
- 切换 `bucket` / `path` 时要**重置**：`items` / `pageNum` / `total` / `pageEmpty` / `activeIndex`。
- `items` 用函数式更新 `setItems(prev => append ? [...prev, ...list] : list)`，
  避免闭包里拿到旧的 `items`。

> 2026-10-25 修复：原来 `pageSize: 500` 一次拉完，实测某个目录有 **929** 张图，
> 超过 500 的部分直接看不到。改成翻页后总数正确显示 929。

### 10.2 缩略图懒加载

**不要一次性把整个目录的图片全部请求下来。** 用 `IntersectionObserver`：

- 组件 `LazyThumb`：外层 `<span className="mos-gallery-thumb">` **始终渲染**（占满格子，
  否则 IntersectionObserver 观察到 0×0 不会触发），内部在进入视口后才插入 `<img>`。
- `rootMargin: '400px 0px'` —— 进入视口前 400px 就开始预取，滚动不卡顿。
- 进入视口后 `io.disconnect()`，每张图只观察一次。
- 浏览器不支持 `IntersectionObserver` 时退化为直接加载。
- 未加载时显示 `.mos-gallery-skeleton` 骨架（微光动画），图片 `onload` 后加 `.is-loaded`
  淡入，避免布局抖动。
- 头部显示「N 张图片（已加载 M）」，方便确认懒加载生效。

> ⚠️ 只写 `<img loading="lazy">` **不够** —— 那是浏览器原生懒加载，
> 但所有 `<img>` 元素照样会一次性建出来，DOM 节点多时照样卡。
> 必须用 `IntersectionObserver` 真正延迟**请求**。

### 10.3 大图灯箱

用**自研灯箱**，不是 `viewerjs`：

- 缩略图是 `<button>`，点击在页内打开灯箱（`activeIndex` state，`-1` 表示未打开）。
  **不要**改回 `<a target="_blank">`（那会跳新标签页，交互很糟）。
- 打开时锁定 `document.body.overflow`，避免背景跟着滚。
- 样式类名：`.mos-lightbox` / `.mos-lightbox-main` / `.mos-lightbox-figure` /
  `.mos-lightbox-nav` / `.mos-lightbox-strip` 等，在 `global.css`。
- 缩略图用 `object-fit: contain`（**不是 cover**），保证原图比例完整可见，不被裁掉。

**切换图片的 4 种方式**（都要支持）：

| 方式 | 实现 |
|---|---|
| 左右箭头按钮 | `.mos-lightbox-prev` / `.mos-lightbox-next` |
| 键盘 | `←` `→` 切换，`Esc` 关闭（`window` 的 `keydown`） |
| 移动端滑动 | `figure` 的 `onTouchStart` / `onTouchEnd`，横向位移阈值 40px |
| **鼠标滚轮** | 灯箱根节点的 `wheel` 事件，上下滚动切图 |

滚轮实现要点（`Gallery.tsx` 的 `onWheel`）：

- `addEventListener('wheel', onWheel, { passive: false })` —— **必须 `passive: false`**，
  否则 `preventDefault()` 无效，背后的图集列表会跟着一起滚。
- `deltaY` 与 `deltaX` 取绝对值大的那个，兼容触控板的横向滑动。
- 阈值 `Math.abs(delta) < 8` 忽略，防止误触。
- **150ms 节流**（`lastWheelRef` 存时间戳），防止快速滚轮连续跳很多张。
- 缩略图条上**不接管滚轮**（`closest('.mos-lightbox-strip')` 判断），
  否则没法用滚轮横向滚动缩略图条。

### 10.4 底部缩略图条（filmstrip）

灯箱底部展示当前图片附近的缩略图，**点击可快速跳转**：

- `STRIP_SIZE = 31`（当前图片居中，左右各留一半）。
- 窗口起点 `stripStart`：
  ```ts
  Math.max(0, Math.min(activeIndex - half, images.length - STRIP_SIZE))
  ```
  末尾会**钳位**，不会越界。
- 只渲染窗口内的 31 张，**不要**把上千张图全塞进 DOM。
- 缩略图条里的图片也走 `LazyThumb`（懒加载）。
- 切换图片后自动把当前项**滚到中间**：
  `stripEl.scrollTo({ left: activeEl.offsetLeft - stripEl.clientWidth / 2 + ... })`。
  用 `scrollTo` 而不是 `scrollIntoView`，避免连带滚动整个页面。
- 窄屏（`< 768px`）缩略图缩到 42×42，左右箭头上移到缩略图条**上方**（`bottom: 92px`），别和条子重叠。

### 10.5 浏览器标题

用 `hooks/useDocumentTitle.ts` 设置标签页标题（**格式 `标题 - MOS 对象存储`**）：

```tsx
useDocumentTitle(`${dirName} · ${total} 张图片`);
```

- 目录名取路径最后一段（根目录用桶名兜底）：`basename(normalizePath(path))`。
- 目录/桶变化时标题要跟着变（`dirName` 用 `useMemo` 依赖 `path` / `bucket`）。
- `useDocumentTitle` 会在组件卸载时恢复默认标题 `MOS 对象存储`。
- 视频查看器（`pages/Viewer/Video.tsx`）同样用了这个 hook，标题是当前视频文件名。

> `config/config.ts` 的 `title: 'MOS 对象存储'` 是 Umi 的默认标题，
> 和 `hooks/useDocumentTitle.ts` 的 `DEFAULT_TITLE` 保持一致。

---

## 10.5 视频播放页

`pages/Viewer/Video.tsx`（路由 `/viewer/video?bucket=&id=&path=`）：
左边播放器 + 视频信息，右边同目录视频列表（关键字过滤 / 滚动加载 / 定位当前播放）。
子组件在 `pages/Viewer/components/`：`VideoBox.tsx`（播放器）、`VideoList.tsx`（列表）。

文件中心的视频**点名称或缩略图都跳到这个页**（见 `pages/Resource/components/ListName.tsx` 的 `onOpenFile`）。

**几个必须记住的坑**：

- **DPlayer 装的是 v1**，播放地址必须写在 `video.url` 上：

  ```ts
  new DPlayer({ container, video: { url: playUrl } });   // ✅
  new DPlayer({ container, url: playUrl });               // ❌ v2 写法，v1 直接忽略（播放器没有画面）
  ```

  换源用 `player.switchVideo({ url })`，不要销毁重建。

- **播放地址要把 `render=true` 换成 `render=false`**：
  服务端 `signUrl` 带 `render=true` 时返回的是 HTML 播放页，播放器要的是视频流本身。

- **同一资源的签名地址每次请求都不一样**（签名带过期时间戳）。
  所以切换视频时**不能**用详情接口返回的 `signUrl` 覆盖列表里的 `signUrl`，
  否则播放器会二次 `switchVideo`，前一次播放请求被中断后的回调会把新视频一起暂停
  （表现为「点了列表不自动播放」）。
  `Video.tsx` 里同 id 且已有可用地址时保留原对象。

- `path` 参数是视频所在**目录**的路径，不是文件路径。
  `GET /member/resource/{b}/file/video/{id}` 用 `d.path = #{dirPath}` 精确匹配目录。

- `signUrl` 依赖**当前用户在该桶下的秘钥**（`AccessControlService.findOwnList`）。
  用户没有秘钥时所有资源的 `signUrl` 都是 `null`，前端要有兜底。

- 列表里的缩略图地址是 `{signUrl}&thumb=true`；
  服务端缩略图是**异步**生成的（`ResourceMetaService.calculateMeta`），
  没生成时 `thumbFileHouseId` 为 `null`，前端显示占位图标即可。
  （截图点取 `min(10s, 视频时长)`，**比视频还短的视频会截不到图**，属服务端既有行为。）

- 视频播完自动连播下一个（`player.on('ended')`）。

- 窄屏（<992px）播放器在上、列表在下；列表项缩略图在 <576px 要缩小到 72×42，
  否则列表项会被撑出容器产生横向滚动。

---

## 11. 验证方式（**不要起 mock 服务**）

> **禁止为验证前端而编写、启动 mock / 假后端服务**（不要新增 `mock-server.cjs` 之类的文件，
> 也不要用 `MOS_API_TARGET` 指向自造的假接口）。

调试与验证的优先级：

1. **直接连真实后端**：`npm run dev` 默认代理到 `config/config.ts` 的 `API_TARGET`
   （当前 `http://localhost:4100`），用真实接口验证。
2. **不需要登录就能验证的接口，直接用 `curl` 打真实后端**。
   例：`POST /upload/mergeFiles` 在未登录时仍会进 controller，
   能直接回显服务端绑定出来的参数值（见第 5.9 节），比 mock 更真实。
3. **需要登录才能验证的** → **直接问用户要账号**（见第 12 节安全红线），
   不要自己造 mock 绕过。

理由：mock 会掩盖真实的请求形态问题 —— 本次「桶不存在:default,default」就是
「前端把 `bucketName` 同时放进 query 和 body」引起的，而 mock 若照抄前端的写法
就复现不出来。**用真实后端当 oracle** 才能验证契约。

### 11.1 打开页面一律用 chrome MCP（不要手动开浏览器）

**开发调试页面时，用 chrome MCP 打开页面，不要自己起浏览器 / 依赖用户手动开页面。**

- 工具：`chrome-devtools` MCP（`navigate_page` / `take_snapshot` / `list_network_requests` /
  `list_console_messages` / `evaluate_script` / `screenshot` 等）。
- 页面地址：`http://localhost:8000/<路由>`（dev server 默认 8000，代理到真实后端 4100）。
- 用途：
  - **看真实渲染效果**（截图），确认布局 / 响应式 / 破图问题；
  - **看控制台报错**（`list_console_messages`），快速定位渲染异常；
  - **看真实请求**（`list_network_requests`），核对参数位置与返回结构；
  - **点按钮跑流程**（删除、还原、上传等），验证接口契约与二次确认。
- 登录：优先用第 12.1 节的本地测试账号；**只试一次**，失败不要连续重试。

> 2026-10-25 教训：靠 `evaluate` 合成事件时踩过两个坑 ——
> ① antd 的 `allowClear` 清除图标本身就是一个 `<button>`，
> `querySelector('.xxx button')` 会选中它而不是搜索按钮；
> ② antd5 的 `Modal.confirm.onOk` 抛错会被 `ActionButton` 再抛一次，
> 必须走 `utils/antdSafe.ts` 的全局补丁，否则整页被错误浮层盖住。

---

## 12. 安全红线

- **禁止用生产环境的真实账号反复试登录**。服务端有失败计数与锁定机制（连续失败会锁定账号、触发验证码）。
- **禁止暴力猜解密码**（撞弱口令、拿哈希离线跑字典）。
- 需要账号时**直接问用户要**：用户名、密码，或让用户自己解锁 / 重置。
- **不要为了验证而编写、启动 mock / 假后端服务**（详见第 11 节）。
- 发现鉴权缺陷（如空值/类型强转绕过）**如实报告给用户**，由用户决定是否修复。

### 12.1 本地测试账号

调试前端时用这个**本地测试账号**登录（用户 2026-10-25 提供）：

| 项 | 值 |
|---|---|
| 用户名 | `localtest` |
| 密码 | `***REMOVED***` |

- 只用于**本地** `mos-server`（`config/config.ts` 的 `API_TARGET` = `http://localhost:4100`）。
- **不要**用它登录生产环境，也不要拿它去试 `admin` 等其它账号。
- 登录页地址 `http://localhost:8000/signin`；验证码只在同一账号连续失败 3 次后才出现。
- 这是**明文口令**，仅供本地调试。若本仓库要公开/上传到公网，**先把这个账号删掉或改掉密码**。

> 2026-10-25 教训：调试 `mos-web` 登录流程时用 `admin/admin` 连续试错，
> 把生产库 `mos_user` 的 admin 账号试到 `locked=1 / failures=5`，需用户手动恢复。
> **任何账号只允许试一次。**

> 补充：本地 `mos-server` 默认连的是 **`mos_dev`**（`bootstrap.properties`），
> 而 `bootstrap.properties` 里的 `admin/admin` 是**引导用的默认密码**，
> 库里实际密码可能已被改过 —— 猜错一次就会计入失败次数。
> **用上面 12.1 的 `localtest` 账号，不要拿 `admin` 试。**

---

## 13. 改动前后 checklist

- [ ] `npm run typecheck` 通过
- [ ] `npm run lint` **没有引入新的** error/warning（存量报错见第 2 节，别大改历史代码）
- [ ] 新增的 URL 前缀已加进 `config/config.ts` 的 `PROXY_PATHS`
- [ ] 新增页面已注册路由 + 菜单 + `getPageTitle`
- [ ] 没有在页面里重复弹错误提示
- [ ] 传给 antd 的异步回调（`onOk` / `onPressEnter`）没有让异常逃逸成
      Unhandled Rejection（`Modal.confirm` 已全局兜底；普通 `Modal` 要用 `safeAsync` 包）
- [ ] 没有硬编码颜色 / 用了 `mos-` 类名
- [ ] 窄屏（<768px）下布局不塌：表格用 `responsive` 收列、操作列收进「更多」、
      分页关掉「每页条数 / 总数」、工具栏分两行
- [ ] 改了目录跳转逻辑 → 实测「进两级目录 → 连按两次后退」能逐级回退
- [ ] 后端契约参数位置没搞错（`@RequestParam` → query）
- [ ] 用 **chrome MCP** 打开 `http://localhost:8000/<路由>` 实际点过一遍，
      确认渲染正常、控制台无报错、请求参数正确（见第 11.1 节）
