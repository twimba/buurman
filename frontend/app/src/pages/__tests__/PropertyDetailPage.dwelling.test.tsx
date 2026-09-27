import { fireEvent, screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { PropertyDetailPage } from '../PropertyDetailPage';
import { PropertyCategory, PropertyType } from '@/types/property';
import { AllocationBasis } from '@/generated/models';
import { UnitStatus, UnitType } from '@/types/unit';
import type { PropertyResponse } from '@/types/property';
import type { Unit } from '@/types/unit';

// Covers the single-unit Info tab restoration (BUUR-106 Task 3): a property with exactly one
// unit must show its dwelling characteristics inline, editable, without the landlord ever
// seeing the word "unit".
const soleUnit: Unit = {
  identifier: 'unt_01TEST',
  propertyIdentifier: 'prp_01TEST',
  unitNumber: '1',
  unitType: UnitType.APARTMENT,
  status: UnitStatus.OCCUPIED,
  implicit: true,
  sortOrder: 0,
  areaValue: 68,
  areaUnit: 'sqm',
  energyEfficiencyRating: 'B',
  createdAt: '2024-01-01T00:00:00Z',
};

const property: PropertyResponse = {
  identifier: 'prp_01TEST',
  propertyCategory: PropertyCategory.RESIDENTIAL,
  propertyType: PropertyType.APARTMENT,
  allocationBasis: AllocationBasis.EQUAL,
  street: 'Main Street 1',
  city: 'Amsterdam',
  postalCode: '1011AB',
  country: 'PT',
  unitCount: 1,
  occupiedUnitCount: 1,
  vacantUnitCount: 0,
  units: [
    {
      identifier: soleUnit.identifier,
      unitNumber: soleUnit.unitNumber,
      unitType: soleUnit.unitType,
      status: soleUnit.status,
    },
  ],
  createdAt: '2024-01-01T00:00:00Z',
};

const updateUnitMutate = vi.fn();

vi.mock('@/hooks/usePropertyHooks', () => ({
  useProperty: () => ({ data: property, isLoading: false, error: null }),
  useDeleteProperty: () => ({ mutateAsync: vi.fn() }),
}));
vi.mock('@/hooks/useUnitHooks', () => ({
  useUnit: () => ({ data: soleUnit }),
  useUpdateUnit: () => ({ mutate: updateUnitMutate, isPending: false }),
}));
vi.mock('@/hooks/useOccupancyPeriodHooks', () => ({
  useOccupancyPeriods: () => ({ data: [] }),
  useDeleteOccupancyPeriod: () => ({ mutate: vi.fn() }),
}));
vi.mock('@/hooks/usePropertyFinancialsHooks', () => ({
  useFinancings: () => ({ data: [] }),
}));
vi.mock('@/hooks/useWwsHooks', () => ({
  useLatestWwsCalculation: () => ({ data: undefined }),
  useWwsCalculations: () => ({ data: [] }),
  useDeleteWwsCalculation: () => ({ mutate: vi.fn() }),
}));
vi.mock('@/context/TeamContext', () => ({
  useTeam: () => ({ canEditData: true, canManageMembers: true }),
}));
vi.mock('@/context/FeatureFlagContext', () => ({
  useFeatureFlags: () => ({
    isEnabled: () => false,
    getValue: () => null,
    flags: {},
    isLoading: false,
  }),
}));
vi.mock('@/components/common/InteractiveMap', () => ({
  InteractiveMap: () => null,
}));
vi.mock('@/components/properties/PropertyLifecycleTimeline', () => ({
  PropertyLifecycleTimeline: () => null,
}));
vi.mock('@/components/common/CalendarFeedPopover', () => ({
  CalendarFeedButton: () => null,
}));
vi.mock('@/components/common/DocumentDownloadMenu', () => ({
  DocumentDownloadMenu: () => null,
}));
vi.mock('@/hooks/useFormatDate', () => ({
  useFormatDate: () => ({
    formatDate: (d: string) => d,
    formatDateTime: (d: string) => d,
    formatRelative: (d: string) => d,
  }),
}));

describe('PropertyDetailPage single-unit dwelling characteristics', () => {
  beforeEach(() => {
    updateUnitMutate.mockClear();
  });

  it('renders the dwelling characteristics card for a single-unit property', async () => {
    renderWithProviders(<PropertyDetailPage />, {
      initialEntries: ['/properties/prp_01TEST'],
    });

    expect(await screen.findByText('Characteristics')).toBeInTheDocument();
  });

  it('saves an edited field through useUpdateUnit, not a hand-built request', async () => {
    renderWithProviders(<PropertyDetailPage />, {
      initialEntries: ['/properties/prp_01TEST'],
    });

    await screen.findByText('Characteristics');
    fireEvent.click(screen.getByRole('button', { name: /Floor area/ }));

    const areaInput = screen.getByRole('spinbutton');
    fireEvent.change(areaInput, { target: { value: '80' } });

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => expect(updateUnitMutate).toHaveBeenCalledTimes(1));
    const [request] = updateUnitMutate.mock.calls[0];
    // unitToUpdateRequest seeds every field the PUT endpoint replaces (amendment A4) --
    // only areaValue differs from the loaded unit.
    expect(request).toMatchObject({
      unitNumber: soleUnit.unitNumber,
      unitType: soleUnit.unitType,
      areaValue: 80,
    });
  });
});
