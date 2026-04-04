import { useState, useCallback, useRef } from 'react';
import {
  Plus,
  Trash2,
  CheckCircle,
  XCircle,
  Loader2,
  ClipboardPaste,
  Send,
} from 'lucide-react';

// --- Types ---

export interface BulkColumnDef {
  key: string;
  label: string;
  type: 'date' | 'number' | 'text' | 'select';
  required?: boolean;
  placeholder?: string;
  options?: { value: string; label: string }[];
  defaultValue?: string;
}

export type RowData = Record<string, string>;

export type RowStatus = 'pending' | 'submitting' | 'success' | 'error';


interface RowState {
  id: number;
  data: RowData;
  status: RowStatus;
  error?: string;
}

type DateFormat = 'DD/MM/YYYY' | 'MM/DD/YYYY' | 'YYYY-MM-DD';

interface BulkDataGridProps {
  columns: BulkColumnDef[];
  onSubmit: (
    rows: RowData[],
    callbacks: {
      onRowStart: (index: number) => void;
      onRowSuccess: (index: number) => void;
      onRowError: (index: number, error: string) => void;
      onComplete: () => void;
    }
  ) => void;
  isSubmitting: boolean;
  disabled?: boolean;
  dateFormat?: DateFormat;
}

// --- Helpers ---

function parseClipboard(
  text: string,
  columns: BulkColumnDef[],
  dateFormat?: DateFormat
): RowData[] {
  const lines = text
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  return lines.map((line) => {
    // Support tab, comma, and semicolon delimiters
    const parts = line.includes('\t')
      ? line.split('\t')
      : line.includes(';')
        ? line.split(';')
        : line.split(',');

    const row: RowData = {};
    columns.forEach((col, i) => {
      const val = (parts[i] ?? '').trim();
      if (col.type === 'date' && val) {
        row[col.key] = normalizeDate(val, dateFormat);
      } else if (col.type === 'select' && col.options && val) {
        row[col.key] =
          matchSelectOption(val, col.options) || col.defaultValue || '';
      } else if (val) {
        row[col.key] = val;
      } else {
        row[col.key] = col.defaultValue ?? '';
      }
    });
    return row;
  });
}

function normalizeDate(raw: string, dateFormat?: DateFormat): string {
  // Already ISO
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return raw;
  }

  const match = raw.match(/^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})$/);
  if (match) {
    const [, a, b, year] = match;
    const n1 = parseInt(a, 10);
    const n2 = parseInt(b, 10);

    // If first number > 12, it can only be a day → DD/MM/YYYY
    if (n1 > 12 && n2 >= 1 && n2 <= 12) {
      return `${year}-${b.padStart(2, '0')}-${a.padStart(2, '0')}`;
    }
    // If second number > 12, it can only be a day → MM/DD/YYYY
    if (n2 > 12 && n1 >= 1 && n1 <= 12) {
      return `${year}-${a.padStart(2, '0')}-${b.padStart(2, '0')}`;
    }
    // Ambiguous (both ≤ 12) — use team's date format preference
    if (dateFormat === 'MM/DD/YYYY') {
      return `${year}-${a.padStart(2, '0')}-${b.padStart(2, '0')}`;
    }
    // Default to DD/MM/YYYY
    return `${year}-${b.padStart(2, '0')}-${a.padStart(2, '0')}`;
  }

  // Fallback: try native Date parsing
  const parsed = new Date(raw);
  if (!isNaN(parsed.getTime())) {
    return parsed.toISOString().split('T')[0];
  }
  return raw;
}

function matchSelectOption(
  val: string,
  options: { value: string; label: string }[]
): string | undefined {
  const lower = val.toLowerCase();
  // Exact value match
  const exact = options.find((o) => o.value.toLowerCase() === lower);
  if (exact) {
    return exact.value;
  }
  // Exact label match
  const label = options.find((o) => o.label.toLowerCase() === lower);
  if (label) {
    return label.value;
  }
  // Prefix match on label
  const prefix = options.find((o) => o.label.toLowerCase().startsWith(lower));
  if (prefix) {
    return prefix.value;
  }
  return undefined;
}

function isRowEmpty(row: RowData, columns: BulkColumnDef[]): boolean {
  return columns.every((col) => {
    const val = row[col.key];
    return !val || val === col.defaultValue;
  });
}

function isRowValid(row: RowData, columns: BulkColumnDef[]): boolean {
  return columns.every((col) => {
    if (!col.required) {
      return true;
    }
    const val = row[col.key];
    if (!val) {
      return false;
    }
    if (col.type === 'number' && (isNaN(Number(val)) || Number(val) <= 0)) {
      return false;
    }
    if (col.type === 'date' && !isValidDate(val)) {
      return false;
    }
    return true;
  });
}

function isValidDate(val: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(val)) {
    return false;
  }
  const d = new Date(val + 'T00:00:00');
  return !isNaN(d.getTime());
}

function isCellInvalid(
  value: string,
  col: BulkColumnDef,
  rowEmpty: boolean
): boolean {
  if (rowEmpty) {
    return false; // don't flag completely empty rows
  }
  if (!col.required) {
    return false;
  }
  if (!value) {
    return true;
  }
  if (col.type === 'number' && (isNaN(Number(value)) || Number(value) <= 0)) {
    return true;
  }
  if (col.type === 'date' && !isValidDate(value)) {
    return true;
  }
  return false;
}

function createEmptyRow(columns: BulkColumnDef[]): RowData {
  const row: RowData = {};
  columns.forEach((col) => (row[col.key] = col.defaultValue ?? ''));
  return row;
}

// --- Component ---

export const BulkDataGrid = ({
  columns,
  onSubmit,
  isSubmitting,
  disabled,
  dateFormat,
}: BulkDataGridProps) => {
  const nextRowId = useRef(1);
  const [rows, setRows] = useState<RowState[]>([
    { id: nextRowId.current++, data: createEmptyRow(columns), status: 'pending' },
  ]);
  const containerRef = useRef<HTMLDivElement>(null);

  const nonEmptyRows = rows.filter((r) => !isRowEmpty(r.data, columns));
  const validRows = nonEmptyRows.filter((r) => isRowValid(r.data, columns));
  const successCount = rows.filter((r) => r.status === 'success').length;
  const errorCount = rows.filter((r) => r.status === 'error').length;

  const handlePaste = useCallback(
    (e: React.ClipboardEvent) => {
      const text = e.clipboardData.getData('text/plain');
      if (!text.trim()) {
        return;
      }

      // Only intercept if not pasting into a focused input
      const target = e.target as HTMLElement;
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
        // If the input is empty and text has newlines, treat as bulk paste
        const input = target as HTMLInputElement;
        if (input.value || !text.includes('\n')) {
          return;
        }
      }

      e.preventDefault();
      // Blur the focused input to prevent native date/number input handling
      // from interfering with the React state update for that cell
      (document.activeElement as HTMLElement)?.blur();
      const parsed = parseClipboard(text, columns, dateFormat);
      if (parsed.length === 0) {
        return;
      }

      setRows((prev) => {
        // Replace empty pending rows, keep submitted ones
        const submitted = prev.filter(
          (r) => r.status === 'success' || r.status === 'error'
        );
        const newRows = parsed.map((data) => ({
          id: nextRowId.current++,
          data,
          status: 'pending' as RowStatus,
        }));
        return [...submitted, ...newRows];
      });
    },
    [columns, dateFormat]
  );

  const updateCell = (rowIndex: number, key: string, value: string) => {
    setRows((prev) =>
      prev.map((r, i) =>
        i === rowIndex
          ? { ...r, data: { ...r.data, [key]: value }, status: 'pending' }
          : r
      )
    );
  };

  const deleteRow = (rowIndex: number) => {
    setRows((prev) => {
      const updated = prev.filter((_, i) => i !== rowIndex);
      return updated.length === 0
        ? [
            {
              id: nextRowId.current++,
              data: createEmptyRow(columns),
              status: 'pending',
            },
          ]
        : updated;
    });
  };

  const addRow = () => {
    setRows((prev) => [
      ...prev,
      { id: nextRowId.current++, data: createEmptyRow(columns), status: 'pending' },
    ]);
  };

  const clearAll = () => {
    setRows([
      { id: nextRowId.current++, data: createEmptyRow(columns), status: 'pending' },
    ]);
  };

  const handleSubmit = () => {
    // Collect valid, non-submitted rows with their original indices
    const toSubmit: { index: number; data: RowData }[] = [];
    rows.forEach((r, i) => {
      if (
        r.status !== 'success' &&
        !isRowEmpty(r.data, columns) &&
        isRowValid(r.data, columns)
      ) {
        toSubmit.push({ index: i, data: r.data });
      }
    });

    if (toSubmit.length === 0) {
      return;
    }

    onSubmit(
      toSubmit.map((r) => r.data),
      {
        onRowStart: (seqIndex) => {
          const rowIndex = toSubmit[seqIndex].index;
          setRows((prev) =>
            prev.map((r, i) =>
              i === rowIndex ? { ...r, status: 'submitting' } : r
            )
          );
        },
        onRowSuccess: (seqIndex) => {
          const rowIndex = toSubmit[seqIndex].index;
          setRows((prev) =>
            prev.map((r, i) =>
              i === rowIndex ? { ...r, status: 'success', error: undefined } : r
            )
          );
        },
        onRowError: (seqIndex, error) => {
          const rowIndex = toSubmit[seqIndex].index;
          setRows((prev) =>
            prev.map((r, i) =>
              i === rowIndex ? { ...r, status: 'error', error } : r
            )
          );
        },
        onComplete: () => {},
      }
    );
  };

  const submittableCount = rows.filter(
    (r) =>
      r.status !== 'success' &&
      !isRowEmpty(r.data, columns) &&
      isRowValid(r.data, columns)
  ).length;

  return (
    <div ref={containerRef} onPaste={handlePaste} className="space-y-4">
      {/* Hint */}
      {nonEmptyRows.length === 0 && (
        <div className="flex items-center gap-2 p-3 bg-surface-inset border border-dashed border-border-strong rounded-lg text-sm text-text-secondary">
          <ClipboardPaste className="h-4 w-4 flex-shrink-0" />
          <span>
            Paste CSV data anywhere on this area, or add rows manually below.
            Supports comma, tab, and semicolon delimiters.
          </span>
        </div>
      )}

      {/* Grid */}
      <div className="overflow-x-auto rounded-lg border border-border-default">
        <table className="w-full">
          <thead>
            <tr className="bg-surface-page">
              <th className="w-10 px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-text-secondary text-center">
                #
              </th>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className="px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wider text-text-secondary"
                >
                  {col.label}
                  {col.required && (
                    <span className="text-error-text ml-0.5">*</span>
                  )}
                </th>
              ))}
              <th className="w-10 px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-text-secondary text-center">
                Status
              </th>
              <th className="w-10 px-2 py-2.5" />
            </tr>
          </thead>
          <tbody>
            {rows.map((row, rowIndex) => {
              const empty = isRowEmpty(row.data, columns);
              return (
                <tr
                  key={row.id}
                  className={`border-t border-border-default transition-colors ${
                    row.status === 'success'
                      ? 'bg-success-bg/50'
                      : row.status === 'error'
                        ? 'bg-error-bg/50'
                        : 'hover:bg-surface-page'
                  }`}
                >
                  <td className="px-3 py-1.5 text-center text-xs font-mono text-text-muted">
                    {rowIndex + 1}
                  </td>
                  {columns.map((col) => {
                    const invalid = isCellInvalid(
                      row.data[col.key],
                      col,
                      empty
                    );
                    const isDisabled =
                      disabled ||
                      isSubmitting ||
                      row.status === 'success' ||
                      row.status === 'submitting';
                    return (
                      <td key={col.key} className="px-1 py-1">
                        {col.type === 'select' && col.options ? (
                          <select
                            value={row.data[col.key]}
                            onChange={(e) =>
                              updateCell(rowIndex, col.key, e.target.value)
                            }
                            disabled={isDisabled}
                            className={`w-full px-2 py-1.5 text-sm rounded border transition-colors
                              ${isDisabled ? 'opacity-60 cursor-not-allowed' : ''}
                              ${
                                invalid
                                  ? 'border-error-border bg-error-bg/50'
                                  : 'border-transparent hover:border-border-strong focus:border-primary-500'
                              }
                              bg-transparent text-text-primary 
                              focus:outline-none focus:ring-1 focus:ring-primary-500/30`}
                          >
                            {col.options.map((opt) => (
                              <option key={opt.value} value={opt.value}>
                                {opt.label}
                              </option>
                            ))}
                          </select>
                        ) : (
                          <input
                            type={col.type === 'number' ? 'text' : col.type}
                            inputMode={
                              col.type === 'number' ? 'decimal' : undefined
                            }
                            value={row.data[col.key]}
                            onChange={(e) =>
                              updateCell(rowIndex, col.key, e.target.value)
                            }
                            onPaste={(e) => {
                              const text =
                                e.clipboardData.getData('text/plain');
                              if (text.includes('\n')) {
                                e.preventDefault();
                              }
                            }}
                            disabled={isDisabled}
                            placeholder={col.placeholder}
                            className={`w-full px-2 py-1.5 text-sm rounded border transition-colors
                              ${isDisabled ? 'opacity-60 cursor-not-allowed' : ''}
                              ${
                                invalid
                                  ? 'border-error-border bg-error-bg/50'
                                  : 'border-transparent hover:border-border-strong focus:border-primary-500'
                              }
                              bg-transparent text-text-primary 
                              focus:outline-none focus:ring-1 focus:ring-primary-500/30`}
                          />
                        )}
                      </td>
                    );
                  })}
                  <td className="px-3 py-1.5 text-center">
                    {row.status === 'submitting' && (
                      <Loader2 className="h-4 w-4 animate-spin text-primary-500 mx-auto" />
                    )}
                    {row.status === 'success' && (
                      <CheckCircle className="h-4 w-4 text-success-text mx-auto" />
                    )}
                    {row.status === 'error' && (
                      <span title={row.error}>
                        <XCircle className="h-4 w-4 text-error-text mx-auto cursor-help" />
                      </span>
                    )}
                  </td>
                  <td className="px-2 py-1.5 text-center">
                    <button
                      type="button"
                      onClick={() => deleteRow(rowIndex)}
                      disabled={
                        disabled || isSubmitting || row.status === 'submitting'
                      }
                      className="p-1 text-text-muted hover:text-error-text transition-colors disabled:opacity-30"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={addRow}
            disabled={disabled || isSubmitting}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-primary-500 hover:bg-primary-500/10 rounded transition-colors disabled:opacity-50"
          >
            <Plus className="h-3.5 w-3.5" />
            Add row
          </button>
          {nonEmptyRows.length > 0 && (
            <button
              type="button"
              onClick={clearAll}
              disabled={disabled || isSubmitting}
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-text-secondary hover:bg-surface-inset rounded transition-colors disabled:opacity-50"
            >
              Clear all
            </button>
          )}
        </div>

        <div className="flex items-center gap-4">
          {/* Stats */}
          <div className="flex items-center gap-3 text-xs text-text-secondary">
            <span>{nonEmptyRows.length} rows</span>
            <span className="text-text-disabled">|</span>
            <span>{validRows.length} valid</span>
            {successCount > 0 && (
              <>
                <span className="text-text-disabled">|</span>
                <span className="text-success-text">{successCount} saved</span>
              </>
            )}
            {errorCount > 0 && (
              <>
                <span className="text-text-disabled">|</span>
                <span className="text-error-text">{errorCount} failed</span>
              </>
            )}
          </div>

          {/* Submit */}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={disabled || isSubmitting || submittableCount === 0}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Submitting...
              </>
            ) : (
              <>
                <Send className="h-4 w-4" />
                Submit {submittableCount > 0 ? `(${submittableCount})` : ''}
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
