import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { PropertyDetailPage } from '../PropertyDetailPage';
import { PropertyCategory, PropertyType } from '@/types/property';
import { AllocationBasis } from '@/generated/models';
import { UnitStatus, UnitType } from '@/types/unit';
import type { PropertyResponse } from '@/types/property';
import type { Unit } from '@/types/unit';

// Review Focus item 3 (BUUR-106 Task 5): once a bulk-create/split turns a property from one
// unit into several, the Info tab's inline dwelling card must stop rendering immediately --
// otherwise a landlord could keep editing a unit that is no longer "the" unit for this
// property, via a stale single-unit view of a now-multi-unit building.
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
  createdAt: '2024-01-01T00:00:00Z',
};

let property: PropertyResponse = {
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

vi.mock('@/hooks/usePropertyHooks', () => ({
  useProperty: () => ({ data: property, isLoading: false, error: null }),
  useDeleteProperty: () => ({ mutateAsync: vi.fn() }),
}));
vi.mock('@/hooks/useUnitHooks', () => ({
  useUnit: () => ({ data: soleUnit }),
  useUpdateUnit: () => ({ mutate: vi.fn(), isPending: false }),
  useUnits: () => ({ data: [], isLoading: false, error: undefined }),
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

describe('PropertyDetailPage stale-cache after the split transition', () => {
  it('stops rendering dwelling fields inline once the property has several units', async () => {
    property = {
      ...property,
      unitCount: 1,
      units: [
        {
          identifier: soleUnit.identifier,
          unitNumber: soleUnit.unitNumber,
          unitType: soleUnit.unitType,
          status: soleUnit.status,
        },
      ],
    };

    const { rerender } = renderWithProviders(<PropertyDetailPage />, {
      initialEntries: ['/properties/prp_01TEST'],
    });

    expect(await screen.findByText('Characteristics')).toBeInTheDocument();

    // Simulate the split having completed: the property now has several real units and the
    // sole implicit unit no longer exists as "the" unit for this property.
    property = { ...property, unitCount: 6, units: [] };
    rerender(<PropertyDetailPage />);

    expect(screen.queryByText('Characteristics')).not.toBeInTheDocument();
  });
});
