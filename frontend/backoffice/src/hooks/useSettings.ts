import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as settingsApi from "../api/settings";

export const usePhonePolicy = () => {
  return useQuery({
    queryKey: ["phone-policy"],
    queryFn: settingsApi.getPhonePolicy,
  });
};

export const usePhonePolicyMetadata = () => {
  return useQuery({
    queryKey: ["phone-policy-metadata"],
    queryFn: settingsApi.getPhonePolicyMetadata,
    staleTime: Infinity, // static data, never changes
  });
};

export const useUpdatePhonePolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: settingsApi.updatePhonePolicy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["phone-policy"] });
    },
  });
};

// ── Rate Limit Config ────────────────────────────────────────────────

export const useRateLimitConfig = (key: string) => {
  return useQuery({
    queryKey: ["rate-limit-config", key],
    queryFn: () => settingsApi.getRateLimitConfig(key),
  });
};

export const useUpdateRateLimitConfig = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      key,
      data,
    }: {
      key: string;
      data: settingsApi.UpdateRateLimitConfigRequest;
    }) => settingsApi.updateRateLimitConfig(key, data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["rate-limit-config", variables.key],
      });
    },
  });
};
