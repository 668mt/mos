import {
  ArrowLeftOutlined,
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons';
import { Button, Empty, Modal, Spin, message } from 'antd';
import { useSearchParams } from '@umijs/max';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { deleteResources, fetchFileByType, fetchFilesByType } from '@/services/api';
import { normalizePath } from '@/utils';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import VideoBox from './components/VideoBox';
import VideoList from './components/VideoList';
import type { DirAndResourceVo } from '@/services/types';

/** 每页加载的视频数量 */
const PAGE_SIZE = 100;

/**
 * 视频查看器。
 *
 * 通过 `?bucket=&id=&path=` 定位视频，`path` 是视频所在**目录**的路径。
 * 接口：
 * - `GET /member/resource/{bucket}/files/video` —— 同目录下的视频列表（分页）
 * - `GET /member/resource/{bucket}/file/video/{id}` —— 单个视频信息
 */
export default function VideoViewer() {
  const [searchParams, setSearchParams] = useSearchParams();
  const bucket = searchParams.get('bucket') || '';
  const id = searchParams.get('id') || '';
  const path = normalizePath(searchParams.get('path'));

  const [videos, setVideos] = useState<DirAndResourceVo[]>([]);
  const [current, setCurrent] = useState<DirAndResourceVo | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [currentLoading, setCurrentLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [error, setError] = useState('');

  /* ------------------------------------------------------------ 数据加载 */

  /** 加载视频列表（append=true 时追加到已有列表后面） */
  const loadList = useCallback(
    async (page: number, append: boolean) => {
      if (!bucket) return;
      setLoading(true);
      try {
        const result = await fetchFilesByType(bucket, 'video', {
          path,
          pageNum: page,
          pageSize: PAGE_SIZE,
        });
        const items = result?.list || [];
        setVideos((prev) => (append ? [...prev, ...items] : items));
        setHasMore(!!result?.hasNextPage);
        setTotal(result?.total ?? items.length);
        setPageNum(page);
        setError('');
      } catch {
        // 错误已由拦截器处理
        if (!append) setVideos([]);
      } finally {
        setLoading(false);
      }
    },
    [bucket, path],
  );

  // 切换目录 / 桶时重置列表
  useEffect(() => {
    if (!bucket) return;
    setVideos([]);
    setPageNum(1);
    setHasMore(true);
    loadList(1, false);
  }, [bucket, path, loadList]);

  // 加载当前播放的视频
  useEffect(() => {
    if (!bucket || !id) {
      setCurrent(null);
      return;
    }
    let cancelled = false;
    setCurrentLoading(true);
    fetchFileByType(bucket, 'video', Number(id), path)
      .then((file) => {
        if (cancelled) return;
        /**
         * 同一个资源，签名地址每次请求都不一样（签名带过期时间戳）。
         * 若这里替换掉，会让播放器二次 switchVideo，
         * 第一次播放请求被中断后的回调会把新视频也一起暂停。
         * 所以同 id 且已有可用地址时保留原对象，避免打断正在播放的视频。
         */
        setCurrent((prev) =>
          prev && prev.id === file.id && prev.signUrl ? prev : file,
        );
        setError('');
      })
      .catch(() => {
        if (cancelled) return;
        setCurrent(null);
        setError('视频加载失败');
      })
      .finally(() => {
        if (!cancelled) setCurrentLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [bucket, id, path]);

  /* ------------------------------------------------------------ 交互 */

  /** 关键字过滤（前端过滤，不重新请求） */
  const filtered = useMemo(
    () =>
      videos.filter(
        (video) => !keyword || (video.fileName || '').indexOf(keyword) > -1,
      ),
    [videos, keyword],
  );

  /** 当前播放的视频在过滤后列表中的位置（从 1 开始） */
  const currentIndex = useMemo(() => {
    if (!current) return 0;
    const idx = filtered.findIndex((v) => String(v.id) === String(current.id));
    return idx >= 0 ? idx + 1 : 0;
  }, [filtered, current]);

  /** 切换播放的视频：写回 URL（replace，不往历史记录里堆） */
  const playVideo = useCallback(
    (video: DirAndResourceVo) => {
      setCurrent(video);
      const next = new URLSearchParams(searchParams);
      next.set('id', String(video.id));
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  /** 播放下一个（视频播完自动连播） */
  const playNext = useCallback(() => {
    if (!current) return;
    const idx = filtered.findIndex((v) => String(v.id) === String(current.id));
    const next = filtered[idx + 1];
    if (next) playVideo(next);
  }, [current, filtered, playVideo]);

  /** 加载下一页 */
  const loadMore = useCallback(() => {
    if (loading || !hasMore) return;
    loadList(pageNum + 1, true);
  }, [loading, hasMore, pageNum, loadList]);

  /** 删除视频：删完从列表移除，如果删的是当前播放的则切到相邻的一个 */
  const onDelete = useCallback(
    (video: DirAndResourceVo) => {
      Modal.confirm({
        title: '确定删除这个视频吗?',
        content: (
          <div>
            <div style={{ wordBreak: 'break-all' }}>{video.path}</div>
            <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
              删除后会移入回收站，可在回收站中还原
            </div>
          </div>
        ),
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          await deleteResources(bucket, { fileIds: video.id });
          message.success('删除成功');
          const idx = videos.findIndex((v) => v.id === video.id);
          const rest = videos.filter((v) => v.id !== video.id);
          setVideos(rest);
          setTotal((prev) => Math.max(0, prev - 1));
          if (String(current?.id) === String(video.id)) {
            const next = rest[idx] || rest[idx - 1];
            if (next) playVideo(next);
            else setCurrent(null);
          }
        },
      });
    },
    [bucket, videos, current, playVideo],
  );

  /* ------------------------------------------------------------ 渲染 */

  // 浏览器标题：当前视频文件名
  useDocumentTitle(current?.fileName || '视频');

  const prevVideo = currentIndex > 1 ? filtered[currentIndex - 2] : undefined;
  const nextVideo =
    currentIndex > 0 ? filtered[currentIndex] : undefined;

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
          {current?.fileName || current?.path || '视频'}
        </span>
        {current?.videoLength && (
          <span style={{ fontSize: 12, opacity: 0.7 }}>{current.videoLength}</span>
        )}
        <span style={{ flex: 1 }} />
        <Button
          type="text"
          icon={<LeftOutlined />}
          disabled={!prevVideo}
          onClick={() => prevVideo && playVideo(prevVideo)}
          style={{ color: '#fff' }}
          aria-label="上一个"
        />
        <span style={{ fontSize: 12, opacity: 0.7 }}>
          {currentIndex > 0 ? `${currentIndex} / ${total}` : `共 ${total} 个`}
        </span>
        <Button
          type="text"
          icon={<RightOutlined />}
          disabled={!nextVideo}
          onClick={() => nextVideo && playVideo(nextVideo)}
          style={{ color: '#fff' }}
          aria-label="下一个"
        />
      </div>

      <div className="mos-viewer-body">
        <Spin spinning={currentLoading && !current}>
          {error ? (
            <Empty description={error} />
          ) : (
            <div className="mos-video-layout">
              <VideoBox video={current} onEnded={playNext} onDelete={onDelete} />
              <VideoList
                videos={filtered}
                currentId={current?.id}
                currentIndex={currentIndex}
                total={total}
                path={path}
                keyword={keyword}
                onKeywordChange={setKeyword}
                onSelect={playVideo}
                onLoadMore={loadMore}
                hasMore={hasMore}
                loading={loading}
              />
            </div>
          )}
        </Spin>
      </div>
    </div>
  );
}
