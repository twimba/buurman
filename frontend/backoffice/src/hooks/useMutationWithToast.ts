import {
  useMutation,
  type UseMutationOptions,
} from '@tanstack/react-query';
import { useToast } from '@buurman/ui';

import { errorMessage } from '../utils/errorMessage';

type ToastMutationOptions<TData, TVars> = Omit<
  UseMutationOptions<TData, unknown, TVars>,
  'onError'
> & {
  /** Toast shown on success (omit to show none). */
  successMessage?: string;
  /** Prefix for the error toast, e.g. "Couldn't save flag" → "Couldn't save flag: <reason>". */
  errorTitle?: string;
};

/**
 * useMutation wrapper that surfaces a toast on error (and optionally on success), so hooks don't
 * each repeat the `useToast()` + `onError` boilerplate. The caller's `onSuccess` (cache
 * invalidation etc.) still runs.
 */
export function useMutationWithToast<TData, TVars>(
  options: ToastMutationOptions<TData, TVars>
) {
  const { showToast } = useToast();
  const { successMessage, errorTitle, onSuccess, ...rest } = options;
  return useMutation<TData, unknown, TVars>({
    ...rest,
    onSuccess: (...args) => {
      if (successMessage) {
        showToast(successMessage, 'success');
      }
      onSuccess?.(...args);
    },
    onError: (error) => {
      const reason = errorMessage(error);
      showToast(errorTitle ? `${errorTitle}: ${reason}` : reason, 'error');
    },
  });
}
