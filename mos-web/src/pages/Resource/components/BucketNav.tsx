import {
  ArrowLeftOutlined,
  DashboardOutlined,
  DeleteOutlined,
  FolderOpenOutlined,
} from '@ant-design/icons';
import { Select } from 'antd';
import { history, useLocation } from '@umijs/max';
import { useMemo, type ReactNode } from 'react';
import type { BucketVo } from '@/services/types';

/** 桶内页签 */
export type BucketTabKey = 'dashboard' | 'files' | 'trash';

/** 页签定义：路径 + 展示名 + 图标 */
export const BUCKET_TABS: {
  key: BucketTabKey;
  name: string;
  path: string;
  icon: ReactNode;
}[] = [
  { key: 'dashboard', name: '仪表盘', path: '/resource/dashboard', icon: <DashboardOutlined /> },
  { key: 'files', name: '资源管理', path: '/resource/files', icon: <FolderOpenOutlined /> },
  { key: 'trash', name: '回收站', path: '/resource/trash', icon: <DeleteOutlined /> },
];

/**
 * 构造「页签 + 桶名」的 URL。
 *
 * 只带 `b`（桶名），其余分页 / 路径参数由各页自行维护。
 */
export function buildBucketUrl(tabKey: BucketTabKey, bucketName?: string): string {
  const tab = BUCKET_TABS.find((t) => t.key === tabKey) ?? BUCKET_TABS[0];
  if (!bucketName) return tab.path;
  return `${tab.path}?b=${encodeURIComponent(bucketName)}`;
}

export interface BucketNavProps {
  /** 全部桶（供切换下拉用） */
  buckets: BucketVo[];
  /** 当前桶名 */
  current?: string;
  /**
   * 切换桶的回调。
   *
   * 传入时由调用方决定如何更新状态与 URL（各页签的 URL 同步策略不同），
   * 不传则退化为「切到当前页签 + 换桶名」。
   */
  onSwitchBucket?: (bucketName: string) => void;
}

/**
 * 桶内导航条。
 *
 * 出现在「资源管理」进入某个桶之后：左侧是返回桶列表 + 桶切换，
 * 右侧是桶内的三个子菜单（仪表盘 / 资源管理 / 回收站）。
 */
export default function BucketNav({ buckets, current, onSwitchBucket }: BucketNavProps) {
  const location = useLocation();

  const active = useMemo<BucketTabKey>(() => {
    const hit = BUCKET_TABS.find((t) => location.pathname === t.path);
    return hit?.key ?? 'dashboard';
  }, [location.pathname]);

  const onBucketChange = (bucketName: string) => {
    if (onSwitchBucket) {
      onSwitchBucket(bucketName);
      return;
    }
    history.push(buildBucketUrl(active, bucketName));
  };

  return (
    <div className="mos-bucket-nav">
      <div className="mos-bucket-nav-left">
        <a className="mos-bucket-back" onClick={() => history.push('/resource')}>
          <ArrowLeftOutlined />
          桶列表
        </a>
        <Select
          className="mos-bucket-switch"
          value={current}
          onChange={onBucketChange}
          placeholder="请选择桶"
          options={buckets.map((b) => ({ value: b.bucketName, label: b.bucketName }))}
          popupMatchSelectWidth={false}
          showSearch
          optionFilterProp="label"
        />
      </div>

      <nav className="mos-bucket-tabs">
        {BUCKET_TABS.map((t) => (
          <a
            key={t.key}
            className={`mos-bucket-tab${active === t.key ? ' active' : ''}`}
            onClick={() => history.push(buildBucketUrl(t.key, current))}
          >
            {t.icon}
            <span>{t.name}</span>
          </a>
        ))}
      </nav>
    </div>
  );
}
