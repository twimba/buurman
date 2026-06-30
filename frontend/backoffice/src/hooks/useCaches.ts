import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listCaches,
  getCacheDetail,
  invalidateCache,
} from '../generated/api/backoffice-caches/backoffice-caches';

export const useCachesList = () => {
  return useQuery({
    queryKey: ['caches'],
    queryFn: () => listCaches(),
    refetchInterval: 15000,
  });
};

export const useCacheDetail = (cacheName: string | null) => {
  return useQuery({
    queryKey: ['caches', cacheName],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => getCacheDetail(cacheName!),
    enabled: !!cacheName,
  });
};

export const useInvalidateCache = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (cacheName: string) => invalidateCache(cacheName),
    errorTitle: "Couldn't invalidate cache",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['caches'] });
    },
  });
};
