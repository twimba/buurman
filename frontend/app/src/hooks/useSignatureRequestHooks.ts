import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  cancelSignatureRequest,
  createSignatureRequest,
  getSignatureRequest,
  getSigningLinks,
  listSignatureRequests,
} from '../generated/api/signatures/signatures';
import type { SignatureRequestResponse } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

const TERMINAL_STATUSES: SignatureRequestResponse['status'][] = [
  'COMPLETED',
  'DECLINED',
  'CANCELLED',
  'FAILED',
];

/**
 * Every signature request ever raised for a document, newest first. Used on mount so a page
 * reload cannot make the UI forget an in-flight request and offer to send a duplicate.
 */
export const useSignatureRequests = (
  contractId: string | undefined,
  documentId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.signatureRequests.all(contractId, documentId),
    queryFn: () => listSignatureRequests(contractId ?? '', documentId ?? ''),
    enabled: !!contractId && !!documentId,
  });
};

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
    onSuccess: (_data, documentId) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.signatureRequests.all(contractId, documentId),
      });
    },
  });
};

export const useCancelSignatureRequest = (
  contractId: string,
  documentId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Signature request retracted',
    mutationFn: ({
      signatureRequestId,
      reason,
    }: {
      signatureRequestId: string;
      reason?: string;
    }) =>
      cancelSignatureRequest(
        contractId,
        documentId,
        signatureRequestId,
        reason ? { reason } : undefined
      ),
    onSuccess: (_data, { signatureRequestId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.signatureRequests.all(contractId, documentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.signatureRequests.detail(
          contractId,
          documentId,
          signatureRequestId
        ),
      });
    },
  });
};

const SIGNING_LINKS_POLL_MS = 15_000;

/**
 * Per-signer signing links of an in-flight request. The links are bearer credentials, so the
 * query is never cached (gcTime 0), only runs while the sheet showing it is open, and stops
 * polling once it fails (a 409/403 will not fix itself; the user retries explicitly).
 */
export const useSignatureSigningLinks = (
  contractId: string | undefined,
  documentId: string | undefined,
  signatureRequestId: string | undefined,
  { enabled }: { enabled: boolean }
) => {
  return useQuery({
    queryKey: queryKeys.signatureRequests.signingLinks(
      contractId,
      documentId,
      signatureRequestId
    ),
    queryFn: () =>
      getSigningLinks(
        contractId ?? '',
        documentId ?? '',
        signatureRequestId ?? ''
      ),
    enabled: enabled && !!contractId && !!documentId && !!signatureRequestId,
    gcTime: 0,
    staleTime: 0,
    retry: false,
    refetchInterval: (query) =>
      query.state.status === 'error' ? false : SIGNING_LINKS_POLL_MS,
  });
};
