import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useExpensesByProperty } from '@/hooks/useExpenseHooks';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { ErrorMessage } from '@/components/ErrorMessage';
import { LoadingSpinner } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import { Plus, Search, ChevronUp, ChevronDown, Receipt } from 'lucide-react';

interface PropertyExpensesTabProps {
  propertyId: string;
}

type ExpenseSortField = 'expenseDate' | 'amount' | 'category' | 'description';

export const PropertyExpensesTab = ({
  propertyId,
}: PropertyExpensesTabProps) => {
  const { t } = useTranslation('properties');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();

  // Table state
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<ExpenseSortField>('expenseDate');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const perPage = 10;

  // Data fetching
  const {
    data: expenses = [],
    isLoading,
    error,
  } = useExpensesByProperty(propertyId);

  // Filtering, sorting, and pagination
  const filteredAndSortedExpenses = useMemo(() => {
    if (!expenses) {
      return [];
    }

    let filtered = [...expenses];

    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (expense) =>
          expense.identifier.toLowerCase().includes(search) ||
          expense.description.toLowerCase().includes(search) ||
          expense.category.toLowerCase().includes(search)
      );
    }

    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

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
        case 'description':
          aVal = a.description;
          bVal = b.description;
          break;
        default:
          return 0;
      }

      if (aVal < bVal) {
        return sortOrder === 'asc' ? -1 : 1;
      }
      if (aVal > bVal) {
        return sortOrder === 'asc' ? 1 : -1;
      }
      return 0;
    });

    return filtered;
  }, [expenses, searchTerm, sortField, sortOrder]);

  const paginatedExpenses = useMemo(() => {
    const startIndex = (currentPage - 1) * perPage;
    const endIndex = startIndex + perPage;
    return filteredAndSortedExpenses.slice(startIndex, endIndex);
  }, [filteredAndSortedExpenses, currentPage]);

  const totalPages = Math.ceil(filteredAndSortedExpenses.length / perPage);

  const handleSort = (field: ExpenseSortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const renderSortIcon = (field: ExpenseSortField) => {
    if (sortField !== field) {
      return null;
    }
    return sortOrder === 'asc' ? (
      <ChevronUp className="h-4 w-4" />
    ) : (
      <ChevronDown className="h-4 w-4" />
    );
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-text-primary">
          {t('expenses.title')} ({filteredAndSortedExpenses.length})
        </h2>
        <button
          onClick={() => navigate(`/expenses/new?propertyId=${propertyId}`)}
          disabled={!canEditData}
          className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
        >
          <Plus className="h-4 w-4" />
          {t('expenses.addExpense')}
        </button>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message={t('expenses.failedToLoad')} />
      ) : expenses.length === 0 ? (
        <div className="text-center py-12">
          <Receipt className="h-12 w-12 text-text-disabled mx-auto mb-3" />
          <p className="text-text-secondary mb-4">{t('expenses.empty')}</p>
          <button
            onClick={() => navigate(`/expenses/new?propertyId=${propertyId}`)}
            disabled={!canEditData}
            className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
          >
            <Plus className="h-4 w-4" />
            {t('expenses.createFirst')}
          </button>
        </div>
      ) : (
        <>
          {/* Search Bar */}
          <div className="mb-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted " />
              <input
                type="text"
                placeholder={t('expenses.searchPlaceholder')}
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
                className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-border-default">
              <thead className="bg-surface-page">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('expenses.table.expenseNumber')}
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('expenseDate')}
                  >
                    <div className="flex items-center gap-1">
                      {t('expenses.table.date')}
                      {renderSortIcon('expenseDate')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('description')}
                  >
                    <div className="flex items-center gap-1">
                      {t('expenses.table.description')}
                      {renderSortIcon('description')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('category')}
                  >
                    <div className="flex items-center gap-1">
                      {t('expenses.table.category')}
                      {renderSortIcon('category')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('amount')}
                  >
                    <div className="flex items-center gap-1">
                      {t('expenses.table.amount')}
                      {renderSortIcon('amount')}
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody className="bg-surface-card divide-y divide-border-default">
                {paginatedExpenses.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="px-6 py-12 text-center text-text-secondary"
                    >
                      {t('expenses.noMatchingSearch')}
                    </td>
                  </tr>
                ) : (
                  paginatedExpenses.map((expense) => (
                    <tr
                      key={expense.identifier}
                      className="hover:bg-primary-50 cursor-pointer"
                      onClick={() =>
                        navigate(`/expenses/${expense.identifier}`, {
                          state: {
                            backTo: `/properties/${propertyId}?tab=expenses`,
                          },
                        })
                      }
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-500 dark:text-primary-300">
                        #{expense.identifier}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                        {formatDate(expense.expenseDate)}
                      </td>
                      <td className="px-6 py-4 text-sm text-text-primary">
                        {expense.description}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <ExpenseCategoryBadge category={expense.category} />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-text-primary">
                        {expense.currency} {expense.amount.toFixed(2)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4 pt-4 border-t border-border-default">
              <div className="text-sm text-text-secondary">
                {t('expenses.pagination.showing', {
                  from: (currentPage - 1) * perPage + 1,
                  to: Math.min(
                    currentPage * perPage,
                    filteredAndSortedExpenses.length
                  ),
                  total: filteredAndSortedExpenses.length,
                })}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                >
                  {t('pagination.previous', { ns: 'common' })}
                </button>
                <span className="px-3 py-1 text-sm text-text-secondary">
                  {t('pagination.page', { ns: 'common' })} {currentPage}{' '}
                  {t('pagination.of', { ns: 'common' })} {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                >
                  {t('pagination.next', { ns: 'common' })}
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
