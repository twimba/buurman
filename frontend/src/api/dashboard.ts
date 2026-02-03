import client from './client';

export interface DashboardStats {
  totalProperties: number;
  occupiedUnits: number;
  vacantUnits: number;
  maintenanceUnits: number;
  unavailableUnits: number;
  monthlyIncome: {
    amount: number;
    currency: string;
  };
  occupancyRate: number;
}

export interface RecentActivity {
  id: string;
  entityType: string;
  entityId: string;
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

export interface AuditLogFilters {
  entityType?: string;
  action?: string;
  search?: string;
}

export const getAllAuditLogs = async (
  filters?: AuditLogFilters
): Promise<RecentActivity[]> => {
  const response = await client.get('/audit-logs', {
    params: filters,
  });
  return response.data;
};
