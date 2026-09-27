import { useEffect } from 'react';
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { ContractCreatePage } from '../ContractCreatePage';
import { PropertyCategory, PropertyType } from '@/types/property';
import { AllocationBasis } from '@/generated/models';
import { UnitStatus, UnitType } from '@/types/unit';
import type { PropertyResponse } from '@/types/property';
import type { UnitSummaryResponse } from '@/types/unit';

// PREAMBLE Review Focus item 4: a contract create flow reached from a deep link with a
// pre-selected property that has several units must block submit with a message naming the
// choice, not let a missing unitIdentifier reach the backend as a raw 400. `contracts.unit_id`
// is NOT NULL and the DB allows at most one ACTIVE contract per unit, so the picker must also
// only ever offer units that belong to the selected property.

function unit(id: string): UnitSummaryResponse {
  return {
    identifier: `UNT${id}`,
    unitNumber: id,
    unitType: UnitType.APARTMENT,
    status: UnitStatus.VACANT,
  };
}

const mockUseProperty = vi.fn();
const mockUseCreateContract = vi.fn();

vi.mock('@/hooks/usePropertyHooks', () => ({
  useProperty: (...args: unknown[]) => mockUseProperty(...args),
}));
vi.mock('@/hooks/useContractHooks', () => ({
  useCreateContract: () => mockUseCreateContract(),
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

function renderContractCreate({
  units,
  mutate = vi.fn(),
}: {
  units: UnitSummaryResponse[];
  mutate?: ReturnType<typeof vi.fn>;
}) {
  const property: PropertyResponse = {
    identifier: 'PRP1',
    propertyCategory: PropertyCategory.RESIDENTIAL,
    propertyType: PropertyType.APARTMENT,
    allocationBasis: AllocationBasis.EQUAL,
    street: 'Main Street 1',
    city: 'Lisbon',
    postalCode: '1000-001',
    country: '',
    unitCount: units.length,
    occupiedUnitCount: 0,
    vacantUnitCount: units.length,
    units,
    createdAt: '2024-01-01T00:00:00Z',
  };

  mockUseProperty.mockReturnValue({
    data: property,
    isLoading: false,
    error: null,
  });
  mockUseCreateContract.mockReturnValue({
    mutateAsync: mutate,
    isPending: false,
  });

  return renderWithProviders(<ContractCreatePage />, {
    initialEntries: ['/contracts/new?propertyId=PRP1'],
  });
}

/** Fills every OTHER required field (dates), leaving the unit as the only open question. */
function fillRequiredDates(container: HTMLElement) {
  const dateInputs = container.querySelectorAll('input[type="date"]');
  fireEvent.change(dateInputs[0], { target: { value: '2025-01-01' } });
  fireEvent.change(dateInputs[1], { target: { value: '2025-12-31' } });
}

describe('ContractCreatePage unit picker', () => {
  beforeEach(() => {
    mockUseProperty.mockReset();
    mockUseCreateContract.mockReset();
  });

  it('hides the picker when the property has one unit', async () => {
    renderContractCreate({ units: [unit('1')] });
    expect(screen.queryByLabelText('Unit')).not.toBeInTheDocument();
  });

  it('shows the picker when the property has several units', async () => {
    renderContractCreate({ units: [unit('1'), unit('2')] });
    expect(await screen.findByLabelText('Unit')).toBeInTheDocument();
  });

  it('blocks submit with a message when no unit is chosen', async () => {
    renderContractCreate({ units: [unit('1'), unit('2'), unit('3')] });
    await userEvent.click(
      screen.getByRole('button', { name: 'Create Contract' })
    );
    expect(
      screen.getByText('Choose which unit this contract is for.')
    ).toBeInTheDocument();
  });

  it('sends the chosen unitIdentifier', async () => {
    const mutate = vi.fn();
    const { container } = renderContractCreate({
      units: [unit('1'), unit('2')],
      mutate,
    });
    fillRequiredDates(container);
    await userEvent.selectOptions(await screen.findByLabelText('Unit'), 'UNT2');
    await userEvent.click(
      screen.getByRole('button', { name: 'Create Contract' })
    );
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ unitIdentifier: 'UNT2' })
    );
  });
});
