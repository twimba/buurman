import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PaymentStatus } from '@/types/payment';
import { usePayments, usePaymentStats } from '@/hooks/usePaymentHooks';
import { usePagination } from '@/hooks/usePagination';
import { Pagination } from '@/components/ui/Pagination';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { ContractCell } from '@/components/contracts/ContractCell';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  DollarSign,
  AlertTriangle,
  Filter,
  ArrowUpDown,
  Clock,
  CheckCircle,
  TrendingUp,
  Eye,
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
import { getCurrencySymbol } from '@/utils/currencies';
import { RefreshButton } from '@/components/ui/RefreshButton';

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
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | undefined>(
    undefined
  );

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({ defaultSort: 'dueDate', defaultDirection: 'asc' });

  const {
    data: paymentsData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = usePayments({ status: statusFilter, ...pageParams });

  const { data: paymentStats } = usePaymentStats();

  const currencySymbol = getCurrencySymbol(paymentStats?.currency ?? 'EUR');

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
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Payments
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
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
              className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
            >
              <Plus className="h-5 w-5" />
              Add Payment
            </button>
          </div>
        </div>

        {/* Metrics Dashboard */}
        {paymentStats && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Pending Payments */}
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
                  Pending Payments
                </h3>
                <Clock className="h-5 w-5 text-yellow-500" />
              </div>
              <p className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {currencySymbol} {paymentStats.pendingAmount.toFixed(2)}
              </p>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                {paymentStats.pendingCount} payment
                {paymentStats.pendingCount !== 1 ? 's' : ''}
              </p>
            </div>

            {/* Overdue Payments */}
            <div
              className={`rounded-xl shadow-sm p-6 transition-colors ${
                paymentStats.overdueCount > 0
                  ? 'bg-gradient-to-br from-red-50 to-red-100 dark:from-red-900/30 dark:to-red-800/30 border-2 border-red-200 dark:border-red-800'
                  : 'bg-white dark:bg-[#14161f]'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <h3
                  className={`text-sm font-medium ${
                    paymentStats.overdueCount > 0
                      ? 'text-red-700 dark:text-red-300'
                      : 'text-[#6b7194] dark:text-[#8b90a8]'
                  }`}
                >
                  Overdue Payments
                </h3>
                {paymentStats.overdueCount > 0 ? (
                  <AlertTriangle className="h-5 w-5 text-red-600 animate-pulse" />
                ) : (
                  <CheckCircle className="h-5 w-5 text-green-500" />
                )}
              </div>
              {paymentStats.overdueCount > 0 ? (
                <>
                  <p className="text-3xl font-bold text-red-600 dark:text-red-400">
                    {currencySymbol} {paymentStats.overdueAmount.toFixed(2)}
                  </p>
                  <p className="text-sm text-red-700 dark:text-red-300 mt-1 font-medium">
                    {paymentStats.overdueCount} payment
                    {paymentStats.overdueCount !== 1 ? 's' : ''} past due
                  </p>
                  <p className="text-xs text-red-600 dark:text-red-400 mt-2">
                    Action required: Review overdue payments
                  </p>
                </>
              ) : (
                <>
                  <p className="text-3xl font-bold text-green-600 dark:text-green-400">
                    {currencySymbol} 0.00
                  </p>
                  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                    All caught up!
                  </p>
                  <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-2">
                    No overdue payments. Keep up the great work!
                  </p>
                </>
              )}
            </div>

            {/* 6-Month Revenue Chart */}
            <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
                  Last 6 Months
                </h3>
                <TrendingUp className="h-5 w-5 text-green-500" />
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
                    formatter={(value: number | undefined) => [
                      value !== undefined
                        ? `${currencySymbol} ${value.toFixed(2)}`
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
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div className="flex items-center gap-2 mb-2">
            <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
            <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Status Filter
            </h3>
          </div>
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
                    ? 'bg-[#5c7cfa] text-white'
                    : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54]'
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>

        {/* Payments Table */}
        {paymentsData?.content && paymentsData.content.length > 0 ? (
          <>
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
                <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSortChange('dueDate')}
                    >
                      <div className="flex items-center gap-1">
                        Due Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Payment #
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider min-w-[280px]">
                      Contract
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
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSortChange('status')}
                    >
                      <div className="flex items-center gap-1">
                        Status
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider"></th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                  {paymentsData.content.map((payment) => (
                    <tr
                      key={payment.identifier}
                      className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                      onClick={() =>
                        navigate(`/payments/${payment.identifier}`)
                      }
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(payment.dueDate)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
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
                          <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                            {getCurrencySymbol(payment.currency)}{' '}
                            {payment.amount.toFixed(2)}
                          </span>
                          {payment.receivedAmount > 0 &&
                            payment.status !== PaymentStatus.PAID && (
                              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                                Balance: {getCurrencySymbol(payment.currency)}{' '}
                                {(payment.balance ?? 0).toFixed(2)}
                              </p>
                            )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <PaymentStatusBadge status={payment.status} />
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap text-right">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/payments/${payment.identifier}`);
                          }}
                          className="p-1.5 rounded hover:bg-[#e8ecf4] dark:hover:bg-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#748ffc] transition-colors"
                          title="View payment"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
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
          <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] p-12 text-center">
            <DollarSign className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No payments found
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
              {statusFilter
                ? 'Try adjusting your filters'
                : 'Get started by recording your first payment'}
            </p>
            {!statusFilter && (
              <button
                onClick={() => navigate('/payments/new')}
                disabled={!canEditData}
                className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
              >
                <Plus className="h-5 w-5" />
                Add Payment
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
