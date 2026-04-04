import { useNavigate } from 'react-router-dom';
import { Check, XCircle, FileStack } from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';
import { LoadingSpinner } from '@buurman/ui';
import type { ContractExtensionResponse } from '@/types/contractExtension';

interface PendingExtensionsPanelProps {
  extensions: ContractExtensionResponse[];
  isLoading: boolean;
  onActivate: (contractId: string, extensionId: string) => void;
  onDecline: (contractId: string, extensionId: string) => void;
  isActivating?: boolean;
}

export const PendingExtensionsPanel = ({
  extensions,
  isLoading,
  onActivate,
  onDecline,
  isActivating = false,
}: PendingExtensionsPanelProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const { canEditData } = useTeam();

  const draftExtensions = extensions.filter((ext) => ext.status === 'DRAFT');

  if (isLoading) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center gap-3 mb-4">
          <FileStack className="h-5 w-5 text-warning-text" />
          <h2 className="text-lg font-semibold text-text-primary">
            Pending Extensions
          </h2>
        </div>
        <div className="flex justify-center py-4">
          <LoadingSpinner />
        </div>
      </div>
    );
  }

  if (draftExtensions.length === 0) {
    return null;
  }

  return (
    <div className="bg-warning-bg rounded-lg shadow-sm border border-warning-border p-6">
      <div className="flex items-center gap-3 mb-4">
        <FileStack className="h-5 w-5 text-warning-text" />
        <div>
          <h2 className="text-lg font-semibold text-text-primary">
            Pending Extensions
          </h2>
          <p className="text-sm text-text-secondary">
            {draftExtensions.length} extension
            {draftExtensions.length !== 1 ? 's' : ''} awaiting action
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {draftExtensions.map((ext) => (
          <div
            key={ext.identifier}
            className="flex items-center justify-between bg-surface-card rounded-lg border border-border-default px-4 py-3"
          >
            <div
              className="flex-1 min-w-0 cursor-pointer"
              onClick={() =>
                navigate(`/contracts/${ext.contractIdentifier}?tab=extensions`)
              }
            >
              <div className="flex items-center gap-2">
                <span className="font-medium text-text-primary text-sm">
                  Extension #{ext.extensionNumber}
                </span>
                <span className="text-xs text-text-muted">
                  Contract #{ext.contractIdentifier}
                </span>
              </div>
              <p className="text-xs text-text-secondary mt-0.5">
                {ext.previousRentCurrency} {ext.previousRentAmount.toFixed(2)}{' '}
                &rarr; {ext.newRentCurrency} {ext.newRentAmount.toFixed(2)}
                {ext.newEndDate && (
                  <span className="ml-2">
                    until {formatDate(ext.newEndDate)}
                  </span>
                )}
              </p>
            </div>
            {canEditData && (
              <div className="flex items-center gap-1.5 shrink-0 ml-3">
                <button
                  onClick={() =>
                    onActivate(ext.contractIdentifier, ext.identifier)
                  }
                  disabled={isActivating}
                  className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-success-text bg-success-bg rounded-md hover:opacity-80 transition-colors disabled:opacity-50"
                  title="Activate"
                >
                  <Check className="h-3.5 w-3.5" />
                  Activate
                </button>
                <button
                  onClick={() =>
                    onDecline(ext.contractIdentifier, ext.identifier)
                  }
                  className="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-error-text bg-error-bg rounded-md hover:opacity-80 transition-colors"
                  title="Decline"
                >
                  <XCircle className="h-3.5 w-3.5" />
                  Decline
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};
