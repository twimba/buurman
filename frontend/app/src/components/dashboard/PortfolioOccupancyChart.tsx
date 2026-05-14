import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
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
  height?: number;
}

export const PortfolioOccupancyChart = ({
  data,
  isDark,
  height = 320,
}: PortfolioOccupancyChartProps) => {
  const { t } = useTranslation('common');
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
      <div className="flex items-center justify-center h-80 text-text-secondary text-sm">
        {t('dashboard.occupancy.noData')}
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data}>
        <defs>
          <linearGradient id="portfolioContactGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#06B6D4" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#06B6D4" stopOpacity={0} />
          </linearGradient>
          <linearGradient id="portfolioSelfGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#0284c7" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#0284c7" stopOpacity={0} />
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
          formatter={(value, name) => {
            const labels: Record<string, string> = {
              contactOccupancyPercent: t(
                'dashboard.occupancy.contactOccupancy'
              ),
              selfOccupancyPercent: t('dashboard.occupancy.selfOccupancy'),
            };
            return [
              `${(typeof value === 'number' ? value : Number(value)).toFixed(1)}%`,
              labels[name ?? ''] ?? name,
            ];
          }}
        />
        <Legend
          formatter={(value) => {
            const labels: Record<string, string> = {
              contactOccupancyPercent: t(
                'dashboard.occupancy.contactOccupancy'
              ),
              selfOccupancyPercent: t('dashboard.occupancy.selfOccupancy'),
            };
            return labels[value] ?? value;
          }}
        />
        <Area
          type="monotone"
          dataKey="contactOccupancyPercent"
          stackId="occ"
          stroke="#06B6D4"
          fill="url(#portfolioContactGrad)"
          strokeWidth={2}
        />
        <Area
          type="monotone"
          dataKey="selfOccupancyPercent"
          stackId="occ"
          stroke="#0284c7"
          fill="url(#portfolioSelfGrad)"
          strokeWidth={2}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};
