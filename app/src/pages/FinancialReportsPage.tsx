import { useState, useMemo } from 'react';
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
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { MetricHint } from '@/components/common/MetricHint';
import { useTheme } from '@/context/ThemeContext';
import { useQuery } from '@tanstack/react-query';
import { getProperties } from '@/api/properties';
import { exportTransactionsCSV, exportTransactionsPDF } from '@/api/reports';
import {
  ChevronDown,
  ChevronUp,
  Filter,
  Check,
  Download,
  FileText,
} from 'lucide-react';

export const FinancialReportsPage = () => {
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
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          Failed to load financial reports
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
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Financial Reports
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Comprehensive financial overview and analytics
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={async () => {
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
            }}
            className="flex items-center gap-2 px-3 py-2 text-[#3d4463] dark:text-[#c4c8db] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors text-sm"
            title="Download CSV"
          >
            <Download className="h-4 w-4" />
            CSV
          </button>
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
            className="flex items-center gap-2 px-3 py-2 text-[#3d4463] dark:text-[#c4c8db] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors text-sm"
            title="Download PDF"
          >
            <FileText className="h-4 w-4" />
            PDF
          </button>
          <button
            onClick={() => navigate('/reports/transactions')}
            className="flex items-center gap-2 px-4 py-2 bg-[#5c7cfa] text-white rounded-md hover:bg-[#4c6ef5] transition-colors text-sm"
          >
            <List className="h-5 w-5" />
            Transactions
          </button>
        </div>
      </div>

      {/* Period Selector */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 mb-6">
        <div className="flex flex-col gap-4">
          <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                Period:
              </span>
              <div className="flex gap-2 flex-wrap">
                {['month', 'quarter', 'year', 'all', 'custom'].map((type) => (
                  <button
                    key={type}
                    onClick={() =>
                      handlePeriodChange(type as typeof periodType)
                    }
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                      periodType === type
                        ? 'bg-[#5c7cfa] text-white'
                        : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54]'
                    }`}
                  >
                    {type === 'all'
                      ? 'All Time'
                      : type.charAt(0).toUpperCase() + type.slice(1)}
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
                className="px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              />
              <span className="text-[#6b7194] dark:text-[#8b90a8] self-center">
                to
              </span>
              <input
                type="date"
                value={customEndDate || dateRange.endDate}
                onChange={(e) => {
                  setCustomEndDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
              />
            </div>
          </div>

          {/* Advanced Filters Toggle */}
          <div className="border-t border-[#edf0f7] dark:border-[#2a2e3f] pt-3 mt-3">
            <button
              onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
              className="flex items-center gap-2 text-sm font-medium text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db] transition-colors"
            >
              <Filter className="h-4 w-4" />
              Advanced Filters
              {selectedPropertyIds.length > 0 && (
                <span className="bg-[#5c7cfa] text-white text-xs px-1.5 py-0.5 rounded-full">
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
                    <label className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8]">
                      Properties
                    </label>
                    {selectedPropertyIds.length > 0 && (
                      <button
                        onClick={() => setSelectedPropertyIds([])}
                        className="text-xs text-[#5c7cfa] hover:underline"
                      >
                        Clear all
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
                              ? 'bg-[#5c7cfa]/10 border-[#5c7cfa] text-[#5c7cfa] dark:bg-[#5c7cfa]/20 dark:text-[#748ffc]'
                              : 'border-[#c9cfd9] dark:border-[#3a3f54] text-[#3d4463] dark:text-[#c4c8db] hover:border-[#5c7cfa]/50'
                          }`}
                        >
                          {isSelected && <Check className="h-3 w-3" />}
                          {p.street}, {p.city}
                        </button>
                      );
                    })}
                    {allProperties.length === 0 && (
                      <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                        No properties found
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
          <div className="bg-gradient-to-br from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-900/10 rounded-xl shadow-sm p-6 border border-green-200 dark:border-green-800">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-500 rounded-lg">
                <TrendingUp className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p className="text-sm text-green-700 dark:text-green-400 font-medium">
                  Total Income
                </p>
                <p className="text-3xl font-bold text-green-900 dark:text-green-200">
                  {formatCurrency(overview.income.total)}
                </p>
              </div>
            </div>
          </div>

          {/* Total Expenses */}
          <div className="bg-gradient-to-br from-red-50 to-red-100 dark:from-red-900/20 dark:to-red-900/10 rounded-xl shadow-sm p-6 border border-red-200 dark:border-red-800">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-red-500 rounded-lg">
                <TrendingDown className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p className="text-sm text-red-700 dark:text-red-400 font-medium">
                  Total Expenses
                </p>
                <p className="text-3xl font-bold text-red-900 dark:text-red-200">
                  {formatCurrency(overview.expenses.total)}
                </p>
              </div>
            </div>
          </div>

          {/* Net Profit */}
          <div
            className={`bg-gradient-to-br ${
              overview.netProfit >= 0
                ? 'from-blue-50 to-blue-100 dark:from-blue-900/20 dark:to-blue-900/10 border-blue-200 dark:border-blue-800'
                : 'from-orange-50 to-orange-100 dark:from-orange-900/20 dark:to-orange-900/10 border-orange-200 dark:border-orange-800'
            } rounded-xl shadow-sm p-6 border`}
          >
            <div className="flex items-center justify-between mb-4">
              <div
                className={`p-3 ${
                  overview.netProfit >= 0 ? 'bg-blue-500' : 'bg-orange-500'
                } rounded-lg`}
              >
                <DollarSign className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p
                  className={`text-sm font-medium ${
                    overview.netProfit >= 0
                      ? 'text-blue-700 dark:text-blue-400'
                      : 'text-orange-700 dark:text-orange-400'
                  }`}
                >
                  <MetricHint label="Net Profit" />
                </p>
                <p
                  className={`text-3xl font-bold ${
                    overview.netProfit >= 0
                      ? 'text-blue-900 dark:text-blue-200'
                      : 'text-orange-900 dark:text-orange-200'
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
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Income Trend
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Monthly income, expenses, and net profit
              </p>
            </div>
            <div className="flex items-center gap-1 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-md p-0.5">
              <button
                onClick={() => setIncomeChartType('line')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  incomeChartType === 'line'
                    ? 'bg-white dark:bg-[#2a2e3f] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm'
                    : 'text-[#6b7194] dark:text-[#8b90a8]'
                }`}
              >
                Line
              </button>
              <button
                onClick={() => setIncomeChartType('bar')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  incomeChartType === 'bar'
                    ? 'bg-white dark:bg-[#2a2e3f] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm'
                    : 'text-[#6b7194] dark:text-[#8b90a8]'
                }`}
              >
                Bar
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
                    formatter={(value: number | undefined) =>
                      value !== undefined ? formatCurrency(value) : 'N/A'
                    }
                    contentStyle={tooltipStyle}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="income"
                    stroke="#10B981"
                    strokeWidth={2}
                    name="Income"
                    dot={{ fill: '#10B981', r: 4 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="expenses"
                    stroke="#EF4444"
                    strokeWidth={2}
                    name="Expenses"
                    dot={{ fill: '#EF4444', r: 4 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="netProfit"
                    stroke="#3B82F6"
                    strokeWidth={2}
                    name="Net Profit"
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
                    formatter={(value: number | undefined) =>
                      value !== undefined ? formatCurrency(value) : 'N/A'
                    }
                    contentStyle={tooltipStyle}
                  />
                  <Legend />
                  <Bar
                    dataKey="income"
                    fill="#10B981"
                    name="Income"
                    radius={[4, 4, 0, 0]}
                  />
                  <Bar
                    dataKey="expenses"
                    fill="#EF4444"
                    name="Expenses"
                    radius={[4, 4, 0, 0]}
                  />
                  <Bar
                    dataKey="netProfit"
                    fill="#3B82F6"
                    name="Net Profit"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              )}
            </ResponsiveContainer>
          ) : null}
        </div>

        {/* Expense Breakdown Pie Chart */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Expense Breakdown
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                By category for selected period
              </p>
            </div>
            <div className="flex items-center gap-1 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-md p-0.5">
              <button
                onClick={() => setExpenseChartType('pie')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  expenseChartType === 'pie'
                    ? 'bg-white dark:bg-[#2a2e3f] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm'
                    : 'text-[#6b7194] dark:text-[#8b90a8]'
                }`}
              >
                Pie
              </button>
              <button
                onClick={() => setExpenseChartType('bar')}
                className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                  expenseChartType === 'bar'
                    ? 'bg-white dark:bg-[#2a2e3f] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm'
                    : 'text-[#6b7194] dark:text-[#8b90a8]'
                }`}
              >
                Bar
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
                      formatter={(value: number | undefined) =>
                        value !== undefined ? formatCurrency(value) : 'N/A'
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
                          <span className="text-[#3d4463] dark:text-[#c4c8db]">
                            {category.name}
                          </span>
                        </div>
                        <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
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
                    formatter={(value: number | undefined) =>
                      value !== undefined ? formatCurrency(value) : 'N/A'
                    }
                    contentStyle={tooltipStyle}
                  />
                  <Bar dataKey="value" name="Amount" radius={[0, 4, 4, 0]}>
                    {expenseBreakdown.categories.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )
          ) : (
            <div className="text-center py-12 text-[#6b7194] dark:text-[#8b90a8]">
              No expense data for selected period
            </div>
          )}
        </div>

        {/* Property Performance Table */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Property Performance
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Income, expenses & net profit per property
              </p>
            </div>
            <Building2 className="h-6 w-6 text-[#9ca0b8] dark:text-[#5c6180]" />
          </div>
          {comparisonLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : propertyComparison && propertyComparison.properties.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b border-[#edf0f7] dark:border-[#2a2e3f]">
                    <th className="text-left py-2 pr-4 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                      Property
                    </th>
                    <th className="text-right py-2 px-4 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                      Income
                    </th>
                    <th className="text-right py-2 px-4 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                      Expenses
                    </th>
                    <th className="text-right py-2 px-4 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                      Net Profit
                    </th>
                    <th className="text-right py-2 pl-4 font-medium text-[#6b7194] dark:text-[#8b90a8]">
                      Margin
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
                        className="border-b border-[#edf0f7] dark:border-[#2a2e3f] last:border-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1e2130] cursor-pointer"
                        onClick={() =>
                          navigate(`/properties/${p.property.identifier}`)
                        }
                      >
                        <td className="py-3 pr-4">
                          <div className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                            {p.property.street}
                          </div>
                          <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                            {p.property.city}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-right font-medium text-green-600 dark:text-green-400">
                          {formatCurrency(p.income)}
                        </td>
                        <td className="py-3 px-4 text-right font-medium text-red-600 dark:text-red-400">
                          {formatCurrency(p.expenses)}
                        </td>
                        <td
                          className={`py-3 px-4 text-right font-semibold ${p.netProfit >= 0 ? 'text-blue-600 dark:text-blue-400' : 'text-red-600 dark:text-red-400'}`}
                        >
                          {formatCurrency(p.netProfit)}
                        </td>
                        <td className="py-3 pl-4 text-right">
                          <span
                            className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                              margin >= 50
                                ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                                : margin >= 20
                                  ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                                  : margin >= 0
                                    ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300'
                                    : 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300'
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
                    <tr className="border-t-2 border-[#c9cfd9] dark:border-[#3a3f54]">
                      <td className="py-3 pr-4 font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                        Total
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-green-600 dark:text-green-400">
                        {formatCurrency(
                          propertyComparison.properties.reduce(
                            (s, p) => s + p.income,
                            0
                          )
                        )}
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-red-600 dark:text-red-400">
                        {formatCurrency(
                          propertyComparison.properties.reduce(
                            (s, p) => s + p.expenses,
                            0
                          )
                        )}
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-blue-600 dark:text-blue-400">
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
            <div className="text-center py-12 text-[#6b7194] dark:text-[#8b90a8]">
              No property data for selected period
            </div>
          )}
        </div>

        {/* Occupancy Trend */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Occupancy Trend
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Monthly occupancy rate percentage
              </p>
            </div>
            <TrendingUp className="h-6 w-6 text-[#9ca0b8] dark:text-[#5c6180]" />
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
                  formatter={(
                    value: number | undefined,
                    name: string | undefined
                  ) => {
                    if (value === undefined) {
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
                  name="Occupancy Rate (%)"
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
