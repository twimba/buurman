import { ExternalLink, X } from 'lucide-react';
import type { GoogleSheetExportResponse as GoogleSheetExportResult } from '@/generated/models';

interface GoogleSheetExportPillProps {
  result: GoogleSheetExportResult | null;
  onDismiss: () => void;
  /** Compact rendering for tight headers. */
  size?: 'sm' | 'md';
}

/**
 * Persistent "Open in Google Sheets" affordance shown next to the export control after a
 * successful Google Sheets export. Pairs with `useGoogleSheetsExport` — the hook returns the most
 * recent result; this component renders it as a clickable chip. The user can dismiss it via the
 * small × to clear the slot.
 *
 * Rationale: per spec FR-10 we do not auto-open a new tab (popup blockers are unreliable for
 * post-await opens), but we still want a one-click path to the resulting sheet that survives the
 * success toast fading out.
 */
export const GoogleSheetExportPill = ({
  result,
  onDismiss,
  size = 'md',
}: GoogleSheetExportPillProps) => {
  if (!result) {
    return null;
  }
  const isSmall = size === 'sm';
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md border border-success-border bg-success-bg text-success-text ${
        isSmall ? 'px-2 py-1 text-xs' : 'px-3 py-1.5 text-sm'
      }`}
    >
      <a
        href={result.url}
        target="_blank"
        rel="noopener noreferrer"
        className="inline-flex items-center gap-1.5 font-medium hover:underline"
      >
        <ExternalLink className={isSmall ? 'h-3 w-3' : 'h-3.5 w-3.5'} />
        Open last Google Sheet
      </a>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss"
        className="opacity-60 hover:opacity-100"
      >
        <X className={isSmall ? 'h-3 w-3' : 'h-3.5 w-3.5'} />
      </button>
    </span>
  );
};
