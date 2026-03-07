import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as rentIncreasesApi from '../api/rentIncreases';
import type { ApplyRentIncreasesRequest } from '../types/rentIncrease';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['rentPeriods'] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      showToast('Rent increases applied successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
