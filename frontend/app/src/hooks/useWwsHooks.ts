import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as wwsApi from '../api/wws';
import type { WwsCalculationRequest } from '../types/wws';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useWwsPreFill = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.wws.preFill(propertyIdentifier),
    queryFn: () => wwsApi.getWwsPreFill(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useWwsCalculations = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.wws.calculations(propertyIdentifier),
    queryFn: () => wwsApi.getWwsCalculations(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useLatestWwsCalculation = (
  propertyIdentifier: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.wws.latest(propertyIdentifier),
    queryFn: () => wwsApi.getLatestWwsCalculation(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
    retry: false,
  });
};

export const useCalculateWws = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: WwsCalculationRequest) => wwsApi.calculateWws(data),
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useCalculateAndSaveWws = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: WwsCalculationRequest) =>
      wwsApi.calculateAndSaveWws(data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.wws.calculations(variables.propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.wws.latest(variables.propertyIdentifier),
      });
      showToast('WWS calculation saved', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteWwsCalculation = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (calculationIdentifier: string) =>
      wwsApi.deleteWwsCalculation(calculationIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.wws.calculations(propertyIdentifier),
      });
      queryClient.removeQueries({
        queryKey: queryKeys.wws.latest(propertyIdentifier),
      });
      showToast('Calculation deleted', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
