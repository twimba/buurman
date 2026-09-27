import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { UnitGrid } from '../UnitGrid';

const row = (overrides = {}) => ({
  identifier: 'UNT01HQJK4B2X5M3N7P8Q9R0S1T',
  unitNumber: '1',
  name: null,
  unitType: 'APARTMENT',
  status: 'VACANT',
  tenantName: null,
  monthlyRent: null,
  vacancyDays: 42,
  ...overrides,
});

describe('UnitGrid', () => {
  it('renders one row per unit', () => {
    render(
      <UnitGrid
        propertyIdentifier="PRO1"
        rows={[row(), row({ unitNumber: '2' })]}
      />
    );
    expect(screen.getAllByRole('row')).toHaveLength(3); // header + 2
  });

  it('shows a dash rather than an empty cell when a unit has no tenant', () => {
    render(<UnitGrid propertyIdentifier="PRO1" rows={[row()]} />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('shows vacancy days only for unlet units', () => {
    render(
      <UnitGrid
        propertyIdentifier="PRO1"
        rows={[
          row({
            status: 'OCCUPIED',
            tenantName: 'A. Tenant',
            vacancyDays: null,
          }),
        ]}
      />
    );
    expect(screen.queryByText(/day/)).not.toBeInTheDocument();
  });

  it('renders 50 units without collapsing', () => {
    const rows = Array.from({ length: 50 }, (_, i) =>
      row({ unitNumber: String(i + 1) })
    );
    render(<UnitGrid propertyIdentifier="PRO1" rows={rows} />);
    expect(screen.getAllByRole('row')).toHaveLength(51);
  });
});
