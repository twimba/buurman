import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  UnitAllocationSettings,
  type UnitAllocationUnit,
} from '../UnitAllocationSettings';
import { AllocationBasis } from '@/types/allocation';

const defaultUnits: UnitAllocationUnit[] = [
  { identifier: 'UNT1', unitNumber: '1', areaValue: 50, sharePct: 50 },
  { identifier: 'UNT2', unitNumber: '2', areaValue: 50, sharePct: 50 },
];

const unitWithShare = (
  unitNumber: string,
  sharePct: number
): UnitAllocationUnit => ({
  identifier: `UNT${unitNumber}`,
  unitNumber,
  sharePct,
});

const unitWithArea = (
  unitNumber: string,
  areaValue: number | null
): UnitAllocationUnit => ({
  identifier: `UNT${unitNumber}`,
  unitNumber,
  areaValue,
});

const renderSettings = (
  overrides: {
    basis?: AllocationBasis;
    units?: UnitAllocationUnit[];
    onSave?: (request: unknown) => void;
  } = {}
) => {
  const onSave = overrides.onSave ?? vi.fn();
  render(
    <UnitAllocationSettings
      propertyIdentifier="PRO1"
      basis={overrides.basis ?? AllocationBasis.EQUAL}
      units={overrides.units ?? defaultUnits}
      onSave={onSave}
    />
  );
  return { onSave };
};

describe('UnitAllocationSettings', () => {
  it('shows a share column only for CUSTOM basis', async () => {
    renderSettings({ basis: AllocationBasis.EQUAL });
    expect(screen.queryByLabelText('Share')).not.toBeInTheDocument();

    await userEvent.selectOptions(
      screen.getByLabelText(/Split building costs by/),
      AllocationBasis.CUSTOM
    );

    expect(screen.getAllByLabelText('Share').length).toBeGreaterThan(0);
  });

  it('blocks saving CUSTOM shares that do not total 100', async () => {
    renderSettings({
      basis: AllocationBasis.CUSTOM,
      units: [unitWithShare('1', 60), unitWithShare('2', 30)],
    });

    expect(
      screen.getByText(
        'Custom shares must add up to 100%. They currently total 90%.'
      )
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
  });

  it('labels a unit with no floor area under AREA basis instead of rendering NaN', () => {
    renderSettings({
      basis: AllocationBasis.AREA,
      units: [unitWithArea('1', 100), unitWithArea('2', null)],
    });

    expect(screen.getByText('No floor area set')).toBeInTheDocument();
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });

  it("previews each unit's share of a sample amount", () => {
    renderSettings({
      basis: AllocationBasis.EQUAL,
      units: [unitWithArea('1', 50), unitWithArea('2', 50)],
    });

    expect(screen.getByText(/Each unit's share/)).toBeInTheDocument();
  });
});
