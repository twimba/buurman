import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getCurrentUser,
  register,
  updateProfile,
  verifyEmail,
  verifyEmailByToken,
  resendVerification,
} from '../generated/api/authentication/authentication';
import {
  getConfig,
  validate,
} from '../generated/api/registration/registration';
import type { RegisterRequest, UpdateProfileRequest } from '../types/auth';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useCurrentUser = (enabled = true) => {
  return useQuery({
    queryKey: queryKeys.auth.currentUser(),
    queryFn: getCurrentUser,
    retry: false,
    enabled,
  });
};

export const useRegister = () => {
  return useMutationWithToast({
    mutationFn: (data: RegisterRequest) => register(data),
  });
};

export const useUpdateProfile = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateProfileRequest) => updateProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.currentUser() });
    },
  });
};

export const useVerifyEmail = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (code: string) => verifyEmail({ code }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.currentUser() });
    },
  });
};

export const useVerifyEmailByToken = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (token: string) => verifyEmailByToken({ token }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.currentUser() });
    },
  });
};

export const useResendVerification = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => resendVerification(),
    onSuccess: () => {
      showToast('Verification code sent to your email', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useRegistrationConfig = () => {
  return useQuery({
    queryKey: queryKeys.auth.registrationConfig(),
    queryFn: getConfig,
    staleTime: 30_000,
  });
};

export const useValidateInvitationCode = () => {
  return useMutationWithToast({
    mutationFn: (code: string) => validate({ code }),
  });
};
