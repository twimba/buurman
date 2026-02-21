import { useMemo } from 'react';
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  Legend,
} from 'recharts';
import {
  TrendingUp,
  DollarSign,
  Home,
  Percent,
  BarChart3,
  AlertCircle,
  CheckCircle2,
} from 'lucide-react';
import { useTheme } from '@/context/ThemeContext';
import { usePropertyDashboard } from '@/hooks/usePropertyHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import type {
  DashboardSummaryMetrics,
  CashFlowChartData,
  EquityChartData,
  ExpenseBreakdownChartData,
  OccupancyChartData,
  DashboardDataCompleteness,
} from '@/types/property';

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

interface PropertyDashboardTabProps {
  propertyId: string;
}

export const PropertyDashboardTab = ({
  propertyId,
}: PropertyDashboardTabProps) => {
  const { data: dashboard, isLoading, error } = usePropertyDashboard(propertyId);
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';

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

  if (isLoading) return <LoadingSpinner />;
  if (error)
    return (
      <div className="text-center py-12 text-red-500">
        Failed to load dashboard data
      </div>
    );
  if (!dashboard) return null;

  const { summary, cashFlow, equity, expenseBreakdown, occupancy, dataCompleteness } =
    dashboard;

  return (
    <div className="space-y-6">
      {/* Data Completeness Banner */}
      {dataCompleteness.completenessPercent < 100 && (
        <DataCompletenessCard data={dataCompleteness} />
      )}

      {/* Summary Metrics */}
      <SummaryCards metrics={summary} />

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard title="Monthly Cash Flow" icon={<BarChart3 className="h-5 w-5" />}>
          <CashFlowChart data={cashFlow} tooltipStyle={tooltipStyle} isDark={isDark} />
        </ChartCard>

        <ChartCard title="Occupancy Rate" icon={<Home className="h-5 w-5" />}>
          <OccupancyChart data={occupancy} tooltipStyle={tooltipStyle} isDark={isDark} />
        </ChartCard>

        <ChartCard title="Expense Breakdown" icon={<DollarSign className="h-5 w-5" />}>
          <ExpensePieChart data={expenseBreakdown} tooltipStyle={tooltipStyle} />
        </ChartCard>

        <ChartCard title="Equity Overview" icon={<TrendingUp className="h-5 w-5" />}>
          <EquityBreakdownCard data={equity} currency={summary.currency} />
        </ChartCard>
      </div>
    </div>
  );
};

// --- Summary Cards ---

function SummaryCards({ metrics }: { metrics: DashboardSummaryMetrics }) {
  const currency = metrics.currency || 'EUR';
  const fmt = (val: number | null, prefix = '') =>
    val != null ? `${prefix}${val.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : 'N/A';
  const fmtPct = (val: number | null) =>
    val != null ? `${val >= 0 ? '+' : ''}${val.toFixed(2)}%` : 'N/A';

  const cards = [
    { label: 'Total ROI', value: fmtPct(metrics.totalRoiPercent), icon: <TrendingUp className="h-5 w-5" />, positive: (metrics.totalRoiPercent ?? 0) >= 0 },
    { label: 'Annualized ROI', value: fmtPct(metrics.annualizedRoiPercent), icon: <Percent className="h-5 w-5" />, positive: (metrics.annualizedRoiPercent ?? 0) >= 0 },
    { label: 'Cap Rate', value: fmtPct(metrics.capRatePercent), icon: <Percent className="h-5 w-5" />, positive: (metrics.capRatePercent ?? 0) >= 0 },
    { label: 'Cash-on-Cash', value: fmtPct(metrics.cashOnCashPercent), icon: <Percent className="h-5 w-5" />, positive: (metrics.cashOnCashPercent ?? 0) >= 0 },
    { label: 'Monthly Cash Flow', value: fmt(metrics.monthlyCashFlow, `${currency} `), icon: <DollarSign className="h-5 w-5" />, positive: (metrics.monthlyCashFlow ?? 0) >= 0 },
    { label: 'Annual NOI', value: fmt(metrics.annualNoi, `${currency} `), icon: <DollarSign className="h-5 w-5" />, positive: (metrics.annualNoi ?? 0) >= 0 },
    { label: 'Total Equity', value: fmt(metrics.totalEquity, `${currency} `), icon: <Home className="h-5 w-5" />, positive: (metrics.totalEquity ?? 0) >= 0 },
    { label: 'Equity Growth', value: fmtPct(metrics.equityGrowthPercent), icon: <TrendingUp className="h-5 w-5" />, positive: (metrics.equityGrowthPercent ?? 0) >= 0 },
    { label: 'Occupancy', value: metrics.occupancyRatePercent != null ? `${metrics.occupancyRatePercent.toFixed(1)}%` : 'N/A', icon: <Home className="h-5 w-5" />, positive: (metrics.occupancyRatePercent ?? 0) >= 50 },
    { label: 'Gross Rent Multiplier', value: metrics.grossRentMultiplier != null ? `${metrics.grossRentMultiplier.toFixed(1)}x` : 'N/A', icon: <BarChart3 className="h-5 w-5" />, positive: true },
  ];

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
      {cards.map((card) => (
        <div
          key={card.label}
          className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4"
        >
          <div className="flex items-center gap-2 mb-2">
            <span className={card.value === 'N/A' ? 'text-[#9ca0b8]' : card.positive ? 'text-emerald-500' : 'text-red-500'}>
              {card.icon}
            </span>
            <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] font-medium">
              {card.label}
            </span>
          </div>
          <div
            className={`text-lg font-bold ${
              card.value === 'N/A'
                ? 'text-[#9ca0b8] dark:text-[#5c6180]'
                : card.positive
                  ? 'text-[#1a1d2e] dark:text-[#eef0f6]'
                  : 'text-red-600 dark:text-red-400'
            }`}
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
    <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
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

// --- Cash Flow Bar Chart ---

function CashFlowChart({
  data,
  tooltipStyle,
  isDark,
}: {
  data: CashFlowChartData;
  tooltipStyle: React.CSSProperties;
  isDark: boolean;
}) {
  if (!data.months.length) return <EmptyChart message="No transaction data" />;

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data.months} margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#2a2e3f' : '#f0f0f0'} />
        <XAxis
          dataKey="month"
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
          tickFormatter={(v) => {
            const [, m] = v.split('-');
            const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
            return months[parseInt(m, 10) - 1] || v;
          }}
        />
        <YAxis tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }} />
        <Tooltip contentStyle={tooltipStyle} />
        <Legend wrapperStyle={{ fontSize: '12px' }} />
        <Bar dataKey="income" name="Income" fill={COLORS.income} radius={[2, 2, 0, 0]} />
        <Bar dataKey="expenses" name="Expenses" fill={COLORS.expenses} radius={[2, 2, 0, 0]} />
        <Bar dataKey="mortgage" name="Mortgage" fill={COLORS.mortgage} radius={[2, 2, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

// --- Occupancy Area Chart ---

function OccupancyChart({
  data,
  tooltipStyle,
  isDark,
}: {
  data: OccupancyChartData;
  tooltipStyle: React.CSSProperties;
  isDark: boolean;
}) {
  if (!data.months.length) return <EmptyChart message="No contract data" />;

  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={data.months} margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
        <defs>
          <linearGradient id="occupancyGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={COLORS.occupancy} stopOpacity={0.3} />
            <stop offset="95%" stopColor={COLORS.occupancy} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#2a2e3f' : '#f0f0f0'} />
        <XAxis
          dataKey="month"
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
          tickFormatter={(v) => {
            const [, m] = v.split('-');
            const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
            return months[parseInt(m, 10) - 1] || v;
          }}
        />
        <YAxis
          domain={[0, 100]}
          tick={{ fontSize: 11, fill: isDark ? '#8b90a8' : '#6b7194' }}
          tickFormatter={(v) => `${v}%`}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          formatter={(value?: number | string) => [`${Number(value ?? 0).toFixed(1)}%`, 'Occupancy']}
        />
        <Area
          type="monotone"
          dataKey="occupancyPercent"
          stroke={COLORS.occupancy}
          strokeWidth={2}
          fill="url(#occupancyGradient)"
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

// --- Expense Pie Chart ---

function ExpensePieChart({
  data,
  tooltipStyle,
}: {
  data: ExpenseBreakdownChartData;
  tooltipStyle: React.CSSProperties;
}) {
  if (!data.categories.length) return <EmptyChart message="No expense data" />;

  const total = data.categories.reduce((sum, c) => sum + c.amount, 0);

  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie
          data={data.categories}
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={100}
          paddingAngle={2}
          dataKey="amount"
          nameKey="category"
          label={({ name, value }) => {
            const pct = ((Number(value) / total) * 100).toFixed(0);
            return `${humanizeCategory(String(name))} ${pct}%`;
          }}
          labelLine={false}
        >
          {data.categories.map((_, index) => (
            <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={tooltipStyle}
          formatter={(value?: number | string, name?: string) => [
            Number(value ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 }),
            humanizeCategory(name ?? ''),
          ]}
        />
      </PieChart>
    </ResponsiveContainer>
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
  const cur = currency || 'EUR';
  const fmt = (v: number | null) =>
    v != null ? `${cur} ${v.toLocaleString(undefined, { minimumFractionDigits: 2 })}` : 'N/A';

  const { purchasePrice, currentMarketValue, mortgageBalance } = data;

  if (purchasePrice == null && currentMarketValue == null) {
    return <EmptyChart message="Add purchase price and market value to see equity" />;
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
            <span className="text-[#6b7194] dark:text-[#8b90a8]">{bar.label}</span>
            <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {fmt(bar.value)}
            </span>
          </div>
          <div className="h-3 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-full overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-700"
              style={{
                width: bar.value != null && maxVal > 0
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
