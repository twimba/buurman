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
  // Calculate showing range
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  // Generate visible page numbers (max 5 with ellipsis)
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

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white dark:bg-[#14161f] px-4 py-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
      {/* Left: Item count + page size */}
      <div className="flex items-center gap-4 text-sm text-[#3d4463] dark:text-[#c4c8db]">
        <span>
          Showing {from} to {to} of {totalElements}
        </span>
        <select
          value={size}
          onChange={(e) => onSizeChange(Number(e.target.value))}
          className="border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-2 py-1 bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
        >
          {PAGE_SIZE_OPTIONS.map((opt) => (
            <option key={opt} value={opt}>
              {opt} / page
            </option>
          ))}
        </select>
      </div>

      {/* Right: Page navigation */}
      <div className="flex items-center gap-1">
        {/* First */}
        <button
          onClick={() => onPageChange(0)}
          disabled={page === 0}
          className="p-1.5 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-30 disabled:cursor-not-allowed text-[#3d4463] dark:text-[#c4c8db]"
          title="First page"
        >
          <ChevronsLeft className="h-4 w-4" />
        </button>
        {/* Prev */}
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="p-1.5 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-30 disabled:cursor-not-allowed text-[#3d4463] dark:text-[#c4c8db]"
          title="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>

        {/* Page numbers - hidden on very small screens */}
        <div className="hidden sm:flex items-center gap-1">
          {getPageNumbers().map((p, idx) =>
            p === 'ellipsis' ? (
              <span
                key={`ellipsis-${idx}`}
                className="px-2 text-[#6b7194] dark:text-[#8b90a8]"
              >
                ...
              </span>
            ) : (
              <button
                key={p}
                onClick={() => onPageChange(p)}
                className={`min-w-[32px] h-8 rounded text-sm font-medium ${
                  p === page
                    ? 'bg-[#5c7cfa] text-white'
                    : 'hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]'
                }`}
              >
                {p + 1}
              </button>
            )
          )}
        </div>

        {/* Mobile page indicator */}
        <span className="sm:hidden text-sm text-[#3d4463] dark:text-[#c4c8db] px-2">
          {page + 1} / {totalPages}
        </span>

        {/* Next */}
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="p-1.5 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-30 disabled:cursor-not-allowed text-[#3d4463] dark:text-[#c4c8db]"
          title="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
        {/* Last */}
        <button
          onClick={() => onPageChange(totalPages - 1)}
          disabled={page >= totalPages - 1}
          className="p-1.5 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-30 disabled:cursor-not-allowed text-[#3d4463] dark:text-[#c4c8db]"
          title="Last page"
        >
          <ChevronsRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};
