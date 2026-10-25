import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from '@umijs/max';
import {
  Alert,
  Button,
  Dropdown,
  Input,
  Modal,
  Table,
  Tag,
  Tooltip,
  message,
  type MenuProps,
  type TableColumnsType,
  type TableProps,
} from 'antd';
import {
  DeleteOutlined,
  DownOutlined,
  ReloadOutlined,
  UndoOutlined,
} from '@ant-design/icons';
import {
  fetchBuckets,
  fetchResources,
  realDeleteResources,
  recoverResources,
} from '@/services/api';
import { usePerms } from '@/hooks/usePerms';
import { useResponsive } from '@/hooks/useResponsive';
import { FileIcon, FolderIcon } from '@/components/FileIcon';
import BucketNav from './components/BucketNav';
import type { BucketVo, DirAndResourceVo } from '@/services/types';

/** URL 查询参数别名（与文件中心一致，便于分享链接） */
const QUERY_ALIAS: Record<string, string> = {
  pageNum: 'n',
  pageSize: 's',
  keyWord: 'k',
  sortField: 'f',
  sortOrder: 'o',
  currentBucket: 'b',
};

/**
 * 服务端 `TrashCron` 每天凌晨 3 点清理 15 天前的回收站内容，
 * 这里同步提示给用户，免得以为回收站是永久保存的。
 */
const AUTO_PURGE_DAYS = 15;

/**
 * 行 key。
 *
 * 文件夹与文件的 id 是两套自增序列，会重复，必须带前缀区分。
 */
function rowKey(record: DirAndResourceVo) {
  return `${record.isDir ? 'dir-' : 'resource-'}${record.id}`;
}

/**
 * 组装批量操作的 id 参数。
 *
 * 空的数组不传（而不是传空串）：服务端 `realDel` 的参数是 `Long[]`，
 * 传空串会被 Spring 绑成空数组，虽然不会报错，但语义上不清晰。
 */
function buildIdsParams(dirs: number[], files: number[]) {
  const params: { dirIds?: string; fileIds?: string } = {};
  if (dirs.length) params.dirIds = dirs.join(',');
  if (files.length) params.fileIds = files.join(',');
  return params;
}

/** antd 没有直接导出 TableRowSelection，从 TableProps 里取 */
type TableRowSelection<T> = NonNullable<TableProps<T>['rowSelection']>;

/**
 * 回收站。
 *
 * 服务端用 `isDelete=true` 查询已删除的资源（不区分目录，跨所有路径），
 * `PUT /member/resource/{bucket}/recover` 还原，
 * `DELETE /member/resource/{bucket}/realDel` 彻底删除。
 */
export default function TrashPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { hasPerm } = usePerms();
  const { isMobile } = useResponsive();

  const [buckets, setBuckets] = useState<BucketVo[]>([]);
  const [currentBucket, setCurrentBucket] = useState<string | undefined>(
    searchParams.get(QUERY_ALIAS.currentBucket) || undefined,
  );
  const [rows, setRows] = useState<DirAndResourceVo[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [loading, setLoading] = useState(false);
  const [keyWord, setKeyWord] = useState(searchParams.get(QUERY_ALIAS.keyWord) || '');
  const [sortField, setSortField] = useState<string | null>(
    searchParams.get(QUERY_ALIAS.sortField),
  );
  const [sortOrder, setSortOrder] = useState<string | null>(
    searchParams.get(QUERY_ALIAS.sortOrder),
  );
  const [pagination, setPagination] = useState({
    current: parseInt(searchParams.get(QUERY_ALIAS.pageNum) || '1', 10) || 1,
    pageSize: parseInt(searchParams.get(QUERY_ALIAS.pageSize) || '20', 10) || 20,
    total: 0,
  });

  const canDelete = hasPerm(currentBucket, 'DELETE');

  /* ------------------------------------------------------------ 数据加载 */

  useEffect(() => {
    fetchBuckets()
      .then((list) => {
        const arr = list || [];
        setBuckets(arr);
        setCurrentBucket((prev) => {
          if (prev && arr.some((b) => b.bucketName === prev)) return prev;
          return arr[0]?.bucketName;
        });
      })
      .catch(() => {
        // 错误已由拦截器处理
      });
  }, []);

  /** 把当前状态同步到 URL（翻页 / 排序用 replace，不往历史记录里堆） */
  const syncUrl = useCallback(
    (params: {
      pageNum?: number;
      pageSize?: number;
      keyWord?: string;
      sortField?: string | null;
      sortOrder?: string | null;
      currentBucket?: string;
    }) => {
      const next = new URLSearchParams();
      Object.keys(QUERY_ALIAS).forEach((key) => {
        const value = (params as any)[key];
        if (value !== undefined && value !== null && value !== '') {
          next.set(QUERY_ALIAS[key], String(value));
        }
      });
      setSearchParams(next, { replace: true });
    },
    [setSearchParams],
  );

  const fetchList = useCallback(
    async (params: {
      pageNum?: number;
      pageSize?: number;
      keyWord?: string;
      sortField?: string | null;
      sortOrder?: string | null;
    }) => {
      if (!currentBucket) return;
      setLoading(true);
      try {
        const result = await fetchResources(currentBucket, {
          pageNum: params.pageNum ?? 1,
          pageSize: params.pageSize ?? 20,
          // path 传空串 = 递归搜所有位置（回收站里的内容分散在各目录）
          path: '',
          keyWord: params.keyWord ?? '',
          sortField: params.sortField ?? undefined,
          sortOrder: params.sortOrder ?? undefined,
          isDelete: true,
        });
        const resources = result?.resources;
        setRows(resources?.list || []);
        const pageSize = resources?.pageSize || params.pageSize || 20;
        setPagination({
          current: resources?.pageNum || 1,
          pageSize,
          total: resources?.total || 0,
        });
        syncUrl({
          pageNum: resources?.pageNum || 1,
          pageSize,
          keyWord: params.keyWord ?? '',
          sortField: params.sortField,
          sortOrder: params.sortOrder,
          currentBucket,
        });
      } catch {
        setRows([]);
      } finally {
        setLoading(false);
      }
    },
    [currentBucket, syncUrl],
  );

  /** 切换桶后重新加载（并清空勾选，避免跨桶误删） */
  useEffect(() => {
    if (!currentBucket) return;
    setSelectedRowKeys([]);
    fetchList({
      pageNum: 1,
      pageSize: pagination.pageSize,
      keyWord,
      sortField,
      sortOrder,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentBucket]);

  const reload = useCallback(() => {
    fetchList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyWord,
      sortField,
      sortOrder,
    });
  }, [fetchList, pagination.current, pagination.pageSize, keyWord, sortField, sortOrder]);

  /** 切换桶（桶内导航条的下拉） */
  const onSwitchBucket = useCallback(
    (bucketName: string) => {
      setCurrentBucket(bucketName);
      setKeyWord('');
      setSortField(null);
      setSortOrder(null);
      syncUrl({
        pageNum: 1,
        pageSize: pagination.pageSize,
        keyWord: '',
        sortField: null,
        sortOrder: null,
        currentBucket: bucketName,
      });
    },
    [syncUrl, pagination.pageSize],
  );

  const onSearch = useCallback(() => {
    setSelectedRowKeys([]);
    fetchList({
      pageNum: 1,
      pageSize: pagination.pageSize,
      keyWord,
      sortField,
      sortOrder,
    });
  }, [fetchList, pagination.pageSize, keyWord, sortField, sortOrder]);

  /* ------------------------------------------------------------ 交互 */

  const getSelectedRecords = useCallback(() => {
    return selectedRowKeys
      .map((key) => rows.find((r) => rowKey(r) === key))
      .filter(Boolean) as DirAndResourceVo[];
  }, [selectedRowKeys, rows]);

  /** 单个彻底删除 */
  const onRealDelete = useCallback(
    (record: DirAndResourceVo) => {
      Modal.confirm({
        title: record.isDir ? '确定彻底删除这个文件夹吗?' : '确定彻底删除这个文件吗?',
        content: (
          <div>
            <div style={{ wordBreak: 'break-all' }}>{record.path}</div>
            <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
              彻底删除后<strong>不可恢复</strong>
              {record.isDir && '，文件夹内的所有内容也会一并删除'}
            </div>
          </div>
        ),
        okText: '彻底删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          await realDeleteResources(
            currentBucket!,
            record.isDir ? { dirIds: record.id } : { fileIds: record.id },
          );
          message.success('删除成功');
          reload();
        },
      });
    },
    [currentBucket, reload],
  );

  /** 单个还原 */
  const onRecoverOne = useCallback(
    (record: DirAndResourceVo) => {
      Modal.confirm({
        title: record.isDir ? '确定还原这个文件夹吗?' : '确定还原这个文件吗?',
        content: (
          <div>
            <div style={{ wordBreak: 'break-all' }}>{record.path}</div>
            <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
              还原后会回到原来的位置
            </div>
          </div>
        ),
        okText: '还原',
        cancelText: '取消',
        onOk: async () => {
          await recoverResources(currentBucket!, {
            dirIds: record.isDir ? [record.id] : [],
            fileIds: record.isDir ? [] : [record.id],
          });
          message.success('还原成功');
          reload();
        },
      });
    },
    [currentBucket, reload],
  );

  /** 批量彻底删除 */
  const onBatchRealDelete = useCallback(() => {
    const selected = getSelectedRecords();
    if (selected.length === 0) {
      message.warning('请先选择要彻底删除的资源');
      return;
    }
    const names = selected.map((r) => r.path);
    const dirIds = selected.filter((r) => r.isDir).map((r) => r.id);
    const fileIds = selected.filter((r) => !r.isDir).map((r) => r.id);

    Modal.confirm({
      title: '确定彻底删除这些资源吗?',
      content: (
        <div>
          <div style={{ wordBreak: 'break-all' }}>{names.join('、')}</div>
          <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
            彻底删除后<strong>不可恢复</strong>
          </div>
        </div>
      ),
      okText: '彻底删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await realDeleteResources(currentBucket!, buildIdsParams(dirIds, fileIds));
        message.success('删除成功');
        setSelectedRowKeys([]);
        reload();
      },
    });
  }, [getSelectedRecords, currentBucket, reload]);

  /** 批量还原 */
  const onBatchRecover = useCallback(() => {
    const selected = getSelectedRecords();
    if (selected.length === 0) {
      message.warning('请先选择要还原的资源');
      return;
    }
    const names = selected.map((r) => r.path);
    const dirIds = selected.filter((r) => r.isDir).map((r) => r.id);
    const fileIds = selected.filter((r) => !r.isDir).map((r) => r.id);

    Modal.confirm({
      title: '确定还原这些资源吗?',
      content: (
        <div>
          <div style={{ wordBreak: 'break-all' }}>{names.join('、')}</div>
          <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
            还原后会回到原来的位置
          </div>
        </div>
      ),
      okText: '还原',
      cancelText: '取消',
      onOk: async () => {
        await recoverResources(currentBucket!, { dirIds, fileIds });
        message.success('还原成功');
        setSelectedRowKeys([]);
        reload();
      },
    });
  }, [getSelectedRecords, currentBucket, reload]);

  /** 表格变化（翻页 / 改每页条数 / 排序），统一在这里重新请求 */
  const onTableChange = useCallback(
    (nextPagination: any, _filters: any, sorter: any) => {
      const s = Array.isArray(sorter) ? sorter[0] : sorter;
      // antd 的 order 是 ascend / descend，服务端认这两个值
      const nextSortField = s?.order ? (s.field as string) : null;
      const nextSortOrder = s?.order ? (s.order as string) : null;
      setSortField(nextSortField);
      setSortOrder(nextSortOrder);
      setSelectedRowKeys([]);
      fetchList({
        pageNum: nextPagination?.current ?? 1,
        pageSize: nextPagination?.pageSize ?? pagination.pageSize,
        keyWord,
        sortField: nextSortField,
        sortOrder: nextSortOrder,
      });
    },
    [fetchList, keyWord, pagination.pageSize],
  );

  /* ------------------------------------------------------------ 表格列 */

  /** 当前生效的排序方向（受控，保证表头的排序箭头与状态一致） */
  const sortOrderOf = useCallback(
    (field: string) => (sortField === field ? ((sortOrder as any) ?? null) : null),
    [sortField, sortOrder],
  );

  const buildRowActions = useCallback((): MenuProps['items'] => [
    { key: 'recover', icon: <UndoOutlined />, label: '还原' },
    { type: 'divider' },
    { key: 'delete', icon: <DeleteOutlined />, label: '彻底删除', danger: true },
  ], []);

  const onRowAction = useCallback(
    (key: string, record: DirAndResourceVo) => {
      if (key === 'recover') onRecoverOne(record);
      else if (key === 'delete') onRealDelete(record);
    },
    [onRecoverOne, onRealDelete],
  );

  const columns: TableColumnsType<DirAndResourceVo> = useMemo(
    () => [
      {
        title: '文件名',
        dataIndex: 'name',
        width: 320,
        sorter: true,
        sortOrder: sortOrderOf('name'),
        render: (_: any, record: DirAndResourceVo) => (
          <div className="mos-resource-name">
            {record.isDir ? (
              <FolderIcon />
            ) : (
              // 回收站里不显示缩略图：服务端 `/mos/**` 只查未删除的资源
              // （findResourceByPathnameAndBucketId(pathname, bucketId, false)），
              // 已删除的请求会 404，渲染出来就是一张破图。
              <FileIcon icon={record.icon} fileName={record.fileName} image={record.image} />
            )}
            <Tooltip title={record.path}>
              <span className="mos-resource-text">{record.path}</span>
            </Tooltip>
          </div>
        ),
      },
      {
        title: '文件大小',
        dataIndex: 'readableSize',
        width: 110,
        sorter: true,
        sortOrder: sortOrderOf('readableSize'),
        responsive: ['md', 'lg', 'xl', 'xxl'],
        render: (v: any, r: DirAndResourceVo) => (r.isDir ? '' : v),
      },
      {
        title: '权限',
        dataIndex: 'isPublic',
        width: 90,
        sorter: true,
        sortOrder: sortOrderOf('isPublic'),
        responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
        render: (v: any, r: DirAndResourceVo) =>
          r.isDir ? '' : v ? <Tag color="orange">公开</Tag> : <Tag color="green">私有</Tag>,
      },
      {
        title: '响应头',
        dataIndex: 'contentType',
        width: 190,
        ellipsis: true,
        responsive: ['lg', 'xl', 'xxl'],
      },
      {
        title: '访问量',
        dataIndex: 'visits',
        width: 90,
        sorter: true,
        sortOrder: sortOrderOf('visits'),
        responsive: ['lg', 'xl', 'xxl'],
        render: (v: any, r: DirAndResourceVo) => (r.isDir ? '' : v ?? 0),
      },
      {
        title: '修改时间',
        dataIndex: 'updatedDate',
        width: 165,
        sorter: true,
        sortOrder: sortOrderOf('updatedDate'),
        responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
      },
      {
        title: '修改人',
        dataIndex: 'updatedBy',
        width: 120,
        sorter: true,
        sortOrder: sortOrderOf('updatedBy'),
        responsive: ['xl', 'xxl'],
      },
      {
        title: '操作',
        width: isMobile ? 76 : 170,
        fixed: 'right',
        render: (_: any, record: DirAndResourceVo) =>
          isMobile ? (
            // 窄屏：操作收进「更多」下拉，避免操作列占满整屏
            <Dropdown
              menu={{
                items: buildRowActions(),
                onClick: ({ key, domEvent }) => {
                  domEvent.stopPropagation();
                  onRowAction(key, record);
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
              {canDelete && (
                <a onClick={() => onRecoverOne(record)}>
                  <UndoOutlined /> 还原
                </a>
              )}
              {canDelete && (
                <a className="mos-danger-link" onClick={() => onRealDelete(record)}>
                  <DeleteOutlined /> 彻底删除
                </a>
              )}
            </div>
          ),
      },
    ],
    [
      canDelete,
      isMobile,
      sortOrderOf,
      buildRowActions,
      onRowAction,
      onRecoverOne,
      onRealDelete,
    ],
  );

  /* ------------------------------------------------------------ 行选择 */

  const rowSelection: TableRowSelection<DirAndResourceVo> = useMemo(() => {
    const isDirKey = (key: React.Key) => {
      const record = rows.find((r) => rowKey(r) === key);
      return record ? record.isDir : false;
    };
    return {
      selectedRowKeys,
      onChange: setSelectedRowKeys,
      // 快捷选择：只看文件夹 / 只看文件
      selections: [
        {
          key: 'dirs',
          text: '文件夹',
          onSelect: (keys: React.Key[]) => setSelectedRowKeys(keys.filter(isDirKey)),
        },
        {
          key: 'files',
          text: '文件',
          onSelect: (keys: React.Key[]) =>
            setSelectedRowKeys(keys.filter((k) => !isDirKey(k))),
        },
      ],
    };
  }, [rows, selectedRowKeys]);

  /* ------------------------------------------------------------ 渲染 */

  return (
    <div>
      <BucketNav
        buckets={buckets}
        current={currentBucket}
        onSwitchBucket={onSwitchBucket}
      />

      <div className="mos-card" style={{ padding: 16 }}>
        <div className="mos-toolbar">
          <Button
            type="primary"
            danger
            icon={<DeleteOutlined />}
            disabled={!canDelete || selectedRowKeys.length === 0}
            onClick={onBatchRealDelete}
          >
            彻底删除
          </Button>
          <Button
            type="primary"
            icon={<UndoOutlined />}
            disabled={!canDelete || selectedRowKeys.length === 0}
            onClick={onBatchRecover}
          >
            还原
          </Button>

          <div className="mos-toolbar-search">
            <Input.Search
              allowClear
              placeholder="关键字搜索"
              value={keyWord}
              onChange={(e) => setKeyWord(e.target.value)}
              onSearch={onSearch}
              enterButton
            />
          </div>

          <Tooltip title="刷新">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
        </div>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={`回收站中的内容超过 ${AUTO_PURGE_DAYS} 天会被自动彻底删除，请及时还原需要的资源`}
      />

      <Table<DirAndResourceVo>
        className="mos-table"
        rowKey={rowKey}
        columns={columns}
        dataSource={rows}
        loading={loading}
        // 窄屏只保留「文件名 + 操作」两列，横向滚动宽度可以收窄
        scroll={{ x: isMobile ? 360 : 1300 }}
        size={isMobile ? 'small' : 'middle'}
        rowSelection={rowSelection}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          // 窄屏隐藏「每页条数」与「总数」，给页码留出空间
          showSizeChanger: !isMobile,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: isMobile ? undefined : (total) => `数据总数：${total}`,
          size: isMobile ? 'small' : 'default',
        }}
        onChange={onTableChange}
        locale={{
          emptyText: '回收站是空的',
        }}
      />
      </div>
    </div>
  );
}
