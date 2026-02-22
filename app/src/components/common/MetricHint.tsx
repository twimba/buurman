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

export function MetricHint({
  label,
  hint: explicitHint,
}: {
  label: string;
  hint?: string;
}) {
  const resolved = explicitHint ?? HINTS[label];
  const tooltipRef = useRef<HTMLSpanElement>(null);
  const [nudge, setNudge] = useState(0);

  const handleMouseEnter = useCallback(() => {
    const el = tooltipRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const vw = window.innerWidth;
    let offset = 0;
    if (rect.right > vw - 8) offset = vw - 8 - rect.right;
    else if (rect.left < 8) offset = 8 - rect.left;
    if (offset !== nudge) setNudge(offset);
  }, [nudge]);

  if (!resolved) return <>{label}</>;

  return (
    <span className="relative group/tip cursor-help" onMouseEnter={handleMouseEnter}>
      <span className="underline decoration-dotted decoration-[#9ca0b8] dark:decoration-[#5c6180] underline-offset-2 decoration-1">
        {label}
      </span>
      <span
        ref={tooltipRef}
        className="pointer-events-none absolute bottom-full left-1/2 mb-2 z-50 w-max max-w-[200px] px-2.5 py-1.5 rounded-md bg-[#1a1d2e] dark:bg-[#2a2e3f] border border-[#2a2e3f] dark:border-[#3a3f55] shadow-lg opacity-0 group-hover/tip:opacity-100 transition-opacity duration-150 text-[10px] leading-snug text-[#d4d7e8] font-normal"
        style={{ transform: `translateX(calc(-50% + ${nudge}px))` }}
        role="tooltip"
      >
        {resolved}
        <span className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#1a1d2e] dark:border-t-[#2a2e3f]" />
      </span>
    </span>
  );
}
