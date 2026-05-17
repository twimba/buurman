import type { ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '../utils/cn';

export interface SelectionBarAction {
  label: string;
  icon?: React.ComponentType<{ className?: string }>;
  onClick: () => void;
  tone?: 'default' | 'danger';
  disabled?: boolean;
}

export interface SelectionBarProps {
  /** Whether the bar is visible (true when selection mode is active). */
  open: boolean;
  /** Selected count label, e.g. "3 selected". */
  count: number;
  /** Localized template for the count label, e.g. "{{count}} selected". */
  label?: string;
  /** Called when the user taps Cancel / × — exits selection mode. */
  onCancel: () => void;
  /** Bulk actions. Shown right-aligned. */
  actions: SelectionBarAction[];
  /** Optional extra trailing content. */
  trailing?: ReactNode;
  className?: string;
}

/**
 * Sticky bottom action bar shown while a list page is in "selection mode".
 *
 * Designed to coexist with the iPhone bottom tab bar — anchored above it via
 * `bottom: calc(--bottomnav-h + --safe-bottom)`. The tab bar can still be
 * tapped, but typical usage hides it for the duration of selection.
 */
export function SelectionBar({
  open,
  count,
  label,
  onCancel,
  actions,
  trailing,
  className,
}: SelectionBarProps) {
  if (!open) {
    return null;
  }
  return (
    <div
      role="toolbar"
      aria-label="Selection actions"
      className={cn(
        'md:hidden fixed inset-x-0 z-40',
        'bg-surface-card/95 backdrop-blur-xl border-t border-border-default',
        'px-3 py-2 flex items-center gap-2',
        className
      )}
      style={{
        bottom:
          'calc(var(--bottomnav-h, 0px) + var(--safe-bottom, 0px))',
        paddingLeft: 'calc(0.75rem + var(--safe-left, 0px))',
        paddingRight: 'calc(0.75rem + var(--safe-right, 0px))',
      }}
    >
      <button
        type="button"
        onClick={onCancel}
        aria-label="Cancel selection"
        className="inline-flex items-center justify-center min-h-touch min-w-touch rounded-md hover:bg-surface-inset focus-ring"
      >
        <X className="h-5 w-5 text-text-secondary" />
      </button>
      <span className="font-semibold text-text-primary text-sm">
        {label ? label.replace('{{count}}', String(count)) : `${count} selected`}
      </span>
      <div className="ml-auto flex items-center gap-1">
        {actions.map((a, i) => {
          const Icon = a.icon;
          return (
            <button
              key={i}
              type="button"
              onClick={a.onClick}
              disabled={a.disabled}
              className={cn(
                'inline-flex items-center gap-1.5 min-h-touch px-3 rounded-md text-sm font-medium focus-ring disabled:opacity-50',
                a.tone === 'danger'
                  ? 'text-error-text hover:bg-error-bg'
                  : 'text-text-primary hover:bg-surface-inset'
              )}
            >
              {Icon && <Icon className="h-4 w-4" />}
              {a.label}
            </button>
          );
        })}
        {trailing}
      </div>
    </div>
  );
}

SelectionBar.displayName = 'SelectionBar';
