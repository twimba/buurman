import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  updatePropertyAllocation,
  getExpenseAllocations,
} from '../generated/api/expense-allocations/expense-allocations';
import type { UpdateAllocationRequest } from '../types/allocation';
import { queryKeys } from '../lib/queryKeys';

/**
 * Reads the stored, backend-computed per-unit split of a building-level expense.
 * This is the source of truth for what a tenant's service-charge statement will
 * show — never recompute or re-round it on the frontend.
 */
export const useExpenseAllocations = (expenseIdentifier: string | undefined) =>
  useQuery({
    queryKey: queryKeys.units.allocation(expenseIdentifier),
    queryFn: () => getExpenseAllocations(expenseIdentifier ?? ''),
    enabled: !!expenseIdentifier,
  });

/**
 * Sets a property's allocation basis (and, for CUSTOM, every unit's share).
 * Does not retroactively rewrite any expense's already-persisted allocations,
 * so only the property/units caches need invalidating — not every expense's.
 */
export const useUpdatePropertyAllocation = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Allocation settings saved',
    mutationFn: (data: UpdateAllocationRequest) =>
      updatePropertyAllocation(propertyIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.all(propertyIdentifier),
      });
    },
  });
};
