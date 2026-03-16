import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { usersApi } from "../api/users";
import type { AsyncSelectOption } from "../components/AsyncSelect";

interface ListUsersParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
  team?: string;
}

export const useUsers = (params?: ListUsersParams) => {
  return useQuery({
    queryKey: ["users", params],
    queryFn: () => usersApi.list(params).then((res) => res.data),
  });
};

export const useUser = (identifier: string) => {
  return useQuery({
    queryKey: ["users", identifier],
    queryFn: () => usersApi.get(identifier).then((res) => res.data),
    enabled: !!identifier,
  });
};

export const useDisableUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (identifier: string) => usersApi.disable(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
};

export const useEnableUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (identifier: string) => usersApi.enable(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
};

export const useResetPassword = () => {
  return useMutation({
    mutationFn: (identifier: string) => usersApi.resetPassword(identifier),
  });
};

export const useUserSearch = () => {
  return useCallback(async (query: string): Promise<AsyncSelectOption[]> => {
    const res = await usersApi.list({ search: query, size: 20 });
    return res.data.content.map((user) => ({
      value: user.identifier,
      label: user.email,
      sublabel:
        `${user.firstName ?? ""} ${user.lastName ?? ""}`.trim() || undefined,
    }));
  }, []);
};
