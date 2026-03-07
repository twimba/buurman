import { useMemo } from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import type { OccupancyDataPoint } from '@/types/property';

const MONTH_NAMES = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

function formatMonthTick(v: string): string {
  const [y, m] = v.split('-');
  const month = MONTH_NAMES[parseInt(m, 10) - 1];
  if (!month) {
    return v;
  }
  return `${month} '${y.slice(2)}`;
}

interface PortfolioOccupancyChartProps {
  data: OccupancyDataPoint[];
  isDark: boolean;
}

export const PortfolioOccupancyChart = ({
  data,
  isDark,
}: PortfolioOccupancyChartProps) => {
  const tooltipStyle = useMemo(
    () => ({
      backgroundColor: isDark ? '#14161f' : '#fff',
      border: `1px solid ${isDark ? '#2a2e3f' : '#e2e6f0'}`,
      borderRadius: '8px',
      fontSize: '12px',
      color: isDark ? '#eef0f6' : '#1a1d2e',
    }),
    [isDark]
  );

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 text-[#6b7194] dark:text-[#8b90a8] text-sm">
        No occupancy data available
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={320}>
      <AreaChart data={data}>
        <defs>
          <linearGradient id="portfolioTenantGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#06B6D4" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#06B6D4" stopOpacity={0} />
          </linearGradient>
          <linearGradient id="portfolioSelfGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#3B82F6" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid
          strokeDasharray="3 3"
          stroke={isDark ? '#2a2e3f' : '#e2e6f0'}
        />
        <XAxis
          dataKey="month"
          tickFormatter={formatMonthTick}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <YAxis
          domain={[0, 100]}
          tickFormatter={(v: number) => `${v}%`}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          labelFormatter={(label) => formatMonthTick(String(label))}
          formatter={(value?: number | string, name?: string) => {
            const labels: Record<string, string> = {
              tenantOccupancyPercent: 'Tenant Occupancy',
              selfOccupancyPercent: 'Self Occupancy',
            };
            return [
              `${Number(value ?? 0).toFixed(1)}%`,
              labels[name ?? ''] ?? name,
            ];
          }}
        />
        <Legend
          formatter={(value: string) => {
            const labels: Record<string, string> = {
              tenantOccupancyPercent: 'Tenant Occupancy',
              selfOccupancyPercent: 'Self Occupancy',
            };
            return labels[value] ?? value;
          }}
        />
        <Area
          type="monotone"
          dataKey="tenantOccupancyPercent"
          stackId="occ"
          stroke="#06B6D4"
          fill="url(#portfolioTenantGrad)"
          strokeWidth={2}
        />
        <Area
          type="monotone"
          dataKey="selfOccupancyPercent"
          stackId="occ"
          stroke="#3B82F6"
          fill="url(#portfolioSelfGrad)"
          strokeWidth={2}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};
