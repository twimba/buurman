import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
  return useMutation({
    mutationFn: (identifier: string) => resendCommunication(identifier),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: invalidateKey }),
  });
};
