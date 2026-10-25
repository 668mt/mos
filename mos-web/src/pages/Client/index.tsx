import {
  Button,
  Progress,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
  type TableColumnsType,
} from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useState } from 'react';
import { fetchClients, kickClient, recoverClient } from '@/services/api';
import { useResponsive } from '@/hooks/useResponsive';
import type { Client } from '@/services/types';

/**
 * 服务器（存储节点）管理。
 *
 * 接口：`/admin/client/**`
 */
export default function ClientPage() {
  const { isMobile } = useResponsive();
  const [rows, setRows] = useState<Client[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const load = useCallback(
    async (params: { pageNum?: number; pageSize?: number }) => {
      setLoading(true);
      try {
        const page = await fetchClients({
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

  const onKick = async (record: Client) => {
    try {
      await kickClient(record.id);
      message.success('剔除成功');
      load({ pageNum: pagination.current, pageSize: pagination.pageSize });
    } catch {
      /* 错误已由拦截器处理 */
    }
  };

  const onRecover = async (record: Client) => {
    try {
      await recoverClient(record.id);
      message.success('恢复成功');
      load({ pageNum: pagination.current, pageSize: pagination.pageSize });
    } catch {
      /* 错误已由拦截器处理 */
    }
  };

  const columns: TableColumnsType<Client> = [
    { title: '客户端名', dataIndex: 'name', width: 220 },
    { title: 'ip', dataIndex: 'ip', width: 130 },
    { title: '端口', dataIndex: 'port', width: 80, responsive: ['sm', 'md', 'lg', 'xl', 'xxl'] },
    { title: '权重', dataIndex: 'weight', width: 80, responsive: ['lg', 'xl', 'xxl'] },
    {
      title: '总存储（GB）',
      dataIndex: 'totalStorageGb',
      width: 140,
      responsive: ['lg', 'xl', 'xxl'],
      render: (v: any) => (v * 1.0).toFixed(1),
    },
    {
      title: '已用存储（GB）',
      dataIndex: 'usedStorageGb',
      width: 140,
      responsive: ['md', 'lg', 'xl', 'xxl'],
      render: (v: any) => (v * 1.0).toFixed(1),
    },
    {
      title: '使用百分比',
      dataIndex: 'usedPercent',
      width: 150,
      responsive: ['md', 'lg', 'xl', 'xxl'],
      render: (v: any) => (
        <Tooltip title={`${(v * 100).toFixed(1)}%`}>
          <Progress
            percent={Math.min(100, Number((v * 100).toFixed(1)))}
            size="small"
            status={v > 0.9 ? 'exception' : v > 0.75 ? 'active' : 'normal'}
          />
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      responsive: ['sm', 'md', 'lg', 'xl', 'xxl'],
      render: (v: string) => {
        if (v === 'UP') return <Tag color="green">{v}</Tag>;
        if (v === 'KICKED') return <Tag color="orange">{v}</Tag>;
        return <Tag color="red">{v}</Tag>;
      },
    },
    {
      title: '上次心跳时间',
      dataIndex: 'lastBeatTime',
      width: 170,
      responsive: ['xl', 'xxl'],
    },
    {
      title: '保留大小（GB）',
      dataIndex: 'keepSpaceByte',
      width: 140,
      responsive: ['xl', 'xxl'],
      render: (v: any) => ((v * 1.0) / (1024 * 1024 * 1024)).toFixed(1),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 120,
      ellipsis: true,
      responsive: ['lg', 'xl', 'xxl'],
    },
    { title: '创建时间', dataIndex: 'createdDate', width: 170, responsive: ['xxl'] },
    { title: '修改时间', dataIndex: 'updatedDate', width: 170, responsive: ['xxl'] },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      render: (_: any, record: Client) => (
        <Space className="mos-table-actions">
          <a onClick={() => onRecover(record)}>恢复</a>
          <a className="mos-danger-link" onClick={() => onKick(record)}>
            剔除
          </a>
        </Space>
      ),
    },
  ];

  return (
    <div className="mos-card" style={{ padding: 16 }}>
      <div className="mos-toolbar">
        <Button
          type="primary"
          icon={<ReloadOutlined />}
          onClick={() => load({ pageNum: pagination.current, pageSize: pagination.pageSize })}
        >
          刷新
        </Button>
      </div>

      <Table<Client>
        className="mos-table"
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={loading}
        scroll={{ x: isMobile ? 380 : 1700 }}
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
    </div>
  );
}
