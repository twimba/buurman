import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useExpenses } from '@/hooks/useExpenseHooks';
import { usePayments } from '@/hooks/usePaymentHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { formatExpenseCategory } from '@/types/expense';
import { PaymentStatusBadge } from '@/components/payments/PaymentStatusBadge';
import { LoadingSpinner } from '@buurman/ui';
import { getCurrencySymbol } from '@/utils/currencies';
import { Receipt, CreditCard, ExternalLink } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface ContactFinancialsTabProps {
  contactIdentifier: string;
  backTo: string;
}

export const ContactFinancialsTab = ({
  contactIdentifier,
  backTo,
}: ContactFinancialsTabProps) => {
  const { t } = useTranslation('tenants');
  const { formatDate } = useFormatDate();

  const { data: expensesData, isLoading: expensesLoading } = useExpenses({
    contactIdentifier,
  });
  const { data: paymentsData, isLoading: paymentsLoading } = usePayments({
    contactIdentifier,
  });

  const expenses = useMemo(() => expensesData?.content ?? [], [expensesData]);
  const payments = useMemo(() => paymentsData?.content ?? [], [paymentsData]);

  // TODO: Totals assume single currency — group by currency when multi-currency support is added
  const expenseTotal = useMemo(() => {
    if (expenses.length === 0) {
      return null;
    }
    const currency = expenses[0].currency;
    const total = expenses.reduce((sum, e) => sum + e.amount, 0);
    return { total, currency };
  }, [expenses]);

  const paymentTotal = useMemo(() => {
    if (payments.length === 0) {
      return null;
    }
    const currency = payments[0].currency;
    const total = payments.reduce((sum, p) => sum + p.amount, 0);
    const received = payments.reduce((sum, p) => sum + p.receivedAmount, 0);
    return { total, received, currency };
  }, [payments]);

  if (expensesLoading || paymentsLoading) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Expenses Section */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-text-primary flex items-center gap-2">
            <Receipt className="h-5 w-5" />
            {t('financials.expenses')} ({expenses.length})
          </h2>
        </div>

        {expenses.length === 0 ? (
          <div className="text-center py-8">
            <Receipt className="h-12 w-12 text-text-disabled mx-auto mb-3" />
            <p className="text-text-secondary">{t('financials.noExpenses')}</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border-default text-left">
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Date
                    </th>
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Description
                    </th>
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Category
                    </th>
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Property
                    </th>
                    <th className="pb-2 text-right font-medium text-text-secondary">
                      Amount
                    </th>
                    <th className="pb-2 w-8" />
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((expense) => (
                    <tr
                      key={expense.identifier}
                      className="border-b border-border-default last:border-0 hover:bg-surface-page transition-colors"
                    >
                      <td className="py-2.5 pr-4 text-text-secondary whitespace-nowrap">
                        {formatDate(expense.expenseDate)}
                      </td>
                      <td className="py-2.5 pr-4 text-text-primary truncate max-w-[200px]">
                        {expense.description}
                      </td>
                      <td className="py-2.5 pr-4 text-text-secondary whitespace-nowrap">
                        {formatExpenseCategory(expense.category)}
                      </td>
                      <td className="py-2.5 pr-4 text-text-secondary truncate max-w-[150px]">
                        {expense.property.street}
                      </td>
                      <td className="py-2.5 text-right font-medium text-text-primary whitespace-nowrap">
                        {getCurrencySymbol(expense.currency)}{' '}
                        {expense.amount.toFixed(2)}
                      </td>
                      <td className="py-2.5 pl-2">
                        <Link
                          to={`/expenses/${expense.identifier}`}
                          state={{ backTo }}
                          className="text-text-muted hover:text-primary-500 transition-colors"
                        >
                          <ExternalLink className="h-4 w-4" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Expense Total */}
            {expenseTotal && (
              <div className="mt-4 pt-3 border-t border-border-default flex justify-end">
                <div className="text-sm">
                  <span className="text-text-secondary">
                    {t('financials.total')}{' '}
                  </span>
                  <span className="font-semibold text-text-primary">
                    {getCurrencySymbol(expenseTotal.currency)}{' '}
                    {expenseTotal.total.toFixed(2)}
                  </span>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Payments Section */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-text-primary flex items-center gap-2">
            <CreditCard className="h-5 w-5" />
            {t('financials.payments')} ({payments.length})
          </h2>
        </div>

        {payments.length === 0 ? (
          <div className="text-center py-8">
            <CreditCard className="h-12 w-12 text-text-disabled mx-auto mb-3" />
            <p className="text-text-secondary">{t('financials.noPayments')}</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border-default text-left">
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Due Date
                    </th>
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Property
                    </th>
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Notes
                    </th>
                    <th className="pb-2 pr-4 font-medium text-text-secondary">
                      Status
                    </th>
                    <th className="pb-2 text-right font-medium text-text-secondary">
                      Amount
                    </th>
                    <th className="pb-2 w-8" />
                  </tr>
                </thead>
                <tbody>
                  {payments.map((payment) => (
                    <tr
                      key={payment.identifier}
                      className="border-b border-border-default last:border-0 hover:bg-surface-page transition-colors"
                    >
                      <td className="py-2.5 pr-4 text-text-secondary whitespace-nowrap">
                        {formatDate(payment.dueDate)}
                      </td>
                      <td className="py-2.5 pr-4 text-text-secondary truncate max-w-[150px]">
                        {payment.property.street}
                      </td>
                      <td className="py-2.5 pr-4 text-text-primary truncate max-w-[200px]">
                        {payment.notes ?? '-'}
                      </td>
                      <td className="py-2.5 pr-4">
                        <PaymentStatusBadge status={payment.status} />
                      </td>
                      <td className="py-2.5 text-right font-medium text-text-primary whitespace-nowrap">
                        {getCurrencySymbol(payment.currency)}{' '}
                        {payment.amount.toFixed(2)}
                      </td>
                      <td className="py-2.5 pl-2">
                        <Link
                          to={`/payments/${payment.identifier}`}
                          state={{ backTo }}
                          className="text-text-muted hover:text-primary-500 transition-colors"
                        >
                          <ExternalLink className="h-4 w-4" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Payment Totals */}
            {paymentTotal && (
              <div className="mt-4 pt-3 border-t border-border-default flex justify-end gap-6">
                <div className="text-sm">
                  <span className="text-text-secondary">
                    {t('financials.total')}{' '}
                  </span>
                  <span className="font-semibold text-text-primary">
                    {getCurrencySymbol(paymentTotal.currency)}{' '}
                    {paymentTotal.total.toFixed(2)}
                  </span>
                </div>
                <div className="text-sm">
                  <span className="text-text-secondary">
                    {t('financials.received')}{' '}
                  </span>
                  <span className="font-semibold text-success-text">
                    {getCurrencySymbol(paymentTotal.currency)}{' '}
                    {paymentTotal.received.toFixed(2)}
                  </span>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
