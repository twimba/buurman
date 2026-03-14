import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { PageParams } from '../types/common';
import { NotificationFilterParams } from '../types/notification';
import * as notificationsApi from '../api/notifications';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useNotifications = (
  params?: NotificationFilterParams & PageParams
) => {
  return useQuery({
    queryKey: ['notifications', params],
    queryFn: () => notificationsApi.getNotifications(params),
    placeholderData: keepPreviousData,
  });
};

export const useNotification = (identifier: string | undefined) => {
  return useQuery({
    queryKey: ['notification', identifier],
    queryFn: () => notificationsApi.getNotification(identifier ?? ''),
    enabled: !!identifier,
  });
};

export const useNotificationStats = () => {
  return useQuery({
    queryKey: ['notification-stats'],
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
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notification-stats'] });
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
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notification-stats'] });
      showToast('Status refreshed', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
