import { useQuery } from '@tanstack/react-query';
import { health, info } from '@/generated/api/health/health';
import { queryKeys } from '../lib/queryKeys';

export const useHealth = () => {
  return useQuery({
    queryKey: queryKeys.health.status(),
    queryFn: () => health(),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};

export const useInfo = () => {
  return useQuery({
    queryKey: queryKeys.health.info(),
    queryFn: () => info(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
