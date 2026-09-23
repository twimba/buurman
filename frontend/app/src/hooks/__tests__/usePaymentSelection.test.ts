import { renderHook, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { usePaymentSelection } from '../usePaymentSelection';

describe('usePaymentSelection', () => {
  it('toggles individual identifiers', () => {
    const { result } = renderHook(() => usePaymentSelection(['a', 'b', 'c']));

    act(() => result.current.toggle('a'));
    expect(result.current.selected.has('a')).toBe(true);
    expect(result.current.allVisibleSelected).toBe(false);

    act(() => result.current.toggle('a'));
    expect(result.current.selected.size).toBe(0);
  });

  it('selects all visible and reports allVisibleSelected', () => {
    const { result } = renderHook(() => usePaymentSelection(['a', 'b']));

    act(() => result.current.toggleAll());
    expect(Array.from(result.current.selected).sort()).toEqual(['a', 'b']);
    expect(result.current.allVisibleSelected).toBe(true);

    act(() => result.current.toggleAll());
    expect(result.current.selected.size).toBe(0);
  });

  it('keeps selections from other pages when toggling all on the current page', () => {
    const { result, rerender } = renderHook(
      ({ ids }: { ids: string[] }) => usePaymentSelection(ids),
      { initialProps: { ids: ['a'] } }
    );
    act(() => result.current.toggle('a'));

    rerender({ ids: ['b', 'c'] });
    expect(result.current.allVisibleSelected).toBe(false);
    act(() => result.current.toggleAll());
    expect(Array.from(result.current.selected).sort()).toEqual(['a', 'b', 'c']);

    act(() => result.current.clear());
    expect(result.current.selected.size).toBe(0);
  });

  it('reports allVisibleSelected false for an empty page', () => {
    const { result } = renderHook(() => usePaymentSelection([]));
    expect(result.current.allVisibleSelected).toBe(false);
  });
});
