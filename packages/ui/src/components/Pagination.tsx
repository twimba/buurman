import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from 'lucide-react';

interface PaginationProps {
  page: number; // 0-indexed
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

const PAGE_SIZE_OPTIONS = [10, 25, 50, 75, 100, 150, 200, 500];

export const Pagination = ({
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
  onSizeChange,
}: PaginationProps) => {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  const getPageNumbers = (): (number | 'ellipsis')[] => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i);
    }
    const pages: (number | 'ellipsis')[] = [];
    pages.push(0);
    if (page > 2) pages.push('ellipsis');
    const start = Math.max(1, page - 1);
    const end = Math.min(totalPages - 2, page + 1);
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    if (page < totalPages - 3) pages.push('ellipsis');
    if (totalPages > 1) pages.push(totalPages - 1);
    return pages;
  };

  if (totalElements === 0) return null;

  const navBtn = (disabled: boolean) =>
    `p-1.5 rounded-md transition-colors ${
      disabled
        ? 'opacity-30 cursor-not-allowed text-[#6b7194] dark:text-[#5c6180]'
        : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]'
    }`;

  return (
    <div
      style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}
      className="bg-white dark:bg-[#14161f] px-4 py-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]"
    >
      {/* Left: Item count + page size */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }} className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
        <span style={{ whiteSpace: 'nowrap' }}>
          Showing {from} to {to} of {totalElements}
        </span>
        <select
          value={size}
          onChange={(e) => onSizeChange(Number(e.target.value))}
          className="border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-2 py-1 bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
        >
          {PAGE_SIZE_OPTIONS.map((opt) => (
            <option key={opt} value={opt}>
              {opt} / page
            </option>
          ))}
        </select>
      </div>

      {/* Right: Page navigation */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
        <button
          onClick={() => onPageChange(0)}
          disabled={page === 0}
          className={navBtn(page === 0)}
          title="First page"
        >
          <ChevronsLeft className="h-4 w-4" />
        </button>
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className={navBtn(page === 0)}
          title="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>

        {/* Page numbers */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.125rem' }}>
          {getPageNumbers().map((p, idx) =>
            p === 'ellipsis' ? (
              <span
                key={`ellipsis-${idx}`}
                style={{ padding: '0 0.5rem', fontSize: '0.875rem' }}
                className="text-[#6b7194] dark:text-[#8b90a8]"
              >
                ...
              </span>
            ) : (
              <button
                key={p}
                onClick={() => onPageChange(p)}
                style={{
                  minWidth: '2rem',
                  height: '2rem',
                  borderRadius: '0.375rem',
                  fontSize: '0.875rem',
                  fontWeight: 500,
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'all 150ms',
                  ...(p === page
                    ? { backgroundColor: '#5c7cfa', color: '#ffffff' }
                    : {}),
                }}
                className={
                  p === page
                    ? ''
                    : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]'
                }
              >
                {(p as number) + 1}
              </button>
            )
          )}
        </div>

        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className={navBtn(page >= totalPages - 1)}
          title="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
        <button
          onClick={() => onPageChange(totalPages - 1)}
          disabled={page >= totalPages - 1}
          className={navBtn(page >= totalPages - 1)}
          title="Last page"
        >
          <ChevronsRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};
