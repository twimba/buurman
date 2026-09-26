import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { RegulationSummary } from '../RegulationSummary';
import type { RentRegulationCountryDetailResponse } from '@/types/rentRegulation';

const base: RentRegulationCountryDetailResponse = {
  identifier: 'rrc_01TEST',
  countryCode: 'PT',
  countryName: 'Portugal',
  hasRegionalRegulations: false,
  stale: false,
  regions: [],
  rules: [],
};

describe('RegulationSummary late-fee regime', () => {
  it('shows the capped regime with its maximum, notes and notice period', () => {
    renderWithProviders(
      <RegulationSummary
        country={{
          ...base,
          lateFeePolicy: 'CAPPED',
          lateFeeMaxPercentage: 20,
          lateFeeNotes: 'Art. 1041 CC. Advisory.',
          formalNoticeDays: 8,
        }}
      />
    );
    expect(screen.getByText('Late fees')).toBeInTheDocument();
    expect(screen.getByText('Late fees capped')).toBeInTheDocument();
    expect(
      screen.getByText('Maximum 20% of the amount due')
    ).toBeInTheDocument();
    expect(screen.getByText('Art. 1041 CC. Advisory.')).toBeInTheDocument();
    expect(
      screen.getByText('Formal notice period: 8 days')
    ).toBeInTheDocument();
  });

  it('shows a forbidden regime without a percentage', () => {
    renderWithProviders(
      <RegulationSummary
        country={{ ...base, countryCode: 'FR', lateFeePolicy: 'FORBIDDEN' }}
      />
    );
    expect(screen.getByText('Late fees not permitted')).toBeInTheDocument();
    expect(screen.queryByText(/of the amount due/)).not.toBeInTheDocument();
  });

  it('hides the block when the regime is unknown', () => {
    renderWithProviders(
      <RegulationSummary country={{ ...base, lateFeePolicy: 'UNKNOWN' }} />
    );
    expect(screen.queryByText('Late fees')).not.toBeInTheDocument();
  });
});
