import type { Dir, DirAndResourceVo } from '@/services/types';

/**
 * 图库页的**会话内内存缓存**。
 *
 * 目的：浏览器「后退」回到上一个目录时，不要重新拉列表、重新下载封面。
 *
 * 背景：React Router 在**同一路由**之间导航（只改 query）时组件**不会卸载**，
 * state 还在；但图库页是按 `path` 变化重新请求的，
 * 所以要在请求前先查缓存 —— 命中就直接把数据灌回 state，秒开。
 *
 * 缓存是**模块级**的（组件不卸载也没关系，缓存活在模块里），
 * 刷新页面就没了 —— 这符合「后退别重刷」的诉求，但不该做成持久化。
 */

/** 子目录的信息：封面 + 图片数量 + 子文件夹数量 */
export interface DirInfo {
  /** 封面缩略图地址（没有可用图片时为空串） */
  cover: string;
  /** 该目录下的图片数量 */
  imageCount: number;
  /** 该目录下的**子文件夹**数量 */
  dirCount: number;
}

/** 一个目录页的缓存内容 */
export interface GalleryPageCache {
  items: DirAndResourceVo[];
  total: number;
  pageNum: number;
  pageEmpty: boolean;
  subDirs: DirAndResourceVo[];
  /** 子目录已加载到第几页 */
  subDirPageNum: number;
  /** 子目录总数 */
  subDirTotal: number;
  parentDirs: Dir[];
  scrollTop: number;
  /** 写入时间戳 */
  at: number;
}

/**
 * 缓存有效期：超过这个时间就当失效（避免看到过期的数据）。
 *
 * 30 分钟 —— 「后退秒开」是硬需求，5 分钟太短，
 * 用户翻一会儿目录再退回去就又要重新拉一遍。
 */
const TTL = 30 * 60 * 1000;

/** 最多缓存多少个目录页（超了丢最旧的，避免无限增长） */
const MAX_PAGE_ENTRIES = 40;

/** 封面缓存最多存多少条 */
const MAX_COVER_ENTRIES = 80;

/** 目录页缓存：key = `${bucket}|${path}` */
const pageCache = new Map<string, GalleryPageCache>();

/** 子目录信息缓存：key = `${bucket}|${dirPath}` → 封面 + 图片数量 */
const dirInfoCache = new Map<string, DirInfo>();

/**
 * sessionStorage 里的持久化副本。
 *
 * 内存缓存在**整页刷新**后就没了（`location.replace`、手动 F5 都会丢），
 * 所以再存一份到 sessionStorage —— 同一个标签页内刷新/整页跳转回来还能命中。
 */
const SESSION_KEY = 'mos-gallery-cache';

/**
 * 把内存缓存写进 sessionStorage（离开目录时调用，频率低）。
 *
 * 目录页缓存 + 子目录信息缓存都要存：
 * 少了子目录信息，整页跳转回来会重新发 N 个子目录请求。
 */
export function persistGalleryCache() {
  try {
    sessionStorage.setItem(
      SESSION_KEY,
      JSON.stringify({
        pages: Array.from(pageCache.entries()),
        dirs: Array.from(dirInfoCache.entries()),
      }),
    );
  } catch {
    // 存不下（容量满、隐私模式）就算了，内存缓存还在
  }
}

/**
 * 从 sessionStorage 恢复缓存。
 *
 * 模块加载时自动执行一次 —— 整页刷新后内存缓存是空的，
 * 但 sessionStorage 里的还在，能直接命中。
 */
function restoreGalleryCache() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object') return;
    const now = Date.now();
    (parsed.pages || []).forEach(([k, v]: [string, GalleryPageCache]) => {
      // 内存里已有的优先；过期的直接丢掉
      if (pageCache.has(k)) return;
      if (!v || typeof v.at !== 'number' || now - v.at > TTL) return;
      pageCache.set(k, v);
    });
    (parsed.dirs || []).forEach(([k, v]: [string, DirInfo]) => {
      if (dirInfoCache.has(k)) return;
      if (!v) return;
      dirInfoCache.set(k, v);
    });
  } catch {
    // 解析失败就算了
  }
}

restoreGalleryCache();

function cacheKey(bucket: string, path: string) {
  return `${bucket}|${path}`;
}

/**
 * 读缓存。
 *
 * 没有、或已过期（超过 TTL）返回 `null`。
 */
export function readGalleryCache(
  bucket: string,
  path: string,
): GalleryPageCache | null {
  const k = cacheKey(bucket, path);
  const hit = pageCache.get(k);
  if (!hit) return null;
  if (Date.now() - hit.at > TTL) {
    pageCache.delete(k);
    return null;
  }
  return hit;
}

/** 写缓存（顺带限制条数：超了就丢最早写入的） */
export function writeGalleryCache(
  bucket: string,
  path: string,
  data: Omit<GalleryPageCache, 'at'>,
) {
  const k = cacheKey(bucket, path);
  pageCache.delete(k);
  pageCache.set(k, { ...data, at: Date.now() });
  while (pageCache.size > MAX_PAGE_ENTRIES) {
    const oldest = pageCache.keys().next().value;
    if (oldest === undefined) break;
    pageCache.delete(oldest);
  }
}

/**
 * 让某个目录页的缓存失效。
 *
 * 删除图片后必须调用 —— 否则后退回来会看到已经删掉的图。
 * 只失效**当前目录**：资源只属于一个目录，不影响别的目录的缓存。
 */
export function invalidateGalleryCache(bucket: string, path: string) {
  pageCache.delete(cacheKey(bucket, path));
}

/** 读子目录信息（没有返回 null） */
export function readDirInfoCache(
  bucket: string,
  dirPath: string,
): DirInfo | null {
  return dirInfoCache.get(cacheKey(bucket, dirPath)) || null;
}

/**
 * 写子目录信息。
 *
 * 封面 URL 必须稳定：后退时用**同一个 URL**，
 * 浏览器才能命中内存缓存直接秒开，不会重新下载。
 */
export function writeDirInfoCache(
  bucket: string,
  dirPath: string,
  info: DirInfo,
) {
  const k = cacheKey(bucket, dirPath);
  dirInfoCache.set(k, info);
  while (dirInfoCache.size > MAX_COVER_ENTRIES) {
    const oldest = dirInfoCache.keys().next().value;
    if (oldest === undefined) break;
    dirInfoCache.delete(oldest);
  }
}

/**
 * 让某个子目录的信息缓存失效。
 *
 * 封面用的是**带签名的 URL**，签名会过期；
 * 过期后封面 404，靠这个清掉缓存重新取一份新的。
 */
export function invalidateDirInfoCache(bucket: string, dirPath: string) {
  dirInfoCache.delete(cacheKey(bucket, dirPath));
}
