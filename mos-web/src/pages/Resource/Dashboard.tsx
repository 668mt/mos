import { Alert, Button, Space, Spin, Tag, Typography } from 'antd';
import {
  DeleteOutlined,
  FolderOpenOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { history } from '@umijs/max';
import { useCallback, useEffect, useState } from 'react';
import { fetchChart, fetchStatisticInfo } from '@/services/api';
import EChart from '@/components/EChart';
import { useBucketContext } from '@/hooks/useBucketContext';
import { usePerms } from '@/hooks/usePerms';
import { useResponsive } from '@/hooks/useResponsive';
import BucketNav from './components/BucketNav';
import type { StatisticInfo } from '@/services/types';

const { Text } = Typography;

type ChartType = 'request24Hours' | 'flow24Hours' | 'request30Days' | 'flow30Days';

const CHART_TYPES: { key: ChartType; title: string; isFlow: boolean }[] = [
  { key: 'request24Hours', title: '最近24小时内的请求', isFlow: false },
  { key: 'flow24Hours', title: '最近24小时内的流量', isFlow: true },
  { key: 'request30Days', title: '最近30天内的请求', isFlow: false },
  { key: 'flow30Days', title: '最近30天内的流量', isFlow: true },
];

/** 统计卡片 */
function StatCard({
  label,
  read,
  write,
  readText,
  writeText,
  unit,
}: {
  label: string;
  read?: number;
  write?: number;
  readText?: string;
  writeText?: string;
  unit?: string;
}) {
  return (
    <div className="mos-stat-card">
      <div className="mos-stat-label">{label}</div>
      <div className="mos-stat-value">
        {readText !== undefined ? (
          <>
            <span>{readText}</span>
            <span className="mos-stat-split">/ {writeText}</span>
          </>
        ) : (
          <>
            <span>{read ?? 0}</span>
            <span className="mos-stat-split">/ {write ?? 0} {unit}</span>
          </>
        )}
      </div>
      <div style={{ marginTop: 8 }}>
        <span className="mos-stat-badge read">
          读 {readText ?? read ?? 0}
          {readText === undefined && unit ? ` ${unit}` : ''}
        </span>
        <span className="mos-stat-badge write">
          写 {writeText ?? write ?? 0}
          {writeText === undefined && unit ? ` ${unit}` : ''}
        </span>
      </div>
    </div>
  );
}

/**
 * 桶内仪表盘。
 *
 * 展示当前桶的读写统计与四张趋势图。
 * 数据来自 `GET /member/audit/statistic/info/{bucket}` 与
 * `GET /member/audit/chart/{bucket}/{type}`。
 */
export default function DashboardPage() {
  const { isMobile } = useResponsive();
  const { buckets, loading, reload, currentBucket, currentBucketVo } = useBucketContext();
  const { hasPerm } = usePerms();

  const [statistic, setStatistic] = useState<StatisticInfo | null>(null);
  const [statLoading, setStatLoading] = useState(false);
  const [chartData, setChartData] = useState<Record<ChartType, any>>({
    request24Hours: { time: [], read: [], write: [] },
    flow24Hours: { time: [], read: [], write: [] },
    request30Days: { time: [], read: [], write: [] },
    flow30Days: { time: [], read: [], write: [] },
  });
  const [chartLoading, setChartLoading] = useState<Record<ChartType, boolean>>({
    request24Hours: false,
    flow24Hours: false,
    request30Days: false,
    flow30Days: false,
  });

  const canSelect = hasPerm(currentBucket, 'SELECT');

  // 统计信息
  useEffect(() => {
    if (!currentBucket) return;
    setStatLoading(true);
    fetchStatisticInfo(currentBucket)
      .then((info) => setStatistic(info))
      .catch(() => setStatistic(null))
      .finally(() => setStatLoading(false));
  }, [currentBucket]);

  // 图表数据
  const loadChart = useCallback(
    async (type: ChartType) => {
      if (!currentBucket) return;
      setChartLoading((prev) => ({ ...prev, [type]: true }));
      try {
        const raw = await fetchChart(currentBucket, type);
        const arr = (raw || []) as any[];
        const isFlow = type.startsWith('flow');
        setChartData((prev) => ({
          ...prev,
          [type]: {
            time: arr.map((r) => r.time),
            read: arr.map((r) => (isFlow ? r.readMb : r.readRequests)),
            write: arr.map((r) => (isFlow ? r.writeMb : r.writeRequests)),
          },
        }));
      } catch {
        // 错误已由拦截器处理
      } finally {
        setChartLoading((prev) => ({ ...prev, [type]: false }));
      }
    },
    [currentBucket],
  );

  useEffect(() => {
    CHART_TYPES.forEach((c) => loadChart(c.key));
  }, [loadChart]);

  const onSwitchBucket = useCallback((bucketName: string) => {
    history.push(`/resource/dashboard?b=${encodeURIComponent(bucketName)}`);
  }, []);

  if (!loading && buckets.length === 0) {
    return (
      <div>
        <BucketNav buckets={buckets} current={currentBucket} onSwitchBucket={onSwitchBucket} />
        <div className="mos-card" style={{ padding: 16 }}>
          <Alert
            type="info"
            showIcon
            message="您还没有创建桶，先去创建一个吧！"
            action={
              <a onClick={() => history.push('/resource')}>去创建</a>
            }
          />
        </div>
      </div>
    );
  }

  const stat = statistic;

  return (
    <div>
      <BucketNav
        buckets={buckets}
        current={currentBucket}
        onSwitchBucket={onSwitchBucket}
      />

      <div className="mos-card" style={{ padding: '16px 20px', marginBottom: 16 }}>
        <Space
          style={{ width: '100%', justifyContent: 'space-between' }}
          wrap
          size={12}
        >
          <div>
            <Space size={8} wrap>
              <Text strong style={{ fontSize: 16 }}>
                {currentBucket}
              </Text>
              {currentBucketVo?.defaultIsPublic ? (
                <Tag color="orange">公开</Tag>
              ) : (
                <Tag color="green">私有</Tag>
              )}
              {currentBucketVo && !currentBucketVo.isOwn && (
                <Tag color="blue">由 {currentBucketVo.owner} 授权</Tag>
              )}
            </Space>
            <div>
              <Text type="secondary" style={{ fontSize: 12 }}>
                {currentBucketVo
                  ? `分片数 ${currentBucketVo.dataFragmentsAmount} · 创建于 ${
                      currentBucketVo.createdDate || '-'
                    }`
                  : ' '}
              </Text>
            </div>
          </div>
          <Space size={8} wrap>
            <Button
              icon={<FolderOpenOutlined />}
              disabled={!canSelect}
              onClick={() =>
                history.push(
                  currentBucket
                    ? `/resource/files?b=${encodeURIComponent(currentBucket)}`
                    : '/resource/files',
                )
              }
            >
              浏览文件
            </Button>
            <Button
              icon={<DeleteOutlined />}
              onClick={() =>
                history.push(
                  currentBucket
                    ? `/resource/trash?b=${encodeURIComponent(currentBucket)}`
                    : '/resource/trash',
                )
              }
            >
              回收站
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                reload();
                CHART_TYPES.forEach((c) => loadChart(c.key));
              }}
            >
              刷新
            </Button>
          </Space>
        </Space>
      </div>

      <Spin spinning={statLoading}>
        <div className="mos-stat-grid" style={{ marginBottom: 16 }}>
          <StatCard
            label="今日读写请求"
            read={stat?.thisDayReadRequests ?? 0}
            write={stat?.thisDayWriteRequests ?? 0}
            unit="次"
          />
          <StatCard
            label="今日读写流量"
            readText={stat?.readableThisDayRead ?? '0 B'}
            writeText={stat?.readableThisDayWrite ?? '0 B'}
          />
          <StatCard
            label="今月读写请求"
            read={stat?.thisMonthReadRequests ?? 0}
            write={stat?.thisMonthWriteRequests ?? 0}
            unit="次"
          />
          <StatCard
            label="今月读写流量"
            readText={stat?.readableThisMonthRead ?? '0 B'}
            writeText={stat?.readableThisMonthWrite ?? '0 B'}
          />
        </div>
      </Spin>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr',
          gap: 16,
        }}
      >
        {CHART_TYPES.map((c) => (
          <div key={c.key} className="mos-chart-card">
            <Spin spinning={chartLoading[c.key]}>
              <EChart
                title={c.title}
                isFlow={c.isFlow}
                data={chartData[c.key]}
              />
            </Spin>
          </div>
        ))}
      </div>
    </div>
  );
}
