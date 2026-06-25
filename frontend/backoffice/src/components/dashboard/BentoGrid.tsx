import { useRef } from 'react';
import {
  ArrowDown,
  ArrowUp,
  EyeOff,
  GripVertical,
  Plus,
  RotateCcw,
} from 'lucide-react';

import {
  PANEL_REGISTRY,
  type PanelDefinition,
} from '../../config/dashboardPanels';
import { useDashboardContext } from '../../context/DashboardContext';
import { useBentoLayout } from '../../hooks/useBentoLayout';

const REG_BY_ID = new Map<string, PanelDefinition>(
  PANEL_REGISTRY.map((d) => [d.id, d])
);

// Static class strings so Tailwind's JIT can detect them. At the md (tablet) breakpoint we cap to a
// balanced 2-up grid: anything wider than 1 column collapses to 2, single-column panels to 1.
const SPAN_CLASS: Record<PanelDefinition['colSpan'], string> = {
  1: 'md:col-span-1 lg:col-span-1',
  2: 'md:col-span-2 lg:col-span-2',
  3: 'md:col-span-2 lg:col-span-3',
  4: 'md:col-span-2 lg:col-span-4',
};

const ROWSPAN_CLASS: Record<NonNullable<PanelDefinition['rowSpan']>, string> = {
  1: 'lg:row-span-1',
  2: 'lg:row-span-2',
};

/**
 * 4-column bento grid driven by the per-user layout (order + visibility), persisted via
 * {@link useBentoLayout}. In edit mode panels can be drag-reordered (native DnD), hidden, and
 * restored. Resize is intentionally not supported (no react-grid-layout dependency).
 */
export const BentoGrid = () => {
  const { editing } = useDashboardContext();
  const { items, reorderById, setHidden, resetToDefault } = useBentoLayout();
  const draggedId = useRef<string | null>(null);

  const visible = items.filter((i) => !i.hidden);
  const hidden = items.filter((i) => i.hidden);

  return (
    <div className="space-y-3">
      {editing && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg border border-dashed border-border-strong bg-surface-page px-3 py-2">
          <span className="text-xs font-medium text-text-secondary">
            Drag panels to reorder, or use the arrow buttons.
          </span>
          <button
            type="button"
            onClick={resetToDefault}
            className="focus-ring inline-flex items-center gap-1 rounded-md border border-border-default bg-surface-card px-2 py-1 text-xs font-medium text-text-secondary hover:text-text-primary"
          >
            <RotateCcw className="h-3 w-3" aria-hidden="true" />
            Reset layout
          </button>
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

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:auto-rows-[minmax(0,auto)] lg:grid-flow-row-dense lg:grid-cols-4">
        {visible.map((item, idx) => {
          const def = REG_BY_ID.get(item.id);
          if (!def) {
            return null;
          }
          const { Component } = def;
          const prevId = idx > 0 ? visible[idx - 1].id : null;
          const nextId = idx < visible.length - 1 ? visible[idx + 1].id : null;
          return (
            <div
              key={item.id}
              className={`relative ${SPAN_CLASS[def.colSpan]} ${ROWSPAN_CLASS[def.rowSpan ?? 1]} ${editing ? 'cursor-move rounded-lg ring-2 ring-primary-200' : ''}`}
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
                <div className="absolute inset-x-0 top-0 z-10 flex items-center justify-end gap-1 rounded-t-lg border-b border-border-default bg-surface-card/95 px-2 py-1 backdrop-blur-sm">
                  <span className="mr-auto rounded p-1 text-text-muted">
                    <GripVertical className="h-3.5 w-3.5" aria-hidden="true" />
                  </span>
                  <button
                    type="button"
                    onClick={() => prevId && reorderById(item.id, prevId)}
                    disabled={!prevId}
                    aria-label={`Move ${def.title} earlier`}
                    title="Move earlier"
                    className="focus-ring rounded bg-surface-card/90 p-1 text-text-muted shadow-sm hover:text-text-primary disabled:opacity-40"
                  >
                    <ArrowUp className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => nextId && reorderById(item.id, nextId)}
                    disabled={!nextId}
                    aria-label={`Move ${def.title} later`}
                    title="Move later"
                    className="focus-ring rounded bg-surface-card/90 p-1 text-text-muted shadow-sm hover:text-text-primary disabled:opacity-40"
                  >
                    <ArrowDown className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
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
              <div className={editing ? 'pt-8' : undefined}>
                <Component />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
