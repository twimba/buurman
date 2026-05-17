import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useIsMobile } from '@/hooks/useIsMobile';
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
  ReferenceLine,
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
  height?: number;
}

export const PortfolioCashFlowChart = ({
  data,
  currency = 'EUR',
  isDark,
  height = 320,
}: PortfolioCashFlowChartProps) => {
  const { t } = useTranslation('common');
  const isMobile = useIsMobile();
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

  // Dynamic bar size: shrink bars when there are many months so they still align
  const barSize = useMemo(() => {
    const count = data.length;
    if (count <= 6) {
      return 30;
    }
    if (count <= 12) {
      return 24;
    }
    if (count <= 24) {
      return 14;
    }
    if (count <= 48) {
      return 8;
    }
    return 5;
  }, [data.length]);

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 text-text-secondary text-sm">
        {t('dashboard.cashFlow.noData')}
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={chartData} barGap={-barSize} barSize={barSize}>
        {!isMobile && (
          <CartesianGrid
            strokeDasharray="3 3"
            stroke={isDark ? '#2a2e3f' : '#e2e6f0'}
          />
        )}
        <XAxis
          dataKey="month"
          interval={isMobile ? 'preserveStartEnd' : 0}
          tickFormatter={formatMonthTick}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <YAxis
          hide={isMobile}
          tickFormatter={(v: number) => formatAxisValue(v, currency)}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          labelFormatter={(label) => formatMonthTick(String(label))}
          formatter={(value, name) => {
            const labels: Record<string, string> = {
              income: t('dashboard.cashFlow.income'),
              negExpenses: t('dashboard.cashFlow.expenses'),
              negMortgage: t('dashboard.cashFlow.mortgage'),
              net: t('dashboard.cashFlow.net'),
            };
            const num = typeof value === 'number' ? value : Number(value);
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
        {!isMobile && (
        <Legend
          formatter={(value) => {
            const labels2: Record<string, string> = {
              income: t('dashboard.cashFlow.income'),
              negExpenses: t('dashboard.cashFlow.expenses'),
              negMortgage: t('dashboard.cashFlow.mortgage'),
              net: t('dashboard.cashFlow.net'),
            };
            return labels2[value] ?? value;
          }}
        />
        )}
        <ReferenceLine y={0} stroke={isDark ? '#4a4e5f' : '#b0b5c8'} />
        <Bar
          dataKey="income"
          stackId="positive"
          fill={COLORS.income}
          radius={[2, 2, 0, 0]}
        />
        <Bar dataKey="negExpenses" stackId="negative" fill={COLORS.expenses} />
        <Bar
          dataKey="negMortgage"
          stackId="negative"
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
