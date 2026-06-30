import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listBroadcastMessages,
  createBroadcastMessage,
  updateBroadcastMessage,
  deleteBroadcastMessage,
} from '../generated/api/backoffice-broadcasts/backoffice-broadcasts';
import type {
  BroadcastMessage,
  CreateBroadcastMessageRequest,
  UpdateBroadcastMessageRequest,
} from '../types';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

export const useBroadcasts = () => {
  return useQuery({
    queryKey: ['broadcasts'],
    queryFn: () => listBroadcastMessages() as Promise<BroadcastMessage[]>,
  });
};

export const useCreateBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateBroadcastMessageRequest) =>
      createBroadcastMessage(data),
    errorTitle: "Couldn't create broadcast",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
      trackEvent(AnalyticsEvent.BO_BROADCAST_CREATED);
    },
  });
};

export const useUpdateBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpdateBroadcastMessageRequest;
    }) => updateBroadcastMessage(identifier, data),
    errorTitle: "Couldn't update broadcast",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
    },
  });
};

export const useDeleteBroadcast = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (identifier: string) => deleteBroadcastMessage(identifier),
    errorTitle: "Couldn't delete broadcast",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broadcasts'] });
    },
  });
};
