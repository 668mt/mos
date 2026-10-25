import {
  Button,
  Dropdown,
  Form,
  Input,
  Modal,
  Space,
  Switch,
  Table,
  Tag,
  message,
  type TableColumnsType,
} from 'antd';
import {
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  PlusOutlined,
  UnlockOutlined,
} from '@ant-design/icons';
import { useCallback, useEffect, useState } from 'react';
import {
  addUser,
  deleteUser,
  fetchUsers,
  unlockUser,
  updateUser,
} from '@/services/api';
import { useResponsive } from '@/hooks/useResponsive';
import { safeAsync } from '@/utils';
import type { User } from '@/services/types';

/**
 * 用户管理（管理员）。
 *
 * 接口：`/admin/user/**`
 */
export default function UserPage() {
  const { isMobile } = useResponsive();
  const [rows, setRows] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const load = useCallback(
    async (params: { pageNum?: number; pageSize?: number }) => {
      setLoading(true);
      try {
        const page = await fetchUsers({
          pageNum: params.pageNum ?? 1,
          pageSize: params.pageSize ?? 10,
        });
        setRows(page?.list || []);
        setPagination({
          current: page?.pageNum || 1,
          pageSize: page?.pageSize || params.pageSize || 10,
          total: page?.total || 0,
        });
      } catch {
        setRows([]);
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    load({ pageNum: 1, pageSize: 10 });
  }, [load]);

  const onAdd = () => {
    setEditingId(null);
    form.setFieldsValue({
      name: '',
      username: '',
      password: '',
      confirm_password: '',
      isEnable: true,
      isAdmin: false,
    });
    setModalOpen(true);
  };

  const onEdit = (record: User) => {
    setEditingId(record.id);
    form.setFieldsValue({
      name: record.name,
      username: record.username,
      password: undefined,
      confirm_password: undefined,
      isEnable: record.isEnable,
      isAdmin: record.isAdmin,
    });
    setModalOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editingId) {
        const payload: any = {
          id: editingId,
          name: values.name,
          username: values.username,
          isEnable: values.isEnable,
          isAdmin: values.isAdmin,
        };
        if (values.password) payload.password = values.password;
        await updateUser(payload);
        message.success('更新成功！');
      } else {
        await addUser({
          name: values.name,
          username: values.username,
          password: values.password,
          isEnable: values.isEnable,
          isAdmin: values.isAdmin,
        });
        message.success('新增成功！');
      }
      setModalOpen(false);
      load({ pageNum: pagination.current, pageSize: pagination.pageSize });
    } catch {
      /* 错误已由拦截器处理 */
    } finally {
      setSaving(false);
    }
  };

  const onUnlock = async (record: User) => {
    try {
      await unlockUser(record.username);
      message.success('解锁成功!');
      load({ pageNum: pagination.current, pageSize: pagination.pageSize });
    } catch {
      /* 错误已由拦截器处理 */
    }
  };

  const onDelete = (record: User) => {
    Modal.confirm({
      title: '确定删除这个用户吗?',
      content: (
        <div>
          <div>
            {record.name}（{record.username}）
          </div>
          <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
            删除后该用户将无法登录，其授权会被清除
          </div>
        </div>
      ),
      okText: '确定删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteUser(record.id);
        message.success('删除成功');
        load({ pageNum: pagination.current, pageSize: pagination.pageSize });
      },
    });
  };

  const columns: TableColumnsType<User> = [
    { title: '姓名', dataIndex: 'name', width: 130 },
    { title: '用户名', dataIndex: 'username', width: 150 },
    {
      title: '是否管理员',
      dataIndex: 'isAdmin',
      width: 110,
      responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
      render: (v: any) => (v ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '是否启用',
      dataIndex: 'isEnable',
      width: 110,
      responsive: ['md', 'lg', 'xl', 'xxl'],
      render: (v: any) => (v ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>),
    },
    {
      title: '是否被锁定',
      dataIndex: 'locked',
      width: 110,
      responsive: ['md', 'lg', 'xl', 'xxl'],
      render: (v: any) => (v ? <Tag color="red">是</Tag> : <Tag>否</Tag>),
    },
    {
      title: '上次登录时间',
      dataIndex: 'lastLoginDate',
      width: 170,
      responsive: ['lg', 'xl', 'xxl'],
    },
    {
      title: '登录次数',
      dataIndex: 'loginTimes',
      width: 100,
      responsive: ['lg', 'xl', 'xxl'],
    },
    { title: '创建时间', dataIndex: 'createdDate', width: 170, responsive: ['xl', 'xxl'] },
    { title: '修改时间', dataIndex: 'updatedDate', width: 170, responsive: ['xl', 'xxl'] },
    {
      title: '操作',
      width: isMobile ? 76 : 220,
      render: (_: any, record: User) =>
        isMobile ? (
          // 窄屏：操作收进「更多」下拉
          <Dropdown
            menu={{
              items: [
                { key: 'edit', icon: <EditOutlined />, label: '编辑' },
                { key: 'unlock', icon: <UnlockOutlined />, label: '解锁' },
                { type: 'divider' },
                { key: 'delete', icon: <DeleteOutlined />, label: '删除', danger: true },
              ],
              onClick: ({ key, domEvent }) => {
                domEvent.stopPropagation();
                if (key === 'edit') onEdit(record);
                else if (key === 'unlock') onUnlock(record);
                else if (key === 'delete') onDelete(record);
              },
            }}
            trigger={['click']}
          >
            <a className="mos-row-more">
              更多 <DownOutlined />
            </a>
          </Dropdown>
        ) : (
          <div className="mos-table-actions">
            <a onClick={() => onEdit(record)}>
              <EditOutlined /> 编辑
            </a>
            <a onClick={() => onUnlock(record)}>
              <UnlockOutlined /> 解锁
            </a>
            <a className="mos-danger-link" onClick={() => onDelete(record)}>
              <DeleteOutlined /> 删除
            </a>
          </div>
        ),
    },
  ];

  return (
    <div className="mos-card" style={{ padding: 16 }}>
      <div className="mos-toolbar">
        <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
          添加
        </Button>
      </div>

      <Table<User>
        className="mos-table"
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={loading}
        scroll={{ x: isMobile ? 380 : 1350 }}
        size={isMobile ? 'small' : 'middle'}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (total) => `数据总数：${total}`,
          onChange: (page, pageSize) => load({ pageNum: page, pageSize }),
        }}
      />

      <Modal
        title={editingId ? '编辑用户' : '新增用户'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={safeAsync(save)}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="姓名"
            name="name"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            dependencies={['confirm_password']}
            rules={[
              ...(editingId
                ? []
                : [{ required: true, message: '请输入密码' }]),
              {
                validator: (_: any, value: string) => {
                  if (!value) return Promise.resolve();
                  if (value !== form.getFieldValue('confirm_password')) {
                    return Promise.reject(new Error('两次密码不相等'));
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Input.Password placeholder="不填写表示不修改" autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            label="确认密码"
            name="confirm_password"
            dependencies={['password']}
            rules={[
              ...(editingId
                ? []
                : [{ required: true, message: '请确认密码' }]),
              {
                validator: (_: any, value: string) => {
                  if (!value) return Promise.resolve();
                  if (value !== form.getFieldValue('password')) {
                    return Promise.reject(new Error('两次密码不相等'));
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Input.Password placeholder="不填写表示不修改" autoComplete="new-password" />
          </Form.Item>
          <Space size={32}>
            <Form.Item label="是否启用" name="isEnable" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="是否管理员" name="isAdmin" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
