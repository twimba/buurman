import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Check, LayoutGrid, Pause, Play, RefreshCw } from 'lucide-react';

import { useDashboardContext } from '../../context/DashboardContext';
import { dashboardQueryKey, useStatusStrip } from '../../hooks/dashboard';

const worstSeverity = (severities: (string | undefined)[]): string => {
  if (severities.includes('crit')) {
    return 'crit';
  }
  if (severities.includes('warn')) {
    return 'warn';
  }
  return 'ok';
};

const agoLabel = (since: number, now: number): string => {
  const s = Math.max(0, Math.round((now - since) / 1000));
  if (s < 60) {
    return `${s}s ago`;
  }
  const m = Math.floor(s / 60);
  return m < 60 ? `${m}m ago` : `${Math.floor(m / 60)}h ago`;
};

/** Mission-control topbar: identity + live health pulse/clock + global refresh, Pause, edit. */
export const DashboardTopbar = () => {
  const queryClient = useQueryClient();
  const { paused, togglePaused, editing, toggleEditing } =
    useDashboardContext();
  const { data, dataUpdatedAt, isFetching } = useStatusStrip();

  // 1s tick drives the live clock + "refreshed Ns ago" (independent of data polling).
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  const sev = worstSeverity((data?.pillars ?? []).map((p) => p.severity));
  const refreshAll = () =>
    queryClient.invalidateQueries({ queryKey: dashboardQueryKey });

  const segItem = (active: boolean) =>
    `focus-ring inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-[13px] font-medium transition-colors ${
      active
        ? 'bg-surface-card text-text-primary'
        : 'text-text-secondary hover:bg-surface-card hover:text-text-primary'
    }`;

  return (
    <div className="flex items-start justify-between gap-3">
      <div className="flex items-center gap-3">
        <span className="relative flex h-2.5 w-2.5" aria-hidden="true">
          <span
            className="absolute inline-flex h-full w-full rounded-full opacity-60 motion-safe:animate-mc-pulse"
            style={{ background: `var(--severity-${sev})` }}
          />
          <span
            className="relative inline-flex h-2.5 w-2.5 rounded-full"
            style={{ background: `var(--severity-${sev})` }}
          />
        </span>
        <div>
          <h1 className="text-[20px] font-bold leading-6 tracking-[-0.02em] text-text-primary">
            Mission Control
          </h1>
          <p className="mt-0.5 flex items-center gap-2 text-[11px] leading-[14px] text-text-muted tabular-nums">
            <span>{new Date(now).toLocaleTimeString()}</span>
            <span aria-hidden="true">·</span>
            {paused ? (
              <span className="font-semibold text-amber-600">
                Paused — data may be stale
              </span>
            ) : (
              <span>
                Refreshed {dataUpdatedAt ? agoLabel(dataUpdatedAt, now) : '—'}
              </span>
            )}
          </p>
        </div>
      </div>

      <div
        className="flex items-center gap-1 rounded-lg border border-border-default bg-surface-inset p-1"
        style={{ boxShadow: 'var(--shadow-inner-top)' }}
      >
        <button
          type="button"
          onClick={toggleEditing}
          aria-pressed={editing}
          title={editing ? 'Finish editing layout' : 'Customize layout'}
          className={segItem(editing)}
          style={editing ? { boxShadow: 'var(--shadow-card)' } : undefined}
        >
          {editing ? (
            <>
              <Check className="h-4 w-4" aria-hidden="true" /> Done
            </>
          ) : (
            <>
              <LayoutGrid className="h-4 w-4" aria-hidden="true" /> Customize
            </>
          )}
        </button>
        <button
          type="button"
          onClick={togglePaused}
          aria-pressed={paused}
          title={paused ? 'Resume auto-refresh' : 'Pause auto-refresh'}
          className={segItem(paused)}
          style={paused ? { boxShadow: 'var(--shadow-card)' } : undefined}
        >
          {paused ? (
            <>
              <Play className="h-4 w-4" aria-hidden="true" /> Resume
            </>
          ) : (
            <>
              <Pause className="h-4 w-4" aria-hidden="true" /> Pause
            </>
          )}
        </button>
        <button
          type="button"
          onClick={refreshAll}
          title="Refresh all panels"
          className={segItem(false)}
        >
          <RefreshCw
            className={`h-4 w-4 ${isFetching ? 'motion-safe:animate-spin' : ''}`}
            aria-hidden="true"
          />{' '}
          Refresh
        </button>
      </div>
    </div>
  );
};
