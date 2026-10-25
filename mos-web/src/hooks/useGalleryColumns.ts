import { useCallback, useState } from 'react';

/** localStorage 键：记住用户设置的每行图片数 */
const STORAGE_KEY = 'mos-gallery-columns';

/** 每行图片数的取值范围 */
export const MIN_COLUMNS = 1;
export const MAX_COLUMNS = 12;

/**
 * 读取已保存的每行图片数。
 *
 * 没保存过（或保存的值非法）返回 `null` —— 表示「自适应」，
 * 由 CSS 的 `repeat(auto-fill, ...)` 决定每行放几张。
 */
export function readStoredColumns(): number | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const n = Number.parseInt(raw, 10);
    if (!Number.isFinite(n)) return null;
    if (n < MIN_COLUMNS || n > MAX_COLUMNS) return null;
    return n;
  } catch {
    // localStorage 不可用（隐私模式等）时按「自适应」处理
    return null;
  }
}

/** 保存每行图片数；传 `null` 表示恢复自适应 */
export function writeStoredColumns(columns: number | null) {
  try {
    if (columns === null) {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, String(columns));
    }
  } catch {
    // 忽略：存不下不影响本次会话内的使用
  }
}

/**
 * 图集每行图片数。
 *
 * - `columns` 为 `null` 表示自适应（默认），由 CSS 决定
 * - 拖动滑块后固定为具体列数，并写入 localStorage 记忆
 * - `reset()` 恢复自适应并清除已保存的值
 */
export function useGalleryColumns() {
  const [columns, setColumns] = useState<number | null>(() => readStoredColumns());

  const setColumnsAndPersist = useCallback((next: number) => {
    const clamped = Math.min(MAX_COLUMNS, Math.max(MIN_COLUMNS, next));
    setColumns(clamped);
    writeStoredColumns(clamped);
  }, []);

  /** 恢复自适应 */
  const reset = useCallback(() => {
    setColumns(null);
    writeStoredColumns(null);
  }, []);

  /** 是否处于「自适应」状态 */
  const isAuto = columns === null;

  return {
    columns,
    isAuto,
    setColumns: setColumnsAndPersist,
    reset,
  };
}
