import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { notificationsApi } from "../api/notifications";

interface ListNotificationsParams {
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

export const useNotifications = (params?: ListNotificationsParams) => {
  return useQuery({
    queryKey: ["notifications", params],
    queryFn: () => notificationsApi.list(params).then((res) => res.data),
  });
};

export const useNotification = (identifier: string) => {
  return useQuery({
    queryKey: ["notifications", identifier],
    queryFn: () => notificationsApi.get(identifier).then((res) => res.data),
    enabled: !!identifier,
  });
};

export const useResendNotification = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (identifier: string) =>
      notificationsApi.resend(identifier).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
};

export const useNotificationStats = () => {
  return useQuery({
    queryKey: ["notification-stats"],
    queryFn: () => notificationsApi.stats().then((res) => res.data),
  });
};
