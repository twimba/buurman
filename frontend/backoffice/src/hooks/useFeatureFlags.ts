import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getAdminStatus,
  getGlobalFlags,
  getUserFlags,
  updateGlobalFlag,
  upsertIdentityOverride,
  deleteIdentityOverride,
  getTeamFlags,
  upsertTeamOverride,
  deleteTeamOverride,
  getSegmentOverrides,
  upsertSegmentOverride,
  deleteSegmentOverride,
} from '../generated/api/backoffice-feature-flags/backoffice-feature-flags';
import type { UpdateFlagRequest } from '../types';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

export const useAdminStatus = () => {
  return useQuery({
    queryKey: ['feature-flags', 'admin-status'],
    queryFn: getAdminStatus,
    staleTime: 5 * 60 * 1000,
  });
};

export const useGlobalFeatureFlags = () => {
  return useQuery({
    queryKey: ['feature-flags', 'global'],
    queryFn: getGlobalFlags,
  });
};

export const useUserFeatureFlags = (userIdentifier: string | null) => {
  return useQuery({
    queryKey: ['feature-flags', 'user', userIdentifier],
    queryFn: () => getUserFlags(userIdentifier ?? ''),
    enabled: !!userIdentifier,
  });
};

export const useUpdateGlobalFlag = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      flagName,
      data,
    }: {
      flagName: string;
      data: UpdateFlagRequest;
    }) => updateGlobalFlag(flagName, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
      trackEvent(AnalyticsEvent.BO_FEATURE_FLAG_UPDATED);
    },
  });
};

export const useUpsertIdentityOverride = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
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
      upsertIdentityOverride(userIdentifier, teamIdentifier, flagName, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};

export const useDeleteIdentityOverride = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      userIdentifier,
      teamIdentifier,
      flagName,
    }: {
      userIdentifier: string;
      teamIdentifier: string;
      flagName: string;
    }) => deleteIdentityOverride(userIdentifier, teamIdentifier, flagName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};

export const useTeamFeatureFlags = (teamIdentifier: string | null) => {
  return useQuery({
    queryKey: ['feature-flags', 'team', teamIdentifier],
    queryFn: () => getTeamFlags(teamIdentifier ?? ''),
    enabled: !!teamIdentifier,
  });
};

export const useUpsertTeamOverride = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      teamIdentifier,
      flagName,
      data,
    }: {
      teamIdentifier: string;
      flagName: string;
      data: UpdateFlagRequest;
    }) => upsertTeamOverride(teamIdentifier, flagName, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};

export const useDeleteTeamOverride = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      teamIdentifier,
      flagName,
    }: {
      teamIdentifier: string;
      flagName: string;
    }) => deleteTeamOverride(teamIdentifier, flagName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};

export const useSegmentFeatureFlags = () => {
  return useQuery({
    queryKey: ['feature-flags', 'segments'],
    queryFn: () => getSegmentOverrides(),
  });
};

export const useUpsertSegmentOverride = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      segmentId,
      flagName,
      data,
    }: {
      segmentId: number;
      flagName: string;
      data: UpdateFlagRequest;
    }) => upsertSegmentOverride(segmentId, flagName, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};

export const useDeleteSegmentOverride = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      segmentId,
      flagName,
    }: {
      segmentId: number;
      flagName: string;
    }) => deleteSegmentOverride(segmentId, flagName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};
