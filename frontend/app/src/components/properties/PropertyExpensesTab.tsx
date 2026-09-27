import { Fragment, useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useExpensesByProperty } from '@/hooks/useExpenseHooks';
import { useProperty } from '@/hooks/usePropertyHooks';
import {
  useExpenseAllocations,
  useUnitDetailsBatch,
  useUpdatePropertyAllocation,
} from '@/hooks/useAllocationHooks';
import {
  UnitAllocationSettings,
  type UnitAllocationUnit,
} from '@/components/units/UnitAllocationSettings';
import { AllocationBasis } from '@/types/allocation';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { ErrorMessage } from '@/components/ErrorMessage';
import { DataList, EmptyState, LoadingSpinner } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import { formatMoney } from '@/utils/formatMoney';
import {
  Plus,
  Search,
  ChevronUp,
  ChevronDown,
  ChevronRight,
  Receipt,
} from 'lucide-react';

interface PropertyExpensesTabProps {
  propertyId: string;
}

/** The stored, backend-computed per-unit split of one building-level expense. Read-only — never
 * recomputed here. This is what a tenant's service-charge statement will show. */
const ExpenseAllocationSplit = ({
  expenseIdentifier,
}: {
  expenseIdentifier: string;
}) => {
  const { t } = useTranslation('properties');
  const { t: tUnits } = useTranslation('units');
  const {
    data: allocations,
    isLoading,
    error,
  } = useExpenseAllocations(expenseIdentifier);

  if (isLoading) {
    return <LoadingSpinner />;
  }
  if (error || !allocations) {
    return <ErrorMessage message={t('expenses.allocation.failedToLoad')} />;
  }

  const warnings = allocations.flatMap((a) => a.warnings);

  return (
    <div className="text-sm">
      <ul className="divide-y divide-border-default">
        {allocations.map((a) => (
          <li key={a.identifier} className="flex justify-between py-1">
            <span className="text-text-secondary">
              {tUnits('detail.title', { number: a.unitNumber })}
            </span>
            <span className="font-medium text-text-primary">
              {a.amount != null
                ? formatMoney(a.amount, a.amountCurrency ?? '')
                : '—'}
            </span>
          </li>
        ))}
      </ul>
      {warnings.length > 0 && (
        <p className="mt-2 text-xs text-warning-text">{warnings.join(' ')}</p>
      )}
    </div>
  );
};

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
  const [expandedExpenseId, setExpandedExpenseId] = useState<string | null>(
    null
  );
  const [showAllocationSettings, setShowAllocationSettings] = useState(false);
  const perPage = 10;

  // Data fetching
  const {
    data: expenses = [],
    isLoading,
    error,
  } = useExpensesByProperty(propertyId);

  // Allocation is only meaningful for a building split across several units — a single-unit
  // property never shows any of this, so the landlord of a single-family house never has to
  // think about "units" to record an expense.
  const { data: property } = useProperty(propertyId);
  const unitCount = property?.unitCount ?? 1;
  const isMultiUnit = unitCount > 1;
  const unitIdentifiers = useMemo(
    () => (property?.units ?? []).map((u) => u.identifier),
    [property]
  );
  const unitDetails = useUnitDetailsBatch(
    unitIdentifiers,
    isMultiUnit && showAllocationSettings
  );
  const allocationUnitsLoading = unitDetails.some((q) => q.isLoading);
  const allocationUnits: UnitAllocationUnit[] = unitIdentifiers.map(
    (identifier, index) => {
      const summary = property?.units?.[index];
      const detail = unitDetails[index]?.data;
      return {
        identifier,
        unitNumber: summary?.unitNumber ?? '',
        name: summary?.name,
        areaValue: detail?.areaValue ?? null,
        sharePct: detail?.allocationShare ?? null,
      };
    }
  );
  const updateAllocation = useUpdatePropertyAllocation(propertyId);

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
          (expense.description?.toLowerCase().includes(search) ?? false) ||
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
          aVal = a.description ?? '';
          bVal = b.description ?? '';
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

      {isMultiUnit && (
        <div className="mb-6 pb-4 border-b border-border-default">
          <button
            type="button"
            onClick={() => setShowAllocationSettings((s) => !s)}
            className="flex items-center gap-1 text-sm font-medium text-primary-500 hover:text-primary-600"
          >
            {showAllocationSettings ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
            {t('expenses.allocation.settingsToggle')}
          </button>
          {showAllocationSettings &&
            (allocationUnitsLoading ? (
              <LoadingSpinner />
            ) : (
              <div className="mt-4">
                <UnitAllocationSettings
                  propertyIdentifier={propertyId}
                  basis={property?.allocationBasis ?? AllocationBasis.EQUAL}
                  units={allocationUnits}
                  onSave={(request) => updateAllocation.mutate(request)}
                  isSaving={updateAllocation.isPending}
                  disabled={!canEditData}
                />
              </div>
            ))}
        </div>
      )}

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message={t('expenses.failedToLoad')} />
      ) : expenses.length === 0 ? (
        <EmptyState
          variant="section"
          icon={<Receipt className="h-10 w-10" />}
          title={t('expenses.empty')}
          actions={
            <button
              onClick={() => navigate(`/expenses/new?propertyId=${propertyId}`)}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded min-h-touch hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500 focus-ring"
            >
              <Plus className="h-4 w-4" />
              {t('expenses.createFirst')}
            </button>
          }
        />
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

          {/* Mobile card list (<md) */}
          <ul className="md:hidden space-y-3 mb-2">
            {paginatedExpenses.length === 0 ? (
              <li className="text-center text-text-secondary py-6">
                {t('expenses.noMatchingSearch')}
              </li>
            ) : (
              paginatedExpenses.map((expense) => (
                <li key={`m-${expense.identifier}`}>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(`/expenses/${expense.identifier}`, {
                        state: {
                          backTo: `/properties/${propertyId}?tab=expenses`,
                        },
                      })
                    }
                    className="block w-full text-left bg-surface-card rounded-lg border border-border-default p-4 min-h-touch hover:border-primary-300 transition-colors focus-ring"
                  >
                    <DataList
                      title={expense.description}
                      trailing={
                        <div className="flex flex-col items-end gap-1">
                          <ExpenseCategoryBadge category={expense.category} />
                          {isMultiUnit && !expense.unitIdentifier && (
                            <span className="text-xs text-text-muted">
                              {t('expenses.allocation.buildingBadge')}
                            </span>
                          )}
                        </div>
                      }
                      items={[
                        {
                          label: t('expenses.table.date'),
                          value: formatDate(expense.expenseDate),
                        },
                        {
                          label: t('expenses.table.amount'),
                          value: (
                            <span className="font-semibold text-text-primary">
                              {expense.currency} {expense.amount.toFixed(2)}
                            </span>
                          ),
                          align: 'right',
                        },
                      ]}
                    />
                  </button>
                </li>
              ))
            )}
          </ul>

          {/* Table (md+) */}
          <div className="hidden md:block overflow-x-auto">
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
                  paginatedExpenses.map((expense) => {
                    const isBuildingLevel =
                      isMultiUnit && !expense.unitIdentifier;
                    const isExpanded = expandedExpenseId === expense.identifier;
                    return (
                      <Fragment key={expense.identifier}>
                        <tr
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
                            <div className="flex items-center gap-2">
                              <ExpenseCategoryBadge
                                category={expense.category}
                              />
                              {isBuildingLevel && (
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setExpandedExpenseId(
                                      isExpanded ? null : expense.identifier
                                    );
                                  }}
                                  className="text-xs text-primary-500 hover:underline"
                                >
                                  {isExpanded
                                    ? t('expenses.allocation.hideSplit')
                                    : t('expenses.allocation.viewSplit')}
                                </button>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-text-primary">
                            {expense.currency} {expense.amount.toFixed(2)}
                          </td>
                        </tr>
                        {isBuildingLevel && isExpanded && (
                          <tr>
                            <td
                              colSpan={5}
                              className="px-6 py-4 bg-surface-inset"
                            >
                              <ExpenseAllocationSplit
                                expenseIdentifier={expense.identifier}
                              />
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })
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
