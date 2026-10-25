import {
  FileExcelOutlined,
  FileImageOutlined,
  FileMarkdownOutlined,
  FilePdfOutlined,
  FilePptOutlined,
  FileTextOutlined,
  FileUnknownOutlined,
  FileWordOutlined,
  FileZipOutlined,
  FolderFilled,
  PlayCircleFilled,
} from '@ant-design/icons';
import { Tooltip } from 'antd';
import type { ReactNode } from 'react';

export interface FileIconProps {
  /** 服务端返回的 icon 标识，如 icon-tupian2 */
  icon?: string | null;
  /** 文件名 */
  fileName?: string;
  /** 是否是图片 */
  image?: boolean;
  size?: number;
}

/**
 * 文件类型图标。
 *
 * 服务端 `DirAndResourceVo.icon` 会返回 iconfont 标识，
 * 这里映射为 Ant Design 图标，效果更统一。
 */
export function FileIcon({ icon, fileName, image, size = 20 }: FileIconProps) {
  const name = (fileName || '').toLowerCase();
  const ext = name.includes('.') ? name.slice(name.lastIndexOf('.') + 1) : '';

  let node: ReactNode;
  if (image || ['jpg', 'jpeg', 'bmp', 'tif', 'png', 'gif'].includes(ext)) {
    node = <FileImageOutlined style={{ color: '#eb2f96' }} />;
  } else if (['doc', 'docx'].includes(ext)) {
    node = <FileWordOutlined style={{ color: '#2b579a' }} />;
  } else if (['ppt', 'pptx'].includes(ext)) {
    node = <FilePptOutlined style={{ color: '#d24726' }} />;
  } else if (['xls', 'xlsx'].includes(ext)) {
    node = <FileExcelOutlined style={{ color: '#217346' }} />;
  } else if (['zip', 'rar', 'gz', 'tar'].includes(ext)) {
    node = <FileZipOutlined style={{ color: '#faad14' }} />;
  } else if (['pdf'].includes(ext)) {
    node = <FilePdfOutlined style={{ color: '#cf1322' }} />;
  } else if (['md', 'markdown'].includes(ext)) {
    node = <FileMarkdownOutlined style={{ color: '#722ed1' }} />;
  } else if (['txt', 'xml', 'json', 'yml', 'yaml'].includes(ext)) {
    node = <FileTextOutlined style={{ color: '#8c8c8c' }} />;
  } else if (['mp4', 'rmvb', 'avi', 'flv', '3gp', 'mov', 'rm', 'mpg', 'mpeg', 'mkv'].includes(ext)) {
    node = <PlayCircleFilled style={{ color: '#fa541c' }} />;
  } else {
    node = <FileUnknownOutlined style={{ color: '#bfbfbf' }} />;
  }

  const label = ext ? ext.toUpperCase() : '文件';
  return (
    <Tooltip title={label}>
      <span className="mos-resource-icon" style={{ fontSize: size }}>
        {node}
      </span>
    </Tooltip>
  );
}

/** 文件夹图标 */
export function FolderIcon({ size = 20 }: { size?: number }) {
  return (
    <span className="mos-resource-icon folder" style={{ fontSize: size }}>
      <FolderFilled />
    </span>
  );
}
