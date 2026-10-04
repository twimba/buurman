import { screen, fireEvent } from '@testing-library/react';
import { useLocation } from 'react-router-dom';
import { renderWithProviders } from '@/test/test-utils';
import { ConfirmationStep } from '../ConfirmationStep';
import type { ContractTerminationResponse } from '@/generated/models';

// MemoryRouter keeps its own history, not the real window.location — a sibling reading
// useLocation() inside the same router is how a test observes where navigate() actually went.
const LocationDisplay = () => {
  const location = useLocation();
  return (
    <div data-testid="location">
      {location.pathname}
      {location.search}
    </div>
  );
};

const response: ContractTerminationResponse = {
  identifier: 'CTM1',
  contractIdentifier: 'CTR1',
  givenBy: 'LANDLORD',
  noticeDate: '2026-01-01',
  computedEndDate: '2026-03-01',
  effectiveEndDate: '2026-03-01',
  noticeLetterDocumentIdentifier: 'DOC1',
  status: 'NOTICE_GIVEN',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('ConfirmationStep', () => {
  it('shows the effective end date and notice letter identifier', () => {
    renderWithProviders(<ConfirmationStep response={response} />);

    expect(screen.getByText('2026-03-01')).toBeInTheDocument();
    expect(screen.getByText('DOC1')).toBeInTheDocument();
  });

  it('shows the deposit note only when the contract has a deposit', () => {
    const { rerender } = renderWithProviders(
      <ConfirmationStep response={response} hasDeposit={false} />
    );
    expect(screen.queryByText(/return due date/i)).not.toBeInTheDocument();

    rerender(<ConfirmationStep response={response} hasDeposit />);
    expect(screen.getByText(/return due date/i)).toBeInTheDocument();
  });

  it('navigates back to the contract when clicked', () => {
    renderWithProviders(
      <>
        <ConfirmationStep response={response} />
        <LocationDisplay />
      </>
    );

    fireEvent.click(screen.getByRole('button', { name: /back to contract/i }));

    expect(screen.getByTestId('location').textContent).toBe('/contracts/CTR1');
  });

  it('navigates to the document tab when "view document" is clicked', () => {
    renderWithProviders(
      <>
        <ConfirmationStep response={response} />
        <LocationDisplay />
      </>
    );

    fireEvent.click(screen.getByRole('button', { name: /view document/i }));

    expect(screen.getByTestId('location').textContent).toBe(
      '/contracts/CTR1?tab=documents'
    );
  });
});
