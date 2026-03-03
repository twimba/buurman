import client from './client';

export interface BroadcastMessage {
  identifier: string;
  title: string;
  body: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  startAt: string;
  endAt?: string;
}

export const getActiveBroadcasts = async (): Promise<BroadcastMessage[]> => {
  const response = await client.get('/broadcasts');
  return response.data;
};

export const getPublicBroadcasts = async (
  context: 'login' | 'register'
): Promise<BroadcastMessage[]> => {
  const response = await client.get('/broadcasts/public', {
    params: { context },
  });
  return response.data;
};

export const dismissBroadcast = async (identifier: string): Promise<void> => {
  await client.post(`/broadcasts/${identifier}/dismiss`);
};
