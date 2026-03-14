import { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import type { PropertyEquity } from '@/types/portfolio';

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

function getCurrencySymbol(currencyCode: string): string {
  try {
    const parts = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currencyCode,
    }).formatToParts(0);
    return parts.find((p) => p.type === 'currency')?.value ?? currencyCode;
  } catch {
    return currencyCode;
  }
}

interface EquityCompositionChartProps {
  data: PropertyEquity[];
  currency?: string;
  isDark: boolean;
}

export const EquityCompositionChart = ({
  data,
  currency = 'EUR',
  isDark,
}: EquityCompositionChartProps) => {
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

  const chartData = useMemo(
    () =>
      data
        .filter((d) => d.marketValue != null)
        .map((d) => ({
          address: d.address,
          equity: d.equity ?? 0,
          mortgage: d.mortgage ?? 0,
        })),
    [data]
  );

  if (chartData.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 text-text-secondary text-sm">
        No equity data available
      </div>
    );
  }

  const barHeight = Math.max(chartData.length * 40, 200);

  return (
    <ResponsiveContainer width="100%" height={barHeight}>
      <BarChart data={chartData} layout="vertical" margin={{ left: 20 }}>
        <CartesianGrid
          strokeDasharray="3 3"
          stroke={isDark ? '#2a2e3f' : '#e2e6f0'}
        />
        <XAxis
          type="number"
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
          tickFormatter={(v: number) => {
            const symbol = getCurrencySymbol(currency);
            const abs = Math.abs(v);
            if (abs >= 1_000_000) {
              return `${symbol}${(abs / 1_000_000).toFixed(1)}M`;
            }
            if (abs >= 1_000) {
              return `${symbol}${(abs / 1_000).toFixed(0)}K`;
            }
            return `${symbol}${abs.toFixed(0)}`;
          }}
        />
        <YAxis
          type="category"
          dataKey="address"
          width={140}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          formatter={(value?: number | string, name?: string) => [
            formatCurrency(Number(value ?? 0), currency),
            name === 'equity' ? 'Equity' : 'Mortgage',
          ]}
        />
        <Legend
          formatter={(value: string) =>
            value === 'equity' ? 'Equity' : 'Mortgage'
          }
        />
        <Bar
          dataKey="equity"
          stackId="equity"
          fill="#059669"
          radius={[0, 0, 0, 0]}
        />
        <Bar
          dataKey="mortgage"
          stackId="equity"
          fill="#0284c7"
          radius={[0, 4, 4, 0]}
        />
      </BarChart>
    </ResponsiveContainer>
  );
};
