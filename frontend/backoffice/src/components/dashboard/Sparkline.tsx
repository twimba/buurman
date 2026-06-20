interface SparklineProps {
  values: number[];
  width?: number;
  height?: number;
  className?: string;
}

/**
 * Tiny inline SVG sparkline. Decorative only (aria-hidden) — the numeric value it accompanies is
 * always shown as text. Renders nothing until there are at least two points (no fabricated trend).
 */
export const Sparkline = ({
  values,
  width = 56,
  height = 18,
  className,
}: SparklineProps) => {
  if (!values || values.length < 2) {
    return null;
  }
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const stepX = width / (values.length - 1);
  const points = values
    .map((v, i) => {
      const x = i * stepX;
      const y = height - ((v - min) / range) * height;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <polyline
        points={points}
        fill="none"
        stroke="var(--sparkline-stroke)"
        strokeWidth={1.5}
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  );
};
