import { renderHook, act } from '@testing-library/react';
import { usePagination } from '../usePagination';
import { createWrapper } from '../../test/test-utils';

describe('usePagination', () => {
  const wrapper = createWrapper();

  it('returns correct default values', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    expect(result.current.page).toBe(0);
    expect(result.current.size).toBe(25);
    expect(result.current.sort).toBeUndefined();
    expect(result.current.direction).toBe('desc');
  });

  it('accepts custom default values via options', () => {
    const { result } = renderHook(
      () =>
        usePagination({
          defaultSize: 50,
          defaultSort: 'name',
          defaultDirection: 'asc',
        }),
      { wrapper }
    );

    expect(result.current.page).toBe(0);
    expect(result.current.size).toBe(50);
    expect(result.current.sort).toBe('name');
    expect(result.current.direction).toBe('asc');
  });

  it('handlePageChange updates the page', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    act(() => result.current.handlePageChange(3));

    expect(result.current.page).toBe(3);
  });

  it('handleSizeChange updates size and resets page to 0', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    act(() => result.current.handlePageChange(5));
    expect(result.current.page).toBe(5);

    act(() => result.current.handleSizeChange(10));
    expect(result.current.size).toBe(10);
    expect(result.current.page).toBe(0);
  });

  it('handleSortChange sets sort field and direction to asc', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    act(() => result.current.handleSortChange('name'));

    expect(result.current.sort).toBe('name');
    expect(result.current.direction).toBe('asc');
  });

  it('handleSortChange toggles direction when same field clicked twice', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    act(() => result.current.handleSortChange('name'));
    expect(result.current.direction).toBe('asc');

    act(() => result.current.handleSortChange('name'));
    expect(result.current.direction).toBe('desc');

    act(() => result.current.handleSortChange('name'));
    expect(result.current.direction).toBe('asc');
  });

  it('handleSortChange resets page to 0', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    act(() => result.current.handlePageChange(4));
    expect(result.current.page).toBe(4);

    act(() => result.current.handleSortChange('date'));
    expect(result.current.page).toBe(0);
  });

  it('resetPage sets page back to 0', () => {
    const { result } = renderHook(() => usePagination(), { wrapper });

    act(() => result.current.handlePageChange(7));
    expect(result.current.page).toBe(7);

    act(() => result.current.resetPage());
    expect(result.current.page).toBe(0);
  });

  it('pageParams has the correct shape matching current state', () => {
    const { result } = renderHook(
      () => usePagination({ defaultSize: 10, defaultSort: 'id' }),
      { wrapper }
    );

    expect(result.current.pageParams).toEqual({
      page: 0,
      size: 10,
      sort: 'id',
      direction: 'desc',
    });

    act(() => result.current.handlePageChange(2));
    act(() => result.current.handleSortChange('name'));

    expect(result.current.pageParams).toEqual({
      page: 0,
      size: 10,
      sort: 'name',
      direction: 'asc',
    });
  });
});
