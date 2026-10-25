import { Modal } from 'antd';
import type { ModalFuncProps } from 'antd';
import { safeAsync } from './index';

/**
 * antd 静态确认框的全局兜底补丁。
 *
 * ## 为什么需要它
 *
 * 后端业务错误统一由 `services/request.ts` 的响应拦截器弹 `notification.error`，
 * 提示本身已经是公共的。但调用方（比如 `Modal.confirm({ onOk })`）通常**不会**
 * 在 `onOk` 里写 try/catch，于是：
 *
 * 1. `onOk` 返回一个 rejected promise；
 * 2. antd5 的 `ActionButton` 会把同一个错误**再抛一次**
 *    （`antd/es/_util/ActionButton.js` 的 `handlePromiseOnOk`）：
 *    ```js
 *    returnValueOfOnOk.then(onOk, e => {
 *      ...
 *      return Promise.reject(e);   // ← 这个新 promise 无人接管
 *    });
 *    ```
 * 3. 于是浏览器判定为 **Unhandled Rejection**；
 * 4. umi 的 dev-server client（`startReportingRuntimeErrors`）监听
 *    `window.onunhandledrejection`，弹出**盖住整页**的错误浮层，
 *    把第 1 步已经弹出的错误提示也一起遮住。
 *
 * react-error-overlay 的处理函数不检查 `event.defaultPrevented`，
 * 所以靠 `window.addEventListener('unhandledrejection')` 是**压不过**它的，
 * 只能在 antd 的 `onOk` 边界上把异常吃掉。
 *
 * ## 这里做了什么
 *
 * 在应用启动时给 antd 的静态确认框方法打一次补丁，
 * 把 `onOk` 统一包成不会 reject 的版本：
 * - 后端业务错误的消息已由响应拦截器统一 `notification.error` 提示；
 * - 确认框会正常关闭，由调用方自己的 `message.success` + 刷新逻辑决定后续。
 *
 * 效果：**现有所有 `Modal.confirm(...)` 调用点一行都不用改**，
 * 以后新写的确认框也自动受保护。
 *
 * 注意：包之后的 `onOk` 长度恒为 0，antd 会走「无参调用」分支，
 * 因此 `onOk` 里拿不到 `close` 回调（当前仓库也没有用到），
 * 同时保留了 antd 对重复点击的拦截（不会重复提交删除请求）。
 */

/** 需要打补丁的静态方法 */
const STATIC_METHODS = ['confirm', 'info', 'success', 'error', 'warning'] as const;

/** 标记，避免 HMR / 多次引入时重复包装 */
const PATCH_FLAG = '__mos_safe_on_ok__';

let patched = false;

/**
 * 给 antd 静态确认框方法的 `onOk` 套上安全包装。
 *
 * 幂等，可重复调用。
 */
export function patchModalStatics() {
  if (patched) return;
  patched = true;

  STATIC_METHODS.forEach((name) => {
    const original = (Modal as any)[name];
    if (typeof original !== 'function' || original[PATCH_FLAG]) return;

    const wrapped = function (props: ModalFuncProps) {
      const { onOk, ...rest } = props || ({} as ModalFuncProps);
      return original.call(Modal, {
        ...rest,
        onOk: onOk ? safeAsync(onOk) : undefined,
      } as ModalFuncProps);
    };

    wrapped[PATCH_FLAG] = true;
    (Modal as any)[name] = wrapped;
  });
}

// 模块被引入即生效（`src/app.tsx` 里 import 一次即可）
patchModalStatics();
