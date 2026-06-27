interface SparklineProps {
  values: number[];
  width?: number;
  height?: number;
  className?: string;
  /** Stroke/fill color (defaults to the indigo sparkline token). */
  color?: string;
}

/**
 * Tiny inline SVG sparkline with an area fill and a live terminal dot. Decorative only
 * (aria-hidden) — the numeric value it accompanies is always shown as text. Renders nothing until
 * there are at least two points (no fabricated trend).
 */
export const Sparkline = ({
  values,
  width = 64,
  height = 20,
  className,
  color = 'var(--sparkline-stroke)',
}: SparklineProps) => {
  if (!values || values.length < 2) {
    return null;
  }
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const stepX = width / (values.length - 1);
  const coords = values.map((v, i) => {
    const x = i * stepX;
    const y = height - ((v - min) / range) * (height - 2) - 1;
    return [x, y] as const;
  });
  const line = coords
    .map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`)
    .join(' ');
  const area = `0,${height} ${line} ${width},${height}`;
  const [lastX, lastY] = coords[coords.length - 1];

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      className={className}
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <polygon points={area} fill="var(--sparkline-fill)" stroke="none" />
      <polyline
        points={line}
        fill="none"
        stroke={color}
        strokeWidth={1.5}
        strokeLinejoin="round"
        strokeLinecap="round"
        vectorEffect="non-scaling-stroke"
      />
      {/* Terminal dot drawn as a zero-length round-capped stroke so non-uniform scaling
          (preserveAspectRatio="none") can't stretch it into an ellipse. */}
      <line
        x1={lastX}
        y1={lastY}
        x2={lastX}
        y2={lastY}
        stroke={color}
        strokeWidth={3.5}
        strokeLinecap="round"
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  );
};
