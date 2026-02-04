import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ExpenseCategory, formatExpenseCategory } from '@/types/expense';
import { useExpenses } from '@/hooks/useExpenseHooks';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { PropertyCell } from '@/components/properties/PropertyCell';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  Receipt,
  Filter,
  Search,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  TrendingDown,
  DollarSign,
  PieChart,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  format,
  subMonths,
  startOfMonth,
  endOfMonth,
  parseISO,
} from 'date-fns';
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
  { value: ExpenseCategory.OTHER, label: 'Other' },
];

const ITEMS_PER_PAGE = 10;

type SortField =
  | 'expenseDate'
  | 'amount'
  | 'category'
  | 'property'
  | 'description';
type SortOrder = 'asc' | 'desc';

export const ExpensesPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [categoryFilter, setCategoryFilter] = useState<
    ExpenseCategory | undefined
  >(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<SortField>('expenseDate');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [currentPage, setCurrentPage] = useState(1);

  const {
    data: expenses,
    isLoading,
    error,
  } = useExpenses(categoryFilter ? { category: categoryFilter } : undefined);

  const filteredAndSortedExpenses = useMemo(() => {
    if (!expenses) return [];

    let filtered = expenses;

    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (e) =>
          e.identifier.toLowerCase().includes(search) ||
          e.description.toLowerCase().includes(search) ||
          e.property.street.toLowerCase().includes(search) ||
          formatExpenseCategory(e.category).toLowerCase().includes(search)
      );
    }

    filtered.sort((a, b) => {
      let aVal: string | number;
      let bVal: string | number;

      switch (sortField) {
        case 'expenseDate':
          aVal = new Date(a.expenseDate).getTime();
          bVal = new Date(b.expenseDate).getTime();
          break;
        case 'amount':
          aVal = a.amount;
          bVal = b.amount;
          break;
        case 'category':
          aVal = a.category;
          bVal = b.category;
          break;
        case 'property':
          aVal = a.property.street;
          bVal = b.property.street;
          break;
        case 'description':
          aVal = a.description;
          bVal = b.description;
          break;
        default:
          return 0;
      }

      if (sortOrder === 'asc') {
        return aVal > bVal ? 1 : -1;
      } else {
        return aVal < bVal ? 1 : -1;
      }
    });

    return filtered;
  }, [expenses, searchTerm, sortField, sortOrder]);

  // Calculate metrics (before conditional returns)
  const totalAmount = useMemo(
    () => expenses?.reduce((sum, expense) => sum + expense.amount, 0) || 0,
    [expenses]
  );

  // Top 3 categories by expense amount
  const topCategories = useMemo(() => {
    if (!expenses) return [];

    const categoryTotals = expenses.reduce(
      (acc, expense) => {
        const category = expense.category;
        acc[category] = (acc[category] || 0) + expense.amount;
        return acc;
      },
      {} as Record<string, number>
    );

    return Object.entries(categoryTotals)
      .map(([category, total]) => ({
        category,
        total,
        label: formatExpenseCategory(category as ExpenseCategory),
      }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 3);
  }, [expenses]);

  // Calculate last 6 months data
  const chartData = useMemo(() => {
    if (!expenses) return [];

    const monthsData = [];
    const now = new Date();

    for (let i = 5; i >= 0; i--) {
      const monthDate = subMonths(now, i);
      const monthStart = startOfMonth(monthDate);
      const monthEnd = endOfMonth(monthDate);

      const monthExpenses = expenses.filter((e) => {
        const expenseDate = parseISO(e.expenseDate);
        return expenseDate >= monthStart && expenseDate <= monthEnd;
      });

      const total = monthExpenses.reduce((sum, e) => sum + e.amount, 0);

      monthsData.push({
        month: format(monthDate, 'MMM'),
        total: Number(total.toFixed(2)),
      });
    }

    return monthsData;
  }, [expenses]);

  const totalPages = Math.ceil(
    filteredAndSortedExpenses.length / ITEMS_PER_PAGE
  );
  const paginatedExpenses = filteredAndSortedExpenses.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
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
          <button
            onClick={() => navigate('/expenses/new')}
            disabled={!canEditData}
            className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
          >
            <Plus className="h-5 w-5" />
            Add Expense
          </button>
        </div>

        {/* Metrics Dashboard */}
        {expenses && expenses.length > 0 && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Total Expenses */}
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
                  Total Expenses
                </h3>
                <DollarSign className="h-5 w-5 text-red-500" />
              </div>
              <p className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                EUR {totalAmount.toFixed(2)}
              </p>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                {expenses.length} expense{expenses.length !== 1 ? 's' : ''}
                {categoryFilter &&
                  ` in ${formatExpenseCategory(categoryFilter)}`}
              </p>
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
                {topCategories.map((cat, index) => (
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
                        {cat.label}
                      </span>
                    </div>
                    <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      EUR {cat.total.toFixed(0)}
                    </span>
                  </div>
                ))}
                {topCategories.length === 0 && (
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
                <AreaChart data={chartData}>
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
                      value !== undefined ? `EUR ${value.toFixed(2)}` : 'N/A',
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

        {/* Search and Filter Bar */}
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div className="flex flex-col md:flex-row gap-4 mb-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
              <input
                type="text"
                placeholder="Search by identifier, description, property, or category..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
                className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6]"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 mb-2">
            <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
            <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Category Filter
            </h3>
          </div>
          <div className="flex gap-2 flex-wrap">
            {categoryFilters.map((filter) => (
              <button
                key={filter.label}
                onClick={() => {
                  setCategoryFilter(filter.value);
                  setCurrentPage(1);
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

        {/* Expenses Table */}
        {filteredAndSortedExpenses.length > 0 ? (
          <>
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('expenseDate')}
                    >
                      <div className="flex items-center gap-1">
                        Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Expense #
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('description')}
                    >
                      <div className="flex items-center gap-1">
                        Description
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('category')}
                    >
                      <div className="flex items-center gap-1">
                        Category
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] min-w-[220px]"
                      onClick={() => handleSort('property')}
                    >
                      <div className="flex items-center gap-1">
                        Property
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('amount')}
                    >
                      <div className="flex items-center justify-end gap-1">
                        Amount
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                  {paginatedExpenses.map((expense) => (
                    <tr
                      key={expense.id}
                      className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                      onClick={() => navigate(`/expenses/${expense.id}`)}
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
                          propertyId={expense.property.id}
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
                          {expense.currency} {expense.amount.toFixed(2)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between bg-white dark:bg-[#14161f] px-4 py-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
                <div className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                  Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} to{' '}
                  {Math.min(
                    currentPage * ITEMS_PER_PAGE,
                    filteredAndSortedExpenses.length
                  )}{' '}
                  of {filteredAndSortedExpenses.length} expenses
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="px-3 py-1 border border-[#c9cfd9] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Previous
                  </button>
                  <button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                    className="px-3 py-1 border border-[#c9cfd9] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                  >
                    Next
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] p-12 text-center">
            <Receipt className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No expenses found
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
              {categoryFilter || searchTerm
                ? 'Try adjusting your filters or search'
                : 'Get started by recording your first expense'}
            </p>
            {!categoryFilter && !searchTerm && (
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
    </div>
  );
};
