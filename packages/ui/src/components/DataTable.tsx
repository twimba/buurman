import { useCallback, useMemo } from "react";
import { ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { cn } from "../utils/cn";
import { Skeleton } from "./Skeleton";

export interface ColumnDef<TRow> {
  id: string;
  header: string | (() => React.ReactNode);
  cell?: (row: TRow, rowIndex: number) => React.ReactNode;
  accessor?: (row: TRow) => string | number | Date | null;
  sortable?: boolean;
  align?: "left" | "center" | "right";
  width?: string;
  className?: string;
  /** Hide this column in the mobile card view */
  hideOnMobile?: boolean;
}

export interface SortState {
  columnId: string;
  direction: "asc" | "desc";
}

interface DataTableProps<TRow> {
  columns: ColumnDef<TRow>[];
  data: TRow[];
  rowKey: (row: TRow) => string;
  sort?: SortState;
  onSortChange?: (sort: SortState) => void;
  sortMode?: "client" | "server";
  onRowClick?: (row: TRow) => void;
  loading?: boolean;
  emptyState?: React.ReactNode;
  stickyHeader?: boolean;
  /** Custom card renderer for mobile view. If provided, cards are shown on small screens. */
  cardRenderer?: (row: TRow, rowIndex: number) => React.ReactNode;
  "aria-label"?: string;
  className?: string;
}

function defaultCellRenderer<TRow>(
  col: ColumnDef<TRow>,
  row: TRow,
  index: number,
): React.ReactNode {
  if (col.cell) {
    return col.cell(row, index);
  }
  if (col.accessor) {
    const val = col.accessor(row);
    return val != null ? String(val) : "";
  }
  return "";
}

export function DataTable<TRow>({
  columns,
  data,
  rowKey,
  sort,
  onSortChange,
  sortMode = "server",
  onRowClick,
  loading,
  emptyState,
  stickyHeader,
  cardRenderer,
  "aria-label": ariaLabel,
  className,
}: DataTableProps<TRow>) {
  const handleSort = useCallback(
    (columnId: string) => {
      if (!onSortChange) {
        return;
      }
      if (sort?.columnId === columnId) {
        onSortChange({
          columnId,
          direction: sort.direction === "asc" ? "desc" : "asc",
        });
      } else {
        onSortChange({ columnId, direction: "asc" });
      }
    },
    [sort, onSortChange],
  );

  const sortedData = useMemo(() => {
    if (sortMode !== "client" || !sort) {
      return data;
    }
    const col = columns.find((c) => c.id === sort.columnId);
    if (!col?.accessor) {
      return data;
    }
    return [...data].sort((a, b) => {
      const aVal = col.accessor!(a);
      const bVal = col.accessor!(b);
      if (aVal == null && bVal == null) {
        return 0;
      }
      if (aVal == null) {
        return 1;
      }
      if (bVal == null) {
        return -1;
      }
      const cmp = aVal < bVal ? -1 : aVal > bVal ? 1 : 0;
      return sort.direction === "asc" ? cmp : -cmp;
    });
  }, [data, sort, sortMode, columns]);

  const alignClass = (align?: string) => {
    if (align === "center") {
      return "text-center";
    }
    if (align === "right") {
      return "text-right";
    }
    return "text-left";
  };

  const hasCardView = !!cardRenderer;

  // Card view for mobile (only when cardRenderer is provided)
  const cardView = hasCardView && (
    <div className={cn("md:hidden space-y-3", className)}>
      {loading ? (
        Array.from({ length: 3 }).map((_, i) => (
          <div
            key={`card-skeleton-${i}`}
            className="rounded-lg border border-border-default bg-surface-card p-4 space-y-2"
          >
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-3 w-1/2" />
            <Skeleton className="h-3 w-1/3" />
          </div>
        ))
      ) : sortedData.length === 0 ? (
        <div className="rounded-lg border border-border-default bg-surface-card px-4 py-8">
          {emptyState || (
            <p className="text-center text-sm text-text-muted">No data</p>
          )}
        </div>
      ) : (
        sortedData.map((row, rowIndex) => (
          <div
            key={rowKey(row)}
            className={cn(
              "rounded-lg border border-border-default bg-surface-card p-4 transition-colors",
              onRowClick && "cursor-pointer hover:bg-primary-50",
            )}
            onClick={onRowClick ? () => onRowClick(row) : undefined}
          >
            {cardRenderer(row, rowIndex)}
          </div>
        ))
      )}
    </div>
  );

  // Table view (always rendered; hidden on mobile when card view exists)
  const tableView = (
    <div
      className={cn(
        "overflow-x-auto rounded-lg border border-border-default",
        hasCardView && "hidden md:block",
        className,
      )}
    >
      <table className="w-full text-sm" aria-label={ariaLabel}>
        <thead>
          <tr
            className={cn(
              "border-b border-border-default bg-surface-inset",
              stickyHeader && "sticky top-0 z-10",
            )}
          >
            {columns.map((col) => (
              <th
                key={col.id}
                className={cn(
                  "px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary",
                  alignClass(col.align),
                  col.className,
                )}
                style={col.width ? { width: col.width } : undefined}
              >
                {col.sortable && onSortChange ? (
                  <button
                    className="inline-flex items-center gap-1 hover:text-text-primary"
                    onClick={() => handleSort(col.id)}
                  >
                    {typeof col.header === "function"
                      ? col.header()
                      : col.header}
                    {sort?.columnId === col.id ? (
                      sort.direction === "asc" ? (
                        <ArrowUp className="h-3.5 w-3.5" />
                      ) : (
                        <ArrowDown className="h-3.5 w-3.5" />
                      )
                    ) : (
                      <ArrowUpDown className="h-3.5 w-3.5 opacity-40" />
                    )}
                  </button>
                ) : typeof col.header === "function" ? (
                  col.header()
                ) : (
                  col.header
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border-default bg-surface-card">
          {loading ? (
            Array.from({ length: 5 }).map((_, i) => (
              <tr key={`skeleton-${i}`}>
                {columns.map((col) => (
                  <td key={col.id} className="px-4 py-3">
                    <Skeleton className="h-4 w-3/4" />
                  </td>
                ))}
              </tr>
            ))
          ) : sortedData.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8">
                {emptyState || (
                  <p className="text-center text-sm text-text-muted">No data</p>
                )}
              </td>
            </tr>
          ) : (
            sortedData.map((row, rowIndex) => (
              <tr
                key={rowKey(row)}
                className={cn(
                  "transition-colors",
                  onRowClick && "cursor-pointer hover:bg-primary-50",
                )}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
              >
                {columns.map((col) => (
                  <td
                    key={col.id}
                    className={cn(
                      "px-4 py-3 text-text-primary",
                      alignClass(col.align),
                      col.className,
                    )}
                  >
                    {defaultCellRenderer(col, row, rowIndex)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );

  if (hasCardView) {
    return (
      <>
        {cardView}
        {tableView}
      </>
    );
  }

  return tableView;
}

DataTable.displayName = "DataTable";
