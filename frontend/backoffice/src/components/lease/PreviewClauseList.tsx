import { useEffect, useRef } from 'react';
import { ChevronDown, ChevronUp, Lock, Pin } from 'lucide-react';
import type { LeasePreviewClause } from '../../generated/models';
import type { MoveButton } from '../../lib/leasePreviewClauses';

export interface FocusRequest {
  clauseKey: string;
  button: MoveButton;
  nonce: number;
}

interface PreviewClauseListProps {
  clauses: LeasePreviewClause[];
  onToggle: (clauseKey: string) => void;
  onMove: (clauseKey: string, direction: -1 | 1) => void;
  /** Disables every control (e.g. while the clause list is stale). */
  disabled?: boolean;
  focusRequest?: FocusRequest | null;
}

const MOVE_BUTTON =
  'rounded p-1 text-text-secondary hover:bg-surface-hover disabled:cursor-not-allowed disabled:opacity-40 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40';

/** `clauses` must already be in display order. */
export const PreviewClauseList = ({
  clauses,
  onToggle,
  onMove,
  disabled = false,
  focusRequest = null,
}: PreviewClauseListProps) => {
  const listRef = useRef<HTMLUListElement>(null);
  useEffect(() => {
    if (focusRequest) {
      listRef.current
        ?.querySelector<HTMLButtonElement>(
          `[data-move="${focusRequest.clauseKey}:${focusRequest.button}"]`
        )
        ?.focus();
    }
  }, [focusRequest]);
  return (
    <ul
      ref={listRef}
      aria-busy={disabled}
      className="divide-y divide-border-default rounded-lg border border-border-default"
    >
      {clauses.map((clause, index) => {
        const previous = clauses[index - 1];
        const next = clauses[index + 1];
        const locked = !clause.optional;
        return (
          <li
            key={clause.clauseKey}
            className="flex items-center gap-2 px-2 py-1.5"
          >
            <input
              type="checkbox"
              checked={clause.included}
              disabled={disabled || locked || clause.pinned}
              onChange={() => onToggle(clause.clauseKey)}
              aria-label={
                locked
                  ? `${clause.title} (required, always included)`
                  : `Include ${clause.title}`
              }
              className="h-4 w-4"
            />
            <span className="min-w-0 flex-1 text-sm text-text-primary">
              <span className="text-text-muted">
                {clause.included ? `Art. ${clause.articleNumber} ` : ''}
              </span>
              <span
                className={
                  clause.included ? '' : 'text-text-muted line-through'
                }
              >
                {clause.title}
              </span>
            </span>
            {locked && (
              <Lock
                className="h-3.5 w-3.5 text-text-muted"
                aria-label="Required"
                role="img"
              />
            )}
            {clause.pinned && (
              <Pin
                className="h-3.5 w-3.5 text-text-muted"
                aria-label="Pinned: fixed position"
                role="img"
              />
            )}
            <span className="flex flex-col">
              <button
                type="button"
                aria-label={`Move ${clause.title} up`}
                disabled={
                  disabled || clause.pinned || !previous || previous.pinned
                }
                data-move={`${clause.clauseKey}:up`}
                onClick={() => onMove(clause.clauseKey, -1)}
                className={MOVE_BUTTON}
              >
                <ChevronUp className="h-4 w-4" aria-hidden="true" />
              </button>
              <button
                type="button"
                aria-label={`Move ${clause.title} down`}
                disabled={disabled || clause.pinned || !next}
                data-move={`${clause.clauseKey}:down`}
                onClick={() => onMove(clause.clauseKey, 1)}
                className={MOVE_BUTTON}
              >
                <ChevronDown className="h-4 w-4" aria-hidden="true" />
              </button>
            </span>
          </li>
        );
      })}
    </ul>
  );
};
