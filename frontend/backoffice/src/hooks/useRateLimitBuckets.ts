import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getRateLimitSummary,
  listRateLimitBuckets,
  deleteRateLimitBucket,
  deleteRateLimitBucketsByConfigKey,
} from '../generated/api/backoffice-rate-limits/backoffice-rate-limits';
import type { ListRateLimitBucketsParams } from '../generated/models';

export const useRateLimitSummary = () => {
  return useQuery({
    queryKey: ['rateLimitSummary'],
    queryFn: getRateLimitSummary,
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
    queryFn: () => listRateLimitBuckets(params as ListRateLimitBucketsParams),
    refetchInterval: 15_000,
  });
};

export const useDeleteRateLimitBucket = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (bucketId: string) => deleteRateLimitBucket(bucketId),
    errorTitle: "Couldn't delete bucket",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rateLimitBuckets'] });
      queryClient.invalidateQueries({ queryKey: ['rateLimitSummary'] });
    },
  });
};

export const useDeleteRateLimitBucketsByConfigKey = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (configKey: string) =>
      deleteRateLimitBucketsByConfigKey({ configKey }),
    errorTitle: "Couldn't delete buckets",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rateLimitBuckets'] });
      queryClient.invalidateQueries({ queryKey: ['rateLimitSummary'] });
    },
  });
};
