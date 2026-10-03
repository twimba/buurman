import { AlertTriangle } from 'lucide-react';
import { isUnresolved } from '../../lib/leaseClauseTable';

interface ResolvedTextCellProps {
  text: string;
  i18nKey: string;
  /** Selected language is missing this key's own translation (text is the English fallback). */
  englishFallback: boolean;
  clamp?: boolean;
}

/** Resolved text with the i18n key beneath it; unresolved keys render as a red error state. */
export const ResolvedTextCell = ({
  text,
  i18nKey,
  englishFallback,
  clamp = false,
}: ResolvedTextCellProps) => {
  const unresolved = isUnresolved(text, i18nKey);
  return (
    <div className="min-w-0">
      {unresolved ? (
        <p className="flex items-center gap-1 text-sm text-error-text">
          <AlertTriangle
            className="h-3.5 w-3.5 flex-shrink-0"
            aria-hidden="true"
          />
          <span>Unresolved key</span>
        </p>
      ) : (
        <p
          title={clamp ? text : undefined}
          className={`text-sm ${
            englishFallback
              ? 'italic text-text-muted'
              : clamp
                ? 'text-text-secondary'
                : 'text-text-primary'
          } ${clamp ? 'line-clamp-2' : ''}`}
        >
          {englishFallback && (
            <span className="mr-1.5 inline-flex items-center gap-1 rounded bg-warning-bg px-1.5 py-0.5 align-middle text-[10px] font-medium not-italic text-warning-text">
              <AlertTriangle className="h-3 w-3" aria-hidden="true" />
              EN fallback
            </span>
          )}
          {text}
        </p>
      )}
      <p
        className={`mt-0.5 break-all font-mono text-xs ${
          unresolved ? 'text-error-text' : 'text-text-muted'
        }`}
      >
        {i18nKey}
      </p>
    </div>
  );
};
