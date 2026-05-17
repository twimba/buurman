import { type ReactNode, useEffect, useState } from 'react';
import { Filter, ChevronDown } from 'lucide-react';
import { cn } from '../utils/cn';
import { Sheet } from './Sheet';

export interface FilterSheetProps {
  /** Number of active filters. Shown as a badge on the trigger and as a label on the Apply button. */
  activeCount: number;
  /** Called when the user taps "Clear all" / "Reset". */
  onClear: () => void;
  /** Filter form content. Same JSX in both inline and sheet modes. */
  children: ReactNode;
  /** Trigger label (phone only). Default: "Filters". */
  triggerLabel?: string;
  /**
   * Where to switch from inline-always-visible to phone-style trigger+sheet.
   * Default `md`: inline at md+ (desktop pixel-equivalent to today's filter card),
   * sheet below md.
   */
  collapseBelow?: 'sm' | 'md' | 'lg';
  /**
   * Optional inline-mode title (rendered above the children at md+). When omitted,
   * inline mode renders just the children — useful when the caller is wrapping
   * the FilterSheet in its own card chrome.
   */
  inlineTitle?: string;
}

const INLINE_VISIBILITY: Record<'sm' | 'md' | 'lg', string> = {
  sm: 'hidden sm:block',
  md: 'hidden md:block',
  lg: 'hidden lg:block',
};
const TRIGGER_VISIBILITY: Record<'sm' | 'md' | 'lg', string> = {
  sm: 'sm:hidden',
  md: 'md:hidden',
  lg: 'lg:hidden',
};

/**
 * Filter primitive with two rendering modes:
 * - md+ (desktop / tablet): inline always-visible filter card — preserves today's UX
 * - phone (`<md` by default): a `[⛕ Filters (n)]` trigger that opens a bottom sheet
 *
 * The children JSX is the SAME in both modes — caller doesn't have to duplicate.
 * Desktop pixel-equivalence is the contract.
 */
export function FilterSheet({
  activeCount,
  onClear,
  children,
  triggerLabel = 'Filters',
  collapseBelow = 'md',
  inlineTitle,
}: FilterSheetProps) {
  const [open, setOpen] = useState(false);

  // Close the sheet automatically if the viewport grows past the collapse breakpoint
  // (e.g. iPad rotates from portrait to landscape).
  useEffect(() => {
    const mq = window.matchMedia(
      collapseBelow === 'sm'
        ? '(min-width: 640px)'
        : collapseBelow === 'lg'
          ? '(min-width: 1024px)'
          : '(min-width: 768px)'
    );
    const onChange = (e: MediaQueryListEvent) => {
      if (e.matches) {
        setOpen(false);
      }
    };
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [collapseBelow]);

  return (
    <>
      {/* Inline mode — md+. Caller chrome may wrap this. */}
      <div className={INLINE_VISIBILITY[collapseBelow]}>
        {inlineTitle && (
          <div className="flex items-center gap-2 mb-3">
            <Filter className="h-5 w-5 text-text-secondary" />
            <h2 className="font-semibold text-text-primary">{inlineTitle}</h2>
          </div>
        )}
        {children}
      </div>

      {/* Trigger — phone only. */}
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-haspopup="dialog"
        aria-expanded={open}
        className={cn(
          TRIGGER_VISIBILITY[collapseBelow],
          'inline-flex items-center gap-2 min-h-touch px-3 py-2 rounded-lg border border-border-strong bg-surface-card text-text-secondary hover:border-primary-500 focus-ring'
        )}
      >
        <Filter className="h-5 w-5" />
        <span>{triggerLabel}</span>
        {activeCount > 0 && (
          <span className="inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1.5 rounded-full text-xs font-semibold bg-primary-500 text-white">
            {activeCount}
          </span>
        )}
        <ChevronDown className="h-4 w-4 -mr-1" />
      </button>

      <Sheet
        open={open}
        onClose={() => setOpen(false)}
        title={triggerLabel}
        footer={
          <div className="flex items-center justify-between gap-2 w-full">
            <button
              type="button"
              onClick={() => {
                onClear();
              }}
              className="min-h-touch px-3 py-2 text-text-secondary hover:text-text-primary"
            >
              Clear all
            </button>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="min-h-touch px-4 py-2 rounded-lg bg-primary-500 text-white hover:bg-primary-600"
            >
              {activeCount > 0 ? `Apply (${activeCount})` : 'Apply'}
            </button>
          </div>
        }
      >
        {children}
      </Sheet>
    </>
  );
}

FilterSheet.displayName = 'FilterSheet';
