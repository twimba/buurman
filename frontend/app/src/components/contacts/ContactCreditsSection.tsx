import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BadgeEuro, Plus, Undo2 } from 'lucide-react';
import { Button, LoadingSpinner, ModalWrapper, Textarea } from '@buurman/ui';
import {
  useContactCredits,
  useCreateContactCredit,
  useRefundContactCredit,
} from '@/hooks/useContactHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';
import type { ContactCreditResponse } from '@/types/payment';

interface ContactCreditsSectionProps {
  contactIdentifier: string;
}

const fmt = (value: number, currency: string) => {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(value);
  } catch {
    return `${currency} ${value.toFixed(2)}`;
  }
};

/** Credits owed to the contact: overpayments and credit notes, with refund and creation. */
export const ContactCreditsSection = ({
  contactIdentifier,
}: ContactCreditsSectionProps) => {
  const { t } = useTranslation('tenants');
  const { canEditData } = useTeam();
  const { formatDate, formatDateTime } = useFormatDate();
  const { data: credits = [], isLoading } =
    useContactCredits(contactIdentifier);
  const createMutation = useCreateContactCredit(contactIdentifier);
  const refundMutation = useRefundContactCredit(contactIdentifier);
  const [showCreate, setShowCreate] = useState(false);
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const [refundTarget, setRefundTarget] =
    useState<ContactCreditResponse | null>(null);
  const [refundDate, setRefundDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [refundNotes, setRefundNotes] = useState('');

  const openTotal = credits.reduce((acc, c) => acc + c.remainingAmount, 0);
  const currency = credits[0]?.currency;

  const submitCreate = async () => {
    const value = Number.parseFloat(amount);
    if (!(value > 0) || !reason.trim()) {
      return;
    }
    await createMutation.mutateAsync({ amount: value, reason: reason.trim() });
    setShowCreate(false);
    setAmount('');
    setReason('');
  };

  const submitRefund = async () => {
    if (!refundTarget) {
      return;
    }
    await refundMutation.mutateAsync({
      creditId: refundTarget.identifier,
      data: { refundDate, notes: refundNotes.trim() || undefined },
    });
    setRefundTarget(null);
    setRefundNotes('');
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between mb-4 gap-3">
        <h2 className="text-xl font-semibold text-text-primary flex items-center gap-2">
          <BadgeEuro className="h-5 w-5" />
          {t('credits.title')} ({credits.length})
        </h2>
        {canEditData && (
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<Plus />}
            onClick={() => setShowCreate(true)}
          >
            {t('credits.add')}
          </Button>
        )}
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : credits.length === 0 ? (
        <div className="text-center py-8">
          <BadgeEuro className="h-12 w-12 text-text-disabled mx-auto mb-3" />
          <p className="text-text-secondary">{t('credits.empty')}</p>
        </div>
      ) : (
        <>
          <p className="text-sm text-text-secondary mb-3">
            {t('credits.openTotal', {
              amount: fmt(openTotal, currency ?? 'EUR'),
            })}
          </p>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-default text-left">
                  <th className="pb-2 pr-4 font-medium text-text-secondary">
                    {t('credits.date')}
                  </th>
                  <th className="pb-2 pr-4 font-medium text-text-secondary">
                    {t('credits.source')}
                  </th>
                  <th className="pb-2 pr-4 font-medium text-text-secondary">
                    {t('credits.reason')}
                  </th>
                  <th className="pb-2 pr-4 text-right font-medium text-text-secondary">
                    {t('credits.amount')}
                  </th>
                  <th className="pb-2 pr-4 text-right font-medium text-text-secondary">
                    {t('credits.remaining')}
                  </th>
                  <th className="pb-2 w-24" />
                </tr>
              </thead>
              <tbody>
                {credits.map((credit) => (
                  <tr
                    key={credit.identifier}
                    className="border-b border-border-default last:border-0"
                  >
                    <td className="py-2.5 pr-4 text-text-secondary whitespace-nowrap">
                      {formatDate(credit.createdAt)}
                    </td>
                    <td className="py-2.5 pr-4 text-text-primary whitespace-nowrap">
                      {t(`credits.sources.${credit.source}`)}
                    </td>
                    <td className="py-2.5 pr-4 text-text-secondary truncate max-w-[240px]">
                      {credit.reason ?? '-'}
                      {credit.refundedAt && (
                        <span className="block text-xs text-text-muted">
                          {t('credits.refundedOn', {
                            date: formatDateTime(credit.refundedAt),
                          })}
                          {credit.refundNotes ? ` · ${credit.refundNotes}` : ''}
                        </span>
                      )}
                    </td>
                    <td className="py-2.5 pr-4 text-right text-text-primary whitespace-nowrap">
                      {fmt(credit.amount, credit.currency)}
                    </td>
                    <td className="py-2.5 pr-4 text-right font-medium whitespace-nowrap">
                      <span
                        className={
                          credit.remainingAmount > 0
                            ? 'text-success-text'
                            : 'text-text-muted'
                        }
                      >
                        {fmt(credit.remainingAmount, credit.currency)}
                      </span>
                    </td>
                    <td className="py-2.5 text-right">
                      {canEditData && credit.remainingAmount > 0 && (
                        <button
                          type="button"
                          onClick={() => setRefundTarget(credit)}
                          className="inline-flex items-center gap-1 text-xs font-medium text-text-secondary hover:text-primary-500 focus-ring rounded"
                        >
                          <Undo2 className="h-3.5 w-3.5" />
                          {t('credits.refund')}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {showCreate && (
        <ModalWrapper
          open={showCreate}
          onClose={() => setShowCreate(false)}
          title={t('credits.createDialog.title')}
          subtitle={t('credits.createDialog.message')}
          size="sm"
          onSubmit={submitCreate}
          footer={
            <>
              <Button variant="secondary" onClick={() => setShowCreate(false)}>
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="primary"
                onClick={submitCreate}
                isLoading={createMutation.isPending}
                disabled={!(Number.parseFloat(amount) > 0) || !reason.trim()}
              >
                {t('credits.createDialog.confirm')}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label
                htmlFor="credit-amount"
                className="block text-sm font-medium text-text-secondary mb-2"
              >
                {t('credits.amount')} <span className="text-error-text">*</span>
              </label>
              <input
                id="credit-amount"
                type="number"
                step="0.01"
                min="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary min-h-touch"
              />
            </div>
            <div>
              <label
                htmlFor="credit-reason"
                className="block text-sm font-medium text-text-secondary mb-2"
              >
                {t('credits.reason')} <span className="text-error-text">*</span>
              </label>
              <Textarea
                id="credit-reason"
                rows={3}
                maxLength={1000}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder={t('credits.createDialog.reasonPlaceholder')}
              />
            </div>
          </div>
        </ModalWrapper>
      )}

      {refundTarget && (
        <ModalWrapper
          open={!!refundTarget}
          onClose={() => setRefundTarget(null)}
          title={t('credits.refundDialog.title')}
          subtitle={t('credits.refundDialog.message', {
            amount: fmt(refundTarget.remainingAmount, refundTarget.currency),
          })}
          size="sm"
          onSubmit={submitRefund}
          footer={
            <>
              <Button variant="secondary" onClick={() => setRefundTarget(null)}>
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="primary"
                onClick={submitRefund}
                isLoading={refundMutation.isPending}
              >
                {t('credits.refundDialog.confirm')}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label
                htmlFor="refund-date"
                className="block text-sm font-medium text-text-secondary mb-2"
              >
                {t('credits.refundDialog.date')}
              </label>
              <input
                id="refund-date"
                type="date"
                value={refundDate}
                onChange={(e) => setRefundDate(e.target.value)}
                className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary min-h-touch"
              />
            </div>
            <div>
              <label
                htmlFor="refund-notes"
                className="block text-sm font-medium text-text-secondary mb-2"
              >
                {t('credits.refundDialog.notes')}
              </label>
              <Textarea
                id="refund-notes"
                rows={2}
                maxLength={1000}
                value={refundNotes}
                onChange={(e) => setRefundNotes(e.target.value)}
              />
            </div>
          </div>
        </ModalWrapper>
      )}
    </div>
  );
};
