import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { PaymentStatus } from '@/types/payment';
import { usePayments } from '@/hooks/usePaymentHooks';
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

const statusFilters = [
  { value: undefined, label: 'All Statuses' },
  { value: PaymentStatus.PENDING, label: 'Pending' },
  { value: PaymentStatus.PAID, label: 'Paid' },
  { value: PaymentStatus.OVERDUE, label: 'Overdue' },
  { value: PaymentStatus.CANCELLED, label: 'Cancelled' },
];

const ITEMS_PER_PAGE = 10;

type SortField = 'dueDate' | 'amount' | 'status' | 'contract' | 'property';
type SortOrder = 'asc' | 'desc';

export const PaymentsPage = () => {
  const navigate = useNavigate();
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
      let aVal: any;
      let bVal: any;

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

  // Calculate metrics (before conditional returns)
  const pendingPayments = useMemo(
    () => payments?.filter((p) => p.status === PaymentStatus.PENDING) || [],
    [payments]
  );
  const overduePayments = useMemo(
    () =>
      payments?.filter(
        (p) =>
          p.status === PaymentStatus.PENDING &&
          isBefore(parseISO(p.dueDate), new Date())
      ) || [],
    [payments]
  );
  const paidPayments = useMemo(
    () => payments?.filter((p) => p.status === PaymentStatus.PAID) || [],
    [payments]
  );

  const totalPending = useMemo(
    () => pendingPayments.reduce((sum, p) => sum + p.amount, 0),
    [pendingPayments]
  );
  const totalOverdue = useMemo(
    () => overduePayments.reduce((sum, p) => sum + p.amount, 0),
    [overduePayments]
  );

  // Calculate last 6 months data (before conditional returns)
  const chartData = useMemo(() => {
    if (!payments) return [];

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
  }, [payments, paidPayments]);

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
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
            <p className="text-sm text-gray-600 mt-1">
              Track rent payments and income
            </p>
          </div>
          <button
            onClick={() => navigate('/payments/new')}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Plus className="h-5 w-5" />
            Add Payment
          </button>
        </div>

        {/* Metrics Dashboard */}
        {payments && payments.length > 0 && (
          <div className="mb-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Pending Payments */}
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-medium text-gray-600">
                  Pending Payments
                </h3>
                <Clock className="h-5 w-5 text-yellow-500" />
              </div>
              <p className="text-3xl font-bold text-gray-900">
                EUR {totalPending.toFixed(2)}
              </p>
              <p className="text-sm text-gray-500 mt-1">
                {pendingPayments.length} payment
                {pendingPayments.length !== 1 ? 's' : ''}
              </p>
            </div>

            {/* Overdue Payments */}
            <div
              className={`rounded-lg shadow p-6 transition-colors ${
                overduePayments.length > 0
                  ? 'bg-gradient-to-br from-red-50 to-red-100 border-2 border-red-200'
                  : 'bg-white'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <h3
                  className={`text-sm font-medium ${
                    overduePayments.length > 0
                      ? 'text-red-700'
                      : 'text-gray-600'
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
                  <p className="text-3xl font-bold text-red-600">
                    EUR {totalOverdue.toFixed(2)}
                  </p>
                  <p className="text-sm text-red-700 mt-1 font-medium">
                    {overduePayments.length} payment
                    {overduePayments.length !== 1 ? 's' : ''} past due
                  </p>
                  <p className="text-xs text-red-600 mt-2">
                    ⚠️ Action required: Review overdue payments
                  </p>
                </>
              ) : (
                <>
                  <p className="text-3xl font-bold text-green-600">EUR 0.00</p>
                  <p className="text-sm text-gray-600 mt-1">All caught up!</p>
                  <p className="text-xs text-gray-500 mt-2">
                    ✨ No overdue payments. Keep up the great work!
                  </p>
                </>
              )}
            </div>

            {/* 6-Month Revenue Chart */}
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-medium text-gray-600">
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
                      value !== undefined ? `EUR ${value.toFixed(2)}` : 'N/A',
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
        <div className="mb-6 bg-white rounded-lg border border-gray-200 p-4">
          <div className="flex flex-col md:flex-row gap-4 mb-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search by identifier, contract, property, or tenant..."
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
            <h3 className="font-semibold text-gray-900">Status Filter</h3>
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
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
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
            <div className="bg-white rounded-lg shadow overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('dueDate')}
                    >
                      <div className="flex items-center gap-1">
                        Due Date
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Payment #
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100 min-w-[280px]"
                      onClick={() => handleSort('property')}
                    >
                      <div className="flex items-center gap-1">
                        Contract
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('amount')}
                    >
                      <div className="flex items-center justify-end gap-1">
                        Amount
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('status')}
                    >
                      <div className="flex items-center gap-1">
                        Status
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {paginatedPayments.map((payment) => (
                    <tr
                      key={payment.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => navigate(`/payments/${payment.id}`)}
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {format(new Date(payment.dueDate), 'MMM d, yyyy')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm font-medium text-gray-900">
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
                        <span className="text-sm font-semibold text-gray-900">
                          {payment.currency} {payment.amount.toFixed(2)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <PaymentStatusBadge status={payment.status} />
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
                    filteredAndSortedPayments.length
                  )}{' '}
                  of {filteredAndSortedPayments.length} payments
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
            <DollarSign className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No payments found
            </h3>
            <p className="text-gray-600 mb-6">
              {statusFilter || searchTerm
                ? 'Try adjusting your filters or search'
                : 'Get started by recording your first payment'}
            </p>
            {!statusFilter && !searchTerm && (
              <button
                onClick={() => navigate('/payments/new')}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors inline-flex items-center gap-2"
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
