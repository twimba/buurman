import type { ReactNode } from 'react';
import { cn } from '../utils/cn';

export interface DataListItem {
  label: ReactNode;
  value: ReactNode;
  /** Hide the label and only render the value (useful for primary cell). */
  hideLabel?: boolean;
  /** Right-align the value (useful for currency, status badges). */
  align?: 'left' | 'right';
}

export interface DataListProps {
  items: DataListItem[];
  /** Optional leading element (icon, avatar). */
  leading?: ReactNode;
  /** Optional title above the list. */
  title?: ReactNode;
  /** Optional badge/status displayed at the top-right. */
  trailing?: ReactNode;
  className?: string;
}

/**
 * Compact label-value pair list — the primary `mobileRow` for ResponsiveTable cards
 * across data-heavy pages (Payments, Expenses, Documents, ...). Renders as a
 * vertical stack with the first row visually emphasized.
 */
export function DataList({
  items,
  leading,
  title,
  trailing,
  className,
}: DataListProps) {
  return (
    <div className={cn('flex items-start gap-3', className)}>
      {leading && <div className="flex-shrink-0">{leading}</div>}
      <div className="flex-1 min-w-0 space-y-1">
        {(title || trailing) && (
          <div className="flex items-start justify-between gap-2">
            {title && (
              <div className="text-base font-semibold text-text-primary truncate">
                {title}
              </div>
            )}
            {trailing && <div className="flex-shrink-0">{trailing}</div>}
          </div>
        )}
        {items.map((item, i) => (
          <div
            key={i}
            className={cn(
              'flex items-baseline gap-2 text-sm',
              item.align === 'right' && 'justify-between'
            )}
          >
            {!item.hideLabel && (
              <span className="text-text-secondary">{item.label}</span>
            )}
            <span
              className={cn(
                'text-text-primary',
                item.align === 'right' && 'ml-auto'
              )}
            >
              {item.value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

DataList.displayName = 'DataList';
