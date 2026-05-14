import { useQuery } from '@tanstack/react-query';
import { getPortfolioDashboard } from '../api/dashboard';
import { queryKeys } from '../lib/queryKeys';

export const usePortfolioDashboard = (
  months?: number,
  startDate?: string,
  endDate?: string
) => {
  return useQuery({
    queryKey: queryKeys.dashboard.portfolio(months, startDate, endDate),
    queryFn: () => getPortfolioDashboard(months, startDate, endDate),
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
