import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  createSignatureRequest,
  getSignatureRequest,
} from '../generated/api/signatures/signatures';
import type { SignatureRequestResponse } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

const TERMINAL_STATUSES: SignatureRequestResponse['status'][] = [
  'COMPLETED',
  'DECLINED',
  'CANCELLED',
  'FAILED',
];

export const useSignatureRequest = (
  contractId: string | undefined,
  documentId: string | undefined,
  signatureRequestId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.signatureRequests.detail(
      contractId,
      documentId,
      signatureRequestId
    ),
    queryFn: () =>
      getSignatureRequest(
        contractId ?? '',
        documentId ?? '',
        signatureRequestId ?? ''
      ),
    enabled: !!contractId && !!documentId && !!signatureRequestId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && TERMINAL_STATUSES.includes(status) ? false : 3000;
    },
  });
};

export const useCreateSignatureRequest = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Sent for signature',
    mutationFn: (documentId: string) =>
      createSignatureRequest(contractId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
    },
  });
};
