import { useQuery, keepPreviousData } from '@tanstack/react-query';
import {
  getDashboardStats,
  getAllAuditLogs,
  AuditLogFilters,
} from '@/api/dashboard';
import type { PageParams } from '@/types/common';

export const useDashboardStats = () => {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: getDashboardStats,
    refetchInterval: 60000, // Refetch every minute
  });
};

export const useAllAuditLogs = (filters?: AuditLogFilters & PageParams) => {
  return useQuery({
    queryKey: ['auditLogs', filters],
    queryFn: () => getAllAuditLogs(filters),
    placeholderData: keepPreviousData,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};
