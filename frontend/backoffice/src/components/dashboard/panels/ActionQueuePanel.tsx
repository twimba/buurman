import { Link } from 'react-router-dom';
import { BellOff, CheckCircle2 } from 'lucide-react';

import type { ActionItem } from '../../../generated/models';
import { severityRank, severityStyle } from '../../../lib/severity';
import { useActionQueue, useSnoozeActionItem } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

const formatAge = (minutes: number): string => {
  if (minutes < 60) {
    return `${minutes}m`;
  }
  if (minutes < 60 * 24) {
    return `${Math.floor(minutes / 60)}h`;
  }
  return `${Math.floor(minutes / (60 * 24))}d`;
};

const Row = ({ item }: { item: ActionItem }) => {
  const snooze = useSnoozeActionItem();
  const sev = severityStyle(item.severity);

  return (
    <li className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-surface-page">
      <Link
        to={item.deeplink}
        className="focus-ring flex min-w-0 flex-1 items-center gap-2 rounded text-left"
      >
        <span
          className={`h-2 w-2 shrink-0 rounded-full ${sev.dot}`}
          role="img"
          aria-label={sev.label}
        />
        <span className="truncate text-sm text-text-primary">{item.title}</span>
      </Link>
      <span className="shrink-0 rounded bg-neutral-100 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-text-muted">
        {item.category}
      </span>
      <span className="shrink-0 text-xs tabular-nums text-text-muted">
        {formatAge(item.ageMinutes)}
      </span>
      <button
        type="button"
        onClick={() => snooze.mutate({ itemKey: item.key, hours: 24 })}
        disabled={snooze.isPending}
        title="Snooze 24h"
        aria-label={`Snooze ${item.title} for 24 hours`}
        className="focus-ring shrink-0 rounded p-1 text-text-muted hover:text-text-primary disabled:opacity-50"
      >
        <BellOff className="h-3.5 w-3.5" aria-hidden="true" />
      </button>
    </li>
  );
};

export const ActionQueuePanel = () => {
  const { data, isLoading, isError, refetch } = useActionQueue();
  const items = [...(data?.items ?? [])].sort(
    (a, b) => severityRank(a.severity) - severityRank(b.severity)
  );

  return (
    <PanelShell
      title="Action queue"
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
      onRetry={refetch}
      headerRight={
        items.length > 0 ? (
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-semibold ${severityStyle(items[0].severity).chip}`}
          >
            {items.length}
          </span>
        ) : undefined
      }
    >
      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-1 py-6 text-center">
          <CheckCircle2
            className="h-5 w-5 text-emerald-500"
            aria-hidden="true"
          />
          <p className="text-xs text-text-secondary">
            All clear — nothing needs attention.
          </p>
        </div>
      ) : (
        <ul className="-mx-2 space-y-0.5">
          {items.map((item) => (
            <Row key={item.key} item={item} />
          ))}
        </ul>
      )}
    </PanelShell>
  );
};
