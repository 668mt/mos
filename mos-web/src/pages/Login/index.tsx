import {
  Alert,
  Button,
  Checkbox,
  Form,
  Input,
  Space,
  Typography,
  message,
} from 'antd';
import { CloudServerOutlined, LockOutlined, UserOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { useCallback, useEffect, useState } from 'react';
import { captchaUrl, login, needCaptcha } from '@/services/api';
import { readCurrentUser, writeCurrentUser } from '@/models/account';
import { isMobileDevice } from '@/utils';

const { Text } = Typography;

interface LoginForm {
  username: string;
  password: string;
  code?: string;
  rememberMe?: boolean;
}

/**
 * 登录页。
 *
 * - 登录地址为 `POST /login`（服务端 `loginProcessingUrl`）
 * - 同一用户名失败 3 次后服务端会要求验证码（`GET /kaptcha/check/{username}`）
 */
export default function LoginPage() {
  const [form] = Form.useForm<LoginForm>();
  const { setInitialState } = useModel('@@initialState') as any;
  const [loading, setLoading] = useState(false);
  const [needCode, setNeedCode] = useState(false);
  const [codeSrc, setCodeSrc] = useState(captchaUrl());
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    // 已登录则直接进入总览
    if (readCurrentUser()) {
      history.replace('/home');
    }
  }, []);

  const refreshCode = useCallback(() => {
    setCodeSrc(captchaUrl());
  }, []);

  const checkNeedCode = useCallback(
    async (username: string) => {
      if (!username) {
        setNeedCode(false);
        return;
      }
      try {
        const required = await needCaptcha(username);
        setNeedCode(!!required);
        if (required) refreshCode();
      } catch {
        // 忽略：校验失败不阻塞登录
      }
    },
    [refreshCode],
  );

  const onFinish = async (values: LoginForm) => {
    setLoading(true);
    setErrorMsg('');
    try {
      const user = await login({
        username: values.username,
        password: values.password,
        rememberMe: !!values.rememberMe,
        code: values.code,
      });
      writeCurrentUser(user);
      message.success('登录成功');
      // 同步更新全局状态，否则主布局仍认为未登录，会反复跳回登录页
      await setInitialState((s: any) => ({ ...s, currentUser: user }));
      if (isMobileDevice()) {
        // 移动端旧版会跳转到服务端渲染的 /mobile 页面
        window.location.href = '/mobile';
        return;
      }
      history.replace('/home');
    } catch (e: any) {
      setErrorMsg(e?.message || '登录失败');
      checkNeedCode(values.username);
      refreshCode();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mos-login">
      <div className="mos-login-card">
        <div className="mos-login-logo">
          <div className="mos-logo-mark">
            <CloudServerOutlined />
          </div>
        </div>
        <div className="mos-login-title">
          <Text>MOS</Text>
        </div>
        <div className="mos-login-desc">分布式对象存储服务</div>

        {errorMsg && (
          <Alert
            type="error"
            showIcon
            message={errorMsg}
            style={{ marginBottom: 16 }}
            closable
            onClose={() => setErrorMsg('')}
          />
        )}

        <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              size="large"
              prefix={<UserOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
              placeholder="用户名"
              autoComplete="username"
              onChange={(e) => {
                if (!needCode) checkNeedCode(e.target.value);
              }}
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              size="large"
              prefix={<LockOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>

          {needCode && (
            <Form.Item
              name="code"
              rules={[{ required: true, message: '请输入验证码' }]}
            >
              <div className="mos-login-captcha">
                <Input
                  size="large"
                  placeholder="验证码"
                  maxLength={8}
                  style={{ flex: 1 }}
                  onPressEnter={() => form.submit()}
                />
                <img src={codeSrc} alt="点击刷新验证码" onClick={refreshCode} />
              </div>
            </Form.Item>
          )}

          <Form.Item style={{ marginBottom: 12 }}>
            <Space style={{ width: '100%', justifyContent: 'space-between' }}>
              <Form.Item name="rememberMe" valuePropName="checked" noStyle>
                <Checkbox>记住我</Checkbox>
              </Form.Item>
              <Text type="secondary" style={{ fontSize: 12 }}>
                连续 5 次失败将锁定账号
              </Text>
            </Space>
          </Form.Item>

          <Button
            type="primary"
            size="large"
            htmlType="submit"
            block
            loading={loading}
          >
            登 录
          </Button>
        </Form>

        <div className="mos-login-footer">
          MOS · Martin Object Storage &nbsp;|&nbsp; © 668mt
        </div>
      </div>
    </div>
  );
}
