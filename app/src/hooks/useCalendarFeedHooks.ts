import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as calendarFeedsApi from '../api/calendarFeeds';
import { CreateCalendarFeedRequest } from '../types/calendarFeed';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useCalendarFeeds = () => {
  return useQuery({
    queryKey: ['calendarFeeds'],
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
      queryClient.invalidateQueries({ queryKey: ['calendarFeeds'] });
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
      queryClient.invalidateQueries({ queryKey: ['calendarFeeds'] });
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
      queryClient.invalidateQueries({ queryKey: ['calendarFeeds'] });
      showToast('Calendar feed deleted', 'success');
    },
    onError: (error) => showToast(getErrorMessage(error), 'error'),
  });
};
