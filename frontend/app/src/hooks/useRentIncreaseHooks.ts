import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as rentIncreasesApi from '../api/rentIncreases';
import type { ApplyRentIncreasesRequest } from '../types/rentIncrease';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useRentIncreasePreview = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (year: number) => rentIncreasesApi.previewRentIncreases(year),
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useApplyRentIncreases = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: ApplyRentIncreasesRequest) =>
      rentIncreasesApi.applyRentIncreases(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.rentPeriods.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      showToast('Rent adjustments applied successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
