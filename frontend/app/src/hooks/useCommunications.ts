import { useQuery } from '@tanstack/react-query';
import { getPaymentCommunications, getContractCommunications } from '@/generated/api';

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
