import { useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  Check,
  ChevronLeft,
  ChevronRight,
  History,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  X,
} from 'lucide-react';

import type { FxRate } from '../generated/models';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { AddPairModal } from '../components/cost/AddPairModal';
import { BackfillFxModal } from '../components/cost/BackfillFxModal';
import { FxRateChart, type FxPoint } from '../components/cost/FxRateChart';
import { RemovePairModal } from '../components/cost/RemovePairModal';
import { currencyName } from '../lib/currencies';
import {
  useAddFxPair,
  useBackfillFxRates,
  useDeleteFxRate,
  useFxPairs,
  useFxRateHistory,
  useRefreshFxRates,
  useRemoveFxPair,
  useSetFxRate,
} from '../hooks/cost';

type SortKey = keyof Pick<FxRate, 'rateDate' | 'rate' | 'source'>;
const PAGE_SIZE = 15;
const parseRate = (v: string) => parseFloat(v.replace(',', '.'));
const fmtRate = (v: number) =>
  v.toLocaleString(undefined, { maximumFractionDigits: 6 });

const SortHeader = ({
  label,
  k,
  sortKey,
  sortDir,
  onSort,
  align = 'left',
}: {
  label: string;
  k: SortKey;
  sortKey: SortKey;
  sortDir: 'asc' | 'desc';
  onSort: (k: SortKey) => void;
  align?: 'left' | 'right';
}) => (
  <th
    scope="col"
    aria-sort={
      sortKey === k ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'
    }
    className={`px-3 py-2 font-medium text-${align}`}
  >
    <button
      type="button"
      onClick={() => onSort(k)}
      className="focus-ring inline-flex items-center gap-1 rounded hover:text-text-primary"
    >
      {label}
      {sortKey === k &&
        (sortDir === 'asc' ? (
          <ArrowUp className="h-3 w-3" aria-hidden="true" />
        ) : (
          <ArrowDown className="h-3 w-3" aria-hidden="true" />
        ))}
    </button>
  </th>
);

export const FxRatesPage = () => {
  const { data: pairsData, isLoading: pairsLoading } = useFxPairs();
  const pairs = useMemo(() => pairsData?.pairs ?? [], [pairsData]);

  const refresh = useRefreshFxRates();
  const backfill = useBackfillFxRates();
  const addPair = useAddFxPair();
  const removePair = useRemoveFxPair();
  const setFx = useSetFxRate();
  const del = useDeleteFxRate();

  const [selected, setSelected] = useState<string | null>(null);
  const activeCurrency =
    selected && pairs.some((p) => p.currency === selected)
      ? selected
      : (pairs[0]?.currency ?? null);

  const { data: histData } = useFxRateHistory(activeCurrency ?? undefined);
  const rows = useMemo(
    () => (activeCurrency ? (histData?.rates ?? []) : []),
    [histData, activeCurrency]
  );

  const chartData: FxPoint[] = useMemo(
    () =>
      [...rows]
        .map((r) => ({ date: r.rateDate, rate: r.rate }))
        .sort((a, b) => a.date.localeCompare(b.date)),
    [rows]
  );
  const latest = chartData[chartData.length - 1];

  // Modals
  const [backfillOpen, setBackfillOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<string | null>(null);

  // Table sort + paging
  const [sortKey, setSortKey] = useState<SortKey>('rateDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const sorted = useMemo(() => {
    const copy = [...rows];
    copy.sort((a, b) => {
      const av = a[sortKey];
      const bv = b[sortKey];
      const cmp =
        typeof av === 'number' && typeof bv === 'number'
          ? av - bv
          : String(av).localeCompare(String(bv));
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return copy;
  }, [rows, sortKey, sortDir]);
  const pageCount = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount - 1);
  const paged = sorted.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE
  );
  const toggleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir(key === 'source' ? 'asc' : 'desc');
    }
    setPage(0);
  };

  // Inline edit / add. The open editor is keyed on currency+date (not date alone) so switching the
  // active currency derives the editor closed — a stale editValue can never be saved against the
  // new currency.
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');
  const [editError, setEditError] = useState<string | null>(null);
  // Two-step confirm for the destructive single-row delete (keyed currency+date like the editor).
  const [confirmDeleteKey, setConfirmDeleteKey] = useState<string | null>(null);
  const rowKey = (date: string) => `${activeCurrency ?? ''}:${date}`;
  const closeEdit = () => {
    setEditingKey(null);
    setEditError(null);
  };
  const saveEdit = (r: FxRate) => {
    const rate = parseRate(editValue);
    if (!(rate > 0)) {
      setEditError('Enter a positive rate.');
      return;
    }
    if (!activeCurrency) {
      closeEdit();
      return;
    }
    // Close only on success — a failed write keeps the editor open; the error surfaces via toast.
    setFx.mutate(
      { currency: activeCurrency, rate, date: r.rateDate },
      { onSuccess: () => closeEdit() }
    );
  };
  const confirmDelete = (r: FxRate) => {
    if (activeCurrency) {
      del.mutate({ currency: activeCurrency, date: r.rateDate });
    }
    setConfirmDeleteKey(null);
  };

  const [newRate, setNewRate] = useState('');
  const [newDate, setNewDate] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const submitAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const rate = parseRate(newRate);
    if (!(rate > 0)) {
      setFormError('Enter a positive rate.');
      return;
    }
    if (!activeCurrency) {
      return;
    }
    setFormError(null);
    setFx.mutate(
      { currency: activeCurrency, rate, date: newDate || undefined },
      {
        onSuccess: () => {
          setNewRate('');
          setNewDate('');
        },
      }
    );
  };

  if (pairsLoading) {
    return <LoadingSpinner message="Loading FX pairs..." />;
  }

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">FX rates</h1>
          <p className="mt-1 text-sm text-text-secondary">
            Daily exchange rates, all anchored on EUR, used to normalize
            foreign-currency costs. Fetched before each snapshot; manual entries
            override the fetched value.
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            onClick={() => setBackfillOpen(true)}
            disabled={pairs.length === 0}
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary disabled:opacity-50"
          >
            <History className="h-4 w-4" aria-hidden="true" />
            Backfill history
          </button>
          <button
            type="button"
            onClick={() => refresh.mutate()}
            disabled={refresh.isPending || pairs.length === 0}
            className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary disabled:opacity-50"
          >
            <RefreshCw
              className={`h-4 w-4 ${refresh.isPending ? 'animate-spin' : ''}`}
              aria-hidden="true"
            />
            Refresh today
          </button>
        </div>
      </div>

      {/* Pairs bar */}
      <div className="flex flex-wrap items-stretch gap-2">
        {pairs.map((p) => {
          const active = p.currency === activeCurrency;
          return (
            <button
              key={p.currency}
              type="button"
              onClick={() => setSelected(p.currency)}
              className={`focus-ring group flex min-w-[150px] flex-col gap-0.5 rounded-xl border px-4 py-2.5 text-left transition-colors ${
                active
                  ? 'border-primary-500 bg-primary-50'
                  : 'border-border-default bg-surface-card hover:border-primary-300'
              }`}
            >
              <span className="flex items-center gap-1.5 text-sm font-semibold text-text-primary">
                {p.currency} <span className="text-text-muted">→ EUR</span>
              </span>
              <span className="text-xs tabular-nums text-text-secondary">
                {p.rate != null ? (
                  <>
                    1 {p.currency} = {fmtRate(p.rate)} €
                  </>
                ) : (
                  <span className="text-text-muted">no rate yet</span>
                )}
              </span>
            </button>
          );
        })}
        <button
          type="button"
          onClick={() => setAddOpen(true)}
          className="focus-ring inline-flex min-w-[120px] items-center justify-center gap-1.5 rounded-xl border border-dashed border-border-default px-4 py-2.5 text-sm font-medium text-text-secondary transition-colors hover:border-primary-400 hover:text-primary-600"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add pair
        </button>
      </div>

      {pairs.length === 0 || !activeCurrency ? (
        <div className="rounded-lg border border-dashed border-border-default bg-surface-card py-16 text-center">
          <p className="text-sm text-text-secondary">
            No currency pairs tracked yet.
          </p>
          <button
            type="button"
            onClick={() => setAddOpen(true)}
            className="focus-ring mt-3 inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-primary-700"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            Add your first pair
          </button>
        </div>
      ) : (
        <>
          {/* Chart */}
          <div className="rounded-lg border border-border-default bg-surface-card p-5">
            <div className="mb-2 flex items-baseline justify-between gap-3">
              <h2 className="text-sm font-semibold text-text-primary">
                {activeCurrency} → EUR · {currencyName(activeCurrency)}
              </h2>
              <div className="flex items-center gap-3">
                {latest && (
                  <span className="text-sm tabular-nums text-text-secondary">
                    1 {activeCurrency} ={' '}
                    <span className="font-semibold text-text-primary">
                      {fmtRate(latest.rate)}
                    </span>{' '}
                    EUR
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => setRemoveTarget(activeCurrency)}
                  className="focus-ring inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-text-muted transition-colors hover:bg-error-bg hover:text-error-text"
                  title="Stop tracking this pair"
                >
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  Stop tracking
                </button>
              </div>
            </div>
            <FxRateChart data={chartData} currency={activeCurrency} />
          </div>

          {/* Add / override */}
          <form
            onSubmit={submitAdd}
            className="rounded-lg border border-border-default bg-surface-card p-4"
          >
            <div className="flex flex-wrap items-end gap-3">
              <label className="flex flex-col gap-1">
                <span className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
                  Rate (EUR per 1 {activeCurrency})
                </span>
                <input
                  type="text"
                  inputMode="decimal"
                  value={newRate}
                  onChange={(e) => setNewRate(e.target.value)}
                  placeholder="0.92"
                  className="w-32 rounded border border-border-default bg-surface-card px-2 py-1 text-right text-sm tabular-nums"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
                  Date (defaults to today)
                </span>
                <input
                  type="date"
                  value={newDate}
                  onChange={(e) => setNewDate(e.target.value)}
                  className="rounded border border-border-default bg-surface-card px-2 py-1 text-sm"
                />
              </label>
              <button
                type="submit"
                disabled={setFx.isPending}
                className="focus-ring inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
              >
                <Plus className="h-4 w-4" aria-hidden="true" />
                Set rate
              </button>
              {formError && (
                <p className="text-xs text-error-text">{formError}</p>
              )}
            </div>
          </form>

          {/* History table */}
          <div className="overflow-hidden rounded-lg border border-border-default bg-surface-card">
            <table className="w-full text-sm">
              <thead className="border-b border-border-default bg-surface-page text-xs text-text-secondary">
                <tr>
                  <SortHeader
                    label="Date"
                    k="rateDate"
                    sortKey={sortKey}
                    sortDir={sortDir}
                    onSort={toggleSort}
                  />
                  <SortHeader
                    label="Rate (EUR)"
                    k="rate"
                    sortKey={sortKey}
                    sortDir={sortDir}
                    onSort={toggleSort}
                    align="right"
                  />
                  <SortHeader
                    label="Source"
                    k="source"
                    sortKey={sortKey}
                    sortDir={sortDir}
                    onSort={toggleSort}
                  />
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody>
                {paged.length === 0 ? (
                  <tr>
                    <td
                      colSpan={4}
                      className="px-3 py-6 text-center text-text-secondary"
                    >
                      No rates yet. Refresh today, backfill, or set one above.
                    </td>
                  </tr>
                ) : (
                  paged.map((r) => {
                    const editing = editingKey === rowKey(r.rateDate);
                    return (
                      <tr
                        key={r.rateDate}
                        className="border-b border-border-default last:border-0"
                      >
                        <td className="px-3 py-2 tabular-nums text-text-primary">
                          {r.rateDate}
                        </td>
                        <td className="px-3 py-2 text-right tabular-nums text-text-primary">
                          {editing ? (
                            <span className="inline-flex flex-col items-end gap-0.5">
                              <input
                                autoFocus
                                type="text"
                                inputMode="decimal"
                                value={editValue}
                                onChange={(e) => setEditValue(e.target.value)}
                                onKeyDown={(e) => {
                                  if (e.key === 'Enter') {
                                    saveEdit(r);
                                  }
                                  if (e.key === 'Escape') {
                                    closeEdit();
                                  }
                                }}
                                aria-invalid={editError != null}
                                aria-describedby={
                                  editError
                                    ? `fx-edit-error-${r.rateDate}`
                                    : undefined
                                }
                                className="w-28 rounded border border-border-default bg-surface-card px-1.5 py-0.5 text-right text-xs tabular-nums"
                                aria-label={`Rate on ${r.rateDate}`}
                              />
                              {editError && (
                                <span
                                  id={`fx-edit-error-${r.rateDate}`}
                                  role="alert"
                                  className="text-[10px] text-error-text"
                                >
                                  {editError}
                                </span>
                              )}
                            </span>
                          ) : (
                            fmtRate(r.rate)
                          )}
                        </td>
                        <td className="px-3 py-2">
                          <span
                            className={`text-[10px] uppercase tracking-wider ${
                              r.source === 'manual'
                                ? 'text-warning-text'
                                : 'text-text-muted'
                            }`}
                          >
                            {r.source}
                          </span>
                        </td>
                        <td className="px-3 py-2 text-right">
                          {editing ? (
                            <span className="inline-flex items-center gap-1">
                              <button
                                type="button"
                                onClick={() => saveEdit(r)}
                                disabled={setFx.isPending}
                                className="focus-ring rounded p-1 text-success-text hover:bg-surface-page disabled:opacity-50"
                                aria-label="Save"
                              >
                                <Check
                                  className="h-3.5 w-3.5"
                                  aria-hidden="true"
                                />
                              </button>
                              <button
                                type="button"
                                onClick={closeEdit}
                                className="focus-ring rounded p-1 text-text-muted hover:bg-surface-page"
                                aria-label="Cancel"
                              >
                                <X className="h-3.5 w-3.5" aria-hidden="true" />
                              </button>
                            </span>
                          ) : confirmDeleteKey === rowKey(r.rateDate) ? (
                            <span className="inline-flex items-center gap-1">
                              <span className="text-[10px] text-text-secondary">
                                Delete?
                              </span>
                              <button
                                type="button"
                                onClick={() => confirmDelete(r)}
                                disabled={del.isPending}
                                className="focus-ring rounded p-1 text-error-text hover:bg-surface-page disabled:opacity-50"
                                aria-label={`Confirm delete rate on ${r.rateDate}`}
                                title="Confirm delete"
                              >
                                <Check
                                  className="h-3.5 w-3.5"
                                  aria-hidden="true"
                                />
                              </button>
                              <button
                                type="button"
                                onClick={() => setConfirmDeleteKey(null)}
                                className="focus-ring rounded p-1 text-text-muted hover:bg-surface-page"
                                aria-label="Cancel delete"
                              >
                                <X className="h-3.5 w-3.5" aria-hidden="true" />
                              </button>
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1">
                              <button
                                type="button"
                                onClick={() => {
                                  setEditingKey(rowKey(r.rateDate));
                                  setEditValue(String(r.rate));
                                  setEditError(null);
                                }}
                                className="focus-ring rounded p-1 text-text-muted hover:text-text-primary"
                                aria-label={`Edit rate on ${r.rateDate}`}
                                title="Override rate"
                              >
                                <Pencil
                                  className="h-3 w-3"
                                  aria-hidden="true"
                                />
                              </button>
                              <button
                                type="button"
                                onClick={() =>
                                  setConfirmDeleteKey(rowKey(r.rateDate))
                                }
                                disabled={del.isPending}
                                className="focus-ring rounded p-1 text-text-muted hover:text-error-text disabled:opacity-50"
                                aria-label={`Delete rate on ${r.rateDate}`}
                                title="Delete rate"
                              >
                                <Trash2
                                  className="h-3 w-3"
                                  aria-hidden="true"
                                />
                              </button>
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>

            {sorted.length > PAGE_SIZE && (
              <div className="flex items-center justify-between border-t border-border-default px-3 py-2 text-xs text-text-secondary">
                <span className="tabular-nums">
                  {safePage * PAGE_SIZE + 1}–
                  {Math.min((safePage + 1) * PAGE_SIZE, sorted.length)} of{' '}
                  {sorted.length}
                </span>
                <span className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={safePage === 0}
                    className="focus-ring rounded p-1 hover:text-text-primary disabled:opacity-40"
                    aria-label="Previous page"
                  >
                    <ChevronLeft className="h-4 w-4" aria-hidden="true" />
                  </button>
                  <span className="tabular-nums">
                    {safePage + 1}/{pageCount}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setPage((p) => Math.min(pageCount - 1, p + 1))
                    }
                    disabled={safePage >= pageCount - 1}
                    className="focus-ring rounded p-1 hover:text-text-primary disabled:opacity-40"
                    aria-label="Next page"
                  >
                    <ChevronRight className="h-4 w-4" aria-hidden="true" />
                  </button>
                </span>
              </div>
            )}
          </div>
        </>
      )}

      {addOpen && (
        <AddPairModal
          isPending={addPair.isPending}
          tracked={pairs.map((p) => p.currency)}
          onClose={() => setAddOpen(false)}
          onConfirm={(currency) =>
            addPair.mutate(currency, {
              onSuccess: () => {
                setSelected(currency);
                setAddOpen(false);
              },
            })
          }
        />
      )}
      <BackfillFxModal
        open={backfillOpen}
        isPending={backfill.isPending}
        onClose={() => setBackfillOpen(false)}
        onConfirm={(since) =>
          backfill.mutate(since, { onSuccess: () => setBackfillOpen(false) })
        }
      />
      <RemovePairModal
        currency={removeTarget}
        rateCount={removeTarget === activeCurrency ? rows.length : 0}
        isPending={removePair.isPending}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() =>
          removeTarget &&
          removePair.mutate(removeTarget, {
            onSuccess: () => {
              setSelected(null);
              setRemoveTarget(null);
            },
          })
        }
      />
    </div>
  );
};
