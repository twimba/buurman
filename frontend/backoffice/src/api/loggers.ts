import client from './client';
import type { LoggerConfiguration } from '../types';

export const loggersApi = {
  list: () => client.get<LoggerConfiguration[]>('/loggers'),
  setLevel: (loggerName: string, level: string | null) =>
    client.post<LoggerConfiguration>(
      `/loggers/${encodeURIComponent(loggerName)}/level`,
      { level }
    ),
  resetAll: () => client.post('/loggers/reset'),
};
