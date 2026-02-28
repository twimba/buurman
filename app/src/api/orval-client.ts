import type { AxiosRequestConfig } from 'axios';
import client from './client';

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => client({ ...config, ...options }).then(({ data }) => data);

export type ErrorType<Error> = import('axios').AxiosError<Error>;
export type BodyType<BodyData> = BodyData;
