import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  calculateWws,
  calculateAndSaveWws,
  getWwsPreFill,
  getWwsCalculations,
  getLatestWwsCalculation,
  deleteWwsCalculation,
} from '../generated/api/wws-calculator/wws-calculator';
import type { WwsCalculationRequest } from '../types/wws';
import { queryKeys } from '../lib/queryKeys';

export const useWwsPreFill = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.wws.preFill(propertyIdentifier),
    queryFn: () => getWwsPreFill(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useWwsCalculations = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.wws.calculations(propertyIdentifier),
    queryFn: () => getWwsCalculations(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useLatestWwsCalculation = (
  propertyIdentifier: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.wws.latest(propertyIdentifier),
    queryFn: () => getLatestWwsCalculation(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
    retry: false,
  });
};

export const useCalculateWws = () => {
  return useMutationWithToast({
    mutationFn: (data: WwsCalculationRequest) => calculateWws(data),
  });
};

export const useCalculateAndSaveWws = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'WWS calculation saved',
    mutationFn: (data: WwsCalculationRequest) => calculateAndSaveWws(data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.wws.calculations(variables.propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.wws.latest(variables.propertyIdentifier),
      });
    },
  });
};

export const useDeleteWwsCalculation = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Calculation deleted',
    mutationFn: (calculationIdentifier: string) =>
      deleteWwsCalculation(calculationIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.wws.calculations(propertyIdentifier),
      });
      queryClient.removeQueries({
        queryKey: queryKeys.wws.latest(propertyIdentifier),
      });
    },
  });
};
