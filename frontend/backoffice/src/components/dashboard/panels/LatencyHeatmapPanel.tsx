import { useLatencyHeatmap } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

/** Maps a cell value to one of the 6 heatmap palette steps (relative to the panel's max). */
const heatColor = (value: number, max: number): string => {
  if (max <= 0 || value <= 0) {
    return 'var(--heatmap-0)';
  }
  const ratio = value / max;
  const level =
    ratio > 0.8 ? 5 : ratio > 0.6 ? 4 : ratio > 0.4 ? 3 : ratio > 0.2 ? 2 : 1;
  return `var(--heatmap-${level})`;
};

export const LatencyHeatmapPanel = () => {
  const { data, isLoading, isError } = useLatencyHeatmap();
  const bands = data?.bands ?? [];
  const columns = data?.columns ?? [];
  const max = columns.reduce(
    (m, col) => Math.max(m, ...(col.values.length ? col.values : [0])),
    0
  );

  return (
    <PanelShell
      title="Latency heatmap"
      status={data?.status}
      previewCta={data?.previewCta}
      docsLink={data?.docsLink}
      isLoading={isLoading}
      isError={isError}
    >
      {columns.length === 0 ? (
        <p className="py-4 text-center text-xs text-text-secondary">
          No request latency in the last 3 hours.
        </p>
      ) : (
        <div className="space-y-1">
          <div
            className="grid gap-px"
            style={{
              gridTemplateColumns: `minmax(46px, auto) repeat(${columns.length}, minmax(0, 1fr))`,
            }}
          >
            {/* One row per band, highest latency at the top. */}
            {bands
              .map((band, i) => ({ band, i }))
              .reverse()
              .map(({ band, i }) => (
                <div key={band} className="contents">
                  <span className="pr-1 text-right text-[9px] leading-[14px] text-text-muted">
                    {band}
                  </span>
                  {columns.map((col) => {
                    const v = col.values[i] ?? 0;
                    return (
                      <div
                        key={col.time + band}
                        className="h-[14px] rounded-[1px]"
                        style={{ backgroundColor: heatColor(v, max) }}
                        role="img"
                        aria-label={`${band} at ${col.time}: ${v.toLocaleString()} requests/s`}
                        title={`${band} · ${col.time}: ${v.toLocaleString()} req/s`}
                      />
                    );
                  })}
                </div>
              ))}
          </div>
          <div className="flex justify-between pl-[46px] text-[9px] tabular-nums text-text-muted">
            <span>{columns[0]?.time}</span>
            <span>{columns[columns.length - 1]?.time}</span>
          </div>
          {/* Legend: color encodes request throughput (peak = max in view). */}
          <div className="flex items-center gap-1.5 pl-[46px] pt-1 text-[9px] text-text-muted">
            <span>Less</span>
            {[0, 1, 2, 3, 4, 5].map((lvl) => (
              <span
                key={lvl}
                className="h-2 w-3 rounded-[1px]"
                style={{ backgroundColor: `var(--heatmap-${lvl})` }}
                aria-hidden="true"
              />
            ))}
            <span>More req/s</span>
          </div>
        </div>
      )}
    </PanelShell>
  );
};
