import client from './client';
import type {
  BackofficeNotification,
  NotificationStats,
  PageResponse,
} from '../types';

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

export const notificationsApi = {
  list: (params?: ListNotificationsParams) =>
    client.get<PageResponse<BackofficeNotification>>('/notifications', {
      params,
    }),
  get: (identifier: string) =>
    client.get<BackofficeNotification>(`/notifications/${identifier}`),
  resend: (identifier: string) =>
    client.post<BackofficeNotification>(`/notifications/${identifier}/resend`),
  stats: () => client.get<NotificationStats>('/notifications/stats'),
};
