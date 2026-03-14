import client from './client';
import type {
  CalendarFeedResponse,
  CreateCalendarFeedRequest,
} from '../generated/models';

export const getCalendarFeeds = async (): Promise<CalendarFeedResponse[]> => {
  const response = await client.get('/calendar/feeds');
  return response.data;
};

export const createCalendarFeed = async (
  data: CreateCalendarFeedRequest
): Promise<CalendarFeedResponse> => {
  const response = await client.post('/calendar/feeds', data);
  return response.data;
};

export const rotateCalendarFeedToken = async (
  identifier: string
): Promise<CalendarFeedResponse> => {
  const response = await client.post(`/calendar/feeds/${identifier}/rotate`);
  return response.data;
};

export const deleteCalendarFeed = async (identifier: string): Promise<void> => {
  await client.delete(`/calendar/feeds/${identifier}`);
};
