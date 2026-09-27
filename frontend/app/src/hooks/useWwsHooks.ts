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
import type { PropertyIdentifier, UnitIdentifier } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

// WWS points are a per-dwelling figure (BUUR-106): pre-fill must be read from the specific unit
// being priced, never from the property. Passing a PropertyIdentifier here is now a compile
// error — UnitIdentifier and PropertyIdentifier are branded (see orval.config.ts) precisely so
// this class of mistake can't compile silently again.
export const useWwsPreFill = (unitIdentifier: UnitIdentifier | undefined) => {
  return useQuery({
    queryKey: queryKeys.wws.preFill(unitIdentifier),
    queryFn: () => getWwsPreFill(unitIdentifier as UnitIdentifier),
    enabled: !!unitIdentifier,
  });
};

export const useWwsCalculations = (
  propertyIdentifier: PropertyIdentifier | undefined
) => {
  return useQuery({
    queryKey: queryKeys.wws.calculations(propertyIdentifier),
    queryFn: () => getWwsCalculations(propertyIdentifier as PropertyIdentifier),
    enabled: !!propertyIdentifier,
  });
};

export const useLatestWwsCalculation = (
  propertyIdentifier: PropertyIdentifier | undefined
) => {
  return useQuery({
    queryKey: queryKeys.wws.latest(propertyIdentifier),
    queryFn: () =>
      getLatestWwsCalculation(propertyIdentifier as PropertyIdentifier),
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

export const useDeleteWwsCalculation = (
  propertyIdentifier: PropertyIdentifier
) => {
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
