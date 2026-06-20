import type { CostSourceType } from '../../generated/models';

const STYLE: Record<CostSourceType, { label: string; cls: string }> = {
  ACTUAL: { label: 'Actual', cls: 'bg-emerald-50 text-emerald-700' },
  ESTIMATED: { label: 'Estimated', cls: 'bg-amber-50 text-amber-700' },
  SUBSCRIPTION: { label: 'Flat', cls: 'bg-primary-50 text-primary-700' },
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
