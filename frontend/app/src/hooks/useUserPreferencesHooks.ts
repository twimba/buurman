import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as usersApi from '../api/users';
import { useToast } from '@buurman/ui';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useCurrentUser = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.profile(),
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
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
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
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
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
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePhonePolicy = () => {
  return useQuery({
    queryKey: queryKeys.userPreferences.phonePolicy(),
    queryFn: usersApi.getPhonePolicy,
    staleTime: 10 * 60 * 1000,
  });
};

export const useUserPreferences = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.preferences(),
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
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.preferences(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useNotificationTypePreferences = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.notificationTypePreferences(),
    queryFn: usersApi.getNotificationTypePreferences,
    enabled: isAuthenticated,
  });
};

export const useUpdateNotificationTypePreferences = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.updateNotificationTypePreferences,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.notificationTypePreferences(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUserTeams = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.teams(),
    queryFn: usersApi.getUserTeams,
    staleTime: 5 * 60 * 1000,
    enabled: isAuthenticated,
  });
};

export const useSwitchTeam = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: usersApi.switchTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
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
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
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
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
