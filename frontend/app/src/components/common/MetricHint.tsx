/**
 * Dotted-underline tooltip for financial acronyms and jargon.
 * Renders a subtle hint indicator; on hover shows a dark tooltip with the definition.
 * If no hint is found for the label, renders plain text.
 */

import { useRef, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';

const HINT_KEYS: Record<string, string> = {
  // Investment metrics
  'Total ROI': 'metricHints.totalROI',
  'Annualized ROI': 'metricHints.annualizedROI',
  'Cap Rate': 'metricHints.capRate',
  'Cash-on-Cash': 'metricHints.cashOnCash',
  'Annual NOI': 'metricHints.annualNOI',
  'Gross Rent Multiplier': 'metricHints.grossRentMultiplier',
  // Property performance table
  'Monthly CF': 'metricHints.monthlyCF',
  CoC: 'metricHints.coc',
  'Data %': 'metricHints.dataPercent',
  // Dashboard summary cards
  'Wtd Cap Rate': 'metricHints.wtdCapRate',
  'Wtd Cash-on-Cash': 'metricHints.wtdCashOnCash',
  Occupancy: 'metricHints.occupancy',
  // Dashboard / reports
  'Occupancy Rate': 'metricHints.occupancyRate',
  'Net Profit': 'metricHints.netProfit',
  // Property financial form
  'HOA Fee': 'metricHints.hoaFee',
  'Maintenance Reserve': 'metricHints.maintenanceReserve',
  Depreciation: 'metricHints.depreciation',
  'Useful Life': 'metricHints.usefulLife',
  'Land Value': 'metricHints.landValue',
};

interface TooltipPos {
  bottom: number;
  left: number;
}

export function MetricHint({
  label,
  hint: explicitHint,
}: {
  label: string;
  hint?: string;
}) {
  const { t } = useTranslation('common');
  const hintKey = HINT_KEYS[label];
  const resolved = explicitHint ?? (hintKey ? t(hintKey) : undefined);
  const triggerRef = useRef<HTMLSpanElement>(null);
  const [pos, setPos] = useState<TooltipPos | null>(null);

  const handleMouseEnter = useCallback(() => {
    const el = triggerRef.current;
    if (!el) {
      return;
    }
    const rect = el.getBoundingClientRect();
    const left = Math.max(
      104,
      Math.min(window.innerWidth - 104, rect.left + rect.width / 2)
    );
    setPos({ bottom: window.innerHeight - rect.top + 8, left });
  }, []);

  const handleMouseLeave = useCallback(() => setPos(null), []);

  if (!resolved) {
    return <>{label}</>;
  }

  return (
    <span
      ref={triggerRef}
      className="cursor-help"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <span className="underline decoration-dotted decoration-neutral-400 dark:decoration-neutral-500 underline-offset-2 decoration-1">
        {label}
      </span>
      {pos && (
        <span
          className="pointer-events-none fixed z-50 w-max max-w-[200px] -translate-x-1/2 px-2.5 py-1.5 rounded-md bg-neutral-900 border border-border-default dark:border-border-strong shadow-lg text-[10px] leading-snug text-neutral-200 font-normal text-left"
          style={{ bottom: pos.bottom, left: pos.left }}
          role="tooltip"
        >
          {resolved}
          <span className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-neutral-900" />
        </span>
      )}
    </span>
  );
}
