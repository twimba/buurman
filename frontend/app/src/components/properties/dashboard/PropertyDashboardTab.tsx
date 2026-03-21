import { useCallback, useMemo, useState } from 'react';
import {
  ComposedChart,
  BarChart,
  Bar,
  LineChart,
  Line,
  ReferenceLine,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  Brush,
  ReferenceArea,
} from 'recharts';
import {
  TrendingUp,
  DollarSign,
  Home,
  Percent,
  BarChart3,
  AlertCircle,
  CheckCircle2,
  RefreshCw,
  Download,
  Calendar,
} from 'lucide-react';
import { useTheme } from '@/context/ThemeContext';
import { usePropertyDashboard } from '@/hooks/usePropertyHooks';
import {
  exportPropertyDashboardPDF,
  exportPropertyDashboardCSV,
  exportPropertyDashboardExcel,
} from '@/api/properties';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ExportDropdown } from '@/components/common/ExportDropdown';
import { MetricHint } from '@/components/common/MetricHint';
import type {
  DashboardSummaryMetrics,
  CashFlowChartData,
  EquityChartData,
  ExpenseBreakdownChartData,
  OccupancyChartData,
  DashboardDataCompleteness,
  FutureTrendData,
} from '@/types/property';
import { Area, AreaChart } from 'recharts';

const COLORS = {
  income: '#10B981',
  expenses: '#EF4444',
  net: '#3B82F6',
  mortgage: '#8B5CF6',
  occupancy: '#06B6D4',
};

const PIE_COLORS = [
  '#EF4444',
  '#F59E0B',
  '#10B981',
  '#3B82F6',
  '#8B5CF6',
  '#EC4899',
  '#06B6D4',
  '#F97316',
  '#6366F1',
  '#14B8A6',
  '#84CC16',
];

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

function formatCurrency(
  value: number | null | undefined,
  currencyCode: string
): string {
  if (value == null) {
    return 'N/A';
  }
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

// --- Chart zoom hook (click-and-drag to zoom) ---

function useChartZoom<T extends { month: string }>(chartData: T[]) {
  const [refAreaLeft, setRefAreaLeft] = useState<string | null>(null);
  const [refAreaRight, setRefAreaRight] = useState<string | null>(null);
  const [zoomStart, setZoomStart] = useState<number | null>(null);
  const [zoomEnd, setZoomEnd] = useState<number | null>(null);

  const handleMouseDown = useCallback(
    (e: { activeLabel?: string | number } | null) => {
      if (e?.activeLabel != null) {
        setRefAreaLeft(String(e.activeLabel));
        setRefAreaRight(null);
      }
    },
    []
  );

  const handleMouseMove = useCallback(
    (e: { activeLabel?: string | number } | null) => {
      if (refAreaLeft && e?.activeLabel != null) {
        setRefAreaRight(String(e.activeLabel));
      }
    },
    [refAreaLeft]
  );

  const handleMouseUp = useCallback(() => {
    if (refAreaLeft && refAreaRight) {
      const leftIdx = chartData.findIndex((d) => d.month === refAreaLeft);
      const rightIdx = chartData.findIndex((d) => d.month === refAreaRight);
      if (leftIdx >= 0 && rightIdx >= 0) {
        const startIdx = Math.min(leftIdx, rightIdx);
        const endIdx = Math.max(leftIdx, rightIdx);
        if (endIdx - startIdx >= 1) {
          setZoomStart(startIdx);
          setZoomEnd(endIdx);
        }
      }
    }
    setRefAreaLeft(null);
    setRefAreaRight(null);
  }, [refAreaLeft, refAreaRight, chartData]);

  const resetZoom = useCallback(() => {
    setZoomStart(null);
    setZoomEnd(null);
  }, []);

  const visibleData = useMemo(() => {
    if (zoomStart !== null && zoomEnd !== null) {
      return chartData.slice(zoomStart, zoomEnd + 1);
    }
    return chartData;
  }, [chartData, zoomStart, zoomEnd]);

  const isZoomed = zoomStart !== null;

  return {
    visibleData,
    refAreaLeft,
    refAreaRight,
    isZoomed,
    handleMouseDown,
    handleMouseMove,
    handleMouseUp,
    resetZoom,
  };
}

type PeriodType = 'ytd' | '3' | '6' | '12' | '24' | '36' | 'all' | 'custom';

const PERIOD_OPTIONS: { value: PeriodType; label: string }[] = [
  { value: 'ytd', label: 'YTD' },
  { value: '3', label: '3M' },
  { value: '6', label: '6M' },
  { value: '12', label: '12M' },
  { value: '24', label: '24M' },
  { value: '36', label: '36M' },
  { value: 'all', label: 'All Time' },
  { value: 'custom', label: 'Custom' },
];

function computeMonths(
  periodType: PeriodType,
  customStartDate: string,
  customEndDate: string
): number {
  const today = new Date();
  switch (periodType) {
    case 'ytd': {
      const jan1 = new Date(today.getFullYear(), 0, 1);
      return (
        (today.getFullYear() - jan1.getFullYear()) * 12 +
        (today.getMonth() - jan1.getMonth()) +
        1
      );
    }
    case '3':
    case '6':
    case '12':
    case '24':
    case '36':
      return parseInt(periodType, 10);
    case 'all':
      return 0;
    case 'custom': {
      if (!customStartDate || !customEndDate) {
        return 12;
      }
      const start = new Date(customStartDate);
      const end = new Date(customEndDate);
      return Math.max(
        (end.getFullYear() - start.getFullYear()) * 12 +
          (end.getMonth() - start.getMonth()) +
          1,
        1
      );
    }
  }
}

interface PropertyDashboardTabProps {
  propertyId: string;
}

export const PropertyDashboardTab = ({
  propertyId,
}: PropertyDashboardTabProps) => {
  const [periodType, setPeriodType] = useState<PeriodType>('12');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  const months = useMemo(
    () => computeMonths(periodType, customStartDate, customEndDate),
    [periodType, customStartDate, customEndDate]
  );

  const {
    data: dashboard,
    isLoading,
    error,
    refetch,
  } = usePropertyDashboard(propertyId, months);
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';

  const [exporting, setExporting] = useState<'pdf' | 'csv' | 'excel' | null>(
    null
  );
  const handlePeriodChange = useCallback((type: PeriodType) => {
    setPeriodType(type);
  }, []);

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

  const handleExport = async (format: 'pdf' | 'csv' | 'excel') => {
    setExporting(format);
    try {
      let blob: Blob;
      let mimeType: string;
      let ext: string;
      if (format === 'pdf') {
        blob = await exportPropertyDashboardPDF(propertyId, months);
        mimeType = 'application/pdf';
        ext = 'pdf';
      } else if (format === 'excel') {
        blob = await exportPropertyDashboardExcel(propertyId, months);
        mimeType =
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        ext = 'xlsx';
      } else {
        blob = await exportPropertyDashboardCSV(propertyId, months);
        mimeType = 'text/csv';
        ext = 'csv';
      }
      const file = new Blob([blob], { type: mimeType });
      const url = window.URL.createObjectURL(file);
      const link = document.createElement('a');
      link.href = url;
      link.download = `property-dashboard-${propertyId}.${ext}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch {
      // silently fail — user sees the button reset
    } finally {
      setExporting(null);
    }
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }
  if (error) {
    return (
      <div className="bg-error-bg border border-error-border text-error-text px-4 py-3 rounded-lg text-sm">
        Failed to load dashboard data.{' '}
        <button
          onClick={() => refetch()}
          className="inline-flex items-center gap-1 underline hover:no-underline"
        >
          <RefreshCw className="h-3 w-3" /> Retry
        </button>
      </div>
    );
  }
  if (!dashboard) {
    return null;
  }

  const {
    summary,
    cashFlow,
    equity,
    expenseBreakdown,
    occupancy,
    dataCompleteness,
    futureTrend,
  } = dashboard;

  return (
    <div className="space-y-6">
      {/* Toolbar: period selector + export buttons */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2 flex-wrap">
          <Calendar className="h-4 w-4 text-text-secondary shrink-0" />
          <div className="flex gap-1 flex-wrap">
            {PERIOD_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => handlePeriodChange(opt.value)}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                  periodType === opt.value
                    ? 'bg-primary-500 text-white'
                    : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
          {periodType === 'custom' && (
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={customStartDate}
                onChange={(e) => {
                  setCustomStartDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-2 py-1.5 border border-border-strong rounded-md text-xs focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-surface-card text-text-primary"
              />
              <span className="text-xs text-text-secondary">to</span>
              <input
                type="date"
                value={customEndDate}
                onChange={(e) => {
                  setCustomEndDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-2 py-1.5 border border-border-strong rounded-md text-xs focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-surface-card text-text-primary"
              />
            </div>
          )}
        </div>
        <div className="flex gap-2 shrink-0">
          <ExportDropdown
            size="sm"
            disabled={exporting !== null}
            exporting={exporting === 'csv' || exporting === 'excel'}
            options={[
              { label: 'CSV', onExport: () => handleExport('csv') },
              { label: 'Excel', onExport: () => handleExport('excel') },
            ]}
          />
          <button
            onClick={() => handleExport('pdf')}
            disabled={exporting !== null}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50"
          >
            <Download className="h-3.5 w-3.5" />
            {exporting === 'pdf' ? 'Exporting...' : 'PDF'}
          </button>
        </div>
      </div>

      {/* Data Completeness Banner */}
      {dataCompleteness.completenessPercent < 100 && (
        <DataCompletenessCard data={dataCompleteness} />
      )}

      {/* Summary Metrics */}
      <SummaryCards metrics={summary} />

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Monthly Cash Flow"
          icon={<BarChart3 className="h-5 w-5" />}
        >
          <CashFlowChart
            data={cashFlow}
            tooltipStyle={tooltipStyle}
            isDark={isDark}
            currency={summary.currency || ''}
          />
        </ChartCard>

        <ChartCard
          title="Occupancy & Income Trend"
          icon={<TrendingUp className="h-5 w-5" />}
        >
          <OccupancyAndTrendChart
            occupancy={occupancy}
            cashFlow={cashFlow}
            tooltipStyle={tooltipStyle}
            isDark={isDark}
            currency={summary.currency || ''}
          />
        </ChartCard>

        <ChartCard
          title="Expense Breakdown"
          icon={<DollarSign className="h-5 w-5" />}
        >
          <ExpenseTimelineChart
            data={expenseBreakdown}
            tooltipStyle={tooltipStyle}
            isDark={isDark}
            currency={summary.currency || ''}
          />
        </ChartCard>

        {futureTrend && futureTrend.months.length > 0 && (
          <ChartCard
            title="6-Month Projection"
            icon={<TrendingUp className="h-5 w-5" />}
          >
            <FutureTrendChart
              data={futureTrend}
              tooltipStyle={tooltipStyle}
              isDark={isDark}
              currency={summary.currency || ''}
            />
          </ChartCard>
        )}

        <ChartCard
          title="Equity Overview"
          icon={<TrendingUp className="h-5 w-5" />}
        >
          <EquityBreakdownCard data={equity} currency={summary.currency} />
        </ChartCard>
      </div>
    </div>
  );
};

// --- Summary Cards ---

function SummaryCards({ metrics }: { metrics: DashboardSummaryMetrics }) {
  const cur = metrics.currency || '';
  const fmtMoney = (val: number | null | undefined) => formatCurrency(val, cur);
  const fmtPct = (val: number | null | undefined) =>
    val != null ? `${val >= 0 ? '+' : ''}${val.toFixed(2)}%` : 'N/A';

  const cards = [
    {
      label: 'Total ROI',
      value: fmtPct(metrics.totalRoiPercent),
      icon: <TrendingUp className="h-5 w-5" />,
      positive: (metrics.totalRoiPercent ?? 0) >= 0,
    },
    {
      label: 'Annualized ROI',
      value: fmtPct(metrics.annualizedRoiPercent),
      icon: <Percent className="h-5 w-5" />,
      positive: (metrics.annualizedRoiPercent ?? 0) >= 0,
    },
    {
      label: 'Cap Rate',
      value: fmtPct(metrics.capRatePercent),
      icon: <Percent className="h-5 w-5" />,
      positive: (metrics.capRatePercent ?? 0) >= 0,
    },
    {
      label: 'Cash-on-Cash',
      value: fmtPct(metrics.cashOnCashPercent),
      icon: <Percent className="h-5 w-5" />,
      positive: (metrics.cashOnCashPercent ?? 0) >= 0,
    },
    {
      label: 'Monthly Cash Flow',
      value: fmtMoney(metrics.monthlyCashFlow),
      icon: <DollarSign className="h-5 w-5" />,
      positive: (metrics.monthlyCashFlow ?? 0) >= 0,
    },
    {
      label: 'Annual NOI',
      value: fmtMoney(metrics.annualNoi),
      icon: <DollarSign className="h-5 w-5" />,
      positive: (metrics.annualNoi ?? 0) >= 0,
    },
    {
      label: 'Total Equity',
      value: fmtMoney(metrics.totalEquity),
      icon: <Home className="h-5 w-5" />,
      positive: (metrics.totalEquity ?? 0) >= 0,
    },
    {
      label: 'Equity Growth',
      value: fmtPct(metrics.equityGrowthPercent),
      icon: <TrendingUp className="h-5 w-5" />,
      positive: (metrics.equityGrowthPercent ?? 0) >= 0,
    },
    {
      label: 'Occupancy',
      value:
        metrics.occupancyRatePercent != null
          ? `${metrics.occupancyRatePercent.toFixed(1)}%`
          : 'N/A',
      icon: <Home className="h-5 w-5" />,
      positive: (metrics.occupancyRatePercent ?? 0) >= 50,
    },
    {
      label: 'Gross Rent Multiplier',
      value:
        metrics.grossRentMultiplier != null
          ? `${metrics.grossRentMultiplier.toFixed(1)}x`
          : 'N/A',
      icon: <BarChart3 className="h-5 w-5" />,
      positive: true,
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
      {cards.map((card) => (
        <div
          key={card.label}
          className="bg-surface-card rounded-lg border border-border-default p-4"
        >
          <div className="flex items-center gap-2 mb-2">
            <span
              className={
                card.value === 'N/A'
                  ? 'text-text-muted'
                  : card.positive
                    ? 'text-success-text'
                    : 'text-error-text'
              }
            >
              {card.icon}
            </span>
            <span className="text-xs text-text-secondary font-medium">
              <MetricHint label={card.label} />
            </span>
          </div>
          <div
            className={`text-base lg:text-lg font-bold truncate ${
              card.value === 'N/A'
                ? 'text-text-muted'
                : card.positive
                  ? 'text-text-primary'
                  : 'text-error-text'
            }`}
            title={card.value}
          >
            {card.value}
          </div>
        </div>
      ))}
    </div>
  );
}

// --- Chart Card Wrapper ---

function ChartCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-surface-card rounded-lg border border-border-default p-6 overflow-hidden">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-text-muted">{icon}</span>
        <h3 className="text-sm font-semibold text-text-primary">{title}</h3>
      </div>
      {children}
    </div>
  );
}

// --- Cash Flow Diverging Chart ---

function CashFlowChart({
  data,
  isDark,
  currency,
}: {
  data: CashFlowChartData;
  tooltipStyle: React.CSSProperties;
  isDark: boolean;
  currency: string;
}) {
  const chartData = useMemo(
    () =>
      data.months.map((d) => ({
        ...d,
        negExpenses: -d.expenses,
        negMortgage: -d.mortgage,
      })),
    [data.months]
  );

  const barSize = useMemo(() => {
    const count = data.months.length;
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
  }, [data.months.length]);

  const tooltipContentStyle = useMemo(
    () => ({
      backgroundColor: isDark ? '#14161f' : '#fff',
      border: `1px solid ${isDark ? '#2a2e3f' : '#e2e6f0'}`,
      borderRadius: '8px',
      fontSize: '12px',
      color: isDark ? '#eef0f6' : '#1a1d2e',
    }),
    [isDark]
  );

  if (!data.months.length) {
    return <EmptyChart message="No transaction data" />;
  }

  return (
    <ResponsiveContainer width="100%" height={320}>
      <ComposedChart data={chartData} barGap={-barSize} barSize={barSize}>
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
          contentStyle={tooltipContentStyle}
          labelFormatter={(label) => formatMonthTick(String(label))}
          formatter={(value, name) => {
            const labels: Record<string, string> = {
              income: 'Income',
              negExpenses: 'Expenses',
              negMortgage: 'Mortgage',
              net: 'Net',
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
        <Legend
          formatter={(value) => {
            const labels: Record<string, string> = {
              income: 'Income',
              negExpenses: 'Expenses',
              negMortgage: 'Mortgage',
              net: 'Net',
            };
            return labels[value] ?? value;
          }}
        />
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
}

// --- Occupancy Timeline + Net Income Trend ---

function OccupancyAndTrendChart({
  occupancy,
  cashFlow,
  tooltipStyle,
  isDark,
  currency,
}: {
  occupancy: OccupancyChartData;
  cashFlow: CashFlowChartData;
  tooltipStyle: React.CSSProperties;
  isDark: boolean;
  currency: string;
}) {
  if (!occupancy.months.length && !cashFlow.months.length) {
    return <EmptyChart message="No contract data" />;
  }

  const vacantColor = isDark ? '#2a2e3f' : '#e2e6f0';

  return (
    <div className="space-y-4">
      {/* Occupancy Timeline */}
      {occupancy.months.length > 0 && (
        <div>
          <p className="text-xs text-text-secondary mb-1.5 font-medium">
            Occupancy
          </p>
          <div className="flex h-6 rounded overflow-hidden">
            {occupancy.months.map((m) => {
              const tenantPct = Number(m.tenantOccupancyPercent);
              const selfPct = Number(m.selfOccupancyPercent);
              const totalPct = tenantPct + selfPct;
              const tenantShare = totalPct > 0 ? tenantPct / totalPct : 0;
              const selfShare = totalPct > 0 ? selfPct / totalPct : 0;
              const tooltipLabel =
                tenantPct > 0 && selfPct > 0
                  ? `${formatMonthTick(m.month)}: Tenant ${tenantPct.toFixed(0)}% · Self ${selfPct.toFixed(0)}%`
                  : tenantPct > 0
                    ? `${formatMonthTick(m.month)}: Tenant occupied ${tenantPct.toFixed(0)}%`
                    : selfPct > 0
                      ? `${formatMonthTick(m.month)}: Self-occupied ${selfPct.toFixed(0)}%`
                      : `${formatMonthTick(m.month)}: Vacant`;
              return (
                <div
                  key={m.month}
                  className="flex-1 min-w-0 flex"
                  title={tooltipLabel}
                  style={{ backgroundColor: vacantColor }}
                >
                  {tenantPct > 0 && (
                    <div
                      style={{
                        width: `${tenantShare * 100}%`,
                        backgroundColor: COLORS.income,
                      }}
                    />
                  )}
                  {selfPct > 0 && (
                    <div
                      style={{
                        width: `${selfShare * 100}%`,
                        backgroundColor: '#6366f1',
                      }}
                    />
                  )}
                </div>
              );
            })}
          </div>
          {/* Labels: show first, last, and evenly spaced ticks */}
          <div className="flex justify-between mt-0.5">
            {(() => {
              const months = occupancy.months;
              const len = months.length;
              if (len <= 12) {
                return months.map((m) => (
                  <span
                    key={m.month}
                    className="text-[10px] text-text-secondary flex-1 text-center"
                  >
                    {formatMonthTick(m.month)}
                  </span>
                ));
              }
              // For large ranges, show ~6 evenly spaced labels
              const tickCount = Math.min(6, len);
              const indices = Array.from({ length: tickCount }, (_, i) =>
                Math.round((i * (len - 1)) / (tickCount - 1))
              );
              return (
                <div className="flex w-full justify-between">
                  {indices.map((idx) => (
                    <span
                      key={months[idx]?.month ?? idx}
                      className="text-[10px] text-text-secondary"
                    >
                      {formatMonthTick(months[idx]?.month ?? '')}
                    </span>
                  ))}
                </div>
              );
            })()}
          </div>
          <div className="flex gap-3 mt-1.5">
            <span className="inline-flex items-center gap-1 text-[10px] text-text-secondary">
              <span
                className="inline-block w-2.5 h-2.5 rounded-sm"
                style={{ backgroundColor: COLORS.income }}
              />
              Tenant
            </span>
            <span className="inline-flex items-center gap-1 text-[10px] text-text-secondary">
              <span
                className="inline-block w-2.5 h-2.5 rounded-sm"
                style={{ backgroundColor: '#6366f1' }}
              />
              Self-occupied
            </span>
            <span className="inline-flex items-center gap-1 text-[10px] text-text-secondary">
              <span
                className="inline-block w-2.5 h-2.5 rounded-sm"
                style={{ backgroundColor: vacantColor }}
              />
              Vacant
            </span>
          </div>
        </div>
      )}

      {/* Net Income Trend */}
      {cashFlow.months.length > 0 && (
        <div>
          <p className="text-xs text-text-secondary mb-1.5 font-medium">
            Net Income Trend
          </p>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart
              data={cashFlow.months}
              margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
              />
              <XAxis
                dataKey="month"
                tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
                tickFormatter={formatMonthTick}
              />
              <YAxis
                tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
                tickFormatter={(v) => formatAxisValue(v, currency)}
              />
              <Tooltip
                contentStyle={tooltipStyle}
                formatter={(value) => [
                  formatCurrency(
                    typeof value === 'number' ? value : Number(value),
                    currency
                  ),
                  'Net Income',
                ]}
              />
              <ReferenceLine
                y={0}
                stroke={isDark ? '#4a4e5f' : '#d0d0d0'}
                strokeDasharray="4 4"
              />
              <Line
                type="monotone"
                dataKey="net"
                name="Net Income"
                stroke={COLORS.net}
                strokeWidth={2}
                dot={{ fill: COLORS.net, r: 3 }}
                activeDot={{ r: 5 }}
                isAnimationActive={false}
              />
              {cashFlow.months.length > 6 && (
                <Brush
                  dataKey="month"
                  height={20}
                  stroke={isDark ? '#3a3f54' : '#c9cfd9'}
                  fill={isDark ? '#14161f' : '#f8f9fc'}
                  tickFormatter={formatMonthTick}
                />
              )}
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

// --- Expense Timeline Stacked Bar Chart ---

function ExpenseTimelineChart({
  data,
  tooltipStyle,
  isDark,
  currency,
}: {
  data: ExpenseBreakdownChartData;
  tooltipStyle: React.CSSProperties;
  isDark: boolean;
  currency: string;
}) {
  const allCategories = data.categories.map((c) => c.category);
  const [enabled, setEnabled] = useState<Set<string>>(
    () => new Set(allCategories)
  );

  const toggle = (cat: string) => {
    setEnabled((prev) => {
      const next = new Set(prev);
      if (next.has(cat)) {
        if (next.size > 1) {
          next.delete(cat);
        }
      } else {
        next.add(cat);
      }
      return next;
    });
  };

  const chartData = useMemo(
    () =>
      (data.timeline ?? []).map((m) => {
        const row: Record<string, string | number> = { month: m.month };
        for (const cat of allCategories) {
          if (enabled.has(cat)) {
            row[cat] = m.categoryAmounts[cat] ?? 0;
          }
        }
        return row;
      }),
    [data.timeline, allCategories, enabled]
  );

  const {
    visibleData,
    refAreaLeft,
    refAreaRight,
    isZoomed,
    handleMouseDown,
    handleMouseMove,
    handleMouseUp,
    resetZoom,
  } = useChartZoom(
    chartData as Array<{ month: string } & Record<string, string | number>>
  );

  if (!data.categories.length) {
    return <EmptyChart message="No expense data" />;
  }

  const enabledCategories = allCategories.filter((c) => enabled.has(c));

  const barCount = chartData.length;

  return (
    <div>
      {/* Stacked bar chart */}
      <div style={{ overflowX: 'auto' }}>
        {isZoomed && (
          <div className="flex justify-end mb-1">
            <button
              onClick={resetZoom}
              className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded transition-colors"
              style={{
                color: isDark ? '#8b90a8' : '#6b7194',
              }}
            >
              <RefreshCw className="h-3 w-3" />
              Reset zoom
            </button>
          </div>
        )}
        <ResponsiveContainer width="100%" height={320}>
          <BarChart
            data={visibleData}
            margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
            barCategoryGap={
              barCount > 18 ? '8%' : barCount > 12 ? '12%' : '20%'
            }
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
          >
            <CartesianGrid
              strokeDasharray="3 3"
              vertical={false}
              stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
            />
            <XAxis
              dataKey="month"
              tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
              tickFormatter={formatMonthTick}
            />
            <YAxis
              tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
              tickFormatter={(v) => formatAxisValue(v, currency)}
            />
            <Tooltip
              contentStyle={tooltipStyle}
              labelFormatter={(label) => formatMonthTick(String(label))}
              formatter={(value, name) => [
                formatCurrency(
                  typeof value === 'number' ? value : Number(value),
                  currency
                ),
                humanizeCategory(String(name ?? '')),
              ]}
            />
            {enabledCategories.map((cat, i) => {
              const colorIndex = allCategories.indexOf(cat);
              const isLast = i === enabledCategories.length - 1;
              return (
                <Bar
                  key={cat}
                  dataKey={cat}
                  name={cat}
                  stackId="expenses"
                  fill={PIE_COLORS[colorIndex % PIE_COLORS.length]}
                  fillOpacity={0.85}
                  isAnimationActive={false}
                  radius={isLast ? [4, 4, 0, 0] : undefined}
                />
              );
            })}
            {refAreaLeft && refAreaRight && (
              <ReferenceArea
                x1={refAreaLeft}
                x2={refAreaRight}
                strokeOpacity={0.3}
                fill={isDark ? 'rgba(92,124,250,0.15)' : 'rgba(92,124,250,0.1)'}
              />
            )}
            {chartData.length > 6 && (
              <Brush
                key={isZoomed ? 'zoomed' : 'full'}
                dataKey="month"
                height={20}
                stroke={isDark ? '#3a3f54' : '#c9cfd9'}
                fill={isDark ? '#14161f' : '#f8f9fc'}
                tickFormatter={formatMonthTick}
              />
            )}
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Clickable legend — styled like Recharts default legend */}
      <div className="flex flex-wrap justify-center gap-x-4 gap-y-1 mt-2">
        {allCategories.map((cat, i) => {
          const color = PIE_COLORS[i % PIE_COLORS.length];
          const active = enabled.has(cat);
          return (
            <button
              key={cat}
              onClick={() => toggle(cat)}
              className="inline-flex items-center gap-1.5 text-xs cursor-pointer"
              style={{
                color: active
                  ? isDark
                    ? '#eef0f6'
                    : '#1a1d2e'
                  : isDark
                    ? '#5c6180'
                    : '#9ca0b8',
              }}
            >
              <span
                className="inline-block w-3 h-3 rounded-sm shrink-0"
                style={{
                  backgroundColor: active
                    ? color
                    : isDark
                      ? '#2a2e3f'
                      : '#e2e6f0',
                }}
              />
              {humanizeCategory(cat)}
            </button>
          );
        })}
      </div>
    </div>
  );
}

// --- Equity Breakdown Card ---

function EquityBreakdownCard({
  data,
  currency,
}: {
  data: EquityChartData;
  currency: string | null | undefined;
}) {
  const cur = currency || '';
  const fmt = (v: number | null | undefined) => formatCurrency(v, cur);

  const { purchasePrice, currentMarketValue, mortgageBalance } = data;

  if (purchasePrice == null && currentMarketValue == null) {
    return (
      <EmptyChart message="Add purchase price and market value to see equity" />
    );
  }

  const equity =
    currentMarketValue != null
      ? currentMarketValue - (mortgageBalance ?? 0)
      : null;

  const maxVal = Math.max(purchasePrice ?? 0, currentMarketValue ?? 0);

  const bars = [
    { label: 'Purchase Price', value: purchasePrice, color: '#6b7194' },
    { label: 'Market Value', value: currentMarketValue, color: '#3B82F6' },
    { label: 'Mortgage', value: mortgageBalance, color: '#8B5CF6' },
    { label: 'Equity', value: equity, color: '#10B981' },
  ];

  return (
    <div className="space-y-4">
      {bars.map((bar) => (
        <div key={bar.label}>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-text-secondary">{bar.label}</span>
            <span className="font-medium text-text-primary">
              {fmt(bar.value)}
            </span>
          </div>
          <div className="h-3 bg-surface-inset rounded-full overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-700"
              style={{
                width:
                  bar.value != null && bar.value > 0 && maxVal > 0
                    ? `${Math.max((bar.value / maxVal) * 100, 2)}%`
                    : '0%',
                backgroundColor: bar.color,
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

// --- Data Completeness Card ---

function DataCompletenessCard({ data }: { data: DashboardDataCompleteness }) {
  const items = [
    { label: 'Purchase price', done: data.hasPurchasePrice },
    { label: 'Market value', done: data.hasMarketValue },
    { label: 'Mortgage info', done: data.hasMortgageInfo },
    { label: 'Operating costs', done: data.hasOperatingCosts },
    { label: 'Contracts', done: data.hasContracts },
    { label: 'Payments', done: data.hasPayments },
    { label: 'Expenses', done: data.hasExpenses },
  ];

  return (
    <div className="bg-warning-bg border border-warning-border rounded-lg p-4">
      <div className="flex items-center gap-2 mb-3">
        <AlertCircle className="h-5 w-5 text-warning-text" />
        <span className="font-semibold text-warning-text text-sm">
          Data Completeness: {data.completenessPercent}%
        </span>
      </div>
      <div className="h-2 bg-warning-border rounded-full mb-3">
        <div
          className="h-full bg-warning-text rounded-full transition-all duration-500"
          style={{ width: `${data.completenessPercent}%` }}
        />
      </div>
      <div className="flex flex-wrap gap-3">
        {items.map((item) => (
          <span
            key={item.label}
            className={`inline-flex items-center gap-1 text-xs ${
              item.done ? 'text-success-text' : 'text-text-muted'
            }`}
          >
            {item.done ? (
              <CheckCircle2 className="h-3.5 w-3.5" />
            ) : (
              <AlertCircle className="h-3.5 w-3.5" />
            )}
            {item.label}
          </span>
        ))}
      </div>
    </div>
  );
}

// --- Empty Chart State ---

// --- Future Trend Chart ---

function FutureTrendChart({
  data,
  tooltipStyle,
  isDark,
  currency,
}: {
  data: FutureTrendData;
  tooltipStyle: React.CSSProperties;
  isDark: boolean;
  currency: string;
}) {
  const chartData = data.months.map((d) => ({
    ...d,
    expectedExpenses: -d.expectedExpenses,
  }));

  return (
    <div>
      <p className="text-xs text-text-secondary mb-2 font-medium">
        Projected income & expenses based on active contracts and operating
        costs
      </p>
      <ResponsiveContainer width="100%" height={280}>
        <AreaChart
          data={chartData}
          margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
        >
          <CartesianGrid
            strokeDasharray="3 3"
            stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
          />
          <XAxis
            dataKey="month"
            tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
            tickFormatter={formatMonthTick}
          />
          <YAxis
            tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
            tickFormatter={(v) => formatAxisValue(v, currency)}
          />
          <Tooltip
            contentStyle={tooltipStyle}
            labelFormatter={(label) => formatMonthTick(String(label))}
            formatter={(value, name) => [
              formatCurrency(
                Math.abs(typeof value === 'number' ? value : Number(value)),
                currency
              ),
              name,
            ]}
          />
          <Legend />
          <Area
            type="monotone"
            dataKey="expectedIncome"
            name="Expected Income"
            stroke="#10B981"
            fill="#10B981"
            fillOpacity={0.15}
            strokeWidth={2}
          />
          <Area
            type="monotone"
            dataKey="expectedExpenses"
            name="Expected Expenses"
            stroke="#EF4444"
            fill="#EF4444"
            fillOpacity={0.15}
            strokeWidth={2}
          />
          <Line
            type="monotone"
            dataKey="expectedNet"
            name="Expected Net"
            stroke="#3B82F6"
            strokeWidth={2}
            strokeDasharray="5 5"
            dot={{ fill: '#3B82F6', r: 3 }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

function EmptyChart({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center h-[300px] text-text-muted">
      <BarChart3 className="h-10 w-10 mb-2 opacity-40" />
      <span className="text-sm">{message}</span>
    </div>
  );
}

// --- Helpers ---

function humanizeCategory(val: string): string {
  return val
    .replace(/_/g, '')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
