import {
  type ComponentType,
  type ReactNode,
  useEffect,
  useId,
  useRef,
  useState,
} from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '../utils/cn';

export interface FilterPopoverProps {
  /** Trigger label. Show the active selection here for single-select filters. */
  label: string;
  /** Active selection count. >0 renders a badge and active styling on the trigger. */
  activeCount?: number;
  /** Optional leading icon for the trigger. */
  icon?: ComponentType<{ className?: string }>;
  /**
   * Panel content. Receives a `close` callback so option handlers can dismiss
   * the popover (e.g. after a single-select pick).
   */
  children: ReactNode | ((close: () => void) => ReactNode);
  /** Horizontal anchor of the panel relative to the trigger. Default `start`. */
  align?: 'start' | 'end';
  className?: string;
  panelClassName?: string;
}

/**
 * Compact filter dropdown: a trigger button that opens an anchored popover
 * panel. Closes on outside-click or Escape. Works at every breakpoint, so a
 * row of these keeps a filter toolbar to a single tidy line instead of
 * spilling every option inline.
 */
export function FilterPopover({
  label,
  activeCount = 0,
  icon: Icon,
  children,
  align = 'start',
  className,
  panelClassName,
}: FilterPopoverProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const panelId = useId();

  useEffect(() => {
    if (!open) {
      return;
    }
    const onPointerDown = (e: PointerEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        triggerRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const active = activeCount > 0;

  return (
    <div ref={rootRef} className={cn('relative', className)}>
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="true"
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        className={cn(
          'inline-flex items-center gap-1.5 h-10 px-3 rounded-lg border text-sm font-medium transition-colors focus-ring',
          active
            ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300'
            : 'border-border-strong bg-surface-card text-text-secondary hover:border-primary-400'
        )}
      >
        {Icon && <Icon className="h-4 w-4 shrink-0" />}
        <span className="max-w-[12rem] truncate">{label}</span>
        {active && (
          <span className="grid place-items-center min-w-5 h-5 px-1 rounded-full text-xs font-semibold tabular-nums bg-primary-500 text-white dark:bg-primary-400 dark:text-primary-950">
            {activeCount}
          </span>
        )}
        <ChevronDown
          className={cn(
            'h-4 w-4 shrink-0 text-text-muted transition-transform duration-200',
            open && 'rotate-180'
          )}
        />
      </button>

      {open && (
        <div
          id={panelId}
          role="group"
          aria-label={label}
          className={cn(
            'absolute z-30 mt-2 min-w-[14rem] rounded-xl border border-border-default bg-surface-card p-2 shadow-lg',
            'origin-top animate-in fade-in-0 zoom-in-95 duration-100',
            align === 'end' ? 'right-0' : 'left-0',
            panelClassName
          )}
        >
          {typeof children === 'function'
            ? children(() => setOpen(false))
            : children}
        </div>
      )}
    </div>
  );
}

FilterPopover.displayName = 'FilterPopover';
