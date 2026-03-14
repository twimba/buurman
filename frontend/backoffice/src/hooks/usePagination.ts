import { useState, useCallback } from "react";

interface PageParams {
  page: number;
  size: number;
  sort?: string;
  direction?: "asc" | "desc";
}

interface UsePaginationOptions {
  defaultSize?: number;
  defaultSort?: string;
  defaultDirection?: "asc" | "desc";
}

export const usePagination = (options: UsePaginationOptions = {}) => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(options.defaultSize ?? 25);
  const [sort, setSort] = useState<string | undefined>(options.defaultSort);
  const [direction, setDirection] = useState<"asc" | "desc">(
    options.defaultDirection ?? "desc",
  );

  const pageParams: PageParams = { page, size, sort, direction };

  const handlePageChange = useCallback(
    (newPage: number) => setPage(newPage),
    [],
  );

  const handleSizeChange = useCallback((newSize: number) => {
    setSize(newSize);
    setPage(0);
  }, []);

  const handleSortChange = useCallback(
    (field: string) => {
      if (sort === field) {
        setDirection((d) => (d === "asc" ? "desc" : "asc"));
      } else {
        setSort(field);
        setDirection("asc");
      }
      setPage(0);
    },
    [sort],
  );

  const resetPage = useCallback(() => setPage(0), []);

  return {
    page,
    size,
    sort,
    direction,
    pageParams,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  };
};
