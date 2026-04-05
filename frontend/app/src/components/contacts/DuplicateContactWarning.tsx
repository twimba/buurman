import { useState, useRef, useEffect, useCallback } from 'react';
import { AlertTriangle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useCheckContactDuplicates } from '@/hooks/useContactHooks';
import type {
  DuplicateCheckRequest,
  DuplicateCheckMatch,
} from '@/types/contact';

const DEBOUNCE_MS = 300;

/**
 * Hook that manages duplicate contact detection with debounced API calls.
 * Returns matches array and a check function to call on field changes.
 */
export function useDuplicateCheck() {
  const [matches, setMatches] = useState<DuplicateCheckMatch[]>([]);
  const [dismissed, setDismissed] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const mutation = useCheckContactDuplicates();
  const mutationRef = useRef(mutation);

  // Keep mutation ref current
  useEffect(() => {
    mutationRef.current = mutation;
  });

  useEffect(() => {
    return () => clearTimeout(timerRef.current);
  }, []);

  const check = useCallback((data: DuplicateCheckRequest) => {
    const hasIdentifyingInfo =
      data.email?.trim() || data.firstName?.trim() || data.companyName?.trim();
    if (!hasIdentifyingInfo) {
      setMatches([]);
      return;
    }
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      mutationRef.current.mutate(data, {
        onSuccess: (result) => {
          setMatches(result.matches);
          setDismissed(false);
        },
      });
    }, DEBOUNCE_MS);
  }, []);

  const reset = useCallback(() => {
    setMatches([]);
    setDismissed(false);
    clearTimeout(timerRef.current);
  }, []);

  const blocking = matches.length > 0 && !dismissed;

  return { matches, dismissed, setDismissed, check, reset, blocking };
}

/**
 * Inline warning banner showing potential duplicate contacts.
 * Compact variant for quick-add forms, full variant for the main contact form.
 */
export function DuplicateContactWarning({
  matches,
  dismissed,
  onDismiss,
  compact = false,
}: {
  matches: DuplicateCheckMatch[];
  dismissed: boolean;
  onDismiss: () => void;
  compact?: boolean;
}) {
  const { t } = useTranslation('tenants');

  if (matches.length === 0 || dismissed) {
    return null;
  }

  if (compact) {
    return (
      <div
        role="alert"
        className="flex items-start gap-2 rounded border border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-950/30 px-3 py-2 text-xs"
      >
        <AlertTriangle className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <span className="font-medium text-amber-800 dark:text-amber-200">
            Possible duplicate:
          </span>{' '}
          {matches.map((m, i) => (
            <span key={m.contact.identifier}>
              {i > 0 && ', '}
              <a
                href={`/contacts/${m.contact.identifier}`}
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium text-amber-700 dark:text-amber-300 hover:underline"
              >
                {m.contact.firstName}
                {m.contact.lastName ? ` ${m.contact.lastName}` : ''}
              </a>
              <span className="text-amber-500"> ({m.matchField})</span>
            </span>
          ))}
          <button
            type="button"
            onClick={onDismiss}
            className="ml-2 font-medium text-amber-700 dark:text-amber-300 hover:text-amber-900 dark:hover:text-amber-100 underline underline-offset-2"
          >
            Not a duplicate
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      role="alert"
      className="bg-amber-50 dark:bg-amber-950/30 border border-amber-300 dark:border-amber-700 rounded-lg p-4"
    >
      <div className="flex items-start gap-3">
        <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
            {t('duplicate.potentialDuplicate', { count: matches.length })}
          </p>
          <div className="mt-2 space-y-2">
            {matches.map((match) => (
              <div
                key={match.contact.identifier}
                className="flex items-center justify-between gap-2 text-sm"
              >
                <span className="text-amber-700 dark:text-amber-300">
                  <span className="font-medium">
                    {match.contact.firstName}
                    {match.contact.lastName ? ` ${match.contact.lastName}` : ''}
                  </span>
                  {match.contact.email && (
                    <span className="text-amber-600 dark:text-amber-400">
                      {' '}
                      — {match.contact.email}
                    </span>
                  )}
                  <span className="text-amber-500 dark:text-amber-500 ml-2">
                    {t('duplicate.matchInfo', {
                      matchType: match.matchType,
                      matchField: match.matchField,
                    })}
                  </span>
                </span>
                <a
                  href={`/contacts/${match.contact.identifier}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs font-medium text-amber-700 dark:text-amber-300 hover:underline flex-shrink-0"
                >
                  View
                </a>
              </div>
            ))}
          </div>
          <button
            type="button"
            onClick={onDismiss}
            className="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-amber-400 dark:border-amber-600 text-amber-700 dark:text-amber-300 bg-amber-100 dark:bg-amber-900/40 hover:bg-amber-200 dark:hover:bg-amber-900/60 transition-colors"
          >
            Not a duplicate — continue
          </button>
        </div>
      </div>
    </div>
  );
}
