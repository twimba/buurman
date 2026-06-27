import { useState } from 'react';
import { createPortal } from 'react-dom';
import { CalendarClock, Loader2, X } from 'lucide-react';

import { useFocusTrap } from '../../hooks/useFocusTrap';

const oneYearAgo = (): string => {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 1);
  return d.toISOString().slice(0, 10);
};

/**
 * Confirmation modal for backfilling historical FX rates. Explains what will happen and asks the
 * user from which date Buurman should start fetching daily rates for all tracked pairs.
 */
export const BackfillFxModal = ({
  open,
  isPending,
  pairCount,
  onClose,
  onConfirm,
}: {
  open: boolean;
  isPending: boolean;
  pairCount?: number;
  onClose: () => void;
  onConfirm: (since: string) => void;
}) => {
  const [since, setSince] = useState(oneYearAgo);
  const today = new Date().toISOString().slice(0, 10);
  const dialogRef = useFocusTrap<HTMLDivElement>(open, onClose);

  if (!open) {
    return null;
  }

  const scopeLabel =
    pairCount && pairCount > 0
      ? `all ${pairCount} tracked ${pairCount === 1 ? 'pair' : 'pairs'}`
      : 'all tracked pairs';

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-surface-overlay p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="backfill-title"
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
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-50 text-primary-600">
              <CalendarClock className="h-5 w-5" aria-hidden="true" />
            </span>
            <h2
              id="backfill-title"
              className="text-lg font-bold text-text-primary"
            >
              Backfill FX history
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
          Buurman will backfill{' '}
          <span className="font-medium text-text-primary">{scopeLabel}</span>{' '}
          (each anchored on{' '}
          <span className="font-medium text-text-primary">EUR</span>), fetching
          the official daily rate for every day from the date below through
          today and storing the full history. Existing days are overwritten with
          the fetched value; weekends and holidays are skipped (no rate is
          published).
        </p>

        <label className="mt-5 flex flex-col gap-1">
          <span className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
            Fetch rates since
          </span>
          <input
            type="date"
            value={since}
            max={today}
            onChange={(e) => setSince(e.target.value)}
            className="rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm"
          />
        </label>

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
            onClick={() => onConfirm(since)}
            disabled={isPending || !since}
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
          >
            {isPending && (
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
            )}
            {isPending ? 'Fetching…' : 'Fetch history'}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};
