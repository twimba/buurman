import type { AxiosRequestConfig } from 'axios';
import client from './client';

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const merged = { ...config, ...options };
  // Strip /backoffice prefix — client.ts baseURL already includes it
  if (typeof merged.url === 'string' && merged.url.startsWith('/backoffice')) {
    merged.url = merged.url.slice('/backoffice'.length);
  }
  return client(merged).then(({ data }) => data);
};

export type ErrorType<Error> = import('axios').AxiosError<Error>;
export type BodyType<BodyData> = BodyData;
