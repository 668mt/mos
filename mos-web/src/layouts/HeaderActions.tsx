import { Avatar, Dropdown, Menu, Space, Tag, Typography, theme } from 'antd';
import { LogoutOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { useState } from 'react';
import ProfileModal from './ProfileModal';

const { Text } = Typography;

export interface HeaderActionsProps {
  /** 当前用户 */
  user?: {
    id: number;
    name: string;
    username: string;
    isAdmin: boolean;
  } | null;
  /** 是否为管理员（决定是否展示管理菜单） */
  isAdmin?: boolean;
}

/**
 * 顶部右侧操作区：当前用户头像 + 下拉菜单。
 */
export default function HeaderActions({ user }: HeaderActionsProps) {
  const { setInitialState } = useModel('@@initialState') as any;
  const [profileOpen, setProfileOpen] = useState(false);
  const { token } = theme.useToken();

  if (!user) return null;

  const onLogout = () => {
    localStorage.removeItem('currentUser');
    setInitialState((s: any) => ({ ...s, currentUser: undefined }));
    history.push('/signin');
  };

  const menuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人资料',
      onClick: () => setProfileOpen(true),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true,
      onClick: onLogout,
    },
  ];

  return (
    <>
      <Dropdown menu={{ items: menuItems }} placement="bottomRight" trigger={['click']}>
        <Space className="mos-header-user" size={8}>
          <Avatar
            size={32}
            style={{ background: token.colorPrimaryBg, color: token.colorPrimary }}
            icon={<UserOutlined />}
          >
            {user.name?.charAt(0)?.toUpperCase()}
          </Avatar>
          <span className="mos-header-user-name">{user.name || user.username}</span>
        </Space>
      </Dropdown>
      <ProfileModal open={profileOpen} onClose={() => setProfileOpen(false)} />
    </>
  );
}

/** 顶部展示的管理员标记 */
export function AdminTag({ isAdmin }: { isAdmin?: boolean }) {
  if (!isAdmin) return null;
  return <Tag color="blue">管理员</Tag>;
}

/** 兼容旧版 Setting 入口占位 */
export function SettingEntry() {
  return <SettingOutlined style={{ display: 'none' }} />;
}

export { Text };
