import client from './client';
import { HealthResponse, InfoResponse } from '@/types/health';

export const getHealth = async (): Promise<HealthResponse> => {
  const { data } = await client.get<HealthResponse>('/health');
  return data;
};

export const getInfo = async (): Promise<InfoResponse> => {
  const { data } = await client.get<InfoResponse>('/info');
  return data;
};
