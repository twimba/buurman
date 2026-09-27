import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { PropertyDetailPage } from '../PropertyDetailPage';
import { PropertyCategory, PropertyType } from '@/types/property';
import { AllocationBasis } from '@/generated/models';
import type { PropertyResponse } from '@/types/property';

// Guards against BUUR-106's dwelling-field deletion (16 fields moved from properties to
// units) accidentally taking a building-level read-out block with it. These four blocks are
// hand-closed <div>s, not a shared wrapper component, so a stray closing tag is easy to miss.
const property: PropertyResponse = {
  identifier: 'prp_01TEST',
  propertyCategory: PropertyCategory.RESIDENTIAL,
  propertyType: PropertyType.APARTMENT,
  allocationBasis: AllocationBasis.EQUAL,
  street: 'Main Street 1',
  city: 'Amsterdam',
  postalCode: '1011AB',
  country: 'PT',
  yearBuilt: 1990,
  constructionType: 'BRICK',
  electricityConnectionType: 'SINGLE_PHASE',
  waterConnectionType: 'MUNICIPAL',
  parkingSpaces: 1,
  parkingType: 'GARAGE',
  hasSprinklerSystem: false,
  hasAlarmSystem: true,
  hasSecurityCameras: false,
  hasSecureEntry: true,
  isWheelchairAccessible: false,
  hasElevator: true,
  hasStepFreeEntrance: false,
  unitCount: 1,
  occupiedUnitCount: 1,
  vacantUnitCount: 0,
  units: [],
  createdAt: '2024-01-01T00:00:00Z',
};

vi.mock('@/hooks/usePropertyHooks', () => ({
  useProperty: () => ({ data: property, isLoading: false, error: null }),
  useDeleteProperty: () => ({ mutateAsync: vi.fn() }),
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

describe('PropertyDetailPage surviving building-level sections', () => {
  it('renders the construction, utilities, parking and safety sections on the Info tab', async () => {
    renderWithProviders(<PropertyDetailPage />, {
      initialEntries: ['/properties/prp_01TEST'],
    });

    expect(
      await screen.findByText('Construction & Structure')
    ).toBeInTheDocument();
    expect(screen.getByText('Utilities & Connections')).toBeInTheDocument();
    expect(screen.getByText('Parking')).toBeInTheDocument();
    expect(screen.getByText('Safety & Security')).toBeInTheDocument();
  });
});
