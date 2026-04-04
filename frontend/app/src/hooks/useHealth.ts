import { useQuery } from '@tanstack/react-query';
import { getHealth, getInfo } from '@/api/health';
import { queryKeys } from '../lib/queryKeys';

export const useHealth = () => {
  return useQuery({
    queryKey: queryKeys.health.status(),
    queryFn: getHealth,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};

export const useInfo = () => {
  return useQuery({
    queryKey: queryKeys.health.info(),
    queryFn: getInfo,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
