import client from './client';

export interface DashboardStats {
  totalProperties: number;
  occupiedUnits: number;
  selfOccupiedUnits: number;
  vacantUnits: number;
  maintenanceUnits: number;
  unavailableUnits: number;
  underRenovationUnits: number;
  fallowUnits: number;
  listedUnits: number;
  monthlyIncome: {
    amount: number;
    currency: string;
  };
  occupancyRate: number;
  rentalOccupancyRate: number;
}

export interface RecentActivity {
  entityType: string;
  entityIdentifier: string;
  entityName: string;
  action: string;
  userName: string;
  timestamp: string;
  description: string;
  changedFields?: Record<string, unknown>;
  oldValues?: Record<string, unknown>;
  newValues?: Record<string, unknown>;
}

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const response = await client.get('/dashboard/stats');
  return response.data;
};

export const getRecentActivities = async (
  limit = 10
): Promise<RecentActivity[]> => {
  const response = await client.get('/dashboard/recent-activities', {
    params: { limit },
  });
  return response.data;
};

import { PageResponse, PageParams } from '@/types/common';

export interface AuditLogFilters {
  entityType?: string;
  action?: string;
  search?: string;
}

export const getAllAuditLogs = async (
  filters?: AuditLogFilters & PageParams
): Promise<PageResponse<RecentActivity>> => {
  const response = await client.get('/audit-logs', {
    params: filters,
  });
  return response.data;
};
