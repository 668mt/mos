import { Button, Form, Input, Modal, Radio, Select, Space, Switch, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { fetchAccessControls, signAccess } from '@/services/api';
import { copyText, safeAsync } from '@/utils';
import type { AccessControl, DirAndResourceVo } from '@/services/types';

export interface AddrModalProps {
  open: boolean;
  onClose: () => void;
  bucketName?: string;
  /** 目标资源 */
  record?: DirAndResourceVo | null;
}

type ExpireUnit = 'minute' | 'hour' | 'day' | 'month' | 'year' | 'ever';

const UNIT_SECONDS: Record<Exclude<ExpireUnit, 'ever'>, number> = {
  minute: 60,
  hour: 3600,
  day: 3600 * 24,
  month: 3600 * 24 * 30,
  year: 3600 * 24 * 30 * 365,
};

/**
 * 生成访问链（签名短链）。
 *
 * 服务端 `POST /member/access/sign`，`expireSeconds` 必须 ≥ 60。
 */
export default function AddrModal({ open, onClose, bucketName, record }: AddrModalProps) {
  const [form] = Form.useForm();
  const [openIds, setOpenIds] = useState<AccessControl[]>([]);
  const [signUrl, setSignUrl] = useState('');
  const [generating, setGenerating] = useState(false);

  const expireNumber = Form.useWatch('expireNumber', form);
  const expireUnit = Form.useWatch('expireUnit', form);
  const isPublic = !!record?.isPublic;

  useEffect(() => {
    if (!open || !bucketName) return;
    setSignUrl('');
    form.setFieldsValue({
      openId: undefined,
      expireNumber: 2,
      expireUnit: 'hour',
      render: false,
    });
    fetchAccessControls(bucketName)
      .then((list) => {
        const arr = list || [];
        setOpenIds(arr);
        if (arr.length > 0) {
          form.setFieldValue('openId', arr[0].openId);
        }
      })
      .catch(() => setOpenIds([]));
  }, [open, bucketName, form]);

  const genAddr = async (values?: any) => {
    const v = values || form.getFieldsValue();
    if (!v.openId) {
      message.warning('请先创建一个秘钥！');
      return;
    }
    const num = parseInt(String(v.expireNumber ?? 1), 10);
    if (!Number.isFinite(num) || num < 1) {
      message.warning('时间不能小于1');
      return;
    }

    let seconds: number;
    if (v.expireUnit === 'ever') {
      seconds = -1;
    } else {
      seconds = (UNIT_SECONDS[v.expireUnit as Exclude<ExpireUnit, 'ever'>] || 3600) * num;
      // 服务端要求 expireSeconds ≥ 60
      if (seconds < 60) seconds = 60;
    }

    setGenerating(true);
    try {
      const url = await signAccess({
        openId: v.openId,
        bucketName: bucketName!,
        resourceId: record?.isDir ? null : record?.id,
        dirId: record?.isDir ? record?.id : null,
        expireSeconds: seconds,
        render: !!v.render,
      });
      setSignUrl(url || '');
    } catch {
      // 错误已由拦截器处理
    } finally {
      setGenerating(false);
    }
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    await genAddr(values);
  };

  const onCopy = async () => {
    if (!signUrl) {
      message.warning('请先生成访问链');
      return;
    }
    const ok = await copyText(signUrl);
    if (ok) message.success('复制成功');
    else message.warning('该浏览器不支持自动复制，请手动选择复制');
  };

  return (
    <Modal
      title="访问链详情"
      open={open}
      onCancel={onClose}
      onOk={safeAsync(handleOk)}
      confirmLoading={generating}
      okText="生成"
      cancelText="关闭"
      destroyOnHidden
      maskClosable={false}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item label="秘钥" name="openId" rules={[{ required: true, message: '请选择秘钥' }]}>
          <Select
            placeholder="请选择秘钥"
            options={openIds.map((o) => ({
              value: o.openId,
              label: o.useInfo ? o.useInfo : `秘钥 #${o.openId}`,
            }))}
            onChange={() => setSignUrl('')}
          />
        </Form.Item>

        <Form.Item label="桶">
          <Input disabled value={bucketName} />
        </Form.Item>

        <Form.Item label="文件">
          <Input disabled value={record?.fileName || record?.path} />
        </Form.Item>

        <Form.Item label="有效时间">
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item name="expireNumber" noStyle>
              <Input
                type="number"
                min={1}
                disabled={isPublic || expireUnit === 'ever'}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <Form.Item name="expireUnit" noStyle>
              <Select
                style={{ width: 110 }}
                disabled={isPublic}
                options={[
                  { value: 'minute', label: '分钟' },
                  { value: 'hour', label: '小时' },
                  { value: 'day', label: '天' },
                  { value: 'month', label: '月' },
                  { value: 'year', label: '年' },
                  { value: 'ever', label: '永久' },
                ]}
                onChange={(v) => {
                  if (v === 'ever') form.setFieldValue('expireNumber', 1);
                  setSignUrl('');
                }}
              />
            </Form.Item>
          </Space.Compact>
        </Form.Item>

        <Form.Item label="是否使用渲染器" name="render">
          <Switch />
        </Form.Item>

        <Form.Item label="访问链">
          <Input.TextArea
            value={signUrl}
            rows={5}
            readOnly
            placeholder="点击「生成」创建访问链"
          />
          <Button
            size="small"
            icon={<CopyOutlined />}
            onClick={onCopy}
            style={{ marginTop: 8 }}
          >
            复制
          </Button>
        </Form.Item>

        {isPublic && (
          <Radio.Group style={{ display: 'none' }}>
            <Radio value={false} />
          </Radio.Group>
        )}
      </Form>
    </Modal>
  );
}
