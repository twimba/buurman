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
  const { data, isLoading, isError } = useGeo();
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
      <p className="mb-2 text-[11px] leading-snug text-text-muted">
        Bubble size shows{' '}
        {metric === 'value' ? 'monthly contract value' : metric} per country
        (team’s default country). Hover for details. Excludes demo teams.
      </p>

      <Suspense fallback={<Skeleton className="h-[520px] w-full rounded-lg" />}>
        <GeoMap countries={countries} metric={metric} />
      </Suspense>

      {withoutCountry > 0 && (
        <p className="mt-2 text-[11px] text-text-muted">
          <span className="font-semibold text-text-secondary tabular-nums">
            {withoutCountry.toLocaleString()}
          </span>{' '}
          team{withoutCountry === 1 ? '' : 's'} with no country set
        </p>
      )}
    </PanelShell>
  );
};
