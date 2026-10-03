interface QueryLike<T> {
  data: T[] | undefined;
  isLoading: boolean;
  isError: boolean;
  isFetching: boolean;
  refetch: () => unknown;
}

/**
 * Module-level so TanStack Query can memoise the combined result (stable `data` identity).
 * One failing query does not hide the data of the others.
 */
export const combineTemplateQueries = <T>(results: QueryLike<T>[]) => {
  const data = results.flatMap((r) => r.data ?? []);
  return {
    data,
    isLoading: data.length === 0 && results.some((r) => r.isLoading),
    isRefreshing: data.length > 0 && results.some((r) => r.isFetching),
    isError: results.some((r) => r.isError),
    refetch: () => results.forEach((r) => r.refetch()),
  };
};
