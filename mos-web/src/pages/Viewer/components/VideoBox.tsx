import { CopyOutlined, DeleteOutlined, DownloadOutlined } from '@ant-design/icons';
import { Button, Tag, Tooltip, message } from 'antd';
import DPlayer from 'dplayer';
import { useCallback, useEffect, useMemo, useRef } from 'react';
import { copyText } from '@/utils';
import type { DirAndResourceVo } from '@/services/types';

/** 播放音量在本地记住，换个视频不用重新调 */
const VOLUME_KEY = 'mos-video-volume';

export interface VideoBoxProps {
  video: DirAndResourceVo | null;
  /** 播放结束（用于自动播下一个） */
  onEnded?: () => void;
  /** 删除当前视频 */
  onDelete?: (video: DirAndResourceVo) => void;
}

/**
 * 视频播放区。
 *
 * 注意：DPlayer 装的是 **v1**，播放地址必须写在 `video.url` 上，
 * 顶层 `url` 是 v2 的写法，v1 会直接忽略（表现为播放器没有画面）。
 */
export default function VideoBox({ video, onEnded, onDelete }: VideoBoxProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const playerRef = useRef<DPlayer | null>(null);
  const endedRef = useRef<(() => void) | undefined>(onEnded);

  useEffect(() => {
    endedRef.current = onEnded;
  }, [onEnded]);

  /**
   * 播放地址。
   *
   * 服务端返回的 signUrl 带 `render=true`，那是一个 HTML 播放页；
   * 播放器要的是视频流本身，所以换成 `render=false`。
   */
  const playUrl = useMemo(() => {
    if (!video?.signUrl) return '';
    return video.signUrl.replace('render=true', 'render=false');
  }, [video?.signUrl]);

  // 创建播放器：首次拿到地址时创建，之后换源用 switchVideo（不重建实例）
  useEffect(() => {
    const container = containerRef.current;
    if (!container || !playUrl) return;

    let player = playerRef.current;
    if (player) {
      player.switchVideo({ url: playUrl });
    } else {
      player = new DPlayer({
        container,
        autoplay: false,
        loop: false,
        preload: 'auto',
        hotkey: true,
        danmaku: false,
        screenshot: false,
        video: { url: playUrl },
      });
      playerRef.current = player;
      player.on('ended', () => endedRef.current?.());
      // 恢复上次播放音量
      const saved = localStorage.getItem(VOLUME_KEY);
      if (saved) {
        const volume = Number(saved);
        if (!Number.isNaN(volume)) player.volume(volume);
      }
      player.on('volumechange', () => {
        localStorage.setItem(VOLUME_KEY, String(player!.volume()));
      });
    }
    player.play();
  }, [playUrl]);

  // 卸载时销毁播放器
  useEffect(
    () => () => {
      playerRef.current?.destroy();
      playerRef.current = null;
    },
    [],
  );

  const onCopy = useCallback(async () => {
    if (!video?.signUrl) return;
    const ok = await copyText(video.signUrl);
    if (ok) message.success('链接已复制');
    else message.warning('复制失败，请手动复制播放地址');
  }, [video?.signUrl]);

  return (
    <div>
      <div className="mos-video-player">
        <div className="mos-video-player-inner" ref={containerRef} />
      </div>

      {video && (
        <div className="mos-video-meta">
          <div className="mos-video-meta-head">
            <span className="mos-video-name" title={video.fileName}>
              {video.fileName}
            </span>
            {video.videoLength && <Tag color="blue">{video.videoLength}</Tag>}
          </div>
          <div className="mos-video-stats">
            <span>{video.visits ?? 0} 次观看</span>
            <span>大小：{video.readableSize}</span>
            <span>创建时间：{video.createdDate}</span>
          </div>
          <div className="mos-video-actions">
            <Tooltip title="复制播放链接">
              <Button size="small" icon={<CopyOutlined />} onClick={onCopy}>
                复制链接
              </Button>
            </Tooltip>
            <Button
              size="small"
              icon={<DownloadOutlined />}
              href={`${playUrl}&download=true`}
              target="_blank"
              rel="noreferrer"
            >
              下载
            </Button>
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => onDelete?.(video)}
            >
              删除
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
