import type { CostSourceType } from '../../generated/models';

const STYLE: Record<
  CostSourceType,
  { label: string; cls: string; title: string }
> = {
  ACTUAL: {
    label: 'Live',
    cls: 'bg-success-bg text-success-text',
    title: 'Live API — the real billed amount pulled from the provider.',
  },
  ESTIMATED: {
    label: 'Estimated',
    cls: 'bg-warning-bg text-warning-text',
    title:
      'Estimated — computed from a formula (e.g. plan fee + live usage volume). You set the formula; we compute the total.',
  },
  SUBSCRIPTION: {
    label: 'Manual',
    cls: 'bg-info-bg text-info-text',
    title:
      'Manual — a flat monthly total you typed in directly (used when a provider has no cost API).',
  },
};

/**
 * Method badge for a cost figure: how the number was arrived at — Live (metered API), Estimated
 * (formula × live volume), or Manual (a flat total typed in). The label IS the disambiguation
 * between an inline manual amount and an estimate formula.
 */
export const SourceTypeBadge = ({ type }: { type: CostSourceType }) => {
  const s = STYLE[type];
  return (
    <span
      title={s.title}
      className={`rounded-md px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.06em] ${s.cls}`}
    >
      {s.label}
    </span>
  );
};
