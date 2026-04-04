import { useSearchParams, useLocation } from 'react-router-dom';
import { useCallback } from 'react';

export function useTabState<T extends string>(
  defaultTab: T,
  validTabs?: readonly T[]
): [T, (tab: T) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const raw = searchParams.get('tab');
  const activeTab =
    raw && (!validTabs || (validTabs as readonly string[]).includes(raw))
      ? (raw as T)
      : defaultTab;

  const setActiveTab = useCallback(
    (tab: T) => {
      setSearchParams(
        (prev) => {
          if (tab === defaultTab) {
            prev.delete('tab');
          } else {
            prev.set('tab', tab);
          }
          return prev;
        },
        { replace: true, state: location.state }
      );
    },
    [defaultTab, setSearchParams, location.state]
  );

  return [activeTab, setActiveTab];
}
