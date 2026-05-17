import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ExpenseCategory } from '@/types/expense';
import {
  useExpenses,
  useExpenseStats,
  useDeleteExpense,
} from '@/hooks/useExpenseHooks';
import {
  ConfirmDialog,
  DataList,
  EmptyState,
  FilterSheet,
  ListPageHeader,
  Pagination,
  RefreshButton,
  Skeleton,
  SwipeAction,
  type ListPageHeaderAction,
  type SwipeActionItem,
} from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { RefreshCw } from 'lucide-react';
import { usePagination } from '@/hooks/usePagination';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { PropertyCell } from '@/components/properties/PropertyCell';
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
import { EntityExportControls } from '@/components/common/EntityExportControls';
import { exportExpensesCsv, exportExpensesXlsx } from '@/api/listExports';
import { exportExpensesGoogleSheet } from '@/api/googleSheetsExport';
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

const categoryFilterValues = [
  undefined,
  ExpenseCategory.MAINTENANCE,
  ExpenseCategory.REPAIR,
  ExpenseCategory.UTILITY,
  ExpenseCategory.TAX,
  ExpenseCategory.INSURANCE,
  ExpenseCategory.LEGAL,
  ExpenseCategory.MARKETING,
  ExpenseCategory.CLEANING,
  ExpenseCategory.LANDSCAPING,
  ExpenseCategory.PROPERTY_MANAGEMENT,
  ExpenseCategory.FEES,
  ExpenseCategory.PROPERTY_TAX,
  ExpenseCategory.OTHER,
];

export const ExpensesPage = () => {
  const { t } = useTranslation('expenses');
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
    if (!currency) {
      return value.toFixed(2);
    }
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
      <div className="min-h-screen bg-background">
        <div className="px-4 py-8 space-y-6">
          {/* Header skeleton */}
          <div className="flex justify-between items-center">
            <div className="space-y-2">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-4 w-56" />
            </div>
            <Skeleton className="h-10 w-36 rounded" />
          </div>
          {/* Stats cards skeleton */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="bg-surface-card rounded-lg border border-border-default p-6 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-5 w-5 rounded" />
                </div>
                <Skeleton className="h-8 w-40" />
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
          {/* Filter bar skeleton */}
          <Skeleton className="h-32 w-full rounded-lg" />
          {/* Table rows skeleton */}
          <div className="space-y-2">
            <Skeleton className="h-10 w-full rounded" />
            {Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="h-14 w-full rounded" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message={t('errors.loadFailed')} />
      </div>
    );
  }

  const headerActions: ListPageHeaderAction[] = [
    {
      label: t('common:refresh', 'Refresh'),
      icon: RefreshCw,
      onClick: () => refetch(),
      showOn: 'mobile',
    },
    {
      label: 'desktop-actions',
      showOn: 'desktop',
      render: () => (
        <div className="flex items-center gap-2 flex-wrap">
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
          <EntityExportControls
            filenameStem="expenses"
            csv={exportExpensesCsv}
            xlsx={exportExpensesXlsx}
            googleSheet={exportExpensesGoogleSheet}
          />
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-4 md:py-8">
        <ListPageHeader
          title={t('page.title')}
          subtitle={t('page.subtitle')}
          icon={Receipt}
          mobileLeading={<MobileMenuButton />}
          actions={headerActions}
          primaryAction={{
            label: t('actions.addExpense'),
            icon: Plus,
            onClick: () => navigate('/expenses/new'),
            disabled: !canEditData,
          }}
        />

        {/* Metrics Dashboard */}
        {expenseStats && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Total Expenses */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 flex flex-col">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-text-secondary">
                  {t('stats.totalExpenses')}
                </h3>
                <DollarSign className="h-5 w-5 text-error-text" />
              </div>
              <div className="flex-1 flex flex-col justify-center">
                <p className="text-3xl font-bold text-text-primary">
                  {fmtMoney(expenseStats.totalAmount, statsCurrency)}
                </p>
                <p className="text-sm text-text-secondary mt-1">
                  {categoryFilter
                    ? t('stats.categorySummary', {
                        category: t(`category.${categoryFilter}`),
                      })
                    : t('stats.categorySummaryAll', {
                        count: expenseStats.topCategories.length,
                      })}
                </p>
              </div>
            </div>

            {/* Top Categories */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium text-text-secondary">
                  {t('stats.topCategories')}
                </h3>
                <PieChart className="h-5 w-5 text-primary-500" />
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
                            ? 'bg-primary-500'
                            : index === 1
                              ? 'bg-primary-400'
                              : 'bg-primary-300'
                        }`}
                      />
                      <span className="text-sm text-text-secondary truncate">
                        {t(`category.${cat.category}`)}
                      </span>
                    </div>
                    <span className="text-sm font-semibold text-text-primary">
                      {fmtMoney(cat.total, statsCurrency)}
                    </span>
                  </div>
                ))}
                {expenseStats.topCategories.length === 0 && (
                  <p className="text-sm text-text-secondary">
                    {t('stats.noData')}
                  </p>
                )}
              </div>
            </div>

            {/* 6-Month Expenses Chart */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-text-secondary">
                  {t('stats.lastSixMonths')}
                </h3>
                <TrendingDown className="h-5 w-5 text-error-text" />
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
                    formatter={(value) => [
                      typeof value === 'number'
                        ? fmtMoney(value, statsCurrency)
                        : 'N/A',
                      t('stats.tooltipExpenses'),
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

        {/* Phone: filter trigger + sheet */}
        <div className="md:hidden mb-4 flex justify-end">
          <FilterSheet
            activeCount={(propertyFilter ? 1 : 0) + (categoryFilter ? 1 : 0)}
            onClear={() => {
              setPropertyFilter(undefined);
              setCategoryFilter(undefined);
              setPeriodRange(null);
              resetPage();
            }}
            triggerLabel={t('filters.title')}
            collapseBelow="md"
          >
            <ExpensesFilterContent
              categoryFilterValues={categoryFilterValues}
              categoryFilter={categoryFilter}
              setCategoryFilter={setCategoryFilter}
              propertyFilter={propertyFilter}
              setPropertyFilter={setPropertyFilter}
              setPeriodRange={setPeriodRange}
              resetPage={resetPage}
              t={t}
            />
          </FilterSheet>
        </div>

        {/* Filter Bar (md+) */}
        <div className="hidden md:block mb-6 bg-surface-card rounded-lg border border-border-default p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h3 className="font-semibold text-text-primary">
              {t('filters.title')}
            </h3>
          </div>

          <div className="flex flex-col gap-4">
            {/* Row 1: Property selector + Period filter */}
            <div className="flex flex-col lg:flex-row gap-4 items-end">
              <div className="lg:w-96">
                <label className="block text-xs font-medium text-text-secondary mb-1">
                  {t('filters.property')}
                </label>
                <PropertySelector
                  value={propertyFilter ?? ''}
                  onChange={(id) => {
                    setPropertyFilter(id || undefined);
                    resetPage();
                  }}
                  clearable
                  placeholder={t('filters.allProperties')}
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
              <label className="block text-xs font-medium text-text-secondary mb-1">
                {t('filters.category')}
              </label>
              <div className="flex gap-2 flex-wrap">
                {categoryFilterValues.map((value) => (
                  <button
                    key={value ?? 'all'}
                    onClick={() => {
                      setCategoryFilter(value);
                      resetPage();
                    }}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      categoryFilter === value
                        ? 'bg-primary-500 text-white'
                        : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
                    }`}
                  >
                    {value
                      ? t(`category.${value}`)
                      : t('filters.allCategories')}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Expenses — Mobile card list (<md). md+ shows the existing table below. */}
        {expensesData?.content && expensesData.content.length > 0 && (
          <ul className="md:hidden space-y-3 mb-4">
            {expensesData.content.map((expense) => {
              const leftActions: SwipeActionItem[] = canEditData
                ? [
                    {
                      label: t('actions.delete', { defaultValue: 'Delete' }),
                      icon: Trash2,
                      tone: 'danger',
                      onAction: () => setDeleteTarget(expense.identifier),
                    },
                  ]
                : [];
              return (
                <li key={`m-${expense.identifier}`}>
                  <SwipeAction
                    leftActions={leftActions}
                    onClick={() => navigate(`/expenses/${expense.identifier}`)}
                  >
                    <div className="bg-surface-card border border-border-default p-4 min-h-touch">
                      <DataList
                        title={expense.description}
                        trailing={
                          <ExpenseCategoryBadge category={expense.category} />
                        }
                        items={[
                          {
                            label: t('table.date'),
                            value: formatDate(expense.expenseDate),
                          },
                          {
                            label: t('table.property'),
                            value: `${expense.property.street}, ${expense.property.city}`,
                          },
                          {
                            label: t('table.amount'),
                            value: (
                              <span className="font-semibold text-text-primary">
                                {fmtMoney(expense.amount, expense.currency)}
                              </span>
                            ),
                            align: 'right',
                          },
                        ]}
                      />
                    </div>
                  </SwipeAction>
                </li>
              );
            })}
          </ul>
        )}

        {/* Expenses Table — md+ */}
        {expensesData?.content && expensesData.content.length > 0 ? (
          <>
            <div className="hidden md:block bg-surface-card rounded-lg shadow-sm border border-border-default overflow-hidden mb-4">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-border-default">
                  <thead className="bg-surface-page">
                    <tr>
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('expenseDate')}
                      >
                        <div className="flex items-center gap-1">
                          {t('table.date')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                        {t('table.expenseNumber')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                        {t('table.description')}
                      </th>
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('category')}
                      >
                        <div className="flex items-center gap-1">
                          {t('table.category')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider min-w-[220px]">
                        {t('table.property')}
                      </th>
                      <th
                        className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('amount')}
                      >
                        <div className="flex items-center justify-end gap-1">
                          {t('table.amount')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider"></th>
                    </tr>
                  </thead>
                  <tbody className="bg-surface-card divide-y divide-border-default">
                    {expensesData.content.map((expense) => (
                      <tr
                        key={expense.identifier}
                        className="hover:bg-primary-50 cursor-pointer"
                        onClick={() =>
                          navigate(`/expenses/${expense.identifier}`)
                        }
                      >
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                          {formatDate(expense.expenseDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-text-primary">
                          {expense.identifier}
                        </td>
                        <td className="px-6 py-4 text-sm text-text-primary">
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
                          <span className="text-sm font-semibold text-text-primary">
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
                              className="p-1.5 rounded hover:bg-surface-inset text-text-secondary hover:text-primary-500 transition-colors"
                              title={t('tooltips.viewExpense')}
                            >
                              <Eye className="h-4 w-4" />
                            </button>
                            {canEditData && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setDeleteTarget(expense.identifier);
                                }}
                                className="p-1.5 rounded hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                                title={t('tooltips.deleteExpense')}
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
          <div className="bg-surface-card rounded-lg border border-border-default">
            <EmptyState
              variant="page"
              icon={<Receipt className="h-12 w-12" />}
              title={t('empty.title')}
              description={
                categoryFilter || propertyFilter
                  ? t('empty.filtered')
                  : t('empty.noData')
              }
              actions={
                !categoryFilter && !propertyFilter ? (
                  <button
                    onClick={() => navigate('/expenses/new')}
                    disabled={!canEditData}
                    className="bg-primary-500 text-white px-4 py-2 rounded min-h-touch hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500 focus-ring"
                  >
                    <Plus className="h-5 w-5" />
                    {t('actions.addExpense')}
                  </button>
                ) : undefined
              }
            />
          </div>
        )}
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title={t('deleteDialog.title')}
          message={t('deleteDialog.messageWithRelated')}
          confirmLabel={t('buttons.delete', { ns: 'common' })}
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

interface ExpensesFilterContentProps {
  categoryFilterValues: (ExpenseCategory | undefined)[];
  categoryFilter: ExpenseCategory | undefined;
  setCategoryFilter: (v: ExpenseCategory | undefined) => void;
  propertyFilter: string | undefined;
  setPropertyFilter: (v: string | undefined) => void;
  setPeriodRange: (range: PeriodDateRange | null) => void;
  resetPage: () => void;
  t: (key: string) => string;
}

const ExpensesFilterContent = ({
  categoryFilterValues,
  categoryFilter,
  setCategoryFilter,
  propertyFilter,
  setPropertyFilter,
  setPeriodRange,
  resetPage,
  t,
}: ExpensesFilterContentProps) => (
  <div className="flex flex-col gap-4">
    <div>
      <label className="block text-xs font-medium text-text-secondary mb-1">
        {t('filters.property')}
      </label>
      <PropertySelector
        value={propertyFilter ?? ''}
        onChange={(id) => {
          setPropertyFilter(id || undefined);
          resetPage();
        }}
        clearable
        placeholder={t('filters.allProperties')}
      />
    </div>
    <PeriodFilter
      presets={['month', 'quarter', 'year', 'all', 'custom']}
      defaultPreset="all"
      onChange={(range) => {
        setPeriodRange(range);
        resetPage();
      }}
    />
    <div>
      <label className="block text-xs font-medium text-text-secondary mb-1">
        {t('filters.category')}
      </label>
      <div className="flex gap-2 flex-wrap">
        {categoryFilterValues.map((value) => (
          <button
            key={value ?? 'all'}
            onClick={() => {
              setCategoryFilter(value);
              resetPage();
            }}
            className={`px-4 py-2 rounded transition-colors text-sm min-h-touch ${
              categoryFilter === value
                ? 'bg-primary-500 text-white'
                : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
            }`}
          >
            {value ? t(`category.${value}`) : t('filters.allCategories')}
          </button>
        ))}
      </div>
    </div>
  </div>
);
