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
  type MenuProps,
  type TableColumnsType,
} from 'antd';
import {
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  RightOutlined,
  ShopOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons';
import { history } from '@umijs/max';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  addAccessControl,
  addBucket,
  deleteAccessControl,
  deleteBucket,
  fetchAccessControls,
  fetchAllPerms,
  fetchBucketGrants,
  saveBucketGrants,
  updateAccessControl,
  updateBucket,
} from '@/services/api';
import { useBucketContext } from '@/hooks/useBucketContext';
import { usePerms } from '@/hooks/usePerms';
import { safeAsync } from '@/utils';
import type {
  AccessControl,
  BucketPerm,
  BucketVo,
  UserGrantVo,
} from '@/services/types';

/**
 * 资源管理 —— 桶列表（桶的管理）。
 *
 * 这是「资源管理」的落地页：先看到自己拥有 / 被授权的全部桶，
 * 点击某个桶进入桶内，桶内有三个页签：仪表盘 / 资源管理 / 回收站。
 */
export default function BucketListPage() {
  const { refresh: refreshPerms } = usePerms();
  const { buckets, loading, reload } = useBucketContext();

  const [allPerms, setAllPerms] = useState<BucketPerm[]>([]);

  const [bucketModalOpen, setBucketModalOpen] = useState(false);
  const [bucketForm] = Form.useForm();
  const [editingBucketId, setEditingBucketId] = useState<number | null>(null);
  const [savingBucket, setSavingBucket] = useState(false);

  /** 秘钥列表弹窗 */
  const [accessListOpen, setAccessListOpen] = useState(false);
  /** 秘钥表单弹窗 */
  const [accessFormOpen, setAccessFormOpen] = useState(false);
  const [accessForm] = Form.useForm();
  const [editingOpenId, setEditingOpenId] = useState<number | null>(null);
  const [savingAccess, setSavingAccess] = useState(false);
  const [accessList, setAccessList] = useState<AccessControl[]>([]);
  /** 秘钥弹窗对应的桶 */
  const [accessBucket, setAccessBucket] = useState<BucketVo | null>(null);

  const [grantOpen, setGrantOpen] = useState(false);
  const [grantUsers, setGrantUsers] = useState<UserGrantVo[]>([]);
  const [grantKeys, setGrantKeys] = useState<React.Key[]>([]);
  const [grantBucketId, setGrantBucketId] = useState<number | null>(null);
  const [savingGrant, setSavingGrant] = useState(false);

  useEffect(() => {
    fetchAllPerms()
      .then((list) => setAllPerms(list || []))
      .catch(() => setAllPerms([]));
  }, []);

  /** 进入桶内（默认落在仪表盘） */
  const enterBucket = useCallback((bucketName: string) => {
    history.push(`/resource/dashboard?b=${encodeURIComponent(bucketName)}`);
  }, []);

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
      reload();
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
        reload();
        refreshPerms();
      },
    });
  };

  /* ------------------------------------------------------------ 秘钥 */

  const loadAccessList = useCallback(async (bucketName: string) => {
    try {
      const list = await fetchAccessControls(bucketName);
      setAccessList(list || []);
    } catch {
      setAccessList([]);
    }
  }, []);

  const onOpenAccess = (record: BucketVo) => {
    setAccessBucket(record);
    setAccessList([]);
    setAccessListOpen(true);
    loadAccessList(record.bucketName);
  };

  const onGenerateAccess = (record: BucketVo) => {
    setEditingOpenId(null);
    accessForm.setFieldsValue({ useInfo: '' });
    accessForm.setFieldValue('bucketName', record.bucketName);
    setAccessFormOpen(true);
  };

  const onEditAccess = (access: AccessControl) => {
    setEditingOpenId(access.openId);
    accessForm.setFieldsValue({
      useInfo: access.useInfo,
      bucketName: access.bucketName,
    });
    setAccessFormOpen(true);
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
      setAccessFormOpen(false);
      await loadAccessList(bucketName);
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
        if (accessBucket) {
          loadAccessList(accessBucket.bucketName);
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

  /* ------------------------------------------------------------ 卡片操作菜单 */

  const buildCardActions = (record: BucketVo): MenuProps['items'] => {
    const items: MenuProps['items'] = [
      { key: 'edit', icon: <EditOutlined />, label: '编辑桶' },
      { key: 'access', icon: <KeyOutlined />, label: '查看秘钥' },
    ];
    if (record.isOwn) {
      items.push({ key: 'grant', icon: <UserSwitchOutlined />, label: '授权用户' });
      items.push({ type: 'divider' });
      items.push({
        key: 'delete',
        icon: <DeleteOutlined />,
        label: '删除桶',
        danger: true,
      });
    }
    return items;
  };

  const onCardAction = (key: string, record: BucketVo) => {
    if (key === 'edit') onEditBucket(record);
    else if (key === 'access') onOpenAccess(record);
    else if (key === 'grant') onGrant(record);
    else if (key === 'delete') onDeleteBucket(record);
  };

  /* ------------------------------------------------------------ 秘钥表格 */

  const accessColumns: TableColumnsType<AccessControl> = useMemo(
    () => [
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
    ],
    [],
  );

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
    <div>
      <div className="mos-page-head">
        <div>
          <div className="mos-page-title">资源管理</div>
          <div className="mos-page-desc">
            管理存储空间，点击桶即可进入仪表盘、资源管理与回收站。
          </div>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={onAddBucket}>
          新建桶
        </Button>
      </div>

      {buckets.length === 0 && !loading ? (
        <div className="mos-card" style={{ padding: 16 }}>
          <div className="mos-empty">
            <ShopOutlined className="mos-empty-icon" />
            <div className="mos-empty-title">还没有任何桶</div>
            <div className="mos-empty-desc">创建一个桶，开始存储你的文件。</div>
            <Button type="primary" icon={<PlusOutlined />} onClick={onAddBucket}>
              新建桶
            </Button>
          </div>
        </div>
      ) : (
        <div className="mos-bucket-grid">
          {buckets.map((record) => (
            <div key={record.id} className="mos-bucket-card">
              <div
                className="mos-bucket-card-head"
                onClick={() => enterBucket(record.bucketName)}
              >
                <div className="mos-bucket-card-icon">
                  <ShopOutlined />
                </div>
                <div className="mos-bucket-card-info">
                  <div className="mos-bucket-card-name">{record.bucketName}</div>
                  <div className="mos-bucket-card-tags">
                    {record.isOwn ? (
                      <Tag color="blue">我的</Tag>
                    ) : (
                      <Tag color="green">{record.owner}</Tag>
                    )}
                    {record.defaultIsPublic ? (
                      <Tag color="orange">公开</Tag>
                    ) : (
                      <Tag color="green">私有</Tag>
                    )}
                  </div>
                </div>
                <Dropdown
                  menu={{
                    items: buildCardActions(record),
                    onClick: ({ key, domEvent }) => {
                      domEvent.stopPropagation();
                      onCardAction(key, record);
                    },
                  }}
                  trigger={['click']}
                >
                  <a
                    className="mos-row-more"
                    onClick={(e) => e.stopPropagation()}
                  >
                    更多 <DownOutlined />
                  </a>
                </Dropdown>
              </div>

              <div className="mos-bucket-card-meta">
                <div className="mos-bucket-card-meta-item">
                  <span className="mos-bucket-card-meta-label">分片数</span>
                  <span>{record.dataFragmentsAmount}</span>
                </div>
                <div className="mos-bucket-card-meta-item">
                  <span className="mos-bucket-card-meta-label">创建时间</span>
                  <span>{record.createdDate || '-'}</span>
                </div>
              </div>

              <div className="mos-bucket-card-foot">
                <a
                  className="mos-bucket-card-enter"
                  onClick={() => enterBucket(record.bucketName)}
                >
                  进入桶 <RightOutlined />
                </a>
              </div>
            </div>
          ))}
        </div>
      )}

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

      {/* 秘钥列表 */}
      <Modal
        title={`秘钥列表${accessBucket ? ` - ${accessBucket.bucketName}` : ''}`}
        open={accessListOpen}
        onCancel={() => setAccessListOpen(false)}
        footer={null}
        width={900}
        destroyOnHidden
      >
        <div className="mos-toolbar">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            disabled={!accessBucket}
            onClick={() => accessBucket && onGenerateAccess(accessBucket)}
          >
            新增秘钥
          </Button>
        </div>
        <Table<AccessControl>
          className="mos-table"
          rowKey="openId"
          columns={accessColumns}
          dataSource={accessList}
          pagination={false}
          size="small"
          locale={{ emptyText: '还没有秘钥' }}
        />
      </Modal>

      {/* 秘钥表单 */}
      <Modal
        title="秘钥详情"
        open={accessFormOpen}
        onCancel={() => setAccessFormOpen(false)}
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
