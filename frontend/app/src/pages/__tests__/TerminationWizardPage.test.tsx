import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { TerminationWizardPage } from '../TerminationWizardPage';
import {
  useTerminationPreview,
  useTerminateContract,
} from '@/hooks/useContractTerminationHooks';
import { useContract, useContractDeposit } from '@/hooks/useContractHooks';
import type { ContractTerminationResponse } from '@/generated/models';

vi.mock('@/hooks/useContractTerminationHooks', () => ({
  useTerminationPreview: vi.fn(),
  useTerminateContract: vi.fn(),
}));

vi.mock('@/hooks/useContractHooks', () => ({
  useContract: vi.fn(),
  useContractDeposit: vi.fn(),
}));

const mockedUseTerminationPreview =
  useTerminationPreview as unknown as ReturnType<typeof vi.fn>;
const mockedUseTerminateContract =
  useTerminateContract as unknown as ReturnType<typeof vi.fn>;
const mockedUseContract = useContract as unknown as ReturnType<typeof vi.fn>;
const mockedUseContractDeposit = useContractDeposit as unknown as ReturnType<
  typeof vi.fn
>;

const terminationResponse: ContractTerminationResponse = {
  identifier: 'CTM1',
  contractIdentifier: 'CTR1',
  givenBy: 'LANDLORD',
  noticeDate: '2026-01-01',
  computedEndDate: '2026-03-01',
  effectiveEndDate: '2026-02-01',
  overrideReason: 'Tenant requested an earlier move-out',
  status: 'NOTICE_GIVEN',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('TerminationWizardPage — override reason reveal', () => {
  let mutate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockedUseContract.mockReturnValue({ data: undefined });
    mockedUseContractDeposit.mockReturnValue({ data: null });
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });
    mutate = vi.fn((_request, options) => {
      options?.onSuccess?.(terminationResponse);
    });
    mockedUseTerminateContract.mockReturnValue({
      mutate,
      isPending: false,
    });
  });

  const advanceToReviewStep = () => {
    fireEvent.click(screen.getByText('Landlord'));
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
  };

  it('does not show the override reason field on entering the review step', () => {
    renderWithProviders(<TerminationWizardPage />);

    advanceToReviewStep();

    expect(screen.getByText('2026-03-01')).toBeInTheDocument();
    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();
  });

  it('reveals the override reason field only once an earlier end date is entered', () => {
    renderWithProviders(<TerminationWizardPage />);

    advanceToReviewStep();

    const endDateInput = screen.getByLabelText(
      /use an earlier end date/i
    ) as HTMLInputElement;

    // Still not visible before any edit.
    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();

    fireEvent.change(endDateInput, { target: { value: '2026-02-01' } });

    expect(
      screen.getByLabelText(/reason for the earlier end date/i)
    ).toBeInTheDocument();
  });

  it('hides the override reason field again if the date is changed back to the computed date', () => {
    renderWithProviders(<TerminationWizardPage />);

    advanceToReviewStep();

    const endDateInput = screen.getByLabelText(/use an earlier end date/i);
    fireEvent.change(endDateInput, { target: { value: '2026-02-01' } });
    expect(
      screen.getByLabelText(/reason for the earlier end date/i)
    ).toBeInTheDocument();

    fireEvent.change(endDateInput, { target: { value: '2026-03-01' } });
    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();
  });

  it('does not submit a stale override reason after the date is edited back to non-earlier', () => {
    renderWithProviders(<TerminationWizardPage />);

    advanceToReviewStep();

    const endDateInput = screen.getByLabelText(/use an earlier end date/i);

    // Type an earlier date, then a reason.
    fireEvent.change(endDateInput, { target: { value: '2026-02-01' } });
    fireEvent.change(
      screen.getByLabelText(/reason for the earlier end date/i),
      { target: { value: 'Tenant requested an earlier move-out' } }
    );

    // Edit the date back to the computed date — the reason field hides, and its value is
    // cleared even though the landlord never touched the textarea directly.
    fireEvent.change(endDateInput, { target: { value: '2026-03-01' } });
    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /next/i }));

    // Letter preview must not show the stale reason.
    expect(
      screen.queryByText('Tenant requested an earlier move-out')
    ).not.toBeInTheDocument();

    fireEvent.click(
      screen.getByRole('button', { name: /confirm & terminate/i })
    );

    const [submittedRequest] = mutate.mock.calls[0];
    expect(submittedRequest.overrideReason).toBeUndefined();
    expect(submittedRequest).not.toHaveProperty(
      'overrideReason',
      'Tenant requested an earlier move-out'
    );
  });

  it('carries the override through to the letter preview and confirmation steps', () => {
    renderWithProviders(<TerminationWizardPage />);

    advanceToReviewStep();

    fireEvent.change(screen.getByLabelText(/use an earlier end date/i), {
      target: { value: '2026-02-01' },
    });
    fireEvent.change(
      screen.getByLabelText(/reason for the earlier end date/i),
      { target: { value: 'Tenant requested an earlier move-out' } }
    );
    fireEvent.click(screen.getByRole('button', { name: /next/i }));

    // Letter preview step shows the override.
    expect(screen.getByText('2026-02-01')).toBeInTheDocument();
    expect(
      screen.getByText('Tenant requested an earlier move-out')
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole('button', { name: /confirm & terminate/i })
    );

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        givenBy: 'LANDLORD',
        effectiveEndDate: '2026-02-01',
        overrideReason: 'Tenant requested an earlier move-out',
      }),
      expect.anything()
    );

    // Confirmation step is shown.
    expect(screen.getByText('Notice Given')).toBeInTheDocument();
  });

  it('shows the deposit reminder on confirmation while the deposit query is still loading, instead of false-negativing to "no deposit"', () => {
    mockedUseContractDeposit.mockReturnValue({
      data: undefined,
      isLoading: true,
    });
    renderWithProviders(<TerminationWizardPage />);

    advanceToReviewStep();
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    fireEvent.click(
      screen.getByRole('button', { name: /confirm & terminate/i })
    );

    expect(
      screen.getByText(
        'The deposit return due date has been updated to match the effective end date.'
      )
    ).toBeInTheDocument();
  });
});
