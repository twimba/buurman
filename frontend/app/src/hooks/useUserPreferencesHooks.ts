import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getUserProfile,
  updateUserProfile,
  getUserTeams,
  switchTeam,
  setDefaultTeam,
  leaveTeam,
  verifyPhone,
  resendPhoneVerification,
  cancelPhoneVerification,
  getPhonePolicy,
} from '../generated/api/users/users';
import {
  getPreferences,
  updatePreferences,
  getNotificationTypePreferences,
  updateNotificationTypePreferences,
} from '../generated/api/user-preferences/user-preferences';
import type {
  UpdateUserProfileRequest,
  UpdateUserPreferencesRequest,
  UpdateNotificationTypePreferencesRequest,
} from '../types/users';
import { useAuth } from '../context/AuthContext';
import { queryKeys } from '../lib/queryKeys';

export const useCurrentUser = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.profile(),
    queryFn: getUserProfile,
    enabled: isAuthenticated,
  });
};

export const useUpdateUserProfile = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateUserProfileRequest) => updateUserProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
    },
  });
};

export const useVerifyPhone = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Phone number verified successfully',
    mutationFn: (code: string) => verifyPhone({ code }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
    },
  });
};

export const useResendPhoneVerification = () => {
  return useMutationWithToast({
    successMessage: 'Verification code sent',
    mutationFn: () => resendPhoneVerification(),
  });
};

export const useCancelPhoneVerification = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: () => cancelPhoneVerification(),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
    },
  });
};

export const usePhonePolicy = () => {
  return useQuery({
    queryKey: queryKeys.userPreferences.phonePolicy(),
    queryFn: getPhonePolicy,
    staleTime: 10 * 60 * 1000,
  });
};

export const useUserPreferences = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.preferences(),
    queryFn: getPreferences,
    enabled: isAuthenticated,
  });
};

export const useUpdateUserPreferences = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateUserPreferencesRequest) => updatePreferences(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.preferences(),
      });
    },
  });
};

export const useNotificationTypePreferences = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.notificationTypePreferences(),
    queryFn: getNotificationTypePreferences,
    enabled: isAuthenticated,
  });
};

export const useUpdateNotificationTypePreferences = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateNotificationTypePreferencesRequest) =>
      updateNotificationTypePreferences(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.notificationTypePreferences(),
      });
    },
  });
};

export const useUserTeams = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.userPreferences.teams(),
    queryFn: getUserTeams,
    staleTime: 5 * 60 * 1000,
    enabled: isAuthenticated,
  });
};

export const useSwitchTeam = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (teamIdentifier: string) => switchTeam({ teamIdentifier }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.currentUser(),
      });
    },
  });
};

export const useSetDefaultTeam = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (teamIdentifier: string) => setDefaultTeam({ teamIdentifier }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
    },
  });
};

export const useLeaveTeam = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (teamIdentifier: string) => leaveTeam(teamIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.userPreferences.teams(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.current() });
    },
  });
};
