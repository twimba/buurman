import client from './client';
import { PageResponse, PageParams } from '@/types/common';

export interface TakeoutResponse {
  identifier: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: number;
  fileSize: number | null;
  createdAt: string;
  completedAt: string | null;
  expiresAt: string | null;
  downloadUrl: string | null;
}

export const requestTakeout = async (): Promise<TakeoutResponse> => {
  const response = await client.post('/takeouts');
  return response.data;
};

export const listTakeouts = async (
  params?: PageParams
): Promise<PageResponse<TakeoutResponse>> => {
  const response = await client.get('/takeouts', { params });
  return response.data;
};

export const getTakeout = async (
  identifier: string
): Promise<TakeoutResponse> => {
  const response = await client.get(`/takeouts/${identifier}`);
  return response.data;
};

export const deleteTakeout = async (identifier: string): Promise<void> => {
  await client.delete(`/takeouts/${identifier}`);
};
