import { fireEvent, render, screen } from '@testing-library/react';
import { UnitCharacteristicsForm } from '../UnitCharacteristicsForm';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { UnitStatus, UnitType } from '@/types/unit';
import type { UpdateUnitRequest } from '@/types/unit';

vi.mock('@/context/FeatureFlagContext', () => ({
  useFeatureFlags: vi.fn(),
}));

const mockUseFeatureFlags = vi.mocked(useFeatureFlags);

const baseValue: UpdateUnitRequest = {
  unitNumber: '1',
  unitType: UnitType.APARTMENT,
  status: UnitStatus.OCCUPIED,
  areaValue: 75,
  areaUnit: 'sqm',
};

// The Floor area section is the first CollapsibleSection rendered, and every section is
// collapsed by default, so its field only mounts once that first toggle is expanded.
const expandFloorAreaSection = () => {
  fireEvent.click(screen.getAllByRole('button', { expanded: false })[0]);
};

describe('UnitCharacteristicsForm', () => {
  beforeEach(() => {
    mockUseFeatureFlags.mockReturnValue({
      flags: {},
      isEnabled: () => true,
      getValue: () => null,
      isLoading: false,
    });
  });

  it('calls onChange with the new floor area when the input is edited', () => {
    const onChange = vi.fn();
    render(<UnitCharacteristicsForm value={baseValue} onChange={onChange} />);
    expandFloorAreaSection();

    const areaInput = screen.getByRole('spinbutton');
    fireEvent.change(areaInput, { target: { value: '90' } });

    expect(onChange).toHaveBeenCalledWith('areaValue', 90);
  });

  it('renders an empty floor area input rather than NaN or "null" when areaValue is null', () => {
    render(
      <UnitCharacteristicsForm
        value={{ ...baseValue, areaValue: null as unknown as undefined }}
        onChange={vi.fn()}
      />
    );
    expandFloorAreaSection();

    const areaInput = screen.getByRole('spinbutton') as HTMLInputElement;
    expect(areaInput.value).toBe('');
  });

  it('disables every input when disabled is true', () => {
    render(
      <UnitCharacteristicsForm value={baseValue} onChange={vi.fn()} disabled />
    );

    // Sections are collapsed by default -- expand every one so its inputs mount.
    screen.getAllByRole('button', { expanded: false }).forEach((toggle) => {
      fireEvent.click(toggle);
    });

    screen
      .getAllByRole('spinbutton')
      .forEach((el) => expect(el).toBeDisabled());
    screen.getAllByRole('combobox').forEach((el) => expect(el).toBeDisabled());
    screen.getAllByRole('switch').forEach((el) => expect(el).toBeDisabled());
    screen.getAllByRole('textbox').forEach((el) => expect(el).toBeDisabled());
    document
      .querySelectorAll('input[type="date"]')
      .forEach((el) => expect(el).toBeDisabled());
  });

  it('renders no header at all when title is omitted', () => {
    render(<UnitCharacteristicsForm value={baseValue} onChange={vi.fn()} />);
    expect(
      screen.queryByRole('button', { name: /Split/ })
    ).not.toBeInTheDocument();
  });

  it('shows the split action next to the title when onSplit is provided', () => {
    const onSplit = vi.fn();
    render(
      <UnitCharacteristicsForm
        value={baseValue}
        onChange={vi.fn()}
        title="Characteristics"
        onSplit={onSplit}
      />
    );
    expect(screen.getByText('Characteristics')).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole('button', { name: 'Split into multiple units' })
    );
    expect(onSplit).toHaveBeenCalledTimes(1);
  });

  it('hides the split action when MULTI_UNIT is disabled, even with onSplit provided', () => {
    mockUseFeatureFlags.mockReturnValue({
      flags: {},
      isEnabled: () => false,
      getValue: () => null,
      isLoading: false,
    });
    render(
      <UnitCharacteristicsForm
        value={baseValue}
        onChange={vi.fn()}
        title="Characteristics"
        onSplit={vi.fn()}
      />
    );
    expect(
      screen.queryByRole('button', { name: 'Split into multiple units' })
    ).not.toBeInTheDocument();
  });
});
