import { act, renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useMutationWithToast } from '../useMutationWithToast';

const showToast = vi.fn();
vi.mock('@buurman/ui', () => ({ useToast: () => ({ showToast }) }));

const problem = (code: string) =>
  new AxiosError('failed', 'ERR_BAD_REQUEST', undefined, undefined, {
    status: 400,
    data: { code, detail: 'server detail' },
    statusText: '',
    headers: {},
    config: {} as never,
  });

const run = async (code: string) => {
  const queryClient = new QueryClient();
  const { result } = renderHook(
    () =>
      useMutationWithToast<void, void>({
        mutationFn: () => Promise.reject(problem(code)),
        errorCodeMessages: { KNOWN: 'Translated text' },
      }),
    {
      wrapper: ({ children }) => (
        <QueryClientProvider client={queryClient}>
          {children}
        </QueryClientProvider>
      ),
    }
  );
  act(() => result.current.mutate());
  await waitFor(() => expect(showToast).toHaveBeenCalled());
};

describe('useMutationWithToast errorCodeMessages', () => {
  beforeEach(() => showToast.mockClear());

  it('shows the mapped text for a listed code', async () => {
    await run('KNOWN');
    expect(showToast).toHaveBeenCalledWith('Translated text', 'error');
  });

  it('falls back to the default handling for an inherited property name', async () => {
    await run('constructor');
    expect(showToast).toHaveBeenCalledTimes(1);
    expect(showToast.mock.calls[0][0]).not.toEqual(expect.any(Function));
    expect(typeof showToast.mock.calls[0][0]).toBe('string');
  });
});

describe('useMutationWithToast successMessage', () => {
  beforeEach(() => showToast.mockClear());

  const runSuccess = async (silent: boolean) => {
    const queryClient = new QueryClient();
    const onSuccess = vi.fn();
    const { result } = renderHook(
      () =>
        useMutationWithToast<void, { silent: boolean }>({
          mutationFn: () => Promise.resolve(),
          successMessage: (vars) => (vars.silent ? undefined : 'Saved'),
          onSuccess,
        }),
      {
        wrapper: ({ children }) => (
          <QueryClientProvider client={queryClient}>
            {children}
          </QueryClientProvider>
        ),
      }
    );
    act(() => result.current.mutate({ silent }));
    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
  };

  it('shows the message the function returns for the variables', async () => {
    await runSuccess(false);
    expect(showToast).toHaveBeenCalledWith('Saved', 'success');
  });

  it('shows no toast when the function returns undefined', async () => {
    await runSuccess(true);
    expect(showToast).not.toHaveBeenCalled();
  });
});
