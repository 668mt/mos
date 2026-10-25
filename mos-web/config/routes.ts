/**
 * 路由配置
 *
 * 说明：
 * - 服务端 `SecurityConfig` 中 `loginPage("/signin")`，
 *   登录处理地址为 `POST /login`，`/signin` 是 SPA 的前端路由。
 * - `/` 为布局路由，其子路由套用 `BasicLayout`（PC 侧边栏 / 移动端抽屉）。
 *
 * 资源管理的信息架构：
 * - `/resource`            桶列表（桶的管理），点桶名进入桶内
 * - `/resource/dashboard`  桶内仪表盘
 * - `/resource/files`      桶内资源（文件）列表
 * - `/resource/trash`      桶内回收站
 * 三个桶内页签通过查询参数 `b` 携带桶名。
 */
export default [
  {
    path: '/signin',
    name: '登录',
    component: '@/pages/Login',
    layout: false,
  },
  {
    path: '/viewer/video',
    name: '视频',
    component: '@/pages/Viewer/Video',
    layout: false,
  },
  {
    path: '/viewer/gallery',
    name: '图库',
    component: '@/pages/Viewer/Gallery',
    layout: false,
  },
  {
    path: '/403',
    name: '403',
    component: '@/pages/Exception/403',
    layout: false,
  },
  {
    path: '/404',
    name: '404',
    component: '@/pages/Exception/404',
    layout: false,
  },
  {
    path: '/',
    component: '@/layouts/LayoutWrapper',
    routes: [
      { path: '/', redirect: '/resource' },
      { path: '/resource', name: '资源管理', component: '@/pages/Resource/BucketList' },
      { path: '/resource/dashboard', name: '仪表盘', component: '@/pages/Resource/Dashboard' },
      { path: '/resource/files', name: '文件管理', component: '@/pages/Resource/FileList' },
      { path: '/resource/trash', name: '回收站', component: '@/pages/Resource/Trash' },
      { path: '/manage/client', name: '服务器管理', component: '@/pages/Client' },
      { path: '/manage/user', name: '用户管理', component: '@/pages/User' },
    ],
  },
  {
    path: '*',
    component: '@/pages/Exception/404',
    layout: false,
  },
];
