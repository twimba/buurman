import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { featureFlagsApi } from "../api/featureFlags";
import type { UpdateFlagRequest } from "../api/featureFlags";

export const useAdminStatus = () => {
  return useQuery({
    queryKey: ["feature-flags", "admin-status"],
    queryFn: () => featureFlagsApi.getAdminStatus().then((res) => res.data),
    staleTime: 5 * 60 * 1000,
  });
};

export const useGlobalFeatureFlags = () => {
  return useQuery({
    queryKey: ["feature-flags", "global"],
    queryFn: () => featureFlagsApi.getGlobal().then((res) => res.data),
  });
};

export const useUserFeatureFlags = (userIdentifier: string | null) => {
  return useQuery({
    queryKey: ["feature-flags", "user", userIdentifier],
    queryFn: () =>
      featureFlagsApi.getForUser(userIdentifier!).then((res) => res.data),
    enabled: !!userIdentifier,
  });
};

export const useUpdateGlobalFlag = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      flagName,
      data,
    }: {
      flagName: string;
      data: UpdateFlagRequest;
    }) => featureFlagsApi.updateGlobalFlag(flagName, data).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["feature-flags"] });
    },
  });
};

export const useUpsertIdentityOverride = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      userIdentifier,
      teamIdentifier,
      flagName,
      data,
    }: {
      userIdentifier: string;
      teamIdentifier: string;
      flagName: string;
      data: UpdateFlagRequest;
    }) =>
      featureFlagsApi
        .upsertIdentityOverride(userIdentifier, teamIdentifier, flagName, data)
        .then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["feature-flags"] });
    },
  });
};

export const useDeleteIdentityOverride = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      userIdentifier,
      teamIdentifier,
      flagName,
    }: {
      userIdentifier: string;
      teamIdentifier: string;
      flagName: string;
    }) =>
      featureFlagsApi.deleteIdentityOverride(
        userIdentifier,
        teamIdentifier,
        flagName,
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["feature-flags"] });
    },
  });
};

export const useSegmentFeatureFlags = () => {
  return useQuery({
    queryKey: ["feature-flags", "segments"],
    queryFn: () => featureFlagsApi.getSegments().then((res) => res.data),
  });
};

export const useUpsertSegmentOverride = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      segmentId,
      flagName,
      data,
    }: {
      segmentId: number;
      flagName: string;
      data: UpdateFlagRequest;
    }) =>
      featureFlagsApi
        .upsertSegmentOverride(segmentId, flagName, data)
        .then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["feature-flags"] });
    },
  });
};

export const useDeleteSegmentOverride = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      segmentId,
      flagName,
    }: {
      segmentId: number;
      flagName: string;
    }) => featureFlagsApi.deleteSegmentOverride(segmentId, flagName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["feature-flags"] });
    },
  });
};
