import { useState } from 'react';
import {
  Plus,
  Check,
  CheckCircle,
  XCircle,
  Ban,
  TrendingUp,
  TrendingDown,
  Zap,
  User,
} from 'lucide-react';
import { ExtensionStatusBadge } from './ExtensionStatusBadge';
import { CreateExtensionModal } from './CreateExtensionModal';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';
import {
  useContractExtensions,
  useCreateExtension,
  useActivateExtension,
  useConfirmExtension,
  useDeclineExtension,
  useCancelExtension,
} from '@/hooks/useContractExtensionHooks';
import type {
  ContractExtensionResponse,
  RentAdjustmentType,
} from '@/types/contractExtension';
import type { ContractResponseStatus } from '@/generated/models';
import { ConfirmDialog } from '@buurman/ui';

interface ExtensionTimelineProps {
  contractIdentifier: string;
  contractStatus: ContractResponseStatus;
  currency: string;
  currentRentAmount: number;
  currentEndDate?: string;
  renewalTermMonths?: number;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
}

export const ExtensionTimeline = ({
  contractIdentifier,
  contractStatus,
  currency,
  currentRentAmount,
  currentEndDate,
  renewalTermMonths,
  rentAdjustmentType,
  rentAdjustmentValue,
}: ExtensionTimelineProps) => {
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [declineTarget, setDeclineTarget] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<string | null>(null);
  const [declineReason, setDeclineReason] = useState('');

  const { data: extensionsPage, isLoading } =
    useContractExtensions(contractIdentifier);
  const createExtension = useCreateExtension(contractIdentifier);
  const activateExtension = useActivateExtension(contractIdentifier);
  const confirmExtension = useConfirmExtension(contractIdentifier);
  const declineExtension = useDeclineExtension(contractIdentifier);
  const cancelExtension = useCancelExtension(contractIdentifier);

  const extensions = extensionsPage?.content ?? [];
  const canCreate = canEditData && contractStatus === 'ACTIVE';

  const handleCreate = (request: Parameters<typeof createExtension.mutate>[0]) => {
    createExtension.mutate(request, {
      onSuccess: () => setShowCreateModal(false),
    });
  };

  const handleActivate = (extensionId: string) => {
    activateExtension.mutate(extensionId);
  };

  const handleConfirm = (extensionId: string) => {
    confirmExtension.mutate(extensionId);
  };

  const handleDecline = () => {
    if (!declineTarget) {
      return;
    }
    declineExtension.mutate(
      { extensionId: declineTarget, request: { reason: declineReason || undefined } },
      {
        onSuccess: () => {
          setDeclineTarget(null);
          setDeclineReason('');
        },
      }
    );
  };

  const handleCancel = () => {
    if (!cancelTarget) {
      return;
    }
    cancelExtension.mutate(cancelTarget, {
      onSuccess: () => setCancelTarget(null),
    });
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-text-primary">
          Extensions
          {extensions.length > 0 && (
            <span className="ml-2 text-sm font-normal text-text-secondary">
              ({extensions.length})
            </span>
          )}
        </h3>
        {canCreate && (
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-primary-500 bg-primary-500/10 rounded-md hover:bg-primary-500/20 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" />
            New Extension
          </button>
        )}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex justify-center py-8">
          <LoadingSpinner />
        </div>
      ) : extensions.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-sm text-text-secondary">
            No extensions for this contract
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {extensions.map((ext) => (
            <ExtensionCard
              key={ext.identifier}
              extension={ext}
              formatDate={formatDate}
              canEdit={canEditData}
              onActivate={() => handleActivate(ext.identifier)}
              onConfirm={() => handleConfirm(ext.identifier)}
              onDecline={() => setDeclineTarget(ext.identifier)}
              onCancel={() => setCancelTarget(ext.identifier)}
              isActivating={activateExtension.isPending}
              isConfirming={confirmExtension.isPending}
            />
          ))}
        </div>
      )}

      {/* Create Extension Modal */}
      {showCreateModal && (
        <CreateExtensionModal
          currentRentAmount={currentRentAmount}
          currency={currency}
          currentEndDate={currentEndDate}
          renewalTermMonths={renewalTermMonths}
          rentAdjustmentType={rentAdjustmentType}
          rentAdjustmentValue={rentAdjustmentValue}
          onClose={() => setShowCreateModal(false)}
          onConfirm={handleCreate}
          isLoading={createExtension.isPending}
        />
      )}

      {/* Decline Confirmation */}
      {declineTarget && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
            <div className="p-4 border-b border-border-default">
              <h3 className="text-lg font-semibold text-text-primary">
                Decline Extension
              </h3>
            </div>
            <form onSubmit={(e) => { e.preventDefault(); handleDecline(); }} onKeyDown={(e) => { if (e.key === 'Escape') { setDeclineTarget(null); setDeclineReason(''); } }}>
              <div className="p-4 space-y-3">
                <p className="text-sm text-text-secondary">
                  Are you sure you want to decline this extension?
                </p>
                <div>
                  <label
                    htmlFor="declineReason"
                    className="block text-sm font-medium text-text-secondary mb-1"
                  >
                    Reason (Optional)
                  </label>
                  <input
                    id="declineReason"
                    type="text"
                    value={declineReason}
                    onChange={(e) => setDeclineReason(e.target.value)}
                    className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
                    placeholder="Reason for declining..."
                    autoFocus
                  />
                </div>
              </div>
              <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default">
                <button
                  type="button"
                  onClick={() => {
                    setDeclineTarget(null);
                    setDeclineReason('');
                  }}
                  className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-medium text-white bg-error-bg-strong rounded-md hover:opacity-90 disabled:opacity-50"
                  disabled={declineExtension.isPending}
                >
                  {declineExtension.isPending ? 'Declining...' : 'Decline'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Confirmation */}
      {cancelTarget && (
        <ConfirmDialog
          title="Cancel Extension"
          message="Are you sure you want to cancel this extension? This action cannot be undone."
          confirmLabel="Cancel Extension"
          variant="danger"
          onConfirm={handleCancel}
          onCancel={() => setCancelTarget(null)}
          isLoading={cancelExtension.isPending}
        />
      )}
    </div>
  );
};

function ExtensionCard({
  extension,
  formatDate,
  canEdit,
  onActivate,
  onConfirm,
  onDecline,
  onCancel,
  isActivating,
  isConfirming,
}: {
  extension: ContractExtensionResponse;
  formatDate: (date: string) => string;
  canEdit: boolean;
  onActivate: () => void;
  onConfirm: () => void;
  onDecline: () => void;
  onCancel: () => void;
  isActivating: boolean;
  isConfirming: boolean;
}) {
  const rentChange =
    extension.previousRentAmount > 0
      ? ((extension.newRentAmount - extension.previousRentAmount) /
          extension.previousRentAmount) *
        100
      : 0;

  const isDraft = extension.status === 'DRAFT';

  return (
    <div className="border border-border-default rounded-lg p-4 bg-surface-card">
      <div className="flex items-start justify-between gap-3">
        {/* Left: Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-2">
            <span className="text-sm font-semibold text-text-primary">
              Extension #{extension.extensionNumber}
            </span>
            <ExtensionStatusBadge status={extension.status} />
            {extension.triggerType === 'AUTO' ? (
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-medium rounded-full bg-info-bg text-info-text">
                <Zap className="h-2.5 w-2.5" />
                Auto
              </span>
            ) : (
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-medium rounded-full bg-surface-inset text-text-secondary">
                <User className="h-2.5 w-2.5" />
                Manual
              </span>
            )}
          </div>

          {/* Date range */}
          <div className="text-sm text-text-secondary mb-1">
            {formatDate(extension.previousEndDate)}
            {extension.newEndDate && (
              <span className="text-text-primary font-medium">
                {' '}
                &rarr; {formatDate(extension.newEndDate)}
              </span>
            )}
          </div>

          {/* Rent change */}
          <div className="flex items-center gap-2 text-sm">
            <span className="text-text-secondary">
              {extension.previousRentCurrency} {extension.previousRentAmount.toFixed(2)}
            </span>
            <span className="text-text-primary font-medium">
              &rarr; {extension.newRentCurrency} {extension.newRentAmount.toFixed(2)}
            </span>
            {rentChange !== 0 && <RentChangeBadge change={rentChange} />}
          </div>

          {/* Notes */}
          {extension.notes && (
            <p className="text-xs text-text-muted mt-2">{extension.notes}</p>
          )}

          {/* Declined reason */}
          {extension.declinedReason && (
            <p className="text-xs text-error-text mt-1">
              Declined: {extension.declinedReason}
            </p>
          )}

          {/* Timestamps */}
          <p className="text-[11px] text-text-muted mt-2">
            Created {formatDate(extension.createdAt)}
            {extension.activatedAt &&
              ` \u00b7 Activated ${formatDate(extension.activatedAt)}`}
            {extension.confirmedAt &&
              ` \u00b7 Confirmed ${formatDate(extension.confirmedAt)}`}
          </p>
        </div>

        {/* Right: Actions */}
        {canEdit && isDraft && (
          <div className="flex items-center gap-1.5 shrink-0">
            <button
              onClick={onActivate}
              disabled={isActivating}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-success-text bg-success-bg rounded-md hover:opacity-80 transition-colors disabled:opacity-50"
              title="Activate this extension"
            >
              <Check className="h-3.5 w-3.5" />
              Activate
            </button>
            {!extension.confirmedAt && (
              <button
                onClick={onConfirm}
                disabled={isConfirming}
                className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-indigo-700 bg-indigo-50 rounded-md hover:opacity-80 transition-colors disabled:opacity-50 dark:text-indigo-300 dark:bg-indigo-500/10"
                title="Confirm this extension"
              >
                <CheckCircle className="h-3.5 w-3.5" />
                Confirm
              </button>
            )}
            <button
              onClick={onDecline}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-warning-text bg-warning-bg rounded-md hover:opacity-80 transition-colors"
              title="Decline this extension"
            >
              <XCircle className="h-3.5 w-3.5" />
              Decline
            </button>
            <button
              onClick={onCancel}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-error-text bg-error-bg rounded-md hover:opacity-80 transition-colors"
              title="Cancel this extension"
            >
              <Ban className="h-3.5 w-3.5" />
              Cancel
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function RentChangeBadge({ change }: { change: number }) {
  if (change > 0) {
    return (
      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-success-bg text-success-text">
        <TrendingUp className="h-3 w-3" />+{change.toFixed(1)}%
      </span>
    );
  }
  if (change < 0) {
    return (
      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-error-bg text-error-text">
        <TrendingDown className="h-3 w-3" />
        {change.toFixed(1)}%
      </span>
    );
  }
  return null;
}
