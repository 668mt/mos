import { Form, Input, Modal, message } from 'antd';
import { useEffect, useState } from 'react';import { addDir, findDirByPath, mergeDir, updateDir } from '@/services/api';
import { normalizePath, safeAsync } from '@/utils';
import type { Dir, DirAndResourceVo } from '@/services/types';

export interface DirModalProps {
  open: boolean;
  onClose: () => void;
  /** 当前桶 */
  bucketName?: string;
  /** 当前所在目录 */
  currentDir: Dir;
  /** 编辑已有目录时传入记录 */
  record?: DirAndResourceVo | null;
  /** 保存成功回调 */
  onOk?: () => void;
}

/**
 * 创建 / 编辑文件夹。
 *
 * 语义与旧版 DirModal.vue 一致：
 * - 新建：父路径取当前目录，名称为空
 * - 重命名：若目标路径已存在，则提示合并（同名文件覆盖）
 */
export default function DirModal({
  open,
  onClose,
  bucketName,
  currentDir,
  record,
  onOk,
}: DirModalProps) {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    let parentPath = '/';
    let name = '';
    if (record?.id) {
      const path = normalizePath(record.path);
      if (path !== '/') {
        const index = path.lastIndexOf('/');
        parentPath = path.substring(0, index) || '/';
        name = path.substring(index + 1);
      }
    } else {
      parentPath = normalizePath(currentDir?.path) || '/';
    }
    form.setFieldsValue({ parentPath, name });
  }, [open, record, currentDir, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    if (!bucketName) return;

    let parentPath = (values.parentPath || '/').trim();
    let name = (values.name || '').trim();
    if (parentPath.endsWith('/')) parentPath = parentPath.slice(0, -1);
    if (name.startsWith('/')) name = name.slice(1);

    let path = `${parentPath}/${name}`;
    if (path !== '/' && path.endsWith('/')) path = path.slice(0, -1);

    setSaving(true);
    try {
      if (record?.id) {
        // 重命名：目标路径已存在则走合并流程
        const desDir = await findDirByPath(bucketName, path);
        if (desDir) {
          Modal.confirm({
            title: '合并确认',
            content: `文件夹 ${path} 已存在，是否将 ${record.path} 合并到 ${path}，同名文件将进行覆盖?`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
              await mergeDir(bucketName, record.id, desDir.id);
              message.success('合并成功');
              onClose();
              onOk?.();
            },
          });
          return;
        }
        await updateDir(bucketName, record.id, { path });
        message.success('修改成功');
      } else {
        await addDir(bucketName, { path });
        message.success('创建成功');
      }
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
      title={record?.id ? '重命名文件夹' : '创建文件夹'}
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
        <Form.Item label="父路径" name="parentPath">
          <Input placeholder="例如 /" />
        </Form.Item>
        <Form.Item
          label="名称"
          name="name"
          rules={[{ required: true, message: '请输入文件夹名称' }]}
        >
          <Input
            placeholder="请输入文件夹名称"
            onPressEnter={safeAsync(handleOk)}
            autoFocus
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

