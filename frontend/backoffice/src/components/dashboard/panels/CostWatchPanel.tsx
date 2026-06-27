import { ArrowDownRight, ArrowUpRight, Minus } from 'lucide-react';

import { useCostWatch } from '../../../hooks/dashboard';
import { formatEurMinor } from '../../../lib/money';
import { SourceTypeBadge } from '../../cost/SourceTypeBadge';
import { PanelShell } from '../PanelShell';

export const CostWatchPanel = () => {
  const { data, isLoading, isError, refetch } = useCostWatch();
  const mom = data?.momChangePct;

  return (
    <PanelShell
      title="Cost watch"
      deeplink="/costs"
      status={data?.status}
      previewCta={data?.previewCta}
      docsLink={data?.docsLink}
      isLoading={isLoading}
      isError={isError}
      onRetry={refetch}
    >
      <div className="flex items-baseline gap-2">
        <span className="text-xl font-bold leading-none text-text-primary tabular-nums">
          {formatEurMinor(data?.totalMonthlyEurMinor ?? 0)}
        </span>
        <span className="text-[11px] text-text-muted">/mo</span>
        {mom !== undefined && mom !== null && (
          <span
            aria-label={`${
              mom > 0 ? 'Up' : mom < 0 ? 'Down' : 'Flat'
            } ${Math.abs(mom).toFixed(1)}% month over month`}
            className={`inline-flex items-center gap-0.5 text-xs font-semibold tabular-nums ${
              mom > 0
                ? 'text-error-text'
                : mom < 0
                  ? 'text-success-text'
                  : 'text-text-muted'
            }`}
          >
            {mom > 0 ? (
              <ArrowUpRight className="h-3 w-3" aria-hidden="true" />
            ) : mom < 0 ? (
              <ArrowDownRight className="h-3 w-3" aria-hidden="true" />
            ) : (
              <Minus className="h-3 w-3" aria-hidden="true" />
            )}
            {Math.abs(mom).toFixed(1)}%
          </span>
        )}
      </div>

      <ul className="mt-3 space-y-1">
        {(data?.topProviders ?? []).map((p) => (
          <li
            key={p.provider}
            className="flex items-center justify-between gap-2 text-xs"
          >
            <span className="flex items-center gap-1.5 truncate text-text-secondary">
              {p.displayName}
              <SourceTypeBadge type={p.sourceType} />
            </span>
            <span className="font-semibold text-text-primary tabular-nums">
              {formatEurMinor(p.amountEurMinor)}
            </span>
          </li>
        ))}
      </ul>

      {data?.headline && (
        <p className="mt-3 border-t border-border-subtle pt-2 text-xs text-text-muted">
          {data.headline.label}:{' '}
          <span className="font-semibold text-text-secondary">
            {data.headline.value}
          </span>
        </p>
      )}
    </PanelShell>
  );
};
