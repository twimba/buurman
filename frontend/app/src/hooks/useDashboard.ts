import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { getDashboardStats } from '../generated/api/dashboard/dashboard';
import { getAllAuditLogs } from '../generated/api/audit-logs/audit-logs';
import type { GetAllAuditLogsParams } from '../generated/models';
import type { PageParams } from '@/types/common';
import { queryKeys } from '../lib/queryKeys';

export interface AuditLogFilters {
  entityType?: string;
  action?: string;
  search?: string;
}

export const useDashboardStats = () => {
  return useQuery({
    queryKey: queryKeys.dashboard.stats(),
    queryFn: () => getDashboardStats(),
    refetchInterval: 60000, // Refetch every minute
  });
};

export const useAllAuditLogs = (filters?: AuditLogFilters & PageParams) => {
  return useQuery({
    queryKey: queryKeys.dashboard.auditLogs(filters),
    queryFn: () => getAllAuditLogs(filters as GetAllAuditLogsParams),
    placeholderData: keepPreviousData,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};
