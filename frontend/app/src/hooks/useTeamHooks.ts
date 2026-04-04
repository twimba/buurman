import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as teamsApi from '../api/teams';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useCurrentTeam = () => {
  return useQuery({
    queryKey: queryKeys.teams.current(),
    queryFn: teamsApi.getCurrentTeam,
  });
};

export const useTeamMembers = (teamId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.members(teamId),
    queryFn: () => teamsApi.getTeamMembers(teamId ?? ''),
    enabled: !!teamId,
  });
};

export const useTeamPendingInvitations = (teamId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.pendingInvitations(teamId),
    queryFn: () => teamsApi.getTeamPendingInvitations(teamId ?? ''),
    enabled: !!teamId,
  });
};

export const useCreateInvitation = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: teamsApi.CreateInvitationRequest) =>
      teamsApi.createInvitation(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.pendingInvitations(teamId),
      });
      trackEvent(AnalyticsEvent.TEAM_MEMBER_INVITED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useResendInvitation = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (token: string) => teamsApi.resendInvitation(teamId, token),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.pendingInvitations(teamId),
      });
      showToast('Invitation resent successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useRemoveMember = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (memberId: string) => teamsApi.removeMember(teamId, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateMemberRole = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      memberId,
      data,
    }: {
      memberId: string;
      data: teamsApi.UpdateMemberRoleRequest;
    }) => teamsApi.updateMemberRole(teamId, memberId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useInvitation = (token: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.invitation(token),
    queryFn: () => teamsApi.getInvitation(token ?? ''),
    enabled: !!token,
    retry: false,
  });
};

export const usePendingInvitations = () => {
  return useQuery({
    queryKey: queryKeys.teams.pending(),
    queryFn: teamsApi.getPendingInvitations,
  });
};

export const useAcceptInvitation = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: teamsApi.acceptInvitation,
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
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useTransferOwnership = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (newOwnerId: string) =>
      teamsApi.transferOwnership(teamId, newOwnerId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.members(teamId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateTeam = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: teamsApi.UpdateTeamRequest) =>
      teamsApi.updateTeam(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useTeamSettings = (teamId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.teams.settings(teamId),
    queryFn: () => teamsApi.getTeamSettings(teamId ?? ''),
    enabled: !!teamId,
  });
};

export const useUpdateTeamSettings = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: teamsApi.UpdateTeamSettingsRequest) =>
      teamsApi.updateTeamSettings(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.teams.settings(teamId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
