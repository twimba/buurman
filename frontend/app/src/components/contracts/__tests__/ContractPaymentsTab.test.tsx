import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { ContractPaymentsTab } from '../ContractPaymentsTab';
import { ContractStatus } from '@/types/contract';

// Guards against the "Generate Future Payments" button staying hidden for NOTICE_GIVEN
// contracts: the backend's generatePaymentsManually was fixed to accept ACTIVE or
// NOTICE_GIVEN contracts (a tenant under notice still owes rent up to the termination
// date), but the button here was still gated to ACTIVE only.

vi.mock('@/hooks/usePaymentHooks', () => ({
  usePaymentsByContract: () => ({ data: [], isLoading: false, error: null }),
  useDeletePayment: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));
vi.mock('@/hooks/useContractHooks', () => ({
  useGenerateContractPayments: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));
vi.mock('@/context/TeamContext', () => ({
  useTeam: () => ({ canEditData: true }),
}));
vi.mock('@/hooks/useFormatDate', () => ({
  useFormatDate: () => ({ formatDate: (d: string) => d }),
}));

describe('ContractPaymentsTab generate-future-payments button', () => {
  it('renders for a NOTICE_GIVEN contract', () => {
    renderWithProviders(
      <ContractPaymentsTab
        contractId="CTR00000000000000000000001"
        contractStatus={ContractStatus.NOTICE_GIVEN}
      />
    );
    expect(
      screen.getByRole('button', { name: /generate future payments/i })
    ).toBeInTheDocument();
  });

  it('renders for an ACTIVE contract', () => {
    renderWithProviders(
      <ContractPaymentsTab
        contractId="CTR00000000000000000000001"
        contractStatus={ContractStatus.ACTIVE}
      />
    );
    expect(
      screen.getByRole('button', { name: /generate future payments/i })
    ).toBeInTheDocument();
  });

  it('does not render for a TERMINATED contract', () => {
    renderWithProviders(
      <ContractPaymentsTab
        contractId="CTR00000000000000000000001"
        contractStatus={ContractStatus.TERMINATED}
      />
    );
    expect(
      screen.queryByRole('button', { name: /generate future payments/i })
    ).not.toBeInTheDocument();
  });
});
