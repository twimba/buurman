import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ExpenseCategory } from '@/types/expense';
import { useExpenses } from '@/hooks/useExpenseHooks';
import { ExpenseCard } from '@/components/expenses/ExpenseCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Receipt, Filter } from 'lucide-react';

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

export const ExpensesPage = () => {
  const navigate = useNavigate();
  const [categoryFilter, setCategoryFilter] = useState<
    ExpenseCategory | undefined
  >(undefined);

  const {
    data: expenses,
    isLoading,
    error,
  } = useExpenses(categoryFilter ? { category: categoryFilter } : undefined);

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

  const totalAmount =
    expenses?.reduce((sum, expense) => sum + expense.amount, 0) || 0;

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

        {/* Summary Card */}
        {expenses && expenses.length > 0 && (
          <div className="mb-6 bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg p-6 text-white">
            <p className="text-sm opacity-90 mb-1">Total Expenses</p>
            <p className="text-3xl font-bold">EUR {totalAmount.toFixed(2)}</p>
            <p className="text-sm opacity-75 mt-1">
              {expenses.length} expense{expenses.length > 1 ? 's' : ''}{' '}
              {categoryFilter
                ? `in ${categoryFilter}`
                : 'across all categories'}
            </p>
          </div>
        )}

        {/* Filter Bar */}
        <div className="mb-6 bg-white rounded-lg border border-gray-200 p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-gray-600" />
            <h2 className="font-semibold text-gray-900">Filters</h2>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category
            </label>
            <div className="flex gap-2 flex-wrap">
              {categoryFilters.map((filter) => (
                <button
                  key={filter.label}
                  onClick={() => setCategoryFilter(filter.value)}
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
        </div>

        {/* Expense Count */}
        <p className="text-sm text-gray-600 mb-4">
          {expenses?.length || 0}{' '}
          {expenses?.length === 1 ? 'expense' : 'expenses'}
        </p>

        {/* Expenses Grid */}
        {expenses && expenses.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {expenses.map((expense) => (
              <ExpenseCard key={expense.id} expense={expense} />
            ))}
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
            <Receipt className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No expenses found
            </h3>
            <p className="text-gray-600 mb-6">
              {categoryFilter
                ? 'Try adjusting your filters'
                : 'Get started by recording your first expense'}
            </p>
            {!categoryFilter && (
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
