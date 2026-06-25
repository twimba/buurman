import { lazy, Suspense, useState } from 'react';

import { Skeleton } from '@buurman/ui';
import type { GeoMetric } from '../GeoMap';
import { useGeo } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

// Lazy so the Google Maps SDK loads with the panel, not the app shell.
const GeoMap = lazy(() =>
  import('../GeoMap').then((m) => ({ default: m.GeoMap }))
);

const METRICS: { key: GeoMetric; label: string }[] = [
  { key: 'teams', label: 'Teams' },
  { key: 'properties', label: 'Properties' },
  { key: 'value', label: 'Value' },
];

export const GeoPanel = () => {
  const { data, isLoading, isError, refetch } = useGeo();
  const [metric, setMetric] = useState<GeoMetric>('teams');
  const countries = data?.countries ?? [];
  const withoutCountry = data?.teamsWithoutCountry ?? 0;

  return (
    <PanelShell
      title="Geo"
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
      onRetry={refetch}
      headerRight={
        <div className="flex items-center gap-0.5 rounded-md border border-border-default p-0.5 text-[11px]">
          {METRICS.map((m) => (
            <button
              key={m.key}
              type="button"
              onClick={() => setMetric(m.key)}
              aria-pressed={metric === m.key}
              className={`focus-ring rounded px-2 py-0.5 font-medium transition-colors ${
                metric === m.key
                  ? 'bg-primary-600 text-white'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {m.label}
            </button>
          ))}
        </div>
      }
    >
      <div className="flex h-full flex-col">
        <Suspense
          fallback={
            <Skeleton className="h-full min-h-[320px] w-full rounded-lg" />
          }
        >
          <div className="min-h-0 flex-1">
            <GeoMap countries={countries} metric={metric} />
          </div>
        </Suspense>

        {withoutCountry > 0 && (
          <p className="mt-2 shrink-0 text-[11px] text-text-muted">
            <span className="font-semibold text-text-secondary tabular-nums">
              {withoutCountry.toLocaleString()}
            </span>{' '}
            team{withoutCountry === 1 ? '' : 's'} with no country set
          </p>
        )}
      </div>
    </PanelShell>
  );
};
