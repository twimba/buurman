import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FileDown, Landmark, Plus, Trash2 } from 'lucide-react';
import {
  Button,
  ModalWrapper,
  StatusBadge,
  Textarea,
  type BadgeColorVariant,
} from '@buurman/ui';
import {
  useAddDepositDeduction,
  useContractDeposit,
  useForfeitDeposit,
  useRemoveDepositDeduction,
  useReturnDeposit,
  useUpsertDeposit,
} from '@/hooks/useContractHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';
import { ReasonDialog } from '@/components/payments/ReasonDialog';
import type { ContractResponse, DepositResponse } from '@/types/contract';
import { getDepositStatement } from '@/generated/api/letters/letters';
import { downloadBlob } from '@/utils/downloadBlob';

interface DepositCardProps {
  contract: ContractResponse;
  contractId: string;
}

const statusColor: Record<DepositResponse['status'], BadgeColorVariant> = {
  EXPECTED: 'amber',
  HELD: 'blue',
  PARTIALLY_RETURNED: 'amber',
  RETURNED: 'emerald',
  FORFEITED: 'gray',
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

/** Deposit lifecycle for one contract: record, deduct, return or forfeit. */
export const DepositCard = ({ contract, contractId }: DepositCardProps) => {
  const { t } = useTranslation('contracts');
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const { data: deposit, isLoading } = useContractDeposit(contractId);
  const upsert = useUpsertDeposit(contractId, t('deposit.toasts.saved'));
  const addDeduction = useAddDepositDeduction(
    contractId,
    t('deposit.toasts.deductionAdded')
  );
  const removeDeduction = useRemoveDepositDeduction(
    contractId,
    t('deposit.toasts.deductionRemoved')
  );
  const returnDeposit = useReturnDeposit(
    contractId,
    t('deposit.toasts.returned')
  );
  const forfeit = useForfeitDeposit(contractId, t('deposit.toasts.forfeited'));

  const [showEdit, setShowEdit] = useState(false);
  const [showDeduction, setShowDeduction] = useState(false);
  const [showReturn, setShowReturn] = useState(false);
  const [showForfeit, setShowForfeit] = useState(false);
  const [statementDownloading, setStatementDownloading] = useState(false);
  const downloadStatement = async () => {
    setStatementDownloading(true);
    try {
      const lang = contract.documentLanguages?.[0] ?? 'en';
      const blob = await getDepositStatement(contractId, { lang });
      downloadBlob(blob, `deposit-statement-${contractId}-${lang}.pdf`);
    } finally {
      setStatementDownloading(false);
    }
  };

  const currency = deposit?.currency ?? contract.rentAmountCurrency;
  const isClosed =
    deposit?.status === 'RETURNED' || deposit?.status === 'FORFEITED';

  // Edit form state
  const [amount, setAmount] = useState('');
  const [receivedDate, setReceivedDate] = useState('');
  const [heldWhere, setHeldWhere] = useState('');
  const [returnDueDate, setReturnDueDate] = useState('');
  const [notes, setNotes] = useState('');
  const openEdit = () => {
    setAmount(
      String(
        deposit?.amount ??
          contract.depositAmount ??
          contract.securityDeposit ??
          ''
      )
    );
    setReceivedDate(deposit?.receivedDate ?? '');
    setHeldWhere(deposit?.heldWhere ?? '');
    setReturnDueDate(deposit?.returnDueDate ?? '');
    setNotes(deposit?.notes ?? '');
    setShowEdit(true);
  };
  const submitEdit = async () => {
    const value = Number.parseFloat(amount);
    if (!(value > 0)) {
      return;
    }
    await upsert.mutateAsync({
      amount: value,
      receivedDate: receivedDate || undefined,
      heldWhere: heldWhere.trim() || undefined,
      returnDueDate: returnDueDate || undefined,
      notes: notes.trim() || undefined,
    });
    setShowEdit(false);
  };

  // Deduction form
  const [dedAmount, setDedAmount] = useState('');
  const [dedReason, setDedReason] = useState('');
  const [dedDate, setDedDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const submitDeduction = async () => {
    const value = Number.parseFloat(dedAmount);
    if (!(value > 0) || !dedReason.trim()) {
      return;
    }
    await addDeduction.mutateAsync({
      amount: value,
      reason: dedReason.trim(),
      deductionDate: dedDate,
    });
    setShowDeduction(false);
    setDedAmount('');
    setDedReason('');
  };

  // Return form
  const [retDate, setRetDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [retAmount, setRetAmount] = useState('');
  const [retNotes, setRetNotes] = useState('');
  const submitReturn = async () => {
    const parsed = retAmount ? Number.parseFloat(retAmount) : undefined;
    await returnDeposit.mutateAsync({
      returnedDate: retDate,
      returnedAmount: parsed && parsed > 0 ? parsed : undefined,
      notes: retNotes.trim() || undefined,
    });
    setShowReturn(false);
    setRetAmount('');
    setRetNotes('');
  };

  const inputClass =
    'w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary min-h-touch';

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between gap-3 mb-4">
        <h2 className="text-lg font-semibold text-text-primary flex items-center gap-2">
          <Landmark className="h-5 w-5" />
          {t('deposit.title')}
        </h2>
        {deposit && (
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<FileDown />}
              onClick={downloadStatement}
              isLoading={statementDownloading}
              title={t('deposit.statementHelp')}
            >
              {t('deposit.statement')}
            </Button>
            <StatusBadge
              label={t(`deposit.status.${deposit.status}`)}
              color={statusColor[deposit.status]}
              shape="pill"
            />
          </div>
        )}
      </div>

      {isLoading ? null : !deposit ? (
        <div className="space-y-3">
          <p className="text-sm text-text-secondary">
            {contract.depositAmount
              ? t('deposit.emptyWithAgreed', {
                  amount: fmt(contract.depositAmount, currency),
                })
              : t('deposit.empty')}
          </p>
          {canEditData && (
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Plus />}
              onClick={openEdit}
            >
              {t('deposit.record')}
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
            <div>
              <dt className="text-text-secondary">{t('deposit.amount')}</dt>
              <dd className="font-medium text-text-primary">
                {fmt(deposit.amount, deposit.currency)}
              </dd>
            </div>
            <div>
              <dt className="text-text-secondary">{t('deposit.refundable')}</dt>
              <dd className="font-medium text-text-primary">
                {fmt(deposit.refundable, deposit.currency)}
              </dd>
            </div>
            <div>
              <dt className="text-text-secondary">
                {t('deposit.receivedDate')}
              </dt>
              <dd className="text-text-primary">
                {deposit.receivedDate ? formatDate(deposit.receivedDate) : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-text-secondary">{t('deposit.heldWhere')}</dt>
              <dd className="text-text-primary">{deposit.heldWhere ?? '—'}</dd>
            </div>
            {deposit.returnDueDate && (
              <div>
                <dt className="text-text-secondary">
                  {t('deposit.returnDueDate')}
                </dt>
                <dd className="text-text-primary">
                  {formatDate(deposit.returnDueDate)}
                </dd>
              </div>
            )}
            {deposit.returnedAmount > 0 && (
              <div>
                <dt className="text-text-secondary">{t('deposit.returned')}</dt>
                <dd className="text-text-primary">
                  {fmt(deposit.returnedAmount, deposit.currency)}
                  {deposit.returnedDate
                    ? ` · ${formatDate(deposit.returnedDate)}`
                    : ''}
                </dd>
              </div>
            )}
          </dl>

          {/* Deductions */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-semibold text-text-primary">
                {t('deposit.deductions')}{' '}
                {deposit.deductionsTotal > 0 && (
                  <span className="font-normal text-text-secondary">
                    ({fmt(deposit.deductionsTotal, deposit.currency)})
                  </span>
                )}
              </h3>
              {canEditData && !isClosed && (
                <button
                  type="button"
                  onClick={() => setShowDeduction(true)}
                  className="inline-flex items-center gap-1 text-sm font-medium text-primary-600 dark:text-primary-300 hover:underline focus-ring rounded"
                >
                  <Plus className="h-4 w-4" />
                  {t('deposit.addDeduction')}
                </button>
              )}
            </div>
            {deposit.deductions.length === 0 ? (
              <p className="text-sm text-text-muted">
                {t('deposit.noDeductions')}
              </p>
            ) : (
              <ul className="divide-y divide-border-default rounded-md border border-border-default">
                {deposit.deductions.map((d) => (
                  <li
                    key={d.identifier}
                    className="flex items-center gap-3 px-3 py-2 text-sm"
                  >
                    <span className="text-text-secondary whitespace-nowrap">
                      {formatDate(d.deductionDate)}
                    </span>
                    <span className="flex-1 text-text-primary truncate">
                      {d.reason}
                    </span>
                    <span className="font-medium text-text-primary whitespace-nowrap">
                      {fmt(d.amount, d.currency)}
                    </span>
                    {canEditData && !isClosed && (
                      <button
                        type="button"
                        onClick={() => removeDeduction.mutate(d.identifier)}
                        aria-label={t('deposit.removeDeduction')}
                        className="p-1 rounded text-text-muted hover:text-error-text hover:bg-error-bg focus-ring"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {deposit.notes && (
            <p className="text-sm text-text-secondary whitespace-pre-line">
              {deposit.notes}
            </p>
          )}

          {canEditData && !isClosed && (
            <div className="flex flex-wrap gap-2 pt-2 border-t border-border-default">
              <Button variant="secondary" size="sm" onClick={openEdit}>
                {t('common:buttons.edit')}
              </Button>
              {deposit.status !== 'EXPECTED' && deposit.refundable > 0 && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    setRetAmount('');
                    setShowReturn(true);
                  }}
                >
                  {t('deposit.return')}
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowForfeit(true)}
              >
                {t('deposit.forfeit')}
              </Button>
            </div>
          )}
        </div>
      )}

      {showEdit && (
        <ModalWrapper
          open={showEdit}
          onClose={() => setShowEdit(false)}
          title={
            deposit
              ? t('deposit.editDialog.titleEdit')
              : t('deposit.editDialog.titleNew')
          }
          size="sm"
          onSubmit={submitEdit}
          footer={
            <>
              <Button variant="secondary" onClick={() => setShowEdit(false)}>
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="primary"
                onClick={submitEdit}
                isLoading={upsert.isPending}
                disabled={!(Number.parseFloat(amount) > 0)}
              >
                {t('common:buttons.save')}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.amount')} ({currency}) *
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.receivedDate')}
              </label>
              <input
                type="date"
                value={receivedDate}
                onChange={(e) => setReceivedDate(e.target.value)}
                className={inputClass}
              />
              <p className="text-xs text-text-muted mt-1">
                {t('deposit.editDialog.receivedHelp')}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.heldWhere')}
              </label>
              <input
                type="text"
                maxLength={255}
                value={heldWhere}
                onChange={(e) => setHeldWhere(e.target.value)}
                className={inputClass}
                placeholder={t('deposit.editDialog.heldWherePlaceholder')}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.returnDueDate')}
              </label>
              <input
                type="date"
                value={returnDueDate}
                onChange={(e) => setReturnDueDate(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.notes')}
              </label>
              <Textarea
                rows={2}
                maxLength={2000}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          </div>
        </ModalWrapper>
      )}

      {showDeduction && (
        <ModalWrapper
          open={showDeduction}
          onClose={() => setShowDeduction(false)}
          title={t('deposit.deductionDialog.title')}
          subtitle={t('deposit.deductionDialog.message', {
            refundable: fmt(deposit?.refundable ?? 0, currency),
          })}
          size="sm"
          onSubmit={submitDeduction}
          footer={
            <>
              <Button
                variant="secondary"
                onClick={() => setShowDeduction(false)}
              >
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="primary"
                onClick={submitDeduction}
                isLoading={addDeduction.isPending}
                disabled={
                  !(Number.parseFloat(dedAmount) > 0) || !dedReason.trim()
                }
              >
                {t('deposit.deductionDialog.confirm')}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.amount')} *
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                max={deposit?.refundable}
                value={dedAmount}
                onChange={(e) => setDedAmount(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.deductionDialog.date')}
              </label>
              <input
                type="date"
                value={dedDate}
                onChange={(e) => setDedDate(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.deductionDialog.reason')} *
              </label>
              <Textarea
                rows={2}
                maxLength={1000}
                value={dedReason}
                onChange={(e) => setDedReason(e.target.value)}
                placeholder={t('deposit.deductionDialog.reasonPlaceholder')}
              />
            </div>
          </div>
        </ModalWrapper>
      )}

      {showReturn && deposit && (
        <ModalWrapper
          open={showReturn}
          onClose={() => setShowReturn(false)}
          title={t('deposit.returnDialog.title')}
          subtitle={t('deposit.returnDialog.message', {
            refundable: fmt(deposit.refundable, deposit.currency),
          })}
          size="sm"
          onSubmit={submitReturn}
          footer={
            <>
              <Button variant="secondary" onClick={() => setShowReturn(false)}>
                {t('common:buttons.cancel')}
              </Button>
              <Button
                variant="primary"
                onClick={submitReturn}
                isLoading={returnDeposit.isPending}
              >
                {t('deposit.returnDialog.confirm')}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.returnDialog.date')} *
              </label>
              <input
                type="date"
                value={retDate}
                onChange={(e) => setRetDate(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.returnDialog.amount')}
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                max={deposit.refundable}
                value={retAmount}
                placeholder={deposit.refundable.toFixed(2)}
                onChange={(e) => setRetAmount(e.target.value)}
                className={inputClass}
              />
              <p className="text-xs text-text-muted mt-1">
                {t('deposit.returnDialog.amountHelp')}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('deposit.notes')}
              </label>
              <Textarea
                rows={2}
                maxLength={2000}
                value={retNotes}
                onChange={(e) => setRetNotes(e.target.value)}
              />
            </div>
          </div>
        </ModalWrapper>
      )}

      {showForfeit && (
        <ReasonDialog
          open={showForfeit}
          title={t('deposit.forfeitDialog.title')}
          message={t('deposit.forfeitDialog.message')}
          reasonLabel={t('deposit.forfeitDialog.reason')}
          confirmLabel={t('deposit.forfeitDialog.confirm')}
          variant="danger"
          isLoading={forfeit.isPending}
          onConfirm={async (reason) => {
            await forfeit.mutateAsync({ reason });
            setShowForfeit(false);
          }}
          onClose={() => setShowForfeit(false)}
        />
      )}
    </div>
  );
};
