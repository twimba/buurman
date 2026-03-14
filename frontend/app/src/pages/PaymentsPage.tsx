import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PaymentStatus } from '@/types/payment';
import {
  usePayments,
  usePaymentStats,
  useDeletePayment,
} from '@/hooks/usePaymentHooks';
import { ConfirmDialog, Pagination } from '@buurman/ui';
import { usePagination } from '@/hooks/usePagination';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { ContractCell } from '@/components/contracts/ContractCell';
import { LoadingSpinner } from '@/components/LoadingSpinner';
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
import { RefreshButton } from '@buurman/ui';

const statusFilters = [
  { value: undefined, label: 'All Statuses' },
  { value: PaymentStatus.PENDING, label: 'Pending' },
  { value: PaymentStatus.PARTIALLY_PAID, label: 'Partial' },
  { value: PaymentStatus.PAID, label: 'Paid' },
  { value: PaymentStatus.OVERDUE, label: 'Overdue' },
  { value: PaymentStatus.CANCELLED, label: 'Cancelled' },
];

export const PaymentsPage = () => {
  const navigate = useNavigate();
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
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load payments" />
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
              <h1 className="text-3xl font-bold text-text-primary">Payments</h1>
            </div>
            <p className="text-text-secondary ml-11">
              Track rent payments and income
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
              Schedule Payment
            </button>
            <button
              onClick={() => navigate('/payments/new?register=true')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <CalendarCheck className="h-5 w-5" />
              Register Payment
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
                  Pending Payments
                </h3>
                <Clock className="h-5 w-5 text-warning-text" />
              </div>
              <p className="text-3xl font-bold text-text-primary">
                {fmtMoney(paymentStats.pendingAmount, statsCurrency)}
              </p>
              <p className="text-sm text-text-secondary mt-1">
                {paymentStats.pendingCount} payment
                {paymentStats.pendingCount !== 1 ? 's' : ''}
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
                  Overdue Payments
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
                    {paymentStats.overdueCount} payment
                    {paymentStats.overdueCount !== 1 ? 's' : ''} past due
                  </p>
                  <p className="text-xs text-error-text mt-2">
                    Action required: Review overdue payments
                  </p>
                </>
              ) : (
                <>
                  <p className="text-3xl font-bold text-success-text">
                    {fmtMoney(0, statsCurrency)}
                  </p>
                  <p className="text-sm text-text-secondary mt-1">
                    All caught up!
                  </p>
                  <p className="text-xs text-text-secondary mt-2">
                    No overdue payments. Keep up the great work!
                  </p>
                </>
              )}
            </div>

            {/* 6-Month Revenue Chart */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-text-secondary">
                  Last 6 Months
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
                      'Received',
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
            <h3 className="font-semibold text-text-primary">Filters</h3>
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

              {/* Contract Filter */}
              <div className="lg:w-72">
                <label className="block text-xs font-medium text-text-secondary mb-1">
                  Contract
                </label>
                <ContractSelector
                  value={contractFilter ?? ''}
                  onChange={(id) => {
                    setContractFilter(id || undefined);
                    resetPage();
                  }}
                  status={undefined}
                  clearable
                  placeholder="All Contracts"
                />
              </div>

              {/* Status Filter */}
              <div className="flex-1">
                <label className="block text-xs font-medium text-text-secondary mb-1">
                  Status
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
                        Due Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Payment #
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider min-w-[280px]">
                      Contract
                    </th>
                    <th
                      className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('amount')}
                    >
                      <div className="flex items-center justify-end gap-1">
                        Amount
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('status')}
                    >
                      <div className="flex items-center gap-1">
                        Status
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
                          tenantFirstName={payment.tenant.firstName}
                          tenantLastName={payment.tenant.lastName}
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
                                Balance:{' '}
                                {fmtMoney(
                                  payment.balance ?? 0,
                                  payment.currency
                                )}
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
                            title="View payment"
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
                              title="Delete payment"
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
              No payments found
            </h3>
            <p className="text-text-secondary mb-6">
              {statusFilter || propertyFilter || contractFilter
                ? 'Try adjusting your filters'
                : 'Get started by recording your first payment'}
            </p>
            {!statusFilter && !propertyFilter && !contractFilter && (
              <div className="flex items-center gap-2 justify-center">
                <button
                  onClick={() => navigate('/payments/new')}
                  disabled={!canEditData}
                  className="text-text-secondary border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Plus className="h-5 w-5" />
                  Schedule Payment
                </button>
                <button
                  onClick={() => navigate('/payments/new?register=true')}
                  disabled={!canEditData}
                  className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
                >
                  <CalendarCheck className="h-5 w-5" />
                  Register Payment
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Delete Payment"
          message="Are you sure you want to delete this payment? All related data (receivals, documents) will also be deleted. This action cannot be undone."
          confirmLabel="Delete"
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
