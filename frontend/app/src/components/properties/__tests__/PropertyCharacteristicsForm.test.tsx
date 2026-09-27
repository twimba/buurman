import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { PropertyCharacteristicsForm } from '../PropertyCharacteristicsForm';
import { PropertyCategory, PropertyType } from '@/types/property';
import type { CreatePropertyRequest } from '@/types/property';

// Guards against BUUR-106's dwelling-field deletion (16 fields moved from properties to
// units) accidentally taking a building-level section with it — every field here has a
// two-line "add" and a one-line "remove", and CollapsibleSection makes it easy to close
// the wrong tag. These headings must survive because the fields under them are still on
// CreatePropertyRequest/PropertyResponse (building-level, not dwelling-level).
const baseFormData: CreatePropertyRequest = {
  propertyCategory: PropertyCategory.RESIDENTIAL,
  propertyType: PropertyType.APARTMENT,
  street: 'Main Street 1',
  city: 'Amsterdam',
  postalCode: '1011AB',
  country: 'NL',
};

describe('PropertyCharacteristicsForm surviving building-level sections', () => {
  it('renders the construction, utilities, parking and safety sections', () => {
    renderWithProviders(
      <PropertyCharacteristicsForm formData={baseFormData} onChange={vi.fn()} />
    );

    expect(screen.getByText('Construction & Structure')).toBeInTheDocument();
    expect(screen.getByText('Utilities & Connections')).toBeInTheDocument();
    expect(screen.getByText('Parking')).toBeInTheDocument();
    expect(screen.getByText('Safety & Security')).toBeInTheDocument();
  });
});
