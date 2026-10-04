import { useMutation, type UseMutationOptions } from '@tanstack/react-query';
import { useToast } from '@buurman/ui';

import { getErrorMessage, getProblemCode } from '../utils/errorMessages';

type ToastMutationOptions<TData, TVars> = Omit<
  UseMutationOptions<TData, unknown, TVars>,
  'onError'
> & {
  /** Toast shown on success (omit to show none). */
  successMessage?: string;
  /**
   * Translated toast text per ProblemDetail `code`; a response whose code is listed shows that
   * text instead of the server's detail. Other errors keep the default handling.
   */
  errorCodeMessages?: Record<string, string>;
};

/**
 * useMutation wrapper that shows an error toast (via getErrorMessage) and, optionally, a success
 * toast — so hooks don't each repeat the `useToast()` + `onError` boilerplate. The caller's
 * `onSuccess` (cache invalidation, analytics, navigation) still runs.
 */
export function useMutationWithToast<TData, TVars>(
  options: ToastMutationOptions<TData, TVars>
) {
  const { showToast } = useToast();
  const { successMessage, errorCodeMessages, onSuccess, ...rest } = options;
  return useMutation<TData, unknown, TVars>({
    ...rest,
    onSuccess: (...args) => {
      if (successMessage) {
        showToast(successMessage, 'success');
      }
      onSuccess?.(...args);
    },
    onError: (error) => {
      const code = getProblemCode(error);
      const mapped =
        code && errorCodeMessages && Object.hasOwn(errorCodeMessages, code)
          ? errorCodeMessages[code]
          : undefined;
      showToast(mapped ?? getErrorMessage(error), 'error');
    },
  });
}
