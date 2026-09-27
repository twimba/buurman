import { screen, fireEvent, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { PropertyForm } from '../PropertyForm';

// BUUR-106 dropped `status` and moved 16 dwelling fields off CreatePropertyRequest onto units.
// This locks in that submitting the create form still produces a payload of the surviving
// building-level keys, and not a payload built around a field that no longer exists.
vi.mock('@/hooks/useTeamDefaults', () => ({
  useTeamDefaults: () => ({ defaultCountryCode: 'NL' }),
}));
vi.mock('@/hooks/useGeocodingHooks', () => ({
  useGeocode: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/generated/api/rent-regulations/rent-regulations', () => ({
  listRentRegulationCountries: () => Promise.resolve([]),
  getRentRegulationCountryDetail: () => Promise.resolve(undefined),
}));
vi.mock('@/components/common/InteractiveMap', () => ({
  InteractiveMap: () => null,
}));

describe('PropertyForm submit payload', () => {
  it('submits the building-level fields (street, city, postalCode, propertyCategory, propertyType)', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(<PropertyForm onSubmit={onSubmit} isLoading={false} />);

    fireEvent.change(screen.getByPlaceholderText('Main Street 123'), {
      target: { value: 'Main Street 1' },
    });
    fireEvent.change(screen.getByPlaceholderText('Amsterdam'), {
      target: { value: 'Amsterdam' },
    });
    fireEvent.change(screen.getByPlaceholderText('1012 AB'), {
      target: { value: '1011AB' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Create Property' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    const payload = onSubmit.mock.calls[0][0];
    expect(payload).toMatchObject({
      street: 'Main Street 1',
      city: 'Amsterdam',
      postalCode: '1011AB',
      propertyCategory: 'RESIDENTIAL',
      propertyType: 'APARTMENT',
    });
    expect(payload).not.toHaveProperty('status');
  });
});
