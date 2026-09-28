import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { useMutationWithToast } from '@/hooks/useMutationWithToast';
import {
  getPaymentCommunications,
  getContractCommunications,
  getPaymentCommunicationBody,
  getContractCommunicationBody,
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

export type CommunicationEntity = 'payment' | 'contract';

/**
 * The stored message of one communication, fetched only when its preview opens.
 *
 * The key nests under the timeline's own key on purpose: both pages already pass
 * ['payments', id, 'communications'] as the resend invalidation key, and React Query invalidates
 * by prefix, so a resend drops cached bodies with no extra wiring.
 */
export const useCommunicationBody = (
  entity: CommunicationEntity,
  entityIdentifier: string,
  communicationIdentifier: string | undefined
) =>
  useQuery({
    queryKey: [
      entity === 'payment' ? 'payments' : 'contracts',
      entityIdentifier,
      'communications',
      communicationIdentifier,
      'body',
    ],
    queryFn: () =>
      entity === 'payment'
        ? getPaymentCommunicationBody(
            entityIdentifier,
            communicationIdentifier as string
          )
        : getContractCommunicationBody(
            entityIdentifier,
            communicationIdentifier as string
          ),
    enabled: Boolean(entityIdentifier && communicationIdentifier),
    // A sent message is immutable. Refetching it on every window focus is pure waste, and
    // reopening the same preview being instant is a good part of how this surface feels.
    staleTime: Infinity,
  });
