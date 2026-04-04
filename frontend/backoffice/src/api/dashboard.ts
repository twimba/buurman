import client from './client';
import type { BackofficeDashboardStats } from '../types';

export const dashboardApi = {
  stats: () => client.get<BackofficeDashboardStats>('/dashboard/stats'),
};
