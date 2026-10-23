# 发布到 npm 中央仓库

包名：**`@668mt/mos-cli`**

> `mos-cli` 这个名字在 npm 上已被他人占用（一个无关的 vue 脚手架），
> 且本项目的 npm 用户名是 `li1296883752`，因此使用与 GitHub 仓库对应的
> 组织 scope `@668mt`。组织名即 scope 名。

## 背景：为什么需要组织

npm 的规则是「**每个用户 / 组织各有一个 scope，且只有你能往自己的 scope 里放包**」。

- 用户 scope（`@li1296883752/xxx`）：注册账号即可发布，不需要组织
- 组织 scope（`@668mt/xxx`）：**必须先创建名为 `668mt` 的组织**

官方原文：

> If publishing a public module to an organization scope, you must first
> either create an organization with the name of the scope that you'd like
> to publish to or be added to an existing organization.

我们选择组织 scope，好处是包名与项目/仓库解耦，后续可邀请其他维护者。

---

## 一次性准备（npmjs.com 上操作）

### 步骤 1：注册 npm 账号

访问 <https://www.npmjs.com/signup>，用邮箱 `765936728@qq.com` 注册。

> 用户名已确认可用：`li1296883752`（也可用你的 GitHub 用户名，二者不必一致）。

注册后需要**通过邮箱验证**，未验证的账号无法发布。

### 步骤 2：开启两步验证（2FA）

npm 现在**强制要求 2FA** 才能发布。

1. 登录 <https://www.npmjs.com/settings>
2. 找到「Two-step authentication」
3. 选择 **Authenticator App**（推荐，无需额外设备）
4. 用 Google Authenticator / Microsoft Authenticator 等扫码
5. 输入验证码完成绑定
6. **务必保存 recovery codes**（丢失 2FA 设备时用它恢复）

### 步骤 3：创建名为 `668mt` 的组织

1. 登录 <https://www.npmjs.com>
2. 右上角头像 → **Add an Organization**
3. `Name` 填 **`668mt`**（这个名字就是 scope 名，不可轻易更改）
4. 选择套餐：
   - **Unlimited public packages（免费）** ← 选这个
   - Unlimited private packages（付费，本次不需要）
5. 点 Create
6. （可选）邀请其他成员：填对方 npm 用户名或邮箱，选择团队后 Invite

> 免费计划对**公开包**完全够用，无需付款。

### 步骤 4：确认组织可用

```bash
npm view @668mt/mos-cli --registry=https://registry.npmjs.org/
```

此时报 `E404` 是正常的（包还没发布）。若报权限错误，说明组织没建好或当前账号不在其中。

---

## 本地配置（一次性）

### 登录

```bash
npm login --registry=https://registry.npmjs.org/
```

> ⚠️ 本机默认 registry 是淘宝镜像（`registry.npmmirror.com`）。
> **登录和发布都必须显式指定官方源**，否则会失败或发错地方。

验证：

```bash
npm whoami --registry=https://registry.npmjs.org/
# 期望输出：li1296883752
```

### 建议：把官方源设为该 scope 的默认 registry

```bash
npm config set @668mt:registry https://registry.npmjs.org/
```

这样 `npm publish` 会自动把 `@668mt/*` 发到官方源，不需要每次带 `--registry`。

---

## 发布

```bash
cd mos-cli
npm run release
```

该命令 = `typecheck` + `test` + `publish --access public`。

首次发布**必须带 `--access public`**（作用域包默认私有，`npm run release` 已内置）。

手动等价命令：

```bash
npm run typecheck && npm test
npm publish --access public --registry=https://registry.npmjs.org/
```

## 发布后验证

```bash
npm view @668mt/mos-cli --registry=https://registry.npmjs.org/

# 换个目录实测安装
npm install -g @668mt/mos-cli
mos --version
```

---

## 后续发新版本

```bash
npm version patch     # 1.0.0 -> 1.0.1，同时打 git tag
git push --follow-tags
npm run release
```

> npm **不允许重复发布同一版本号**。删包需要 72 小时冷静期，且只能删 72 小时内、
> 下载量 < 300 的版本，所以版本号要一次给对。

## 邀请其他维护者

组织成员管理：<https://www.npmjs.com/settings/access/collaborators/668mt/packages>

可添加成员并授予 `Read` / `Write` 权限（Write 才能发版）。

---

## 可选：GitHub Actions 自动发布

比 token 更安全的方式是 **Trusted Publishing**（OIDC 免密）：

1. 先手动发布一次 `1.0.0`
2. 在 npmjs.com 包页面 → Access → Trusted Publisher → Add
   - Package: `@668mt/mos-cli`
   - Repository owner: `668mt`，Repository: `mos`
   - Workflow filename: `release.yml`，Environment: （留空）
3. 创建 `.github/workflows/release.yml`：

```yaml
name: Publish
on:
  release:
    types: [published]
jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      id-token: write   # OIDC 必需
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          registry-url: 'https://registry.npmjs.org'
      - run: npm ci
      - run: npm run prepublishCheck
      - run: npm publish --access public
```

这样以后在 GitHub 上打 Release 即自动发布，无需配置任何 token。

---

## 常见问题

**`E403 You do not have permission to publish`**
组织没建成，或当前账号不是该组织成员。检查 <https://www.npmjs.com/orgs/668mt>

**`ENEEDAUTH` / `401 Unauthorized`**
未登录或登录的是镜像源。用 `npm whoami --registry=https://registry.npmjs.org/` 验证。

**`EOTP`（需要一次性密码）**
npm 要求 2FA。发布时用 `npm publish --otp=xxxxxx`，OTP 从 Authenticator App 获取。

**发到了淘宝镜像**
说明漏了 `--registry`。用 `npm config set @668mt:registry https://registry.npmjs.org/` 永久修正。

**包名报 403 / scope 不存在**
组织 `668mt` 尚未创建完成，或名字已被占用。