import type { ComponentType } from 'react';

import { ActionQueuePanel } from '../components/dashboard/panels/ActionQueuePanel';
import { BusinessKpiPanel } from '../components/dashboard/panels/BusinessKpiPanel';
import { LiveTailPanel } from '../components/dashboard/panels/LiveTailPanel';
import { ActivationFunnelPanel } from '../components/dashboard/panels/ActivationFunnelPanel';
import { TopTeamsPanel } from '../components/dashboard/panels/TopTeamsPanel';
import { ProductEntitiesPanel } from '../components/dashboard/panels/ProductEntitiesPanel';
import { CostWatchPanel } from '../components/dashboard/panels/CostWatchPanel';
import { SchedulerHealthPanel } from '../components/dashboard/panels/SchedulerHealthPanel';
import { LatencyHeatmapPanel } from '../components/dashboard/panels/LatencyHeatmapPanel';
import { GeoPanel } from '../components/dashboard/panels/GeoPanel';

export interface PanelDefinition {
  id: string;
  title: string;
  Component: ComponentType;
  /** Default column span in the 4-column bento grid (1–4). */
  colSpan: 1 | 2 | 3 | 4;
  /** Default row span (1–2). Tall panels (e.g. the map) span 2 so shorter panels can stack
   *  vertically beside them in the same band. */
  rowSpan?: 1 | 2;
}

/**
 * Single source of truth for the bento grid. Order = default layout (spec §4.2). Adding a panel is
 * one entry here. Per-user reorder/hide (react-grid-layout) lands in a later phase and will read
 * this registry for defaults and for the "+ Add panel" menu.
 */
export const PANEL_REGISTRY: PanelDefinition[] = [
  // Row A — act now (support) + is it running (SRE) + is spend sane (finance). The verdict triad.
  {
    id: 'action-queue',
    title: 'Action queue',
    Component: ActionQueuePanel,
    colSpan: 2,
  },
  {
    id: 'scheduler-health',
    title: 'Scheduler health',
    Component: SchedulerHealthPanel,
    colSpan: 1,
  },
  {
    id: 'cost-watch',
    title: 'Cost watch',
    Component: CostWatchPanel,
    colSpan: 1,
  },
  // Row B — growth (leadership)
  {
    id: 'product-entities',
    title: 'Product entities',
    Component: ProductEntitiesPanel,
    colSpan: 2,
  },
  {
    id: 'funnel',
    title: 'Activation funnel',
    Component: ActivationFunnelPanel,
    colSpan: 1,
  },
  { id: 'top-teams', title: 'Top teams', Component: TopTeamsPanel, colSpan: 1 },
  // Row C band — the tall map spans two rows; two shorter panels stack vertically beside it.
  { id: 'geo', title: 'Geo', Component: GeoPanel, colSpan: 2, rowSpan: 2 },
  {
    id: 'latency-heatmap',
    title: 'Latency heatmap',
    Component: LatencyHeatmapPanel,
    colSpan: 2,
    rowSpan: 1,
  },
  {
    id: 'business',
    title: 'Business · MTD',
    Component: BusinessKpiPanel,
    colSpan: 2,
    rowSpan: 1,
  },
  // Row D — full-width opt-in log stream
  { id: 'live-tail', title: 'Live tail', Component: LiveTailPanel, colSpan: 4 },
];
