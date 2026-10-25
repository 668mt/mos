import {
  ClusterOutlined,
  HomeOutlined,
  ShopOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import type { ReactNode } from 'react';

/** 菜单项定义 */
export interface MenuItem {
  path: string;
  name: string;
  icon?: ReactNode;
  /** 需要管理员权限 */
  adminOnly?: boolean;
}

/**
 * 一级菜单配置。
 *
 * 结构：进入「资源管理」先看到桶的管理（桶列表），
 * 点进某个桶后，桶内的三个子菜单（仪表盘 / 资源管理 / 回收站）
 * 由 `pages/Resource/components/BucketNav.tsx` 呈现。
 */
export const MENU_ITEMS: MenuItem[] = [
  { path: '/resource', name: '资源管理', icon: <ShopOutlined /> },
  { path: '/manage/client', name: '服务器管理', icon: <ClusterOutlined />, adminOnly: true },
  { path: '/manage/user', name: '用户管理', icon: <TeamOutlined />, adminOnly: true },
];

export const MOBILE_ICON = {
  home: <HomeOutlined />,
};
