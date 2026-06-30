import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listBuurmies,
  getBuurmy,
  createBuurmy,
  disableBuurmy,
  enableBuurmy,
  deleteBuurmy,
  forcePasswordUpdate,
  forceProfileUpdate,
  verifyBuurmy,
  unverifyBuurmy,
  removePasswordReset,
  removeProfileReset,
} from '../generated/api/backoffice-buurmies/backoffice-buurmies';
import type { ListBuurmiesParams } from '../generated/models';
import type { CreateBuurmyRequest } from '../types';

interface ListBuurmiesParamsInput {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
}

export const useBuurmies = (params?: ListBuurmiesParamsInput) => {
  return useQuery({
    queryKey: ['buurmies', params],
    queryFn: () => listBuurmies(params as ListBuurmiesParams),
  });
};

export const useBuurmy = (keycloakId: string) => {
  return useQuery({
    queryKey: ['buurmies', keycloakId],
    queryFn: () => getBuurmy(keycloakId),
    enabled: !!keycloakId,
  });
};

export const useCreateBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateBuurmyRequest) => createBuurmy(data),
    errorTitle: "Couldn't create buurmy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useDisableBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => disableBuurmy(keycloakId),
    errorTitle: "Couldn't disable buurmy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useEnableBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => enableBuurmy(keycloakId),
    errorTitle: "Couldn't enable buurmy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useDeleteBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => deleteBuurmy(keycloakId),
    errorTitle: "Couldn't delete buurmy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useForcePasswordUpdate = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => forcePasswordUpdate(keycloakId),
    errorTitle: "Couldn't force password update",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useForceProfileUpdate = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => forceProfileUpdate(keycloakId),
    errorTitle: "Couldn't force profile update",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useVerifyBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => verifyBuurmy(keycloakId),
    errorTitle: "Couldn't verify buurmy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useUnverifyBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => unverifyBuurmy(keycloakId),
    errorTitle: "Couldn't unverify buurmy",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useRemovePasswordReset = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => removePasswordReset(keycloakId),
    errorTitle: "Couldn't remove password reset",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};

export const useRemoveProfileReset = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (keycloakId: string) => removeProfileReset(keycloakId),
    errorTitle: "Couldn't remove profile reset",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['buurmies'] });
    },
  });
};
