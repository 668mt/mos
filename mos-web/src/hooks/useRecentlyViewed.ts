import { useCallback, useEffect, useState } from 'react';

/** localStorage 键：最近查看过的**图片**（图片 id -> 最后查看时间戳） */
const VIEWED_IMAGES_KEY = 'mos-gallery-viewed';

/**
 * localStorage 键：最近进入过的**目录**（目录 id -> 最后进入时间戳）。
 *
 * 必须和图片分开存：资源的 id 和目录的 id 是两张表的自增列，会撞号，
 * 放同一个 key 里会把「目录 5」当成「图片 5」标记上。
 */
const VISITED_DIRS_KEY = 'mos-gallery-visited-dirs';

/**
 * 「看过 / 进过」的有效期：2 小时。
 *
 * 超过这个时间就不再标色 —— 用户要的是「哪些还没看」，
 * 过了两小时再看就当重新看一遍。
 */
export const VIEWED_TTL = 2 * 60 * 60 * 1000;

/** id -> 最后查看时间戳 */
type VisitedMap = Record<string, number>;

/** 读出本地记录（localStorage 不可用时返回空） */
function readMap(storageKey: string): VisitedMap {
  try {
    const raw = localStorage.getItem(storageKey);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object') return {};
    return parsed as VisitedMap;
  } catch {
    return {};
  }
}

/** 丢掉超过有效期的记录，顺便把非法值也清掉 */
function prune(map: VisitedMap, now: number): VisitedMap {
  const out: VisitedMap = {};
  Object.keys(map).forEach((key) => {
    const ts = map[key];
    if (typeof ts === 'number' && now - ts < VIEWED_TTL) out[key] = ts;
  });
  return out;
}

/** 把记录转成 id 集合 */
function toIdSet(map: VisitedMap): Set<number> {
  return new Set(Object.keys(map).map((k) => Number(k)));
}

/**
 * 通用「最近看过 / 进过」的记录。
 *
 * - 记录在 **localStorage**（按 id，不是按桶）—— 资源、目录的 id 都全局唯一
 * - 超过 `VIEWED_TTL` 自动失效
 * - 多个标签页之间通过 `storage` 事件同步
 */
function useRecentlyVisited(storageKey: string) {
  const [visited, setVisited] = useState<Set<number>>(() =>
    toIdSet(prune(readMap(storageKey), Date.now())),
  );

  /** 记下「刚看过 / 刚进过」这个 id */
  const markVisited = useCallback(
    (id: number) => {
      const now = Date.now();
      const map = prune(readMap(storageKey), now);
      map[String(id)] = now;
      try {
        localStorage.setItem(storageKey, JSON.stringify(map));
      } catch {
        // 存不下（隐私模式等）不影响本次会话内的标记
      }
      setVisited(toIdSet(map));
    },
    [storageKey],
  );

  // 其它标签页看过/清空后，本页同步更新
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key !== storageKey) return;
      setVisited(toIdSet(prune(readMap(storageKey), Date.now())));
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, [storageKey]);

  /** 这个 id 是否在有效期内被看过 / 进过 */
  const isVisited = useCallback((id: number) => visited.has(id), [visited]);

  return { visited, isVisited, markVisited };
}

/**
 * 最近查看过的图片。
 *
 * 用途：图库里把 2 小时内点开看过的图片标个颜色，
 * 免得在一堆缩略图里分不清哪些已经看过了。
 */
export function useRecentlyViewed() {
  return useRecentlyVisited(VIEWED_IMAGES_KEY);
}

/**
 * 最近进入过的目录。
 *
 * 用途：图库的子目录条上，把 2 小时内点进去过的文件夹标出来。
 */
export function useVisitedDirs() {
  return useRecentlyVisited(VISITED_DIRS_KEY);
}
