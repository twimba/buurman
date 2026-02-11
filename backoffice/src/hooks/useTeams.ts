import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { teamsApi } from "../api/teams";

interface ListTeamsParams {
  page?: number;
  size?: number;
  search?: string;
}

export const useTeams = (params?: ListTeamsParams) => {
  return useQuery({
    queryKey: ["teams", params],
    queryFn: () => teamsApi.list(params).then((res) => res.data),
  });
};

export const useTeam = (identifier: string) => {
  return useQuery({
    queryKey: ["teams", identifier],
    queryFn: () => teamsApi.get(identifier).then((res) => res.data),
    enabled: !!identifier,
  });
};

export const useUpdateTeam = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: { name: string };
    }) => teamsApi.update(identifier, data).then((res) => res.data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["teams"] });
      queryClient.invalidateQueries({
        queryKey: ["teams", variables.identifier],
      });
    },
  });
};

export const useDeleteTeam = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (identifier: string) => teamsApi.delete(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teams"] });
    },
  });
};
