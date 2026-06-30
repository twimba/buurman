import { useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  previewRentIncreases,
  applyRentIncreases,
} from '../generated/api/rent-increases/rent-increases';
import type { ApplyRentIncreasesRequest } from '../types/rentIncrease';
import { queryKeys } from '../lib/queryKeys';

export const useRentIncreasePreview = () => {
  return useMutationWithToast({
    mutationFn: (year: number) => previewRentIncreases({ year }),
  });
};

export const useApplyRentIncreases = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Rent adjustments applied successfully',
    mutationFn: (data: ApplyRentIncreasesRequest) => applyRentIncreases(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.rentPeriods.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
    },
  });
};
