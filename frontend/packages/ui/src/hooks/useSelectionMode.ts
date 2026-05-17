import { useCallback, useMemo, useState } from 'react';

export interface UseSelectionModeResult<TId extends string> {
  /** Whether the user is in selection mode (multi-select active). */
  active: boolean;
  /** Set of selected ids. */
  selected: ReadonlySet<TId>;
  /** Number of selected items. */
  count: number;
  /** True iff `id` is currently selected. */
  isSelected: (id: TId) => boolean;
  /** Enter selection mode (optionally pre-selecting one id). */
  enter: (id?: TId) => void;
  /** Exit selection mode and clear selection. */
  exit: () => void;
  /** Toggle an id. If selection mode isn't active, entering it AND selecting. */
  toggle: (id: TId) => void;
  /** Select every id in `ids`. Implicitly enters selection mode. */
  selectAll: (ids: readonly TId[]) => void;
  /** Clear all (without exiting selection mode). */
  clear: () => void;
}

/**
 * Multi-select state machine for list pages. Use alongside <SelectionBar>:
 *
 *   const sel = useSelectionMode<string>();
 *   <SelectionBar open={sel.active} count={sel.count} onCancel={sel.exit}
 *     actions={[...]} />
 *
 *   onLongPress={() => sel.enter(id)}
 *   onClick={() => sel.active ? sel.toggle(id) : navigate(...)}
 */
export function useSelectionMode<
  TId extends string = string,
>(): UseSelectionModeResult<TId> {
  const [active, setActive] = useState(false);
  const [selected, setSelected] = useState<Set<TId>>(() => new Set());

  const enter = useCallback((id?: TId) => {
    setActive(true);
    if (id) {
      setSelected((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });
    }
  }, []);

  const exit = useCallback(() => {
    setActive(false);
    setSelected(new Set());
  }, []);

  const toggle = useCallback((id: TId) => {
    setActive(true);
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

  const selectAll = useCallback((ids: readonly TId[]) => {
    setActive(true);
    setSelected(new Set(ids));
  }, []);

  const clear = useCallback(() => {
    setSelected(new Set());
  }, []);

  const isSelected = useCallback(
    (id: TId) => selected.has(id),
    [selected]
  );

  return useMemo(
    () => ({
      active,
      selected,
      count: selected.size,
      isSelected,
      enter,
      exit,
      toggle,
      selectAll,
      clear,
    }),
    [active, selected, isSelected, enter, exit, toggle, selectAll, clear]
  );
}
