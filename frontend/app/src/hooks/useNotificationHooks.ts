import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { PageParams } from '../types/common';
import { NotificationFilterParams } from '../types/notification';
import * as notificationsApi from '../api/notifications';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useNotifications = (
  params?: NotificationFilterParams & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.notifications.all(params),
    queryFn: () => notificationsApi.getNotifications(params),
    placeholderData: keepPreviousData,
  });
};

export const useNotification = (identifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.notifications.detail(identifier),
    queryFn: () => notificationsApi.getNotification(identifier ?? ''),
    enabled: !!identifier,
  });
};

export const useNotificationStats = () => {
  return useQuery({
    queryKey: queryKeys.notifications.stats(),
    queryFn: () => notificationsApi.getNotificationStats(),
  });
};

export const useResendNotification = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (identifier: string) =>
      notificationsApi.resendNotification(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.stats(),
      });
      showToast('Notification resent successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useRefreshNotificationStatus = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (identifier: string) =>
      notificationsApi.refreshNotificationStatus(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.notifications.stats(),
      });
      showToast('Status refreshed', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
