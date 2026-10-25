import { useEffect } from 'react';

/** 默认标题（与 `config/config.ts` 的 `title` 保持一致） */
export const DEFAULT_TITLE = 'MOS 对象存储';

/**
 * 设置浏览器标签页标题。
 *
 * - `title` 为空时只显示默认标题
 * - 非空时格式为 `标题 - 默认标题`
 * - 组件卸载时恢复默认标题
 */
export function useDocumentTitle(
  title?: string | null,
  fallback: string = DEFAULT_TITLE,
) {
  useEffect(() => {
    document.title = title ? `${title} - ${fallback}` : fallback;
  }, [title, fallback]);

  // 只在卸载时执行一次，恢复默认标题
  useEffect(() => {
    return () => {
      document.title = fallback;
    };
  }, [fallback]);
}
