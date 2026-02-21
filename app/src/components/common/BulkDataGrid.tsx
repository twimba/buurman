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

export interface ColumnDef {
  key: string;
  label: string;
  type: 'date' | 'number' | 'text';
  required?: boolean;
  placeholder?: string;
}

export type RowData = Record<string, string>;

export type RowStatus = 'pending' | 'submitting' | 'success' | 'error';

interface RowState {
  data: RowData;
  status: RowStatus;
  error?: string;
}

interface BulkDataGridProps {
  columns: ColumnDef[];
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
}

// --- Helpers ---

function parseClipboard(text: string, columns: ColumnDef[]): RowData[] {
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
      const val = (parts[i] || '').trim();
      if (col.type === 'date' && val) {
        // Normalize common date formats to YYYY-MM-DD
        row[col.key] = normalizeDate(val);
      } else {
        row[col.key] = val;
      }
    });
    return row;
  });
}

function normalizeDate(raw: string): string {
  // Already ISO
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw;
  // DD/MM/YYYY or DD-MM-YYYY
  const dmy = raw.match(/^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})$/);
  if (dmy) return `${dmy[3]}-${dmy[2].padStart(2, '0')}-${dmy[1].padStart(2, '0')}`;
  // MM/DD/YYYY — ambiguous, but try parsing
  const parsed = new Date(raw);
  if (!isNaN(parsed.getTime())) return parsed.toISOString().split('T')[0];
  return raw;
}

function isRowEmpty(row: RowData): boolean {
  return Object.values(row).every((v) => !v);
}

function isRowValid(row: RowData, columns: ColumnDef[]): boolean {
  return columns.every((col) => {
    if (!col.required) return true;
    const val = row[col.key];
    if (!val) return false;
    if (col.type === 'number' && (isNaN(Number(val)) || Number(val) <= 0))
      return false;
    if (col.type === 'date' && !/^\d{4}-\d{2}-\d{2}$/.test(val)) return false;
    return true;
  });
}

function isCellInvalid(
  value: string,
  col: ColumnDef,
  rowEmpty: boolean
): boolean {
  if (rowEmpty) return false; // don't flag completely empty rows
  if (!col.required) return false;
  if (!value) return true;
  if (col.type === 'number' && (isNaN(Number(value)) || Number(value) <= 0))
    return true;
  if (col.type === 'date' && !/^\d{4}-\d{2}-\d{2}$/.test(value)) return true;
  return false;
}

function createEmptyRow(columns: ColumnDef[]): RowData {
  const row: RowData = {};
  columns.forEach((col) => (row[col.key] = ''));
  return row;
}

// --- Component ---

export const BulkDataGrid = ({
  columns,
  onSubmit,
  isSubmitting,
  disabled,
}: BulkDataGridProps) => {
  const [rows, setRows] = useState<RowState[]>([
    { data: createEmptyRow(columns), status: 'pending' },
  ]);
  const containerRef = useRef<HTMLDivElement>(null);

  const nonEmptyRows = rows.filter((r) => !isRowEmpty(r.data));
  const validRows = nonEmptyRows.filter((r) => isRowValid(r.data, columns));
  const successCount = rows.filter((r) => r.status === 'success').length;
  const errorCount = rows.filter((r) => r.status === 'error').length;

  const handlePaste = useCallback(
    (e: React.ClipboardEvent) => {
      const text = e.clipboardData.getData('text/plain');
      if (!text.trim()) return;

      // Only intercept if not pasting into a focused input
      const target = e.target as HTMLElement;
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
        // If the input is empty and text has newlines, treat as bulk paste
        const input = target as HTMLInputElement;
        if (input.value || !text.includes('\n')) return;
      }

      e.preventDefault();
      const parsed = parseClipboard(text, columns);
      if (parsed.length === 0) return;

      setRows((prev) => {
        // Replace empty pending rows, keep submitted ones
        const submitted = prev.filter(
          (r) => r.status === 'success' || r.status === 'error'
        );
        const newRows = parsed.map((data) => ({
          data,
          status: 'pending' as RowStatus,
        }));
        return [...submitted, ...newRows];
      });
    },
    [columns]
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
        ? [{ data: createEmptyRow(columns), status: 'pending' }]
        : updated;
    });
  };

  const addRow = () => {
    setRows((prev) => [
      ...prev,
      { data: createEmptyRow(columns), status: 'pending' },
    ]);
  };

  const clearAll = () => {
    setRows([{ data: createEmptyRow(columns), status: 'pending' }]);
  };

  const handleSubmit = () => {
    // Collect valid, non-submitted rows with their original indices
    const toSubmit: { index: number; data: RowData }[] = [];
    rows.forEach((r, i) => {
      if (
        r.status !== 'success' &&
        !isRowEmpty(r.data) &&
        isRowValid(r.data, columns)
      ) {
        toSubmit.push({ index: i, data: r.data });
      }
    });

    if (toSubmit.length === 0) return;

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
      !isRowEmpty(r.data) &&
      isRowValid(r.data, columns)
  ).length;

  return (
    <div
      ref={containerRef}
      onPaste={handlePaste}
      className="space-y-4"
    >
      {/* Hint */}
      {nonEmptyRows.length === 0 && (
        <div className="flex items-center gap-2 p-3 bg-[#f1f3f9] dark:bg-[#1e2130] border border-dashed border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg text-sm text-[#6b7194] dark:text-[#8b90a8]">
          <ClipboardPaste className="h-4 w-4 flex-shrink-0" />
          <span>
            Paste CSV data anywhere on this area, or add rows manually below.
            Supports comma, tab, and semicolon delimiters.
          </span>
        </div>
      )}

      {/* Grid */}
      <div className="overflow-x-auto rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
        <table className="w-full">
          <thead>
            <tr className="bg-[#f8f9fc] dark:bg-[#1a1d28]">
              <th className="w-10 px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] text-center">
                #
              </th>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className="px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]"
                >
                  {col.label}
                  {col.required && (
                    <span className="text-red-500 ml-0.5">*</span>
                  )}
                </th>
              ))}
              <th className="w-10 px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] text-center">
                Status
              </th>
              <th className="w-10 px-2 py-2.5" />
            </tr>
          </thead>
          <tbody>
            {rows.map((row, rowIndex) => {
              const empty = isRowEmpty(row.data);
              return (
                <tr
                  key={rowIndex}
                  className={`border-t border-[#e2e6f0] dark:border-[#2a2e3f] transition-colors ${
                    row.status === 'success'
                      ? 'bg-emerald-50/50 dark:bg-emerald-900/10'
                      : row.status === 'error'
                        ? 'bg-red-50/50 dark:bg-red-900/10'
                        : 'hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28]'
                  }`}
                >
                  <td className="px-3 py-1.5 text-center text-xs font-mono text-[#9ca0b8] dark:text-[#5c6180]">
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
                        <input
                          type={col.type === 'number' ? 'text' : col.type}
                          inputMode={
                            col.type === 'number' ? 'decimal' : undefined
                          }
                          value={row.data[col.key]}
                          onChange={(e) =>
                            updateCell(rowIndex, col.key, e.target.value)
                          }
                          disabled={isDisabled}
                          placeholder={col.placeholder}
                          className={`w-full px-2 py-1.5 text-sm rounded border transition-colors
                            ${isDisabled ? 'opacity-60 cursor-not-allowed' : ''}
                            ${
                              invalid
                                ? 'border-red-300 dark:border-red-700 bg-red-50/50 dark:bg-red-900/10'
                                : 'border-transparent hover:border-[#c9cfd9] dark:hover:border-[#3a3f54] focus:border-[#5c7cfa] dark:focus:border-[#5c7cfa]'
                            }
                            bg-transparent text-[#1a1d2e] dark:text-[#eef0f6]
                            focus:outline-none focus:ring-1 focus:ring-[#5c7cfa]/30`}
                        />
                      </td>
                    );
                  })}
                  <td className="px-3 py-1.5 text-center">
                    {row.status === 'submitting' && (
                      <Loader2 className="h-4 w-4 animate-spin text-[#5c7cfa] mx-auto" />
                    )}
                    {row.status === 'success' && (
                      <CheckCircle className="h-4 w-4 text-emerald-500 mx-auto" />
                    )}
                    {row.status === 'error' && (
                      <span title={row.error}>
                        <XCircle className="h-4 w-4 text-red-500 mx-auto cursor-help" />
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
                      className="p-1 text-[#9ca0b8] dark:text-[#5c6180] hover:text-red-500 dark:hover:text-red-400 transition-colors disabled:opacity-30"
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
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-[#5c7cfa] hover:bg-[#5c7cfa]/10 rounded transition-colors disabled:opacity-50"
          >
            <Plus className="h-3.5 w-3.5" />
            Add row
          </button>
          {nonEmptyRows.length > 0 && (
            <button
              type="button"
              onClick={clearAll}
              disabled={disabled || isSubmitting}
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors disabled:opacity-50"
            >
              Clear all
            </button>
          )}
        </div>

        <div className="flex items-center gap-4">
          {/* Stats */}
          <div className="flex items-center gap-3 text-xs text-[#6b7194] dark:text-[#8b90a8]">
            <span>{nonEmptyRows.length} rows</span>
            <span className="text-[#c9cfd9] dark:text-[#3a3f54]">|</span>
            <span>
              {validRows.length} valid
            </span>
            {successCount > 0 && (
              <>
                <span className="text-[#c9cfd9] dark:text-[#3a3f54]">|</span>
                <span className="text-emerald-600 dark:text-emerald-400">
                  {successCount} saved
                </span>
              </>
            )}
            {errorCount > 0 && (
              <>
                <span className="text-[#c9cfd9] dark:text-[#3a3f54]">|</span>
                <span className="text-red-600 dark:text-red-400">
                  {errorCount} failed
                </span>
              </>
            )}
          </div>

          {/* Submit */}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={disabled || isSubmitting || submittableCount === 0}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] rounded-md hover:bg-[#4c6ef5] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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
