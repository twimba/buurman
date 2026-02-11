import { useQuery } from '@tanstack/react-query';
import { getHealth, getInfo } from '@/api/health';

export const useHealth = () => {
  return useQuery({
    queryKey: ['health'],
    queryFn: getHealth,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};

export const useInfo = () => {
  return useQuery({
    queryKey: ['info'],
    queryFn: getInfo,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
