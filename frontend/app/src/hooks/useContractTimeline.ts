import { useQuery } from '@tanstack/react-query';
import { getContractTimeline } from '../generated/api/contracts/contracts';
import { queryKeys } from '../lib/queryKeys';

export const useContractTimeline = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.timeline(contractId),
    queryFn: () => getContractTimeline(contractId ?? ''),
    enabled: !!contractId,
  });
};
