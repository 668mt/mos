import { defineConfig } from '@umijs/max';
import routes from './routes';

const API_TARGET = process.env.MOS_API_TARGET || 'http://localhost:9700';
// const API_TARGET = process.env.MOS_API_TARGET || 'http://localhost:4100';

/**
 * 需要代理到后端的路径前缀。
 *
 * 注意：`http-proxy-middleware` 的字符串 context 是**前缀匹配**，
 * `/s` 会误匹配 `/signin`、`/static` 等路径。
 * 这里改用函数 context，精确判断路径边界（`/xxx` 或 `/xxx/...`）。
 */
const PROXY_PATHS = [
  '/member',
  '/admin',
  '/open',
  '/login',
  '/mos',
  '/upload',
  '/kaptcha',
  '/render',
  '/gallary',
  '/s',
  '/health',
];

function isProxyPath(pathname: string): boolean {
  return PROXY_PATHS.some(
    (p) => pathname === p || pathname.startsWith(`${p}/`),
  );
}

export default defineConfig({
  antd: {},
  model: {},
  request: {},
  initialState: {},
  routes,
  npmClient: 'npm',
  title: 'MOS 对象存储',
  hash: true,
  favicons: ['/favicon.ico'],
  metas: [
    { charset: 'utf-8' },
    {
      name: 'viewport',
      content:
        'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover',
    },
    { name: 'theme-color', content: '#1677ff' },
    {
      name: 'description',
      content: 'MOS 分布式对象存储服务 Web 控制台',
    },
    { name: 'apple-mobile-web-app-capable', content: 'yes' },
    { name: 'format-detection', content: 'telephone=no' },
  ],
  /** 开发环境代理到后端 */
  proxy: [
    {
      // http-proxy-middleware 支持函数形式的 context，用于精确匹配路径边界
      context: isProxyPath as unknown as string,
      target: API_TARGET,
      changeOrigin: true,
      /**
       * 重写 302 的 Location 头。
       *
       * 服务端未登录时会 302 跳到自己的 /signin（例如 http://localhost:9700/signin）。
       * 开发环境下前后端不同源，若原样透传，浏览器会跳到后端而不是前端 SPA，
       * 导致页面变成后端的登录页。
       */
      onProxyRes: (proxyRes: any) => {
        const location = proxyRes.headers?.location;
        if (location && /\/signin(\?|$)/.test(location)) {
          proxyRes.headers.location = '/signin';
        }
      },
    },
  ],
});
