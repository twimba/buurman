import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as usersApi from '../api/users';
import { useToast } from '../context/ToastContext';
import { useAuth } from '../contexts/AuthContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useCurrentUser = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ['userProfile'],
    queryFn: usersApi.getCurrentUser,
    enabled: isAuthenticated,
  });
};

export const useUpdateUserProfile = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.updateUserProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useVerifyPhone = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (code: string) => usersApi.verifyPhone(code),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
      showToast('Phone number verified successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useResendPhoneVerification = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.resendPhoneVerification,
    onSuccess: () => {
      showToast('Verification code sent', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useCancelPhoneVerification = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.cancelPhoneVerification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePhonePolicy = () => {
  return useQuery({
    queryKey: ['phonePolicy'],
    queryFn: usersApi.getPhonePolicy,
    staleTime: 10 * 60 * 1000,
  });
};

export const useUserPreferences = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ['userPreferences'],
    queryFn: usersApi.getUserPreferences,
    enabled: isAuthenticated,
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

export const useNotificationTypePreferences = () => {
  return useQuery({
    queryKey: ['notificationTypePreferences'],
    queryFn: usersApi.getNotificationTypePreferences,
  });
};

export const useUpdateNotificationTypePreferences = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.updateNotificationTypePreferences,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['notificationTypePreferences'],
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
