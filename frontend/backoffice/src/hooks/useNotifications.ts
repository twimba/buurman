import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listNotifications,
  getNotification,
  resendNotification,
  getStats,
} from '../generated/api/backoffice-notifications/backoffice-notifications';
import type { ListNotificationsParams } from '../generated/models';

interface ListNotificationsArgs {
  page?: number;
  size?: number;
  type?: string;
  channel?: string;
  status?: string;
  recipientEmail?: string;
  teamIdentifier?: string;
  dateFrom?: string;
  dateTo?: string;
  sort?: string;
  direction?: string;
}

export const useNotifications = (params?: ListNotificationsArgs) => {
  return useQuery({
    queryKey: ['notifications', params],
    queryFn: () => listNotifications(params as ListNotificationsParams),
  });
};

export const useNotification = (identifier: string) => {
  return useQuery({
    queryKey: ['notifications', identifier],
    queryFn: () => getNotification(identifier),
    enabled: !!identifier,
  });
};

export const useResendNotification = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: (identifier: string) => resendNotification(identifier),
    errorTitle: "Couldn't resend notification",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
};

export const useNotificationStats = () => {
  return useQuery({
    queryKey: ['notification-stats'],
    queryFn: getStats,
  });
};
