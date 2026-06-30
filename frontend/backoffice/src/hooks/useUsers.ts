import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listUsers,
  getUser,
  disableUser,
  enableUser,
  resetPassword,
} from '../generated/api/backoffice-users/backoffice-users';
import type { ListUsersParams } from '../generated/models';
import type { AsyncSelectOption } from '../components/AsyncSelect';

interface ListUsersParamsInput {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
  team?: string;
}

export const useUsers = (params?: ListUsersParamsInput) => {
  return useQuery({
    queryKey: ['users', params],
    queryFn: () => listUsers(params as ListUsersParams),
  });
};

export const useUser = (identifier: string) => {
  return useQuery({
    queryKey: ['users', identifier],
    queryFn: () => getUser(identifier),
    enabled: !!identifier,
  });
};

export const useDisableUser = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: (identifier: string) => disableUser(identifier),
    errorTitle: "Couldn't disable user",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
};

export const useEnableUser = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: (identifier: string) => enableUser(identifier),
    errorTitle: "Couldn't enable user",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
};

export const useResetPassword = () => {
  return useMutationWithToast({
    mutationFn: (identifier: string) => resetPassword(identifier),
    errorTitle: "Couldn't reset password",
  });
};

export const useUserSearch = () => {
  return useCallback(async (query: string): Promise<AsyncSelectOption[]> => {
    const res = await listUsers({ search: query, size: 20 });
    return (res.content ?? []).map((user) => ({
      value: user.identifier,
      label: user.email,
      sublabel:
        `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || undefined,
    }));
  }, []);
};
