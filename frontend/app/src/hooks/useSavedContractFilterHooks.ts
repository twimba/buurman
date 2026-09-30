import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getSavedContractFilters,
  createSavedContractFilter,
  deleteSavedContractFilter,
} from '../generated/api/saved-contract-filters/saved-contract-filters';
import type { CreateSavedContractFilterRequest } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

export const useSavedContractFilters = () => {
  return useQuery({
    queryKey: queryKeys.savedContractFilters.all(),
    queryFn: () => getSavedContractFilters(),
  });
};

export const useCreateSavedContractFilter = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Filter saved',
    mutationFn: (request: CreateSavedContractFilterRequest) =>
      createSavedContractFilter(request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.savedContractFilters.all(),
      });
    },
  });
};

export const useDeleteSavedContractFilter = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Filter deleted',
    mutationFn: (identifier: string) => deleteSavedContractFilter(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.savedContractFilters.all(),
      });
    },
  });
};
