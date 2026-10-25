import {
  AreaChartOutlined,
  ClusterOutlined,
  DeleteOutlined,
  FolderOpenOutlined,
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
 * 菜单配置。
 *
 * 与旧版路由保持一致：总览 / 文件中心 / 桶管理 / 管理中心（服务器、用户）。
 * 回收站不占侧边栏一级菜单，入口放在文件中心右上角（与旧版一致）。
 */
export const MENU_ITEMS: MenuItem[] = [
  { path: '/home', name: '总览', icon: <AreaChartOutlined /> },
  { path: '/resource', name: '文件中心', icon: <FolderOpenOutlined /> },
  { path: '/bucket', name: '桶管理', icon: <ShopOutlined /> },
  { path: '/manage/client', name: '服务器管理', icon: <ClusterOutlined />, adminOnly: true },
  { path: '/manage/user', name: '用户管理', icon: <TeamOutlined />, adminOnly: true },
];

export const MOBILE_ICON = {
  home: <HomeOutlined />,
  trash: <DeleteOutlined />,
};
