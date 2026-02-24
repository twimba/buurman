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
} from 'recharts';
import type { BarShapeProps } from 'recharts';
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
} from '@/api/properties';
import { LoadingSpinner } from '@/components/LoadingSpinner';
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

function formatCurrency(value: number | null, currencyCode: string): string {
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

  const [exporting, setExporting] = useState<'pdf' | 'csv' | null>(null);

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

  const handleExport = async (format: 'pdf' | 'csv') => {
    setExporting(format);
    try {
      const blob =
        format === 'pdf'
          ? await exportPropertyDashboardPDF(propertyId, months)
          : await exportPropertyDashboardCSV(propertyId, months);
      const mimeType = format === 'pdf' ? 'application/pdf' : 'text/csv';
      const file = new Blob([blob], { type: mimeType });
      const url = window.URL.createObjectURL(file);
      const link = document.createElement('a');
      link.href = url;
      link.download = `property-dashboard-${propertyId}.${format}`;
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
      <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg text-sm">
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
          <Calendar className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8] shrink-0" />
          <div className="flex gap-1 flex-wrap">
            {PERIOD_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => handlePeriodChange(opt.value)}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                  periodType === opt.value
                    ? 'bg-[#5c7cfa] text-white'
                    : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
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
                className="px-2 py-1.5 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md text-xs focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              />
              <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                to
              </span>
              <input
                type="date"
                value={customEndDate}
                onChange={(e) => {
                  setCustomEndDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-2 py-1.5 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md text-xs focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              />
            </div>
          )}
        </div>
        <div className="flex gap-2 shrink-0">
          <button
            onClick={() => handleExport('csv')}
            disabled={exporting !== null}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-[#e2e6f0] dark:border-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f5f7fa] dark:hover:bg-[#1e2130] transition-colors disabled:opacity-50"
          >
            <Download className="h-3.5 w-3.5" />
            {exporting === 'csv' ? 'Exporting...' : 'CSV'}
          </button>
          <button
            onClick={() => handleExport('pdf')}
            disabled={exporting !== null}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-[#e2e6f0] dark:border-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f5f7fa] dark:hover:bg-[#1e2130] transition-colors disabled:opacity-50"
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
  const fmtMoney = (val: number | null) => formatCurrency(val, cur);
  const fmtPct = (val: number | null) =>
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
          className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4"
        >
          <div className="flex items-center gap-2 mb-2">
            <span
              className={
                card.value === 'N/A'
                  ? 'text-[#9ca0b8]'
                  : card.positive
                    ? 'text-emerald-500'
                    : 'text-red-500'
              }
            >
              {card.icon}
            </span>
            <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] font-medium">
              <MetricHint label={card.label} />
            </span>
          </div>
          <div
            className={`text-base lg:text-lg font-bold truncate ${
              card.value === 'N/A'
                ? 'text-[#9ca0b8] dark:text-[#5c6180]'
                : card.positive
                  ? 'text-[#1a1d2e] dark:text-[#eef0f6]'
                  : 'text-red-600 dark:text-red-400'
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
    <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6 overflow-hidden">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-[#9ca0b8] dark:text-[#5c6180]">{icon}</span>
        <h3 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          {title}
        </h3>
      </div>
      {children}
    </div>
  );
}

// --- Custom bar shape for outflow stack rounding ---

/**
 * Creates a Recharts shape function for outflow stack bars.
 * Rounds the bottom corners of the outermost bar (furthest from zero),
 * matching the income bar's rounded top corners for visual symmetry.
 *
 * For the outflow stack (expenses + mortgage stacked below zero):
 * - If this bar is "mortgage" and its value is non-zero -> round bottom corners
 * - If this bar is "expenses" and mortgage is 0 -> round bottom corners (it's the outermost)
 * - Otherwise -> sharp corners (this bar is interior to the stack)
 */
function makeOutflowShape(dataKey: 'expenses' | 'mortgage') {
  function OutflowBar(props: BarShapeProps): React.ReactElement | null {
    const {
      x = 0,
      y = 0,
      width = 0,
      height = 0,
      fill,
      fillOpacity,
      payload,
    } = props;
    if (height === 0 || width === 0) {
      return null;
    }

    // Normalize: Recharts passes negative height for below-zero bars.
    // SVG <rect> doesn't render with negative height, so always normalize.
    const absH = Math.abs(height);
    const top = height >= 0 ? y : y + height;
    const bottom = top + absH;

    const r = 4;
    const isOutermost =
      dataKey === 'mortgage'
        ? (payload?.mortgage ?? 0) !== 0
        : (payload?.mortgage ?? 0) === 0;

    if (!isOutermost) {
      return (
        <rect
          x={x}
          y={top}
          width={width}
          height={absH}
          fill={fill}
          fillOpacity={fillOpacity}
        />
      );
    }

    // Rounded bottom corners only (outermost edge away from zero)
    const clampedR = Math.min(r, width / 2, absH);
    const d = [
      `M ${x},${top}`,
      `L ${x + width},${top}`,
      `L ${x + width},${bottom - clampedR}`,
      `Q ${x + width},${bottom} ${x + width - clampedR},${bottom}`,
      `L ${x + clampedR},${bottom}`,
      `Q ${x},${bottom} ${x},${bottom - clampedR}`,
      `Z`,
    ].join(' ');

    return <path d={d} fill={fill} fillOpacity={fillOpacity} />;
  }
  OutflowBar.displayName = `OutflowBar(${dataKey})`;
  return OutflowBar;
}

const expensesBarShape = makeOutflowShape('expenses');
const mortgageBarShape = makeOutflowShape('mortgage');

// --- Cash Flow Diverging Chart ---

function CashFlowTooltip({
  active,
  payload,
  label,
  isDark,
  currency,
}: {
  active?: boolean;
  payload?: Array<{ dataKey?: string; value?: number }>;
  label?: string;
  isDark: boolean;
  currency: string;
}) {
  if (!active || !payload?.length) {
    return null;
  }
  const get = (key: string) =>
    payload.find((p) => p.dataKey === key)?.value ?? 0;
  const income = get('income') as number;
  const expenses = get('expenses') as number;
  const mortgage = get('mortgage') as number;
  const net = get('net') as number;

  const fmt = (v: number) => formatCurrency(Math.abs(v), currency);
  const fmtSigned = (v: number) => {
    const prefix = v >= 0 ? '+' : '-';
    return `${prefix}${formatCurrency(Math.abs(v), currency).replace(/^-/, '')}`;
  };

  return (
    <div
      className="rounded-lg px-3 py-2.5 text-xs shadow-lg border"
      style={{
        backgroundColor: isDark ? '#14161f' : '#fff',
        borderColor: isDark ? '#2a2e3f' : '#e2e6f0',
        color: isDark ? '#eef0f6' : '#1a1d2e',
      }}
    >
      <p className="font-semibold mb-1.5">{formatMonthTick(label as string)}</p>
      <div className="space-y-0.5">
        <div className="flex justify-between gap-4">
          <span style={{ color: COLORS.income }}>Income</span>
          <span className="font-medium">{fmt(income)}</span>
        </div>
        <div className="flex justify-between gap-4">
          <span style={{ color: COLORS.expenses }}>Expenses</span>
          <span className="font-medium">{fmt(expenses)}</span>
        </div>
        <div className="flex justify-between gap-4">
          <span style={{ color: COLORS.mortgage }}>Mortgage</span>
          <span className="font-medium">{fmt(mortgage)}</span>
        </div>
        <div
          className="flex justify-between gap-4 border-t pt-1 mt-1 font-bold"
          style={{ borderColor: isDark ? '#2a2e3f' : '#e2e6f0' }}
        >
          <span style={{ color: COLORS.net }}>Net</span>
          <span style={{ color: net >= 0 ? COLORS.income : COLORS.expenses }}>
            {fmtSigned(net)}
          </span>
        </div>
      </div>
    </div>
  );
}

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
        month: d.month,
        income: d.income,
        expenses: -Math.abs(d.expenses),
        mortgage: -Math.abs(d.mortgage),
        net: d.net,
      })),
    [data.months]
  );

  if (!data.months.length) {
    return <EmptyChart message="No transaction data" />;
  }

  return (
    <ResponsiveContainer width="100%" height={380}>
      <ComposedChart
        data={chartData}
        margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
        barCategoryGap="20%"
        barGap={2}
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
        <ReferenceLine
          y={0}
          stroke={isDark ? '#5c6180' : '#9ca0b8'}
          strokeWidth={1.5}
        />
        <Tooltip
          content={<CashFlowTooltip isDark={isDark} currency={currency} />}
          cursor={{
            fill: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)',
          }}
        />
        <Legend wrapperStyle={{ fontSize: '12px' }} />
        <Bar
          dataKey="income"
          name="Income"
          stackId="inflow"
          fill={COLORS.income}
          fillOpacity={0.85}
          radius={[4, 4, 0, 0]}
          isAnimationActive={false}
        />
        <Bar
          dataKey="expenses"
          name="Expenses"
          stackId="outflow"
          fill={COLORS.expenses}
          fillOpacity={0.85}
          shape={expensesBarShape}
          isAnimationActive={false}
        />
        <Bar
          dataKey="mortgage"
          name="Mortgage"
          stackId="outflow"
          fill={COLORS.mortgage}
          fillOpacity={0.85}
          shape={mortgageBarShape}
          isAnimationActive={false}
        />
        {chartData.length > 6 && (
          <Brush
            dataKey="month"
            height={20}
            stroke={isDark ? '#3a3f54' : '#c9cfd9'}
            fill={isDark ? '#14161f' : '#f8f9fc'}
            tickFormatter={formatMonthTick}
          />
        )}
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
          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-1.5 font-medium">
            Occupancy
          </p>
          <div className="flex h-6 rounded overflow-hidden">
            {occupancy.months.map((m) => {
              const occupied = m.occupancyPercent > 0;
              return (
                <div
                  key={m.month}
                  className="flex-1 min-w-0"
                  style={{
                    backgroundColor: occupied ? COLORS.income : vacantColor,
                  }}
                  title={`${formatMonthTick(m.month)}: ${occupied ? 'Occupied' : 'Vacant'}`}
                />
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
                    className="text-[10px] text-[#6b7194] dark:text-[#8b90a8] flex-1 text-center"
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
                      key={months[idx].month}
                      className="text-[10px] text-[#6b7194] dark:text-[#8b90a8]"
                    >
                      {formatMonthTick(months[idx].month)}
                    </span>
                  ))}
                </div>
              );
            })()}
          </div>
          <div className="flex gap-3 mt-1.5">
            <span className="inline-flex items-center gap-1 text-[10px] text-[#6b7194] dark:text-[#8b90a8]">
              <span
                className="inline-block w-2.5 h-2.5 rounded-sm"
                style={{ backgroundColor: COLORS.income }}
              />
              Occupied
            </span>
            <span className="inline-flex items-center gap-1 text-[10px] text-[#6b7194] dark:text-[#8b90a8]">
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
          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-1.5 font-medium">
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
                formatter={(value?: number | string) => [
                  formatCurrency(Number(value ?? 0), currency),
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

  if (!data.categories.length) {
    return <EmptyChart message="No expense data" />;
  }

  const enabledCategories = allCategories.filter((c) => enabled.has(c));

  const barCount = chartData.length;

  return (
    <div>
      {/* Stacked bar chart */}
      <div style={{ overflowX: 'auto' }}>
        <ResponsiveContainer width="100%" height={320}>
          <BarChart
            data={chartData}
            margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
            barCategoryGap={
              barCount > 18 ? '8%' : barCount > 12 ? '12%' : '20%'
            }
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
              formatter={(value?: number | string, name?: string) => [
                formatCurrency(Number(value ?? 0), currency),
                humanizeCategory(name ?? ''),
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
            {chartData.length > 6 && (
              <Brush
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
  currency: string | null;
}) {
  const cur = currency || '';
  const fmt = (v: number | null) => formatCurrency(v, cur);

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
            <span className="text-[#6b7194] dark:text-[#8b90a8]">
              {bar.label}
            </span>
            <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {fmt(bar.value)}
            </span>
          </div>
          <div className="h-3 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-full overflow-hidden">
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
    <div className="bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/40 rounded-lg p-4">
      <div className="flex items-center gap-2 mb-3">
        <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400" />
        <span className="font-semibold text-amber-800 dark:text-amber-300 text-sm">
          Data Completeness: {data.completenessPercent}%
        </span>
      </div>
      <div className="h-2 bg-amber-200 dark:bg-amber-900/40 rounded-full mb-3">
        <div
          className="h-full bg-amber-500 rounded-full transition-all duration-500"
          style={{ width: `${data.completenessPercent}%` }}
        />
      </div>
      <div className="flex flex-wrap gap-3">
        {items.map((item) => (
          <span
            key={item.label}
            className={`inline-flex items-center gap-1 text-xs ${
              item.done
                ? 'text-emerald-700 dark:text-emerald-400'
                : 'text-[#9ca0b8] dark:text-[#5c6180]'
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
  return (
    <div>
      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-2 font-medium">
        Projected income & expenses based on active contracts and operating
        costs
      </p>
      <ResponsiveContainer width="100%" height={280}>
        <AreaChart
          data={data.months}
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
            formatter={(value?: number | string, name?: string) => [
              formatCurrency(Number(value ?? 0), currency),
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
    <div className="flex flex-col items-center justify-center h-[300px] text-[#9ca0b8] dark:text-[#5c6180]">
      <BarChart3 className="h-10 w-10 mb-2 opacity-40" />
      <span className="text-sm">{message}</span>
    </div>
  );
}

// --- Helpers ---

function humanizeCategory(val: string): string {
  return val
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
