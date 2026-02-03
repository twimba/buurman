import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ExpenseCategory, formatExpenseCategory } from '@/types/expense';
import { useExpenses } from '@/hooks/useExpenseHooks';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
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
      let aVal: any;
      let bVal: any;

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
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Expenses</h1>
            <p className="text-sm text-gray-600 mt-1">
              Track property expenses and costs
            </p>
          </div>
          <button
            onClick={() => navigate('/expenses/new')}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Plus className="h-5 w-5" />
            Add Expense
          </button>
        </div>

        {/* Metrics Dashboard */}
        {expenses && expenses.length > 0 && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Total Expenses */}
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-gray-600">
                  Total Expenses
                </h3>
                <DollarSign className="h-5 w-5 text-red-500" />
              </div>
              <p className="text-3xl font-bold text-gray-900">
                EUR {totalAmount.toFixed(2)}
              </p>
              <p className="text-sm text-gray-500 mt-1">
                {expenses.length} expense{expenses.length !== 1 ? 's' : ''}
                {categoryFilter &&
                  ` in ${formatExpenseCategory(categoryFilter)}`}
              </p>
            </div>

            {/* Top Categories */}
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium text-gray-600">
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
                      <span className="text-sm text-gray-700 truncate">
                        {cat.label}
                      </span>
                    </div>
                    <span className="text-sm font-semibold text-gray-900">
                      EUR {cat.total.toFixed(0)}
                    </span>
                  </div>
                ))}
                {topCategories.length === 0 && (
                  <p className="text-sm text-gray-500">No data available</p>
                )}
              </div>
            </div>

            {/* 6-Month Expenses Chart */}
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-gray-600">
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
                    formatter={(value: number) => [
                      `EUR ${value.toFixed(2)}`,
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
        <div className="mb-6 bg-white rounded-lg border border-gray-200 p-4">
          <div className="flex flex-col md:flex-row gap-4 mb-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search by identifier, description, property, or category..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 mb-2">
            <Filter className="h-5 w-5 text-gray-600" />
            <h3 className="font-semibold text-gray-900">Category Filter</h3>
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
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
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
            <div className="bg-white rounded-lg shadow overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('expenseDate')}
                    >
                      <div className="flex items-center gap-1">
                        Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Expense #
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('description')}
                    >
                      <div className="flex items-center gap-1">
                        Description
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('category')}
                    >
                      <div className="flex items-center gap-1">
                        Category
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('property')}
                    >
                      <div className="flex items-center gap-1">
                        Property
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('amount')}
                    >
                      <div className="flex items-center gap-1">
                        Amount
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {paginatedExpenses.map((expense) => (
                    <tr
                      key={expense.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => navigate(`/expenses/${expense.id}`)}
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {format(new Date(expense.expenseDate), 'MMM d, yyyy')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {expense.identifier}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        {expense.description}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <ExpenseCategoryBadge category={expense.category} />
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        {expense.property.street}, {expense.property.city}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                        {expense.currency} {expense.amount.toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between bg-white px-4 py-3 rounded-lg border border-gray-200">
                <div className="text-sm text-gray-700">
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
                    className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Previous
                  </button>
                  <button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                    className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                  >
                    Next
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
            <Receipt className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No expenses found
            </h3>
            <p className="text-gray-600 mb-6">
              {categoryFilter || searchTerm
                ? 'Try adjusting your filters or search'
                : 'Get started by recording your first expense'}
            </p>
            {!categoryFilter && !searchTerm && (
              <button
                onClick={() => navigate('/expenses/new')}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors inline-flex items-center gap-2"
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
