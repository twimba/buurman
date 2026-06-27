import { useState } from 'react';
import { FunctionSquare, Save } from 'lucide-react';

import { useCostConfig, useUpdateCostConfig } from '../../hooks/cost';
import { EurInput } from './EurInput';
import { parseDecimal } from '../../lib/money';

/** Compare two EUR-ish doubles at a fixed scale so float reformatting doesn't flip the dirty flag. */
const sameAt = (a: number, b: number, scale: number): boolean =>
  Math.round(a * scale) === Math.round(b * scale);

/**
 * Edits Mailgun's ESTIMATE FORMULA — distinct from a manual amount. Mailgun has no cost API, so its
 * monthly figure is computed: a flat plan fee + (accepted-email volume × a per-email rate), with the
 * volume pulled live from Mailgun. Here you set the two rate parameters (stored in {@code
 * cost_config}); the computed total shows up against Mailgun in the provider list. Contrast with the
 * inline ✎ in that list, which sets a flat *manual total* directly (stored in {@code
 * cost_manual_amount}) for providers that have no formula at all.
 */
export const ProviderSettingsCard = () => {
  const { data, isLoading } = useCostConfig();
  const update = useUpdateCostConfig();
  // Null = unedited (mirror the server value); a string = the in-progress local edit.
  const [baseEdit, setBaseEdit] = useState<string | null>(null);
  const [perEmailEdit, setPerEmailEdit] = useState<string | null>(null);

  const baseStr = baseEdit ?? (data ? data.mailgunBaseEur.toString() : '');
  const perEmailStr =
    perEmailEdit ?? (data ? data.mailgunPerEmailEur.toString() : '');

  const base = parseDecimal(baseStr);
  const perEmail = parseDecimal(perEmailStr);
  const invalid = base === null || perEmail === null;
  const dirty =
    !!data &&
    !invalid &&
    (!sameAt(base, data.mailgunBaseEur, 100) ||
      !sameAt(perEmail, data.mailgunPerEmailEur, 10000));

  const save = () => {
    if (invalid || base === null || perEmail === null) {
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
    <section className="mc-panel motion-safe:animate-mc-rise rounded-xl border border-border-default bg-surface-card p-5">
      <div className="flex items-center gap-2">
        <span
          className="flex h-7 w-7 items-center justify-center rounded-lg"
          style={{
            background: 'var(--severity-warn-bg)',
            color: 'var(--severity-warn)',
          }}
          aria-hidden="true"
        >
          <FunctionSquare className="h-4 w-4" />
        </span>
        <h2 className="text-sm font-semibold text-text-primary">
          Mailgun estimate formula
        </h2>
      </div>
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        Mailgun has no cost API, so its monthly figure is{' '}
        <span className="font-medium text-warning-text">estimated</span>: a flat
        plan fee plus accepted-email volume × a per-email rate. Set the
        parameters here; the computed total appears against Mailgun in the list.
      </p>

      {isLoading ? (
        <p className="py-4 text-xs text-text-muted">Loading…</p>
      ) : (
        <div className="mt-4 space-y-3">
          <label className="flex items-center justify-between gap-3">
            <span className="text-xs font-medium text-text-secondary">
              Plan fee
              <span className="block text-[11px] text-text-muted">
                per month
              </span>
            </span>
            <EurInput
              value={baseStr}
              onChange={setBaseEdit}
              invalid={base === null}
              className="w-32"
              ariaLabel="Mailgun plan fee in EUR per month"
            />
          </label>
          <label className="flex items-center justify-between gap-3">
            <span className="text-xs font-medium text-text-secondary">
              Per-email rate
              <span className="block text-[11px] text-text-muted">
                per accepted email
              </span>
            </span>
            <EurInput
              value={perEmailStr}
              onChange={setPerEmailEdit}
              invalid={perEmail === null}
              className="w-32"
              ariaLabel="Mailgun rate in EUR per accepted email"
            />
          </label>

          <p className="rounded-lg bg-surface-page px-3 py-2 text-[11px] leading-relaxed text-text-muted">
            <span className="font-medium text-text-secondary">Formula:</span> €
            {base != null ? base.toFixed(2) : '—'} +
            (accepted&nbsp;emails&nbsp;×&nbsp;€
            {perEmail != null ? perEmail.toFixed(4) : '—'})
          </p>

          <button
            type="button"
            onClick={save}
            disabled={!dirty || invalid || update.isPending}
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg bg-primary-500 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:opacity-50"
          >
            <Save className="h-4 w-4" aria-hidden="true" />
            Save formula
          </button>
        </div>
      )}
    </section>
  );
};
