/**
 * Dotted-underline tooltip for financial acronyms and jargon.
 * Renders a subtle hint indicator; on hover shows a dark tooltip with the definition.
 * If no hint is found for the label, renders plain text.
 */

import { useRef, useState, useCallback } from 'react';

const HINTS: Record<string, string> = {
  // Investment metrics
  'Total ROI': 'Return on Investment — total profit relative to total cost',
  'Annualized ROI': 'Return on Investment normalized to a yearly rate',
  'Cap Rate': 'Capitalization Rate — NOI divided by property value',
  'Cash-on-Cash': 'Annual pre-tax cash flow divided by total cash invested',
  'Annual NOI':
    'Net Operating Income — revenue minus operating expenses, before debt service',
  'Gross Rent Multiplier':
    'Purchase price divided by gross annual rent — lower is better',
  // Property performance table
  'Monthly CF':
    'Monthly Cash Flow — total rent collected minus operating expenses and debt service',
  CoC: 'Cash-on-Cash Return — annual pre-tax cash flow divided by total cash invested',
  'Data %':
    'Financial data completeness — how much cost and income data has been entered; higher means more accurate metrics',
  // Dashboard summary cards
  'Wtd Cap Rate':
    'Weighted Capitalization Rate — NOI divided by property value, weighted by portfolio value across all properties',
  'Wtd Cash-on-Cash':
    'Weighted Cash-on-Cash Return — annual pre-tax cash flow divided by total cash invested, weighted by equity across all properties',
  Occupancy: 'Percentage of units currently occupied by tenants',
  // Dashboard / reports
  'Occupancy Rate': 'Percentage of units currently rented out',
  'Net Profit': 'Total income minus total expenses for the period',
  // Property financial form
  'HOA Fee':
    'Homeowners Association fee — shared building/complex maintenance costs',
  'Maintenance Reserve':
    'Annual budget set aside for unexpected repairs and upkeep',
  Depreciation:
    'Tax deduction for the gradual loss of property value over time',
  'Useful Life':
    'Number of years over which the property is depreciated for tax purposes',
  'Land Value':
    'Value of the land alone (excluded from depreciation calculations)',
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
  const resolved = explicitHint ?? HINTS[label];
  const triggerRef = useRef<HTMLSpanElement>(null);
  const [pos, setPos] = useState<TooltipPos | null>(null);

  const handleMouseEnter = useCallback(() => {
    const el = triggerRef.current;
    if (!el) {
      return;
    }
    const rect = el.getBoundingClientRect();
    const left = Math.max(104, Math.min(window.innerWidth - 104, rect.left + rect.width / 2));
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
      <span className="underline decoration-dotted decoration-[#9ca0b8] dark:decoration-[#5c6180] underline-offset-2 decoration-1">
        {label}
      </span>
      {pos && (
        <span
          className="pointer-events-none fixed z-50 w-max max-w-[200px] -translate-x-1/2 px-2.5 py-1.5 rounded-md bg-[#1a1d2e] dark:bg-[#2a2e3f] border border-[#2a2e3f] dark:border-[#3a3f55] shadow-lg text-[10px] leading-snug text-[#d4d7e8] font-normal"
          style={{ bottom: pos.bottom, left: pos.left }}
          role="tooltip"
        >
          {resolved}
          <span className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#1a1d2e] dark:border-t-[#2a2e3f]" />
        </span>
      )}
    </span>
  );
}
