import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getLeaseClauses,
  updateLeaseClauses,
  generateLeaseAgreement,
} from '../generated/api/lease-agreement/lease-agreement';
import type { UpdateContractLeaseClausesRequestClausesItem } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

/**
 * Resolved lease clauses for a contract — the country's default clause set combined with any
 * per-contract overrides, in sort order with each clause's current inclusion state.
 */
export const useLeaseClauses = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.leaseClauses(contractId),
    queryFn: () => getLeaseClauses(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useUpdateLeaseClauses = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Clause selection saved',
    mutationFn: (clauses: UpdateContractLeaseClausesRequestClausesItem[]) =>
      updateLeaseClauses(contractId, { clauses }),
    onSuccess: (data) => {
      queryClient.setQueryData(
        queryKeys.contracts.leaseClauses(contractId),
        data
      );
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.leaseClauses(contractId),
      });
    },
  });
};

/**
 * Renders and persists the lease agreement PDF. Invalidates the documents query — same target
 * useCreateSignatureRequest uses — so the newly generated document shows up in the Documents tab
 * without a manual refresh.
 */
export const useGenerateLeaseAgreement = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Lease agreement generated — find it in the Documents tab',
    mutationFn: () => generateLeaseAgreement(contractId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
    },
  });
};
