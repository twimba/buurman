import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useLocation } from 'react-router-dom';
import { ToastProvider } from '@buurman/ui';
import { renderWithProviders } from '@/test/test-utils';
import { ContractsPage } from '../ContractsPage';

// MemoryRouter keeps its own history, not the real window.location — a sibling reading
// useLocation() inside the same router is how a test observes what ContractsPage wrote via
// setSearchParams.
const LocationDisplay = () => {
  const location = useLocation();
  return <div data-testid="location-search">{location.search}</div>;
};

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
  useCreateSavedContractFilter: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useDeleteSavedContractFilter: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
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

  it('defaults to Active, Draft, Pending Signature and Notice Given on first load', async () => {
    renderWithProviders(
      <ToastProvider>
        <ContractsPage />
      </ToastProvider>
    );

    await waitFor(() => {
      const lastCallParams = mockUseContracts.mock.calls.at(-1)?.[0];
      expect(lastCallParams?.status).toEqual(
        expect.arrayContaining([
          'ACTIVE',
          'DRAFT',
          'PENDING_SIGNATURE',
          'NOTICE_GIVEN',
        ])
      );
      expect(lastCallParams?.status).toHaveLength(4);
    });
  });

  it('is a multi-select — toggling "Expired" on adds it without clearing the others', async () => {
    renderWithProviders(
      <ToastProvider>
        <ContractsPage />
      </ToastProvider>
    );

    await userEvent.click(screen.getByRole('button', { name: /status/i }));
    const expiredOption = await screen.findByRole('checkbox', {
      name: 'Expired',
    });
    await userEvent.click(expiredOption);

    // Multi-select popovers stay open after a toggle.
    expect(expiredOption).toBeInTheDocument();

    await waitFor(() => {
      const lastCallParams = mockUseContracts.mock.calls.at(-1)?.[0];
      expect(lastCallParams?.status).toEqual(
        expect.arrayContaining([
          'ACTIVE',
          'DRAFT',
          'PENDING_SIGNATURE',
          'NOTICE_GIVEN',
          'EXPIRED',
        ])
      );
      expect(lastCallParams?.status).toHaveLength(5);
    });

    await userEvent.click(
      screen.getByRole('checkbox', { name: 'Notice Given' })
    );

    await waitFor(() => {
      const lastCallParams = mockUseContracts.mock.calls.at(-1)?.[0];
      expect(lastCallParams?.status).not.toContain('NOTICE_GIVEN');
    });
  });
});

describe('ContractsPage URL sync', () => {
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

  it('initializes filters from the URL on mount instead of the defaults — a refresh or a bookmark must reproduce the same view', async () => {
    renderWithProviders(
      <ToastProvider>
        <ContractsPage />
      </ToastProvider>,
      {
        initialEntries: [
          '/contracts?status=EXPIRED&status=TERMINATED&search=smith&endingWithinDays=30&sort=startDate&direction=DESC',
        ],
      }
    );

    await waitFor(() => {
      const lastCallParams = mockUseContracts.mock.calls.at(-1)?.[0];
      expect(lastCallParams?.status).toEqual(
        expect.arrayContaining(['EXPIRED', 'TERMINATED'])
      );
      expect(lastCallParams?.status).toHaveLength(2);
      expect(lastCallParams?.search).toBe('smith');
      expect(lastCallParams?.endingWithinDays).toBe(30);
      expect(lastCallParams?.sort).toBe('startDate');
      expect(lastCallParams?.direction).toBe('DESC');
    });
  });

  it('ignores an invalid status/sort value from the URL rather than passing it through to the API', async () => {
    renderWithProviders(
      <ToastProvider>
        <ContractsPage />
      </ToastProvider>,
      { initialEntries: ['/contracts?status=NOT_A_REAL_STATUS&sort=nonsense'] }
    );

    await waitFor(() => {
      const lastCallParams = mockUseContracts.mock.calls.at(-1)?.[0];
      // An invalid status list falls back to the default set, same as no status param at all.
      expect(lastCallParams?.status).toEqual(
        expect.arrayContaining([
          'ACTIVE',
          'DRAFT',
          'PENDING_SIGNATURE',
          'NOTICE_GIVEN',
        ])
      );
      expect(lastCallParams?.sort).toBe('endDate');
    });
  });

  it('writes the current filter state back to the URL as it changes', async () => {
    renderWithProviders(
      <ToastProvider>
        <ContractsPage />
        <LocationDisplay />
      </ToastProvider>
    );

    await userEvent.click(screen.getByRole('button', { name: /status/i }));
    const expiredOption = await screen.findByRole('checkbox', {
      name: 'Expired',
    });
    await userEvent.click(expiredOption);

    await waitFor(() => {
      expect(screen.getByTestId('location-search').textContent).toContain(
        'status=EXPIRED'
      );
    });
  });
});
