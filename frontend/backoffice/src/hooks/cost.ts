import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';

import {
  addFxPair,
  backfillFxRates,
  deleteFxRate,
  getCostConfig,
  getCostOverview,
  getFxPairs,
  getFxRateHistory,
  getFxRates,
  refreshCost,
  refreshFxRates,
  removeFxPair,
  setFxRate,
  setManualCost,
  updateCostConfig,
} from '../generated/api/backoffice-cost/backoffice-cost';
import type { UpdateCostConfigRequest } from '../generated/models';

const KEY = ['bo-cost', 'overview'];
const CONFIG_KEY = ['bo-cost', 'config'];
const FX_KEY = ['bo-cost', 'fx'];
const FX_PAIRS_KEY = ['bo-cost', 'fx-pairs'];

/** Tracked currency pairs (anchored on EUR) with their latest rate. */
export const useFxPairs = () =>
  useQuery({
    queryKey: FX_PAIRS_KEY,
    queryFn: () => getFxPairs(),
    staleTime: 60_000,
  });

/** Start tracking a new pair (source currency → EUR); fetches today's rate. */
export const useAddFxPair = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (currency: string) => addFxPair({ currency }),
    errorTitle: "Couldn't add pair",
    onSuccess: (data) => {
      queryClient.setQueryData(FX_PAIRS_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
    },
  });
};

/** Stop tracking a pair and remove its stored rates. */
export const useRemoveFxPair = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (currency: string) => removeFxPair(currency),
    errorTitle: "Couldn't remove pair",
    onSuccess: (data) => {
      queryClient.setQueryData(FX_PAIRS_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
    },
  });
};

export const useCostOverview = () =>
  useQuery({
    queryKey: KEY,
    queryFn: () => getCostOverview(),
    staleTime: 60_000,
  });

/** Triggers a fresh snapshot of every provider, then updates the overview cache. */
export const useRefreshCost = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast<Awaited<ReturnType<typeof refreshCost>>, void>({
    mutationFn: () => refreshCost(),
    errorTitle: 'Cost refresh failed',
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
  return useMutationWithToast({
    mutationFn: ({
      provider,
      amountEur,
    }: {
      provider: string;
      amountEur: number;
    }) => setManualCost(provider, { amountEur }),
    errorTitle: "Couldn't save cost",
    onSuccess: (data) => {
      queryClient.setQueryData(KEY, data);
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
    },
  });
};

/** Editable provider cost parameters (e.g. Mailgun plan fee + per-email rate), DB-backed. */
export const useCostConfig = () =>
  useQuery({
    queryKey: CONFIG_KEY,
    queryFn: () => getCostConfig(),
    staleTime: 60_000,
  });

/** Persists provider cost parameters; re-snapshotting the affected provider happens on next refresh. */
export const useUpdateCostConfig = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (body: UpdateCostConfigRequest) => updateCostConfig(body),
    successMessage: 'Cost settings saved',
    errorTitle: "Couldn't save settings",
    onSuccess: (data) => {
      queryClient.setQueryData(CONFIG_KEY, data);
    },
  });
};

/** Latest FX rate per currency (source currency -> EUR), refreshed daily before snapshots. */
export const useFxRates = () =>
  useQuery({
    queryKey: FX_KEY,
    queryFn: () => getFxRates(),
    staleTime: 60_000,
  });

/** Dated FX history (optionally for one currency), newest first — for the table + graph. */
export const useFxRateHistory = (currency?: string) =>
  useQuery({
    queryKey: ['bo-cost', 'fx-history', currency ?? 'all'],
    queryFn: () => getFxRateHistory(currency ? { currency } : undefined),
    staleTime: 60_000,
  });

/** Manually set/override a currency's EUR rate (optionally for a specific day). */
export const useSetFxRate = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      currency,
      rate,
      date,
    }: {
      currency: string;
      rate: number;
      date?: string;
    }) => setFxRate(currency, { rate, date }),
    errorTitle: "Couldn't save rate",
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
      // Costs are FX-normalized to EUR, so a rate change can move the Costs page total.
      queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
};

/** Delete a stored rate for a currency on a specific day. */
export const useDeleteFxRate = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({ currency, date }: { currency: string; date: string }) =>
      deleteFxRate(currency, date),
    errorTitle: "Couldn't delete rate",
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
      queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
};

/** Backfill daily rates from a start date through today. */
export const useBackfillFxRates = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (since: string) => backfillFxRates({ since }),
    errorTitle: 'Backfill failed',
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
      queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
};

/** Fetch live rates on demand. */
export const useRefreshFxRates = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast<Awaited<ReturnType<typeof refreshFxRates>>, void>(
    {
      mutationFn: () => refreshFxRates(),
      errorTitle: 'FX refresh failed',
      onSuccess: (data) => {
        queryClient.setQueryData(FX_KEY, data);
        queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
        queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
        queryClient.invalidateQueries({
          queryKey: ['bo-dashboard', 'cost-watch'],
        });
        queryClient.invalidateQueries({ queryKey: KEY });
      },
    }
  );
};
