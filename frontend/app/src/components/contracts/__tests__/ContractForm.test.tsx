import { useEffect } from 'react';
import { fireEvent, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { ContractForm } from '../ContractForm';
import type { ContractResponse } from '@/types/contract';
import { PropertyCategory, PropertyType } from '@/types/property';
import { AllocationBasis } from '@/generated/models';
import { UnitStatus, UnitType } from '@/types/unit';
import type { PropertyResponse } from '@/types/property';
import type { UnitSummaryResponse } from '@/types/unit';

function unit(id: string): UnitSummaryResponse {
  return {
    identifier: `UNT${id}`,
    unitNumber: id,
    unitType: UnitType.APARTMENT,
    status: UnitStatus.VACANT,
  };
}

const mockUseProperty = vi.fn();

vi.mock('@/hooks/usePropertyHooks', () => ({
  useProperty: (...args: unknown[]) => mockUseProperty(...args),
}));
vi.mock('@/hooks/useContractHooks', () => ({
  useAddContractParty: () => ({ mutate: vi.fn(), isPending: false }),
  useRemoveContractParty: () => ({ mutate: vi.fn(), isPending: false }),
  useChangePrimaryContact: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/context/TeamContext', () => ({
  useTeam: () => ({ activeTeam: undefined, canEditData: true }),
}));
vi.mock('@/context/ImpersonationContext', () => ({
  useImpersonation: () => ({ active: false, mode: 'FULL' }),
}));
vi.mock('@/components/common/PropertySelector', () => ({
  PropertySelector: () => null,
}));
// Auto-selects on mount -- these tests exercise the unit picker, not contact/rent entry, so
// the other required fields resolve themselves rather than needing their own driven UI.
vi.mock('@/components/common/ContactSelector', () => ({
  ContactSelector: ({ onChange }: { onChange: (value: string) => void }) => {
    // `onChange` is a fresh inline closure on every ContractForm render, so depending on it
    // here would refire every render and loop forever -- fire once on mount only.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(() => onChange('CNT1'), []);
    return null;
  },
}));
vi.mock('@/components/contracts/CountryMetadataForm', () => ({
  default: () => null,
  useCountryName: () => undefined,
}));
vi.mock('@/components/contracts/RenewalConfigForm', () => ({
  RenewalConfigForm: () => null,
}));
vi.mock('@/components/contracts/RentBreakdown', () => ({
  RentBreakdown: ({
    onValueChange,
  }: {
    onValueChange: (value: number) => void;
  }) => {
    // Same one-shot rationale as the ContactSelector mock above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(() => onValueChange(1200), []);
    return null;
  },
}));
vi.mock('@buurman/ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@buurman/ui')>();
  return { ...actual, RichTextEditor: () => null };
});

const property: PropertyResponse = {
  identifier: 'PRP1',
  propertyCategory: PropertyCategory.RESIDENTIAL,
  propertyType: PropertyType.APARTMENT,
  allocationBasis: AllocationBasis.EQUAL,
  street: 'Main Street 1',
  city: 'Lisbon',
  postalCode: '1000-001',
  country: '',
  unitCount: 1,
  occupiedUnitCount: 0,
  vacantUnitCount: 1,
  units: [unit('1')],
  createdAt: '2024-01-01T00:00:00Z',
};

function submitForm(container: HTMLElement) {
  const form = container.querySelector('form') as HTMLFormElement;
  fireEvent.submit(form);
}

describe('ContractForm leaseRegime', () => {
  beforeEach(() => {
    mockUseProperty.mockReset();
    mockUseProperty.mockReturnValue({
      data: property,
      isLoading: false,
      error: null,
    });
  });

  it('sends STANDARD on create', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const { container } = renderWithProviders(
      <ContractForm
        onSubmit={onSubmit}
        isLoading={false}
        prefilledPropertyId="PRP1"
      />
    );
    const dates = container.querySelectorAll('input[type="date"]');
    fireEvent.change(dates[0], { target: { value: '2025-01-01' } });
    fireEvent.change(dates[1], { target: { value: '2025-12-31' } });
    submitForm(container);
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ leaseRegime: 'STANDARD' })
    );
  });

  it('preserves the regime of an edited contract', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const contract = {
      identifier: 'CON1',
      property: { identifier: 'PRP1' },
      parties: [],
      primaryContact: { identifier: 'CNT1' },
      contractType: 'FIXED_TERM',
      startDate: '2025-01-01',
      endDate: '2025-12-31',
      rentAmount: 1200,
      rentAmountCurrency: 'EUR',
      paymentFrequency: 'MONTHLY',
      status: 'DRAFT',
      leaseRegime: 'SHORT_TERM',
    } as unknown as ContractResponse;
    const { container } = renderWithProviders(
      <ContractForm contract={contract} onSubmit={onSubmit} isLoading={false} />
    );
    submitForm(container);
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ leaseRegime: 'SHORT_TERM' })
    );
  });
});
