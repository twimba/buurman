import client from './client';
import { PageParams, PageResponse } from '../types/common';
import type { NotificationStatsResponse } from '../generated/models';
import {
  NotificationResponse,
  NotificationFilterParams,
} from '../types/notification';

export const getNotifications = async (
  params?: NotificationFilterParams & PageParams
): Promise<PageResponse<NotificationResponse>> => {
  const response = await client.get('/notifications', { params });
  return response.data;
};

export const getNotification = async (
  identifier: string
): Promise<NotificationResponse> => {
  const response = await client.get(`/notifications/${identifier}`);
  return response.data;
};

export const getNotificationStats =
  async (): Promise<NotificationStatsResponse> => {
    const response = await client.get('/notifications/stats');
    return response.data;
  };

export const resendNotification = async (
  identifier: string
): Promise<NotificationResponse> => {
  const response = await client.post(`/notifications/${identifier}/resend`);
  return response.data;
};

export const refreshNotificationStatus = async (
  identifier: string
): Promise<NotificationResponse> => {
  const response = await client.post(
    `/notifications/${identifier}/refresh-status`
  );
  return response.data;
};
