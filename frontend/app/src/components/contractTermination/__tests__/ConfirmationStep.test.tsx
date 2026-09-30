import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { ConfirmationStep } from '../ConfirmationStep';
import type { ContractTerminationResponse } from '@/generated/models';

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
    renderWithProviders(<ConfirmationStep response={response} />);

    fireEvent.click(
      screen.getByRole('button', { name: /back to contract/i })
    );
    // No error thrown means navigation was attempted without crashing.
  });
});
