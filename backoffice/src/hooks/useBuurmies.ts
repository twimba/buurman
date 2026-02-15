import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { buurmiesApi } from "../api/buurmies";
import type { CreateBuurmyRequest } from "../types";

interface ListBuurmiesParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
}

export const useBuurmies = (params?: ListBuurmiesParams) => {
  return useQuery({
    queryKey: ["buurmies", params],
    queryFn: () => buurmiesApi.list(params).then((res) => res.data),
  });
};

export const useBuurmy = (keycloakId: string) => {
  return useQuery({
    queryKey: ["buurmies", keycloakId],
    queryFn: () => buurmiesApi.get(keycloakId).then((res) => res.data),
    enabled: !!keycloakId,
  });
};

export const useCreateBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateBuurmyRequest) =>
      buurmiesApi.create(data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buurmies"] });
    },
  });
};

export const useDisableBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (keycloakId: string) => buurmiesApi.disable(keycloakId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buurmies"] });
    },
  });
};

export const useEnableBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (keycloakId: string) => buurmiesApi.enable(keycloakId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buurmies"] });
    },
  });
};

export const useDeleteBuurmy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (keycloakId: string) => buurmiesApi.delete(keycloakId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buurmies"] });
    },
  });
};

export const useForcePasswordUpdate = () => {
  return useMutation({
    mutationFn: (keycloakId: string) =>
      buurmiesApi.forcePasswordUpdate(keycloakId),
  });
};

export const useForceProfileUpdate = () => {
  return useMutation({
    mutationFn: (keycloakId: string) =>
      buurmiesApi.forceProfileUpdate(keycloakId),
  });
};
