import { useRef } from 'react';
import { EyeOff, GripVertical, Plus } from 'lucide-react';

import {
  PANEL_REGISTRY,
  type PanelDefinition,
} from '../../config/dashboardPanels';
import { useDashboardContext } from '../../context/DashboardContext';
import { useBentoLayout } from '../../hooks/useBentoLayout';

const REG_BY_ID = new Map<string, PanelDefinition>(
  PANEL_REGISTRY.map((d) => [d.id, d])
);

// Static class strings so Tailwind's JIT can detect them.
const SPAN_CLASS: Record<PanelDefinition['colSpan'], string> = {
  1: 'lg:col-span-1',
  2: 'lg:col-span-2',
  3: 'lg:col-span-3',
  4: 'lg:col-span-4',
};

/**
 * 4-column bento grid driven by the per-user layout (order + visibility), persisted via
 * {@link useBentoLayout}. In edit mode panels can be drag-reordered (native DnD), hidden, and
 * restored. Resize is intentionally not supported (no react-grid-layout dependency).
 */
export const BentoGrid = () => {
  const { editing } = useDashboardContext();
  const { items, reorderById, setHidden } = useBentoLayout();
  const draggedId = useRef<string | null>(null);

  const visible = items.filter((i) => !i.hidden);
  const hidden = items.filter((i) => i.hidden);

  return (
    <div className="space-y-3">
      {editing && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg border border-dashed border-border-strong bg-surface-page px-3 py-2">
          <span className="text-xs font-medium text-text-secondary">
            Drag panels to reorder.
          </span>
          {hidden.length > 0 && (
            <span className="text-xs text-text-muted">Add back:</span>
          )}
          {hidden.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setHidden(item.id, false)}
              className="focus-ring inline-flex items-center gap-1 rounded-md border border-border-default bg-surface-card px-2 py-1 text-xs font-medium text-text-secondary hover:text-text-primary"
            >
              <Plus className="h-3 w-3" aria-hidden="true" />
              {REG_BY_ID.get(item.id)?.title ?? item.id}
            </button>
          ))}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
        {visible.map((item) => {
          const def = REG_BY_ID.get(item.id);
          if (!def) {
            return null;
          }
          const { Component } = def;
          return (
            <div
              key={item.id}
              className={`relative ${SPAN_CLASS[def.colSpan]} ${editing ? 'cursor-move rounded-lg ring-2 ring-primary-200' : ''}`}
              draggable={editing}
              onDragStart={() => {
                draggedId.current = item.id;
              }}
              onDragOver={(e) => {
                if (editing) {
                  e.preventDefault();
                }
              }}
              onDrop={(e) => {
                if (!editing) {
                  return;
                }
                e.preventDefault();
                const from = draggedId.current;
                if (from && from !== item.id) {
                  reorderById(from, item.id);
                }
                draggedId.current = null;
              }}
            >
              {editing && (
                <div className="absolute right-2 top-2 z-10 flex items-center gap-1">
                  <span className="rounded bg-surface-card/90 p-1 text-text-muted shadow-sm">
                    <GripVertical className="h-3.5 w-3.5" aria-hidden="true" />
                  </span>
                  <button
                    type="button"
                    onClick={() => setHidden(item.id, true)}
                    aria-label={`Hide ${def.title}`}
                    title={`Hide ${def.title}`}
                    className="focus-ring rounded bg-surface-card/90 p-1 text-text-muted shadow-sm hover:text-text-primary"
                  >
                    <EyeOff className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                </div>
              )}
              <Component />
            </div>
          );
        })}
      </div>
    </div>
  );
};
