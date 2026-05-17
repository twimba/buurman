import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useIsMobile } from '@/hooks/useIsMobile';
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
  const { t } = useTranslation('common');
  const isMobile = useIsMobile();
  // Theme-aligned palette — matches packages/ui/src/styles/theme.css. The
  // previous values (#14161f / #2a2e3f / #eef0f6 / #1a1d2e) were off-theme
  // (a custom blue-grey not in the Buurman palette), creating a visible
  // seam between charts and Card backgrounds.
  const surfaceCard = isDark ? '#1c1917' : '#ffffff';
  const borderDefault = isDark ? '#1c1917' : '#e7e5e4';
  const textPrimary = isDark ? '#fafaf9' : '#0c0a09';
  const textMuted = isDark ? '#a8a29e' : '#78716c';

  const tooltipStyle = useMemo(
    () => ({
      backgroundColor: surfaceCard,
      border: `1px solid ${borderDefault}`,
      borderRadius: '8px',
      fontSize: '12px',
      color: textPrimary,
    }),
    [surfaceCard, borderDefault, textPrimary]
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
        {t('dashboard.equity.noData')}
      </div>
    );
  }

  const barHeight = Math.max(chartData.length * 40, 200);

  return (
    <ResponsiveContainer width="100%" height={barHeight}>
      <BarChart
        data={chartData}
        layout="vertical"
        margin={{ left: isMobile ? 6 : 20 }}
      >
        {!isMobile && (
          <CartesianGrid strokeDasharray="3 3" stroke={borderDefault} />
        )}
        <XAxis
          type="number"
          hide={isMobile}
          tick={{ fontSize: 11, fill: textMuted }}
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
          width={isMobile ? 110 : 140}
          tick={{
            fontSize: isMobile ? 10 : 11,
            fill: textMuted,
          }}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          formatter={(value, name) => [
            formatCurrency(
              typeof value === 'number' ? value : Number(value),
              currency
            ),
            name === 'equity'
              ? t('dashboard.equity.equity')
              : t('dashboard.equity.mortgage'),
          ]}
        />
        {!isMobile && (
          <Legend
            formatter={(value) =>
              value === 'equity'
                ? t('dashboard.equity.equity')
                : t('dashboard.equity.mortgage')
            }
          />
        )}
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
