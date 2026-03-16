import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { teamsApi } from "../api/teams";
import type { AsyncSelectOption } from "../components/AsyncSelect";

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

export const useTeamSearch = () => {
  return useCallback(async (query: string): Promise<AsyncSelectOption[]> => {
    const res = await teamsApi.list({ search: query, size: 20 });
    return res.data.content.map((team) => ({
      value: team.identifier,
      label: team.teamName,
      sublabel: team.identifier,
    }));
  }, []);
};
