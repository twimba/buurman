import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PaymentStatus } from '@/types/payment';
import { usePayments } from '@/hooks/usePaymentHooks';
import { PaymentCard } from '@/components/payments/PaymentCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Receipt, Filter, AlertCircle } from 'lucide-react';

const statusFilters = [
  { value: undefined, label: 'All Payments' },
  { value: PaymentStatus.PENDING, label: 'Pending' },
  { value: PaymentStatus.PAID, label: 'Paid' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: PaymentStatus.CANCELLED, label: 'Cancelled' },
];

export const PaymentsPage = () => {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<
    PaymentStatus | 'OVERDUE' | undefined
  >(undefined);

  const {
    data: payments,
    isLoading,
    error,
  } = usePayments(statusFilter ? { status: statusFilter } : undefined);

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

  const overdueCount =
    payments?.filter((p) => p.status === PaymentStatus.OVERDUE).length || 0;
  const pendingCount =
    payments?.filter((p) => p.status === PaymentStatus.PENDING).length || 0;

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
            <p className="text-sm text-gray-600 mt-1">
              Track and manage rental payments
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

        {/* Alert for overdue payments */}
        {overdueCount > 0 && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
            <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0" />
            <div>
              <p className="font-semibold text-red-900">
                {overdueCount} overdue payment{overdueCount > 1 ? 's' : ''}
              </p>
              <p className="text-sm text-red-700">
                Please review and follow up on overdue payments
              </p>
            </div>
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
              Status
            </label>
            <div className="flex gap-2 flex-wrap">
              {statusFilters.map((filter) => (
                <button
                  key={filter.label}
                  onClick={() => setStatusFilter(filter.value as any)}
                  className={`px-4 py-2 rounded transition-colors text-sm ${
                    statusFilter === filter.value
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {filter.label}
                  {filter.value === PaymentStatus.PENDING &&
                    pendingCount > 0 && (
                      <span className="ml-2 px-2 py-0.5 bg-yellow-200 text-yellow-900 rounded-full text-xs">
                        {pendingCount}
                      </span>
                    )}
                  {filter.value === 'OVERDUE' && overdueCount > 0 && (
                    <span className="ml-2 px-2 py-0.5 bg-red-200 text-red-900 rounded-full text-xs">
                      {overdueCount}
                    </span>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Payment Count */}
        <p className="text-sm text-gray-600 mb-4">
          {payments?.length || 0}{' '}
          {payments?.length === 1 ? 'payment' : 'payments'}
        </p>

        {/* Payments Grid */}
        {payments && payments.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {payments.map((payment) => (
              <PaymentCard key={payment.id} payment={payment} />
            ))}
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
            <Receipt className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No payments found
            </h3>
            <p className="text-gray-600 mb-6">
              {statusFilter
                ? 'Try adjusting your filters'
                : 'Get started by creating your first payment'}
            </p>
            {!statusFilter && (
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
