import { renderHook, act } from '@testing-library/react';
import { useDebounce } from '../useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns the initial value immediately', () => {
    const { result } = renderHook(() => useDebounce('hello', 500));

    expect(result.current).toBe('hello');
  });

  it('does not update during the delay period', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'initial', delay: 500 } }
    );

    rerender({ value: 'updated', delay: 500 });

    act(() => vi.advanceTimersByTime(300));

    expect(result.current).toBe('initial');
  });

  it('updates the value after the delay', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'initial', delay: 500 } }
    );

    rerender({ value: 'updated', delay: 500 });

    act(() => vi.advanceTimersByTime(500));

    expect(result.current).toBe('updated');
  });

  it('cancels the previous timer when value changes again', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'first', delay: 500 } }
    );

    rerender({ value: 'second', delay: 500 });
    act(() => vi.advanceTimersByTime(300));

    rerender({ value: 'third', delay: 500 });
    act(() => vi.advanceTimersByTime(300));

    // 600ms total since 'second', but only 300ms since 'third'
    expect(result.current).toBe('first');

    act(() => vi.advanceTimersByTime(200));

    expect(result.current).toBe('third');
  });

  it('works with different delay values', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'start', delay: 1000 } }
    );

    rerender({ value: 'end', delay: 1000 });

    act(() => vi.advanceTimersByTime(999));
    expect(result.current).toBe('start');

    act(() => vi.advanceTimersByTime(1));
    expect(result.current).toBe('end');
  });
});
