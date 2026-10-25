import { Button, Result } from 'antd';
import { history } from '@umijs/max';

export default function Forbidden() {
  return (
    <div className="mos-exception">
      <div className="mos-exception-code">403</div>
      <Result
        status="403"
        title="403"
        subTitle="抱歉，你无权访问该页面。"
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
