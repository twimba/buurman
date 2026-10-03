import { useState, useCallback } from 'react';

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
  // Uppercase to match the API's SortDirection enum; the hook keeps a lowercase
  // `direction` for UI (sort arrows) and uppercases it here for the request.
  direction?: 'ASC' | 'DESC';
}

interface UsePaginationOptions {
  defaultPage?: number;
  defaultSize?: number;
  defaultSort?: string;
  defaultDirection?: 'asc' | 'desc';
}

export const usePagination = (options: UsePaginationOptions = {}) => {
  const [page, setPage] = useState(options.defaultPage ?? 0);
  const [size, setSize] = useState(options.defaultSize ?? 25);
  const [sort, setSort] = useState<string | undefined>(options.defaultSort);
  const [direction, setDirection] = useState<'asc' | 'desc'>(
    options.defaultDirection ?? 'desc'
  );

  const pageParams: PageParams = {
    page,
    size,
    sort,
    direction: direction.toUpperCase() as 'ASC' | 'DESC',
  };

  const handlePageChange = useCallback(
    (newPage: number) => setPage(newPage),
    []
  );

  const handleSizeChange = useCallback((newSize: number) => {
    setSize(newSize);
    setPage(0);
  }, []);

  const handleSortChange = useCallback(
    (field: string) => {
      if (sort === field) {
        setDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
      } else {
        setSort(field);
        setDirection('asc');
      }
      setPage(0);
    },
    [sort]
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
