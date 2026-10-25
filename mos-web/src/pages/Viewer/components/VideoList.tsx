import { EnvironmentOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { Input, Spin } from 'antd';
import { useCallback, useRef } from 'react';
import type { DirAndResourceVo } from '@/services/types';

export interface VideoListProps {
  /** 已加载的视频（已按关键字过滤） */
  videos: DirAndResourceVo[];
  /** 当前播放的视频 id */
  currentId?: number | string;
  /** 当前播放位置（从 1 开始），未找到时为 0 */
  currentIndex: number;
  /** 目录下的视频总数 */
  total: number;
  /** 当前目录 */
  path: string;
  /** 关键字 */
  keyword: string;
  onKeywordChange: (keyword: string) => void;
  /** 点击列表项切换播放 */
  onSelect: (video: DirAndResourceVo) => void;
  /** 滚动到底部时加载下一页 */
  onLoadMore: () => void;
  /** 是否还有下一页 */
  hasMore: boolean;
  /** 加载中 */
  loading: boolean;
}

/**
 * 视频列表（播放器右侧）。
 *
 * - 关键字搜索在前端过滤，不重新请求
 * - 滚动到底部自动加载下一页
 */
export default function VideoList({
  videos,
  currentId,
  currentIndex,
  total,
  path,
  keyword,
  onKeywordChange,
  onSelect,
  onLoadMore,
  hasMore,
  loading,
}: VideoListProps) {
  const listRef = useRef<HTMLDivElement | null>(null);

  /** 滚动到底部（留 80px 提前量）时加载下一页 */
  const onScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const el = e.currentTarget;
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 80) {
        onLoadMore();
      }
    },
    [onLoadMore],
  );

  /** 把当前播放的视频滚动到可视区域 */
  const locateCurrent = useCallback(() => {
    const active = listRef.current?.querySelector('.mos-video-item.active');
    if (active) {
      active.scrollIntoView({ block: 'center', behavior: 'smooth' });
    }
  }, []);

  return (
    <aside className="mos-video-side">
      <div className="mos-video-side-head">
        <div className="mos-video-side-title">
          <span className="mos-video-side-folder" title={path}>
            文件夹：{path}
          </span>
          <span className="mos-video-side-counter">
            {currentIndex > 0 ? `${currentIndex} / ${total}` : `共 ${total} 个`}
            <a className="mos-video-locate" onClick={locateCurrent} title="定位到当前播放">
              <EnvironmentOutlined />
            </a>
          </span>
        </div>
        <Input.Search
          size="small"
          allowClear
          placeholder="关键字搜索"
          value={keyword}
          onChange={(e) => onKeywordChange(e.target.value)}
        />
      </div>

      <div className="mos-video-list" ref={listRef} onScroll={onScroll}>
        {videos.map((video, index) => {
          const thumb = video.thumbFileHouseId
            ? video.signUrl
              ? `${video.signUrl}&thumb=true`
              : null
            : null;
          const active = String(video.id) === String(currentId);
          return (
            <div
              key={video.id}
              className={`mos-video-item${active ? ' active' : ''}`}
              onClick={() => onSelect(video)}
            >
              <span className="mos-video-item-idx">{index + 1}</span>
              <span className="mos-video-item-thumb">
                {thumb ? (
                  <img src={thumb} alt={video.fileName} loading="lazy" />
                ) : (
                  <PlayCircleOutlined />
                )}
                {video.videoLength && (
                  <span className="mos-video-item-duration">{video.videoLength}</span>
                )}
              </span>
              <span className="mos-video-item-info">
                <span className="mos-video-item-name" title={video.fileName}>
                  {video.fileName}
                </span>
                <span className="mos-video-item-meta">
                  <span>{video.visits ?? 0} 次观看</span>
                  <span>{video.readableSize}</span>
                  <span>{video.createdDate}</span>
                </span>
              </span>
            </div>
          );
        })}

        {loading && (
          <div className="mos-video-list-foot">
            <Spin size="small" />
          </div>
        )}
        {!loading && !hasMore && videos.length > 0 && !keyword && (
          <div className="mos-video-list-foot">已加载全部 {total} 个</div>
        )}
        {!loading && videos.length === 0 && (
          <div className="mos-video-list-foot">
            {keyword ? '没有匹配的视频' : '该目录下没有视频'}
          </div>
        )}
      </div>
    </aside>
  );
}
