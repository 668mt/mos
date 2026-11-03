import { Button, Empty, Modal, Slider, Spin, message } from 'antd';
import {
  ArrowLeftOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  EyeOutlined,
  FileImageOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  LeftOutlined,
  RightOutlined,
  SelectOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from '@umijs/max';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { deleteResources, fetchResources } from '@/services/api';
import { basename, normalizePath } from '@/utils';
import {
  invalidateDirInfoCache,
  invalidateGalleryCache,
  persistGalleryCache,
  readDirInfoCache,
  readGalleryCache,
  writeDirInfoCache,
  writeGalleryCache,
} from '@/utils/galleryCache';
import type { DirInfo } from '@/utils/galleryCache';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { useGalleryColumns, MAX_COLUMNS } from '@/hooks/useGalleryColumns';
import { useRecentlyViewed, useVisitedDirs } from '@/hooks/useRecentlyViewed';
import { usePerms } from '@/hooks/usePerms';
import type { Dir, DirAndResourceVo } from '@/services/types';

/** 每页拉取数量 */
const PAGE_SIZE = 60;

/** 提前加载距离：距底部 600px 就开始拉下一页 */
const LOAD_MORE_MARGIN = '600px 0px';

/** 缩略图提前加载距离：进入视口前 400px 就开始请求 */
const PREFETCH_MARGIN = '400px 0px';

/** 灯箱底部缩略图条一次渲染多少张（当前图片居中，两边各留一半） */
const STRIP_SIZE = 31;

/** 大图预加载：当前图之后提前拉几张原图 */
const ORIGIN_PREFETCH_COUNT = 5;

/** 大图缩放：每格滚轮的放大倍数 */
const ZOOM_STEP = 1.2;

/** 大图缩放范围（1 = 原始大小） */
const MIN_ZOOM = 0.4;
const MAX_ZOOM = 5;

/** 双击放大到的倍数（再双击一次还原） */
const DOUBLE_CLICK_ZOOM = 2;

/**
 * 单个缩略图的最小宽度。
 *
 * 固定每行张数时，若容器太窄会把图挤成一团，
 * 所以按这个下限反推「最多能放几列」，超出的部分自动少放几列。
 */
const MIN_CELL_WIDTH = 96;

const IMAGE_SUFFIXS = ['.jpg', '.gif', '.jpeg', '.png', '.bmp', '.webp'];

/** 子目录每页拉多少个 */
const SUBDIR_PAGE_SIZE = 60;

/**
 * 挑封面时取前几张来选。
 *
 * 后端只在资源**确实有缩略图**时才返回 `thumbFileHouseId`，
 * 没有缩略图的资源请求 `?thumb=true` 会直接报错。
 * 所以要多取几张，从中挑一张**有缩略图**的当封面。
 */
const COVER_PICK_SIZE = 8;

/**
 * 把 URL 上的 path 参数还原成**原始路径**。
 *
 * path 参数有两种来源：
 * - 前端自己拼的（`encodeURIComponent(原始路径)` 编码一次）→ 浏览器解码一次就是原始路径 ✅
 * - 历史上被**编码过两次**的链接 → 浏览器解码一次后仍然是编码态，
 *   后端拿它去查目录会查不到（目录名带中文/括号/空格时必现）。
 *
 * 后者用「还含 %XX 就再解一次」来兜底。
 * 副作用：路径里本来就有 `%20` 这类字面量的会被误解码 ——
 * 这种目录名极其罕见，认了。
 */
function decodePathParam(p: string): string {
  const raw = p || '/';
  if (!/%[0-9a-fA-F]{2}/.test(raw)) return normalizePath(raw);
  try {
    // ⚠️ 必须**先解码再归一化**：
    // 编码态的路径以 `%2F` 开头，normalizePath 会以为它缺前导斜杠而补一个，
    // 解码后就变成 `//resources/...` 了。
    return normalizePath(decodeURIComponent(raw));
  } catch {
    return normalizePath(raw);
  }
}

interface GalleryImage {
  id: number;
  name: string;
  origin: string;
  thumb: string;
  /** 完整路径，删除确认框里要显示 */
  path: string;
}

/**
 * 懒加载缩略图。
 *
 * 只有滚动到视口附近（`rootMargin` 预加载 400px）才真正发起图片请求，
 * 避免一次性把整个目录的图片全部拉下来。
 */
function LazyThumb({
  src,
  alt,
  onLoad,
}: {
  src: string;
  alt: string;
  onLoad?: () => void;
}) {
  const holderRef = useRef<HTMLSpanElement | null>(null);
  const [inView, setInView] = useState(false);
  const [loaded, setLoaded] = useState(false);
  /** 加载失败（签名过期、文件损坏等）—— 不能一直转骨架 */
  const [failed, setFailed] = useState(false);

  // 换了地址就重新加载
  useEffect(() => {
    setLoaded(false);
    setFailed(false);
  }, [src]);

  useEffect(() => {
    const el = holderRef.current;
    if (!el) return;
    // 浏览器不支持 IntersectionObserver 时退化为直接加载
    if (typeof IntersectionObserver === 'undefined') {
      setInView(true);
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          setInView(true);
          io.disconnect();
        }
      },
      { rootMargin: PREFETCH_MARGIN },
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);

  return (
    <span className="mos-gallery-thumb" ref={holderRef}>
      {inView ? (
        failed ? (
          <span className="mos-gallery-thumb-failed" title="图片加载失败">
            <FileImageOutlined />
          </span>
        ) : (
          <img
            src={src}
            alt={alt}
            loading="lazy"
            decoding="async"
            className={loaded ? 'is-loaded' : ''}
            onLoad={() => {
              setLoaded(true);
              onLoad?.();
            }}
            onError={() => setFailed(true)}
          />
        )
      ) : (
        <span className="mos-gallery-skeleton" aria-hidden="true" />
      )}
    </span>
  );
}

/**
 * 文件夹封面。
 *
 * 只用目录下**一张**图片的缩略图当封面；
 * 没有图片（或加载失败）就退回文件夹图标。
 */
function DirCover({
  cover,
  visited,
  onError,
}: {
  cover: string;
  visited: boolean;
  onError?: () => void;
}) {
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);

  // 换了封面地址就重新尝试（旧的签名过期后父组件会换一份新的）
  useEffect(() => {
    setFailed(false);
    setLoaded(false);
  }, [cover]);

  return (
    <span className="mos-gallery-subdir-cover">
      {cover && !failed ? (
        <img
          src={cover}
          alt=""
          loading="lazy"
          decoding="async"
          className={loaded ? 'is-loaded' : ''}
          onLoad={() => setLoaded(true)}
          onError={() => {
            setFailed(true);
            onError?.();
          }}
        />
      ) : visited ? (
        <FolderOpenOutlined />
      ) : (
        <FolderOutlined />
      )}
    </span>
  );
}

/**
 * 图库查看器。
 *
 * 通过 `?bucket=&path=` 定位目录，
 * 接口：`GET /member/resource/{bucket}/list`（限定图片后缀）
 *
 * 交互：
 * - **翻页加载**：滚动到底部自动拉下一页（每页 60 张）
 * - 缩略图**懒加载**：滚动到视口附近才请求
 * - 点击缩略图打开大图灯箱（不跳新标签页）
 * - 灯箱内左右箭头 / 键盘 ← → / 移动端左右滑动切换图片
 * - Esc 关闭
 * - **删除图片**：缩略图右上角按钮 / 灯箱底部「删除」按钮，软删除进回收站
 * - **批量删除**：顶部「批量删除」进入多选模式，勾选后一次删掉
 */
export default function GalleryViewer() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const bucket = searchParams.get('bucket') || '';
  /** URL 上的原始参数（可能还是编码态） */
  const rawPath = searchParams.get('path') || '/';
  /**
   * 还原成原始路径。
   *
   * 后端查目录用的是**原始路径**，所以这里必须解到位 ——
   * 目录名里带中文/括号/空格时，编码多一次就会查不到。
   */
  const path = useMemo(() => decodePathParam(rawPath), [rawPath]);

  /** 桶权限：删除接口要求 BucketPerm.DELETE */
  const { hasPerm } = usePerms();
  const canDelete = hasPerm(bucket, 'DELETE');

  const [items, setItems] = useState<DirAndResourceVo[]>([]);
  /** 当前目录下的子目录（点进去可以继续看子目录里的图片） */
  const [subDirs, setSubDirs] = useState<DirAndResourceVo[]>([]);
  /** 子目录已加载到第几页 */
  const [subDirPageNum, setSubDirPageNum] = useState(1);
  /** 子目录总数 */
  const [subDirTotal, setSubDirTotal] = useState(0);
  /** 正在加载下一页子目录 */
  const [subDirLoading, setSubDirLoading] = useState(false);
  /** 子目录信息：id → { 封面地址, 图片数量 } */
  const [dirInfos, setDirInfos] = useState<Record<number, DirInfo>>({});
  /**
   * 子目录是否已经拉完了。
   *
   * 子目录和图片是两个独立请求，子目录可能更慢 ——
   * 图片先回来、发现是空目录，但子目录还在路上，
   * 这时候不能急着显示「没有图片」的空状态（会闪一下）。
   */
  const [subDirsLoaded, setSubDirsLoaded] = useState(false);
  /** 面包屑：当前目录的祖先链（从根到直接父级），点任意一级可跳过去 */
  const [parentDirs, setParentDirs] = useState<Dir[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  /** 灯箱当前图片下标，-1 表示未打开 */
  const [activeIndex, setActiveIndex] = useState(-1);
  /**
   * 多选模式。
   *
   * 图库一个目录动辄几百上千张图，逐张点删除不现实，
   * 所以额外提供「批量删除」：进入多选后点缩略图变成勾选，
   * 而不是打开大图。
   */
  const [selectMode, setSelectMode] = useState(false);
  /** 多选模式下已勾选的图片 id（存 id 而不是下标，翻页加载不会错位） */
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  /** 本页是否拉到了空数据（空了就说明没有更多了） */
  const [pageEmpty, setPageEmpty] = useState(false);
  /**
   * 已加载完成的大图下标（-1 表示无）。
   *
   * 记录「下标」而不是布尔值：切换图片时 `activeIndex` 立刻变，
   * 而这个 state 还是上一张的值，两者一比就知道当前图到底加载好没有，
   * 不会闪出上一张的「已加载」状态。
   */
  const [loadedIndex, setLoadedIndex] = useState(-1);
  /** 每行图片数（null = 自适应），从 localStorage 恢复 */
  const { columns, isAuto, setColumns, reset } = useGalleryColumns();
  /** 2 小时内点开看过的图片（图库里标色用） */
  const { isVisited: isViewed, markVisited: markViewed } = useRecentlyViewed();
  /** 2 小时内点进去过的目录（子目录条上标色用） */
  const { isVisited: isDirVisited, markVisited: markDirVisited } =
    useVisitedDirs();
  /** 网格容器宽度，用来把「每行张数」换算成实际列数 */
  const [gridWidth, setGridWidth] = useState(0);

  /** 触摸滑动的起始横坐标 */
  const touchStartX = useRef<number | null>(null);
  /** 已经发起过大图预加载的 URL，避免重复预热 */
  const preloadedRef = useRef<Set<string>>(new Set());
  /**
   * 当前目录的数据快照。
   *
   * 为什么不用 state 写缓存：请求回调是异步的，那时读到的 state 是**旧值**，
   * 会把上一个目录的数据写进新目录的缓存。
   * 所以这里用 ref 存一份与 state 同步的快照，写缓存一律读它。
   *
   * `path` 用来判断这份快照是不是当前目录的 —— 切换目录的那一瞬间，
   * state 还是旧目录的，不能拿去写新目录的缓存。
   */
  const snapRef = useRef<{
    path: string;
    items: DirAndResourceVo[];
    total: number;
    pageNum: number;
    pageEmpty: boolean;
    subDirs: DirAndResourceVo[];
    subDirPageNum: number;
    subDirTotal: number;
    parentDirs: Dir[];
  }>({
    path: '',
    items: [],
    total: 0,
    pageNum: 1,
    pageEmpty: false,
    subDirs: [],
    subDirPageNum: 1,
    subDirTotal: 0,
    parentDirs: [],
  });
  /** 当前灯箱下标（删除回调是异步的，要读最新值） */
  const activeIndexRef = useRef(-1);
  /** 大图缩放比例（1 = 原始大小） */
  const [zoom, setZoom] = useState(1);
  /** 缩放后的平移偏移 */
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  /** 是否正在拖动（拖动时不要加过渡动画，否则会粘手） */
  const [dragging, setDragging] = useState(false);
  /**
   * 切换方向：1 = 下一张，-1 = 上一张，0 = 首次打开。
   *
   * 只用来决定入场动画的方向（往哪边滑入），不参与任何逻辑。
   */
  const [slideDir, setSlideDir] = useState(0);
  /** zoom / offset 的最新值（滚轮回调里要用，读 state 会拿到旧值） */
  const zoomRef = useRef(1);
  const offsetRef = useRef({ x: 0, y: 0 });
  /** 拖动起点 */
  const dragRef = useRef<{
    startX: number;
    startY: number;
    baseX: number;
    baseY: number;
  } | null>(null);
  /** 封面加载失败的重试次数（目录 id -> 次数），防止无限重试造成死循环 */
  const coverRetryRef = useRef<Map<number, number>>(new Map());
  /** 正文容器的滚动位置（写缓存用） */
  const scrollTopRef = useRef(0);
  /** 到底部自动加载的哨兵元素 */
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  /** 子目录条末尾的哨兵（滚动加载下一页子目录） */
  const subDirSentinelRef = useRef<HTMLDivElement | null>(null);
  /** 灯箱底部的缩略图条 */
  const stripRef = useRef<HTMLDivElement | null>(null);
  /** 缩略图条中当前图片对应的按钮 */
  const activeStripRef = useRef<HTMLButtonElement | null>(null);
  /** 灯箱根节点（滚轮事件的绑定目标） */
  const lightboxRef = useRef<HTMLDivElement | null>(null);
  /** 可滚动的正文容器，切换目录后要回到顶部 */
  const bodyRef = useRef<HTMLDivElement | null>(null);

  /**
   * 把当前快照写进缓存。
   *
   * 只写「快照属于当前目录」的情况 —— 切换目录的那一瞬间，
   * 快照还是上一个目录的，不能拿去写新目录的缓存。
   */
  const writeCache = useCallback(() => {
    const snap = snapRef.current;
    if (snap.path !== path) return;
    // 还没加载出内容就别写 —— 否则会把「空目录」缓存下来，
    // 下次进来命中缓存却是空的
    if (snap.items.length === 0 && snap.subDirs.length === 0) return;
    writeGalleryCache(bucket, path, {
      items: snap.items,
      total: snap.total,
      pageNum: snap.pageNum,
      pageEmpty: snap.pageEmpty,
      subDirs: snap.subDirs,
      subDirPageNum: snap.subDirPageNum,
      subDirTotal: snap.subDirTotal,
      parentDirs: snap.parentDirs,
      scrollTop: scrollTopRef.current,
    });
  }, [bucket, path]);

  /** 拉取指定页；append=true 时追加到已有列表 */
  const loadPage = useCallback(
    async (targetPage: number, append: boolean) => {
      if (!bucket) return;
      if (append) setLoadingMore(true);
      else setLoading(true);
      try {
        const result = await fetchResources(bucket, {
          pageNum: targetPage,
          pageSize: PAGE_SIZE,
          path: normalizePath(path),
          suffixs: IMAGE_SUFFIXS,
          isFile: true,
          sortField: 'name',
          sortOrder: 'ascend',
        });
        const list = result?.resources?.list || [];
        const listTotal = result?.resources?.total || 0;
        // 用快照算下一份列表，避免闭包里的旧 items 把新加载的图片弄丢
        const snap = snapRef.current;
        const nextItems = append ? [...snap.items, ...list] : list;
        const nextParentDirs = append ? snap.parentDirs : result?.parentDirs || [];

        snapRef.current = {
          path,
          items: nextItems,
          total: listTotal,
          pageNum: targetPage,
          pageEmpty: list.length === 0,
          subDirs: snap.subDirs,
          subDirPageNum: snap.subDirPageNum,
          subDirTotal: snap.subDirTotal,
          parentDirs: nextParentDirs,
        };

        setItems(nextItems);
        setPageNum(targetPage);
        setTotal(listTotal);
        setPageEmpty(list.length === 0);
        // 面包屑由同一个接口顺带返回，不用再发一次请求
        if (!append) setParentDirs(nextParentDirs);
        setError('');
        writeCache();
      } catch {
        if (!append) {
          setError('图库加载失败');
          // 目录都打不开，面包屑没有意义，别留着上一个目录的
          setParentDirs([]);
          snapRef.current = {
            ...snapRef.current,
            path,
            items: [],
            total: 0,
            pageNum: 1,
            pageEmpty: true,
            parentDirs: [],
          };
        }
        setPageEmpty(true);
      } finally {
        if (append) setLoadingMore(false);
        else setLoading(false);
      }
    },
    [bucket, path, writeCache],
  );

  /**
   * 目录 / 桶变化时重新加载。
   *
   * **先查缓存**：浏览器「后退」回到上一个目录时，
   * 直接把缓存里的数据灌回 state —— 不重新请求列表，
   * 封面地址也和上次完全一致（浏览器命中内存缓存，秒开不重下）。
   */
  useEffect(() => {
    const cached = readGalleryCache(bucket, path);

    /**
     * 离开这个目录时把快照（含滚动位置）写进缓存。
     *
     * ⚠️ 必须**在两个分支都注册**：
     * 之前只在「没有缓存」的分支注册了 cleanup，
     * 结果「从缓存恢复的目录」离开时不会更新缓存 ——
     * 用户在这个目录里加载了更多页，后退回来又只剩第一页。
     */
    const onLeave = () => {
      writeCache();
      persistGalleryCache();
    };

    if (cached) {
      // 命中缓存：整个目录的数据直接灌回来，不发任何请求
      console.debug('[mos-gallery] 目录缓存命中', bucket, path);
      // 快照和 state 一起灌回去（快照是写缓存用的）
      snapRef.current = {
        path,
        items: cached.items,
        total: cached.total,
        pageNum: cached.pageNum,
        pageEmpty: cached.pageEmpty,
        subDirs: cached.subDirs,
        subDirPageNum: cached.subDirPageNum || 1,
        subDirTotal: cached.subDirTotal || cached.subDirs.length,
        parentDirs: cached.parentDirs,
      };
      setItems(cached.items);
      setTotal(cached.total);
      setPageNum(cached.pageNum);
      setPageEmpty(cached.pageEmpty);
      setSubDirs(cached.subDirs);
      setSubDirPageNum(cached.subDirPageNum || 1);
      setSubDirTotal(cached.subDirTotal || cached.subDirs.length);
      setSubDirsLoaded(true);
      setParentDirs(cached.parentDirs);
      // 子目录的封面/数量也**同步**灌回来：
      // 少了这一步封面要等异步查完才出现，后退时会「闪一下」
      const infos: Record<number, DirInfo> = {};
      cached.subDirs.forEach((d) => {
        const info = readDirInfoCache(bucket, d.path);
        if (info) infos[d.id] = info;
      });
      if (Object.keys(infos).length > 0) {
        setDirInfos((prev) => ({ ...prev, ...infos }));
      }
      setActiveIndex(-1);
      setLoadedIndex(-1);
      setSelectMode(false);
      setSelectedIds(new Set());
      setError('');
      setLoading(false);
      scrollTopRef.current = cached.scrollTop;
      // 恢复之前的滚动位置（要等列表渲染出来）
      requestAnimationFrame(() => {
        bodyRef.current?.scrollTo({ top: cached.scrollTop });
      });
      return onLeave;
    }

    // 没有缓存：先把快照标记成新目录（旧的子目录数据不能带过来）
    snapRef.current = {
      path,
      items: [],
      total: 0,
      pageNum: 1,
      pageEmpty: false,
      subDirs: [],
      subDirPageNum: 1,
      subDirTotal: 0,
      parentDirs: [],
    };
    setItems([]);
    setPageNum(1);
    setTotal(0);
    setPageEmpty(false);
    setSubDirs([]);
    setSubDirPageNum(1);
    setSubDirTotal(0);
    // 新目录的子目录还没拉回来，先别显示空状态
    setSubDirsLoaded(false);
    setActiveIndex(-1);
    // 换目录后图片列表整体变了，已加载的下标不再有意义
    setLoadedIndex(-1);
    // 换目录后勾选的图片已经不在当前列表里，多选状态要清掉
    setSelectMode(false);
    setSelectedIds(new Set());
    // 换目录后回到顶部，不然会停在新目录的中间
    scrollTopRef.current = 0;
    bodyRef.current?.scrollTo({ top: 0 });
    loadPage(1, false);

    return onLeave;
  }, [bucket, path, loadPage, writeCache]);

  /**
   * 拉取当前目录下的子目录。
   *
   * 单独发一次请求而不是和图片混在一起：图片要分页懒加载，目录是一次性的小列表，
   * 混在一起会让「图片总数」和翻页逻辑都变复杂。
   * 后端 `isDir=true` 会把文件分支置成 `1 = 2`，只返回目录。
   */
  /**
   * 拉取子目录。
   *
   * `append=true` 时追加到已有列表（滚动加载下一页）。
   */
  const loadSubDirs = useCallback(
    async (targetPage: number, append: boolean) => {
      if (!bucket) return;
      // 有缓存就不用再请求了（后退回来时）
      if (!append) {
        const cached = readGalleryCache(bucket, path);
        if (cached) {
          setSubDirs(cached.subDirs);
          setSubDirPageNum(cached.subDirPageNum || 1);
          setSubDirTotal(cached.subDirTotal || cached.subDirs.length);
          setSubDirsLoaded(true);
          return;
        }
      }
      if (append) setSubDirLoading(true);
      try {
        const result = await fetchResources(bucket, {
          pageNum: targetPage,
          pageSize: SUBDIR_PAGE_SIZE,
          path: normalizePath(path),
          isDir: true,
          sortField: 'name',
          sortOrder: 'ascend',
        });
        const list = result?.resources?.list || [];
        const total = result?.resources?.total || 0;
        setSubDirs((prev) => (append ? [...prev, ...list] : list));
        setSubDirPageNum(targetPage);
        setSubDirTotal(total);
        const snap = snapRef.current;
        if (snap.path === path) {
          snapRef.current = {
            ...snap,
            subDirs: append ? [...snap.subDirs, ...list] : list,
            subDirPageNum: targetPage,
            subDirTotal: total,
          };
          writeCache();
        }
      } catch {
        // 错误提示已在拦截器中处理；子目录拉不到时只是不显示，不影响看图
        if (!append) setSubDirs([]);
      } finally {
        if (append) setSubDirLoading(false);
        setSubDirsLoaded(true);
      }
    },
    [bucket, path, writeCache],
  );

  useEffect(() => {
    loadSubDirs(1, false);
  }, [loadSubDirs]);

  /** 子目录还有没有下一页 */
  const hasMoreSubDirs = subDirs.length < subDirTotal;

  /** 加载下一页子目录 */
  const loadMoreSubDirs = useCallback(() => {
    if (subDirLoading || !hasMoreSubDirs) return;
    loadSubDirs(subDirPageNum + 1, true);
  }, [subDirLoading, hasMoreSubDirs, subDirPageNum, loadSubDirs]);

  // 滚到子目录条末尾时自动加载下一页
  useEffect(() => {
    const el = subDirSentinelRef.current;
    if (!el) return;
    if (typeof IntersectionObserver === 'undefined') return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) loadMoreSubDirs();
      },
      { rootMargin: LOAD_MORE_MARGIN },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [loadMoreSubDirs]);

  /**
   * 拉取每个子目录的**封面**和**图片数量**。
   *
   * 一次请求两用：`GET /member/resource/{bucket}/list`（path=子目录路径、
   * suffixs=图片后缀、pageSize=1）返回的 `resources.total` 就是该目录的**图片总数**，
   * `list[0]` 就是封面那张图。
   *
   * 一次要发 N 个请求（每个子目录一个），所以：
   * - **命中缓存的同步填进 state**（不等异步）—— 否则封面会先显示图标、
   *   再变成图片，后退时看着「闪了一下」；
   * - 只请求**没有缓存**的那些；
   * - 剩下的**分批并发**（每批 6 个），别一次打几十个并发把连接池占满。
   */
  const loadDirInfos = useCallback(
    async (dirs: DirAndResourceVo[]) => {
      if (!bucket || dirs.length === 0) return;

      // 先把命中缓存的同步用上（不 await，界面立刻就有封面）
      const cachedInfos: Record<number, DirInfo> = {};
      const missing: DirAndResourceVo[] = [];
      dirs.forEach((d) => {
        const cached = readDirInfoCache(bucket, d.path);
        if (cached) cachedInfos[d.id] = cached;
        else missing.push(d);
      });
      if (Object.keys(cachedInfos).length > 0) {
        setDirInfos((prev) => ({ ...prev, ...cachedInfos }));
      }
      if (missing.length === 0) return;

      const BATCH = 4;
      for (let i = 0; i < missing.length; i += BATCH) {
        const batch = missing.slice(i, i + BATCH);
        const results = await Promise.all(
          batch.map(async (d): Promise<[number, DirInfo]> => {
            try {
              // 两个请求并行：
              // ① 图片（带后缀过滤）→ 图片数量 + 封面拼贴用的几张图
              // ② 子目录（isDir）→ 子文件夹数量
              //    光有图片数不够：目录里可能全是子文件夹，
              //    只显示「0 张」会让人以为这目录是空的。
              const [imgRes, dirRes] = await Promise.all([
                fetchResources(bucket, {
                  pageNum: 1,
                  // 多取几张拼贴用；只挑**有缩略图**的 ——
                  // 没有缩略图的资源请求 `?thumb=true` 会直接报错（NoThumbBizException）
                  pageSize: COVER_PICK_SIZE,
                  path: d.path,
                  suffixs: IMAGE_SUFFIXS,
                  isFile: true,
                  sortField: 'name',
                  sortOrder: 'ascend',
                }),
                fetchResources(bucket, {
                  pageNum: 1,
                  pageSize: 1,
                  path: d.path,
                  isDir: true,
                  sortField: 'name',
                  sortOrder: 'ascend',
                }),
              ]);
              const list = imgRes?.resources?.list || [];
              // 只取**一张**有缩略图的当封面
              const pick = list.find((it) => it.thumbFileHouseId);
              const info: DirInfo = {
                cover: pick
                  ? pick.signUrl
                    ? `${pick.signUrl}&thumb=true`
                    : `/mos/${bucket}${pick.urlEncodePath}?thumb=true`
                  : '',
                imageCount: imgRes?.resources?.total || 0,
                dirCount: dirRes?.resources?.total || 0,
              };
              writeDirInfoCache(bucket, d.path, info);
              return [d.id, info];
            } catch {
              // 取不到就当空目录，不影响看图
              return [d.id, { cover: '', imageCount: 0, dirCount: 0 }];
            }
          }),
        );
        setDirInfos((prev) => {
          const next = { ...prev };
          results.forEach(([id, info]) => {
            next[id] = info;
          });
          return next;
        });
      }
    },
    [bucket],
  );

  useEffect(() => {
    loadDirInfos(subDirs);
  }, [subDirs, loadDirInfos]);

  /**
   * 封面 404 了 —— 多半是签名 URL 过期（或这张图压根没有缩略图）。
   *
   * 清掉这个子目录的缓存重新取一份新签名。
   *
   * ⚠️ **必须限制重试次数**：签名 URL 每次都不一样，
   * 无限制重试就是「请求 → 404 → 再请求 → 再 404」的死循环。
   * 每个目录最多重试一次，之后就认了（显示文件夹图标）。
   */
  const onCoverError = useCallback(
    (dir: DirAndResourceVo) => {
      const tries = (coverRetryRef.current.get(dir.id) || 0) + 1;
      coverRetryRef.current.set(dir.id, tries);
      if (tries > 1) return;
      invalidateDirInfoCache(bucket, dir.path);
      setDirInfos((prev) => {
        if (!(dir.id in prev)) return prev;
        const next = { ...prev };
        delete next[dir.id];
        return next;
      });
      loadDirInfos([dir]);
    },
    [bucket, loadDirInfos],
  );

  /**
   * 来源页（资源管理页）地址。
   *
   * 只接受**站内路径**：这个值会进 URL、也会被 `location.replace` 用掉，
   * 不做校验就等于留了个跳转外站的口子。
   */
  const fromPath = useMemo(() => {
    const from = searchParams.get('from');
    return from && from.startsWith('/') ? from : '';
  }, [searchParams]);

  /**
   * 跳到某个子目录。
   *
   * 用 **push** 而不是 replace —— 浏览器「后退」要能回到上一级目录。
   *
   * ⚠️ 这里只能传**原始路径**：`setSearchParams` 内部用 `URLSearchParams.toString()`
   * 序列化，会自动把值编码一次。自己再编码一遍就是**双重编码**，
   * 浏览器解码一次后拿到的还是编码态，后端查目录直接查不到。
   *
   * 传了 `targetId` 就顺手记一下「这个目录刚进过」，
   * 回到父目录时它会带上「进过」的标记。
   */
  const goToDir = useCallback(
    (targetPath: string, targetId?: number) => {
      const next: Record<string, string> = {
        bucket,
        path: normalizePath(targetPath),
      };
      // 来源页要带着走，返回按钮才能回到原来的列表
      if (fromPath) next.from = fromPath;
      if (targetId !== undefined && targetId !== null) {
        markDirVisited(targetId);
      }
      setSearchParams(next);
    },
    [bucket, fromPath, markDirVisited, setSearchParams],
  );

  /**
   * 返回。
   *
   * 图库通常是**新标签页**打开的（资源管理页用 `window.open`），
   * 这种标签页没有历史记录，`history.back()` 什么都不会发生 —— 这就是「点了没反应」的原因。
   * 所以优先关掉这个标签页（回到打开它的那个页面）。
   */
  const goBack = useCallback(() => {
    const fallback = `/resource/files?b=${encodeURIComponent(bucket)}`;

    // 由脚本打开的标签页：直接关掉，回到来源页
    if (window.opener) {
      window.close();
      // 万一被浏览器拦截了（理论上不会），兜底跳走，避免"点了没反应"
      window.setTimeout(() => {
        if (!window.closed) window.location.replace(fromPath || fallback);
      }, 300);
      return;
    }
    /*
     * 同标签页：**用路由跳转**，不要 `location.replace`。
     *
     * replace 有两个问题：
     * ① 整页刷新 → 内存缓存全丢（后退回来又要重新拉一遍）
     * ② 吃掉历史条目 → 用户再点浏览器「后退」就回不到图库页了
     */
    if (fromPath) {
      navigate(fromPath);
      return;
    }
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    navigate(fallback);
  }, [bucket, fromPath, navigate]);

  /** 是否还有下一页 */
  const hasMore = !pageEmpty && items.length < total;

  /** 加载下一页 */
  const loadMore = useCallback(() => {
    if (loading || loadingMore || pageEmpty) return;
    if (items.length >= total && total > 0) return;
    loadPage(pageNum + 1, true);
  }, [loading, loadingMore, pageEmpty, items.length, total, pageNum, loadPage]);

  // 滚到底部自动加载下一页
  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    if (typeof IntersectionObserver === 'undefined') return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          loadMore();
        }
      },
      { rootMargin: LOAD_MORE_MARGIN },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [loadMore]);

  /** 整理成灯箱用的图片列表 */
  const images: GalleryImage[] = useMemo(
    () =>
      items.map((item) => {
        const origin = item.signUrl || `/mos/${bucket}${item.urlEncodePath}`;
        return {
          id: item.id,
          name: item.fileName,
          origin,
          thumb: item.thumbFileHouseId ? `${origin}&thumb=true` : origin,
          path: item.path,
        };
      }),
    [items, bucket],
  );

  // 把最新值同步进 ref，供删除回调读取（避免异步确认框里拿到陈旧闭包）
  snapRef.current.items = items;
  activeIndexRef.current = activeIndex;

  /**
   * 把已删除的图片从本地列表里摘掉，并修正灯箱下标。
   *
   * 不重新拉列表 —— 目录可能有上千张图，重新拉会丢掉已经滚动加载出来的部分。
   * 单张删除和批量删除共用这段逻辑。
   */
  const removeImages = useCallback((deletedIds: number[]) => {
    if (deletedIds.length === 0) return;
    const ids = new Set(deletedIds);
    const list = snapRef.current.items;
    const idx = activeIndexRef.current;

    // 用函数式更新，避免闭包里的旧 items 把新加载的图片弄丢
    setItems((prev) => prev.filter((i) => !ids.has(i.id)));
    setTotal((prev) => Math.max(0, prev - deletedIds.length));
    // 快照同步去掉删掉的图，否则会把「已经不存在的图」写回缓存
    snapRef.current.items = list.filter((i) => !ids.has(i.id));
    // 这个目录的缓存里有已删的图，必须失效，否则后退回来还能看到它们
    invalidateGalleryCache(bucket, snapRef.current.path);

    if (idx < 0) return;
    const current = list[idx];
    if (!current) return;

    /** 旧下标 → 新下标：扣掉它前面被删掉的张数 */
    const toNewIndex = (oldIndex: number) => {
      let removedBefore = 0;
      for (let i = 0; i < oldIndex; i++) {
        if (ids.has(list[i].id)) removedBefore++;
      }
      return oldIndex - removedBefore;
    };

    if (!ids.has(current.id)) {
      // 当前图还在，只是前面少了几张 → 下标前移。
      // 图片地址没变，浏览器不会重新加载、onLoad 也不会再触发，
      // 所以要把「已加载」标记跟着挪到新下标，否则会一直卡在「大图加载中」。
      const next = toNewIndex(idx);
      setActiveIndex(next);
      setLoadedIndex(next);
      // 删完图会切到别的大图，平移要复位（缩放比例保持）
      setOffset({ x: 0, y: 0 });
      dragRef.current = null;
      setDragging(false);
      return;
    }

    // 删的正是灯箱当前这张：切到相邻的一张（优先下一张，没有则上一张）
    let target = -1;
    for (let i = idx + 1; i < list.length; i++) {
      if (!ids.has(list[i].id)) {
        target = i;
        break;
      }
    }
    if (target < 0) {
      for (let i = idx - 1; i >= 0; i--) {
        if (!ids.has(list[i].id)) {
          target = i;
          break;
        }
      }
    }
    setActiveIndex(target >= 0 ? toNewIndex(target) : -1);
    // 清掉「已加载」标记：新图会重新触发 onLoad，否则会闪出上一张的加载完成状态
    setLoadedIndex(-1);
    // 删完图会切到别的大图，平移要复位（缩放比例保持）
    setOffset({ x: 0, y: 0 });
    dragRef.current = null;
    setDragging(false);
  }, []);

  /**
   * 删除图片（软删除，进回收站）。
   *
   * 删完就地把它从本地列表里摘掉并把总数减一，不重新拉列表 ——
   * 目录可能有上千张图，重新拉会丢掉已经滚动加载出来的部分。
   */
  const onDelete = useCallback(
    (target: { id: number; path: string }) => {
      Modal.confirm({
        title: '确定删除这张图片吗?',
        content: (
          <div>
            <div style={{ wordBreak: 'break-all' }}>{target.path}</div>
            <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
              删除后会移入回收站，可在回收站中还原
            </div>
          </div>
        ),
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          await deleteResources(bucket, { fileIds: target.id });
          message.success('删除成功');
          removeImages([target.id]);
        },
      });
    },
    [bucket, removeImages],
  );

  /** 多选模式下切换勾选 */
  const toggleSelect = useCallback((id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  /** 退出多选模式并清空勾选 */
  const exitSelectMode = useCallback(() => {
    setSelectMode(false);
    setSelectedIds(new Set());
  }, []);

  /**
   * 批量删除勾选的图片。
   *
   * fileIds 传**逗号分隔的字符串**：后端 `ResourceController.del` 的
   * `Long[] fileIds` 是普通参数（不是 @RequestParam），逗号分隔能被 Spring
   * 绑成数组；传数组会被 axios 序列化成 `fileIds[]=1`，Spring 认不出来。
   */
  const onBatchDelete = useCallback(() => {
    const targets = snapRef.current.items.filter((i) => selectedIds.has(i.id));
    if (targets.length === 0) return;
    const names = targets.map((t) => t.path);
    Modal.confirm({
      title: `确定删除选中的 ${targets.length} 张图片吗?`,
      content: (
        <div>
          <div style={{ wordBreak: 'break-all' }}>
            {names.slice(0, 10).join('、')}
            {names.length > 10 ? ` 等 ${names.length} 张` : ''}
          </div>
          <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
            删除后会移入回收站，可在回收站中还原
          </div>
        </div>
      ),
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteResources(bucket, {
          fileIds: targets.map((t) => t.id).join(','),
        });
        message.success(`已删除 ${targets.length} 张图片`);
        removeImages(targets.map((t) => t.id));
        setSelectedIds(new Set());
        setSelectMode(false);
      },
    });
  }, [bucket, selectedIds, removeImages]);

  /**
   * 预加载后面几张大图。
   *
   * 灯箱里翻大图时，每次都要等一张大图从零加载，体验很慢。
   * 这里提前把后面几张的原图塞进浏览器缓存，翻到时基本是秒开。
   *
   * 用 `new Image()` 发起请求即可 —— 浏览器会按 URL 缓存响应，
   * 后面真正渲染 `<img src={origin}>` 时直接命中缓存。
   *
   * `preloadedRef` 记录已发起过的 URL，避免列表刷新时重复预热。
   */
  useEffect(() => {
    if (activeIndex < 0) return;
    const end = Math.min(activeIndex + ORIGIN_PREFETCH_COUNT + 1, images.length);
    for (let i = activeIndex + 1; i < end; i++) {
      const origin = images[i]?.origin;
      if (!origin || preloadedRef.current.has(origin)) continue;
      preloadedRef.current.add(origin);
      const pre = new Image();
      pre.decoding = 'async';
      pre.src = origin;
    }
  }, [activeIndex, images]);

  /**
   * 记录正文容器的滚动位置。
   *
   * 只写 ref 不写 state —— 滚动事件很频繁，触发渲染会卡；
   * 写缓存时统一读这个 ref。
   */
  const handleScroll = useCallback(() => {
    const el = bodyRef.current;
    if (!el) return;
    scrollTopRef.current = el.scrollTop;
  }, []);

  /**
   * 记录「刚看过」的图片。
   *
   * 灯箱里显示过的那张就算看过（点开看过、翻页翻到过都算），
   * 2 小时内在缩略图上标个颜色，免得在一堆图里分不清哪些已经看过。
   */
  useEffect(() => {
    if (activeIndex < 0) return;
    const img = images[activeIndex];
    if (img) markViewed(img.id);
  }, [activeIndex, images, markViewed]);

  /**
   * 测量网格容器宽度。
   *
   * 「每行 N 张」不能直接写死成 `repeat(N, 1fr)` —— 窄屏上 N 张会被挤扁。
   * 这里测出容器宽度，再按缩略图最小宽度反推实际能放几列。
   *
   * 用「state 存元素」而不是 ref + useEffect：
   * 网格是条件渲染的（没加载出来时显示 Empty），
   * 首次挂载时元素还不存在，ref 拿到的会是 null 就永远测不到宽度。
   */
  const [gridEl, setGridEl] = useState<HTMLDivElement | null>(null);
  useEffect(() => {
    if (!gridEl) return;
    const update = () => setGridWidth(gridEl.getBoundingClientRect().width);
    update();
    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', update);
      return () => window.removeEventListener('resize', update);
    }
    const ro = new ResizeObserver(update);
    ro.observe(gridEl);
    return () => ro.disconnect();
  }, [gridEl]);

  /**
   * 网格的列模板。
   *
   * - 自适应（`columns === null`）：交给 CSS 的 `auto-fill` 决定
   * - 指定张数：按容器宽度算出最多能放几列，取「用户想要的」与「能放下的」较小值
   */
  const gridTemplateColumns = useMemo(() => {
    if (columns === null || gridWidth <= 0) return undefined;
    const gap = 12;
    // (n - 1) * gap 是列间距总占用，每列至少 MIN_CELL_WIDTH 宽
    const maxColumns = Math.max(
      1,
      Math.floor((gridWidth + gap) / (MIN_CELL_WIDTH + gap)),
    );
    const actual = Math.min(columns, maxColumns);
    return `repeat(${actual}, minmax(0, 1fr))`;
  }, [columns, gridWidth]);

  /** 目录名（根目录用桶名兜底） */
  const dirName = useMemo(() => {
    const p = normalizePath(path);
    if (p === '/') return bucket || '图库';
    return basename(p);
  }, [path, bucket]);

  /**
   * 面包屑：桶 → 各级祖先目录 → 当前目录。
   *
   * `parentDirs` 是后端随列表接口一起返回的祖先链（从根到直接父级），
   * 点任意一级都能跳过去。
   */
  const crumbs = useMemo(() => {
    const list: {
      key: string;
      label: string;
      path: string;
      id?: number;
    }[] = [{ key: 'root', label: bucket || '根目录', path: '/' }];
    parentDirs
      .filter((d) => d.path && d.path !== '/')
      .forEach((d) => {
        list.push({
          key: `d${d.id}`,
          label: d.name || basename(d.path),
          path: d.path,
          id: d.id,
        });
      });
    const cur = normalizePath(path);
    if (cur !== '/') {
      list.push({ key: 'current', label: basename(cur), path: cur });
    }
    return list;
  }, [bucket, parentDirs, path]);

  // 浏览器标题：目录名 + 图片数量
  useDocumentTitle(
    loading && items.length === 0
      ? `${dirName} · 加载中…`
      : `${dirName} · ${total || items.length} 张图片`,
  );

  /**
   * 只复位平移偏移，**缩放比例保持不变**。
   *
   * 切换图片时用：放大状态下翻页，比例要延续，
   * 但平移要归零 —— 新图的内容位置和上一张不同，居中显示才对。
   */
  const resetOffset = useCallback(() => {
    offsetRef.current = { x: 0, y: 0 };
    setOffset({ x: 0, y: 0 });
    dragRef.current = null;
    setDragging(false);
  }, []);

  /** 完全复位缩放（关闭灯箱时用） */
  const resetZoom = useCallback(() => {
    zoomRef.current = 1;
    setZoom(1);
    resetOffset();
  }, [resetOffset]);

  const goPrev = useCallback(() => {
    setActiveIndex((prev) => (prev > 0 ? prev - 1 : prev));
    setSlideDir(-1);
    resetOffset();
  }, [resetOffset]);

  const goNext = useCallback(() => {
    setActiveIndex((prev) =>
      prev >= 0 && prev < images.length - 1 ? prev + 1 : prev,
    );
    setSlideDir(1);
    resetOffset();
  }, [images.length, resetOffset]);

  /**
   * 关闭灯箱。
   *
   * 同时把已加载下标清掉：重新打开同一张图时，`<img>` 是新挂载的元素，
   * 需要重新走一次 onLoad，否则会直接命中旧的「已加载」状态而跳过 loading。
   */
  const close = useCallback(() => {
    setActiveIndex(-1);
    setLoadedIndex(-1);
    resetZoom();
  }, [resetZoom]);

  // 灯箱打开时锁定页面滚动
  useEffect(() => {
    if (activeIndex < 0) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [activeIndex]);

  // 键盘操作：← → ↑ ↓ 切换，Esc 关闭
  useEffect(() => {
    if (activeIndex < 0) return;
    const onKeyDown = (e: KeyboardEvent) => {
      // 上下左右都能翻上一张 / 下一张
      if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
        e.preventDefault();
        goPrev();
      } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
        e.preventDefault();
        goNext();
      } else if (e.key === 'Escape') {
        e.preventDefault();
        close();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [activeIndex, goPrev, goNext, close]);

  /**
   * 鼠标滚轮：**缩放**大图（向上放大 / 向下缩小）。
   *
   * - 翻页改用键盘（← → ↑ ↓）和左右箭头按钮，滚轮专门用来缩放。
   * - 缩略图条上不接管滚轮，否则没法横向滚动缩略图条。
   * - 必须 `passive: false` 才能 preventDefault —— 否则背后的图集列表会跟着滚。
   */
  useEffect(() => {
    const el = lightboxRef.current;
    if (!el || activeIndex < 0) return;
    const onWheel = (e: WheelEvent) => {
      // 缩略图条上不拦截，保留其自身的滚动能力
      if ((e.target as HTMLElement)?.closest?.('.mos-lightbox-strip')) return;
      e.preventDefault();
      const delta =
        Math.abs(e.deltaY) >= Math.abs(e.deltaX) ? e.deltaY : e.deltaX;
      if (Math.abs(delta) < 1) return;
      const factor = delta < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
      const current = zoomRef.current;
      const next = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, current * factor));
      if (next === current) return;
      zoomRef.current = next;
      setZoom(next);
      // 缩回原始大小时把平移偏移复位，否则图片会停在一边
      if (next <= 1) {
        offsetRef.current = { x: 0, y: 0 };
        setOffset({ x: 0, y: 0 });
      }
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [activeIndex]);

  /**
   * 放大后可以按住鼠标拖动查看局部。
   *
   * 监听挂在 window 上 —— 鼠标拖出图片区域也要能继续拖。
   */
  useEffect(() => {
    if (activeIndex < 0 || zoom <= 1) return;
    const onMove = (e: MouseEvent) => {
      const drag = dragRef.current;
      if (!drag) return;
      const next = {
        x: drag.baseX + (e.clientX - drag.startX),
        y: drag.baseY + (e.clientY - drag.startY),
      };
      offsetRef.current = next;
      setOffset(next);
    };
    const onUp = () => {
      if (!dragRef.current) return;
      dragRef.current = null;
      setDragging(false);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, [activeIndex, zoom]);

  const active = activeIndex >= 0 ? images[activeIndex] : undefined;

  /**
   * 当前大图是否已加载完成。
   *
   * 用下标比对而不是布尔 state：切图时 `activeIndex` 立刻变，
   * 布尔 state 要等下一个 effect 才重置，中间会闪一帧「已加载」。
   */
  const originLoaded = activeIndex >= 0 && loadedIndex === activeIndex;

  /**
   * 底部缩略图条：只渲染当前图片附近的一小段，避免上千张图全进 DOM。
   * 窗口随当前图片滑动。
   */
  const stripStart = useMemo(() => {
    if (activeIndex < 0) return 0;
    const half = Math.floor(STRIP_SIZE / 2);
    return Math.max(0, Math.min(activeIndex - half, images.length - STRIP_SIZE));
  }, [activeIndex, images.length]);
  const strip = images.slice(stripStart, stripStart + STRIP_SIZE);

  // 缩略图条自动把当前图片滚到中间
  useEffect(() => {
    const stripEl = stripRef.current;
    const activeEl = activeStripRef.current;
    if (!stripEl || !activeEl) return;
    const target =
      activeEl.offsetLeft - stripEl.clientWidth / 2 + activeEl.clientWidth / 2;
    stripEl.scrollTo({ left: Math.max(0, target), behavior: 'smooth' });
  }, [activeIndex, stripStart]);

  return (
    <div className="mos-viewer">
      <div className="mos-viewer-header">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={goBack}
          style={{ color: '#fff' }}
          aria-label="返回"
        />
        <span className="mos-viewer-title">
          {bucket} {path}
        </span>
        <span style={{ flex: 1 }} />

        {/* 批量删除：进入多选模式后点缩略图变成勾选 */}
        {canDelete &&
          (selectMode ? (
            <>
              <span className="mos-gallery-selected-tip">
                已选 {selectedIds.size} 张
              </span>
              <Button
                type="text"
                size="small"
                className="mos-gallery-batch-btn mos-gallery-batch-del"
                icon={<DeleteOutlined />}
                disabled={selectedIds.size === 0}
                onClick={onBatchDelete}
              >
                删除所选
              </Button>
              <Button
                type="text"
                size="small"
                className="mos-gallery-batch-btn"
                onClick={exitSelectMode}
              >
                取消
              </Button>
            </>
          ) : (
            <Button
              type="text"
              size="small"
              className="mos-gallery-batch-btn"
              icon={<SelectOutlined />}
              onClick={() => {
                setSelectMode(true);
                setSelectedIds(new Set());
              }}
            >
              批量删除
            </Button>
          ))}

        {/* 每行张数调节：滑块 + 重置 */}
        <div className="mos-gallery-columns">
          <span className="mos-gallery-columns-label">每行</span>
          <Slider
            className="mos-gallery-columns-slider"
            min={0}
            max={MAX_COLUMNS}
            value={columns ?? 0}
            tooltip={{ formatter: (v) => (v === 0 ? '自适应' : `${v} 张`) }}
            onChange={(v) => {
              // 滑到最左端（0）= 恢复自适应
              if (v <= 0) reset();
              else setColumns(v);
            }}
            aria-label="每行图片数"
          />
          <span className="mos-gallery-columns-value">
            {isAuto ? '自适应' : `${columns} 张`}
          </span>
          <Button
            type="text"
            size="small"
            className="mos-gallery-columns-reset"
            disabled={isAuto}
            onClick={reset}
          >
            重置
          </Button>
        </div>

        <span style={{ fontSize: 12, opacity: 0.7 }}>
          {total || items.length} 张图片
          {items.length < total ? `（已加载 ${items.length}）` : ''}
        </span>
      </div>

      <div
        className="mos-viewer-body"
        ref={bodyRef}
        onScroll={handleScroll}
      >
        <Spin spinning={loading}>
          {/*
           * 面包屑和子目录条放在 Empty 判断**外面**：
           * 当前目录一张图都没有、但下面还有子目录时，
           * 也要能把子目录点开进去看。
           */}
          <div className="mos-gallery-crumbs">
            {crumbs.map((c, i) => (
              <span key={c.key} className="mos-gallery-crumb-item">
                {i > 0 && <span className="mos-gallery-crumb-sep">/</span>}
                {i === crumbs.length - 1 ? (
                  <span className="mos-gallery-crumb is-current">{c.label}</span>
                ) : (
                  <button
                    type="button"
                    className="mos-gallery-crumb"
                    onClick={() => goToDir(c.path, c.id)}
                    title={c.path}
                  >
                    {c.label}
                  </button>
                )}
              </span>
            ))}
          </div>

          {/* 当前目录下的子目录：点进去继续看子目录里的图片 */}
          {subDirs.length > 0 && (
            <div className="mos-gallery-subdirs">
              {subDirs.map((d) => {
                const visited = isDirVisited(d.id);
                const info = dirInfos[d.id];
                return (
                  <button
                    key={d.id}
                    type="button"
                    className={`mos-gallery-subdir${
                      visited ? ' is-visited' : ''
                    }`}
                    onClick={() => goToDir(d.path, d.id)}
                    title={d.path}
                  >
                    {/* 进过的文件夹用「打开的文件夹」图标 */}
                    <DirCover
                      cover={info?.cover || ''}
                      visited={visited}
                      onError={() => onCoverError(d)}
                    />
                    <span className="mos-gallery-subdir-text">
                      <span className="name">{d.fileName}</span>
                      {/* 图片数 + 子文件夹数（只有图片没有文件夹时后者不显示） */}
                      <span className="count">
                        {info ? `${info.imageCount} 张` : '…'}
                        {info && info.dirCount > 0
                          ? ` · ${info.dirCount} 个文件夹`
                          : ''}
                      </span>
                    </span>
                    {visited && (
                      <span
                        className="mos-gallery-subdir-visited"
                        title="2 小时内进过"
                        aria-label="2 小时内进过"
                      >
                        <CheckOutlined />
                      </span>
                    )}
                  </button>
                );
              })}

              {/* 滚到子目录条末尾时自动加载下一页 */}
              {hasMoreSubDirs && (
                <div
                  ref={subDirSentinelRef}
                  className="mos-gallery-subdir-sentinel"
                />
              )}
              {subDirLoading && (
                <div className="mos-gallery-subdir-tip">正在加载更多文件夹…</div>
              )}
            </div>
          )}

          {error ? (
            <Empty description={error} />
          ) : items.length === 0 && !loading ? (
            /*
             * 只有「既没有图片、也没有子目录」才是真的空。
             *
             * 有子目录时不算空 —— 上面已经把文件夹列出来了，
             * 再显示「该目录下没有图片」会让人以为这里啥都没有。
             * 子目录还没拉回来时也先不显示（会闪一下）。
             */
            subDirs.length > 0 || !subDirsLoaded ? null : (
              <Empty description="该目录下没有图片" />
            )
          ) : (
            <>
              <div
                className="mos-gallery-grid"
                ref={setGridEl}
                style={
                  gridTemplateColumns
                    ? { gridTemplateColumns }
                    : undefined
                }
              >
                {images.map((img, index) => (
                  <div
                    className={`mos-gallery-item${
                      selectMode ? ' is-selecting' : ''
                    }`}
                    key={img.id}
                  >
                    <button
                      type="button"
                      className="mos-gallery-open"
                      onClick={() => {
                        // 多选模式下点缩略图是「勾选」，不是打开大图
                        if (selectMode) toggleSelect(img.id);
                        else setActiveIndex(index);
                      }}
                      aria-label={
                        selectMode ? `选择 ${img.name}` : `查看 ${img.name}`
                      }
                    >
                      <LazyThumb src={img.thumb} alt={img.name} />
                      <span className="name">{img.name}</span>
                    </button>

                    {/* 多选模式的勾选标记（格子左上角） */}
                    {selectMode && (
                      <span
                        className={`mos-gallery-check${
                          selectedIds.has(img.id) ? ' is-checked' : ''
                        }`}
                        aria-hidden="true"
                      >
                        {selectedIds.has(img.id) && <CheckOutlined />}
                      </span>
                    )}

                    {/* 2 小时内看过：左上角标一个蓝色的小眼睛 */}
                    {!selectMode && isViewed(img.id) && (
                      <span
                        className="mos-gallery-viewed"
                        title="2 小时内看过"
                        aria-label="2 小时内看过"
                      >
                        <EyeOutlined />
                      </span>
                    )}

                    {/* 删除按钮与打开按钮平级，避免 button 嵌套 button（非法 HTML） */}
                    {canDelete && !selectMode && (
                      <button
                        type="button"
                        className="mos-gallery-del"
                        onClick={(e) => {
                          e.stopPropagation();
                          onDelete(img);
                        }}
                        title="删除（移入回收站）"
                        aria-label={`删除 ${img.name}`}
                      >
                        <DeleteOutlined />
                      </button>
                    )}
                  </div>
                ))}
              </div>

              {/* 滚动到底部自动加载下一页 */}
              <div ref={sentinelRef} className="mos-gallery-sentinel" />

              {loadingMore && (
                <div className="mos-gallery-tip">正在加载更多…</div>
              )}
              {!hasMore && items.length > 0 && (
                <div className="mos-gallery-tip">没有更多了</div>
              )}
            </>
          )}
        </Spin>
      </div>

      {/* 大图灯箱 */}
      {active && (
        <div
          className="mos-lightbox"
          ref={lightboxRef}
          role="dialog"
          aria-modal="true"
          aria-label="图片预览"
          onClick={close}
        >
          <Button
            type="text"
            className="mos-lightbox-close"
            icon={<CloseOutlined />}
            onClick={close}
            style={{ color: '#fff' }}
            aria-label="关闭"
          />

          <div className="mos-lightbox-main">
            <Button
              type="text"
              className="mos-lightbox-nav mos-lightbox-prev"
              icon={<LeftOutlined />}
              disabled={activeIndex <= 0}
              onClick={(e) => {
                e.stopPropagation();
                goPrev();
              }}
              style={{ color: '#fff' }}
              aria-label="上一张"
            />

            <figure
              className="mos-lightbox-figure"
              onClick={(e) => e.stopPropagation()}
              onMouseDown={(e) => {
                // 只有放大后才能拖动看局部
                if (zoom <= 1) return;
                dragRef.current = {
                  startX: e.clientX,
                  startY: e.clientY,
                  baseX: offsetRef.current.x,
                  baseY: offsetRef.current.y,
                };
                setDragging(true);
              }}
              onTouchStart={(e) => {
                touchStartX.current = e.touches[0].clientX;
              }}
              onTouchEnd={(e) => {
                const start = touchStartX.current;
                touchStartX.current = null;
                if (start === null) return;
                const delta = e.changedTouches[0].clientX - start;
                // 滑动阈值 40px，低于阈值视为误触
                if (Math.abs(delta) < 40) return;
                if (delta < 0) goNext();
                else goPrev();
              }}
            >
              {/*
               * 大图未加载完成时先占位显示 loading：
               * 图片元素始终渲染（这样浏览器才会真正发起请求），
               * 只是没加载完之前用 visibility 隐藏，避免出现半张图。
               */}
              {!originLoaded && (
                <div className="mos-lightbox-loading">
                  <Spin size="large" />
                  <span className="mos-lightbox-loading-text">大图加载中…</span>
                </div>
              )}
              {/*
               * 图片外面包一层专门做入场动画。
               *
               * ⚠️ 动画和缩放/平移都动 `transform`，放在同一个元素上会打架：
               * 动画会把缩放值覆盖掉（切图后缩放比例就不对了）。
               * 所以动画放在外层 wrap，缩放平移放在内层 img。
               *
               * `key={active.id}` —— 换图时重新挂载，动画才会重播。
               */}
              <span
                className={`mos-lightbox-img-wrap${
                  slideDir === 1
                    ? ' is-next'
                    : slideDir === -1
                      ? ' is-prev'
                      : ''
                }`}
                key={active.id}
              >
                <img
                  src={active.origin}
                  alt={active.name}
                  // ⚠️ 加载完成时必须给 'is-loaded'：
                  // CSS 里 `.is-loaded` 负责把 opacity 从 0 提到 1，
                  // 不加这个类图片就一直是全黑的。
                  className={originLoaded ? 'is-loaded' : 'is-loading'}
                  style={{
                    transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`,
                    cursor:
                      zoom > 1 ? (dragging ? 'grabbing' : 'grab') : 'default',
                    // 拖动时不要过渡动画，否则会粘手
                    // ⚠️ transition 要在这里写全：inline style 会覆盖 CSS 的 transition，
                    // 漏掉 opacity 就失去「加载完淡入」的效果
                    transition: dragging
                      ? 'none'
                      : 'transform 0.12s ease-out, opacity 0.22s ease',
                  }}
                  onLoad={() => setLoadedIndex(activeIndex)}
                  onError={() => setLoadedIndex(activeIndex)}
                  onDoubleClick={(e) => {
                    e.stopPropagation();
                    // 双击：100% → 放大；已放大 → 还原
                    const current = zoomRef.current;
                    const next = current === 1 ? DOUBLE_CLICK_ZOOM : 1;
                    // 以双击的那一点为中心缩放（图片中心在屏幕上的位置
                    // 已经含了 translate/scale 的影响，getBoundingClientRect 拿到的就是屏幕坐标）
                    const rect = e.currentTarget.getBoundingClientRect();
                    const cx = rect.left + rect.width / 2;
                    const cy = rect.top + rect.height / 2;
                    // 鼠标点相对「图片中心、未缩放」的位置
                    const px = (e.clientX - cx) / current;
                    const py = (e.clientY - cy) / current;
                    const nextOffset = {
                      x: offsetRef.current.x + (current - next) * px,
                      y: offsetRef.current.y + (current - next) * py,
                    };
                    zoomRef.current = next;
                    offsetRef.current = nextOffset;
                    setZoom(next);
                    setOffset(nextOffset);
                    if (next === 1) {
                      dragRef.current = null;
                      setDragging(false);
                    }
                  }}
                />
              </span>
            </figure>

            <Button
              type="text"
              className="mos-lightbox-nav mos-lightbox-next"
              icon={<RightOutlined />}
              disabled={activeIndex >= images.length - 1}
              onClick={(e) => {
                e.stopPropagation();
                goNext();
              }}
              style={{ color: '#fff' }}
              aria-label="下一张"
            />
          </div>

          {/*
           * 底部信息条：图片名 / 张数 / 缩放比例 / 删除按钮。
           *
           * 固定在灯箱底部（缩略图条的上方），不跟着图片上下浮动 ——
           * 图片放大后信息被推到屏幕外就看不见了。
           */}
          <div className="mos-lightbox-footer mos-lightbox-caption">
            <span className="mos-lightbox-name">{active.name}</span>
            <span className="mos-lightbox-counter">
              {activeIndex + 1} / {images.length}
            </span>
            {/* 缩放比例：点一下复位到 100% */}
            <button
              type="button"
              className="mos-lightbox-zoom"
              onClick={(e) => {
                e.stopPropagation();
                resetZoom();
              }}
              title="滚轮缩放，点击复位"
            >
              {Math.round(zoom * 100)}%
            </button>
            {canDelete && (
              <Button
                size="small"
                danger
                type="text"
                icon={<DeleteOutlined />}
                className="mos-lightbox-del"
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(active);
                }}
              >
                删除
              </Button>
            )}
          </div>

          {/* 底部缩略图条：点击可快速跳转 */}
          {images.length > 1 && (
            <div
              className="mos-lightbox-strip"
              ref={stripRef}
              onClick={(e) => e.stopPropagation()}
            >
              {strip.map((img, i) => {
                const realIndex = stripStart + i;
                const isActive = realIndex === activeIndex;
                return (
                  <button
                    key={img.id}
                    type="button"
                    className={`mos-lightbox-strip-item${isActive ? ' active' : ''}`}
                    ref={isActive ? activeStripRef : undefined}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveIndex(realIndex);
                      // 缩略图条跳转不知道方向，用居中淡入
                      setSlideDir(0);
                      // 切换图片：缩放比例保持，只复位平移
                      resetOffset();
                    }}
                    title={img.name}
                    aria-label={`跳转到 ${img.name}`}
                    aria-current={isActive}
                  >
                    <LazyThumb src={img.thumb} alt={img.name} />
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
