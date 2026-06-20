import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  getCostOverview,
  refreshCost,
  setManualCost,
} from '../generated/api/backoffice-cost/backoffice-cost';

const KEY = ['bo-cost', 'overview'];

export const useCostOverview = () =>
  useQuery({
    queryKey: KEY,
    queryFn: () => getCostOverview(),
    staleTime: 60_000,
  });

/** Triggers a fresh snapshot of every provider, then updates the overview cache. */
export const useRefreshCost = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => refreshCost(),
    onSuccess: (data) => {
      queryClient.setQueryData(KEY, data);
      // Keep the mission-control Cost watch panel in sync.
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
    },
  });
};

/** Sets/overrides a provider's monthly EUR cost (manual fallback). */
export const useSetManualCost = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      provider,
      amountEur,
    }: {
      provider: string;
      amountEur: number;
    }) => setManualCost(provider, { amountEur }),
    onSuccess: (data) => {
      queryClient.setQueryData(KEY, data);
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
    },
  });
};
