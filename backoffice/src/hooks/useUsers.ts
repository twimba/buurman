import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { usersApi } from "../api/users";

interface ListUsersParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
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
