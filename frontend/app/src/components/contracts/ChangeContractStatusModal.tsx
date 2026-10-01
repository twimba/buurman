import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { ContractStatus } from '@/types/contract';
import { X } from 'lucide-react';
import { RichTextEditor } from '@buurman/ui';

interface ChangeContractStatusModalProps {
  currentStatus: ContractStatus;
  onClose: () => void;
  onConfirm: (newStatus: ContractStatus, reason?: string) => void;
  isLoading?: boolean;
}

const getValidTransitions = (
  currentStatus: ContractStatus
): ContractStatus[] => {
  switch (currentStatus) {
    case ContractStatus.DRAFT:
      return [ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE];
    case ContractStatus.PENDING_SIGNATURE:
      return [ContractStatus.DRAFT, ContractStatus.ACTIVE];
    case ContractStatus.ACTIVE:
      return [ContractStatus.TERMINATED, ContractStatus.EXPIRED];
    case ContractStatus.NOTICE_GIVEN:
      // Entering NOTICE_GIVEN only happens via the dedicated termination wizard (the backend's
      // generic changeContractStatus endpoint rejects it as a target), but leaving it for
      // TERMINATED through this modal is still valid and allowed server-side.
      return [ContractStatus.TERMINATED];
    case ContractStatus.EXPIRED:
    case ContractStatus.TERMINATED:
      return []; // Terminal states
    default:
      return [];
  }
};

export const ChangeContractStatusModal = ({
  currentStatus,
  onClose,
  onConfirm,
  isLoading = false,
}: ChangeContractStatusModalProps) => {
  const { t } = useTranslation('contracts');
  const validTransitions = getValidTransitions(currentStatus);

  const statusLabels = useMemo(
    (): Record<ContractStatus, string> => ({
      DRAFT: t('statusChange.statuses.DRAFT'),
      PENDING_SIGNATURE: t('statusChange.statuses.PENDING_SIGNATURE'),
      ACTIVE: t('statusChange.statuses.ACTIVE'),
      EXPIRED: t('statusChange.statuses.EXPIRED'),
      TERMINATED: t('statusChange.statuses.TERMINATED'),
      NOTICE_GIVEN: t('statusChange.statuses.NOTICE_GIVEN'),
    }),
    [t]
  );
  const [selectedStatus, setSelectedStatus] = useState<ContractStatus>(
    validTransitions[0] ?? currentStatus
  );
  const [reason, setReason] = useState('');

  const submitForm = () => {
    onConfirm(selectedStatus, reason || undefined);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border-default">
          <h2 className="text-lg font-semibold text-text-primary">
            {t('statusChange.title')}
          </h2>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text-secondary"
            disabled={isLoading}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} onKeyDown={handleCmdEnter}>
          <div className="p-4 space-y-4">
            {/* Current Status */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('statusChange.currentStatus')}
              </label>
              <p className="text-sm text-text-primary">
                {statusLabels[currentStatus]}
              </p>
            </div>

            {/* New Status Selection */}
            {validTransitions.length > 0 ? (
              <>
                <div>
                  <label
                    htmlFor="newStatus"
                    className="block text-sm font-medium text-text-secondary mb-1"
                  >
                    {t('statusChange.newStatus')}
                  </label>
                  <select
                    id="newStatus"
                    value={selectedStatus}
                    onChange={(e) =>
                      setSelectedStatus(e.target.value as ContractStatus)
                    }
                    className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                    disabled={isLoading}
                  >
                    {validTransitions.map((status) => (
                      <option key={status} value={status}>
                        {statusLabels[status]}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Reason */}
                <div>
                  <label
                    htmlFor="reason"
                    className="block text-sm font-medium text-text-secondary mb-1"
                  >
                    {t('statusChange.reason')}
                  </label>
                  <RichTextEditor
                    value={reason}
                    onChange={setReason}
                    placeholder={t('statusChange.reasonPlaceholder')}
                    onSubmit={submitForm}
                  />
                </div>
              </>
            ) : (
              <div className="text-sm text-text-secondary">
                {t('statusChange.noTransitions', {
                  status: statusLabels[currentStatus],
                })}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
              disabled={isLoading}
            >
              {t('common:buttons.cancel')}
            </button>
            {validTransitions.length > 0 && (
              <button
                type="submit"
                className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50"
                disabled={isLoading}
              >
                {isLoading
                  ? t('statusChange.changing')
                  : t('statusChange.confirm')}
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
};
