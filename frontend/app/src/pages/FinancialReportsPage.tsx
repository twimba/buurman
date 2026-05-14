import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Calendar,
  Building2,
  BarChart3,
  List,
} from 'lucide-react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import {
  useDataDateRange,
  useFinancialOverview,
  useIncomeTrend,
  useExpenseBreakdown,
  usePropertyComparison,
  useOccupancyTrend,
} from '@/hooks/useReportHooks';
import { LoadingSpinner } from '@buurman/ui';
import { MetricHint } from '@/components/common/MetricHint';
import { useTheme } from '@/context/ThemeContext';
import { useQuery } from '@tanstack/react-query';
import { getProperties } from '@/api/properties';
import {
  exportTransactionsCSV,
  exportTransactionsPDF,
  exportTransactionsExcel,
} from '@/api/reports';
import { ChevronDown, ChevronUp, Filter, Check, FileText } from 'lucide-react';
import { ExportDropdown } from '@/components/common/ExportDropdown';

export const FinancialReportsPage = () => {
  const { t } = useTranslation('admin');
  const navigate = useNavigate();
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';
  const tooltipStyle = {
    backgroundColor: isDark ? '#14161f' : '#fff',
    border: `1px solid ${isDark ? '#2a2e3f' : '#e2e6f0'}`,
    borderRadius: '8px',
    fontSize: '12px',
    color: isDark ? '#eef0f6' : '#1a1d2e',
  };
  const [expenseChartType, setExpenseChartType] = useState<'pie' | 'bar'>(
    'pie'
  );
  const [incomeChartType, setIncomeChartType] = useState<'line' | 'bar'>(
    'line'
  );
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [selectedPropertyIds, setSelectedPropertyIds] = useState<string[]>([]);
  const { data: dateRangeData } = useDataDateRange();

  const { data: propertiesData } = useQuery({
    queryKey: ['properties'],
    queryFn: () => getProperties(),
  });
  const allProperties = propertiesData?.content ?? [];

  const toggleProperty = (id: string) => {
    setSelectedPropertyIds((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]
    );
  };

  const [periodType, setPeriodType] = useState<
    'month' | 'quarter' | 'year' | 'all' | 'custom'
  >('month');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  // Earliest data date from backend (for "All Time" period)
  const earliestDataDate = dateRangeData?.earliestDate ?? null;

  // Calculate date range based on period type
  const dateRange = useMemo(() => {
    const today = new Date();
    let startDate: Date = today;
    let endDate = today;

    switch (periodType) {
      case 'month':
        startDate = new Date(today.getFullYear(), today.getMonth(), 1);
        break;
      case 'quarter': {
        const quarter = Math.floor(today.getMonth() / 3);
        startDate = new Date(today.getFullYear(), quarter * 3, 1);
        break;
      }
      case 'year':
        startDate = new Date(today.getFullYear(), 0, 1);
        break;
      case 'all':
        startDate = earliestDataDate
          ? new Date(earliestDataDate + 'T00:00:00')
          : new Date(today.getFullYear(), today.getMonth(), 1);
        break;
      case 'custom':
        if (customStartDate && customEndDate) {
          startDate = new Date(customStartDate);
          endDate = new Date(customEndDate);
        } else {
          startDate = new Date(today.getFullYear(), today.getMonth(), 1);
        }
        break;
    }

    return {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0],
    };
  }, [periodType, customStartDate, customEndDate, earliestDataDate]);

  // Pre-fill custom date fields when switching to a predefined period
  const handlePeriodChange = (type: typeof periodType) => {
    setPeriodType(type);
    if (type !== 'custom') {
      const today = new Date();
      let start: Date;
      const end = today;

      switch (type) {
        case 'month':
          start = new Date(today.getFullYear(), today.getMonth(), 1);
          break;
        case 'quarter': {
          const quarter = Math.floor(today.getMonth() / 3);
          start = new Date(today.getFullYear(), quarter * 3, 1);
          break;
        }
        case 'year':
          start = new Date(today.getFullYear(), 0, 1);
          break;
        case 'all':
          start = earliestDataDate
            ? new Date(earliestDataDate + 'T00:00:00')
            : new Date(today.getFullYear(), today.getMonth(), 1);
          break;
        default:
          start = new Date(today.getFullYear(), today.getMonth(), 1);
      }

      setCustomStartDate(start.toISOString().split('T')[0]);
      setCustomEndDate(end.toISOString().split('T')[0]);
    }
  };

  const {
    data: overview,
    isLoading: overviewLoading,
    error: overviewError,
  } = useFinancialOverview(
    dateRange.startDate,
    dateRange.endDate,
    selectedPropertyIds.length > 0 ? selectedPropertyIds : undefined,
    undefined,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: incomeTrend,
    isLoading: trendLoading,
    error: trendError,
  } = useIncomeTrend(
    dateRange.startDate,
    dateRange.endDate,
    selectedPropertyIds.length > 0 ? selectedPropertyIds : undefined,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: expenseBreakdown,
    isLoading: breakdownLoading,
    error: breakdownError,
  } = useExpenseBreakdown(
    dateRange.startDate,
    dateRange.endDate,
    selectedPropertyIds.length > 0 ? selectedPropertyIds : undefined,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: propertyComparison,
    isLoading: comparisonLoading,
    error: comparisonError,
  } = usePropertyComparison(
    dateRange.startDate,
    dateRange.endDate,
    selectedPropertyIds.length > 0 ? selectedPropertyIds : undefined,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: occupancyTrend,
    isLoading: occupancyLoading,
    error: occupancyError,
  } = useOccupancyTrend(
    dateRange.startDate,
    dateRange.endDate,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const formatCurrency = (value: number) => {
    const cur = overview?.currency;
    if (!cur) {
      return value.toLocaleString('nl-NL', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    }
    return new Intl.NumberFormat('nl-NL', {
      style: 'currency',
      currency: cur,
    }).format(value);
  };

  const formatYAxis = (value: number) => {
    const cur = overview?.currency;
    if (!cur) {
      return new Intl.NumberFormat('nl-NL', {
        notation: 'compact',
        maximumFractionDigits: 1,
      }).format(value);
    }
    return new Intl.NumberFormat('nl-NL', {
      style: 'currency',
      currency: cur,
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  };

  const formatPercent = (value: number) => {
    return `${value.toFixed(1)}%`;
  };

  if (
    overviewError ||
    trendError ||
    breakdownError ||
    comparisonError ||
    occupancyError
  ) {
    return (
      <div className="px-4 py-8">
        <div className="bg-error-bg border border-error-border text-error-text px-4 py-3 rounded">
          {t('reports.failedToLoad')}
        </div>
      </div>
    );
  }

  return (
    <div className="px-4 py-8">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <BarChart3 className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('reports.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">{t('reports.subtitle')}</p>
        </div>
        <div className="flex items-center gap-2">
          <ExportDropdown
            size="md"
            options={[
              {
                label: 'CSV',
                onExport: async () => {
                  try {
                    const blob = await exportTransactionsCSV(
                      dateRange.startDate,
                      dateRange.endDate
                    );
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'transactions.csv';
                    a.click();
                    URL.revokeObjectURL(url);
                  } catch {
                    /* ignore */
                  }
                },
              },
              {
                label: 'Excel',
                onExport: async () => {
                  try {
                    const blob = await exportTransactionsExcel(
                      dateRange.startDate,
                      dateRange.endDate
                    );
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'transactions.xlsx';
                    a.click();
                    URL.revokeObjectURL(url);
                  } catch {
                    /* ignore */
                  }
                },
              },
            ]}
          />
          <button
            onClick={async () => {
              try {
                const blob = await exportTransactionsPDF(
                  dateRange.startDate,
                  dateRange.endDate
                );
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'financial-report.pdf';
                a.click();
                URL.revokeObjectURL(url);
              } catch {
                /* ignore */
              }
            }}
            className="flex items-center gap-2 px-3 py-2 text-text-secondary border border-border-strong rounded-md hover:bg-surface-inset transition-colors text-sm"
            title={t('reports.downloadPdf')}
          >
            <FileText className="h-4 w-4" />
            PDF
          </button>
          <button
            onClick={() => navigate('/reports/transactions')}
            className="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-md hover:bg-primary-600 transition-colors text-sm"
          >
            <List className="h-5 w-5" />
            {t('reports.transactions')}
          </button>
        </div>
      </div>

      {/* Period Selector */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 mb-6">
        <div className="flex flex-col gap-4">
          <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-text-secondary " />
              <span className="font-medium text-text-primary">
                {t('reports.period')}
              </span>
              <div className="flex gap-2 flex-wrap">
                {(
                  [
                    { key: 'month', label: t('reports.month') },
                    { key: 'quarter', label: t('reports.quarter') },
                    { key: 'year', label: t('reports.year') },
                    { key: 'all', label: t('reports.allTime') },
                    { key: 'custom', label: t('reports.custom') },
                  ] as const
                ).map(({ key, label }) => (
                  <button
                    key={key}
                    onClick={() => handlePeriodChange(key as typeof periodType)}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                      periodType === key
                        ? 'bg-primary-500 text-white'
                        : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex gap-3">
              <input
                type="date"
                value={customStartDate || dateRange.startDate}
                onChange={(e) => {
                  setCustomStartDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-3 py-2 border border-border-strong rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-surface-card text-text-primary"
              />
              <span className="text-text-secondary self-center">
                {t('reports.to')}
              </span>
              <input
                type="date"
                value={customEndDate || dateRange.endDate}
                onChange={(e) => {
                  setCustomEndDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-3 py-2 border border-border-strong rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-surface-card text-text-primary"
              />
            </div>
          </div>

          {/* Advanced Filters Toggle */}
          <div className="border-t border-border-default pt-3 mt-3">
            <button
              onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
              className="flex items-center gap-2 text-sm font-medium text-text-secondary hover:text-text-secondary transition-colors"
            >
              <Filter className="h-4 w-4" />
              {t('reports.advancedFilters')}
              {selectedPropertyIds.length > 0 && (
                <span className="bg-primary-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                  {selectedPropertyIds.length}
                </span>
              )}
              {showAdvancedFilters ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </button>

            {showAdvancedFilters && (
              <div className="mt-3 space-y-3">
                {/* Property Filter */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label className="text-xs font-medium text-text-secondary">
                      {t('reports.properties')}
                    </label>
                    {selectedPropertyIds.length > 0 && (
                      <button
                        onClick={() => setSelectedPropertyIds([])}
                        className="text-xs text-primary-500 hover:underline"
                      >
                        {t('reports.clearAll')}
                      </button>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {allProperties.map((p) => {
                      const isSelected = selectedPropertyIds.includes(
                        p.identifier
                      );
                      return (
                        <button
                          key={p.identifier}
                          onClick={() => toggleProperty(p.identifier)}
                          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium border transition-colors ${
                            isSelected
                              ? 'bg-primary-500/10 border-primary-500 text-primary-500 dark:bg-primary-500/20 dark:text-primary-400'
                              : 'border-border-strong text-text-secondary hover:border-primary-500/50'
                          }`}
                        >
                          {isSelected && <Check className="h-3 w-3" />}
                          {p.street}, {p.city}
                        </button>
                      );
                    })}
                    {allProperties.length === 0 && (
                      <span className="text-xs text-text-secondary">
                        {t('reports.noProperties')}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Summary Cards */}
      {overviewLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      ) : overview ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Total Income */}
          <div className="bg-success-bg rounded-lg shadow-sm p-6 border border-success-border">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-success-text rounded-lg">
                <TrendingUp className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p className="text-sm text-success-text font-medium">
                  {t('reports.totalIncome')}
                </p>
                <p className="text-3xl font-bold text-success-text">
                  {formatCurrency(overview.income.total)}
                </p>
              </div>
            </div>
          </div>

          {/* Total Expenses */}
          <div className="bg-error-bg rounded-lg shadow-sm p-6 border border-error-border">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-error-text rounded-lg">
                <TrendingDown className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p className="text-sm text-error-text font-medium">
                  {t('reports.totalExpenses')}
                </p>
                <p className="text-3xl font-bold text-error-text">
                  {formatCurrency(overview.expenses.total)}
                </p>
              </div>
            </div>
          </div>

          {/* Net Profit */}
          <div
            className={`bg-gradient-to-br ${
              overview.netProfit >= 0
                ? 'bg-info-bg border-info-border'
                : 'bg-warning-bg border-warning-border'
            } rounded-lg shadow-sm p-6 border`}
          >
            <div className="flex items-center justify-between mb-4">
              <div
                className={`p-3 ${
                  overview.netProfit >= 0 ? 'bg-info-text' : 'bg-warning-text'
                } rounded-lg`}
              >
                <DollarSign className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p
                  className={`text-sm font-medium ${
                    overview.netProfit >= 0
                      ? 'text-info-text'
                      : 'text-warning-text'
                  }`}
                >
                  <MetricHint label={t('reports.netProfit')} />
                </p>
                <p
                  className={`text-3xl font-bold ${
                    overview.netProfit >= 0
                      ? 'text-info-text'
                      : 'text-warning-text'
                  }`}
                >
                  {formatCurrency(overview.netProfit)}
                </p>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Income Trend Chart */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('reports.incomeTrend.title')}
              </h2>
              <p className="text-sm text-text-secondary">
                {t('reports.incomeTrend.subtitle')}
              </p>
            </div>
            <div className="flex items-center gap-1 bg-surface-inset rounded-md p-0.5">
              <button
                onClick={() => setIncomeChartType('line')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  incomeChartType === 'line'
                    ? 'bg-surface-card text-text-primary shadow-sm'
                    : 'text-text-secondary'
                }`}
              >
                {t('reports.incomeTrend.line')}
              </button>
              <button
                onClick={() => setIncomeChartType('bar')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  incomeChartType === 'bar'
                    ? 'bg-surface-card text-text-primary shadow-sm'
                    : 'text-text-secondary'
                }`}
              >
                {t('reports.incomeTrend.bar')}
              </button>
            </div>
          </div>
          {trendLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : incomeTrend ? (
            <ResponsiveContainer width="100%" height={300}>
              {incomeChartType === 'line' ? (
                <LineChart data={incomeTrend.dataPoints}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
                  />
                  <XAxis
                    dataKey="period"
                    tick={{ fontSize: 12 }}
                    stroke={isDark ? '#5c6180' : '#9CA3AF'}
                  />
                  <YAxis
                    tick={{ fontSize: 12 }}
                    stroke={isDark ? '#5c6180' : '#9CA3AF'}
                    tickFormatter={formatYAxis}
                  />
                  <Tooltip
                    formatter={(value) =>
                      typeof value === 'number' ? formatCurrency(value) : 'N/A'
                    }
                    contentStyle={tooltipStyle}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="income"
                    stroke="#10B981"
                    strokeWidth={2}
                    name={t('reports.income')}
                    dot={{ fill: '#10B981', r: 4 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="expenses"
                    stroke="#EF4444"
                    strokeWidth={2}
                    name={t('reports.expenses')}
                    dot={{ fill: '#EF4444', r: 4 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="netProfit"
                    stroke="#3B82F6"
                    strokeWidth={2}
                    name={t('reports.netProfit')}
                    dot={{ fill: '#3B82F6', r: 4 }}
                  />
                </LineChart>
              ) : (
                <BarChart data={incomeTrend.dataPoints}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
                  />
                  <XAxis
                    dataKey="period"
                    tick={{ fontSize: 12 }}
                    stroke={isDark ? '#5c6180' : '#9CA3AF'}
                  />
                  <YAxis
                    tick={{ fontSize: 12 }}
                    stroke={isDark ? '#5c6180' : '#9CA3AF'}
                    tickFormatter={formatYAxis}
                  />
                  <Tooltip
                    formatter={(value) =>
                      typeof value === 'number' ? formatCurrency(value) : 'N/A'
                    }
                    contentStyle={tooltipStyle}
                  />
                  <Legend />
                  <Bar
                    dataKey="income"
                    fill="#10B981"
                    name={t('reports.income')}
                    radius={[4, 4, 0, 0]}
                  />
                  <Bar
                    dataKey="expenses"
                    fill="#EF4444"
                    name={t('reports.expenses')}
                    radius={[4, 4, 0, 0]}
                  />
                  <Bar
                    dataKey="netProfit"
                    fill="#3B82F6"
                    name={t('reports.netProfit')}
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              )}
            </ResponsiveContainer>
          ) : null}
        </div>

        {/* Expense Breakdown Pie Chart */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('reports.expenseBreakdown.title')}
              </h2>
              <p className="text-sm text-text-secondary">
                {t('reports.expenseBreakdown.subtitle')}
              </p>
            </div>
            <div className="flex items-center gap-1 bg-surface-inset rounded-md p-0.5">
              <button
                onClick={() => setExpenseChartType('pie')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  expenseChartType === 'pie'
                    ? 'bg-surface-card text-text-primary shadow-sm'
                    : 'text-text-secondary'
                }`}
              >
                {t('reports.expenseBreakdown.pie')}
              </button>
              <button
                onClick={() => setExpenseChartType('bar')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  expenseChartType === 'bar'
                    ? 'bg-surface-card text-text-primary shadow-sm'
                    : 'text-text-secondary'
                }`}
              >
                {t('reports.expenseBreakdown.bar')}
              </button>
            </div>
          </div>
          {breakdownLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : expenseBreakdown && expenseBreakdown.categories.length > 0 ? (
            expenseChartType === 'pie' ? (
              <div className="flex flex-col lg:flex-row items-center gap-6">
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={expenseBreakdown.categories}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({
                        name,
                        percent,
                      }: {
                        name?: string;
                        percent?: number;
                      }) =>
                        `${name ?? ''} ${((percent ?? 0) * 100).toFixed(0)}%`
                      }
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {expenseBreakdown.categories.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value) =>
                        typeof value === 'number'
                          ? formatCurrency(value)
                          : 'N/A'
                      }
                      contentStyle={tooltipStyle}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="flex-1 max-w-xs">
                  <div className="space-y-2">
                    {expenseBreakdown.categories.map((category) => (
                      <div
                        key={category.name}
                        className="flex items-center justify-between text-sm"
                      >
                        <div className="flex items-center gap-2">
                          <div
                            className="w-3 h-3 rounded-full"
                            style={{ backgroundColor: category.color }}
                          />
                          <span className="text-text-secondary">
                            {category.name}
                          </span>
                        </div>
                        <span className="font-medium text-text-primary">
                          {formatCurrency(category.value)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={expenseBreakdown.categories} layout="vertical">
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
                  />
                  <XAxis
                    type="number"
                    tick={{ fontSize: 12 }}
                    stroke={isDark ? '#5c6180' : '#9CA3AF'}
                    tickFormatter={formatYAxis}
                  />
                  <YAxis
                    type="category"
                    dataKey="name"
                    tick={{ fontSize: 11 }}
                    stroke={isDark ? '#5c6180' : '#9CA3AF'}
                    width={120}
                  />
                  <Tooltip
                    formatter={(value) =>
                      typeof value === 'number' ? formatCurrency(value) : 'N/A'
                    }
                    contentStyle={tooltipStyle}
                  />
                  <Bar
                    dataKey="value"
                    name={t('reports.amount')}
                    radius={[0, 4, 4, 0]}
                  >
                    {expenseBreakdown.categories.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )
          ) : (
            <div className="text-center py-12 text-text-secondary">
              {t('reports.expenseBreakdown.noData')}
            </div>
          )}
        </div>

        {/* Property Performance Table */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('reports.propertyPerformance.title')}
              </h2>
              <p className="text-sm text-text-secondary">
                {t('reports.propertyPerformance.subtitle')}
              </p>
            </div>
            <Building2 className="h-6 w-6 text-text-muted " />
          </div>
          {comparisonLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : propertyComparison && propertyComparison.properties.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b border-border-default">
                    <th className="text-left py-2 pr-4 font-medium text-text-secondary">
                      {t('reports.propertyPerformance.property')}
                    </th>
                    <th className="text-right py-2 px-4 font-medium text-text-secondary">
                      {t('reports.propertyPerformance.income')}
                    </th>
                    <th className="text-right py-2 px-4 font-medium text-text-secondary">
                      {t('reports.propertyPerformance.expenses')}
                    </th>
                    <th className="text-right py-2 px-4 font-medium text-text-secondary">
                      {t('reports.propertyPerformance.netProfit')}
                    </th>
                    <th className="text-right py-2 pl-4 font-medium text-text-secondary">
                      {t('reports.propertyPerformance.margin')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {propertyComparison.properties.map((p) => {
                    const margin =
                      p.income > 0 ? (p.netProfit / p.income) * 100 : 0;
                    return (
                      <tr
                        key={p.property.identifier}
                        className="border-b border-border-default last:border-0 hover:bg-surface-page cursor-pointer"
                        onClick={() =>
                          navigate(`/properties/${p.property.identifier}`)
                        }
                      >
                        <td className="py-3 pr-4">
                          <div className="font-medium text-text-primary">
                            {p.property.street}
                          </div>
                          <div className="text-xs text-text-secondary">
                            {p.property.city}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-right font-medium text-success-text">
                          {formatCurrency(p.income)}
                        </td>
                        <td className="py-3 px-4 text-right font-medium text-error-text">
                          {formatCurrency(p.expenses)}
                        </td>
                        <td
                          className={`py-3 px-4 text-right font-semibold ${p.netProfit >= 0 ? 'text-info-text' : 'text-error-text'}`}
                        >
                          {formatCurrency(p.netProfit)}
                        </td>
                        <td className="py-3 pl-4 text-right">
                          <span
                            className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                              margin >= 50
                                ? 'bg-success-bg text-success-text'
                                : margin >= 20
                                  ? 'bg-info-bg text-info-text'
                                  : margin >= 0
                                    ? 'bg-warning-bg text-warning-text'
                                    : 'bg-error-bg text-error-text'
                            }`}
                          >
                            {margin.toFixed(1)}%
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
                {propertyComparison.properties.length > 1 && (
                  <tfoot>
                    <tr className="border-t-2 border-border-strong">
                      <td className="py-3 pr-4 font-semibold text-text-primary">
                        {t('reports.propertyPerformance.total')}
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-success-text">
                        {formatCurrency(
                          propertyComparison.properties.reduce(
                            (s, p) => s + p.income,
                            0
                          )
                        )}
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-error-text">
                        {formatCurrency(
                          propertyComparison.properties.reduce(
                            (s, p) => s + p.expenses,
                            0
                          )
                        )}
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-info-text">
                        {formatCurrency(
                          propertyComparison.properties.reduce(
                            (s, p) => s + p.netProfit,
                            0
                          )
                        )}
                      </td>
                      <td className="py-3 pl-4"></td>
                    </tr>
                  </tfoot>
                )}
              </table>
            </div>
          ) : (
            <div className="text-center py-12 text-text-secondary">
              {t('reports.propertyPerformance.noData')}
            </div>
          )}
        </div>

        {/* Occupancy Trend */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('reports.occupancyTrend.title')}
              </h2>
              <p className="text-sm text-text-secondary">
                {t('reports.occupancyTrend.subtitle')}
              </p>
            </div>
            <TrendingUp className="h-6 w-6 text-text-muted " />
          </div>
          {occupancyLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : occupancyTrend ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={occupancyTrend.dataPoints}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke={isDark ? '#2a2e3f' : '#f0f0f0'}
                />
                <XAxis
                  dataKey="period"
                  tick={{ fontSize: 12 }}
                  stroke={isDark ? '#5c6180' : '#9CA3AF'}
                />
                <YAxis
                  tick={{ fontSize: 12 }}
                  stroke={isDark ? '#5c6180' : '#9CA3AF'}
                  domain={[0, 100]}
                  tickFormatter={formatPercent}
                />
                <Tooltip
                  formatter={(value, name) => {
                    if (typeof value !== 'number') {
                      return 'N/A';
                    }
                    if (name === 'occupancyRate') {
                      return formatPercent(value);
                    }
                    return value;
                  }}
                  contentStyle={tooltipStyle}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="occupancyRate"
                  stroke="#8B5CF6"
                  strokeWidth={3}
                  name={t('reports.occupancyTrend.occupancyRate')}
                  dot={{ fill: '#8B5CF6', r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : null}
        </div>
      </div>
    </div>
  );
};
