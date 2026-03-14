import { useMemo } from 'react';
import {
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import type { MonthlyDataPoint } from '@/types/property';

const COLORS = {
  income: '#059669',
  expenses: '#dc2626',
  net: '#0284c7',
  mortgage: '#7c3aed',
};

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

function formatAxisValue(value: number, currencyCode: string): string {
  const symbol = getCurrencySymbol(currencyCode);
  const abs = Math.abs(value);
  const sign = value < 0 ? '-' : '';
  if (abs >= 1_000_000) {
    return `${sign}${symbol}${(abs / 1_000_000).toFixed(1)}M`;
  }
  if (abs >= 1_000) {
    return `${sign}${symbol}${(abs / 1_000).toFixed(0)}K`;
  }
  return `${sign}${symbol}${abs.toFixed(0)}`;
}

function formatCurrency(value: number, currencyCode: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currencyCode,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  } catch {
    return `${currencyCode} ${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}

interface PortfolioCashFlowChartProps {
  data: MonthlyDataPoint[];
  currency?: string;
  isDark: boolean;
}

export const PortfolioCashFlowChart = ({
  data,
  currency = 'EUR',
  isDark,
}: PortfolioCashFlowChartProps) => {
  const chartData = useMemo(
    () =>
      data.map((d) => ({
        ...d,
        negExpenses: -d.expenses,
        negMortgage: -d.mortgage,
      })),
    [data]
  );

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
        No cash flow data available
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={320}>
      <ComposedChart data={chartData}>
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
          tickFormatter={(v: number) => formatAxisValue(v, currency)}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          labelFormatter={(label) => formatMonthTick(String(label))}
          formatter={(value?: number | string, name?: string) => {
            const labels: Record<string, string> = {
              income: 'Income',
              negExpenses: 'Expenses',
              negMortgage: 'Mortgage',
              net: 'Net',
            };
            const num = Number(value ?? 0);
            const display =
              name === 'negExpenses' || name === 'negMortgage'
                ? Math.abs(num)
                : num;
            return [
              formatCurrency(display, currency),
              labels[name ?? ''] ?? name,
            ];
          }}
        />
        <Legend
          formatter={(value: string) => {
            const labels: Record<string, string> = {
              income: 'Income',
              negExpenses: 'Expenses',
              negMortgage: 'Mortgage',
              net: 'Net',
            };
            return labels[value] ?? value;
          }}
        />
        <Bar
          dataKey="income"
          stackId="cashflow"
          fill={COLORS.income}
          radius={[2, 2, 0, 0]}
        />
        <Bar
          dataKey="negExpenses"
          stackId="cashflow"
          fill={COLORS.expenses}
          radius={[0, 0, 2, 2]}
        />
        <Bar
          dataKey="negMortgage"
          stackId="cashflow"
          fill={COLORS.mortgage}
          radius={[0, 0, 2, 2]}
        />
        <Line
          type="monotone"
          dataKey="net"
          stroke={COLORS.net}
          strokeWidth={2}
          dot={false}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
};
