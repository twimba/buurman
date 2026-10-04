import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  terminateContract,
  previewContractTermination,
} from '../generated/api/contracts/contracts';
import type {
  TerminateContractRequest,
  ContractTerminationResponse,
  TerminationPreviewResponse,
} from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

// The OpenAPI spec declares `givenBy` as an inline enum on three separate schemas
// (TerminateContractRequest, the termination-preview query params, and
// ContractTerminationResponse), so Orval generates three distinct — but structurally
// identical — `'LANDLORD' | 'TENANT'` types. This local alias lets the wizard pass one
// value around without caring which generated type a given call site expects.
export type TerminationGivenBy = 'LANDLORD' | 'TENANT';

/**
 * Read-only preview of the notice period the `terminate` endpoint would compute, for the
 * wizard's review step. Pure `useQuery` — no mutation, no side effects — so it can be called
 * as often as the landlord edits the notice date without ever recording anything.
 */
export const useTerminationPreview = (
  contractId: string | undefined,
  givenBy: TerminationGivenBy | undefined,
  noticeDate: string | undefined
) => {
  return useQuery<TerminationPreviewResponse>({
    queryKey: queryKeys.contracts.terminationPreview(
      contractId,
      givenBy,
      noticeDate
    ),
    queryFn: () =>
      previewContractTermination(contractId ?? '', {
        givenBy: givenBy ?? 'LANDLORD',
        noticeDate: noticeDate ?? '',
      }),
    enabled: !!contractId && !!givenBy && !!noticeDate,
  });
};

/**
 * Records the termination. A separate, explicit call from `useTerminationPreview` above —
 * made once, only when the landlord confirms on the final step.
 */
export const useTerminateContract = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast<
    ContractTerminationResponse,
    TerminateContractRequest
  >({
    successMessage: 'Contract termination recorded',
    mutationFn: (request) => terminateContract(contractId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      // The notice letter is filed as a contract document, the deposit's return-due date moves
      // to the new effective end date, and the status change lands on the timeline/audit log.
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.deposit(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.timeline(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
  });
};
