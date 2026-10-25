import { FolderFilled, PlayCircleFilled } from '@ant-design/icons';
import { Spin, Tag, Tooltip } from 'antd';
import { useEffect, useState } from 'react';
import { fetchDirDetailInfo } from '@/services/api';
import { FileIcon, FolderIcon } from '@/components/FileIcon';
import type { DirAndResourceVo, FileSuffixMap, ResourceVo } from '@/services/types';

export interface ListNameProps {
  record: DirAndResourceVo;
  bucketName?: string;
  /** 搜索全部位置时显示完整路径 */
  showDetailPath?: boolean;
  /** 文件后缀分类，用于识别视频 */
  fileSuffix?: FileSuffixMap;
  /** 最近访问过的路径（高亮） */
  historyClicks?: string[];
  /** 目录缩略图最多展示几个（窄屏可减少请求数） */
  maxThumbs?: number;
  /** 点击目录 */
  onOpenDir?: (record: DirAndResourceVo) => void;
  /** 点击图片：打开图片预览 */
  onPreviewImage?: (url: string, record: DirAndResourceVo) => void;
  /** 点击文件：跳转到查看器 */
  onOpenFile?: (record: DirAndResourceVo) => void;
}

/**
 * 资源名称单元格。
 *
 * - 目录：显示名称 + 子项数量徽标 + 缩略图预览
 * - 图片：显示缩略图，点击打开大图预览
 * - 视频：显示缩略图，点击进入视频查看器
 * - 其他：显示类型图标
 */
export default function ListName({
  record,
  bucketName,
  showDetailPath,
  fileSuffix,
  historyClicks = [],
  maxThumbs = 6,
  onOpenDir,
  onPreviewImage,
  onOpenFile,
}: ListNameProps) {
  const [thumbs, setThumbs] = useState<ResourceVo[]>([]);
  const [count, setCount] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(false);

  const isDir = record.isDir;
  const label = isDir
    ? showDetailPath
      ? record.path
      : `/${record.fileName}`
    : showDetailPath
    ? record.path
    : record.fileName;

  const isActive = historyClicks.indexOf(record.path) !== -1;

  // 目录：加载子项统计与缩略图
  useEffect(() => {
    if (!isDir || !record.id || !bucketName) return;
    let cancelled = false;
    setLoading(true);
    fetchDirDetailInfo(bucketName, record.id)
      .then((info) => {
        if (cancelled || !info) return;
        setThumbs(info.thumbs || []);
        setCount((info.fileCount || 0) + (info.dirCount || 0));
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isDir, record.id, bucketName]);

  const isVideo = () => {
    const videoSuffix = fileSuffix?.video || [];
    const fileName = record.fileName || '';
    return videoSuffix.some(
      (item) => fileName.endsWith(item) || fileName.endsWith(item.toUpperCase()),
    );
  };

  /** 缩略图地址：{signUrl}&thumb=true */
  const thumbUrl = record.thumbFileHouseId
    ? record.signUrl
      ? `${record.signUrl}&thumb=true`
      : `/mos/${bucketName}${record.urlEncodePath}?thumb=true`
    : null;

  if (isDir) {
    return (
      <div>
        <div className="mos-resource-name">
          <FolderIcon />
          <a
            className={`mos-resource-link${isActive ? ' active' : ''}`}
            onClick={() => onOpenDir?.(record)}
          >
            {label}
          </a>
          {count !== undefined && (
            <Tag color="pink" style={{ marginInlineEnd: 0 }}>
              {count}
            </Tag>
          )}
        </div>
        {thumbs.length > 0 && (
          <div className="mos-dir-thumbs">
            {loading && <Spin size="small" />}
            {thumbs.slice(0, maxThumbs).map((item) => {
              const url = item.signUrl ? `${item.signUrl}&thumb=true` : '';
              return (
                <Tooltip key={item.id} title={item.name}>
                  <a
                    className="mos-dir-thumb"
                    href={url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <img src={url} alt={item.name} loading="lazy" />
                    {item.videoLength && (
                      <span className="duration">
                        <PlayCircleFilled style={{ marginRight: 2 }} />
                        {item.videoLength}
                      </span>
                    )}
                  </a>
                </Tooltip>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  // 文件
  const imageThumb = thumbUrl && record.image;
  const videoThumb = thumbUrl && isVideo();

  return (
    <div className="mos-resource-name">
      {imageThumb || videoThumb ? (
        <a
          onClick={(e) => {
            e.preventDefault();
            if (record.image) {
              onPreviewImage?.(
                record.signUrl || `/mos/${bucketName}${record.urlEncodePath}`,
                record,
              );
            } else {
              onOpenFile?.(record);
            }
          }}
          style={{ display: 'inline-flex' }}
        >
          <img className="mos-resource-thumb" src={thumbUrl!} alt={record.fileName} loading="lazy" />
        </a>
      ) : (
        <FileIcon icon={record.icon} fileName={record.fileName} image={record.image} />
      )}
      <span className="mos-resource-text">
        {record.image ? (
          <a
            className={`mos-resource-link${isActive ? ' active' : ''}`}
            onClick={(e) => {
              e.preventDefault();
              onPreviewImage?.(
                record.signUrl || `/mos/${bucketName}${record.urlEncodePath}`,
                record,
              );
            }}
          >
            {label}
          </a>
        ) : isVideo() ? (
          <a
            className={`mos-resource-link${isActive ? ' active' : ''}`}
            onClick={(e) => {
              e.preventDefault();
              if (onOpenFile) {
                // 视频跳转到播放页（播放器 + 同目录视频列表）
                onOpenFile(record);
              } else {
                window.open(
                  record.signUrl ||
                    `/mos/${bucketName}${record.urlEncodePath}?render=true`,
                  '_blank',
                );
              }
            }}
          >
            {label}
          </a>
        ) : (
          <a
            className={`mos-resource-link${isActive ? ' active' : ''}`}
            href={record.signUrl || `/mos/${bucketName}${record.urlEncodePath}?render=true`}
            target="_blank"
            rel="noreferrer"
          >
            {label}
          </a>
        )}
      </span>
      <FolderFilled style={{ display: 'none' }} />
    </div>
  );
}
