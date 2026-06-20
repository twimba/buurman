import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryKey,
} from '@tanstack/react-query';

import {
  getDashboardActionQueue,
  getDashboardBusiness,
  getDashboardCostWatch,
  getDashboardFunnel,
  getDashboardGeo,
  getDashboardLatencyHeatmap,
  getDashboardLayout,
  getDashboardLiveTail,
  getDashboardProductEntities,
  getDashboardPropertyLocations,
  getDashboardSchedulerHealth,
  getDashboardStatusStrip,
  getDashboardTopTeams,
  saveDashboardLayout,
  snoozeDashboardActionItem,
} from '../generated/api/backoffice-dashboard/backoffice-dashboard';
import type { ActionQueueResponse, PanelPlacement } from '../generated/models';
import { useDashboardContext } from '../context/DashboardContext';

const ROOT = 'bo-dashboard';

/** Refetch cadence per panel (ms). */
export const REFETCH = {
  statusStrip: 30_000,
  actionQueue: 30_000,
  productEntities: 300_000,
  funnel: 3_600_000,
  topTeams: 3_600_000,
  schedulerHealth: 60_000,
  business: 300_000,
  costWatch: 3_600_000,
  latencyHeatmap: 300_000,
  geo: 3_600_000,
  liveTail: 10_000,
} as const;

/** All dashboard queries share this prefix so the topbar can refresh them together. */
export const dashboardQueryKey: QueryKey = [ROOT];

/**
 * Standardises a panel query: namespaced key, half-interval staleness, and respects the page-level
 * Pause toggle (disables polling when paused).
 */
function usePanelQuery<T>(
  key: string,
  fn: () => Promise<T>,
  refetchInterval: number
) {
  const { paused } = useDashboardContext();
  return useQuery({
    queryKey: [ROOT, key],
    queryFn: fn,
    refetchInterval: paused ? false : refetchInterval,
    staleTime: Math.floor(refetchInterval / 2),
  });
}

export const useStatusStrip = () =>
  usePanelQuery('status-strip', getDashboardStatusStrip, REFETCH.statusStrip);

export const useActionQueue = () =>
  usePanelQuery('action-queue', getDashboardActionQueue, REFETCH.actionQueue);

export const useProductEntities = () =>
  usePanelQuery(
    'product-entities',
    getDashboardProductEntities,
    REFETCH.productEntities
  );

export const useFunnel = () =>
  usePanelQuery('funnel', getDashboardFunnel, REFETCH.funnel);

export const useTopTeams = () =>
  usePanelQuery('top-teams', getDashboardTopTeams, REFETCH.topTeams);

export const useSchedulerHealth = () =>
  usePanelQuery(
    'scheduler-health',
    getDashboardSchedulerHealth,
    REFETCH.schedulerHealth
  );

export const useBusiness = () =>
  usePanelQuery('business', getDashboardBusiness, REFETCH.business);

export const useCostWatch = () =>
  usePanelQuery('cost-watch', getDashboardCostWatch, REFETCH.costWatch);

export const useLatencyHeatmap = () =>
  usePanelQuery(
    'latency-heatmap',
    getDashboardLatencyHeatmap,
    REFETCH.latencyHeatmap
  );

export const useGeo = () => usePanelQuery('geo', getDashboardGeo, REFETCH.geo);

/** Property point cloud for the world map. Fetched lazily — only when the map is opened. */
export const usePropertyLocations = (enabled: boolean) =>
  useQuery({
    queryKey: [ROOT, 'property-locations'],
    queryFn: getDashboardPropertyLocations,
    enabled,
    staleTime: REFETCH.geo,
  });

// Wrapped so React Query's context isn't passed through as the `params` argument.
export const useLiveTail = () =>
  usePanelQuery('live-tail', () => getDashboardLiveTail(), REFETCH.liveTail);

export const useDashboardLayout = () =>
  useQuery({
    queryKey: [ROOT, 'layout'],
    queryFn: () => getDashboardLayout(),
    staleTime: 5 * 60_000,
  });

/** Persist the per-user bento layout; mirrors the server's response back into the cache. */
export const useSaveDashboardLayout = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (panels: PanelPlacement[]) => saveDashboardLayout({ panels }),
    onSuccess: (data) => {
      queryClient.setQueryData([ROOT, 'layout'], data);
    },
  });
};

/** Snooze an action-queue item; updates the action-queue cache from the server's fresh response. */
export const useSnoozeActionItem = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemKey, hours }: { itemKey: string; hours: number }) =>
      snoozeDashboardActionItem({ itemKey, hours }),
    onSuccess: (data: ActionQueueResponse) => {
      queryClient.setQueryData([ROOT, 'action-queue'], data);
      // Keep the status-strip "Alerts" pillar consistent with the queue after a snooze.
      queryClient.invalidateQueries({ queryKey: [ROOT, 'status-strip'] });
    },
  });
};
