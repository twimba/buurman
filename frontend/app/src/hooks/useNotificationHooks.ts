import { useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import { PageParams } from '../types/common';
import { NotificationFilterParams } from '../types/notification';
import {
  getNotifications,
  getNotification,
  getStats,
  resendNotification,
  refreshStatus,
} from '../generated/api/notifications/notifications';
import type { GetNotificationsParams } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

export const useNotifications = (
  params?: NotificationFilterParams & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.notifications.all(params),
    queryFn: () => getNotifications(params as GetNotificationsParams),
    placeholderData: keepPreviousData,
  });
};

export const useNotification = (identifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.notifications.detail(identifier),
    queryFn: () => getNotification(identifier ?? ''),
    enabled: !!identifier,
  });
};

export const useNotificationStats = () => {
  return useQuery({
    queryKey: queryKeys.notifications.stats(),
    queryFn: () => getStats(),
  });
};

export const useResendNotification = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    successMessage: 'Notification resent successfully',
    mutationFn: (identifier: string) => resendNotification(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.stats(),
      });
    },
  });
};

export const useRefreshNotificationStatus = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    successMessage: 'Status refreshed',
    mutationFn: (identifier: string) => refreshStatus(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.stats(),
      });
    },
  });
};
