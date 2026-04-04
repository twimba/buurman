import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { rateLimitBucketsApi } from '../api/rateLimitBuckets';

export const useRateLimitSummary = () => {
  return useQuery({
    queryKey: ['rateLimitSummary'],
    queryFn: () => rateLimitBucketsApi.getSummary().then((res) => res.data),
    refetchInterval: 15_000,
  });
};

export const useRateLimitBuckets = (params: {
  configKey?: string;
  clientIp?: string;
  page?: number;
  size?: number;
}) => {
  return useQuery({
    queryKey: ['rateLimitBuckets', params],
    queryFn: () => rateLimitBucketsApi.list(params).then((res) => res.data),
    refetchInterval: 15_000,
  });
};

export const useDeleteRateLimitBucket = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bucketId: string) =>
      rateLimitBucketsApi.deleteBucket(bucketId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rateLimitBuckets'] });
      queryClient.invalidateQueries({ queryKey: ['rateLimitSummary'] });
    },
  });
};

export const useDeleteRateLimitBucketsByConfigKey = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (configKey: string) =>
      rateLimitBucketsApi.deleteByConfigKey(configKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rateLimitBuckets'] });
      queryClient.invalidateQueries({ queryKey: ['rateLimitSummary'] });
    },
  });
};
