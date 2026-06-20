import { useState } from 'react';
import {
  ArrowDownRight,
  ArrowUpRight,
  Check,
  Pencil,
  RefreshCw,
  X,
} from 'lucide-react';

import type { ProviderCost } from '../generated/models';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { SourceTypeBadge } from '../components/cost/SourceTypeBadge';
import {
  useCostOverview,
  useRefreshCost,
  useSetManualCost,
} from '../hooks/cost';
import { formatEurMinor } from '../lib/money';

/** A provider's cost can be set manually when it has no live API figure (or is a flat subscription). */
const isEditable = (p: ProviderCost): boolean =>
  p.sourceType === 'SUBSCRIPTION' || !p.available;

const monthLabel = (iso: string): string => {
  const [y, m] = iso.split('-');
  const date = new Date(Number(y), Number(m) - 1, 1);
  return Number.isNaN(date.getTime())
    ? iso
    : date.toLocaleDateString(undefined, { month: 'short' });
};

export const CostsPage = () => {
  const { data, isLoading, isError } = useCostOverview();
  const refresh = useRefreshCost();
  const setManual = useSetManualCost();
  const [editing, setEditing] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');

  const startEdit = (p: ProviderCost) => {
    setEditing(p.provider);
    setEditValue(p.available ? (p.amountEurMinor / 100).toString() : '');
  };
  const saveEdit = (provider: string) => {
    setManual.mutate({ provider, amountEur: parseFloat(editValue) || 0 });
    setEditing(null);
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading costs..." />;
  }
  if (isError || !data) {
    return (
      <p className="py-12 text-center text-error-text">Failed to load costs.</p>
    );
  }

  const mom = data.momChangePct;
  const maxProvider = data.providers.reduce(
    (m, p) => Math.max(m, p.amountEurMinor),
    0
  );
  const maxTrend = data.trend.reduce((m, t) => Math.max(m, t.totalEurMinor), 0);

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Costs</h1>
          <p className="mt-1 text-sm text-text-secondary">
            Infrastructure &amp; communications spend, normalized to{' '}
            {data.baseCurrency}.
            {data.asOf && (
              <span className="text-text-muted">
                {' '}
                · updated {new Date(data.asOf).toLocaleString()}
              </span>
            )}
          </p>
        </div>
        <button
          type="button"
          onClick={() => refresh.mutate()}
          disabled={refresh.isPending}
          className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary disabled:opacity-50"
        >
          <RefreshCw
            className={`h-4 w-4 ${refresh.isPending ? 'animate-spin' : ''}`}
            aria-hidden="true"
          />
          Refresh
        </button>
      </div>

      {/* Hero + insights */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
        <div className="rounded-lg border border-border-default bg-surface-card p-5 lg:col-span-1">
          <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
            Monthly run-rate
          </p>
          <p className="mt-1 text-3xl font-bold leading-none text-text-primary tabular-nums">
            {formatEurMinor(data.totalMonthlyEurMinor)}
          </p>
          {mom !== undefined && mom !== null && (
            <p
              className={`mt-2 inline-flex items-center gap-0.5 text-sm font-semibold tabular-nums ${
                mom > 0 ? 'text-red-600' : 'text-emerald-600'
              }`}
            >
              {mom > 0 ? (
                <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
              ) : (
                <ArrowDownRight className="h-4 w-4" aria-hidden="true" />
              )}
              {Math.abs(mom).toFixed(1)}% vs last month
            </p>
          )}
        </div>

        {data.insights.map((insight) => (
          <div
            key={insight.label}
            className="rounded-lg border border-border-default bg-surface-card p-5"
          >
            <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
              {insight.label}
            </p>
            <p className="mt-1 text-2xl font-bold leading-none text-text-primary tabular-nums">
              {insight.value}
            </p>
            {insight.hint && (
              <p className="mt-1 text-xs text-text-secondary">{insight.hint}</p>
            )}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Provider breakdown */}
        <div className="rounded-lg border border-border-default bg-surface-card p-5 lg:col-span-2">
          <h2 className="mb-3 text-sm font-semibold text-text-primary">
            By provider
          </h2>
          <ul className="space-y-3">
            {data.providers.map((p) => (
              <li key={p.provider}>
                <div className="flex items-baseline justify-between gap-2 text-sm">
                  <span className="flex items-center gap-2 truncate">
                    <span
                      className={
                        p.available ? 'text-text-primary' : 'text-text-muted'
                      }
                    >
                      {p.displayName}
                    </span>
                    <SourceTypeBadge type={p.sourceType} />
                  </span>
                  <span className="flex items-center gap-1.5">
                    {editing === p.provider ? (
                      <>
                        <input
                          type="number"
                          min="0"
                          step="0.01"
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          className="w-20 rounded border border-border-default bg-surface-card px-1.5 py-0.5 text-right text-xs tabular-nums"
                          aria-label={`Monthly EUR cost for ${p.displayName}`}
                        />
                        <button
                          type="button"
                          onClick={() => saveEdit(p.provider)}
                          disabled={setManual.isPending}
                          className="focus-ring rounded p-1 text-emerald-600 hover:bg-surface-page disabled:opacity-50"
                          aria-label="Save"
                        >
                          <Check className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditing(null)}
                          className="focus-ring rounded p-1 text-text-muted hover:bg-surface-page"
                          aria-label="Cancel"
                        >
                          <X className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                      </>
                    ) : (
                      <>
                        <span className="font-semibold text-text-primary tabular-nums">
                          {p.available ? (
                            formatEurMinor(p.amountEurMinor)
                          ) : (
                            <span className="text-xs font-normal text-text-muted">
                              {p.note ?? 'Not configured'}
                            </span>
                          )}
                        </span>
                        {isEditable(p) && (
                          <button
                            type="button"
                            onClick={() => startEdit(p)}
                            className="focus-ring rounded p-1 text-text-muted hover:text-text-primary"
                            aria-label={`Set ${p.displayName} monthly cost`}
                            title="Set manual cost"
                          >
                            <Pencil className="h-3 w-3" aria-hidden="true" />
                          </button>
                        )}
                      </>
                    )}
                  </span>
                </div>
                <div className="mt-1 h-2 overflow-hidden rounded-full bg-surface-page">
                  <div
                    className="h-full rounded-full bg-primary-500"
                    style={{
                      width: `${maxProvider > 0 ? (p.amountEurMinor / maxProvider) * 100 : 0}%`,
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </div>

        {/* Trend */}
        <div className="rounded-lg border border-border-default bg-surface-card p-5">
          <h2 className="mb-3 text-sm font-semibold text-text-primary">
            Monthly trend
          </h2>
          {data.trend.length === 0 ? (
            <p className="py-4 text-center text-xs text-text-secondary">
              Trend appears once snapshots accrue across months.
            </p>
          ) : (
            <div className="flex h-32 items-end gap-2">
              {data.trend.map((t) => (
                <div
                  key={t.month}
                  className="flex flex-1 flex-col items-center gap-1"
                >
                  <div
                    className="w-full rounded-t bg-primary-500"
                    style={{
                      height: `${maxTrend > 0 ? Math.max(2, (t.totalEurMinor / maxTrend) * 100) : 2}%`,
                    }}
                    title={`${t.month}: ${formatEurMinor(t.totalEurMinor)}`}
                  />
                  <span className="text-[9px] text-text-muted">
                    {monthLabel(t.month)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
