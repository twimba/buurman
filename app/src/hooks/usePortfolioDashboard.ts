import { useQuery } from '@tanstack/react-query';
import { getPortfolioDashboard } from '../api/dashboard';

export const usePortfolioDashboard = (months?: number) => {
  return useQuery({
    queryKey: ['portfolioDashboard', months],
    queryFn: () => getPortfolioDashboard(months),
    staleTime: 120_000,
    retry: (failureCount, error: unknown) => {
      if (
        error &&
        typeof error === 'object' &&
        'response' in error &&
        (error as { response?: { status?: number } }).response?.status === 403
      ) {
        return false;
      }
      return failureCount < 3;
    },
  });
};
