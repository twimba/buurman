import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as teamsApi from '../api/teams';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useCurrentTeam = () => {
  return useQuery({
    queryKey: ['currentTeam'],
    queryFn: teamsApi.getCurrentTeam,
  });
};

export const useTeamMembers = (teamId: string | undefined) => {
  return useQuery({
    queryKey: ['teamMembers', teamId],
    queryFn: () => teamsApi.getTeamMembers(teamId!),
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
      queryClient.invalidateQueries({ queryKey: ['teamMembers', teamId] });
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
      queryClient.invalidateQueries({ queryKey: ['teamMembers', teamId] });
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
      queryClient.invalidateQueries({ queryKey: ['teamMembers', teamId] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useInvitation = (token: string | undefined) => {
  return useQuery({
    queryKey: ['invitation', token],
    queryFn: () => teamsApi.getInvitation(token!),
    enabled: !!token,
    retry: false,
  });
};

export const useAcceptInvitation = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: teamsApi.acceptInvitation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
      queryClient.invalidateQueries({ queryKey: ['currentTeam'] });
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
      queryClient.invalidateQueries({ queryKey: ['teamMembers', teamId] });
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
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
      queryClient.invalidateQueries({ queryKey: ['currentTeam'] });
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useTeamSettings = (teamId: string | undefined) => {
  return useQuery({
    queryKey: ['teamSettings', teamId],
    queryFn: () => teamsApi.getTeamSettings(teamId!),
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
      queryClient.invalidateQueries({ queryKey: ['teamSettings', teamId] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
