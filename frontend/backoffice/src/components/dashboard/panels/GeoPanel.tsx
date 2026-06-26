import { lazy, Suspense, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Maximize2, X } from 'lucide-react';

import { Skeleton } from '@buurman/ui';
import type { CountryStats } from '../../../generated/models';
import type { GeoMetric } from '../GeoMap';
import { useGeo } from '../../../hooks/dashboard';
import { useFocusTrap } from '../../../hooks/useFocusTrap';
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

/** Segmented metric switcher, shared by the inline panel header and the fullscreen overlay. */
const MetricSwitch = ({
  metric,
  onChange,
}: {
  metric: GeoMetric;
  onChange: (m: GeoMetric) => void;
}) => (
  <div className="flex items-center gap-0.5 rounded-md border border-border-default p-0.5 text-[11px]">
    {METRICS.map((m) => (
      <button
        key={m.key}
        type="button"
        onClick={() => onChange(m.key)}
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
);

const WithoutCountryNote = ({ count }: { count: number }) =>
  count > 0 ? (
    <p className="mt-2 shrink-0 text-[11px] text-text-muted">
      <span className="font-semibold text-text-secondary tabular-nums">
        {count.toLocaleString()}
      </span>{' '}
      team{count === 1 ? '' : 's'} with no country set
    </p>
  ) : null;

/** Fullscreen overlay: the same choropleth at viewport scale, with metric switching + Esc/close. */
const GeoFullscreen = ({
  countries,
  withoutCountry,
  metric,
  onMetric,
  onClose,
}: {
  countries: CountryStats[];
  withoutCountry: number;
  metric: GeoMetric;
  onMetric: (m: GeoMetric) => void;
  onClose: () => void;
}) => {
  const containerRef = useFocusTrap<HTMLDivElement>(true, onClose);

  // Modal contract: lock background scroll while open.
  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, []);

  return createPortal(
    <div
      ref={containerRef}
      className="fixed inset-0 z-50 flex flex-col bg-surface-page"
      role="dialog"
      aria-modal="true"
      aria-labelledby="geo-fullscreen-title"
    >
      <header className="flex items-center justify-between gap-3 border-b border-border-default bg-surface-card px-4 py-2.5">
        <h2
          id="geo-fullscreen-title"
          className="text-sm font-semibold text-text-primary"
        >
          Geo
        </h2>
        <div className="flex items-center gap-2">
          <MetricSwitch metric={metric} onChange={onMetric} />
          <button
            type="button"
            onClick={onClose}
            aria-label="Exit fullscreen"
            className="focus-ring inline-flex items-center gap-1.5 rounded-md border border-border-default px-2.5 py-1 text-xs font-medium text-text-secondary hover:text-text-primary"
          >
            <X className="h-3.5 w-3.5" aria-hidden="true" /> Close
          </button>
        </div>
      </header>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        <Suspense fallback={<Skeleton className="h-full w-full rounded-lg" />}>
          <div className="min-h-0 flex-1">
            <GeoMap countries={countries} metric={metric} />
          </div>
        </Suspense>
        <WithoutCountryNote count={withoutCountry} />
      </div>
    </div>,
    document.body
  );
};

export const GeoPanel = () => {
  const { data, isLoading, isError, refetch } = useGeo();
  const [metric, setMetric] = useState<GeoMetric>('teams');
  const [fullscreen, setFullscreen] = useState(false);
  const countries = data?.countries ?? [];
  const withoutCountry = data?.teamsWithoutCountry ?? 0;

  return (
    <>
      <PanelShell
        title="Geo"
        status={data?.status}
        previewCta={data?.previewCta}
        isLoading={isLoading}
        isError={isError}
        onRetry={refetch}
        headerRight={
          <div className="flex items-center gap-1.5">
            <MetricSwitch metric={metric} onChange={setMetric} />
            <button
              type="button"
              onClick={() => setFullscreen(true)}
              aria-label="View Geo map fullscreen"
              title="Fullscreen"
              className="focus-ring rounded-md border border-border-default p-1 text-text-secondary hover:text-text-primary"
            >
              <Maximize2 className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
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

          <WithoutCountryNote count={withoutCountry} />
        </div>
      </PanelShell>

      {fullscreen && (
        <GeoFullscreen
          countries={countries}
          withoutCountry={withoutCountry}
          metric={metric}
          onMetric={setMetric}
          onClose={() => setFullscreen(false)}
        />
      )}
    </>
  );
};
