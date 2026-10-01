import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ToastProvider } from '@buurman/ui';
import { renderWithProviders } from '@/test/test-utils';
import { ContractsPage } from '../ContractsPage';

// Guards against the NOTICE_GIVEN contract-status filter option being missing from the list
// page even though the backend's status filter already accepts it (openapi/src/paths/
// contracts.yaml) — a tenant under notice is still "in force" and landlords need to find
// those contracts just like any other status.

const mockUseContracts = vi.fn();

vi.mock('@/hooks/useContractHooks', () => ({
  useContracts: (...args: unknown[]) => mockUseContracts(...args),
}));
vi.mock('@/hooks/useSavedContractFilterHooks', () => ({
  useSavedContractFilters: () => ({ data: [] }),
  useCreateSavedContractFilter: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteSavedContractFilter: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));
vi.mock('@/context/TeamContext', () => ({
  useTeam: () => ({ canEditData: true }),
}));
vi.mock('@/context/FeatureFlagContext', () => ({
  useFeatureFlags: () => ({ isEnabled: () => false }),
}));

describe('ContractsPage status filter', () => {
  beforeEach(() => {
    mockUseContracts.mockReset();
    mockUseContracts.mockReturnValue({
      data: { content: [], totalElements: 0, totalPages: 0 },
      isLoading: false,
      isFetching: false,
      refetch: vi.fn(),
      error: null,
    });
  });

  it('offers a "Notice Given" status filter option and applies it', async () => {
    renderWithProviders(
      <ToastProvider>
        <ContractsPage />
      </ToastProvider>
    );

    await userEvent.click(screen.getByRole('button', { name: /^status$/i }));
    const noticeGivenOption = await screen.findByRole('option', {
      name: 'Notice Given',
    });
    expect(noticeGivenOption).toBeInTheDocument();

    await userEvent.click(noticeGivenOption);

    await waitFor(() => {
      const lastCallParams = mockUseContracts.mock.calls.at(-1)?.[0];
      expect(lastCallParams).toMatchObject({ status: 'NOTICE_GIVEN' });
    });
  });
});
