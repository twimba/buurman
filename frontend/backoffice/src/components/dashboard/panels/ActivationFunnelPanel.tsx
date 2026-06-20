import { Fragment, useMemo } from 'react';
import { AlertTriangle, ArrowDown } from 'lucide-react';

import { useFunnel } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

const clamp = (n: number) => Math.min(100, Math.max(0, n));

/**
 * Centred trapezoid clip connecting this stage's width to the next stage's, so consecutive
 * segments fuse into an unmistakable funnel cone rather than a stack of bars.
 */
const coneClip = (topPct: number, bottomPct: number) => {
  const t = clamp(topPct);
  const b = clamp(bottomPct);
  const lt = (100 - t) / 2;
  const lb = (100 - b) / 2;
  return `polygon(${lt}% 0, ${100 - lt}% 0, ${100 - lb}% 100%, ${lb}% 100%)`;
};

const fmt = (n: number) => n.toLocaleString();

/** Round to a whole percent for compact labels, keeping one decimal only under 10%. */
const pct = (n: number) => (n < 10 ? Math.round(n * 10) / 10 : Math.round(n));

/** Step-conversion tone — green when healthy, escalating to red as the leak widens. */
const toneVar = (conversion: number) => {
  if (conversion >= 75) return 'var(--severity-ok)';
  if (conversion >= 50) return 'var(--severity-info)';
  if (conversion >= 25) return 'var(--severity-warn)';
  return 'var(--severity-crit)';
};

export const ActivationFunnelPanel = () => {
  const { data, isLoading, isError, refetch } = useFunnel();
  const stages = useMemo(() => data?.stages ?? [], [data]);

  const { top, last, overall, bottleneckIdx } = useMemo(() => {
    const top = stages[0]?.count ?? 0;
    const last = stages[stages.length - 1];
    let bottleneckIdx = -1;
    let worst = Number.POSITIVE_INFINITY;
    stages.forEach((s, i) => {
      if (i > 0 && s.pctOfPrevious < worst) {
        worst = s.pctOfPrevious;
        bottleneckIdx = i;
      }
    });
    return {
      top,
      last,
      overall: last?.pctOfTop ?? 0,
      bottleneckIdx,
    };
  }, [stages]);

  const srSummary =
    stages.length > 0
      ? `Activation funnel: ${fmt(last?.count ?? 0)} of ${fmt(top)} teams ` +
        `reached ${last?.label ?? 'the final stage'} (${pct(overall)}% overall).`
      : undefined;

  return (
    <PanelShell
      title="Activation funnel"
      status={data?.status}
      previewCta={data?.previewCta}
      docsLink={data?.docsLink}
      isLoading={isLoading}
      isError={isError}
      onRetry={refetch}
      srSummary={srSummary}
      headerRight={
        stages.length > 0 ? (
          <span className="flex items-baseline gap-1 tabular-nums">
            <span className="text-sm font-bold leading-none text-text-primary">
              {pct(overall)}%
            </span>
            <span
              className="text-[10px] font-medium uppercase tracking-[0.06em] text-text-muted"
              title={`Activated = reached "${last?.label ?? ''}"`}
            >
              activated
            </span>
          </span>
        ) : undefined
      }
    >
      {stages.length === 0 ? (
        <p className="py-6 text-center text-xs text-text-muted">
          No activity yet.
        </p>
      ) : (
        <div className="flex h-full flex-col">
          {/* Headline: the one number a COO scans for, with the human-readable count. */}
          <p className="text-xs leading-snug text-text-secondary">
            <span className="font-semibold tabular-nums text-text-primary">
              {fmt(last?.count ?? 0)}
            </span>{' '}
            of{' '}
            <span className="font-semibold tabular-nums text-text-primary">
              {fmt(top)}
            </span>{' '}
            teams reached the final stage.
          </p>

          {/* Connected trapezoids fuse into a funnel cone; deeper (surviving) stages
              render more vivid so the eye lands on who made it through. */}
          <div
            className="mt-3"
            role="group"
            aria-label="Activation funnel stages"
          >
            {stages.map((stage, i) => {
              const isLast = i === stages.length - 1;
              const nextPct = isLast
                ? stage.pctOfTop
                : (stages[i + 1]?.pctOfTop ?? stage.pctOfTop);
              const isBottleneck = i === bottleneckIdx;
              const conversion = stage.pctOfPrevious;
              const depth =
                stages.length > 1 ? 0.62 + (0.38 * i) / (stages.length - 1) : 1;
              return (
                <Fragment key={stage.label}>
                  {i > 0 && (
                    <div className="flex items-center justify-center gap-1.5 py-0.5 text-[10px] font-medium tabular-nums">
                      <ArrowDown
                        className="h-3 w-3 shrink-0"
                        style={{ color: toneVar(conversion) }}
                        aria-hidden="true"
                      />
                      <span
                        className="font-semibold"
                        style={{ color: toneVar(conversion) }}
                      >
                        {pct(conversion)}% continue
                      </span>
                      <span className="text-text-muted">·</span>
                      <span className="text-text-muted">
                        −{fmt(stage.dropOff)} lost
                      </span>
                      {isBottleneck && (
                        <span
                          className="ml-0.5 inline-flex items-center gap-0.5 rounded px-1 py-px text-[9px] font-semibold uppercase tracking-[0.04em]"
                          style={{
                            color: 'var(--severity-warn)',
                            backgroundColor: 'var(--severity-warn-bg)',
                          }}
                        >
                          <AlertTriangle
                            className="h-2.5 w-2.5"
                            aria-hidden="true"
                          />
                          Bottleneck
                        </span>
                      )}
                    </div>
                  )}

                  <div className="mb-1 flex items-baseline justify-between text-xs">
                    <span className="truncate text-text-secondary">
                      {stage.label}
                    </span>
                    <span className="font-semibold tabular-nums text-text-primary">
                      {fmt(stage.count)}
                      <span className="ml-1 font-normal text-text-muted">
                        {pct(stage.pctOfTop)}%
                      </span>
                    </span>
                  </div>
                  <div
                    className="h-7 transition-opacity duration-500 motion-reduce:transition-none"
                    style={{
                      clipPath: coneClip(stage.pctOfTop, nextPct),
                      background:
                        'linear-gradient(90deg, var(--color-primary-600), var(--color-primary-400))',
                      opacity: depth,
                    }}
                  />
                </Fragment>
              );
            })}
          </div>

          {/* Operator takeaway: name the worst leak explicitly. */}
          {bottleneckIdx > 0 && (
            <p className="mt-3 border-t border-border-subtle pt-2 text-[11px] leading-snug text-text-muted">
              Biggest leak{' '}
              <span className="font-medium text-text-secondary">
                {stages[bottleneckIdx - 1].label} →{' '}
                {stages[bottleneckIdx].label}
              </span>
              : only{' '}
              <span
                className="font-semibold tabular-nums"
                style={{ color: toneVar(stages[bottleneckIdx].pctOfPrevious) }}
              >
                {pct(stages[bottleneckIdx].pctOfPrevious)}%
              </span>{' '}
              continue.
            </p>
          )}
        </div>
      )}
    </PanelShell>
  );
};
