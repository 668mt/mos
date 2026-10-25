import { Outlet } from '@umijs/max';
import BasicLayout from './BasicLayout';

/**
 * 主布局路由包装器。
 *
 * 未登录时跳转 `/signin`；登录页与查看器页面不使用本布局。
 */
export default function LayoutWrapper() {
  return (
    <BasicLayout>
      <Outlet />
    </BasicLayout>
  );
}
