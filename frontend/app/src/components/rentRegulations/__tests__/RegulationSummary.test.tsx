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

describe('RegulationSummary tenancy rules', () => {
  it('groups tenancy rules by topic and shows value, date and legal basis', () => {
    renderWithProviders(
      <RegulationSummary
        country={{
          ...base,
          tenancyRules: [
            {
              identifier: 'rrt_01TEST',
              topic: 'TENANCY_DURATION',
              label: 'Minimum fixed term',
              value: '5 years',
              effectiveFrom: '2026-01-01',
              legalBasis: 'MRG § 29',
            },
          ],
        }}
      />
    );
    expect(screen.getByText('Tenancy rules')).toBeInTheDocument();
    expect(screen.getByText('Tenancy duration')).toBeInTheDocument();
    expect(screen.getByText('Minimum fixed term')).toBeInTheDocument();
    expect(screen.getByText('5 years')).toBeInTheDocument();
    expect(screen.getByText(/MRG § 29/)).toBeInTheDocument();

    // Derive the expected date independently of how the component parses
    // `effectiveFrom` — built from explicit local-time components (year,
    // month, day), not from the ISO string. `effectiveFrom` is a date-only
    // value with no time component: `new Date('2026-01-01')` parses as UTC
    // midnight, which rolls back a day in any negative-UTC-offset timezone.
    // If the component regresses to that parsing, this expectation (still
    // "Jan 1, 2026" wherever the test runs) stops matching and this
    // assertion fails — computing the expectation the same way the
    // component does would hide exactly that bug.
    const expectedDate = new Date(2026, 0, 1).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
    const expectedText = `From ${expectedDate}`;
    expect(
      screen.getByText((_content, element) => {
        if (!element?.textContent?.includes(expectedText)) {
          return false;
        }
        // Only match the innermost element containing the text — the date is
        // rendered as a text node alongside an em dash, so the parent <li>
        // and <ul> also "contain" it and would otherwise match too.
        return Array.from(element.children).every(
          (child) => !child.textContent?.includes(expectedText)
        );
      })
    ).toBeInTheDocument();
  });

  it('hides the section when there are no tenancy rules', () => {
    renderWithProviders(
      <RegulationSummary country={{ ...base, tenancyRules: [] }} />
    );
    expect(screen.queryByText('Tenancy rules')).not.toBeInTheDocument();
  });

  it('hides the section when tenancyRules is absent', () => {
    renderWithProviders(<RegulationSummary country={base} />);
    expect(screen.queryByText('Tenancy rules')).not.toBeInTheDocument();
  });
});
