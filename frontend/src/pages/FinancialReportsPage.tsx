import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Calendar,
  Building2,
  PieChart as PieChartIcon,
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
  useFinancialOverview,
  useIncomeTrend,
  useExpenseBreakdown,
  usePropertyComparison,
  useOccupancyTrend,
} from '@/hooks/useReportHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';

export const FinancialReportsPage = () => {
  const navigate = useNavigate();
  const [periodType, setPeriodType] = useState<
    'month' | 'quarter' | 'year' | 'all' | 'custom'
  >('month');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  // Calculate date range based on period type
  const dateRange = useMemo(() => {
    const today = new Date();
    let startDate: Date;
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
        startDate = new Date(1982, 6, 24);
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
      startDate: startDate!.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0],
    };
  }, [periodType, customStartDate, customEndDate]);

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
          start = new Date(1982, 6, 24);
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
    undefined,
    'EUR',
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: incomeTrend,
    isLoading: trendLoading,
    error: trendError,
  } = useIncomeTrend(12);

  const {
    data: expenseBreakdown,
    isLoading: breakdownLoading,
    error: breakdownError,
  } = useExpenseBreakdown(
    dateRange.startDate,
    dateRange.endDate,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: propertyComparison,
    isLoading: comparisonLoading,
    error: comparisonError,
  } = usePropertyComparison(
    dateRange.startDate,
    dateRange.endDate,
    periodType !== 'custom' || (!!customStartDate && !!customEndDate)
  );

  const {
    data: occupancyTrend,
    isLoading: occupancyLoading,
    error: occupancyError,
  } = useOccupancyTrend(12);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('nl-NL', {
      style: 'currency',
      currency: 'EUR',
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
            <BarChart3 className="h-8 w-8 text-blue-600 dark:text-blue-400" />
            <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
              Financial Reports
            </h1>
          </div>
          <p className="text-gray-600 dark:text-gray-400 ml-11">
            Comprehensive financial overview and analytics
          </p>
        </div>
        <button
          onClick={() => navigate('/reports/transactions')}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
        >
          <List className="h-5 w-5" />
          View Transaction History
        </button>
      </div>

      {/* Period Selector */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm dark:shadow-gray-900 p-6 mb-6">
        <div className="flex flex-col gap-4">
          <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-gray-500 dark:text-gray-400" />
              <span className="font-medium text-gray-900 dark:text-gray-100">
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
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
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
                className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <span className="text-gray-500 self-center">to</span>
              <input
                type="date"
                value={customEndDate || dateRange.endDate}
                onChange={(e) => {
                  setCustomEndDate(e.target.value);
                  setPeriodType('custom');
                }}
                className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
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
          <div className="bg-gradient-to-br from-green-50 to-green-100 rounded-lg shadow-sm p-6 border border-green-200">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-500 rounded-lg">
                <TrendingUp className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p className="text-sm text-green-700 font-medium">
                  Total Income
                </p>
                <p className="text-3xl font-bold text-green-900">
                  {formatCurrency(overview.income.total)}
                </p>
              </div>
            </div>
          </div>

          {/* Total Expenses */}
          <div className="bg-gradient-to-br from-red-50 to-red-100 rounded-lg shadow-sm p-6 border border-red-200">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-red-500 rounded-lg">
                <TrendingDown className="h-6 w-6 text-white" />
              </div>
              <div className="text-right">
                <p className="text-sm text-red-700 font-medium">
                  Total Expenses
                </p>
                <p className="text-3xl font-bold text-red-900">
                  {formatCurrency(overview.expenses.total)}
                </p>
              </div>
            </div>
          </div>

          {/* Net Profit */}
          <div
            className={`bg-gradient-to-br ${
              overview.netProfit >= 0
                ? 'from-blue-50 to-blue-100 border-blue-200'
                : 'from-orange-50 to-orange-100 border-orange-200'
            } rounded-lg shadow-sm p-6 border`}
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
                      ? 'text-blue-700'
                      : 'text-orange-700'
                  }`}
                >
                  Net Profit
                </p>
                <p
                  className={`text-3xl font-bold ${
                    overview.netProfit >= 0
                      ? 'text-blue-900'
                      : 'text-orange-900'
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
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Income Trend (12 Months)
              </h2>
              <p className="text-sm text-gray-600">
                Monthly income, expenses, and net profit
              </p>
            </div>
            <BarChart3 className="h-6 w-6 text-gray-400" />
          </div>
          {trendLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : incomeTrend ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={incomeTrend.dataPoints}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="period"
                  tick={{ fontSize: 12 }}
                  stroke="#9CA3AF"
                />
                <YAxis tick={{ fontSize: 12 }} stroke="#9CA3AF" />
                <Tooltip
                  formatter={(value: number | undefined) =>
                    value !== undefined ? formatCurrency(value) : 'N/A'
                  }
                  contentStyle={{
                    backgroundColor: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '12px',
                  }}
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
            </ResponsiveContainer>
          ) : null}
        </div>

        {/* Expense Breakdown Pie Chart */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Expense Breakdown
              </h2>
              <p className="text-sm text-gray-600">
                By category for selected period
              </p>
            </div>
            <PieChartIcon className="h-6 w-6 text-gray-400" />
          </div>
          {breakdownLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : expenseBreakdown && expenseBreakdown.categories.length > 0 ? (
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
                    }) => `${name ?? ''} ${((percent ?? 0) * 100).toFixed(0)}%`}
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
                    contentStyle={{
                      backgroundColor: '#fff',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
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
                        <span className="text-gray-700">{category.name}</span>
                      </div>
                      <span className="font-medium text-gray-900">
                        {formatCurrency(category.value)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-12 text-gray-500">
              No expense data for selected period
            </div>
          )}
        </div>

        {/* Property Comparison Bar Chart */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Property Comparison
              </h2>
              <p className="text-sm text-gray-600">
                Income vs expenses by property
              </p>
            </div>
            <Building2 className="h-6 w-6 text-gray-400" />
          </div>
          {comparisonLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : propertyComparison && propertyComparison.properties.length > 0 ? (
            <ResponsiveContainer width="100%" height={450}>
              <BarChart
                data={propertyComparison.properties.map((p) => ({
                  street: p.property.street,
                  city: p.property.city,
                  income: p.income,
                  expenses: p.expenses,
                  netProfit: p.netProfit,
                }))}
                margin={{ top: 20, right: 30, left: 20, bottom: 70 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="street"
                  tick={(props: {
                    x: string | number;
                    y: string | number;
                    index: number;
                  }) => {
                    const { x, y, index } = props;
                    const data = propertyComparison.properties[index];
                    return (
                      <g transform={`translate(${x},${y})`}>
                        <text
                          x={0}
                          y={0}
                          dy={24}
                          textAnchor="middle"
                          fill="#6B7280"
                          fontSize={11}
                        >
                          <tspan x={0} dy={0}>
                            {data.property.street}
                          </tspan>
                          <tspan x={0} dy={14} fontSize={10} fill="#9CA3AF">
                            {data.property.city}
                          </tspan>
                        </text>
                      </g>
                    );
                  }}
                  height={60}
                  interval={0}
                  stroke="#6B7280"
                />
                <YAxis tick={{ fontSize: 12 }} stroke="#6B7280" />
                <Tooltip
                  formatter={(value: number | undefined) =>
                    value !== undefined ? formatCurrency(value) : 'N/A'
                  }
                  labelFormatter={(label, payload) => {
                    if (
                      Array.isArray(payload) &&
                      payload.length > 0 &&
                      payload[0].payload?.fullName
                    ) {
                      return payload[0].payload.fullName;
                    }
                    return label;
                  }}
                  contentStyle={{
                    backgroundColor: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '12px',
                  }}
                />
                <Legend />
                <Bar dataKey="income" fill="#10B981" name="Income" />
                <Bar dataKey="expenses" fill="#EF4444" name="Expenses" />
                <Bar dataKey="netProfit" fill="#3B82F6" name="Net Profit" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="text-center py-12 text-gray-500">
              No property data for selected period
            </div>
          )}
        </div>

        {/* Occupancy Trend */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                Occupancy Trend (12 Months)
              </h2>
              <p className="text-sm text-gray-600">
                Monthly occupancy rate percentage
              </p>
            </div>
            <TrendingUp className="h-6 w-6 text-gray-400" />
          </div>
          {occupancyLoading ? (
            <div className="flex justify-center py-12">
              <LoadingSpinner />
            </div>
          ) : occupancyTrend ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={occupancyTrend.dataPoints}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="period"
                  tick={{ fontSize: 12 }}
                  stroke="#9CA3AF"
                />
                <YAxis
                  tick={{ fontSize: 12 }}
                  stroke="#9CA3AF"
                  domain={[0, 100]}
                  tickFormatter={formatPercent}
                />
                <Tooltip
                  formatter={(
                    value: number | undefined,
                    name: string | undefined
                  ) => {
                    if (value === undefined) return 'N/A';
                    if (name === 'occupancyRate') return formatPercent(value);
                    return value;
                  }}
                  contentStyle={{
                    backgroundColor: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    fontSize: '12px',
                  }}
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
