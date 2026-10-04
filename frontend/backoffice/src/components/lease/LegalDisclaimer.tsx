import { AlertTriangle } from 'lucide-react';

export const LEGAL_DISCLAIMER_TEXT =
  'NL residential clauses are draft legal text pending counsel review; other countries use placeholder text. Not vetted legal content.';

/** Standing notice (not an alert: it is static page context, not a status change). */
export const LegalDisclaimer = ({ className = '' }: { className?: string }) => (
  <div
    role="note"
    className={`flex items-start gap-3 rounded-lg border border-warning-border bg-warning-bg px-4 py-3 ${className}`}
  >
    <AlertTriangle
      className="mt-0.5 h-5 w-5 flex-shrink-0 text-warning-text"
      aria-hidden="true"
    />
    <p className="text-sm text-warning-text">{LEGAL_DISCLAIMER_TEXT}</p>
  </div>
);
