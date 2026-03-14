import { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import type { PropertyPerformance } from '@/types/portfolio';

function formatCurrency(value: number, currencyCode: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currencyCode,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  } catch {
    return `${currencyCode} ${value.toLocaleString()}`;
  }
}

interface PropertyComparisonChartProps {
  data: PropertyPerformance[];
  currency?: string;
  isDark: boolean;
}

export const PropertyComparisonChart = ({
  data,
  currency = 'EUR',
  isDark,
}: PropertyComparisonChartProps) => {
  const sorted = useMemo(() => {
    return [...data]
      .filter((d) => d.monthlyCashFlow != null)
      .sort((a, b) => (b.monthlyCashFlow ?? 0) - (a.monthlyCashFlow ?? 0))
      .slice(0, 10);
  }, [data]);

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

  if (sorted.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 text-text-secondary text-sm">
        No property comparison data available
      </div>
    );
  }

  const barHeight = Math.max(sorted.length * 40, 200);

  return (
    <ResponsiveContainer width="100%" height={barHeight}>
      <BarChart data={sorted} layout="vertical" margin={{ left: 20 }}>
        <CartesianGrid
          strokeDasharray="3 3"
          stroke={isDark ? '#2a2e3f' : '#e2e6f0'}
        />
        <XAxis
          type="number"
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
          tickFormatter={(v: number) => formatCurrency(v, currency)}
        />
        <YAxis
          type="category"
          dataKey="address"
          width={140}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          formatter={(value) => [
            formatCurrency(
              typeof value === 'number' ? value : Number(value),
              currency
            ),
            'Monthly Cash Flow',
          ]}
        />
        <Bar dataKey="monthlyCashFlow" radius={[0, 4, 4, 0]}>
          {sorted.map((entry) => (
            <Cell
              key={entry.identifier}
              fill={(entry.monthlyCashFlow ?? 0) >= 0 ? '#059669' : '#dc2626'}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
};
