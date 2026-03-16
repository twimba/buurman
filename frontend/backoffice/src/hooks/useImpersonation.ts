import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { CreateImpersonationRequest } from "../generated/models";
import {
  createImpersonationSession,
  rejoinImpersonationSession,
  terminateImpersonationSession,
} from "../generated/api/backoffice-impersonation/backoffice-impersonation";
import { getUserFlags } from "../generated/api/backoffice-feature-flags/backoffice-feature-flags";
import {
  impersonationApi,
  type ListImpersonationSessionsParams,
} from "../api/impersonation";

export const useImpersonationSessions = (
  params?: ListImpersonationSessionsParams,
) => {
  return useQuery({
    queryKey: ["impersonation-sessions", params],
    queryFn: () =>
      impersonationApi.listSessions(params).then((res) => res.data),
  });
};

export const useCreateImpersonation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateImpersonationRequest) =>
      createImpersonationSession(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["impersonation-sessions"] });
    },
  });
};

export const useRejoinImpersonation = () => {
  return useMutation({
    mutationFn: ({
      identifier,
      password,
    }: {
      identifier: string;
      password: string;
    }) => rejoinImpersonationSession(identifier, { password }),
  });
};

export const useTerminateImpersonation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (identifier: string) =>
      terminateImpersonationSession(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["impersonation-sessions"] });
    },
  });
};

export const useUserTeams = (userIdentifier: string) => {
  return useQuery({
    queryKey: ["user-teams", userIdentifier],
    queryFn: () =>
      getUserFlags(userIdentifier).then((teams) =>
        teams.map((t) => ({
          teamIdentifier: t.teamIdentifier ?? "",
          teamName: t.teamName ?? "",
          role: t.role ?? "",
          isOwner: t.isOwner ?? false,
        })),
      ),
    enabled: !!userIdentifier,
  });
};
