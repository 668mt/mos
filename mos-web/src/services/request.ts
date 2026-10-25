import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { notification } from 'antd';
import { history } from 'umi';
import { markNotified } from '@/utils';
import type { ResResult } from './types';

/**
 * 后端地址。
 *
 * - 开发环境：走 devServer 代理（同源 /member、/admin 等），无需额外配置。
 * - 生产环境：可设置 window.__MOS_API_BASE__ 或通过环境变量注入。
 */
const baseURL =
  (typeof window !== 'undefined' && (window as any).__MOS_API_BASE__) || '';

const instance: AxiosInstance = axios.create({
  baseURL,
  withCredentials: true,
  timeout: 60_000,
});

/** 登录过期时跳转到登录页（防重复跳转） */
let redirecting = false;
function redirectToLogin() {
  if (redirecting) return;
  redirecting = true;
  notification.error({
    message: '登录已过期',
    description: '请重新登录后再试',
    placement: 'topRight',
  });
  setTimeout(() => {
    localStorage.removeItem('currentUser');
    history.replace('/signin');
    redirecting = false;
  }, 1200);
}

/** 业务错误：HTTP 200 但 status !== 'ok' */
export class BizError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'BizError';
  }
}

/** 静态资源（图片、样式等）不参与「会话失效」判定 */
const STATIC_EXT = /\.(png|jpg|jpeg|gif|webp|svg|bmp|ico|mp4|webm|css)(\?|$)/i;

/**
 * 判断响应是否是「本该返回 JSON，却返回了 HTML」。
 *
 * 服务端未登录时会 302 跳到 /signin。浏览器会自动跟随重定向，
 * 最终可能拿到登录页 HTML（200），也可能因为跳转目标不存在而拿到 404/5xx。
 * 共同特征是 content-type 不是 JSON。
 */
function isHtmlResponse(response: AxiosResponse): boolean {
  const contentType = String(
    response.headers?.['content-type'] || '',
  ).toLowerCase();
  return contentType.includes('text/html');
}

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data;
    const url = response.config?.url || '';
    const isLoginRequest = /\/login(\?|$)/.test(url);
    const isStatic = STATIC_EXT.test(url);

    // 会话失效：本该返回 JSON，却返回了 HTML
    if (!isStatic && isHtmlResponse(response)) {
      if (isLoginRequest) {
        notification.error({
          message: '登录失败',
          description: '认证未通过，请检查用户名、密码或验证码',
          placement: 'topRight',
        });
        return Promise.reject(markNotified(new BizError('认证未通过')));
      }
      redirectToLogin();
      return Promise.reject(markNotified(new BizError('登录已过期')));
    }

    // 后端大部分业务错误以 HTTP 200 + { status: 'error' } 返回
    if (data && data.status !== undefined && data.status !== 'ok') {
      notification.error({
        message: data.message || '请求失败',
        placement: 'topRight',
      });
      return Promise.reject(markNotified(new BizError(data.message || '请求失败')));
    }
    return response;
  },
  (error: AxiosError) => {
    const response = error.response;
    const url = error.config?.url || '';
    const isLoginRequest = /\/login(\?|$)/.test(url);
    const isStatic = STATIC_EXT.test(url);

    if (response?.status === 403) {
      const msg = (response.data as any)?.message;
      notification.error({
        message: '没有权限访问',
        description: msg || url,
        placement: 'topRight',
      });
      return Promise.reject(markNotified(error));
    }

    if (response?.status === 401) {
      redirectToLogin();
      return Promise.reject(markNotified(error));
    }

    // 会话失效：重定向到登录页后拿到 HTML（可能是 404 / 5xx）
    if (!isStatic && response && isHtmlResponse(response)) {
      if (isLoginRequest) {
        notification.error({
          message: '登录失败',
          description: '认证未通过，请检查用户名、密码或验证码',
          placement: 'topRight',
        });
      } else {
        redirectToLogin();
      }
      return Promise.reject(markNotified(error));
    }

    if (!response) {
      notification.error({
        message: '网络错误',
        description: error.message || '无法连接服务器，请检查网络或后端服务是否已启动',
        placement: 'topRight',
      });
      return Promise.reject(markNotified(error));
    }

    const bizMessage = (response.data as any)?.message;
    notification.error({
      message: '请求失败',
      description: bizMessage || `HTTP ${response.status}`,
      placement: 'topRight',
    });
    return Promise.reject(markNotified(error));
  },
);

/** 发起 GET 请求，返回解包后的 `result` */
export async function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.get<ResResult<T>>(url, config);
  return res.data.result;
}

/** 发起 GET 请求，返回完整响应体（用于裸 JSON 接口，如 file/suffix） */
export async function getRaw<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.get<T>(url, config);
  return res.data;
}

/** 发起 POST 请求，返回解包后的 `result` */
export async function post<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig,
): Promise<T> {
  const res = await instance.post<ResResult<T>>(url, data, config);
  return res.data.result;
}

/** 发起 PUT 请求，返回解包后的 `result` */
export async function put<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig,
): Promise<T> {
  const res = await instance.put<ResResult<T>>(url, data, config);
  return res.data.result;
}

/** 发起 DELETE 请求，返回解包后的 `result` */
export async function del<T = any>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<T> {
  const res = await instance.delete<ResResult<T>>(url, config);
  return res.data.result;
}

/** 表单序列化（application/x-www-form-urlencoded），与旧版 transformFormData 保持一致 */
export function transformFormData(data: Record<string, any>): string {
  let ret = '';
  for (const it in data) {
    ret += `${encodeURIComponent(it)}=${encodeURIComponent(data[it])}&`;
  }
  if (ret.length > 0) {
    ret = ret.substring(0, ret.length - 1);
  }
  return ret;
}

export default instance;
