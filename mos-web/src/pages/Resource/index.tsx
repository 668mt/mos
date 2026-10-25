import {
  Button,
  Dropdown,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
  type MenuProps,
  type TableColumnsType,
} from 'antd';
import {
  CloudUploadOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  FolderAddOutlined,
  LinkOutlined,
  PictureOutlined,
  ReloadOutlined,
  RollbackOutlined,
} from '@ant-design/icons';
import { history, useSearchParams } from '@umijs/max';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Viewer from 'viewerjs';
import 'viewerjs/dist/viewer.css';
import {
  deleteResources,
  fetchBuckets,
  fetchFileSuffix,
  fetchResources,
} from '@/services/api';
import { usePerms } from '@/hooks/usePerms';
import { useResponsive } from '@/hooks/useResponsive';
import { basename, normalizePath, parentOf, pathChain } from '@/utils';
import ListName from './components/ListName';
import UploadModal from './components/UploadModal';
import DirModal from './components/DirModal';
import ResourceModal from './components/ResourceModal';
import AddrModal from './components/AddrModal';
import CopyModal from './components/CopyModal';
import type {
  BucketVo,
  Dir,
  DirAndResourceVo,
  FileSuffixMap,
} from '@/services/types';

/** URL 查询参数别名（与旧版保持一致，便于分享链接） */
const QUERY_ALIAS: Record<string, string> = {
  pageNum: 'n',
  pageSize: 's',
  path: 'p',
  keyWord: 'k',
  sortField: 'f',
  sortOrder: 'o',
  currentBucket: 'b',
};

export default function ResourcePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { hasPerm } = usePerms();
  const { isMobile } = useResponsive();

  const [buckets, setBuckets] = useState<BucketVo[]>([]);
  const [currentBucket, setCurrentBucket] = useState<string | undefined>(
    searchParams.get('b') || undefined,
  );
  const [currentDir, setCurrentDir] = useState<Dir>({
    id: undefined as any,
    path: '/',
    urlEncodePath: '/',
  } as Dir);
  const [parentDirs, setParentDirs] = useState<Dir[]>([]);
  const [rows, setRows] = useState<DirAndResourceVo[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [loading, setLoading] = useState(false);
  const [keyWord, setKeyWord] = useState(searchParams.get('k') || '');
  const [sortField, setSortField] = useState<string | null>(
    searchParams.get('f'),
  );
  const [sortOrder, setSortOrder] = useState<string | null>(
    searchParams.get('o'),
  );
  const [fileSuffix, setFileSuffix] = useState<FileSuffixMap>({});
  const [historyClicks, setHistoryClicks] = useState<string[]>([]);
  const [showDetailPath, setShowDetailPath] = useState(false);
  const [pagination, setPagination] = useState({
    current: parseInt(searchParams.get('n') || '1', 10) || 1,
    pageSize: parseInt(searchParams.get('s') || '20', 10) || 20,
    total: 0,
  });

  const [uploadOpen, setUploadOpen] = useState(false);
  const [dirModalOpen, setDirModalOpen] = useState(false);
  const [dirRecord, setDirRecord] = useState<DirAndResourceVo | null>(null);
  const [resourceModalOpen, setResourceModalOpen] = useState(false);
  const [resourceRecord, setResourceRecord] = useState<DirAndResourceVo | null>(null);
  const [addrOpen, setAddrOpen] = useState(false);
  const [addrRecord, setAddrRecord] = useState<DirAndResourceVo | null>(null);
  const [copyOpen, setCopyOpen] = useState(false);
  const [copyType, setCopyType] = useState<'copy' | 'move'>('copy');

  const viewerRef = useRef<Viewer | null>(null);
  const viewerImagesRef = useRef<{ origin: string; thumb: string }[]>([]);

  const canInsert = hasPerm(currentBucket, 'INSERT');
  const canSelect = hasPerm(currentBucket, 'SELECT');
  const canUpdate = hasPerm(currentBucket, 'UPDATE');
  const canDelete = hasPerm(currentBucket, 'DELETE');

  /* ------------------------------------------------------------ 数据加载 */

  const loadBuckets = useCallback(async () => {
    try {
      const list = await fetchBuckets();
      setBuckets(list || []);
      setCurrentBucket((prev) => {
        if (prev && list.some((b) => b.bucketName === prev)) return prev;
        return list[0]?.bucketName;
      });
    } catch {
      // 错误已由拦截器处理
    }
  }, []);

  useEffect(() => {
    loadBuckets();
    fetchFileSuffix()
      .then((map) => setFileSuffix(map || {}))
      .catch(() => setFileSuffix({}));
  }, [loadBuckets]);

  /**
   * 把当前状态同步到 URL。
   *
   * `push` 语义：
   * - 切换目录 / 切换桶 -> push，让浏览器「后退」能回到上一个目录
   * - 翻页 / 排序 / 改每页条数 -> replace，避免历史记录被这些操作刷屏
   */
  const syncUrl = useCallback(
    (
      params: {
        pageNum?: number;
        pageSize?: number;
        path?: string;
        keyWord?: string;
        sortField?: string | null;
        sortOrder?: string | null;
        currentBucket?: string;
      },
      options: { push?: boolean } = {},
    ) => {
      const next = new URLSearchParams();
      const merged = { ...params };
      Object.keys(QUERY_ALIAS).forEach((key) => {
        const value = (merged as any)[key];
        if (value !== undefined && value !== null && value !== '') {
          next.set(QUERY_ALIAS[key], String(value));
        }
      });
      setSearchParams(next, { replace: !options.push });
    },
    [setSearchParams],
  );

  const fetchList = useCallback(
    async (
      params: {
        pageNum?: number;
        pageSize?: number;
        path?: string;
        keyWord?: string;
        sortField?: string | null;
        sortOrder?: string | null;
      },
      options: { syncUrl?: boolean; push?: boolean } = { syncUrl: true },
    ) => {
      if (!currentBucket) return;
      setLoading(true);
      try {
        const result = await fetchResources(currentBucket, {
          pageNum: params.pageNum ?? 1,
          pageSize: params.pageSize ?? 20,
          path: params.path ?? '/',
          keyWord: params.keyWord ?? '',
          sortField: params.sortField ?? undefined,
          sortOrder: params.sortOrder ?? undefined,
        });
        const resources = result?.resources;
        setCurrentDir(result?.currentDir || ({ path: '/', urlEncodePath: '/' } as Dir));
        setParentDirs(result?.parentDirs || []);
        setRows(resources?.list || []);
        setPagination({
          current: resources?.pageNum || 1,
          pageSize: resources?.pageSize || params.pageSize || 20,
          total: resources?.total || 0,
        });
        setShowDetailPath((params.path ?? '/') === '');
        if (options.syncUrl !== false) {
          syncUrl(
            {
              pageNum: resources?.pageNum || 1,
              pageSize: resources?.pageSize || params.pageSize,
              path: params.path ?? '/',
              keyWord: params.keyWord ?? '',
              sortField: params.sortField,
              sortOrder: params.sortOrder,
              currentBucket,
            },
            { push: options.push },
          );
        }
      } catch {
        setRows([]);
      } finally {
        setLoading(false);
      }
    },
    [currentBucket, syncUrl],
  );

  /**
   * 桶变化时重新加载。
   *
   * 首次挂载时用 URL 里的路径（支持分享链接直达子目录）；
   * 之后切换桶则回到根目录（由桶下拉框的 onChange 写入 URL 的 p=/）；
   * 但如果是浏览器前进/后退跨桶切换，则用 pendingPathRef 里记下的路径。
   */
  const firstLoadRef = useRef(true);
  const pendingPathRef = useRef<string | null>(null);
  useEffect(() => {
    if (!currentBucket) return;
    setSelectedRowKeys([]);
    const path =
      pendingPathRef.current ??
      (firstLoadRef.current ? searchParams.get(QUERY_ALIAS.path) || '/' : '/');
    pendingPathRef.current = null;
    firstLoadRef.current = false;
    fetchList({
      pageNum: 1,
      pageSize: pagination.pageSize,
      path,
      keyWord,
      sortField,
      sortOrder,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentBucket]);

  /* ------------------------------------------------------------ 交互 */

  const openDir = useCallback(
    (record: DirAndResourceVo) => {
      setHistoryClicks((prev) => [...prev, record.path]);
      setKeyWord('');
      fetchList(
        {
          pageNum: 1,
          pageSize: pagination.pageSize,
          path: record.path,
          keyWord: '',
          sortField,
          sortOrder,
        },
        // 切换目录用 push，浏览器「后退」可回到上一个目录
        { push: true },
      );
      setSelectedRowKeys([]);
    },
    [fetchList, pagination.pageSize, sortField, sortOrder],
  );

  const onSearch = useCallback(() => {
    const path = currentDir.path === '' ? '/' : currentDir.path;
    fetchList({
      pageNum: 1,
      pageSize: pagination.pageSize,
      path,
      keyWord,
      sortField,
      sortOrder,
    });
  }, [currentDir.path, pagination.pageSize, keyWord, sortField, sortOrder, fetchList]);

  const reload = useCallback(() => {
    fetchList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      path: currentDir.path,
      keyWord,
      sortField,
      sortOrder,
    });
  }, [fetchList, pagination.current, pagination.pageSize, currentDir.path, keyWord, sortField, sortOrder]);

  /**
   * 浏览器「后退 / 前进」时按 URL 重新加载列表。
   *
   * 目录切换用的是 push，所以后退会回到上一个目录。
   * 这里监听 popstate，把 URL 里的 p / k / b / f / o 同步回状态并重新请求。
   */
  useEffect(() => {
    const onPopState = () => {
      const params = new URLSearchParams(window.location.search);
      const nextPath = params.get(QUERY_ALIAS.path) || '/';
      const nextBucket = params.get(QUERY_ALIAS.currentBucket) || currentBucket;
      const nextKeyWord = params.get(QUERY_ALIAS.keyWord) || '';
      const nextSortField = params.get(QUERY_ALIAS.sortField);
      const nextSortOrder = params.get(QUERY_ALIAS.sortOrder);
      const nextPageNum = parseInt(params.get(QUERY_ALIAS.pageNum) || '1', 10) || 1;
      const nextPageSize = parseInt(params.get(QUERY_ALIAS.pageSize) || '20', 10) || 20;

      setHistoryClicks((prev) =>
        prev.indexOf(nextPath) === -1 ? [...prev, nextPath] : prev,
      );

      // 桶发生变化时，交给「桶变化」effect 重新加载，
      // 这里把目标路径记下来，避免被重置成根目录
      if (nextBucket !== currentBucket) {
        pendingPathRef.current = nextPath;
        setCurrentBucket(nextBucket);
        return;
      }

      setKeyWord(nextKeyWord);
      setSortField(nextSortField);
      setSortOrder(nextSortOrder);
      setSelectedRowKeys([]);
      fetchList(
        {
          pageNum: nextPageNum,
          pageSize: nextPageSize,
          path: nextPath,
          keyWord: nextKeyWord,
          sortField: nextSortField,
          sortOrder: nextSortOrder,
        },
        // 后退时不要再次写 URL，否则会打断浏览器的历史遍历
        { syncUrl: false },
      );
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, [currentBucket, fetchList]);

  const onDelete = useCallback(
    (record: DirAndResourceVo) => {
      const isDir = record.isDir;
      // antd5 Modal.confirm 是静态方法，onOk 的 rejection 已由 utils/antdSafe.ts 全局兜底
      Modal.confirm({
        title: isDir ? '确定删除这个文件夹吗?' : '确定删除这个文件吗?',
        content: (
          <div>
            <div style={{ wordBreak: 'break-all' }}>{record.path}</div>
            {isDir && (
              <div style={{ marginTop: 8, color: 'rgba(0,0,0,.45)' }}>
                文件夹内的所有内容会一并移入回收站
              </div>
            )}
          </div>
        ),
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
          const params = isDir ? { dirIds: record.id } : { fileIds: record.id };
          await deleteResources(currentBucket!, params);
          message.success('删除成功');
          reload();
        },
      });
    },
    [currentBucket, reload],
  );

  const getSelectedRecords = useCallback(() => {
    return selectedRowKeys
      .map((key) => rows.find((r) => `${r.isDir ? 'dir-' : 'resource-'}${r.id}` === key))
      .filter(Boolean) as DirAndResourceVo[];
  }, [selectedRowKeys, rows]);

  const onBatchDelete = useCallback(() => {
    const selected = getSelectedRecords();
    if (selected.length === 0) return;
    const names = selected.map((r) => r.path);
    const dirIds = selected.filter((r) => r.isDir).map((r) => r.id);
    const fileIds = selected.filter((r) => !r.isDir).map((r) => r.id);

    // antd5 Modal.confirm 是静态方法，需要用 App.useApp 获取 context 版本
    Modal.confirm({
      title: '确定删除这些资源吗?',
      content: names.join('、'),
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteResources(currentBucket!, {
          fileIds: fileIds.join(','),
          dirIds: dirIds.join(','),
        });
        message.success('删除成功');
        setSelectedRowKeys([]);
        reload();
      },
    });
  }, [getSelectedRecords, currentBucket, reload]);

  const openCopy = useCallback(
    (type: 'copy' | 'move') => {
      setCopyType(type);
      setCopyOpen(true);
    },
    [],
  );

  const actionMenu: MenuProps['items'] = [
    {
      key: 'copy',
      label: '复制',
      disabled: !canSelect || selectedRowKeys.length === 0,
    },
    {
      key: 'move',
      label: '移动',
      disabled: !canDelete || selectedRowKeys.length === 0,
    },
    {
      key: 'delete',
      label: '删除',
      disabled: !canDelete || selectedRowKeys.length === 0,
      danger: true,
    },
  ];

  const onActionClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'delete') onBatchDelete();
    else if (key === 'copy') openCopy('copy');
    else if (key === 'move') openCopy('move');
  };

  /* ------------------------------------------------------------ 图片预览 */

  const previewImages = useMemo(
    () =>
      rows
        .filter((r) => r.image)
        .map((r) => {
          const url = `/mos/${currentBucket}${r.urlEncodePath}`;
          return {
            origin: r.signUrl || url,
            thumb: r.thumbFileHouseId ? `${url}?thumb=true` : url,
          };
        }),
    [rows, currentBucket],
  );

  useEffect(() => {
    viewerImagesRef.current = previewImages;
    if (viewerRef.current) {
      viewerRef.current.update();
    }
  }, [previewImages]);

  const openImageViewer = useCallback((url: string) => {
    const images = viewerImagesRef.current;
    const index = images.findIndex((img) => img.origin.indexOf(url) !== -1);
    if (viewerRef.current && index >= 0) {
      viewerRef.current.view(index);
    }
  }, []);

  /** 窄屏「更多」菜单项 */
  const buildRowActions = useCallback(
    (record: DirAndResourceVo): MenuProps['items'] => {
      const items: MenuProps['items'] = [];
      if (canUpdate) {
        items.push({
          key: 'edit',
          icon: <EditOutlined />,
          label: '编辑',
        });
      }
      items.push({
        key: 'addr',
        icon: <LinkOutlined />,
        label: '访问链',
      });
      if (!record.isDir) {
        items.push({
          key: 'download',
          label: '下载',
        });
      } else {
        items.push({
          key: 'gallery',
          icon: <PictureOutlined />,
          label: '图集',
        });
      }
      if (canDelete) {
        items.push({ type: 'divider' });
        items.push({
          key: 'delete',
          icon: <DeleteOutlined />,
          label: '删除',
          danger: true,
        });
      }
      return items;
    },
    [canUpdate, canDelete],
  );

  /** 窄屏「更多」菜单点击处理 */
  const onRowAction = useCallback(
    (key: string, record: DirAndResourceVo) => {
      switch (key) {
        case 'edit':
          if (record.isDir) {
            setDirRecord(record);
            setDirModalOpen(true);
          } else {
            setResourceRecord(record);
            setResourceModalOpen(true);
          }
          break;
        case 'addr':
          setAddrRecord(record);
          setAddrOpen(true);
          break;
        case 'download':
          window.open(
            `/mos/${currentBucket}${record.urlEncodePath}?download=true`,
            '_blank',
          );
          break;
        case 'gallery':
          window.open(
            `/viewer/gallery?bucket=${currentBucket}&path=${record.urlEncodePath}`,
            '_blank',
          );
          break;
        case 'delete':
          onDelete(record);
          break;
        default:
          break;
      }
    },
    [currentBucket, onDelete],
  );

  /* ------------------------------------------------------------ 表格列 */

  const columns: TableColumnsType<DirAndResourceVo> = useMemo(() => {
    const cols: any[] = [
      {
        title: '文件名',
        dataIndex: 'name',
        width: 300,
        sorter: true,
        render: (_: any, record: DirAndResourceVo) => (
          <ListName
            record={record}
            bucketName={currentBucket}
            showDetailPath={showDetailPath}
            fileSuffix={fileSuffix}
            historyClicks={historyClicks}
            maxThumbs={isMobile ? 3 : 6}
            onOpenDir={openDir}
            onPreviewImage={openImageViewer}
            onOpenFile={(r) =>
              history.push(
                `/viewer/video?bucket=${currentBucket}&id=${r.id}&path=${encodeURIComponent(
                  normalizePath(currentDir.path),
                )}`,
              )
            }
          />
        ),
      },
      {
        title: '文件大小',
        dataIndex: 'readableSize',
        width: 110,
        sorter: true,
        // 窄屏隐藏次要列，避免表格横向滚动过多
        responsive: ['md', 'lg', 'xl', 'xxl'],
        render: (v: any, r: DirAndResourceVo) => (r.isDir ? '' : v),
      },
      {
        title: '权限',
        dataIndex: 'isPublic',
        width: 90,
        sorter: true,
        responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
        render: (v: any, r: DirAndResourceVo) =>
          r.isDir ? '' : v ? <Tag color="orange">公开</Tag> : <Tag color="green">私有</Tag>,
      },
      {
        title: '访问量',
        dataIndex: 'visits',
        width: 90,
        sorter: true,
        responsive: ['lg', 'xl', 'xxl'],
        render: (v: any, r: DirAndResourceVo) => (r.isDir ? '' : v ?? 0),
      },
      {
        title: '修改时间',
        dataIndex: 'updatedDate',
        width: 165,
        sorter: true,
        responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
      },
      {
        title: '修改人',
        dataIndex: 'updatedBy',
        width: 120,
        sorter: true,
        responsive: ['xl', 'xxl'],
      },
      {
        title: '操作',
        width: isMobile ? 76 : 260,
        fixed: 'right',
        render: (_: any, record: DirAndResourceVo) =>
          isMobile ? (
            // 窄屏：把操作收进「更多」下拉，避免操作列占满整屏
            <Dropdown
              menu={{
                items: buildRowActions(record),
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
                <Tooltip title="删除">
                  <a className="mos-danger-link" onClick={() => onDelete(record)}>
                    <DeleteOutlined /> 删除
                  </a>
                </Tooltip>
              )}
              {canUpdate && (
                <a
                  onClick={() => {
                    if (record.isDir) {
                      setDirRecord(record);
                      setDirModalOpen(true);
                    } else {
                      setResourceRecord(record);
                      setResourceModalOpen(true);
                    }
                  }}
                >
                  <EditOutlined /> 编辑
                </a>
              )}
              <a onClick={() => { setAddrRecord(record); setAddrOpen(true); }}>
                <LinkOutlined /> 访问链
              </a>
              {!record.isDir ? (
                <a
                  href={`/mos/${currentBucket}${record.urlEncodePath}?download=true`}
                  target="_blank"
                  rel="noreferrer"
                >
                  下载
                </a>
              ) : (
                <a
                  href={`/viewer/gallery?bucket=${currentBucket}&path=${record.urlEncodePath}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  <PictureOutlined /> 图集
                </a>
              )}
            </div>
          ),
      },
    ];
    return cols;
  }, [
    currentBucket,
    showDetailPath,
    fileSuffix,
    historyClicks,
    openDir,
    openImageViewer,
    currentDir.path,
    canDelete,
    canUpdate,
    onDelete,
    isMobile,
    buildRowActions,
    onRowAction,
  ]);

  /* ------------------------------------------------------------ 渲染 */

  const crumbs = useMemo(() => {
    const chain = pathChain(currentDir.path);
    return chain.map((p) => ({ path: p, name: basename(p) }));
  }, [currentDir.path]);

  return (
    <div className="mos-card" style={{ padding: 16 }}>
      <div className="mos-toolbar">
        <Button
          type="primary"
          icon={<CloudUploadOutlined />}
          disabled={!canInsert}
          onClick={() => setUploadOpen(true)}
        >
          上传
        </Button>
        <Button
          type="primary"
          icon={<FolderAddOutlined />}
          disabled={!canInsert}
          onClick={() => {
            setDirRecord(null);
            setDirModalOpen(true);
          }}
        >
          创建文件夹
        </Button>
        <Dropdown menu={{ items: actionMenu, onClick: onActionClick }} trigger={['click']}>
          <Button>
            操作 <DownOutlined />
          </Button>
        </Dropdown>

        <div className="mos-toolbar-search">
          <Input.Search
            allowClear
            placeholder="搜索当前位置"
            value={keyWord}
            onChange={(e) => setKeyWord(e.target.value)}
            onSearch={onSearch}
            onPressEnter={onSearch}
            enterButton
          />
        </div>

        <Select
          className="mos-toolbar-bucket"
          value={currentBucket}
          onChange={(v) => {
            setCurrentBucket(v);
            // 切换桶同样用 push，后退可以回到上一个桶
            syncUrl(
              {
                currentBucket: v,
                path: '/',
                pageNum: 1,
                pageSize: pagination.pageSize,
                keyWord: '',
                sortField,
                sortOrder,
              },
              { push: true },
            );
          }}
          options={buckets.map((b) => ({ value: b.bucketName, label: b.bucketName }))}
        />

        {/* 回收站入口：带上当前桶，回收站直接定位到同一个桶 */}
        <Button
          icon={<DeleteOutlined />}
          onClick={() =>
            history.push(
              currentBucket
                ? `/trash?b=${encodeURIComponent(currentBucket)}`
                : '/trash',
            )
          }
        >
          回收站
        </Button>
      </div>

      <div className="mos-breadcrumb">
        <Tooltip title="返回上一级目录">
          <a
            className="mos-crumb"
            onClick={() =>
              openDir({
                path: parentOf(currentDir.path),
                isDir: true,
              } as DirAndResourceVo)
            }
          >
            <RollbackOutlined />
          </a>
        </Tooltip>
        <a className="mos-crumb" onClick={reload}>
          <ReloadOutlined />
        </a>
        <span
          className="mos-crumb"
          onClick={() => openDir({ path: '/', isDir: true } as DirAndResourceVo)}
        >
          根目录
        </span>
        {crumbs.map((c) => (
          <span key={c.path}>
            <span className="mos-crumb" style={{ opacity: 0.4 }}>/</span>
            <span
              className={`mos-crumb${c.path === currentDir.path ? ' current' : ''}`}
              onClick={() =>
                openDir({ path: c.path, isDir: true } as DirAndResourceVo)
              }
            >
              {c.name}
            </span>
          </span>
        ))}
      </div>

      <Table<DirAndResourceVo>
        className="mos-table"
        rowKey={(record) => `${record.isDir ? 'dir-' : 'resource-'}${record.id}`}
        columns={columns}
        dataSource={rows}
        loading={loading}
        // 窄屏只保留「文件名 + 操作」两列，横向滚动宽度可以收窄
        scroll={{ x: isMobile ? 360 : 1100 }}
        size={isMobile ? 'small' : 'middle'}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          // 窄屏隐藏「每页条数」与「总数」，给页码留出空间
          showSizeChanger: !isMobile,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: isMobile ? undefined : (total) => `数据总数：${total}`,
          size: isMobile ? 'small' : 'default',
          onChange: (page, pageSize) => {
            fetchList({
              pageNum: page,
              pageSize,
              path: currentDir.path,
              keyWord,
              sortField,
              sortOrder,
            });
          },
          onShowSizeChange: (_current, size) => {
            fetchList({
              pageNum: 1,
              pageSize: size,
              path: currentDir.path,
              keyWord,
              sortField,
              sortOrder,
            });
          },
        }}
        onChange={(_pagination, _filters, sorter) => {
          const s = Array.isArray(sorter) ? sorter[0] : sorter;
          if (s?.order) {
            setSortField(s.field as string);
            setSortOrder(s.order);
          } else {
            setSortField(null);
            setSortOrder(null);
          }
          setSelectedRowKeys([]);
        }}
      />

      {/* 图片查看器 */}
      <div
        id="mos-viewer"
        style={{ display: 'none' }}
        ref={(el) => {
          if (el && !viewerRef.current) {
            viewerRef.current = new Viewer(el, {
              zIndex: 9999,
              toolbar: true,
              tooltip: true,
              movable: true,
              zoomable: true,
              rotatable: false,
              scalable: false,
              transition: true,
              fullscreen: false,
              keyboard: true,
              url: 'origin',
            });
          }
        }}
      >
        {previewImages.map((img) => (
          <img key={img.origin} src={img.thumb} data-origin={img.origin} alt="" />
        ))}
      </div>

      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        buckets={buckets}
        currentBucket={currentBucket}
        currentDir={currentDir}
        onOk={reload}
      />
      <DirModal
        open={dirModalOpen}
        onClose={() => setDirModalOpen(false)}
        bucketName={currentBucket}
        currentDir={currentDir}
        record={dirRecord}
        onOk={reload}
      />
      <ResourceModal
        open={resourceModalOpen}
        onClose={() => setResourceModalOpen(false)}
        bucketName={currentBucket}
        record={resourceRecord}
        onOk={reload}
      />
      <AddrModal
        open={addrOpen}
        onClose={() => setAddrOpen(false)}
        bucketName={currentBucket}
        record={addrRecord}
      />
      <CopyModal
        open={copyOpen}
        onClose={() => setCopyOpen(false)}
        type={copyType}
        buckets={buckets}
        currentBucket={currentBucket}
        currentDir={currentDir}
        rows={getSelectedRecords()}
        onOk={reload}
      />
    </div>
  );
}

/** 兼容旧版路径归一化 */
export { normalizePath };
