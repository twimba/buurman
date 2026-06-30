import type { AxiosRequestConfig } from 'axios';
import client from './client';

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig
): Promise<T> =>
  client({
    ...config,
    ...options,
    // Merge (not replace) params/headers so per-call options can extend the generated config.
    params: { ...config.params, ...options?.params },
    headers: { ...config.headers, ...options?.headers },
  }).then(({ data }) => data);

export type ErrorType<Error> = import('axios').AxiosError<Error>;
export type BodyType<BodyData> = BodyData;
