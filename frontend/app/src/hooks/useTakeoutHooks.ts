import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  requestTakeout,
  listTakeouts,
  getTakeout,
  deleteTakeout,
  TakeoutResponse,
} from '@/api/takeouts';
import { PageResponse } from '@/types/common';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '@/utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useTakeouts = () => {
  return useQuery({
    queryKey: queryKeys.takeouts.list(),
    queryFn: () => listTakeouts({ size: 20 }),
    refetchInterval: (query) => {
      const data = query.state.data;
      const hasActive = data?.content?.some(
        (t) => t.status === 'PENDING' || t.status === 'PROCESSING'
      );
      return hasActive ? 3_000 : 30_000;
    },
  });
};

export const useTakeout = (identifier: string) => {
  return useQuery({
    queryKey: queryKeys.takeouts.detail(identifier),
    queryFn: () => getTakeout(identifier),
    enabled: !!identifier,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === 'PENDING' || data?.status === 'PROCESSING') {
        return 3_000;
      }
      return false;
    },
  });
};

export const useRequestTakeout = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: requestTakeout,
    onSuccess: (newTakeout) => {
      queryClient.setQueryData<PageResponse<TakeoutResponse> | undefined>(
        queryKeys.takeouts.list(),
        (old) => {
          if (!old) {
            return undefined;
          }
          return { ...old, content: [newTakeout, ...old.content] };
        }
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.takeouts.root });
      showToast(
        'Data export requested. This may take a few minutes.',
        'success'
      );
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteTakeout = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: deleteTakeout,
    onSuccess: (_data, identifier) => {
      queryClient.setQueryData<PageResponse<TakeoutResponse> | undefined>(
        queryKeys.takeouts.list(),
        (old) => {
          if (!old) {
            return old;
          }
          return {
            ...old,
            content: old.content.filter((t) => t.identifier !== identifier),
          };
        }
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.takeouts.root });
      showToast('Data export deleted.', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
