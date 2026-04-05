import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  usePaymentsByContract,
  useDeletePayment,
} from '@/hooks/usePaymentHooks';
import { useGenerateContractPayments } from '@/hooks/useContractHooks';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Button, ConfirmDialog, LoadingSpinner } from '@buurman/ui';
import GeneratePaymentsModal from '@/components/contracts/GeneratePaymentsModal';
import { useTeam } from '@/context/TeamContext';
import {
  DollarSign,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Search,
  Plus,
  Calendar,
  Trash2,
} from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';
import { ContractStatus } from '@/types/contract';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { PaymentStatus } from '@/types/payment';
import { useTranslation } from 'react-i18next';

interface ContractPaymentsTabProps {
  contractId: string;
  contractStatus: string;
}

export const ContractPaymentsTab = ({
  contractId,
  contractStatus,
}: ContractPaymentsTabProps) => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();

  const [showGeneratePaymentsModal, setShowGeneratePaymentsModal] =
    useState(false);
  const [deletePaymentTarget, setDeletePaymentTarget] = useState<string | null>(
    null
  );
  const [paymentsSearchTerm, setPaymentsSearchTerm] = useState('');
  const [paymentsSortField, setPaymentsSortField] = useState<
    'dueDate' | 'amount' | 'status' | 'paymentDate'
  >('dueDate');
  const [paymentsSortOrder, setPaymentsSortOrder] = useState<'asc' | 'desc'>(
    'asc'
  );
  const [paymentsCurrentPage, setPaymentsCurrentPage] = useState(1);
  const paymentsPerPage = 10;

  const {
    data: payments = [],
    isLoading: paymentsLoading,
    error: paymentsError,
  } = usePaymentsByContract(contractId);
  const deletePaymentMutation = useDeletePayment();
  const generatePaymentsMutation = useGenerateContractPayments(contractId);

  const filteredAndSortedPayments = useMemo(() => {
    if (!payments) {
      return [];
    }

    let filtered = [...payments];

    if (paymentsSearchTerm) {
      const search = paymentsSearchTerm.toLowerCase();
      filtered = filtered.filter(
        (payment) =>
          payment.identifier.toLowerCase().includes(search) ||
          payment.status.toLowerCase().includes(search)
      );
    }

    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

      switch (paymentsSortField) {
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
        case 'paymentDate':
          aVal = a.paymentDate ? new Date(a.paymentDate).getTime() : 0;
          bVal = b.paymentDate ? new Date(b.paymentDate).getTime() : 0;
          break;
        default:
          return 0;
      }

      if (aVal < bVal) {
        return paymentsSortOrder === 'asc' ? -1 : 1;
      }
      if (aVal > bVal) {
        return paymentsSortOrder === 'asc' ? 1 : -1;
      }
      return 0;
    });

    return filtered;
  }, [payments, paymentsSearchTerm, paymentsSortField, paymentsSortOrder]);

  const paginatedPayments = useMemo(() => {
    const startIndex = (paymentsCurrentPage - 1) * paymentsPerPage;
    const endIndex = startIndex + paymentsPerPage;
    return filteredAndSortedPayments.slice(startIndex, endIndex);
  }, [filteredAndSortedPayments, paymentsCurrentPage]);

  const paymentsTotalPages = Math.ceil(
    filteredAndSortedPayments.length / paymentsPerPage
  );

  const handlePaymentsSort = (field: typeof paymentsSortField) => {
    if (paymentsSortField === field) {
      setPaymentsSortOrder(paymentsSortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setPaymentsSortField(field);
      setPaymentsSortOrder('asc');
    }
  };

  return (
    <>
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-text-primary">
            {t('payments.title')} ({filteredAndSortedPayments.length})
          </h2>
          <div className="flex items-center gap-2">
            {contractStatus === ContractStatus.ACTIVE && (
              <Button
                variant="success"
                leftIcon={<RefreshCw />}
                onClick={() => setShowGeneratePaymentsModal(true)}
                disabled={!canEditData}
              >
                {t('payments.generateFuture')}
              </Button>
            )}
            <Button
              variant="secondary"
              leftIcon={<Calendar />}
              onClick={() => navigate(`/payments/new?contractId=${contractId}`)}
              disabled={!canEditData}
            >
              {t('payments.schedulePayment')}
            </Button>
            <Button
              variant="primary"
              leftIcon={<Plus />}
              onClick={() =>
                navigate(`/payments/new?contractId=${contractId}&register=true`)
              }
              disabled={!canEditData}
            >
              {t('payments.registerPayments')}
            </Button>
          </div>
        </div>

        {paymentsLoading ? (
          <LoadingSpinner />
        ) : paymentsError ? (
          <ErrorMessage message={t('payments.failedToLoad')} />
        ) : payments.length === 0 ? (
          <div className="text-center py-12">
            <DollarSign className="h-12 w-12 text-text-disabled mx-auto mb-3" />
            <p className="text-text-secondary mb-4">
              {t('payments.noPayments')}
            </p>
            <Button
              variant="primary"
              leftIcon={<Plus />}
              onClick={() => navigate(`/payments/new?contractId=${contractId}`)}
              disabled={!canEditData}
            >
              {t('payments.createFirstPayment')}
            </Button>
          </div>
        ) : (
          <>
            {/* Search Bar */}
            <div className="mb-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted " />
                <input
                  type="text"
                  placeholder={t('payments.searchPlaceholder')}
                  value={paymentsSearchTerm}
                  onChange={(e) => {
                    setPaymentsSearchTerm(e.target.value);
                    setPaymentsCurrentPage(1);
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
                      {t('payments.table.paymentNumber')}
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handlePaymentsSort('dueDate')}
                    >
                      <div className="flex items-center gap-1">
                        {t('payments.table.dueDate')}
                        {paymentsSortField === 'dueDate' &&
                          (paymentsSortOrder === 'asc' ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          ))}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handlePaymentsSort('amount')}
                    >
                      <div className="flex items-center gap-1">
                        {t('payments.table.amount')}
                        {paymentsSortField === 'amount' &&
                          (paymentsSortOrder === 'asc' ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          ))}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handlePaymentsSort('status')}
                    >
                      <div className="flex items-center gap-1">
                        {t('payments.table.status')}
                        {paymentsSortField === 'status' &&
                          (paymentsSortOrder === 'asc' ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          ))}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handlePaymentsSort('paymentDate')}
                    >
                      <div className="flex items-center gap-1">
                        {t('payments.table.paymentDate')}
                        {paymentsSortField === 'paymentDate' &&
                          (paymentsSortOrder === 'asc' ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          ))}
                      </div>
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                      {t('payments.table.actions')}
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-surface-card divide-y divide-border-default">
                  {paginatedPayments.length === 0 ? (
                    <tr>
                      <td
                        colSpan={5}
                        className="px-6 py-12 text-center text-text-secondary"
                      >
                        {t('payments.noMatch')}
                      </td>
                    </tr>
                  ) : (
                    paginatedPayments.map((payment) => (
                      <tr
                        key={payment.identifier}
                        className="hover:bg-primary-50 cursor-pointer"
                        onClick={() =>
                          navigate(`/payments/${payment.identifier}`, {
                            state: {
                              backTo: `/contracts/${contractId}?tab=payments`,
                            },
                          })
                        }
                      >
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-500 dark:text-primary-300">
                          #{payment.identifier}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                          {formatDate(payment.dueDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                          <div>
                            <span className="font-semibold text-text-primary">
                              {payment.currency} {payment.amount.toFixed(2)}
                            </span>
                            {payment.receivedAmount > 0 &&
                              payment.status !== PaymentStatus.PAID && (
                                <p className="text-xs text-text-secondary">
                                  {t('payments.balance', {
                                    currency: payment.currency,
                                    amount: (payment.balance ?? 0).toFixed(2),
                                  })}
                                </p>
                              )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <PaymentStatusBadge status={payment.status} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                          {payment.paymentDate
                            ? formatDate(payment.paymentDate)
                            : '-'}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-right">
                          {canEditData && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeletePaymentTarget(payment.identifier);
                              }}
                              className="p-1.5 rounded hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                              title={t('common:buttons.delete')}
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {paymentsTotalPages > 1 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-border-default">
                <div className="text-sm text-text-secondary">
                  {t('payments.showing', {
                    from: (paymentsCurrentPage - 1) * paymentsPerPage + 1,
                    to: Math.min(
                      paymentsCurrentPage * paymentsPerPage,
                      filteredAndSortedPayments.length
                    ),
                    total: filteredAndSortedPayments.length,
                  })}
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() =>
                      setPaymentsCurrentPage(paymentsCurrentPage - 1)
                    }
                    disabled={paymentsCurrentPage === 1}
                    className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                  >
                    {t('common:pagination.previous')}
                  </button>
                  <span className="px-3 py-1 text-sm text-text-secondary">
                    {t('payments.page', {
                      current: paymentsCurrentPage,
                      total: paymentsTotalPages,
                    })}
                  </span>
                  <button
                    onClick={() =>
                      setPaymentsCurrentPage(paymentsCurrentPage + 1)
                    }
                    disabled={paymentsCurrentPage === paymentsTotalPages}
                    className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                  >
                    {t('common:pagination.next')}
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Generate Future Payments Modal */}
      <GeneratePaymentsModal
        isOpen={showGeneratePaymentsModal}
        onClose={() => setShowGeneratePaymentsModal(false)}
        onSubmit={async (count, markAsPaid) => {
          await generatePaymentsMutation.mutateAsync({ count, markAsPaid });
        }}
        isLoading={generatePaymentsMutation.isPending}
      />

      {deletePaymentTarget && (
        <ConfirmDialog
          title={t('payments.deleteTitle')}
          message={t('payments.deleteMessage')}
          confirmLabel={t('common:buttons.delete')}
          variant="danger"
          isLoading={deletePaymentMutation.isPending}
          onConfirm={async () => {
            await deletePaymentMutation.mutateAsync(deletePaymentTarget);
            setDeletePaymentTarget(null);
          }}
          onCancel={() => setDeletePaymentTarget(null)}
        />
      )}
    </>
  );
};
