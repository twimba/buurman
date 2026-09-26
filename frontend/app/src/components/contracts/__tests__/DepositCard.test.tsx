import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { DepositCard } from '../DepositCard';
import { useContractDeposit } from '@/hooks/useContractHooks';
import { useTeam } from '@/context/TeamContext';
import { getDepositStatement } from '@/generated/api/letters/letters';
import { downloadBlob } from '@/utils/downloadBlob';
import type { ContractResponse, DepositResponse } from '@/types/contract';

vi.mock('@/hooks/useContractHooks', () => {
  const mutation = () => ({
    mutate: vi.fn(),
    mutateAsync: vi.fn(),
    isPending: false,
  });
  return {
    useContractDeposit: vi.fn(),
    useUpsertDeposit: mutation,
    useAddDepositDeduction: mutation,
    useRemoveDepositDeduction: mutation,
    useReturnDeposit: mutation,
    useForfeitDeposit: mutation,
  };
});
vi.mock('@/context/TeamContext', () => ({ useTeam: vi.fn() }));
vi.mock('@/hooks/useFormatDate', () => ({
  useFormatDate: () => ({ formatDate: (d: string) => `fmt:${d}` }),
}));
vi.mock('@/generated/api/letters/letters', () => ({
  getDepositStatement: vi.fn(),
}));
vi.mock('@/utils/downloadBlob', () => ({ downloadBlob: vi.fn() }));

const contract = {
  identifier: 'ctr_01TEST',
  rentAmountCurrency: 'EUR',
  documentLanguages: ['nl', 'en'],
} as unknown as ContractResponse;

const deposit: DepositResponse = {
  identifier: 'dep_01TEST',
  contractIdentifier: 'ctr_01TEST',
  amount: 1250,
  currency: 'EUR',
  receivedDate: '2025-01-01',
  heldWhere: 'Escrow',
  status: 'HELD',
  returnedAmount: 0,
  deductionsTotal: 100,
  refundable: 1150,
  deductions: [
    {
      identifier: 'ddd_01TEST',
      amount: 100,
      reason: 'Wall repair',
      deductionDate: '2025-09-15',
      createdAt: '2025-09-15T00:00:00Z',
    },
  ],
  createdAt: '2025-01-01T00:00:00Z',
};

const mockUseTeam = vi.mocked(useTeam);
const mockUseDeposit = vi.mocked(useContractDeposit);

beforeEach(() => {
  vi.clearAllMocks();
  mockUseTeam.mockReturnValue({ canEditData: true } as ReturnType<
    typeof useTeam
  >);
});

describe('DepositCard', () => {
  it('offers to record a deposit when none exists yet', () => {
    mockUseDeposit.mockReturnValue({
      data: undefined,
      isLoading: false,
    } as never);
    renderWithProviders(
      <DepositCard contract={contract} contractId="ctr_01TEST" />
    );
    expect(
      screen.getByRole('button', { name: 'Record deposit' })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /Statement/ })
    ).not.toBeInTheDocument();
  });

  it('shows the held deposit, its deductions and the refundable amount', () => {
    mockUseDeposit.mockReturnValue({
      data: deposit,
      isLoading: false,
    } as never);
    renderWithProviders(
      <DepositCard contract={contract} contractId="ctr_01TEST" />
    );
    expect(screen.getByText('Held')).toBeInTheDocument();
    expect(screen.getByText('Wall repair')).toBeInTheDocument();
    expect(screen.getByText('€1,150.00')).toBeInTheDocument();
  });

  it('downloads the statement in the contract document language', async () => {
    const user = userEvent.setup();
    mockUseDeposit.mockReturnValue({
      data: deposit,
      isLoading: false,
    } as never);
    const blob = new Blob(['%PDF'], { type: 'application/pdf' });
    vi.mocked(getDepositStatement).mockResolvedValue(blob);

    renderWithProviders(
      <DepositCard contract={contract} contractId="ctr_01TEST" />
    );
    await user.click(screen.getByRole('button', { name: 'Statement (PDF)' }));

    await waitFor(() => expect(downloadBlob).toHaveBeenCalled());
    expect(getDepositStatement).toHaveBeenCalledWith('ctr_01TEST', {
      lang: 'nl',
    });
    expect(downloadBlob).toHaveBeenCalledWith(
      blob,
      'deposit-statement-ctr_01TEST-nl.pdf'
    );
  });

  it('hides editing actions for read-only members', () => {
    mockUseTeam.mockReturnValue({ canEditData: false } as ReturnType<
      typeof useTeam
    >);
    mockUseDeposit.mockReturnValue({
      data: deposit,
      isLoading: false,
    } as never);
    renderWithProviders(
      <DepositCard contract={contract} contractId="ctr_01TEST" />
    );
    expect(screen.queryByText('Add deduction')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Statement (PDF)' })
    ).toBeInTheDocument();
  });
});
