import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { broadcastsApi } from '../api/broadcasts';
import type {
  CreateBroadcastMessageRequest,
  UpdateBroadcastMessageRequest,
} from '../types';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

export const useBroadcasts = () => {
  return useQuery({
    queryKey: ['broadcasts'],
    queryFn: () => broadcastsApi.list().then((res) => res.data),
  });
};

export const useCreateBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateBroadcastMessageRequest) =>
      broadcastsApi.create(data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
      trackEvent(AnalyticsEvent.BO_BROADCAST_CREATED);
    },
  });
};

export const useUpdateBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpdateBroadcastMessageRequest;
    }) => broadcastsApi.update(identifier, data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
    },
  });
};

export const useDeleteBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (identifier: string) => broadcastsApi.delete(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
    },
  });
};
