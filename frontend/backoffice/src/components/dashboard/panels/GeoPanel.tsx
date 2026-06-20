import { lazy, Suspense, useState } from 'react';
import { Globe } from 'lucide-react';

import type { GeoCountry } from '../../../generated/models';
import { useGeo } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

// Lazy: the Google Maps libraries (~60kB) load only when the map is opened.
const PropertyWorldMap = lazy(() =>
  import('../PropertyWorldMap').then((m) => ({ default: m.PropertyWorldMap }))
);

type GeoTab = 'teams' | 'properties';

const CountryBars = ({ rows }: { rows: GeoCountry[] }) => {
  const max = rows.reduce((m, c) => Math.max(m, c.count), 0);
  if (rows.length === 0) {
    return (
      <p className="py-4 text-center text-xs text-text-secondary">
        No country data yet.
      </p>
    );
  }
  return (
    <ul className="space-y-2">
      {rows.map((c) => (
        <li key={c.code}>
          <div className="flex items-baseline justify-between text-xs">
            <span className="truncate font-medium text-text-primary">
              {c.code}
            </span>
            <span className="font-semibold text-text-primary tabular-nums">
              {c.count.toLocaleString()}
            </span>
          </div>
          <div className="mt-1 h-2 overflow-hidden rounded-full bg-surface-page">
            <div
              className="h-full rounded-full bg-primary-500"
              style={{ width: `${max > 0 ? (c.count / max) * 100 : 0}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  );
};

export const GeoPanel = () => {
  const { data, isLoading, isError } = useGeo();
  const [tab, setTab] = useState<GeoTab>('teams');
  const [mapOpen, setMapOpen] = useState(false);

  const rows =
    tab === 'teams'
      ? (data?.teamCountries ?? [])
      : (data?.propertyCountries ?? []);

  return (
    <>
      <PanelShell
        title="Geo"
        status={data?.status}
        previewCta={data?.previewCta}
        isLoading={isLoading}
        isError={isError}
        headerRight={
          <button
            type="button"
            onClick={() => setMapOpen(true)}
            className="focus-ring inline-flex items-center gap-1 rounded-md border border-border-default px-2 py-1 text-xs font-medium text-text-secondary hover:text-text-primary"
          >
            <Globe className="h-3.5 w-3.5" aria-hidden="true" /> Map
          </button>
        }
      >
        <div className="mb-3 inline-flex rounded-md border border-border-default p-0.5 text-xs">
          {(['teams', 'properties'] as const).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setTab(t)}
              aria-pressed={tab === t}
              className={`focus-ring rounded px-2 py-0.5 font-medium capitalize transition-colors ${
                tab === t
                  ? 'bg-primary-600 text-white'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        <CountryBars rows={rows} />

        {tab === 'teams' && (data?.teamsWithoutCountry ?? 0) > 0 && (
          <p className="mt-3 border-t border-border-subtle pt-2 text-xs text-text-muted">
            <span className="font-semibold text-text-secondary tabular-nums">
              {data?.teamsWithoutCountry.toLocaleString()}
            </span>{' '}
            team{data?.teamsWithoutCountry === 1 ? '' : 's'} with no country set
          </p>
        )}
      </PanelShell>

      {mapOpen && (
        <Suspense fallback={null}>
          <PropertyWorldMap onClose={() => setMapOpen(false)} />
        </Suspense>
      )}
    </>
  );
};
