import client from './client';
import type { HealthResponse, InfoResponse } from '@/generated/models';

export const getHealth = async (): Promise<HealthResponse> => {
  const { data } = await client.get<HealthResponse>('/health');
  return data;
};

export const getInfo = async (): Promise<InfoResponse> => {
  const { data } = await client.get<InfoResponse>('/info');
  return data;
};
