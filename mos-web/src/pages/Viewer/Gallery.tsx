import { Button, Empty, Slider, Spin } from 'antd';
import {
  ArrowLeftOutlined,
  LeftOutlined,
  RightOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { useSearchParams } from '@umijs/max';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { fetchResources } from '@/services/api';
import { basename, normalizePath } from '@/utils';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { useGalleryColumns, MAX_COLUMNS } from '@/hooks/useGalleryColumns';
import type { DirAndResourceVo } from '@/services/types';

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

/**
 * 单个缩略图的最小宽度。
 *
 * 固定每行张数时，若容器太窄会把图挤成一团，
 * 所以按这个下限反推「最多能放几列」，超出的部分自动少放几列。
 */
const MIN_CELL_WIDTH = 96;

const IMAGE_SUFFIXS = ['.jpg', '.gif', '.jpeg', '.png', '.bmp', '.webp'];

interface GalleryImage {
  id: number;
  name: string;
  origin: string;
  thumb: string;
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
        />
      ) : (
        <span className="mos-gallery-skeleton" aria-hidden="true" />
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
 */
export default function GalleryViewer() {
  const [searchParams] = useSearchParams();
  const bucket = searchParams.get('bucket') || '';
  const path = searchParams.get('path') || '/';

  const [items, setItems] = useState<DirAndResourceVo[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  /** 灯箱当前图片下标，-1 表示未打开 */
  const [activeIndex, setActiveIndex] = useState(-1);
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
  /** 网格容器宽度，用来把「每行张数」换算成实际列数 */
  const [gridWidth, setGridWidth] = useState(0);

  /** 触摸滑动的起始横坐标 */
  const touchStartX = useRef<number | null>(null);
  /** 已经发起过大图预加载的 URL，避免重复预热 */
  const preloadedRef = useRef<Set<string>>(new Set());
  /** 到底部自动加载的哨兵元素 */
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  /** 灯箱底部的缩略图条 */
  const stripRef = useRef<HTMLDivElement | null>(null);
  /** 缩略图条中当前图片对应的按钮 */
  const activeStripRef = useRef<HTMLButtonElement | null>(null);
  /** 灯箱根节点（滚轮事件的绑定目标） */
  const lightboxRef = useRef<HTMLDivElement | null>(null);
  /** 上一次滚轮切换的时间戳（用于节流） */
  const lastWheelRef = useRef(0);

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
        setItems((prev) => (append ? [...prev, ...list] : list));
        setPageNum(targetPage);
        setTotal(listTotal);
        setPageEmpty(list.length === 0);
        setError('');
      } catch {
        if (!append) setError('图库加载失败');
        setPageEmpty(true);
      } finally {
        if (append) setLoadingMore(false);
        else setLoading(false);
      }
    },
    [bucket, path],
  );

  // 目录 / 桶变化时重新从第一页开始
  useEffect(() => {
    setItems([]);
    setPageNum(1);
    setTotal(0);
    setPageEmpty(false);
    setActiveIndex(-1);
    // 换目录后图片列表整体变了，已加载的下标不再有意义
    setLoadedIndex(-1);
    loadPage(1, false);
  }, [bucket, path, loadPage]);

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
        };
      }),
    [items, bucket],
  );

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

  // 浏览器标题：目录名 + 图片数量
  useDocumentTitle(
    loading && items.length === 0
      ? `${dirName} · 加载中…`
      : `${dirName} · ${total || items.length} 张图片`,
  );

  const goPrev = useCallback(() => {
    setActiveIndex((prev) => (prev > 0 ? prev - 1 : prev));
  }, []);

  const goNext = useCallback(() => {
    setActiveIndex((prev) =>
      prev >= 0 && prev < images.length - 1 ? prev + 1 : prev,
    );
  }, [images.length]);

  /**
   * 关闭灯箱。
   *
   * 同时把已加载下标清掉：重新打开同一张图时，`<img>` 是新挂载的元素，
   * 需要重新走一次 onLoad，否则会直接命中旧的「已加载」状态而跳过 loading。
   */
  const close = useCallback(() => {
    setActiveIndex(-1);
    setLoadedIndex(-1);
  }, []);

  // 灯箱打开时锁定页面滚动
  useEffect(() => {
    if (activeIndex < 0) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [activeIndex]);

  // 键盘操作：← → 切换，Esc 关闭
  useEffect(() => {
    if (activeIndex < 0) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        goPrev();
      } else if (e.key === 'ArrowRight') {
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
   * 鼠标滚轮切换：上下滚动 = 上一张 / 下一张。
   *
   * - 触控板横向滑动（deltaX）同样能切换。
   * - 缩略图条上不接管滚轮，否则没法横向滚动缩略图条。
   * - 必须 `passive: false` 才能 preventDefault —— 否则背后的图集列表会跟着滚。
   * - 加了 150ms 的节流，防止快速滚轮连续跳很多张。
   */
  useEffect(() => {
    const el = lightboxRef.current;
    if (!el || activeIndex < 0) return;
    const onWheel = (e: WheelEvent) => {
      // 缩略图条上不拦截，保留其自身的滚动能力
      if ((e.target as HTMLElement)?.closest?.('.mos-lightbox-strip')) return;
      const delta =
        Math.abs(e.deltaY) >= Math.abs(e.deltaX) ? e.deltaY : e.deltaX;
      if (Math.abs(delta) < 8) return;
      e.preventDefault();
      const now = Date.now();
      if (now - lastWheelRef.current < 150) return;
      lastWheelRef.current = now;
      if (delta > 0) goNext();
      else goPrev();
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [activeIndex, goPrev, goNext]);

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
          onClick={() => window.history.back()}
          style={{ color: '#fff' }}
          aria-label="返回"
        />
        <span className="mos-viewer-title">
          {bucket} {path}
        </span>
        <span style={{ flex: 1 }} />

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

      <div className="mos-viewer-body">
        <Spin spinning={loading}>
          {error ? (
            <Empty description={error} />
          ) : items.length === 0 && !loading ? (
            <Empty description="该目录下没有图片" />
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
                  <button
                    key={img.id}
                    type="button"
                    className="mos-gallery-item"
                    onClick={() => setActiveIndex(index)}
                  >
                    <LazyThumb src={img.thumb} alt={img.name} />
                    <span className="name">{img.name}</span>
                  </button>
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
              <img
                src={active.origin}
                alt={active.name}
                className={originLoaded ? undefined : 'is-loading'}
                onLoad={() => setLoadedIndex(activeIndex)}
                onError={() => setLoadedIndex(activeIndex)}
              />
              <figcaption className="mos-lightbox-caption">
                <span className="mos-lightbox-name">{active.name}</span>
                <span className="mos-lightbox-counter">
                  {activeIndex + 1} / {images.length}
                </span>
              </figcaption>
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
