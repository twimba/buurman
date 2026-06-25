import { useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Check, Loader2, Plus, Search, X } from 'lucide-react';

import { CURRENCIES } from '../../lib/currencies';
import { useFocusTrap } from '../../hooks/useFocusTrap';

/**
 * Add a currency pair (source → EUR). Searchable list of supported currencies, with the ones
 * already tracked excluded. Picking a currency starts tracking it and fetches today's rate.
 */
export const AddPairModal = ({
  isPending,
  tracked,
  onClose,
  onConfirm,
}: {
  isPending: boolean;
  tracked: string[];
  onClose: () => void;
  onConfirm: (currency: string) => void;
}) => {
  const [query, setQuery] = useState('');
  const [picked, setPicked] = useState<string | null>(null);
  const dialogRef = useFocusTrap<HTMLDivElement>(true, onClose);

  const trackedSet = useMemo(() => new Set(tracked), [tracked]);
  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    return CURRENCIES.filter(
      (c) =>
        !trackedSet.has(c.code) &&
        (q === '' ||
          c.code.toLowerCase().includes(q) ||
          c.name.toLowerCase().includes(q))
    );
  }, [query, trackedSet]);

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-surface-overlay p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="add-pair-title"
      onClick={onClose}
    >
      <div
        ref={dialogRef}
        className="flex max-h-[80vh] w-full max-w-md flex-col rounded-2xl border border-border-default bg-surface-card p-6"
        style={{ boxShadow: 'var(--shadow-raised)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-1 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-50 text-primary-600">
              <Plus className="h-5 w-5" aria-hidden="true" />
            </span>
            <h2
              id="add-pair-title"
              className="text-lg font-bold text-text-primary"
            >
              Track a currency pair
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
        <p className="mb-4 text-sm leading-relaxed text-text-secondary">
          Pick a currency to convert to{' '}
          <span className="font-medium text-text-primary">EUR</span>. Buurman
          fetches the latest rate now, and one each day going forward.
        </p>

        <div className="relative mb-3">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted"
            aria-hidden="true"
          />
          <input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPicked(null);
            }}
            placeholder="Search currency or code…"
            className="w-full rounded-lg border border-border-default bg-surface-card py-2 pl-9 pr-3 text-sm"
          />
        </div>

        <div className="-mr-2 flex-1 overflow-y-auto pr-2">
          {results.length === 0 ? (
            <p className="py-8 text-center text-sm text-text-secondary">
              {query ? 'No matching currency.' : 'All currencies are tracked.'}
            </p>
          ) : (
            <ul className="space-y-0.5">
              {results.map((c) => {
                const isPicked = picked === c.code;
                return (
                  <li key={c.code}>
                    <button
                      type="button"
                      disabled={isPending}
                      aria-pressed={isPicked}
                      onClick={() => setPicked(isPicked ? null : c.code)}
                      className={`focus-ring flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-colors disabled:opacity-60 ${
                        isPicked
                          ? 'bg-primary-50 text-primary-700'
                          : 'hover:bg-surface-page'
                      }`}
                    >
                      <span className="w-10 font-mono text-sm font-semibold text-text-primary">
                        {c.code}
                      </span>
                      <span className="flex-1 text-sm text-text-secondary">
                        {c.name}
                      </span>
                      <span className="text-xs text-text-muted">→ EUR</span>
                      {isPicked && (
                        <Check
                          className="h-4 w-4 text-primary-600"
                          aria-hidden="true"
                        />
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {picked && (
          <div className="mt-4 flex items-center justify-end gap-2 border-t border-border-default pt-4">
            <button
              type="button"
              onClick={() => setPicked(null)}
              disabled={isPending}
              className="focus-ring rounded-lg px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => onConfirm(picked)}
              disabled={isPending}
              className="focus-ring inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
            >
              {isPending && (
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              )}
              {isPending ? 'Adding…' : `Add ${picked}`}
            </button>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};
