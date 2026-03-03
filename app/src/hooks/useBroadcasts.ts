import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as broadcastsApi from '../api/broadcasts';

export const useActiveBroadcasts = (enabled = true) => {
  return useQuery({
    queryKey: ['broadcasts', 'active'],
    queryFn: broadcastsApi.getActiveBroadcasts,
    enabled,
    staleTime: 60_000,
    refetchInterval: 5 * 60_000,
  });
};

export const usePublicBroadcasts = (context: 'login' | 'register') => {
  return useQuery({
    queryKey: ['broadcasts', 'public', context],
    queryFn: () => broadcastsApi.getPublicBroadcasts(context),
    staleTime: 60_000,
  });
};

export const useDismissBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: broadcastsApi.dismissBroadcast,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
    },
  });
};
