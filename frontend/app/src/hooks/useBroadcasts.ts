import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getActiveBroadcasts,
  getPublicBroadcasts,
  dismissBroadcast,
} from '../generated/api/broadcasts/broadcasts';
import { queryKeys } from '../lib/queryKeys';

export const useActiveBroadcasts = (enabled = true) => {
  return useQuery({
    queryKey: queryKeys.broadcasts.active(),
    queryFn: () => getActiveBroadcasts(),
    enabled,
    staleTime: 60_000,
    refetchInterval: 5 * 60_000,
  });
};

export const usePublicBroadcasts = (context: 'login' | 'register') => {
  return useQuery({
    queryKey: queryKeys.broadcasts.public(context),
    queryFn: () => getPublicBroadcasts({ context }),
    staleTime: 60_000,
  });
};

export const useDismissBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (identifier: string) => dismissBroadcast(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.broadcasts.all() });
    },
  });
};
