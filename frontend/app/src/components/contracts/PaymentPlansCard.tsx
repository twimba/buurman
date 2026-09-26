import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { CalendarClock } from 'lucide-react';
import { Button, StatusBadge, type BadgeColorVariant } from '@buurman/ui';
import {
  useCancelPaymentPlan,
  useContractPaymentPlans,
} from '@/hooks/useContractHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';
import { ReasonDialog } from '@/components/payments/ReasonDialog';
import type { PaymentPlanResponse } from '@/types/contract';

interface PaymentPlansCardProps {
  contractId: string;
}

const statusColor: Record<PaymentPlanResponse['status'], BadgeColorVariant> = {
  ACTIVE: 'blue',
  COMPLETED: 'emerald',
  CANCELLED: 'gray',
};

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

/** Payment plans agreed on this contract with progress; hidden when there are none. */
export const PaymentPlansCard = ({ contractId }: PaymentPlansCardProps) => {
  const { t } = useTranslation('contracts');
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const { data: plans = [] } = useContractPaymentPlans(contractId);
  const cancel = useCancelPaymentPlan(
    contractId,
    t('paymentPlans.toasts.cancelled')
  );
  const [cancelTarget, setCancelTarget] = useState<PaymentPlanResponse | null>(
    null
  );

  if (plans.length === 0) {
    return null;
  }

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <h2 className="text-lg font-semibold text-text-primary flex items-center gap-2 mb-4">
        <CalendarClock className="h-5 w-5" />
        {t('paymentPlans.title')}
      </h2>
      <ul className="space-y-4">
        {plans.map((plan) => {
          const progress =
            plan.totalAmount > 0
              ? Math.min(
                  100,
                  Math.round((plan.paidAmount / plan.totalAmount) * 100)
                )
              : 0;
          return (
            <li
              key={plan.identifier}
              className="rounded-md border border-border-default p-4 space-y-2"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="text-sm font-medium text-text-primary">
                    {t('paymentPlans.summary', {
                      total: fmt(plan.totalAmount, plan.currency),
                      count: plan.instalmentCount,
                      frequency: t(`paymentPlans.frequency.${plan.frequency}`),
                    })}
                  </p>
                  <p className="text-xs text-text-secondary">
                    {t('paymentPlans.from', {
                      date: formatDate(plan.startDate),
                    })}
                    {' · '}
                    {t('paymentPlans.covers', {
                      count: plan.coveredPaymentIdentifiers.length,
                    })}
                  </p>
                </div>
                <StatusBadge
                  label={t(`paymentPlans.status.${plan.status}`)}
                  color={statusColor[plan.status]}
                  shape="pill"
                />
              </div>
              <div
                className="h-2 rounded-full bg-surface-inset overflow-hidden"
                role="progressbar"
                aria-valuenow={progress}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <div
                  className="h-full bg-success rounded-full"
                  style={{ width: `${progress}%` }}
                />
              </div>
              <p className="text-xs text-text-secondary">
                {t('paymentPlans.progress', {
                  paid: fmt(plan.paidAmount, plan.currency),
                  remaining: fmt(plan.remainingAmount, plan.currency),
                })}
              </p>
              <ul className="flex flex-wrap gap-1.5">
                {plan.instalments.map((inst) => (
                  <li key={inst.identifier}>
                    <Link
                      to={`/payments/${inst.identifier}`}
                      className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded-full border focus-ring ${
                        inst.status === 'PAID'
                          ? 'border-success-border bg-success-bg text-success-text'
                          : inst.status === 'CANCELLED'
                            ? 'border-border-default text-text-muted line-through'
                            : inst.status === 'OVERDUE'
                              ? 'border-error-border bg-error-bg text-error-text'
                              : 'border-border-default text-text-secondary'
                      }`}
                    >
                      {formatDate(inst.dueDate)} ·{' '}
                      {fmt(inst.amount, inst.currency)}
                    </Link>
                  </li>
                ))}
              </ul>
              {plan.notes && (
                <p className="text-xs text-text-secondary">{plan.notes}</p>
              )}
              {plan.cancelReason && (
                <p className="text-xs text-text-muted">
                  {t('paymentPlans.cancelledBecause', {
                    reason: plan.cancelReason,
                  })}
                </p>
              )}
              {canEditData && plan.status === 'ACTIVE' && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setCancelTarget(plan)}
                >
                  {t('paymentPlans.cancel')}
                </Button>
              )}
            </li>
          );
        })}
      </ul>

      {cancelTarget && (
        <ReasonDialog
          open={!!cancelTarget}
          title={t('paymentPlans.cancelDialog.title')}
          message={t('paymentPlans.cancelDialog.message', {
            remaining: fmt(cancelTarget.remainingAmount, cancelTarget.currency),
          })}
          reasonLabel={t('paymentPlans.cancelDialog.reason')}
          confirmLabel={t('paymentPlans.cancelDialog.confirm')}
          variant="danger"
          isLoading={cancel.isPending}
          onConfirm={async (reason) => {
            await cancel.mutateAsync({
              planId: cancelTarget.identifier,
              data: { reason },
            });
            setCancelTarget(null);
          }}
          onClose={() => setCancelTarget(null)}
        />
      )}
    </div>
  );
};
