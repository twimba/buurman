import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { PaymentStatus } from '@/types/payment';
import { usePayments, useOverduePayments } from '@/hooks/usePaymentHooks';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { ContractCell } from '@/components/contracts/ContractCell';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  DollarSign,
  AlertTriangle,
  Filter,
  Search,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  CheckCircle,
  TrendingUp,
  Eye,
} from 'lucide-react';
import {
  isBefore,
  parseISO,
  format,
  subMonths,
  startOfMonth,
  endOfMonth,
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
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import { getCurrencySymbol } from '@/utils/currencies';

const statusFilters = [
  { value: undefined, label: 'All Statuses' },
  { value: PaymentStatus.PENDING, label: 'Pending' },
  { value: PaymentStatus.PARTIALLY_PAID, label: 'Partial' },
  { value: PaymentStatus.PAID, label: 'Paid' },
  { value: PaymentStatus.OVERDUE, label: 'Overdue' },
  { value: PaymentStatus.CANCELLED, label: 'Cancelled' },
];

const ITEMS_PER_PAGE = 10;

type SortField = 'dueDate' | 'amount' | 'status' | 'contract' | 'property';
type SortOrder = 'asc' | 'desc';

export const PaymentsPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | undefined>(
    undefined
  );
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<SortField>('dueDate');
  const [sortOrder, setSortOrder] = useState<SortOrder>('asc');
  const [currentPage, setCurrentPage] = useState(1);

  const {
    data: payments,
    isLoading,
    error,
  } = usePayments(statusFilter ? { status: statusFilter } : undefined);

  // Always fetch all payments for metrics (independent of status filter)
  const { data: allPayments } = usePayments();
  const { data: overduePaymentsData } = useOverduePayments();

  const filteredAndSortedPayments = useMemo(() => {
    if (!payments) return [];

    let filtered = payments;

    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.identifier.toLowerCase().includes(search) ||
          p.contract.identifier.toLowerCase().includes(search) ||
          p.property.street.toLowerCase().includes(search) ||
          p.tenant.firstName.toLowerCase().includes(search) ||
          p.tenant.lastName?.toLowerCase().includes(search)
      );
    }

    filtered.sort((a, b) => {
      let aVal: string | number;
      let bVal: string | number;

      switch (sortField) {
        case 'dueDate':
          aVal = new Date(a.dueDate).getTime();
          bVal = new Date(b.dueDate).getTime();
          break;
        case 'amount':
          aVal = a.amount;
          bVal = b.amount;
          break;
        case 'status':
          aVal = a.status;
          bVal = b.status;
          break;
        case 'contract':
          aVal = a.contract.identifier;
          bVal = b.contract.identifier;
          break;
        case 'property':
          aVal = a.property.street;
          bVal = b.property.street;
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
  }, [payments, searchTerm, sortField, sortOrder]);

  // Calculate metrics from unfiltered data (independent of status filter)
  const overduePayments = useMemo(
    () => overduePaymentsData || [],
    [overduePaymentsData]
  );
  const pendingPayments = useMemo(
    () =>
      allPayments?.filter(
        (p) =>
          (p.status === PaymentStatus.PENDING ||
            p.status === PaymentStatus.PARTIALLY_PAID) &&
          !isBefore(parseISO(p.dueDate), new Date())
      ) || [],
    [allPayments]
  );
  const paidPayments = useMemo(
    () => allPayments?.filter((p) => p.status === PaymentStatus.PAID) || [],
    [allPayments]
  );

  const totalPending = useMemo(
    () => pendingPayments.reduce((sum, p) => sum + (p.balance ?? p.amount), 0),
    [pendingPayments]
  );
  const totalOverdue = useMemo(
    () => overduePayments.reduce((sum, p) => sum + (p.balance ?? p.amount), 0),
    [overduePayments]
  );

  // Calculate last 6 months data (before conditional returns)
  const chartData = useMemo(() => {
    if (!allPayments) return [];

    const monthsData = [];
    const now = new Date();

    for (let i = 5; i >= 0; i--) {
      const monthDate = subMonths(now, i);
      const monthStart = startOfMonth(monthDate);
      const monthEnd = endOfMonth(monthDate);

      const monthPayments = paidPayments.filter((p) => {
        if (!p.paymentDate) return false;
        const paymentDate = parseISO(p.paymentDate);
        return paymentDate >= monthStart && paymentDate <= monthEnd;
      });

      const total = monthPayments.reduce((sum, p) => sum + p.amount, 0);

      monthsData.push({
        month: format(monthDate, 'MMM'),
        total: Number(total.toFixed(2)),
      });
    }

    return monthsData;
  }, [allPayments, paidPayments]);

  const totalPages = Math.ceil(
    filteredAndSortedPayments.length / ITEMS_PER_PAGE
  );
  const paginatedPayments = filteredAndSortedPayments.slice(
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
          <button
            onClick={() => navigate('/payments/new')}
            disabled={!canEditData}
            className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
          >
            <Plus className="h-5 w-5" />
            Add Payment
          </button>
        </div>

        {/* Metrics Dashboard */}
        {allPayments && allPayments.length > 0 && (
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
                {getCurrencySymbol(allPayments?.[0]?.currency ?? 'EUR')}{' '}
                {totalPending.toFixed(2)}
              </p>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                {pendingPayments.length} payment
                {pendingPayments.length !== 1 ? 's' : ''}
              </p>
            </div>

            {/* Overdue Payments */}
            <div
              className={`rounded-xl shadow-sm p-6 transition-colors ${
                overduePayments.length > 0
                  ? 'bg-gradient-to-br from-red-50 to-red-100 dark:from-red-900/30 dark:to-red-800/30 border-2 border-red-200 dark:border-red-800'
                  : 'bg-white dark:bg-[#14161f]'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <h3
                  className={`text-sm font-medium ${
                    overduePayments.length > 0
                      ? 'text-red-700 dark:text-red-300'
                      : 'text-[#6b7194] dark:text-[#8b90a8]'
                  }`}
                >
                  Overdue Payments
                </h3>
                {overduePayments.length > 0 ? (
                  <AlertTriangle className="h-5 w-5 text-red-600 animate-pulse" />
                ) : (
                  <CheckCircle className="h-5 w-5 text-green-500" />
                )}
              </div>
              {overduePayments.length > 0 ? (
                <>
                  <p className="text-3xl font-bold text-red-600 dark:text-red-400">
                    {getCurrencySymbol(allPayments?.[0]?.currency ?? 'EUR')}{' '}
                    {totalOverdue.toFixed(2)}
                  </p>
                  <p className="text-sm text-red-700 dark:text-red-300 mt-1 font-medium">
                    {overduePayments.length} payment
                    {overduePayments.length !== 1 ? 's' : ''} past due
                  </p>
                  <p className="text-xs text-red-600 dark:text-red-400 mt-2">
                    ⚠️ Action required: Review overdue payments
                  </p>
                </>
              ) : (
                <>
                  <p className="text-3xl font-bold text-green-600 dark:text-green-400">
                    {getCurrencySymbol(allPayments?.[0]?.currency ?? 'EUR')}{' '}
                    0.00
                  </p>
                  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                    All caught up!
                  </p>
                  <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-2">
                    ✨ No overdue payments. Keep up the great work!
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
                <AreaChart data={chartData}>
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
                        ? `${getCurrencySymbol(allPayments?.[0]?.currency ?? 'EUR')} ${value.toFixed(2)}`
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

        {/* Search and Filter Bar */}
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div className="flex flex-col md:flex-row gap-4 mb-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
              <input
                type="text"
                placeholder="Search by identifier, contract, property, or tenant..."
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
              Status Filter
            </h3>
          </div>
          <div className="flex gap-2 flex-wrap">
            {statusFilters.map((filter) => (
              <button
                key={filter.label}
                onClick={() => {
                  setStatusFilter(filter.value);
                  setCurrentPage(1);
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
        {filteredAndSortedPayments.length > 0 ? (
          <>
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
                <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('dueDate')}
                    >
                      <div className="flex items-center gap-1">
                        Due Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Payment #
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] min-w-[280px]"
                      onClick={() => handleSort('property')}
                    >
                      <div className="flex items-center gap-1">
                        Contract
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
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('status')}
                    >
                      <div className="flex items-center gap-1">
                        Status
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                  {paginatedPayments.map((payment) => (
                    <tr
                      key={payment.id}
                      className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                      onClick={() => navigate(`/payments/${payment.id}`)}
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
                          contractId={payment.contract.id}
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
                            navigate(`/payments/${payment.id}`);
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

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between bg-white dark:bg-[#14161f] px-4 py-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
                <div className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                  Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} to{' '}
                  {Math.min(
                    currentPage * ITEMS_PER_PAGE,
                    filteredAndSortedPayments.length
                  )}{' '}
                  of {filteredAndSortedPayments.length} payments
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
            <DollarSign className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No payments found
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
              {statusFilter || searchTerm
                ? 'Try adjusting your filters or search'
                : 'Get started by recording your first payment'}
            </p>
            {!statusFilter && !searchTerm && (
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
