import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, ArrowUp, ArrowDown } from 'lucide-react';
import type { PropertyPerformance } from '@/types/portfolio';

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
      <div className="text-center py-8 text-[#6b7194] dark:text-[#8b90a8] text-sm">
        No property performance data available
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            {columns.map((col) => (
              <th
                key={col.key}
                className={`py-3 px-3 font-medium text-[#6b7194] dark:text-[#8b90a8] cursor-pointer select-none hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] transition-colors ${
                  col.align === 'right' ? 'text-right' : 'text-left'
                }`}
                onClick={() => handleSort(col.key)}
              >
                <span className="inline-flex items-center gap-1">
                  {col.label}
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
                className={`border-b border-[#edf0f7] dark:border-[#1e2130] hover:bg-[#f8f9fc] dark:hover:bg-[#1e2130] transition-colors ${
                  isNegative
                    ? 'border-l-2 border-l-red-400 bg-red-50/50 dark:bg-red-900/10'
                    : ''
                }`}
              >
                <td className="py-3 px-3">
                  <button
                    onClick={() => navigate(`/properties/${row.identifier}`)}
                    className="text-[#5c7cfa] hover:text-[#4263eb] font-medium text-left"
                  >
                    {row.address}
                  </button>
                  {row.currencyMismatch && (
                    <AlertTriangle className="inline-block h-3.5 w-3.5 text-yellow-500 ml-1" />
                  )}
                </td>
                <td className="py-3 px-3 text-[#1a1d2e] dark:text-[#eef0f6]">
                  {row.category}
                </td>
                <td
                  className={`py-3 px-3 text-right tabular-nums font-medium ${
                    isNegative
                      ? 'text-red-600 dark:text-red-400'
                      : 'text-[#1a1d2e] dark:text-[#eef0f6]'
                  }`}
                >
                  {formatMoney(row.monthlyCashFlow, row.currency ?? currency)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-[#1a1d2e] dark:text-[#eef0f6]">
                  {formatMoney(row.annualNoi, row.currency ?? currency)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-[#1a1d2e] dark:text-[#eef0f6]">
                  {formatPercent(row.capRate)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-[#1a1d2e] dark:text-[#eef0f6]">
                  {formatPercent(row.cashOnCash)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-[#1a1d2e] dark:text-[#eef0f6]">
                  {formatPercent(row.occupancyRate)}
                </td>
                <td className="py-3 px-3 text-right tabular-nums text-[#1a1d2e] dark:text-[#eef0f6]">
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
