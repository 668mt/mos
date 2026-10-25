import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Progress,
  Radio,
  Select,
  Space,
  Switch,
  Upload,
  message,
} from 'antd';
import type { UploadFile } from 'antd';
import { DeleteOutlined, InboxOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  checkFileExists,
  initUpload,
  mergeUpload,
  uploadChunk,
} from '@/services/api';
import { md5File, splitFile } from '@/utils/upload';
import { CONTENT_TYPES, basename, joinPath, safeAsync } from '@/utils';
import type { BucketVo, Dir } from '@/services/types';

const { Dragger } = Upload;

export interface UploadModalProps {
  open: boolean;
  onClose: () => void;
  /** 可选桶列表 */
  buckets: BucketVo[];
  /** 默认选中的桶 */
  currentBucket?: string;
  /** 当前目录 */
  currentDir: Dir;
  onOk?: () => void;
}

interface UploadItem {
  /** 唯一 key */
  key: string;
  file: File;
  pathname: string;
  status: 'pending' | 'uploading' | 'done' | 'error';
  errorMsg?: string;
  percent: number;
}

/** 上传进度（整体） */
interface Progress {
  current: string;
  fileIndex: number;
  files: number;
  percent: number;
  errorMsg?: string;
}

/**
 * 上传弹窗。
 *
 * 流程与旧版 Upload.vue 完全一致：
 * 1. `POST /member/resource/{bucket}/checkFile/isExists` 批量重名检查
 * 2. 计算整文件 MD5（分片 2MB 读取）
 * 3. `POST /upload/{bucket}/init` —— 若 `fileExists` 为 true 则为秒传，直接完成
 * 4. 逐片 `POST /upload/{bucket}`，跳过 `existedChunkIndexs` 中已存在的分片（断点续传）
 * 5. `POST /upload/mergeFiles?wait=true` 合并
 */
export default function UploadModal({
  open,
  onClose,
  buckets,
  currentBucket,
  currentDir,
  onOk,
}: UploadModalProps) {
  const [form] = Form.useForm();
  const [items, setItems] = useState<UploadItem[]>([]);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState<Progress>({
    current: '',
    fileIndex: 1,
    files: 1,
    percent: 0,
  });
  const [cover, setCover] = useState(false);
  const [isDirectory, setIsDirectory] = useState(false);
  const [typeOptions, setTypeOptions] = useState<string[]>(CONTENT_TYPES);
  const [existResults, setExistResults] = useState<Record<string, boolean>>({});
  const uploadingRef = useRef(false);

  const reset = useCallback(() => {
    setItems([]);
    setUploading(false);
    uploadingRef.current = false;
    setProgress({ current: '', fileIndex: 1, files: 1, percent: 0 });
    setCover(false);
    setIsDirectory(false);
    setExistResults({});
    setTypeOptions(CONTENT_TYPES);
  }, []);

  useEffect(() => {
    if (!open) return;
    reset();
    form.setFieldsValue({
      bucketName: currentBucket,
      isPublic: false,
      contentType: '',
    });
  }, [open, currentBucket, form, reset]);

  /** 计算目标路径 */
  const getUploadPathname = useCallback(
    (file: File) => {
      const relative = (file as any).webkitRelativePath;
      const name = relative || file.name;
      return joinPath(normalizeDir(currentDir?.path), name);
    },
    [currentDir],
  );

  const updateItem = useCallback((key: string, patch: Partial<UploadItem>) => {
    setItems((prev) =>
      prev.map((it) => (it.key === key ? { ...it, ...patch } : it)),
    );
  }, []);

  /** 新增待上传文件 */
  const beforeUpload = useCallback(
    (file: File) => {
      const pathname = getUploadPathname(file);
      setItems((prev) => {
        if (prev.some((it) => it.pathname === pathname)) {
          message.warning(`文件已存在：${pathname}`);
          return prev;
        }
        return [
          ...prev,
          {
            key: `${pathname}-${Date.now()}-${prev.length}`,
            file,
            pathname,
            status: 'pending',
            percent: 0,
          },
        ];
      });
      return false;
    },
    [getUploadPathname],
  );

  const removeItem = useCallback((key: string) => {
    if (uploadingRef.current) return;
    setItems((prev) => prev.filter((it) => it.key !== key));
  }, []);

  /** 上传单个文件（秒传 / 分片 / 合并） */
  const uploadOne = useCallback(
    async (
      item: UploadItem,
      bucketName: string,
      isPublic: boolean,
      contentType: string,
      index: number,
      total: number,
    ) => {
      const { file, pathname } = item;
      const splitResult = splitFile(file);
      const { chunks, totalSize, uploadParts } = splitResult;

      updateItem(item.key, { status: 'uploading', percent: 0 });
      setProgress({
        current: basename(pathname),
        fileIndex: index + 1,
        files: total,
        percent: 0,
      });

      // 1) 计算整文件 MD5
      const totalMd5 = await md5File(file, (c, i) => {
        const p = Math.floor((5 * i) / c);
        setProgress((prev) => ({ ...prev, percent: p }));
      });

      // 2) 初始化上传
      const initResult = await initUpload(bucketName, {
        pathname,
        chunks,
        totalMd5,
        totalSize,
        contentType: contentType || undefined,
        isPublic,
        cover,
        lastModified: file.lastModified,
      });

      // 秒传：服务端已存在相同 md5 + size 的文件
      if (initResult?.fileExists) {
        updateItem(item.key, { status: 'done', percent: 100 });
        setProgress({ current: basename(pathname), fileIndex: index + 1, files: total, percent: 100 });
        message.success(`${pathname} 上传成功（秒传）`);
        return;
      }

      const existed = initResult?.existedChunkIndexs || [];

      // 3) 逐个上传分片
      for (let chunkIndex = 0; chunkIndex < uploadParts.length; chunkIndex++) {
        if (existed.indexOf(chunkIndex) !== -1) {
          // 断点续传：跳过已上传的分片
          updateItem(item.key, {
            percent: Math.floor(
              ((5 + ((chunkIndex + 1) / chunks) * 94) / 100) * 100,
            ),
          });
          continue;
        }

        const uploadPart = uploadParts[chunkIndex];
        const chunkMd5 = await md5File(uploadPart);
        const formData = new FormData();
        formData.append('cover', String(cover));
        formData.append('contentType', contentType || '');
        formData.append('isPublic', String(isPublic));
        formData.append('file', uploadPart, basename(pathname));
        formData.append('pathname', pathname);
        formData.append('totalMd5', totalMd5);
        formData.append('totalSize', String(totalSize));
        formData.append('chunkMd5', chunkMd5);
        formData.append('chunkSize', String(uploadPart.size));
        formData.append('chunkIndex', String(chunkIndex));

        await uploadChunk(bucketName, formData);

        const p = Math.floor(5 + ((chunkIndex + 1) / chunks) * 94);
        updateItem(item.key, { percent: p });
        setProgress({
          current: basename(pathname),
          fileIndex: index + 1,
          files: total,
          percent: p,
        });
      }

      // 4) 合并分片
      await mergeUpload({
        bucketName,
        pathname,
        totalMd5,
        totalSize,
        chunks,
        lastModified: file.lastModified,
        contentType: contentType || undefined,
        isPublic,
        cover,
        updateMd5: false,
        wait: true,
      });

      updateItem(item.key, { status: 'done', percent: 100 });
      setProgress({
        current: basename(pathname),
        fileIndex: index + 1,
        files: total,
        percent: 100,
      });
      message.success(`${pathname} 上传成功`);
    },
    [cover, updateItem],
  );

  const handleOk = async () => {
    if (items.length === 0) {
      message.warning('请先选择要上传的文件！');
      return;
    }

    const values = await form.validateFields();
    const bucketName: string = values.bucketName;
    if (!bucketName) {
      message.warning('请选择桶');
      return;
    }

    // 重名检查
    const pathnames = items.map((it) => it.pathname);
    try {
      const checkResult = await checkFileExists(bucketName, pathnames);
      setExistResults(checkResult?.checkResults || {});
    } catch {
      // 忽略：仍允许继续（服务端 init 也会校验）
    }

    // 覆盖模式下忽略重名
    if (!cover) {
      const results = existResults;
      const duplicated = pathnames.filter((p) => results[p]);
      if (duplicated.length > 0) {
        // 用最新的检查结果再判断一次
        const fresh = await checkFileExists(bucketName, pathnames);
        const dup = pathnames.filter((p) => fresh?.checkResults?.[p]);
        if (dup.length > 0) {
          Modal.confirm({
            title: '存在同名文件',
            content: `以下文件已存在：${dup.join('、')}。是否覆盖上传？`,
            okText: '覆盖上传',
            cancelText: '取消',
            onOk: () => doUpload(values, bucketName, true),
          });
          return;
        }
      }
    }

    doUpload(values, bucketName, false);
  };

  const doUpload = async (
    values: any,
    bucketName: string,
    forceCover: boolean,
  ) => {
    const isPublic = !!values.isPublic;
    const contentType = values.contentType || '';
    if (forceCover) setCover(true);

    uploadingRef.current = true;
    setUploading(true);
    setProgress({
      current: '',
      fileIndex: 1,
      files: items.length,
      percent: 0,
    });

    try {
      for (let i = 0; i < items.length; i++) {
        await uploadOne(items[i], bucketName, isPublic, contentType, i, items.length);
      }
      message.success('上传完成！');
      onClose();
      onOk?.();
    } catch (e: any) {
      const msg = e?.message || '上传失败';
      setProgress((prev) => ({ ...prev, errorMsg: msg }));
      message.error(msg);
      setUploading(false);
      uploadingRef.current = false;
      // 标记未完成的文件
      setItems((prev) =>
        prev.map((it) =>
          it.status === 'uploading' ? { ...it, status: 'error', errorMsg: msg } : it,
        ),
      );
    }
  };

  const uploadFileList: UploadFile[] = useMemo(
    () =>
      items.map((it) => ({
        uid: it.key,
        name: basename(it.pathname),
        status:
          it.status === 'uploading'
            ? 'uploading'
            : it.status === 'done'
            ? 'done'
            : it.status === 'error'
            ? 'error'
            : undefined,
        percent: it.percent,
      })),
    [items],
  );

  return (
    <Modal
      title="上传"
      open={open}
      onCancel={uploading ? () => undefined : onClose}
      onOk={safeAsync(handleOk)}
      okText="上传"
      cancelText={uploading ? '切到后台' : '取消'}
      confirmLoading={uploading}
      maskClosable={false}
      destroyOnHidden
      width={860}
    >
      {(uploading || progress.errorMsg) && (
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 6, fontSize: 13 }}>
            （{progress.fileIndex}/{progress.files}）[{progress.current}]{' '}
            {progress.errorMsg
              ? `上传失败：${progress.errorMsg}`
              : uploading
              ? '上传中...'
              : '上传完成'}
          </div>
          <Progress
            percent={progress.percent}
            status={progress.errorMsg ? 'exception' : uploading ? 'active' : 'success'}
          />
        </div>
      )}

      <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
        <div style={{ flex: '1 1 320px', minWidth: 280 }}>
          <Form form={form} layout="vertical" preserve={false}>
            <Form.Item
              label="桶"
              name="bucketName"
              rules={[{ required: true, message: '请选择桶' }]}
            >
              <Select
                options={buckets.map((b) => ({
                  value: b.bucketName,
                  label: b.bucketName,
                }))}
              />
            </Form.Item>

            <Form.Item label="是否覆盖">
              <Switch
                checked={cover}
                onChange={(v) => {
                  setCover(v);
                  form.validateFields();
                }}
              />
            </Form.Item>

            <Form.Item label="权限" name="isPublic">
              <Radio.Group>
                <Radio value={true}>公开</Radio>
                <Radio value={false}>私有</Radio>
              </Radio.Group>
            </Form.Item>

            <Form.Item label="响应头" name="contentType">
              <Select
                showSearch
                allowClear
                placeholder="Content-Type"
                options={typeOptions.map((t) => ({ value: t, label: t }))}
                onSearch={(text) =>
                  setTypeOptions(CONTENT_TYPES.filter((v) => v.indexOf(text) !== -1))
                }
                filterOption={(input, option) =>
                  String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
              />
            </Form.Item>

            <Form.Item label="文件 / 文件夹">
              <Radio.Group
                value={isDirectory}
                onChange={(e) => setIsDirectory(e.target.value)}
              >
                <Radio value={false}>文件</Radio>
                <Radio value={true}>文件夹</Radio>
              </Radio.Group>
            </Form.Item>

            <Form.Item label="选择">
              <Dragger
                multiple
                directory={isDirectory}
                showUploadList={false}
                beforeUpload={beforeUpload}
                disabled={uploading}
                style={{ padding: '18px 8px' }}
              >
                <p className="ant-upload-drag-icon">
                  <InboxOutlined />
                </p>
                <p className="ant-upload-text">
                  点击或拖动{isDirectory ? '文件夹' : '文件'}至此
                </p>
                <p className="ant-upload-hint">支持分片上传、秒传与断点续传</p>
              </Dragger>
            </Form.Item>
          </Form>
        </div>

        <div style={{ flex: '1 1 320px', minWidth: 280 }}>
          {items.length === 0 ? (
            <Alert
              type="info"
              showIcon
              message="暂无文件上传"
              style={{ marginTop: 60 }}
            />
          ) : (
            <div className="mos-upload-list">
              {items.map((item, index) => (
                <div className="mos-upload-item" key={item.key}>
                  <div className="mos-upload-item-head">
                    <span className="mos-upload-item-name" title={item.pathname}>
                      {index + 1}. {basename(item.pathname)}
                    </span>
                    <Space size={4}>
                      {item.status === 'uploading' && <span>上传中...</span>}
                      {item.status === 'done' && (
                        <span style={{ color: '#52c41a' }}>完成</span>
                      )}
                      {item.status === 'error' && (
                        <span style={{ color: '#f5222d' }}>失败</span>
                      )}
                      <Button
                        type="text"
                        size="small"
                        disabled={uploading}
                        icon={<DeleteOutlined />}
                        onClick={() => removeItem(item.key)}
                      />
                    </Space>
                  </div>
                  <Input
                    size="small"
                    value={item.pathname}
                    disabled={uploading}
                    onChange={(e) =>
                      setItems((prev) =>
                        prev.map((it) =>
                          it.key === item.key
                            ? { ...it, pathname: e.target.value }
                            : it,
                        ),
                      )
                    }
                    suffix={
                      item.status === 'uploading' ? (
                        <Progress
                          type="circle"
                          size={14}
                          percent={item.percent}
                          format={() => ''}
                        />
                      ) : null
                    }
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}

/** 归一化目录路径 */
function normalizeDir(path?: string): string {
  if (!path) return '/';
  let p = path;
  if (!p.startsWith('/')) p = `/${p}`;
  while (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
  return p;
}
