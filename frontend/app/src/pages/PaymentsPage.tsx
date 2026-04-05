import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PaymentStatus } from '@/types/payment';
import {
  usePayments,
  usePaymentStats,
  useDeletePayment,
} from '@/hooks/usePaymentHooks';
import {
  ConfirmDialog,
  Pagination,
  RefreshButton,
  Skeleton,
} from '@buurman/ui';
import { usePagination } from '@/hooks/usePagination';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { ContractCell } from '@/components/contracts/ContractCell';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ContractSelector } from '@/components/common/ContractSelector';
import { PropertySelector } from '@/components/common/PropertySelector';
import {
  PeriodFilter,
  PeriodDateRange,
} from '@/components/common/PeriodFilter';
import {
  Plus,
  CalendarCheck,
  DollarSign,
  AlertTriangle,
  Filter,
  ArrowUpDown,
  Clock,
  CheckCircle,
  TrendingUp,
  Eye,
  Trash2,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';

export const PaymentsPage = () => {
  const navigate = useNavigate();
  const { t } = useTranslation('payments');
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const deletePaymentMutation = useDeletePayment();
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | undefined>(
    undefined
  );
  const [contractFilter, setContractFilter] = useState<string | undefined>(
    undefined
  );
  const [propertyFilter, setPropertyFilter] = useState<string | undefined>(
    undefined
  );
  const [periodRange, setPeriodRange] = useState<PeriodDateRange | null>(null);

  const statusFilters = useMemo(
    () => [
      { value: undefined, label: t('filters.allStatuses') },
      { value: PaymentStatus.PENDING, label: t('status.pending') },
      { value: PaymentStatus.PARTIALLY_PAID, label: t('status.partiallyPaid') },
      { value: PaymentStatus.PAID, label: t('status.paid') },
      { value: PaymentStatus.OVERDUE, label: t('status.overdue') },
      { value: PaymentStatus.LATE, label: t('status.late') },
      { value: PaymentStatus.CANCELLED, label: t('status.cancelled') },
    ],
    [t]
  );

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({ defaultSort: 'dueDate' });

  const {
    data: paymentsData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = usePayments({
    status: statusFilter,
    contractIdentifier: contractFilter,
    propertyIdentifier: propertyFilter,
    dateFrom: periodRange?.startDate,
    dateTo: periodRange?.endDate,
    ...pageParams,
  });

  const { data: paymentStats } = usePaymentStats();

  const statsCurrency = paymentStats?.currency ?? '';

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
            <div className="flex items-center gap-2">
              <Skeleton className="h-10 w-36 rounded" />
              <Skeleton className="h-10 w-40 rounded" />
            </div>
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

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <DollarSign className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">{t('page.title')}</h1>
            </div>
            <p className="text-text-secondary ml-11">
              {t('page.subtitle')}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <button
              onClick={() => navigate('/payments/new')}
              disabled={!canEditData}
              className="text-text-secondary border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Plus className="h-5 w-5" />
              {t('actions.schedulePayment')}
            </button>
            <button
              onClick={() => navigate('/payments/new?register=true')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <CalendarCheck className="h-5 w-5" />
              {t('actions.registerPayment')}
            </button>
          </div>
        </div>

        {/* Metrics Dashboard */}
        {paymentStats && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Pending Payments */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-text-secondary">
                  {t('stats.pendingPayments')}
                </h3>
                <Clock className="h-5 w-5 text-warning-text" />
              </div>
              <p className="text-3xl font-bold text-text-primary">
                {fmtMoney(paymentStats.pendingAmount, statsCurrency)}
              </p>
              <p className="text-sm text-text-secondary mt-1">
                {t('stats.pendingCount', { count: paymentStats.pendingCount })}
              </p>
            </div>

            {/* Overdue Payments */}
            <div
              className={`rounded-lg shadow-sm border border-border-default p-6 transition-colors ${
                paymentStats.overdueCount > 0
                  ? 'bg-error-bg border-2 border-error-border'
                  : 'bg-surface-card'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <h3
                  className={`text-sm font-medium ${
                    paymentStats.overdueCount > 0
                      ? 'text-error-text'
                      : 'text-text-secondary'
                  }`}
                >
                  {t('stats.overduePayments')}
                </h3>
                {paymentStats.overdueCount > 0 ? (
                  <AlertTriangle className="h-5 w-5 text-error-text animate-pulse" />
                ) : (
                  <CheckCircle className="h-5 w-5 text-success-text" />
                )}
              </div>
              {paymentStats.overdueCount > 0 ? (
                <>
                  <p className="text-3xl font-bold text-error-text">
                    {fmtMoney(paymentStats.overdueAmount, statsCurrency)}
                  </p>
                  <p className="text-sm text-error-text mt-1 font-medium">
                    {t('stats.overdueCount', { count: paymentStats.overdueCount })}
                  </p>
                  <p className="text-xs text-error-text mt-2">
                    {t('stats.overdueAction')}
                  </p>
                </>
              ) : (
                <>
                  <p className="text-3xl font-bold text-success-text">
                    {fmtMoney(0, statsCurrency)}
                  </p>
                  <p className="text-sm text-text-secondary mt-1">
                    {t('stats.allCaughtUp')}
                  </p>
                  <p className="text-xs text-text-secondary mt-2">
                    {t('stats.noOverdue')}
                  </p>
                </>
              )}
            </div>

            {/* 6-Month Revenue Chart */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-text-secondary">
                  {t('stats.lastSixMonths')}
                </h3>
                <TrendingUp className="h-5 w-5 text-success-text" />
              </div>
              <ResponsiveContainer width="100%" height={80}>
                <AreaChart data={paymentStats.monthlyTrend}>
                  <defs>
                    <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
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
                      t('stats.tooltipReceived'),
                    ]}
                    contentStyle={{ fontSize: 12 }}
                  />
                  <Area
                    type="monotone"
                    dataKey="total"
                    stroke="#10b981"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#colorTotal)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* Filter Bar */}
        <div className="mb-6 bg-surface-card rounded-lg border border-border-default p-4">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h3 className="font-semibold text-text-primary">{t('filters.title')}</h3>
          </div>

          <div className="flex flex-col gap-4">
            {/* Period Filter */}
            <PeriodFilter
              presets={['month', 'quarter', 'year', 'all', 'custom']}
              defaultPreset="all"
              onChange={(range) => {
                setPeriodRange(range);
                resetPage();
              }}
            />

            <div className="flex flex-col lg:flex-row gap-4">
              {/* Property Filter */}
              <div className="lg:w-72">
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

              {/* Contract Filter */}
              <div className="lg:w-72">
                <label className="block text-xs font-medium text-text-secondary mb-1">
                  {t('filters.contract')}
                </label>
                <ContractSelector
                  value={contractFilter ?? ''}
                  onChange={(id) => {
                    setContractFilter(id || undefined);
                    resetPage();
                  }}
                  status={undefined}
                  clearable
                  placeholder={t('filters.allContracts')}
                />
              </div>

              {/* Status Filter */}
              <div className="flex-1">
                <label className="block text-xs font-medium text-text-secondary mb-1">
                  {t('filters.status')}
                </label>
                <div className="flex gap-2 flex-wrap">
                  {statusFilters.map((filter) => (
                    <button
                      key={filter.label}
                      onClick={() => {
                        setStatusFilter(filter.value);
                        resetPage();
                      }}
                      className={`px-4 py-2 rounded transition-colors text-sm ${
                        statusFilter === filter.value
                          ? 'bg-primary-500 text-white'
                          : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
                      }`}
                    >
                      {filter.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Payments Table */}
        {paymentsData?.content && paymentsData.content.length > 0 ? (
          <>
            <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-border-default">
                <thead className="bg-surface-page">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('dueDate')}
                    >
                      <div className="flex items-center gap-1">
                        {t('table.dueDate')}
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      {t('table.paymentNumber')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider min-w-[280px]">
                      {t('table.contract')}
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
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('status')}
                    >
                      <div className="flex items-center gap-1">
                        {t('table.status')}
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider"></th>
                  </tr>
                </thead>
                <tbody className="bg-surface-card divide-y divide-border-default">
                  {paymentsData.content.map((payment) => (
                    <tr
                      key={payment.identifier}
                      className="hover:bg-primary-50 cursor-pointer"
                      onClick={() =>
                        navigate(`/payments/${payment.identifier}`)
                      }
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                        {formatDate(payment.dueDate)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm font-medium text-text-primary">
                          #{payment.identifier}
                        </span>
                      </td>
                      <td className="px-6 py-3">
                        <ContractCell
                          contractIdentifier={payment.contract.identifier}
                          contractStatus={payment.contract.status}
                          propertyStreet={payment.property.street}
                          propertyCity={payment.property.city}
                          contactFirstName={
                            payment.contact.firstName ??
                            payment.contact.displayName
                          }
                          contactLastName={payment.contact.lastName}
                        />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right">
                        <div>
                          <span className="text-sm font-semibold text-text-primary">
                            {fmtMoney(payment.amount, payment.currency)}
                          </span>
                          {payment.receivedAmount > 0 &&
                            payment.status !== PaymentStatus.PAID && (
                              <p className="text-xs text-text-secondary">
                                {t('table.balance', { amount: fmtMoney(payment.balance ?? 0, payment.currency) })}
                              </p>
                            )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <PaymentStatusBadge status={payment.status} />
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              navigate(`/payments/${payment.identifier}`);
                            }}
                            className="p-1.5 rounded hover:bg-neutral-100 text-text-secondary hover:text-primary-500 transition-colors"
                            title={t('tooltips.viewPayment')}
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                          {canEditData && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeleteTarget(payment.identifier);
                              }}
                              className="p-1.5 rounded hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                              title={t('tooltips.deletePayment')}
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

            {paymentsData && (
              <Pagination
                page={page}
                totalPages={paymentsData.totalPages}
                totalElements={paymentsData.totalElements}
                size={size}
                onPageChange={handlePageChange}
                onSizeChange={handleSizeChange}
              />
            )}
          </>
        ) : (
          <div className="bg-surface-card rounded-lg border border-border-default p-12 text-center">
            <DollarSign className="h-12 w-12 text-text-muted mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              {t('empty.title')}
            </h3>
            <p className="text-text-secondary mb-6">
              {statusFilter || propertyFilter || contractFilter
                ? t('empty.filtered')
                : t('empty.noData')}
            </p>
            {!statusFilter && !propertyFilter && !contractFilter && (
              <div className="flex items-center gap-2 justify-center">
                <button
                  onClick={() => navigate('/payments/new')}
                  disabled={!canEditData}
                  className="text-text-secondary border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Plus className="h-5 w-5" />
                  {t('actions.schedulePayment')}
                </button>
                <button
                  onClick={() => navigate('/payments/new?register=true')}
                  disabled={!canEditData}
                  className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
                >
                  <CalendarCheck className="h-5 w-5" />
                  {t('actions.registerPayment')}
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title={t('deleteDialog.title')}
          message={t('deleteDialog.messageWithRelated')}
          confirmLabel={t('common:buttons.delete')}
          variant="danger"
          isLoading={deletePaymentMutation.isPending}
          onConfirm={async () => {
            await deletePaymentMutation.mutateAsync(deleteTarget);
            setDeleteTarget(null);
          }}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
};
