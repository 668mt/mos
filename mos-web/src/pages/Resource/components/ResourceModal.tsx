import { AutoComplete, Form, Input, Modal, Radio, message } from 'antd';
import { useEffect, useState } from 'react';
import { updateResource } from '@/services/api';
import { CONTENT_TYPES, safeAsync } from '@/utils';
import type { DirAndResourceVo } from '@/services/types';

export interface ResourceModalProps {
  open: boolean;
  onClose: () => void;
  bucketName?: string;
  /** 被编辑的资源 */
  record?: DirAndResourceVo | null;
  onOk?: () => void;
}

/**
 * 编辑资源：重命名 / 修改权限 / 修改响应头 Content-Type。
 */
export default function ResourceModal({
  open,
  onClose,
  bucketName,
  record,
  onOk,
}: ResourceModalProps) {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [typeOptions, setTypeOptions] = useState<string[]>(CONTENT_TYPES);

  useEffect(() => {
    if (!open || !record) return;
    form.setFieldsValue({
      pathname: record.path,
      isPublic: !!record.isPublic,
      contentType: record.contentType ?? '',
    });
    setTypeOptions(CONTENT_TYPES);
  }, [open, record, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    if (!bucketName || !record) return;
    setSaving(true);
    try {
      await updateResource(bucketName, record.id, {
        pathname: values.pathname,
        isPublic: !!values.isPublic,
        contentType: values.contentType || null,
      });
      message.success('更新成功');
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
      title="资源详情"
      open={open}
      onCancel={onClose}
      onOk={safeAsync(handleOk)}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      destroyOnHidden
      maskClosable={false}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          label="资源名"
          name="pathname"
          rules={[{ required: true, message: '请输入资源名' }]}
        >
          <Input onPressEnter={safeAsync(handleOk)} />
        </Form.Item>
        <Form.Item label="权限" name="isPublic">
          <Radio.Group optionType="button" buttonStyle="solid">
            <Radio.Button value={true}>公开</Radio.Button>
            <Radio.Button value={false}>私有</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="响应头" name="contentType">
          <AutoComplete
            options={typeOptions.map((t) => ({ value: t }))}
            onSearch={(text) =>
              setTypeOptions(
                CONTENT_TYPES.filter((v) => v.indexOf(text) !== -1),
              )
            }
            placeholder="Content-Type"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
