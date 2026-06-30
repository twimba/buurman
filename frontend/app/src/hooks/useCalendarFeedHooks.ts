import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getUserFeeds,
  createFeed,
  rotateFeedToken,
  deleteFeed,
} from '../generated/api/calendar-feeds/calendar-feeds';
import type { CreateCalendarFeedRequest } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

export const useCalendarFeeds = () => {
  return useQuery({
    queryKey: queryKeys.calendarFeeds.all(),
    queryFn: () => getUserFeeds(),
  });
};

export const useCreateCalendarFeed = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Calendar feed created',
    mutationFn: (data: CreateCalendarFeedRequest) => createFeed(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.calendarFeeds.all(),
      });
    },
  });
};

export const useRotateCalendarFeedToken = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Feed URL regenerated. Old URL is now invalid.',
    mutationFn: (identifier: string) => rotateFeedToken(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.calendarFeeds.all(),
      });
    },
  });
};

export const useDeleteCalendarFeed = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Calendar feed deleted',
    mutationFn: (identifier: string) => deleteFeed(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.calendarFeeds.all(),
      });
    },
  });
};
