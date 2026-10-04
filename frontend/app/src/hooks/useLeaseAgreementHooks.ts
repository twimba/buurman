import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getLeaseClauses,
  updateLeaseClauses,
  generateLeaseAgreement,
} from '../generated/api/lease-agreement/lease-agreement';
import type {
  GenerateLeaseAgreementLang,
  LeaseClausesResponse,
  UpdateContractLeaseClausesRequestClausesItem,
} from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

/**
 * Lease clause envelope for a contract: the availability state for its country plus the resolved
 * clauses (the country's default set combined with per-contract overrides, in sort order with
 * each clause's inclusion state). Clauses are empty when the lease is unavailable.
 */
export const useLeaseClauses = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.leaseClauses(contractId),
    queryFn: (): Promise<LeaseClausesResponse> =>
      getLeaseClauses(contractId ?? ''),
    enabled: !!contractId,
  });
};

/** Translated toast text for the 409 problem codes the lease endpoints can return. */
const useLeaseErrorCodeMessages = (): Record<string, string> => {
  const { t } = useTranslation('contracts');
  return {
    LEASE_NOT_AVAILABLE_FOR_COUNTRY: t(
      'leaseAgreement.errors.notAvailableForCountry'
    ),
    LEASE_CONTRACT_HAS_NO_COUNTRY: t('leaseAgreement.errors.noCountry'),
  };
};

export const useUpdateLeaseClauses = (contractId: string) => {
  const queryClient = useQueryClient();
  const errorCodeMessages = useLeaseErrorCodeMessages();
  return useMutationWithToast({
    errorCodeMessages,
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
 * Renders and persists the lease agreement PDF in the given language (omitted: the server uses
 * the contract's document language). Invalidates the documents query — same target
 * useCreateSignatureRequest uses — so the newly generated document shows up in the Documents tab
 * without a manual refresh.
 */
export const useGenerateLeaseAgreement = (contractId: string) => {
  const queryClient = useQueryClient();
  const errorCodeMessages = useLeaseErrorCodeMessages();
  return useMutationWithToast({
    errorCodeMessages,
    successMessage: 'Lease agreement generated — find it in the Documents tab',
    mutationFn: (lang?: GenerateLeaseAgreementLang) =>
      generateLeaseAgreement(contractId, lang ? { lang } : undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
    },
  });
};
