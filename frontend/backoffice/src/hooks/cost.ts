import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@buurman/ui';

import {
  addFxPair,
  backfillFxRates,
  deleteFxRate,
  getCostOverview,
  getFxPairs,
  getFxRateHistory,
  getFxRates,
  refreshCost,
  refreshFxRates,
  removeFxPair,
  setFxRate,
  setManualCost,
} from '../generated/api/backoffice-cost/backoffice-cost';

const KEY = ['bo-cost', 'overview'];
const FX_KEY = ['bo-cost', 'fx'];
const FX_PAIRS_KEY = ['bo-cost', 'fx-pairs'];

/** Best-effort human message from an Axios/HTTP error for surfacing in a toast. */
const errorMessage = (e: unknown): string => {
  const ax = e as { response?: { data?: { message?: string } }; message?: string };
  return ax?.response?.data?.message || ax?.message || 'Request failed';
};

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
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (currency: string) => addFxPair({ currency }),
    onSuccess: (data) => {
      queryClient.setQueryData(FX_PAIRS_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
    },
    onError: (e) => showToast(`Couldn't add pair: ${errorMessage(e)}`, 'error'),
  });
};

/** Stop tracking a pair and remove its stored rates. */
export const useRemoveFxPair = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (currency: string) => removeFxPair(currency),
    onSuccess: (data) => {
      queryClient.setQueryData(FX_PAIRS_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
    },
    onError: (e) => showToast(`Couldn't remove pair: ${errorMessage(e)}`, 'error'),
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
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => refreshCost(),
    onSuccess: (data) => {
      queryClient.setQueryData(KEY, data);
      // Keep the mission-control Cost watch panel in sync.
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
    },
    onError: (e) => showToast(`Cost refresh failed: ${errorMessage(e)}`, 'error'),
  });
};

/** Sets/overrides a provider's monthly EUR cost (manual fallback). */
export const useSetManualCost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
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
    onError: (e) => showToast(`Couldn't save cost: ${errorMessage(e)}`, 'error'),
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
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      currency,
      rate,
      date,
    }: {
      currency: string;
      rate: number;
      date?: string;
    }) => setFxRate(currency, { rate, date }),
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
    },
    onError: (e) => showToast(`Couldn't save rate: ${errorMessage(e)}`, 'error'),
  });
};

/** Delete a stored rate for a currency on a specific day. */
export const useDeleteFxRate = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({ currency, date }: { currency: string; date: string }) =>
      deleteFxRate(currency, date),
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
    },
    onError: (e) => showToast(`Couldn't delete rate: ${errorMessage(e)}`, 'error'),
  });
};

/** Backfill daily rates from a start date through today. */
export const useBackfillFxRates = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (since: string) => backfillFxRates({ since }),
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
    },
    onError: (e) => showToast(`Backfill failed: ${errorMessage(e)}`, 'error'),
  });
};

/** Fetch live rates on demand. */
export const useRefreshFxRates = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => refreshFxRates(),
    onSuccess: (data) => {
      queryClient.setQueryData(FX_KEY, data);
      queryClient.invalidateQueries({ queryKey: ['bo-cost', 'fx-history'] });
      queryClient.invalidateQueries({ queryKey: FX_PAIRS_KEY });
      queryClient.invalidateQueries({
        queryKey: ['bo-dashboard', 'cost-watch'],
      });
    },
    onError: (e) => showToast(`FX refresh failed: ${errorMessage(e)}`, 'error'),
  });
};
