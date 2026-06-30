import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getPhonePolicy,
  getPhonePolicyMetadata,
  updatePhonePolicy,
  getRateLimit,
  updateRateLimit,
} from '../generated/api/backoffice-settings/backoffice-settings';
import type {
  UpdatePhoneNumberPolicyRequest,
  UpdateRateLimitConfigRequest,
} from '../generated/models';

export const usePhonePolicy = () => {
  return useQuery({
    queryKey: ['phone-policy'],
    queryFn: getPhonePolicy,
  });
};

export const usePhonePolicyMetadata = () => {
  return useQuery({
    queryKey: ['phone-policy-metadata'],
    queryFn: getPhonePolicyMetadata,
    staleTime: Infinity, // static data, never changes
  });
};

export const useUpdatePhonePolicy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdatePhoneNumberPolicyRequest) =>
      updatePhonePolicy(data),
    errorTitle: "Couldn't update phone policy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['phone-policy'] });
    },
  });
};

// ── Rate Limit Config ────────────────────────────────────────────────

export const useRateLimitConfig = (key: string) => {
  return useQuery({
    queryKey: ['rate-limit-config', key],
    queryFn: () => getRateLimit(key),
  });
};

export const useUpdateRateLimitConfig = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      key,
      data,
    }: {
      key: string;
      data: UpdateRateLimitConfigRequest;
    }) => updateRateLimit(key, data),
    errorTitle: "Couldn't update rate limit",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rate-limit-config', variables.key],
      });
    },
  });
};
