import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as usersApi from '../api/users';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useCurrentUser = () => {
  return useQuery({
    queryKey: ['currentUser'],
    queryFn: usersApi.getCurrentUser,
  });
};

export const useUpdateUserProfile = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.updateUserProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUserPreferences = (enabled: boolean = true) => {
  return useQuery({
    queryKey: ['userPreferences'],
    queryFn: usersApi.getUserPreferences,
    enabled,
  });
};

export const useUpdateUserPreferences = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.updateUserPreferences,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userPreferences'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useTeamNotificationPreferences = (teamId: string | undefined) => {
  return useQuery({
    queryKey: ['teamNotificationPreferences', teamId],
    queryFn: () => usersApi.getTeamNotificationPreferences(teamId!),
    enabled: !!teamId,
  });
};

export const useUpdateTeamNotificationPreferences = (teamId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: usersApi.UpdateTeamNotificationPreferencesRequest) =>
      usersApi.updateTeamNotificationPreferences(teamId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['teamNotificationPreferences', teamId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUserTeams = () => {
  return useQuery({
    queryKey: ['user-teams'],
    queryFn: usersApi.getUserTeams,
    staleTime: 5 * 60 * 1000,
  });
};

export const useSwitchTeam = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.switchTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
      queryClient.invalidateQueries({ queryKey: ['currentTeam'] });
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useSetDefaultTeam = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.setDefaultTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useLeaveTeam = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.leaveTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-teams'] });
      queryClient.invalidateQueries({ queryKey: ['currentTeam'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
