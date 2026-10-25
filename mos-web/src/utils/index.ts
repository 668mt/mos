/**
 * 通用工具函数
 */

/**
 * 「这个错误已经向用户提示过了」的标记。
 *
 * 响应拦截器（`services/request.ts`）统一弹 `notification.error` 后，
 * 会给错误打上这个标记。于是「吞掉异常」的公共兜底（`safeAsync`）可以判断：
 * - 已提示过 → 静默吞掉（提示已经由拦截器发出，不要重复弹）；
 * - 未提示过 → 说明是意料之外的异常，打印到控制台便于排查。
 *
 * 用 Symbol 而不是字符串键，避免与业务字段冲突，也不会出现在 JSON 序列化里。
 */
const NOTIFIED = Symbol('mos-notified');

/** 给错误打上「已提示」标记 */
export function markNotified<T>(error: T): T {
  if (error && typeof error === 'object') {
    (error as any)[NOTIFIED] = true;
  }
  return error;
}

/** 这个错误是否已经提示过用户 */
export function isNotified(error: unknown): boolean {
  return !!error && typeof error === 'object' && (error as any)[NOTIFIED] === true;
}

/**
 * 包装异步回调，吞掉它抛出的异常。
 *
 * ## 为什么需要它
 *
 * antd 的 `Modal.onOk` / `Modal.confirm.onOk` 都不会接管回调返回的 Promise：
 * `Modal.confirm` 甚至会在 `ActionButton` 里把 rejection **再抛一次**
 * （见 `antd/es/_util/ActionButton.js`），这个新 promise 无人接管，
 * 于是变成浏览器的 Unhandled Rejection，
 * 开发环境的错误浮层会直接盖住整页，用户连已经弹出的错误提示都看不到。
 *
 * ## 处理方式
 *
 * 在这里把异常吞掉，让 antd 拿到的永远是一个 resolve 的 promise：
 * - 后端业务错误的消息已由 `services/request.ts` 的响应拦截器统一 `notification.error` 提示；
 * - 表单校验失败由 antd Form 在字段下方标红；
 * - 意料之外的异常（没有提示过的）打印到控制台，便于排查。
 */
export function safeAsync<A extends any[]>(
  fn: (...args: A) => Promise<unknown> | unknown,
): (...args: A) => Promise<void> {
  return async (...args: A) => {
    try {
      await fn(...args);
    } catch (err: any) {
      // 已提示过用户的错误（后端业务错误 / 网络错误）→ 静默吞掉
      if (isNotified(err)) return;
      // antd 表单校验失败 → 预期内，antd 已在字段下方标红
      if (err && Array.isArray(err.errorFields)) return;
      // 其余是代码缺陷，输出到控制台便于排查
      console.error('[mos-web] 异步回调异常:', err);
    }
  };
}

/** 深拷贝（兼容旧版 mt.deepCopy 行为） */
export function deepCopy<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') return obj;
  if (Array.isArray(obj)) return obj.map((item) => deepCopy(item)) as unknown as T;
  const result: Record<string, any> = {};
  Object.keys(obj as Record<string, any>).forEach((key) => {
    result[key] = deepCopy((obj as Record<string, any>)[key]);
  });
  return result as T;
}

/** 判断数组是否包含某项 */
export function contains<T>(arr: T[] | undefined | null, item: T): boolean {
  if (!arr) return false;
  return arr.indexOf(item) !== -1;
}

/** 字节数转为可读大小 */
export function readableSize(sizeByte: number | null | undefined): string {
  if (sizeByte === null || sizeByte === undefined || sizeByte < 0) return '-';
  if (sizeByte <= 1024) return `${sizeByte} B`;
  if (sizeByte <= 1024 * 1024) return `${(sizeByte / 1024).toFixed(2)} KB`;
  if (sizeByte <= 1024 * 1024 * 1024) {
    return `${(sizeByte / 1024 / 1024).toFixed(2)} MB`;
  }
  return `${(sizeByte / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

/** 路径拼接 */
export function joinPath(parent: string, name: string): string {
  if (!parent || parent === '/') return `/${name}`;
  return `${parent.replace(/\/+$/, '')}/${name}`;
}

/** 归一化目录路径：去掉尾部斜杠，根目录统一为 '/' */
export function normalizePath(path: string | undefined | null): string {
  if (!path) return '/';
  let p = path;
  if (!p.startsWith('/')) p = `/${p}`;
  while (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
  return p;
}

/** 取路径的父路径 */
export function parentOf(path: string): string {
  const p = normalizePath(path);
  if (p === '/') return '/';
  const idx = p.lastIndexOf('/');
  if (idx <= 0) return '/';
  return p.slice(0, idx);
}

/** 取路径的最后一段（文件名 / 目录名） */
export function basename(path: string): string {
  const p = normalizePath(path);
  if (p === '/') return '/';
  return p.slice(p.lastIndexOf('/') + 1);
}

/** 生成父级路径链（根在最前） */
export function pathChain(path: string): string[] {
  const p = normalizePath(path);
  if (p === '/') return [];
  const segments = p.split('/').filter(Boolean);
  const chain: string[] = [];
  let acc = '';
  segments.forEach((seg) => {
    acc += `/${seg}`;
    chain.push(acc);
  });
  return chain;
}

/** 防抖 */
export function debounce<T extends (...args: any[]) => void>(fn: T, wait = 300) {
  let timer: ReturnType<typeof setTimeout> | null = null;
  return (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

/** 复制文本到剪贴板（兼容不支持 Clipboard API 的浏览器） */
export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // fallthrough
  }
  try {
    const el = document.createElement('textarea');
    el.value = text;
    el.style.position = 'fixed';
    el.style.opacity = '0';
    document.body.appendChild(el);
    el.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(el);
    return ok;
  } catch {
    return false;
  }
}

/** 是否是移动端 / 小屏（用于 PC 与 WEB 双端适配） */
export function isMobileDevice(): boolean {
  if (typeof navigator === 'undefined') return false;
  const ua = navigator.userAgent;
  const mobileAgents = ['Android', 'iPhone', 'SymbianOS', 'Windows Phone', 'iPad', 'iPod'];
  if (mobileAgents.some((m) => ua.indexOf(m) > 0)) return true;
  const w = window.screen?.width ?? 0;
  const h = window.screen?.height ?? 0;
  return w > 0 && h > 0 && w < 500 && h < 800;
}

/** 复制文本时使用的常量 */
export const CONTENT_TYPES = [
  'text/html;charset=UTF-8',
  'text/xml;charset=UTF-8',
  'text/plain;charset=UTF-8',
  'application/json;charset=UTF-8',
  'application/octet-stream',
  'application/pdf',
  'text/markdown;charset=UTF-8',
  'text/event-stream;charset=UTF-8',
  'image/png',
  'image/jpeg',
  'image/gif',
  'application/xml',
  'application/xhtml+xml',
  'application/stream+json',
  'application/rss+xml',
  'application/problem+xml',
  'application/problem+json;charset=UTF-8',
  'application/problem+json',
];
