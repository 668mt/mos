/**
 * 路由配置
 *
 * 说明：
 * - 服务端 `SecurityConfig` 中 `loginPage("/signin")`，
 *   登录处理地址为 `POST /login`，`/signin` 是 SPA 的前端路由。
 * - `/` 为布局路由，其子路由套用 `BasicLayout`（PC 侧边栏 / 移动端抽屉）。
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
      { path: '/', redirect: '/home' },
      { path: '/home', name: '总览', component: '@/pages/Home' },
      { path: '/resource', name: '文件中心', component: '@/pages/Resource' },
      { path: '/trash', name: '回收站', component: '@/pages/Trash' },
      { path: '/bucket', name: '桶管理', component: '@/pages/Bucket' },
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
