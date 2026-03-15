import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { CreateImpersonationRequest } from "../generated/models";
import {
  createImpersonationSession,
  listImpersonationSessions,
  rejoinImpersonationSession,
  terminateImpersonationSession,
} from "../generated/api/backoffice-impersonation/backoffice-impersonation";
import { getUserFlags } from "../generated/api/backoffice-feature-flags/backoffice-feature-flags";

export const useImpersonationSessions = () => {
  return useQuery({
    queryKey: ["impersonation-sessions"],
    queryFn: () => listImpersonationSessions().then((res) => res.content ?? []),
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
    mutationFn: (identifier: string) => rejoinImpersonationSession(identifier),
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
