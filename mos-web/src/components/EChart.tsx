import * as echarts from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { useEffect, useRef } from 'react';

echarts.use([
  LineChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer,
]);

export interface ChartData {
  time: string[];
  read: number[];
  write: number[];
}

export interface EChartProps {
  /** 是否流量图（单位 MB） */
  isFlow?: boolean;
  title: string;
  data: ChartData;
  height?: number;
}

/**
 * 折线图。
 *
 * 数据来源：`GET /member/audit/chart/{bucket}/{type}`
 */
export default function EChart({ isFlow = false, title, data, height }: EChartProps) {
  const ref = useRef<HTMLDivElement>(null);
  const chartRef = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!ref.current) return;
    const chart = echarts.init(ref.current);
    chartRef.current = chart;

    const resize = () => chart.resize();
    window.addEventListener('resize', resize);

    return () => {
      window.removeEventListener('resize', resize);
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart) return;

    chart.setOption(
      {
        title: {
          text: title,
          left: 'center',
          top: 8,
          textStyle: { fontSize: 14, fontWeight: 'normal', color: 'rgba(0,0,0,.65)' },
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross' },
        },
        legend: {
          data: isFlow ? ['读流量', '写流量'] : ['读请求', '写请求'],
          bottom: 4,
          itemWidth: 14,
          itemHeight: 8,
        },
        grid: { left: 46, right: 20, top: 46, bottom: 46 },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: data.time,
          axisLabel: { fontSize: 10, color: 'rgba(0,0,0,.45)' },
          axisLine: { lineStyle: { color: '#e8e8e8' } },
        },
        yAxis: {
          type: 'value',
          name: isFlow ? '单位：MB' : '请求数',
          nameTextStyle: { fontSize: 10, color: 'rgba(0,0,0,.45)' },
          axisLabel: { fontSize: 10, color: 'rgba(0,0,0,.45)' },
          splitLine: { lineStyle: { color: '#f0f0f0' } },
        },
        series: [
          {
            name: isFlow ? '读流量' : '读请求',
            type: 'line',
            smooth: true,
            showSymbol: false,
            itemStyle: { color: '#1677ff' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(22,119,255,.28)' },
                { offset: 1, color: 'rgba(22,119,255,.02)' },
              ]),
            },
            data: data.read,
          },
          {
            name: isFlow ? '写流量' : '写请求',
            type: 'line',
            smooth: true,
            showSymbol: false,
            itemStyle: { color: 'rgb(255, 70, 131)' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgb(255, 158, 68)' },
                { offset: 1, color: 'rgb(255, 70, 131)' },
              ]),
            },
            data: data.write,
          },
        ],
      },
      true,
    );
  }, [data, isFlow, title]);

  return (
    <div
      ref={ref}
      className="mos-chart"
      style={height ? { height } : undefined}
    />
  );
}
