import { Layout, Menu, Drawer, Button, Space, Typography, theme } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { history, useLocation, useModel } from '@umijs/max';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { MENU_ITEMS } from './menuConfig';
import HeaderActions from './HeaderActions';
import { useResponsive } from '@/hooks/useResponsive';
import { readCurrentUser } from '@/models/account';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export interface BasicLayoutProps {
  children?: ReactNode;
}

/** 根据当前路径找到激活的菜单 key */
function useSelectedKeys(): string[] {
  const location = useLocation();
  return useMemo(() => {
    const pathname = location.pathname;
    const hit = MENU_ITEMS.filter(
      (m) => pathname === m.path || pathname.startsWith(`${m.path}/`),
    ).sort((a, b) => b.path.length - a.path.length);
    return hit.length ? [hit[0].path] : [];
  }, [location.pathname]);
}

/**
 * 基础布局。
 *
 * - PC（≥ md）：固定侧边栏 + 顶部栏
 * - 平板 / 手机（< md）：抽屉式菜单，顶部栏保留汉堡按钮
 */
export default function BasicLayout({ children }: BasicLayoutProps) {
  const { initialState } = useModel('@@initialState') as any;
  const { token } = theme.useToken();
  const { isMobile } = useResponsive();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const selectedKeys = useSelectedKeys();

  const user = (initialState?.currentUser ?? readCurrentUser()) as any;
  const isAdmin = !!user?.isAdmin;

  // 未登录时跳转登录页
  useEffect(() => {
    if (!user) {
      history.replace('/signin');
    }
  }, [user]);

  const menuItems = useMemo(
    () =>
      MENU_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => ({
        key: item.path,
        icon: item.icon,
        label: item.name,
      })),
    [isAdmin],
  );

  // 路由变化时关闭移动端抽屉
  useEffect(() => {
    setDrawerOpen(false);
  }, [location.pathname]);

  const onMenuClick = ({ key }: { key: string }) => {
    history.push(key);
  };

  const logo = (
    <div className="mos-logo" onClick={() => history.push('/resource')}>
      <div className="mos-logo-mark">
        <CloudServerOutlined />
      </div>
      {!collapsed && !isMobile && (
        <div className="mos-logo-text">
          <Text strong>MOS</Text>
          <Text className="mos-logo-sub">对象存储</Text>
        </div>
      )}
    </div>
  );

  const menuNode = (
    <Menu
      mode="inline"
      theme="light"
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={onMenuClick}
      className="mos-side-menu"
    />
  );

  return (
    <Layout className="mos-layout">
      {!isMobile && (
        <Sider
          theme="light"
          width={224}
          collapsedWidth={64}
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          className="mos-sider"
        >
          {logo}
          {menuNode}
        </Sider>
      )}

      <Drawer
        placement="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={240}
        closable={false}
        styles={{ body: { padding: 0 } }}
        className="mos-drawer"
      >
        {logo}
        {menuNode}
      </Drawer>

      <Layout>
        <Header className="mos-header">
          <Space size={12}>
            <Button
              type="text"
              className="mos-header-trigger"
              icon={
                isMobile ? (
                  <MenuUnfoldOutlined />
                ) : collapsed ? (
                  <MenuUnfoldOutlined />
                ) : (
                  <MenuFoldOutlined />
                )
              }
              onClick={() => {
                if (isMobile) {
                  setDrawerOpen(true);
                } else {
                  setCollapsed(!collapsed);
                }
              }}
            />
            <div className="mos-header-title">
              <Text strong>{getPageTitle(location.pathname)}</Text>
            </div>
          </Space>
          <HeaderActions user={user} isAdmin={isAdmin} />
        </Header>
        <Content className="mos-content">{children}</Content>
      </Layout>
    </Layout>
  );
}

/** 路径 -> 页面标题 */
function getPageTitle(pathname: string): string {
  if (pathname.startsWith('/resource/dashboard')) return '仪表盘';
  if (pathname.startsWith('/resource/files')) return '资源管理';
  if (pathname.startsWith('/resource/trash')) return '回收站';
  if (pathname.startsWith('/resource')) return '资源管理';
  if (pathname.startsWith('/manage/client')) return '服务器管理';
  if (pathname.startsWith('/manage/user')) return '用户管理';
  return '资源管理';
}
