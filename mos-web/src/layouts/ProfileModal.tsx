import { Form, Input, Modal, message } from 'antd';
import { useState } from 'react';
import { updateCurrentUser } from '@/services/api';
import { readCurrentUser, writeCurrentUser } from '@/models/account';
import { safeAsync } from '@/utils';

interface ProfileModalProps {
  open: boolean;
  onClose: () => void;
}

/**
 * 个人资料：修改姓名 / 密码。
 *
 * 服务端 `PUT /member/user` 要求：修改密码时必须提供 `currentPassword`。
 */
export default function ProfileModal({ open, onClose }: ProfileModalProps) {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const handleOk = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const payload: { name?: string; currentPassword?: string; password?: string } = {};
      if (values.name) payload.name = values.name;
      if (values.password) {
        payload.currentPassword = values.currentPassword;
        payload.password = values.password;
      }
      await updateCurrentUser(payload);
      const user = readCurrentUser();
      if (user) {
        writeCurrentUser({ ...user, name: values.name || user.name });
      }
      message.success('修改成功');
      form.resetFields();
      onClose();
      // 刷新页面以更新顶部展示的用户名
      window.location.reload();
    } catch {
      // 错误已在拦截器中处理
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title="个人资料"
      open={open}
      onCancel={onClose}
      onOk={safeAsync(handleOk)}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      destroyOnHidden
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item label="姓名" name="name">
          <Input placeholder="请输入姓名" />
        </Form.Item>
        <Form.Item
          label="当前密码"
          name="currentPassword"
          dependencies={['password']}
          rules={[
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!getFieldValue('password')) return Promise.resolve();
                if (!value) {
                  return Promise.reject(new Error('修改密码时必须输入当前密码'));
                }
                return Promise.resolve();
              },
            }),
          ]}
        >
          <Input.Password placeholder="不修改密码则留空" autoComplete="current-password" />
        </Form.Item>
        <Form.Item
          label="新密码"
          name="password"
          dependencies={['currentPassword']}
          rules={[
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value) return Promise.resolve();
                if (!getFieldValue('currentPassword')) {
                  return Promise.reject(new Error('请先输入当前密码'));
                }
                return Promise.resolve();
              },
            }),
          ]}
        >
          <Input.Password placeholder="不修改密码则留空" autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
