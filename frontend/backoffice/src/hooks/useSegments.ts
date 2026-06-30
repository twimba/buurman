import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listSegments,
  getSegment,
  createSegment,
  updateSegment,
  deleteSegment,
  getSegmentMatches,
  getMatchingTeams,
  getMatchingUsers,
} from '../generated/api/backoffice-segments/backoffice-segments';
import type {
  CreateSegmentRequest,
  UpdateSegmentRequest,
} from '../generated/models';

export const useSegmentsList = () => {
  return useQuery({
    queryKey: ['segments'],
    queryFn: () => listSegments(),
  });
};

export const useSegmentDetail = (key: string | undefined) => {
  return useQuery({
    queryKey: ['segments', key],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => getSegment(key!),
    enabled: !!key,
  });
};

export const useSegmentMatches = (key: string | undefined) => {
  return useQuery({
    queryKey: ['segments', key, 'matches'],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => getSegmentMatches(key!),
    enabled: !!key,
  });
};

export const useSegmentMatchingTeams = (
  key: string | undefined,
  enabled: boolean
) => {
  return useQuery({
    queryKey: ['segments', key, 'matching-teams'],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => getMatchingTeams(key!),
    enabled: !!key && enabled,
  });
};

export const useSegmentMatchingUsers = (
  key: string | undefined,
  enabled: boolean
) => {
  return useQuery({
    queryKey: ['segments', key, 'matching-users'],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => getMatchingUsers(key!),
    enabled: !!key && enabled,
  });
};

export const useCreateSegment = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateSegmentRequest) => createSegment(data),
    errorTitle: "Couldn't create segment",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['segments'] });
    },
  });
};

export const useUpdateSegment = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({ key, data }: { key: string; data: UpdateSegmentRequest }) =>
      updateSegment(key, data),
    errorTitle: "Couldn't update segment",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['segments'] });
    },
  });
};

export const useDeleteSegment = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (key: string) => deleteSegment(key),
    errorTitle: "Couldn't delete segment",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['segments'] });
    },
  });
};
