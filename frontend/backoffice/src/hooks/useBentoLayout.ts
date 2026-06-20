import { useEffect, useMemo, useRef, useState } from 'react';

import type { PanelPlacement } from '../generated/models';
import {
  PANEL_REGISTRY,
  type PanelDefinition,
} from '../config/dashboardPanels';
import { useDashboardLayout, useSaveDashboardLayout } from './dashboard';

export interface LayoutItem {
  id: string;
  hidden: boolean;
}

const REG_BY_ID = new Map<string, PanelDefinition>(
  PANEL_REGISTRY.map((d) => [d.id, d])
);

/**
 * Merge a persisted layout with the registry: keep known persisted panels in their saved order,
 * then append any registry panels the user has never seen (e.g. newly shipped) as visible. Unknown
 * persisted ids are dropped.
 */
const merge = (saved: PanelPlacement[]): LayoutItem[] => {
  const seen = new Set<string>();
  const items: LayoutItem[] = [];
  for (const p of saved) {
    if (REG_BY_ID.has(p.panel) && !seen.has(p.panel)) {
      items.push({ id: p.panel, hidden: p.hidden });
      seen.add(p.panel);
    }
  }
  for (const def of PANEL_REGISTRY) {
    if (!seen.has(def.id)) {
      items.push({ id: def.id, hidden: false });
    }
  }
  return items;
};

const toPlacements = (items: LayoutItem[]): PanelPlacement[] =>
  items.map((it, idx) => ({
    panel: it.id,
    x: 0,
    y: idx,
    w: REG_BY_ID.get(it.id)?.colSpan ?? 1,
    h: 1,
    hidden: it.hidden,
  }));

const move = (items: LayoutItem[], from: number, to: number): LayoutItem[] => {
  if (from === to || from < 0 || to < 0) {
    return items;
  }
  const next = [...items];
  const [picked] = next.splice(from, 1);
  next.splice(to, 0, picked);
  return next;
};

/**
 * Local, optimistic bento layout state seeded from the server, with debounced persistence. Single
 * source of truth for the grid's order/visibility while editing.
 */
export const useBentoLayout = () => {
  const { data } = useDashboardLayout();
  const save = useSaveDashboardLayout();
  // `items` stays null until the user edits; until then we render the server layout directly. This
  // avoids seeding state in an effect (and the cascading render that comes with it).
  const [items, setItems] = useState<LayoutItem[] | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (timer.current) {
        clearTimeout(timer.current);
      }
    },
    []
  );

  const effective = useMemo(
    () => items ?? merge(data?.panels ?? []),
    [items, data]
  );

  const update = (next: LayoutItem[]) => {
    setItems(next);
    if (timer.current) {
      clearTimeout(timer.current);
    }
    timer.current = setTimeout(() => save.mutate(toPlacements(next)), 800);
  };

  const reorderById = (draggedId: string, targetId: string) => {
    const from = effective.findIndex((i) => i.id === draggedId);
    const to = effective.findIndex((i) => i.id === targetId);
    update(move(effective, from, to));
  };

  const setHidden = (id: string, hidden: boolean) => {
    update(effective.map((i) => (i.id === id ? { ...i, hidden } : i)));
  };

  return { items: effective, reorderById, setHidden };
};
