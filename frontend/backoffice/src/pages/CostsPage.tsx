import { useRef, useState } from 'react';
import {
  ArrowDownRight,
  ArrowUpRight,
  Check,
  Minus,
  Pencil,
  RefreshCw,
  X,
} from 'lucide-react';

import { Link } from 'react-router-dom';

import type { CostSourceType, ProviderCost } from '../generated/models';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EurInput } from '../components/cost/EurInput';
import { ProviderSettingsCard } from '../components/cost/ProviderSettingsCard';
import { SourceTypeBadge } from '../components/cost/SourceTypeBadge';
import {
  useCostOverview,
  useRefreshCost,
  useSetManualCost,
} from '../hooks/cost';
import { eurMinorToInput, formatEurMinor, parseEurToMinor } from '../lib/money';

/**
 * A provider takes a manual flat amount only when it's a flat/subscription provider (e.g. Better
 * Stack), or a live-API provider whose API is currently unavailable — both surface as SUBSCRIPTION.
 * Formula providers (Mailgun, always ESTIMATED) are managed via their formula, never manually.
 */
const isEditable = (p: ProviderCost): boolean =>
  p.sourceType === 'SUBSCRIPTION';

const monthLabel = (iso: string): string => {
  const [y, m] = iso.split('-');
  const date = new Date(Number(y), Number(m) - 1, 1);
  return Number.isNaN(date.getTime())
    ? iso
    : date.toLocaleDateString(undefined, { month: 'short' });
};

/** Bar colour per cost method, so the bar itself reads as the legend. */
const barColor = (type: CostSourceType): string =>
  type === 'ACTUAL'
    ? 'var(--color-primary-600)'
    : type === 'ESTIMATED'
      ? 'var(--severity-warn)'
      : 'var(--severity-info)';

type Sev = 'ok' | 'warn' | 'crit';

/** Relative "updated Xh ago" + a freshness severity, so a stalled snapshot job reads as a warning. */
const freshness = (
  asOf?: string | null
): { sev: Sev; label: string } | null => {
  if (!asOf) {
    return null;
  }
  const ms = Date.now() - new Date(asOf).getTime();
  if (Number.isNaN(ms)) {
    return null;
  }
  const mins = Math.round(ms / 60000);
  const hours = ms / 3.6e6;
  const sev: Sev = hours <= 24 ? 'ok' : hours <= 72 ? 'warn' : 'crit';
  const label =
    mins < 1
      ? 'just now'
      : mins < 60
        ? `${mins}m ago`
        : hours < 48
          ? `${Math.round(hours)}h ago`
          : `${Math.round(hours / 24)}d ago`;
  return { sev, label };
};

export const CostsPage = () => {
  const { data, isLoading, isError } = useCostOverview();
  const refresh = useRefreshCost();
  const setManual = useSetManualCost();
  const [editing, setEditing] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');
  const [editError, setEditError] = useState<string | null>(null);
  // The trigger that opened the editor, so focus returns to it on save/cancel (a11y).
  const triggerRef = useRef<HTMLButtonElement | null>(null);

  const startEdit = (p: ProviderCost, trigger: HTMLButtonElement) => {
    triggerRef.current = trigger;
    setEditing(p.provider);
    setEditError(null);
    setEditValue(p.available ? eurMinorToInput(p.amountEurMinor) : '');
  };
  const closeEdit = () => {
    setEditing(null);
    setEditError(null);
    triggerRef.current?.focus();
  };
  const saveEdit = (provider: string) => {
    const minor = parseEurToMinor(editValue);
    if (minor === null) {
      setEditError('Enter a valid amount.');
      return;
    }
    // Only close on success — a failed write keeps the editor open and surfaces a toast (onError).
    setManual.mutate(
      { provider, amountEur: minor / 100 },
      { onSuccess: () => closeEdit() }
    );
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
  const total = data.totalMonthlyEurMinor;
  const available = data.providers.filter((p) => p.available);
  const maxProvider = data.providers.reduce(
    (m, p) => Math.max(m, p.amountEurMinor),
    0
  );
  const maxTrend = data.trend.reduce((m, t) => Math.max(m, t.totalEurMinor), 0);

  // Confidence split: how much of the run-rate is metered (Live) vs estimated/manual.
  const meteredMinor = available
    .filter((p) => p.sourceType === 'ACTUAL')
    .reduce((s, p) => s + p.amountEurMinor, 0);
  const meteredPct = total > 0 ? Math.round((meteredMinor / total) * 100) : 0;

  // Projected month-end: flat (manual/subscription) costs are already whole-month; metered +
  // estimated spend scales by the share of the month elapsed. Naive but honest — labelled as such.
  const now = new Date();
  const dayOfMonth = now.getDate();
  const daysInMonth = new Date(
    now.getFullYear(),
    now.getMonth() + 1,
    0
  ).getDate();
  const flatMinor = available
    .filter((p) => p.sourceType === 'SUBSCRIPTION')
    .reduce((s, p) => s + p.amountEurMinor, 0);
  const variableMinor = total - flatMinor;
  const projectedMinor =
    dayOfMonth > 0 && dayOfMonth < daysInMonth
      ? Math.round(flatMinor + (variableMinor * daysInMonth) / dayOfMonth)
      : total;

  const fresh = freshness(data.asOf);
  const costToServe = data.insights.find((i) =>
    i.label.toLowerCase().includes('serve')
  );
  const momSev: Sev = mom == null || mom === 0 ? 'ok' : mom > 0 ? 'warn' : 'ok';

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-[-0.01em] text-text-primary">
            Costs
          </h1>
          <p className="mt-1 text-sm text-text-secondary">
            Infrastructure &amp; communications spend, normalized to{' '}
            {data.baseCurrency}.
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-2.5">
          {fresh && (
            <span
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium"
              style={{
                background: `var(--severity-${fresh.sev}-bg)`,
                color: `var(--severity-${fresh.sev})`,
              }}
              title={`Snapshot captured ${new Date(data.asOf as string).toLocaleString()}`}
            >
              <span
                className="h-1.5 w-1.5 rounded-full"
                style={{ background: `var(--severity-${fresh.sev})` }}
                aria-hidden="true"
              />
              {fresh.sev === 'ok' ? 'Fresh' : 'Stale'} · {fresh.label}
            </span>
          )}
          <button
            type="button"
            onClick={() => refresh.mutate()}
            disabled={refresh.isPending}
            title="Re-polls every provider live and writes a new snapshot"
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary disabled:opacity-50"
          >
            <RefreshCw
              className={`h-4 w-4 ${refresh.isPending ? 'animate-spin' : ''}`}
              aria-hidden="true"
            />
            Refresh
          </button>
        </div>
      </div>

      {/* Hero band — the numbers an exec scans first */}
      <div
        className="flex flex-col items-stretch divide-y divide-border-subtle overflow-hidden rounded-xl border border-border-subtle sm:flex-row sm:divide-x sm:divide-y-0"
        style={{
          background: 'var(--hero-gradient)',
          boxShadow: 'var(--shadow-card), var(--shadow-inner-top)',
        }}
      >
        <div className="flex flex-[1.3] items-center gap-3 px-5 py-4">
          <span
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl"
            style={{
              background: `var(--severity-${momSev}-bg)`,
              color: `var(--severity-${momSev})`,
              boxShadow: `var(--glow-${momSev})`,
            }}
            aria-hidden="true"
          >
            {mom != null && mom > 0 ? (
              <ArrowUpRight className="h-[22px] w-[22px]" />
            ) : mom != null && mom < 0 ? (
              <ArrowDownRight className="h-[22px] w-[22px]" />
            ) : (
              <Minus className="h-[22px] w-[22px]" />
            )}
          </span>
          <div className="min-w-0">
            <p className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-text-muted">
              Monthly run-rate
            </p>
            <p className="text-[28px] font-bold leading-none tracking-[-0.025em] text-text-primary tabular-nums">
              {formatEurMinor(total)}
            </p>
            <p className="mt-1.5 text-[11px] leading-[14px] text-text-muted">
              {mom != null ? (
                <span
                  className="font-medium tabular-nums"
                  style={{ color: `var(--severity-${momSev})` }}
                >
                  {mom > 0 ? '+' : ''}
                  {mom.toFixed(1)}% vs last month
                </span>
              ) : (
                'across all providers'
              )}
            </p>
          </div>
        </div>

        <HeroMetric
          label="Projected month-end"
          value={formatEurMinor(projectedMinor)}
          sub="if usage holds"
        />
        <HeroMetric
          label="Metered share"
          value={`${meteredPct}%`}
          sub={`${100 - meteredPct}% estimated / manual`}
          accent
        />
        {costToServe && (
          <HeroMetric
            label={costToServe.label}
            value={costToServe.value}
            sub={costToServe.hint ?? ''}
          />
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Provider breakdown */}
        <section className="mc-panel motion-safe:animate-mc-rise rounded-xl border border-border-default bg-surface-card p-5 lg:col-span-2">
          <div className="mb-3 flex items-baseline justify-between">
            <h2 className="text-sm font-semibold text-text-primary">
              By provider
            </h2>
            <span className="text-[11px] text-text-muted">
              share of run-rate
            </span>
          </div>
          <ul className="space-y-3.5">
            {data.providers.map((p) => {
              const pct =
                total > 0 ? Math.round((p.amountEurMinor / total) * 100) : 0;
              return (
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
                        <span className="inline-flex flex-col items-end gap-0.5">
                          <span className="inline-flex items-center gap-1.5">
                            <EurInput
                              value={editValue}
                              onChange={setEditValue}
                              onEnter={() => saveEdit(p.provider)}
                              onEscape={closeEdit}
                              invalid={editError != null}
                              autoFocus
                              className="w-28"
                              ariaLabel={`Monthly EUR cost for ${p.displayName}`}
                            />
                            <button
                              type="button"
                              onClick={() => saveEdit(p.provider)}
                              disabled={setManual.isPending}
                              className="focus-ring rounded p-1 text-success-text hover:bg-surface-page disabled:opacity-50"
                              aria-label="Save"
                            >
                              <Check
                                className="h-3.5 w-3.5"
                                aria-hidden="true"
                              />
                            </button>
                            <button
                              type="button"
                              onClick={closeEdit}
                              className="focus-ring rounded p-1 text-text-muted hover:bg-surface-page"
                              aria-label="Cancel"
                            >
                              <X className="h-3.5 w-3.5" aria-hidden="true" />
                            </button>
                          </span>
                          {editError && (
                            <span
                              role="alert"
                              className="text-[10px] text-error-text"
                            >
                              {editError}
                            </span>
                          )}
                        </span>
                      ) : (
                        <>
                          {p.available && (
                            <span className="text-[11px] text-text-muted tabular-nums">
                              {pct}%
                            </span>
                          )}
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
                              onClick={(e) => startEdit(p, e.currentTarget)}
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
                  {p.breakdown.length > 0 && (
                    <ul className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-[11px] text-text-muted">
                      {p.breakdown.map((li) => (
                        <li
                          key={li.label}
                          className="inline-flex items-center gap-1"
                        >
                          <span>{li.label}</span>
                          <span className="tabular-nums text-text-secondary">
                            {p.currency === 'EUR'
                              ? formatEurMinor(li.amountMinor)
                              : `${li.amountMinor}`}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                  <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-surface-page">
                    <div
                      className="motion-safe:animate-mc-grow-x h-full origin-left rounded-full"
                      style={{
                        width: `${maxProvider > 0 ? (p.amountEurMinor / maxProvider) * 100 : 0}%`,
                        background: barColor(p.sourceType),
                      }}
                    />
                  </div>
                </li>
              );
            })}
          </ul>
        </section>

        {/* Trend */}
        <section className="mc-panel motion-safe:animate-mc-rise rounded-xl border border-border-default bg-surface-card p-5">
          <h2 className="mb-3 text-sm font-semibold text-text-primary">
            Monthly trend
          </h2>
          {data.trend.length === 0 ? (
            <p className="py-4 text-center text-xs text-text-secondary">
              Trend appears once snapshots accrue across months.
            </p>
          ) : (
            <div
              className="flex h-32 items-end gap-2"
              role="img"
              aria-label={`Monthly cost trend: ${data.trend
                .map(
                  (t) =>
                    `${monthLabel(t.month)} ${formatEurMinor(t.totalEurMinor)}`
                )
                .join(', ')}`}
            >
              {data.trend.map((t, i) => {
                const current = i === data.trend.length - 1;
                return (
                  <div
                    key={t.month}
                    className="flex flex-1 flex-col items-center gap-1"
                  >
                    <div
                      className="motion-safe:animate-mc-grow-x w-full origin-bottom rounded-t"
                      style={{
                        height: `${t.totalEurMinor > 0 && maxTrend > 0 ? Math.max(2, (t.totalEurMinor / maxTrend) * 100) : 0}%`,
                        background: current
                          ? 'var(--color-primary-600)'
                          : 'var(--color-primary-300)',
                      }}
                      title={`${t.month}: ${formatEurMinor(t.totalEurMinor)}`}
                    />
                    <span
                      className={`text-[9px] ${current ? 'font-semibold text-text-secondary' : 'text-text-muted'}`}
                    >
                      {monthLabel(t.month)}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <ProviderSettingsCard />
      </div>

      <p className="text-xs text-text-secondary">
        Costs are normalized to {data.baseCurrency} using live FX rates.{' '}
        <Link
          to="/costs/fx"
          className="focus-ring rounded font-medium text-primary-600 hover:underline"
        >
          Manage FX rates →
        </Link>
      </p>
    </div>
  );
};

const HeroMetric = ({
  label,
  value,
  sub,
  accent,
}: {
  label: string;
  value: string;
  sub: string;
  accent?: boolean;
}) => (
  <div className="flex flex-1 flex-col justify-center px-5 py-4">
    <p className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-text-muted">
      {label}
    </p>
    <p
      className={`mt-1 text-[24px] font-bold leading-none tracking-[-0.025em] tabular-nums ${
        accent ? 'text-primary-600' : 'text-text-primary'
      }`}
    >
      {value}
    </p>
    <p className="mt-1.5 text-[11px] leading-[14px] text-text-muted">{sub}</p>
  </div>
);
