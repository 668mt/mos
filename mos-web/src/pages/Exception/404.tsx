import { Button, Result } from 'antd';
import { history } from '@umijs/max';

export default function NotFound() {
  return (
    <div className="mos-exception">
      <div className="mos-exception-code">404</div>
      <Result
        status="404"
        title="404"
        subTitle="抱歉，你访问的页面不存在。"
        style={{ background: 'transparent', boxShadow: 'none' }}
        extra={
          <Button type="primary" onClick={() => history.push('/home')}>
            返回首页
          </Button>
        }
      />
    </div>
  );
}
