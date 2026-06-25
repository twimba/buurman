import { createPortal } from 'react-dom';
import { Loader2, Trash2, X } from 'lucide-react';

import { useFocusTrap } from '../../hooks/useFocusTrap';

/** Destructive confirm for un-tracking a pair (also removes its stored rate history). */
export const RemovePairModal = ({
  currency,
  rateCount,
  isPending,
  onClose,
  onConfirm,
}: {
  currency: string | null;
  rateCount: number;
  isPending: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) => {
  const dialogRef = useFocusTrap<HTMLDivElement>(Boolean(currency), onClose);

  if (!currency) {
    return null;
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-surface-overlay p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="remove-pair-title"
      onClick={onClose}
    >
      <div
        ref={dialogRef}
        className="w-full max-w-md rounded-2xl border border-border-default bg-surface-card p-6"
        style={{ boxShadow: 'var(--shadow-raised)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-error-bg text-error-text">
              <Trash2 className="h-5 w-5" aria-hidden="true" />
            </span>
            <h2
              id="remove-pair-title"
              className="text-lg font-bold text-text-primary"
            >
              Stop tracking {currency} → EUR?
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="focus-ring rounded-md p-1 text-text-muted hover:text-text-primary"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <p className="text-sm leading-relaxed text-text-secondary">
          This removes the pair and deletes its{' '}
          <span className="font-medium text-text-primary">
            {rateCount.toLocaleString()} stored daily{' '}
            {rateCount === 1 ? 'rate' : 'rates'}
          </span>
          . Refresh and backfill will no longer fetch {currency}. Costs billed
          in {currency} will not be normalized until you track it again.
        </p>

        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={isPending}
            className="focus-ring rounded-lg px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPending}
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg bg-error-text px-4 py-2 text-sm font-medium text-white transition-colors hover:opacity-90 disabled:opacity-50"
          >
            {isPending && (
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
            )}
            {isPending ? 'Removing…' : 'Stop tracking'}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};
