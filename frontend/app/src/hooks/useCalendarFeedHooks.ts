import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as calendarFeedsApi from '../api/calendarFeeds';
import type { CreateCalendarFeedRequest } from '../generated/models';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useCalendarFeeds = () => {
  return useQuery({
    queryKey: queryKeys.calendarFeeds.all(),
    queryFn: calendarFeedsApi.getCalendarFeeds,
  });
};

export const useCreateCalendarFeed = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateCalendarFeedRequest) =>
      calendarFeedsApi.createCalendarFeed(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.calendarFeeds.all(),
      });
      showToast('Calendar feed created', 'success');
    },
    onError: (error) => showToast(getErrorMessage(error), 'error'),
  });
};

export const useRotateCalendarFeedToken = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (identifier: string) =>
      calendarFeedsApi.rotateCalendarFeedToken(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.calendarFeeds.all(),
      });
      showToast('Feed URL regenerated. Old URL is now invalid.', 'success');
    },
    onError: (error) => showToast(getErrorMessage(error), 'error'),
  });
};

export const useDeleteCalendarFeed = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (identifier: string) =>
      calendarFeedsApi.deleteCalendarFeed(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.calendarFeeds.all(),
      });
      showToast('Calendar feed deleted', 'success');
    },
    onError: (error) => showToast(getErrorMessage(error), 'error'),
  });
};
