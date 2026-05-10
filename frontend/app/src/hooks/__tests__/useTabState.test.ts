import { renderHook, act } from '@testing-library/react';
import { useTabState } from '../useTabState';
import { createWrapper } from '../../test/test-utils';

describe('useTabState', () => {
  it('returns the default tab when no search param is present', () => {
    const wrapper = createWrapper({ initialEntries: ['/page'] });
    const { result } = renderHook(() => useTabState('overview'), { wrapper });

    expect(result.current[0]).toBe('overview');
  });

  it('returns the tab value from URL search params', () => {
    const wrapper = createWrapper({
      initialEntries: ['/page?tab=settings'],
    });
    const { result } = renderHook(() => useTabState('overview'), { wrapper });

    expect(result.current[0]).toBe('settings');
  });

  it('setTab updates the search params', () => {
    const wrapper = createWrapper({ initialEntries: ['/page'] });
    const { result } = renderHook(() => useTabState('overview'), { wrapper });

    expect(result.current[0]).toBe('overview');

    act(() => result.current[1]('settings'));

    expect(result.current[0]).toBe('settings');
  });

  it('removes the tab param when set back to the default tab', () => {
    const wrapper = createWrapper({
      initialEntries: ['/page?tab=settings'],
    });
    const { result } = renderHook(() => useTabState('overview'), { wrapper });

    expect(result.current[0]).toBe('settings');

    act(() => result.current[1]('overview'));

    // After setting to default, the active tab should be the default
    expect(result.current[0]).toBe('overview');
  });

  it('falls back to default tab when URL param is not in validTabs', () => {
    const wrapper = createWrapper({
      initialEntries: ['/page?tab=invalid'],
    });
    const validTabs = ['overview', 'settings', 'billing'] as const;
    const { result } = renderHook(() => useTabState('overview', validTabs), {
      wrapper,
    });

    expect(result.current[0]).toBe('overview');
  });
});
