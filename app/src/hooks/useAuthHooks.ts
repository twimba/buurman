import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as authApi from '../api/auth';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useCurrentUser = (enabled = true) => {
  return useQuery({
    queryKey: ['currentUser'],
    queryFn: authApi.getCurrentUser,
    retry: false,
    enabled,
  });
};

export const useRegister = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: authApi.register,
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateProfile = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: authApi.updateProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useVerifyEmail = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: authApi.verifyEmail,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
    },
  });
};

export const useResendVerification = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: authApi.resendVerificationCode,
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
    queryKey: ['registrationConfig'],
    queryFn: authApi.getRegistrationConfig,
    staleTime: 30_000,
  });
};

export const useValidateInvitationCode = () => {
  return useMutation({
    mutationFn: authApi.validateInvitationCode,
  });
};
