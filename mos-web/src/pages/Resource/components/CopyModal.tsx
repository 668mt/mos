import { Form, Input, Modal, Select, message } from 'antd';
import { useEffect, useState } from 'react';
import { copyResources, moveResources, selectDirs } from '@/services/api';
import { basename, normalizePath, safeAsync } from '@/utils';
import type { BucketVo, DirAndResourceVo } from '@/services/types';

export interface CopyModalProps {
  open: boolean;
  onClose: () => void;
  /** copy | move */
  type: 'copy' | 'move';
  buckets: BucketVo[];
  currentBucket?: string;
  currentDir: { path: string; id?: number | null };
  /** 选中的记录 */
  rows: DirAndResourceVo[];
  onOk?: () => void;
}

/**
 * 复制 / 移动到其他桶或目录。
 *
 * 服务端接口：
 * - `POST /member/resource/copy/{src}/to/{des}`
 * - `POST /member/resource/move/{src}/to/{des}`
 */
export default function CopyModal({
  open,
  onClose,
  type,
  buckets,
  currentBucket,
  currentDir,
  rows,
  onOk,
}: CopyModalProps) {
  const [form] = Form.useForm();
  const [pathOptions, setPathOptions] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  const isMove = type === 'move';
  const dirIds = rows.filter((r) => r.isDir).map((r) => r.id);
  const resourceIds = rows.filter((r) => !r.isDir).map((r) => r.id);

  const loadPaths = async (bucketName: string, path: string) => {
    if (!bucketName) return;
    try {
      const dirs = await selectDirs(bucketName, path || '/');
      setPathOptions(
        (dirs || [])
          .filter(
            (d) =>
              dirIds.indexOf(d.id) === -1 && d.id !== currentDir?.id,
          )
          .map((d) => d.path),
      );
    } catch {
      setPathOptions([]);
    }
  };

  useEffect(() => {
    if (!open) return;
    const desBucket = buckets[0]?.bucketName;
    form.setFieldsValue({
      srcBucketName: currentBucket,
      desBucketName: desBucket,
      desPath: normalizePath(currentDir?.path),
    });
    loadPaths(desBucket, normalizePath(currentDir?.path));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, currentBucket, buckets]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const desBucketName: string = values.desBucketName;
    if (!desBucketName) {
      message.warning('请选择目标桶！');
      return;
    }
    const desPath = normalizePath(values.desPath);

    if (desBucketName === currentBucket && desPath === normalizePath(currentDir?.path)) {
      message.warning(`不能${isMove ? '移动' : '复制'}到当前文件夹！`);
      return;
    }

    setSaving(true);
    try {
      const payload = { desPath, dirIds, resourceIds };
      if (isMove) {
        await moveResources(currentBucket!, desBucketName, payload);
      } else {
        await copyResources(currentBucket!, desBucketName, payload);
      }
      message.success(`${isMove ? '移动' : '复制'}成功！`);
      onClose();
      onOk?.();
    } catch {
      // 错误已由拦截器处理
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={isMove ? '移动选中文件' : '复制选中文件'}
      open={open}
      onCancel={onClose}
      onOk={safeAsync(handleOk)}
      confirmLoading={saving}
      okText="确定"
      cancelText="取消"
      destroyOnHidden
      maskClosable={false}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item label="从" name="srcBucketName">
          <Input disabled />
        </Form.Item>
        <Form.Item
          label={isMove ? '移动到' : '复制到'}
          name="desBucketName"
          rules={[{ required: true, message: '请选择目标桶' }]}
        >
          <Select
            options={buckets.map((b) => ({
              value: b.bucketName,
              label: b.bucketName,
            }))}
            onChange={(v) => {
              form.setFieldValue('desPath', '/');
              loadPaths(v, '/');
            }}
          />
        </Form.Item>
        <Form.Item
          label="目标路径"
          name="desPath"
          rules={[{ required: true, message: '请输入目标路径' }]}
        >
          <Select
            showSearch
            options={pathOptions.map((p) => ({ value: p, label: p }))}
            onSearch={(text) => {
              const v = form.getFieldValue('desBucketName');
              loadPaths(v, text);
            }}
            filterOption={(input, option) =>
              String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
            placeholder="选择或输入目标路径"
          />
        </Form.Item>
      </Form>
      <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 12 }}>
        共 {rows.length} 项
        {rows.length > 0 && `（${rows.slice(0, 3).map((r) => basename(r.path)).join('、')}${rows.length > 3 ? ' 等' : ''}）`}
      </div>
    </Modal>
  );
}
