import { PlayCircleOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';

export interface ThumbProps {
  src?: string | null;
  /** 视频时长，如 "00:01:20" */
  videoLength?: string | null;
  alt?: string;
}

/**
 * 缩略图。
 *
 * 缩略图地址规则：`{signUrl}&thumb=true`
 */
export default function Thumb({ src, videoLength, alt }: ThumbProps) {
  if (!src) {
    return <div className="mos-resource-thumb" />;
  }
  return (
    <Tooltip title={alt}>
      <div className="mos-dir-thumb" style={{ width: 44, height: 44 }}>
        <img src={src} alt={alt} loading="lazy" />
        {videoLength && (
          <span className="duration">
            <PlayCircleOutlined style={{ marginRight: 2 }} />
            {videoLength}
          </span>
        )}
      </div>
    </Tooltip>
  );
}
