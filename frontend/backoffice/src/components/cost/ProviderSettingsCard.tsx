import { useState } from 'react';
import { Save } from 'lucide-react';

import { useCostConfig, useUpdateCostConfig } from '../../hooks/cost';

/**
 * Edits the DB-backed provider cost parameters that used to live only in application.yml. Today
 * that's the Mailgun estimate inputs (Mailgun has no money API): a flat monthly plan fee plus a
 * per-accepted-email rate. Saved values override the seed and take effect on the next snapshot.
 */
export const ProviderSettingsCard = () => {
  const { data, isLoading } = useCostConfig();
  const update = useUpdateCostConfig();
  // Null = unedited (mirror the server value); a string = the in-progress local edit.
  const [baseEdit, setBaseEdit] = useState<string | null>(null);
  const [perEmailEdit, setPerEmailEdit] = useState<string | null>(null);

  const baseEur = baseEdit ?? (data ? data.mailgunBaseEur.toString() : '');
  const perEmailEur =
    perEmailEdit ?? (data ? data.mailgunPerEmailEur.toString() : '');

  const base = parseFloat(baseEur);
  const perEmail = parseFloat(perEmailEur);
  const invalid =
    Number.isNaN(base) || base < 0 || Number.isNaN(perEmail) || perEmail < 0;
  const dirty =
    !!data &&
    (base !== data.mailgunBaseEur || perEmail !== data.mailgunPerEmailEur);

  const save = () => {
    if (invalid) {
      return;
    }
    update.mutate(
      { mailgunBaseEur: base, mailgunPerEmailEur: perEmail },
      {
        onSuccess: () => {
          setBaseEdit(null);
          setPerEmailEdit(null);
        },
      }
    );
  };

  return (
    <div className="rounded-lg border border-border-default bg-surface-card p-5">
      <h2 className="text-sm font-semibold text-text-primary">
        Provider settings
      </h2>
      <p className="mt-1 text-xs text-text-secondary">
        Mailgun has no cost API, so its monthly figure is estimated from accepted
        volume: a flat plan fee plus a per-email rate.
      </p>

      {isLoading ? (
        <p className="py-4 text-xs text-text-muted">Loading…</p>
      ) : (
        <div className="mt-4 space-y-3">
          <label className="block">
            <span className="text-xs font-medium text-text-secondary">
              Mailgun plan fee (EUR / month)
            </span>
            <input
              type="number"
              min="0"
              step="0.01"
              value={baseEur}
              onChange={(e) => setBaseEdit(e.target.value)}
              className="mt-1 w-full rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm tabular-nums focus-ring"
            />
          </label>
          <label className="block">
            <span className="text-xs font-medium text-text-secondary">
              Mailgun rate (EUR / accepted email)
            </span>
            <input
              type="number"
              min="0"
              step="0.0001"
              value={perEmailEur}
              onChange={(e) => setPerEmailEdit(e.target.value)}
              className="mt-1 w-full rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm tabular-nums focus-ring"
            />
          </label>
          <button
            type="button"
            onClick={save}
            disabled={!dirty || invalid || update.isPending}
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg bg-primary-500 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:opacity-50"
          >
            <Save className="h-4 w-4" aria-hidden="true" />
            Save settings
          </button>
        </div>
      )}
    </div>
  );
};
