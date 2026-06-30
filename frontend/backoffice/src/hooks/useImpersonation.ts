import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import type {
  CreateImpersonationRequest,
  ListImpersonationSessionsParams,
} from '../generated/models';
import {
  createImpersonationSession,
  listImpersonationSessions,
  rejoinImpersonationSession,
  terminateImpersonationSession,
} from '../generated/api/backoffice-impersonation/backoffice-impersonation';
import { getUserFlags } from '../generated/api/backoffice-feature-flags/backoffice-feature-flags';

interface SessionFilters {
  adminEmail?: string;
  targetUserEmail?: string;
  teamIdentifier?: string;
  status?: string;
  mode?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}

export const useImpersonationSessions = (params?: SessionFilters) => {
  return useQuery({
    queryKey: ['impersonation-sessions', params],
    queryFn: () =>
      listImpersonationSessions(params as ListImpersonationSessionsParams),
  });
};

export const useCreateImpersonation = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: (data: CreateImpersonationRequest) =>
      createImpersonationSession(data),
    errorTitle: "Couldn't start impersonation",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['impersonation-sessions'] });
    },
  });
};

export const useRejoinImpersonation = () => {
  return useMutationWithToast({
    mutationFn: ({
      identifier,
      password,
    }: {
      identifier: string;
      password: string;
    }) => rejoinImpersonationSession(identifier, { password }),
    errorTitle: "Couldn't rejoin session",
  });
};

export const useTerminateImpersonation = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: (identifier: string) =>
      terminateImpersonationSession(identifier),
    errorTitle: "Couldn't terminate session",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['impersonation-sessions'] });
    },
  });
};

export const useUserTeams = (userIdentifier: string) => {
  return useQuery({
    queryKey: ['user-teams', userIdentifier],
    queryFn: () =>
      getUserFlags(userIdentifier).then((teams) =>
        teams.map((t) => ({
          teamIdentifier: t.teamIdentifier ?? '',
          teamName: t.teamName ?? '',
          role: t.role ?? '',
          isOwner: t.isOwner ?? false,
        }))
      ),
    enabled: !!userIdentifier,
  });
};
