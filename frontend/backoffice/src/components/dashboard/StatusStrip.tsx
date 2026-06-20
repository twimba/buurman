import { Link } from 'react-router-dom';
import { Skeleton } from '@buurman/ui';

import type { Pillar } from '../../generated/models';
import { PanelStatus } from '../../generated/models';
import { severityStyle } from '../../lib/severity';
import { useStatusStrip } from '../../hooks/dashboard';
import { Sparkline } from './Sparkline';

const PillarTile = ({ pillar }: { pillar: Pillar }) => {
  const isPreview = pillar.status === PanelStatus.PREVIEW;
  const sev = severityStyle(pillar.severity);

  const body = (
    <>
      <div className="flex items-center justify-between gap-1">
        <span className="truncate text-[11px] font-medium uppercase tracking-wider text-text-muted">
          {pillar.label}
        </span>
        {!isPreview && pillar.severity && (
          <span
            className={`h-2 w-2 shrink-0 rounded-full ${sev.dot}`}
            role="img"
            aria-label={sev.label}
          />
        )}
      </div>
      <div className="mt-1 flex items-end justify-between gap-1">
        {isPreview ? (
          <span className="rounded bg-neutral-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-text-muted">
            Preview
          </span>
        ) : (
          <span className="text-base font-bold leading-none text-text-primary">
            {pillar.value ?? '—'}
          </span>
        )}
        {!isPreview && <Sparkline values={pillar.sparkline} />}
      </div>
    </>
  );

  const base =
    'block rounded-lg border border-border-default bg-surface-card px-3 py-2';

  if (pillar.deeplink && !isPreview) {
    return (
      <Link
        to={pillar.deeplink}
        className={`${base} focus-ring transition-shadow hover:shadow-sm`}
      >
        {body}
      </Link>
    );
  }
  return <div className={base}>{body}</div>;
};

/** The pinned, non-draggable 8-pillar status strip. */
export const StatusStrip = () => {
  const { data, isLoading, isError } = useStatusStrip();

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-4 xl:grid-cols-8">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-14 rounded-lg" />
        ))}
      </div>
    );
  }

  if (isError || !data) {
    return (
      <p className="text-xs text-error-text">Failed to load status strip.</p>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-4 xl:grid-cols-8">
      {data.pillars.map((pillar) => (
        <PillarTile key={pillar.key} pillar={pillar} />
      ))}
    </div>
  );
};
