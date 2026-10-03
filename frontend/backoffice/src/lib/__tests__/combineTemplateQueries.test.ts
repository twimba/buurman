import { describe, expect, it } from 'vitest';
import { combineTemplateQueries } from '../combineTemplateQueries';

const ok = (data: number[]) => ({
  data,
  isLoading: false,
  isError: false,
  isFetching: false,
  refetch: () => undefined,
});

describe('combineTemplateQueries', () => {
  it('flattens data and keeps partial data when one query fails', () => {
    const r = combineTemplateQueries([
      ok([1]),
      {
        data: undefined,
        isLoading: false,
        isError: true,
        isFetching: false,
        refetch: () => undefined,
      },
    ]);
    expect(r.data).toEqual([1]);
    expect(r.isError).toBe(true);
    expect(r.isLoading).toBe(false);
  });
  it('only reports loading when nothing is available yet', () => {
    const loading = {
      data: undefined,
      isLoading: true,
      isError: false,
      isFetching: true,
      refetch: () => undefined,
    };
    expect(combineTemplateQueries([loading]).isLoading).toBe(true);
    const r = combineTemplateQueries([ok([1]), loading]);
    expect(r.isLoading).toBe(false);
    expect(r.isRefreshing).toBe(true);
  });
  it('refetch calls every query', () => {
    let n = 0;
    const q = {
      ...ok([]),
      refetch: () => {
        n += 1;
      },
    };
    combineTemplateQueries([q, q]).refetch();
    expect(n).toBe(2);
  });
});
