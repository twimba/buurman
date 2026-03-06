import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as wwsApi from '../api/wws';
import type { WwsCalculationRequest } from '../types/wws';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useWwsPreFill = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: ['wwsPreFill', propertyIdentifier],
    queryFn: () => wwsApi.getWwsPreFill(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useWwsCalculations = (
  propertyIdentifier: string | undefined
) => {
  return useQuery({
    queryKey: ['wwsCalculations', propertyIdentifier],
    queryFn: () => wwsApi.getWwsCalculations(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useLatestWwsCalculation = (
  propertyIdentifier: string | undefined
) => {
  return useQuery({
    queryKey: ['wwsLatest', propertyIdentifier],
    queryFn: () => wwsApi.getLatestWwsCalculation(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
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
        queryKey: ['wwsCalculations', variables.propertyIdentifier],
      });
      queryClient.invalidateQueries({
        queryKey: ['wwsLatest', variables.propertyIdentifier],
      });
      showToast('WWS calculation saved', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
