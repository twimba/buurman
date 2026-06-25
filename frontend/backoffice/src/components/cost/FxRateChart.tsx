import { useId, useLayoutEffect, useMemo, useRef, useState } from 'react';

export interface FxPoint {
  date: string;
  rate: number;
}

/**
 * Compact, dependency-free area chart of a currency→EUR rate over time. Renders at the container's
 * real pixel width (measured) with a 1:1 viewBox, so labels stay crisp and undistorted.
 */
export const FxRateChart = ({
  data,
  currency = 'USD',
}: {
  data: FxPoint[];
  currency?: string;
}) => {
  const gradientId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(720);
  const H = 160;
  const padX = 44;
  const padTop = 18;
  const padBottom = 26;

  useLayoutEffect(() => {
    const el = containerRef.current;
    if (!el) {
      return;
    }
    const measure = () => setWidth(el.clientWidth || 720);
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const model = useMemo(() => {
    if (data.length === 0) {
      return null;
    }
    const rates = data.map((d) => d.rate);
    let min = Math.min(...rates);
    let max = Math.max(...rates);
    if (min === max) {
      const pad = min === 0 ? 1 : Math.abs(min) * 0.05;
      min -= pad;
      max += pad;
    }
    const n = data.length;
    const x = (i: number) =>
      n === 1 ? width / 2 : padX + (i / (n - 1)) * (width - padX - 12);
    const y = (r: number) =>
      padTop + (1 - (r - min) / (max - min)) * (H - padTop - padBottom);

    const pts = data.map((d, i) => ({ px: x(i), py: y(d.rate), ...d }));
    const line = pts
      .map((p) => `${p.px.toFixed(1)},${p.py.toFixed(1)}`)
      .join(' ');
    const baseY = H - padBottom;
    const area =
      `${pts[0].px.toFixed(1)},${baseY} ` +
      pts.map((p) => `${p.px.toFixed(1)},${p.py.toFixed(1)}`).join(' ') +
      ` ${pts[pts.length - 1].px.toFixed(1)},${baseY}`;
    return { min, max, pts, line, area, last: pts[pts.length - 1] };
  }, [data, width]);

  const fmt = (v: number) =>
    v.toLocaleString(undefined, { maximumFractionDigits: 4 });

  return (
    <div ref={containerRef} className="w-full">
      {!model ? (
        <div className="flex h-[160px] items-center justify-center text-xs text-text-secondary">
          No history yet — refresh or set a rate to start the series.
        </div>
      ) : (
        <svg
          width={width}
          height={H}
          role="img"
          aria-label={`${currency} to EUR rate over time`}
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--sparkline-stroke)" stopOpacity="0.35" />
              <stop offset="100%" stopColor="var(--sparkline-stroke)" stopOpacity="0" />
            </linearGradient>
          </defs>

          {/* min / max guide lines + labels */}
          {[model.max, model.min].map((r, i) => {
            const py = padTop + i * (H - padTop - padBottom);
            return (
              <g key={r}>
                <line
                  x1={padX}
                  x2={width - 12}
                  y1={py}
                  y2={py}
                  className="text-text-muted"
                  stroke="currentColor"
                  strokeOpacity="0.15"
                  strokeWidth="1"
                />
                <text
                  x={padX - 6}
                  y={py + 3}
                  textAnchor="end"
                  className="fill-text-muted"
                  fontSize="10"
                >
                  {fmt(r)}
                </text>
              </g>
            );
          })}

          <polygon points={model.area} fill={`url(#${gradientId})`} />
          {model.pts.length > 1 && (
            <polyline
              points={model.line}
              fill="none"
              stroke="var(--sparkline-stroke)"
              strokeWidth="2"
              strokeLinejoin="round"
              strokeLinecap="round"
            />
          )}
          <circle
            cx={model.last.px}
            cy={model.last.py}
            r="3.5"
            fill="var(--sparkline-stroke)"
          />

          <text x={padX} y={H - 8} className="fill-text-muted" fontSize="10">
            {model.pts[0].date}
          </text>
          <text
            x={width - 12}
            y={H - 8}
            textAnchor="end"
            className="fill-text-secondary"
            fontSize="10"
          >
            {model.last.date} · {fmt(model.last.rate)}
          </text>
        </svg>
      )}
    </div>
  );
};
