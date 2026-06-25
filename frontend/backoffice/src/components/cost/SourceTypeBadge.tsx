import type { CostSourceType } from '../../generated/models';

const STYLE: Record<CostSourceType, { label: string; cls: string }> = {
  ACTUAL: { label: 'Actual', cls: 'bg-success-bg text-success-text' },
  ESTIMATED: { label: 'Estimated', cls: 'bg-warning-bg text-warning-text' },
  SUBSCRIPTION: { label: 'Flat', cls: 'bg-info-bg text-info-text' },
};

/** Confidence badge for a cost figure — actual bill vs run-rate estimate vs flat subscription. */
export const SourceTypeBadge = ({ type }: { type: CostSourceType }) => {
  const s = STYLE[type];
  return (
    <span
      className={`rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${s.cls}`}
    >
      {s.label}
    </span>
  );
};
