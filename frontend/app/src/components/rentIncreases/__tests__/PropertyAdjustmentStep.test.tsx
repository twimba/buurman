import { render, screen } from '@testing-library/react';
import { PropertyAdjustmentStep } from '../PropertyAdjustmentStep';
import type { RentIncreaseContractPreview } from '@/types/rentIncrease';

const contract = (
  overrides: Partial<RentIncreaseContractPreview>
): RentIncreaseContractPreview => ({
  contractIdentifier: 'CTR1',
  propertyIdentifier: 'PRO1',
  propertyName: '123 Main St',
  propertyAddress: '123 Main St, Amsterdam',
  propertyCountryCode: 'NL',
  currentRentAmount: 1000,
  currency: 'EUR',
  ...overrides,
});

describe('PropertyAdjustmentStep — multi-unit disambiguation', () => {
  it('renders one row per contract for a building with several active leases', () => {
    render(
      <PropertyAdjustmentStep
        contracts={[
          contract({ contractIdentifier: 'CTR1' }),
          contract({ contractIdentifier: 'CTR2' }),
          contract({ contractIdentifier: 'CTR3' }),
        ]}
        increases={[]}
        onIncreaseChange={vi.fn()}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.getAllByText('123 Main St')).toHaveLength(3);
  });

  it('labels rows sharing a property as distinct leases, never a fabricated unit name', () => {
    render(
      <PropertyAdjustmentStep
        contracts={[
          contract({ contractIdentifier: 'CTR1' }),
          contract({ contractIdentifier: 'CTR2' }),
        ]}
        increases={[]}
        onIncreaseChange={vi.fn()}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(
      screen.getByText('Lease 1 of 2 at this property')
    ).toBeInTheDocument();
    expect(
      screen.getByText('Lease 2 of 2 at this property')
    ).toBeInTheDocument();
  });

  it('shows no lease label when a property has only one contract in the list', () => {
    render(
      <PropertyAdjustmentStep
        contracts={[contract({ contractIdentifier: 'CTR1' })]}
        increases={[]}
        onIncreaseChange={vi.fn()}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.queryByText(/Lease \d+ of \d+/)).not.toBeInTheDocument();
  });
});
