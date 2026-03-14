import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, ArrowUp, ArrowDown } from 'lucide-react';
import type { PropertyPerformance } from '@/types/portfolio';
import { MetricHint } from '@/components/common/MetricHint';

type SortKey =
  | 'address'
  | 'category'
  | 'monthlyCashFlow'
  | 'annualNoi'
  | 'capRate'
  | 'cashOnCash'
  | 'occupancyRate'
  | 'completenessPercent';

function formatMoney(value: number | undefined, currencyCode?: string): string {
  if (value == null) {
    return '-';
  }
  if (currencyCode) {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: currencyCode,
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(value);
    } catch {
      // fall through
    }
  }
  return value.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });
}

function formatPercent(value: number | undefined): string {
  if (value == null) {
    return '-';
  }
  return `${value.toFixed(1)}%`;
}

interface PropertyPerformanceTableProps {
  data: PropertyPerformance[];
  currency?: string;
}

export const PropertyPerformanceTable = ({
  data,
  currency,
}: PropertyPerformanceTableProps) => {
  const navigate = useNavigate();
  const [sortKey, setSortKey] = useState<SortKey>('monthlyCashFlow');
  const [sortAsc, setSortAsc] = useState(false);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortAsc(!sortAsc);
    } else {
      setSortKey(key);
      setSortAsc(false);
    }
  };

  const sorted = useMemo(() => {
    return [...data].sort((a, b) => {
      const av = a[sortKey] ?? -Infinity;
      const bv = b[sortKey] ?? -Infinity;
      if (av < bv) {
        return sortAsc ? -1 : 1;
      }
      if (av > bv) {
        return sortAsc ? 1 : -1;
      }
      return 0;
    });
  }, [data, sortKey, sortAsc]);

  const columns: { key: SortKey; label: string; align?: string }[] = [
    { key: 'address', label: 'Property' },
    { key: 'category', label: 'Category' },
    { key: 'monthlyCashFlow', label: 'Monthly CF', align: 'right' },
    { key: 'annualNoi', label: 'Annual NOI', align: 'right' },
    { key: 'capRate', label: 'Cap Rate', align: 'right' },
    { key: 'cashOnCash', label: 'CoC', align: 'right' },
    { key: 'occupancyRate', label: 'Occupancy', align: 'right' },
    { key: 'completenessPercent', label: 'Data %', align: 'right' },
  ];

  if (data.length === 0) {
    return (
      <div className="text-center py-8 text-text-secondary text-sm">
        No property performance data available
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border-default">
            {columns.map((col) => (
              <th
                key={col.key}
                className={`py-3 px-3 font-medium text-text-secondary cursor-pointer select-none hover:text-text-primary transition-colors ${
                  col.align === 'right' ? 'text-right' : 'text-left'
                }`}
                onClick={() => handleSort(col.key)}
              >
                <span className="inline-flex items-center gap-1">
                  <MetricHint label={col.label} />
                  {sortKey === col.key &&
                    (sortAsc ? (
                      <ArrowUp className="h-3 w-3" />
                    ) : (
                      <ArrowDown className="h-3 w-3" />
                    ))}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map((row) => {
            const isNegative = (row.monthlyCashFlow ?? 0) < 0;
            return (
              <tr
                key={row.identifier}
                className={`border-b border-border-default hover:bg-surface-page transition-colors ${
                  isNegative
                    ? 'border-l-2 border-l-error-border bg-error-bg/50'
                    : ''
                }`}
              >
                <td className="py-3 px-3">
                  <button
                    onClick={() => navigate(`/properties/${row.identifier}`)}
                    className="text-primary-500 hover:text-primary-600 font-medium text-left"
                  >
                    {row.address}
                  </button>
                  {row.currencyMismatch && (
                    <AlertTriangle className="inline-block h-3.5 w-3.5 text-warning-text ml-1" />
                  )}
                </td>
                <td className="py-3 px-3 text-text-primary">{row.category}</td>
                <td
                  className={`py-3 px-3 text-right tabular-nums font-medium ${
                    isNegative ? 'text-error-text' : 'text-text-primary'
                  }`}
                >
                  {formatMoney(row.monthlyCashFlow, row.currency ?? currency)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-text-primary">
                  {formatMoney(row.annualNoi, row.currency ?? currency)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-text-primary">
                  {formatPercent(row.capRate)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-text-primary">
                  {formatPercent(row.cashOnCash)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-text-primary">
                  {formatPercent(row.occupancyRate)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-text-primary">
                  {row.completenessPercent}%
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
