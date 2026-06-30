import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listTeams,
  getTeam,
  updateTeamName,
  deleteTeam,
} from '../generated/api/backoffice-teams/backoffice-teams';
import type { ListTeamsParams } from '../generated/models';
import type { AsyncSelectOption } from '../components/AsyncSelect';

export const useTeams = (params?: ListTeamsParams) => {
  return useQuery({
    queryKey: ['teams', params],
    queryFn: () => listTeams(params),
  });
};

export const useTeam = (identifier: string) => {
  return useQuery({
    queryKey: ['teams', identifier],
    queryFn: () => getTeam(identifier),
    enabled: !!identifier,
  });
};

export const useUpdateTeam = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: { name: string };
    }) => updateTeamName(identifier, data),
    errorTitle: "Couldn't update team",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({
        queryKey: ['teams', variables.identifier],
      });
    },
  });
};

export const useDeleteTeam = () => {
  const queryClient = useQueryClient();

  return useMutationWithToast({
    mutationFn: (identifier: string) => deleteTeam(identifier),
    errorTitle: "Couldn't delete team",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });
};

export const useTeamSearch = () => {
  return useCallback(async (query: string): Promise<AsyncSelectOption[]> => {
    const res = await listTeams({ search: query, size: 20 });
    return (res.content ?? []).map((team) => ({
      value: team.identifier,
      label: team.teamName,
      sublabel: team.identifier,
    }));
  }, []);
};
