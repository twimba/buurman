import { Link } from 'react-router-dom';
import { Skeleton } from '@buurman/ui';

import type { Pillar } from '../../generated/models';
import { PanelStatus } from '../../generated/models';
import { useStatusStrip } from '../../hooks/dashboard';
import { Sparkline } from './Sparkline';

const SEV_LABEL: Record<string, string> = {
  crit: 'Critical',
  warn: 'Warning',
  ok: 'OK',
  info: 'Info',
};

const sevKey = (s?: string): string => (s && s in SEV_LABEL ? s : 'info');

const sparkColor = (s?: string): string =>
  s === 'crit'
    ? 'var(--severity-crit)'
    : s === 'warn'
      ? 'var(--severity-warn)'
      : 'var(--sparkline-stroke)';

const PillarTile = ({ pillar }: { pillar: Pillar }) => {
  const isPreview = pillar.status === PanelStatus.PREVIEW;
  const sev = sevKey(pillar.severity);
  const escalated =
    !isPreview && (pillar.severity === 'crit' || pillar.severity === 'warn');
  const linked = Boolean(pillar.deeplink) && !isPreview;

  const body = (
    <>
      <div className="flex items-center justify-between gap-1">
        <span className="truncate text-[10.5px] font-semibold uppercase tracking-[0.08em] text-text-muted">
          {pillar.label}
        </span>
        {!isPreview && pillar.severity && (
          <span
            className="h-2 w-2 shrink-0 rounded-full"
            style={{
              background: `var(--severity-${sev})`,
              boxShadow: `var(--glow-${sev})`,
            }}
            role="img"
            aria-label={SEV_LABEL[sev]}
          />
        )}
      </div>
      <div className="mt-1.5 flex items-end justify-between gap-1">
        {isPreview ? (
          <span
            className="rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-text-muted"
            style={{ backgroundColor: 'var(--severity-info-bg)' }}
          >
            Preview
          </span>
        ) : (
          <span className="text-[15px] font-bold leading-4 tracking-[-0.015em] text-text-primary tabular-nums">
            {pillar.value ?? '—'}
          </span>
        )}
        {!isPreview && (
          <Sparkline
            values={pillar.sparkline}
            color={sparkColor(pillar.severity)}
          />
        )}
      </div>
    </>
  );

  const className = `block rounded-lg border border-border-default bg-surface-card px-3 py-2 ${
    linked ? 'mc-panel focus-ring' : 'mc-panel-quiet'
  }`;
  const style = escalated
    ? { borderLeftWidth: '3px', borderLeftColor: `var(--severity-${sev})` }
    : undefined;
  const ariaLabel = `${pillar.label}: ${pillar.value ?? 'no data'}${
    !isPreview && pillar.severity ? `, ${SEV_LABEL[sev]}` : ''
  }`;

  if (linked && pillar.deeplink) {
    return (
      <Link
        to={pillar.deeplink}
        className={className}
        style={style}
        aria-label={ariaLabel}
      >
        {body}
      </Link>
    );
  }
  return (
    <div className={className} style={style}>
      {body}
    </div>
  );
};

/** The pinned, non-draggable 8-pillar status strip — the dashboard's "vitals" cluster. */
export const StatusStrip = () => {
  const { data, isLoading, isError } = useStatusStrip();

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4 xl:grid-cols-8">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-[58px] rounded-lg" />
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
    <div
      className="grid grid-cols-2 gap-2.5 rounded-xl border border-border-subtle p-1.5 sm:grid-cols-4 xl:grid-cols-8"
      style={{ boxShadow: 'var(--shadow-inner-top)' }}
    >
      {data.pillars.map((pillar) => (
        <PillarTile key={pillar.key} pillar={pillar} />
      ))}
    </div>
  );
};
