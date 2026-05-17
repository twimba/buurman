import type { ReactNode } from 'react';
import { cn } from '../utils/cn';

export interface ResponsiveTableColumn<T> {
  /** Cell key (stable id, used for React keys). */
  key: string;
  /** Header label or JSX. */
  header: ReactNode;
  /** Cell renderer for a row. */
  cell: (row: T) => ReactNode;
  /** Optional cell alignment. */
  align?: 'left' | 'right' | 'center';
  /** Hide this column at the given breakpoint and below. */
  hideBelow?: 'sm' | 'md' | 'lg' | 'xl';
  /** Make this the sticky first column on horizontally-scrolling tables. */
  sticky?: boolean;
  /** Per-column className override (applied to td + th). */
  className?: string;
}

export interface ResponsiveTableProps<T> {
  rows: T[];
  columns: ResponsiveTableColumn<T>[];
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
  /**
   * Mobile (`<md`) card body. Receives a row and must return JSX. The whole
   * card is the click target if `onRowClick` is provided.
   */
  mobileRow: (row: T) => ReactNode;
  emptyState?: ReactNode;
  /** Sticky table header on the desktop side. Default true. */
  stickyHeader?: boolean;
  className?: string;
}

const ALIGN: Record<'left' | 'right' | 'center', string> = {
  left: 'text-left',
  right: 'text-right',
  center: 'text-center',
};

const HIDE_BELOW: Record<NonNullable<ResponsiveTableColumn<unknown>['hideBelow']>, string> = {
  sm: 'hidden sm:table-cell',
  md: 'hidden md:table-cell',
  lg: 'hidden lg:table-cell',
  xl: 'hidden xl:table-cell',
};

/**
 * Adaptive table:
 * - Phone (`<md`): renders rows as cards via `mobileRow`. Click target is the whole card.
 * - Tablet/Desktop (`md+`): renders a real `<table>` wrapped in `overflow-x-auto`,
 *   with optional sticky first column and sticky header.
 *
 * Caller owns sort / filter / pagination. This primitive is presentational only.
 */
export function ResponsiveTable<T>({
  rows,
  columns,
  rowKey,
  onRowClick,
  mobileRow,
  emptyState,
  stickyHeader = true,
  className,
}: ResponsiveTableProps<T>) {
  if (rows.length === 0 && emptyState !== undefined) {
    return <>{emptyState}</>;
  }

  return (
    <>
      {/* Phone: cards */}
      <ul className={cn('md:hidden space-y-3', className)}>
        {rows.map((row) => {
          const card = (
            <div className="bg-surface-card rounded-lg border border-border-default p-4">
              {mobileRow(row)}
            </div>
          );
          return (
            <li key={rowKey(row)}>
              {onRowClick ? (
                <button
                  type="button"
                  onClick={() => onRowClick(row)}
                  className="block w-full text-left min-h-touch focus-ring rounded-lg"
                >
                  {card}
                </button>
              ) : (
                card
              )}
            </li>
          );
        })}
      </ul>

      {/* Tablet/Desktop: scrollable table */}
      <div
        className={cn(
          'hidden md:block overflow-x-auto rounded-lg border border-border-default',
          className
        )}
      >
        <table className="min-w-full divide-y divide-border-default">
          <thead
            className={cn(
              'bg-surface-page',
              stickyHeader && 'sticky top-0 z-10'
            )}
          >
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={cn(
                    'px-4 py-3 text-xs font-medium text-text-secondary uppercase tracking-wider',
                    ALIGN[col.align ?? 'left'],
                    col.hideBelow && HIDE_BELOW[col.hideBelow],
                    col.sticky && 'sticky left-0 bg-surface-page z-10',
                    col.className
                  )}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="bg-surface-card divide-y divide-border-default">
            {rows.map((row) => (
              <tr
                key={rowKey(row)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={cn(
                  onRowClick && 'cursor-pointer hover:bg-surface-inset'
                )}
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={cn(
                      'px-4 py-3 text-sm text-text-primary',
                      ALIGN[col.align ?? 'left'],
                      col.hideBelow && HIDE_BELOW[col.hideBelow],
                      col.sticky && 'sticky left-0 bg-surface-card z-10',
                      col.className
                    )}
                  >
                    {col.cell(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

ResponsiveTable.displayName = 'ResponsiveTable';
