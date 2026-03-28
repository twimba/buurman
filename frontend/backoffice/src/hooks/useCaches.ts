import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { cachesApi } from "../api/caches";

export const useCachesList = () => {
  return useQuery({
    queryKey: ["caches"],
    queryFn: () => cachesApi.list().then((res) => res.data),
    refetchInterval: 15000,
  });
};

export const useCacheDetail = (cacheName: string | null) => {
  return useQuery({
    queryKey: ["caches", cacheName],
    queryFn: () =>
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
      cachesApi.getDetail(cacheName!).then((res) => res.data),
    enabled: !!cacheName,
  });
};

export const useInvalidateCache = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (cacheName: string) => cachesApi.invalidate(cacheName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["caches"] });
    },
  });
};
