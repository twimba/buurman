import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getCurrentTeam,
  getTeamMembers,
  getTeamPendingInvitations,
  createInvitation,
  resendInvitation,
  removeMember,
  updateMemberRole,
  transferOwnership,
  updateTeam,
  getTeamSettings,
  updateTeamSettings,
} from '../generated/api/teams/teams';
import {
  getInvitation,
  getPendingInvitations,
  acceptInvitation,
} from '../generated/api/invitations/invitations';
import type {
  CreateInvitationRequest,
  UpdateMemberRoleRequest,
  UpdateTeamRequest,
  UpdateTeamSettingsRequest,
} from '../generated/models';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useCurrentTeam = () => {
  return useQuery({
    queryKey: queryKeys.teams.current(),
    queryFn: () => getCurrentTeam(),
  });
};

export const useTeamMembers = (teamId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.members(teamId),
    queryFn: () => getTeamMembers(teamId ?? ''),
    enabled: !!teamId,
  });
};

export const useTeamPendingInvitations = (teamId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.pendingInvitations(teamId),
    queryFn: () => getTeamPendingInvitations(teamId ?? ''),
    enabled: !!teamId,
  });
};

export const useCreateInvitation = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateInvitationRequest) =>
      createInvitation(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.pendingInvitations(teamId),
      });
      trackEvent(AnalyticsEvent.TEAM_MEMBER_INVITED);
    },
  });
};

export const useResendInvitation = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Invitation resent successfully',
    mutationFn: (token: string) => resendInvitation(teamId, token),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.pendingInvitations(teamId),
      });
    },
  });
};

export const useRemoveMember = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (memberId: string) => removeMember(teamId, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
    },
  });
};

export const useUpdateMemberRole = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      memberId,
      data,
    }: {
      memberId: string;
      data: UpdateMemberRoleRequest;
    }) => updateMemberRole(teamId, memberId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
    },
  });
};

export const useInvitation = (token: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.invitation(token),
    queryFn: () => getInvitation(token ?? ''),
    enabled: !!token,
    retry: false,
  });
};

export const usePendingInvitations = () => {
  return useQuery({
    queryKey: queryKeys.teams.pending(),
    queryFn: () => getPendingInvitations(),
  });
};

export const useAcceptInvitation = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (token: string) => acceptInvitation(token),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.pending() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
    },
  });
};

export const useTransferOwnership = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (newOwnerId: string) =>
      transferOwnership(teamId, { newOwnerIdentifier: newOwnerId }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
    },
  });
};

export const useUpdateTeam = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateTeamRequest) => updateTeam(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
    },
  });
};

export const useTeamSettings = (teamId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.settings(teamId),
    queryFn: () => getTeamSettings(teamId ?? ''),
    enabled: !!teamId,
  });
};

export const useUpdateTeamSettings = (teamId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateTeamSettingsRequest) =>
      updateTeamSettings(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.settings(teamId),
      });
    },
  });
};
