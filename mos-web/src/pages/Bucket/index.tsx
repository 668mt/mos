import {
  Button,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Transfer,
  message,
  type TableColumnsType,
} from 'antd';
import {
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  addAccessControl,
  addBucket,
  deleteAccessControl,
  deleteBucket,
  fetchAccessControls,
  fetchAllPerms,
  fetchBucketGrants,
  fetchBuckets,
  saveBucketGrants,
  updateAccessControl,
  updateBucket,
} from '@/services/api';
import { usePerms } from '@/hooks/usePerms';
import { useResponsive } from '@/hooks/useResponsive';
import { safeAsync } from '@/utils';
import type {
  AccessControl,
  BucketPerm,
  BucketVo,
  UserGrantVo,
} from '@/services/types';

/**
 * 桶管理。
 *
 * 功能：桶的增删改查、秘钥（访问控制）管理、桶授权（用户 + 权限）。
 */
export default function BucketPage() {
  const { refresh: refreshPerms } = usePerms();
  const { isMobile } = useResponsive();

  const [buckets, setBuckets] = useState<BucketVo[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  const [accessList, setAccessList] = useState<AccessControl[]>([]);

  const [bucketModalOpen, setBucketModalOpen] = useState(false);
  const [bucketForm] = Form.useForm();
  const [editingBucketId, setEditingBucketId] = useState<number | null>(null);
  const [savingBucket, setSavingBucket] = useState(false);

  const [accessModalOpen, setAccessModalOpen] = useState(false);
  const [accessForm] = Form.useForm();
  const [editingOpenId, setEditingOpenId] = useState<number | null>(null);
  const [savingAccess, setSavingAccess] = useState(false);

  const [grantOpen, setGrantOpen] = useState(false);
  const [allPerms, setAllPerms] = useState<BucketPerm[]>([]);
  const [grantUsers, setGrantUsers] = useState<UserGrantVo[]>([]);
  const [grantKeys, setGrantKeys] = useState<React.Key[]>([]);
  const [grantBucketId, setGrantBucketId] = useState<number | null>(null);
  const [savingGrant, setSavingGrant] = useState(false);

  const loadBuckets = useCallback(async () => {
    setLoading(true);
    try {
      const list = await fetchBuckets();
      setBuckets(list || []);
    } catch {
      /* 忽略 */
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBuckets();
    fetchAllPerms()
      .then((list) => setAllPerms(list || []))
      .catch(() => setAllPerms([]));
  }, [loadBuckets]);

  // 展开行时加载秘钥列表
  const onExpand = async (expanded: boolean, record: BucketVo) => {
    setExpandedRowKeys(expanded ? [record.id] : []);
    if (!expanded) return;
    try {
      const list = await fetchAccessControls(record.bucketName);
      setAccessList(list || []);
    } catch {
      setAccessList([]);
    }
  };

  /* ------------------------------------------------------------ 桶表单 */

  const onAddBucket = () => {
    setEditingBucketId(null);
    bucketForm.setFieldsValue({
      bucketName: '',
      defaultIsPublic: false,
      dataFragmentsAmount: 1,
    });
    setBucketModalOpen(true);
  };

  const onEditBucket = (record: BucketVo) => {
    setEditingBucketId(record.id);
    bucketForm.setFieldsValue({
      bucketName: record.bucketName,
      defaultIsPublic: !!record.defaultIsPublic,
      dataFragmentsAmount: record.dataFragmentsAmount,
    });
    setBucketModalOpen(true);
  };

  const saveBucket = async () => {
    const values = await bucketForm.validateFields();
    setSavingBucket(true);
    try {
      if (editingBucketId) {
        await updateBucket({ id: editingBucketId, ...values });
        message.success('更新成功！');
      } else {
        await addBucket(values);
        message.success('新增成功！');
      }
      setBucketModalOpen(false);
      loadBuckets();
      refreshPerms();
    } catch {
      /* 错误已由拦截器处理 */
    } finally {
      setSavingBucket(false);
    }
  };

  const onDeleteBucket = (record: BucketVo) => {
    Modal.confirm({
      title: '确定删除这个桶吗?',
      content: (
        <div>
          <div style={{ fontWeight: 500 }}>{record.bucketName}</div>
          <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
            桶内的所有文件会一并删除，且<strong>不可恢复</strong>
          </div>
        </div>
      ),
      okText: '确定删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteBucket(record.id);
        message.success('删除成功');
        loadBuckets();
        refreshPerms();
      },
    });
  };

  /* ------------------------------------------------------------ 秘钥 */

  const onGenerateAccess = (record: BucketVo) => {
    setEditingOpenId(null);
    accessForm.setFieldsValue({ useInfo: '' });
    accessForm.setFieldValue('bucketName', record.bucketName);
    setAccessModalOpen(true);
  };

  const onEditAccess = (access: AccessControl) => {
    setEditingOpenId(access.openId);
    accessForm.setFieldsValue({
      useInfo: access.useInfo,
      bucketName: access.bucketName,
    });
    setAccessModalOpen(true);
  };

  const saveAccess = async () => {
    const values = await accessForm.validateFields();
    const bucketName = values.bucketName;
    setSavingAccess(true);
    try {
      if (editingOpenId) {
        await updateAccessControl(bucketName, editingOpenId, { useInfo: values.useInfo });
        message.success('修改成功');
      } else {
        await addAccessControl(bucketName, { useInfo: values.useInfo });
        message.success('新增成功');
      }
      setAccessModalOpen(false);
      const expanded = buckets.find((b) => expandedRowKeys.includes(b.id));
      if (expanded) {
        setAccessList((await fetchAccessControls(expanded.bucketName)) || []);
      }
      refreshPerms();
    } catch {
      /* 错误已由拦截器处理 */
    } finally {
      setSavingAccess(false);
    }
  };

  const onDeleteAccess = (access: AccessControl) => {
    Modal.confirm({
      title: '确定删除这个秘钥吗?',
      content: (
        <div>
          <div>{access.useInfo || '（未填写用途）'}</div>
          <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
            删除后使用该秘钥的访问将失效
          </div>
        </div>
      ),
      okText: '确定删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteAccessControl(access.bucketName!, access.openId);
        message.success('删除成功');
        const expanded = buckets.find((b) => expandedRowKeys.includes(b.id));
        if (expanded) {
          setAccessList((await fetchAccessControls(expanded.bucketName)) || []);
        }
      },
    });
  };

  /* ------------------------------------------------------------ 授权 */

  const onGrant = async (record: BucketVo) => {
    setGrantBucketId(record.id);
    setGrantOpen(true);
    setGrantUsers([]);
    setGrantKeys([]);
    try {
      const result = await fetchBucketGrants(record.id);
      setGrantUsers(result?.allUsers || []);
      setGrantKeys(result?.userKeys || []);
    } catch {
      setGrantUsers([]);
      setGrantKeys([]);
    }
  };

  const saveGrant = async () => {
    if (!grantBucketId) return;
    const grants = grantUsers
      .filter((u) => grantKeys.map(String).includes(String(u.key)))
      .map((u) => ({ userId: u.id, perms: u.perms || [] }));
    setSavingGrant(true);
    try {
      await saveBucketGrants({ bucketId: grantBucketId, grants });
      message.success('授权成功');
      setGrantOpen(false);
      refreshPerms();
    } catch {
      /* 错误已由拦截器处理 */
    } finally {
      setSavingGrant(false);
    }
  };

  /* ------------------------------------------------------------ 表格 */

  const columns: TableColumnsType<BucketVo> = [
    {
      title: '所有者',
      dataIndex: 'owner',
      width: 120,
      render: (v: any, r: BucketVo) =>
        r.isOwn ? <Tag color="blue">{v}</Tag> : <Tag color="green">{v}</Tag>,
    },
    {
      title: '名称',
      dataIndex: 'bucketName',
      render: (v: string) => <span className="mos-bucket-name">{v}</span>,
    },
    {
      title: '默认权限',
      dataIndex: 'defaultIsPublic',
      width: 110,
      responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
      render: (v: any) =>
        v ? <Tag color="orange">公开</Tag> : <Tag color="green">私有</Tag>,
    },
    {
      title: '分片数',
      dataIndex: 'dataFragmentsAmount',
      width: 90,
      responsive: ['md', 'lg', 'xl', 'xxl'],
    },
    {
      title: '创建时间',
      dataIndex: 'createdDate',
      width: 170,
      responsive: ['lg', 'xl', 'xxl'],
    },
    {
      title: '修改时间',
      dataIndex: 'updatedDate',
      width: 170,
      responsive: ['xl', 'xxl'],
    },
    {
      title: '操作',
      width: isMobile ? 76 : 280,
      render: (_: any, record: BucketVo) =>
        isMobile ? (
          // 窄屏：操作收进「更多」下拉
          <Dropdown
            menu={{
              items: [
                { key: 'edit', icon: <EditOutlined />, label: '编辑' },
                { key: 'access', icon: <KeyOutlined />, label: '新增秘钥' },
                ...(record.isOwn
                  ? [
                      { key: 'grant', icon: <UserSwitchOutlined />, label: '授权' },
                      { type: 'divider' as const },
                      { key: 'delete', icon: <DeleteOutlined />, label: '删除', danger: true },
                    ]
                  : []),
              ],
              onClick: ({ key, domEvent }) => {
                domEvent.stopPropagation();
                if (key === 'edit') onEditBucket(record);
                else if (key === 'access') onGenerateAccess(record);
                else if (key === 'grant') onGrant(record);
                else if (key === 'delete') onDeleteBucket(record);
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
            <a onClick={() => onEditBucket(record)}>
              <EditOutlined /> 编辑
            </a>
            <a className="mos-danger-link" onClick={() => onGenerateAccess(record)}>
              <KeyOutlined /> 新增秘钥
            </a>
            {record.isOwn && (
              <a onClick={() => onGrant(record)}>
                <UserSwitchOutlined /> 授权
              </a>
            )}
            {record.isOwn && (
              <a className="mos-danger-link" onClick={() => onDeleteBucket(record)}>
                <DeleteOutlined /> 删除
              </a>
            )}
          </div>
        ),
    },
  ];

  const accessColumns: TableColumnsType<AccessControl> = [
    { title: 'openId', dataIndex: 'openId', width: 90 },
    { title: '用途', dataIndex: 'useInfo', width: 160 },
    {
      title: '私钥',
      dataIndex: 'secretKey',
      ellipsis: true,
      render: (v: string) => (
        <Tooltip title={v}>
          <span className="mos-secret-key">{v}</span>
        </Tooltip>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdDate',
      width: 170,
      responsive: ['md', 'lg', 'xl', 'xxl'],
    },
    {
      title: '修改时间',
      dataIndex: 'updatedDate',
      width: 170,
      responsive: ['lg', 'xl', 'xxl'],
    },
    {
      title: '操作',
      width: 130,
      render: (_: any, record: AccessControl) => (
        <div className="mos-table-actions">
          <a onClick={() => onEditAccess(record)}>
            <EditOutlined /> 编辑
          </a>
          <a className="mos-danger-link" onClick={() => onDeleteAccess(record)}>
            <DeleteOutlined /> 删除
          </a>
        </div>
      ),
    },
  ];

  /** 授权面板：Transfer 的数据源 */
  const transferData = useMemo(
    () =>
      grantUsers.map((u) => ({
        key: String(u.key),
        title: u.title,
        name: u.name,
        username: u.username,
        perms: u.perms || [],
      })),
    [grantUsers],
  );

  return (
    <div className="mos-card" style={{ padding: 16 }}>
      <div className="mos-toolbar">
        <Button type="primary" icon={<PlusOutlined />} onClick={onAddBucket}>
          添加
        </Button>
      </div>

      <Table<BucketVo>
        className="mos-table"
        rowKey="id"
        columns={columns}
        dataSource={buckets}
        loading={loading}
        scroll={{ x: isMobile ? 380 : 1000 }}
        expandable={{
          expandedRowKeys,
          onExpand: (expanded, record) => onExpand(expanded, record),
          expandedRowRender: () => (
            <Table<AccessControl>
              className="mos-table"
              rowKey="openId"
              columns={accessColumns}
              dataSource={accessList}
              pagination={false}
              size="small"
            />
          ),
        }}
        pagination={false}
      />

      {/* 桶表单 */}
      <Modal
        title={editingBucketId ? '编辑桶' : '新增桶'}
        open={bucketModalOpen}
        onCancel={() => setBucketModalOpen(false)}
        onOk={safeAsync(saveBucket)}
        confirmLoading={savingBucket}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={bucketForm} layout="vertical" preserve={false}>
          <Form.Item
            label="桶名称"
            name="bucketName"
            rules={[
              { required: true, message: '请输入桶名称' },
              { min: 2, max: 20, message: '桶名称长度需为 2-20 个字符' },
              {
                pattern: /^\w*[a-zA-Z]\w*$/,
                message: '桶名称只能包含字母、数字、下划线，且至少包含一个字母',
              },
            ]}
          >
            <Input placeholder="请输入桶名称" />
          </Form.Item>
          <Form.Item label="数据分片" name="dataFragmentsAmount">
            <InputNumber style={{ width: '100%' }} min={1} max={10} />
          </Form.Item>
          <Form.Item label="默认权限" name="defaultIsPublic">
            <Radio.Group>
              <Radio value={true}>公开</Radio>
              <Radio value={false}>私有</Radio>
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>

      {/* 秘钥表单 */}
      <Modal
        title="秘钥详情"
        open={accessModalOpen}
        onCancel={() => setAccessModalOpen(false)}
        onOk={safeAsync(saveAccess)}
        confirmLoading={savingAccess}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={accessForm} layout="vertical" preserve={false}>
          <Form.Item label="用途" name="useInfo">
            <Input placeholder="用于标识该秘钥的用途" />
          </Form.Item>
          <Form.Item label="桶" name="bucketName" hidden>
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      {/* 授权面板 */}
      <Modal
        title="桶授权"
        open={grantOpen}
        onCancel={() => setGrantOpen(false)}
        onOk={safeAsync(saveGrant)}
        confirmLoading={savingGrant}
        okText="确定"
        cancelText="取消"
        width={900}
        destroyOnHidden
      >
        <Transfer
          showSearch
          titles={['待授权用户', '已授权用户']}
          dataSource={transferData}
          targetKeys={grantKeys}
          onChange={(keys) => setGrantKeys(keys)}
          render={(item) => {
            const user = grantUsers.find((u) => String(u.key) === String(item.key));
            return (
              <div style={{ minWidth: 240 }}>
                <div style={{ fontWeight: 500 }}>{user?.name}</div>
                <div style={{ color: 'rgba(0,0,0,.45)', fontSize: 12 }}>
                  {user?.username}
                </div>
                <Select
                  mode="multiple"
                  style={{ width: '100%', marginTop: 6 }}
                  placeholder="请选择权限"
                  value={user?.perms || []}
                  options={allPerms.map((p) => ({ value: p, label: p }))}
                  onChange={(perms) =>
                    setGrantUsers((prev) =>
                      prev.map((u) =>
                        String(u.key) === String(item.key) ? { ...u, perms } : u,
                      ),
                    )
                  }
                />
              </div>
            );
          }}
          filterOption={(inputValue, option) =>
            (option.name?.indexOf(inputValue) !== -1) ||
            (option.username?.indexOf(inputValue) !== -1)
          }
        />
        <Space style={{ marginTop: 12, color: 'rgba(0,0,0,.45)', fontSize: 12 }}>
          提示：勾选用户后在右侧选择权限，保存后立即生效。
        </Space>
      </Modal>
    </div>
  );
}
