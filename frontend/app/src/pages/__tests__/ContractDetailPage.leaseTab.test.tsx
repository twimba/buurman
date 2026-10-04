import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';
import { ContractDetailPage } from '../ContractDetailPage';

const CONTRACT = {
  identifier: 'CON00000000000000000000001',
  status: 'DRAFT',
  contractType: 'RESIDENTIAL',
  property: { identifier: 'PRP00000000000000000000001' },
};

const team = vi.hoisted(() => ({ canEditData: true }));

vi.mock('@/context/TeamContext', () => ({
  useTeam: () => team,
}));
vi.mock('@/hooks/useContractHooks', () => ({
  useContract: () => ({ data: CONTRACT, isLoading: false, error: null }),
  useDeleteContract: () => ({ mutateAsync: vi.fn() }),
  useChangeContractStatus: () => ({ mutateAsync: vi.fn() }),
  useReopenContract: () => ({ mutateAsync: vi.fn() }),
  useDuplicateContract: () => ({ mutateAsync: vi.fn() }),
  useContractTimeline: () => ({ data: [], isLoading: false, isError: false }),
}));
vi.mock('@/hooks/useCommunications', () => ({
  useContractCommunications: () => ({
    data: [],
    isLoading: false,
    isError: false,
  }),
  useResendCommunication: () => ({ mutate: vi.fn() }),
}));
vi.mock('@/components/common/DocumentDownloadMenu', () => ({
  DocumentDownloadMenu: () => null,
}));
vi.mock('@/components/contracts/ContractOverviewTab', () => ({
  ContractOverviewTab: () => null,
}));
vi.mock('@/components/contracts/ContractDocumentsTab', () => ({
  ContractDocumentsTab: () => <div>documents tab content</div>,
}));
// The real tab's own behaviour is covered in ContractLeaseAgreementTab.test.tsx; here only the
// props the page wires into it matter.
vi.mock('@/components/contracts/ContractLeaseAgreementTab', () => ({
  ContractLeaseAgreementTab: ({
    onGoToDocuments,
    onEditProperty,
  }: {
    onGoToDocuments: () => void;
    onEditProperty?: () => void;
  }) => (
    <div>
      <button onClick={onGoToDocuments}>stub go to documents</button>
      {onEditProperty && (
        <button onClick={onEditProperty}>stub edit property</button>
      )}
    </div>
  ),
}));

const Location = () => {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
};

const renderPage = () =>
  renderWithProviders(
    <>
      <Location />
      <Routes>
        <Route path="/contracts/:id" element={<ContractDetailPage />} />
        <Route path="/properties/:pid/edit" element={<div>edit page</div>} />
      </Routes>
    </>,
    {
      initialEntries: [
        '/contracts/CON00000000000000000000001?tab=leaseAgreement',
      ],
    }
  );

describe('ContractDetailPage lease agreement tab wiring', () => {
  beforeEach(() => {
    team.canEditData = true;
  });

  it('navigates to the property edit page from the tab action', async () => {
    renderPage();

    await userEvent.click(
      await screen.findByRole('button', { name: 'stub edit property' })
    );

    expect(await screen.findByText('edit page')).toBeInTheDocument();
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/properties/PRP00000000000000000000001/edit'
    );
  });

  it('does not offer the property edit action when the user cannot edit data', async () => {
    team.canEditData = false;
    renderPage();

    await screen.findByRole('button', { name: 'stub go to documents' });
    expect(
      screen.queryByRole('button', { name: 'stub edit property' })
    ).toBeNull();
  });

  it('switches to the Documents tab from the tab action', async () => {
    renderPage();

    await userEvent.click(
      await screen.findByRole('button', { name: 'stub go to documents' })
    );

    expect(
      await screen.findByText('documents tab content')
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'stub go to documents' })
    ).toBeNull();
  });
});
