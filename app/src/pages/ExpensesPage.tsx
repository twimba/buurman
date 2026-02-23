import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ExpenseCategory, formatExpenseCategory } from '@/types/expense';
import {
  useExpenses,
  useExpenseStats,
  useDeleteExpense,
} from '@/hooks/useExpenseHooks';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { usePagination } from '@/hooks/usePagination';
import { Pagination } from '@/components/ui/Pagination';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { PropertyCell } from '@/components/properties/PropertyCell';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  Receipt,
  Filter,
  ArrowUpDown,
  TrendingDown,
  DollarSign,
  PieChart,
  Eye,
  Trash2,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import { RefreshButton } from '@/components/ui/RefreshButton';
import { PropertySelector } from '@/components/common/PropertySelector';
import {
  PeriodFilter,
  PeriodDateRange,
} from '@/components/common/PeriodFilter';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

const categoryFilters = [
  { value: undefined, label: 'All Categories' },
  { value: ExpenseCategory.MAINTENANCE, label: 'Maintenance' },
  { value: ExpenseCategory.REPAIR, label: 'Repair' },
  { value: ExpenseCategory.UTILITY, label: 'Utility' },
  { value: ExpenseCategory.TAX, label: 'Tax' },
  { value: ExpenseCategory.INSURANCE, label: 'Insurance' },
  { value: ExpenseCategory.LEGAL, label: 'Legal' },
  { value: ExpenseCategory.MARKETING, label: 'Marketing' },
  { value: ExpenseCategory.CLEANING, label: 'Cleaning' },
  { value: ExpenseCategory.LANDSCAPING, label: 'Landscaping' },
  { value: ExpenseCategory.PROPERTY_MANAGEMENT, label: 'Property Management' },
  { value: ExpenseCategory.FEES, label: 'Fees' },
  { value: ExpenseCategory.PROPERTY_TAX, label: 'Property Taxes' },
  { value: ExpenseCategory.MORTGAGE_PAYMENT, label: 'Mortgage Payment' },
  { value: ExpenseCategory.OTHER, label: 'Other' },
];

export const ExpensesPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const deleteExpenseMutation = useDeleteExpense();
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [categoryFilter, setCategoryFilter] = useState<
    ExpenseCategory | undefined
  >(undefined);
  const [propertyFilter, setPropertyFilter] = useState<string | undefined>(
    undefined
  );
  const [periodRange, setPeriodRange] = useState<PeriodDateRange | null>(null);

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({ defaultSort: 'expenseDate' });

  const {
    data: expensesData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = useExpenses({
    category: categoryFilter,
    propertyIdentifier: propertyFilter,
    dateFrom: periodRange?.startDate,
    dateTo: periodRange?.endDate,
    ...pageParams,
  });

  const { data: expenseStats } = useExpenseStats();

  const statsCurrency = expenseStats?.currency ?? '';

  const fmtMoney = (value: number, currency: string) => {
    if (!currency) return value.toFixed(2);
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency,
      }).format(value);
    } catch {
      return `${currency} ${value.toFixed(2)}`;
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load expenses" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <Receipt className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Expenses
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
              Track property expenses and costs
            </p>
          </div>
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <button
              onClick={() => navigate('/expenses/new')}
              disabled={!canEditData}
              className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
            >
              <Plus className="h-5 w-5" />
              Add Expense
            </button>
          </div>
        </div>

        {/* Metrics Dashboard */}
        {expenseStats && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Total Expenses */}
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 flex flex-col">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
                  Total Expenses
                </h3>
                <DollarSign className="h-5 w-5 text-red-500" />
              </div>
              <div className="flex-1 flex flex-col justify-center">
                <p className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                  {fmtMoney(expenseStats.totalAmount, statsCurrency)}
                </p>
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                  {categoryFilter
                    ? `in ${formatExpenseCategory(categoryFilter)}`
                    : `across ${expenseStats.topCategories.length} categories`}
                </p>
              </div>
            </div>

            {/* Top Categories */}
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
                  Top Categories
                </h3>
                <PieChart className="h-5 w-5 text-purple-500" />
              </div>
              <div className="space-y-2">
                {expenseStats.topCategories.slice(0, 3).map((cat, index) => (
                  <div
                    key={cat.category}
                    className="flex items-center justify-between"
                  >
                    <div className="flex items-center gap-2">
                      <div
                        className={`w-2 h-2 rounded-full ${
                          index === 0
                            ? 'bg-purple-500'
                            : index === 1
                              ? 'bg-purple-400'
                              : 'bg-purple-300'
                        }`}
                      />
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] truncate">
                        {formatExpenseCategory(cat.category as ExpenseCategory)}
                      </span>
                    </div>
                    <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      {fmtMoney(cat.total, statsCurrency)}
                    </span>
                  </div>
                ))}
                {expenseStats.topCategories.length === 0 && (
                  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                    No data available
                  </p>
                )}
              </div>
            </div>

            {/* 6-Month Expenses Chart */}
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
                  Last 6 Months
                </h3>
                <TrendingDown className="h-5 w-5 text-red-500" />
              </div>
              <ResponsiveContainer width="100%" height={80}>
                <AreaChart data={expenseStats.monthlyTrend}>
                  <defs>
                    <linearGradient
                      id="colorExpenses"
                      x1="0"
                      y1="0"
                      x2="0"
                      y2="1"
                    >
                      <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis
                    dataKey="month"
                    tick={{ fontSize: 10 }}
                    stroke="#9ca3af"
                  />
                  <YAxis hide />
                  <Tooltip
                    formatter={(value: number | undefined) => [
                      value !== undefined
                        ? fmtMoney(value, statsCurrency)
                        : 'N/A',
                      'Expenses',
                    ]}
                    contentStyle={{ fontSize: 12 }}
                  />
                  <Area
                    type="monotone"
                    dataKey="total"
                    stroke="#ef4444"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#colorExpenses)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* Filter Bar */}
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
            <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Filters
            </h3>
          </div>

          <div className="flex flex-col gap-4">
            {/* Row 1: Property selector + Period filter */}
            <div className="flex flex-col lg:flex-row gap-4 items-end">
              <div className="lg:w-96">
                <label className="block text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
                  Property
                </label>
                <PropertySelector
                  value={propertyFilter ?? ''}
                  onChange={(id) => {
                    setPropertyFilter(id || undefined);
                    resetPage();
                  }}
                  clearable
                  placeholder="All Properties"
                />
              </div>
              <div className="flex-1">
                <PeriodFilter
                  presets={['month', 'quarter', 'year', 'all', 'custom']}
                  defaultPreset="all"
                  onChange={(range) => {
                    setPeriodRange(range);
                    resetPage();
                  }}
                />
              </div>
            </div>

            {/* Row 2: Category filter */}
            <div>
              <label className="block text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] mb-1">
                Category
              </label>
              <div className="flex gap-2 flex-wrap">
                {categoryFilters.map((filter) => (
                  <button
                    key={filter.label}
                    onClick={() => {
                      setCategoryFilter(filter.value);
                      resetPage();
                    }}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      categoryFilter === filter.value
                        ? 'bg-[#5c7cfa] text-white'
                        : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54]'
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Expenses Table */}
        {expensesData?.content && expensesData.content.length > 0 ? (
          <>
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSortChange('expenseDate')}
                    >
                      <div className="flex items-center gap-1">
                        Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Expense #
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Description
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSortChange('category')}
                    >
                      <div className="flex items-center gap-1">
                        Category
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider min-w-[220px]">
                      Property
                    </th>
                    <th
                      className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSortChange('amount')}
                    >
                      <div className="flex items-center justify-end gap-1">
                        Amount
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider"></th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                  {expensesData.content.map((expense) => (
                    <tr
                      key={expense.identifier}
                      className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                      onClick={() =>
                        navigate(`/expenses/${expense.identifier}`)
                      }
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(expense.expenseDate)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {expense.identifier}
                      </td>
                      <td className="px-6 py-4 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                        {expense.description}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <ExpenseCategoryBadge category={expense.category} />
                      </td>
                      <td className="px-6 py-3">
                        <PropertyCell
                          propertyIdentifier={expense.property.identifier}
                          propertyStatus={expense.property.status}
                          propertyType={expense.property.propertyType}
                          street={expense.property.street}
                          city={expense.property.city}
                          postalCode={expense.property.postalCode}
                        />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right">
                        <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                          {fmtMoney(expense.amount, expense.currency)}
                        </span>
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              navigate(`/expenses/${expense.identifier}`);
                            }}
                            className="p-1.5 rounded hover:bg-[#e8ecf4] dark:hover:bg-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#748ffc] transition-colors"
                            title="View expense"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                          {canEditData && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeleteTarget(expense.identifier);
                              }}
                              className="p-1.5 rounded hover:bg-red-50 dark:hover:bg-red-900/20 text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600 dark:hover:text-red-400 transition-colors"
                              title="Delete expense"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {expensesData && (
              <Pagination
                page={page}
                totalPages={expensesData.totalPages}
                totalElements={expensesData.totalElements}
                size={size}
                onPageChange={handlePageChange}
                onSizeChange={handleSizeChange}
              />
            )}
          </>
        ) : (
          <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] p-12 text-center">
            <Receipt className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No expenses found
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
              {categoryFilter || propertyFilter
                ? 'Try adjusting your filters'
                : 'Get started by recording your first expense'}
            </p>
            {!categoryFilter && !propertyFilter && (
              <button
                onClick={() => navigate('/expenses/new')}
                disabled={!canEditData}
                className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
              >
                <Plus className="h-5 w-5" />
                Add Expense
              </button>
            )}
          </div>
        )}
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Delete Expense"
          message="Are you sure you want to delete this expense? All related documents will also be deleted. This action cannot be undone."
          confirmLabel="Delete"
          variant="danger"
          isLoading={deleteExpenseMutation.isPending}
          onConfirm={async () => {
            await deleteExpenseMutation.mutateAsync(deleteTarget);
            setDeleteTarget(null);
          }}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
};
