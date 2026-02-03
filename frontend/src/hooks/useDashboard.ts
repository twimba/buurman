import { useQuery } from '@tanstack/react-query';
import {
  getDashboardStats,
  getRecentActivities,
  getAllAuditLogs,
  AuditLogFilters,
} from '@/api/dashboard';

export const useDashboardStats = () => {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: getDashboardStats,
    refetchInterval: 60000, // Refetch every minute
  });
};

export const useRecentActivities = (limit = 10) => {
  return useQuery({
    queryKey: ['dashboard', 'activities', limit],
    queryFn: () => getRecentActivities(limit),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};

export const useAllAuditLogs = (filters?: AuditLogFilters) => {
  return useQuery({
    queryKey: ['auditLogs', filters],
    queryFn: () => getAllAuditLogs(filters),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};
