import { useCallback, useMemo, useState } from 'react';

/**
 * Multi-select state for a list of identifiers. Shift-click range selection is intentionally
 * left out — payments are acted on via "select all on page" plus individual toggles.
 */
export const usePaymentSelection = (visibleIds: string[]) => {
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const toggle = useCallback((id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  const allVisibleSelected = useMemo(
    () => visibleIds.length > 0 && visibleIds.every((id) => selected.has(id)),
    [visibleIds, selected]
  );

  const toggleAll = useCallback(() => {
    setSelected((prev) => {
      if (visibleIds.every((id) => prev.has(id))) {
        const next = new Set(prev);
        visibleIds.forEach((id) => next.delete(id));
        return next;
      }
      return new Set([...prev, ...visibleIds]);
    });
  }, [visibleIds]);

  const clear = useCallback(() => setSelected(new Set()), []);

  return { selected, toggle, toggleAll, allVisibleSelected, clear };
};
