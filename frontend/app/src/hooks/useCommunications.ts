import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { useMutationWithToast } from '@/hooks/useMutationWithToast';
import {
  getPaymentCommunications,
  getContractCommunications,
  resendCommunication,
} from '@/generated/api';

export const usePaymentCommunications = (identifier: string) =>
  useQuery({
    queryKey: ['payments', identifier, 'communications'],
    queryFn: () => getPaymentCommunications(identifier),
    enabled: Boolean(identifier),
  });

export const useContractCommunications = (identifier: string) =>
  useQuery({
    queryKey: ['contracts', identifier, 'communications'],
    queryFn: () => getContractCommunications(identifier),
    enabled: Boolean(identifier),
  });

/**
 * The backend gates this at TEAM_EDITOR; callers still hide the control for viewers so the
 * button is not offered and then refused.
 */
export const useResendCommunication = (invalidateKey: unknown[]) => {
  const queryClient = useQueryClient();
  const { t } = useTranslation('common');
  // Resending sends a real message and costs money, so silence is not an acceptable outcome:
  // a landlord with no feedback clicks again. Every other mutation hook in the app goes through
  // this wrapper for the same reason.
  return useMutationWithToast({
    successMessage: t('communications.resendSuccess'),
    mutationFn: (identifier: string) => resendCommunication(identifier),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: invalidateKey }),
  });
};
