import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider } from '@buurman/ui';
import { useCancelSignatureRequest } from '../useSignatureRequestHooks';
import * as signaturesApi from '@/generated/api/signatures/signatures';
import { queryKeys } from '@/lib/queryKeys';

describe('useCancelSignatureRequest', () => {
  it("invalidates both the list and the specific request's detail query on success", async () => {
    vi.spyOn(signaturesApi, 'cancelSignatureRequest').mockResolvedValue(
      undefined as never
    );
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>
        <ToastProvider>{children}</ToastProvider>
      </QueryClientProvider>
    );

    const { result } = renderHook(
      () => useCancelSignatureRequest('con_1', 'doc_1'),
      { wrapper }
    );

    act(() => {
      result.current.mutate({ signatureRequestId: 'sgr_1', reason: 'test' });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: queryKeys.signatureRequests.all('con_1', 'doc_1'),
    });
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: queryKeys.signatureRequests.detail('con_1', 'doc_1', 'sgr_1'),
    });
  });
});
