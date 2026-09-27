import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { ExpenseForm } from '../ExpenseForm';
import { useUnits } from '@/hooks/useUnitHooks';

vi.mock('@/hooks/useUnitHooks', () => ({ useUnits: vi.fn() }));

vi.mock('@/hooks/useTeamDefaults', () => ({
  useTeamDefaults: () => ({
    defaultCurrency: 'EUR',
    defaultCountryCode: undefined,
    defaultDateFormat: 'YYYY-MM-DD',
    isLoading: false,
    activeTeamId: 'TEA1',
  }),
}));

vi.mock('@/components/common/PropertySelector', () => ({
  PropertySelector: ({ value }: { value: string }) => (
    <div data-testid="property-selector">{value}</div>
  ),
}));

vi.mock('@/components/common/ContactSelector', () => ({
  ContactSelector: () => <div data-testid="contact-selector" />,
}));

vi.mock('@buurman/ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@buurman/ui')>();
  return {
    ...actual,
    RichTextEditor: ({
      value,
      onChange,
    }: {
      value: string;
      onChange: (v: string) => void;
    }) => (
      <textarea
        aria-label="notes"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    ),
  };
});

const unit = (identifier: string, unitNumber: string) => ({
  identifier,
  unitNumber,
  name: undefined,
  unitType: 'APARTMENT',
  status: 'VACANT',
  tenantName: undefined,
  monthlyRent: undefined,
  vacancyDays: null,
});

const renderForm = (onSubmit = vi.fn()) => {
  renderWithProviders(
    <ExpenseForm
      onSubmit={onSubmit}
      onCancel={vi.fn()}
      isLoading={false}
      prefilledPropertyId="PRO1"
    />
  );
  return { onSubmit };
};

describe('ExpenseForm target selector', () => {
  it('hides the building/unit choice for a single-unit property', () => {
    vi.mocked(useUnits).mockReturnValue({
      data: [unit('UNT1', '1')],
    } as unknown as ReturnType<typeof useUnits>);

    renderForm();

    expect(screen.queryByText('The whole building')).not.toBeInTheDocument();
    expect(screen.queryByText('One unit')).not.toBeInTheDocument();
  });

  it('shows the choice, defaulting to the whole building, for a multi-unit property', () => {
    vi.mocked(useUnits).mockReturnValue({
      data: [unit('UNT1', '1'), unit('UNT2', '2')],
    } as unknown as ReturnType<typeof useUnits>);

    renderForm();

    expect(screen.getByText('The whole building')).toBeInTheDocument();
    const buildingRadio = screen.getByRole('radio', {
      name: 'The whole building',
    });
    expect(buildingRadio).toBeChecked();
    expect(screen.queryByLabelText('Unit')).not.toBeInTheDocument();
  });

  it('submits the chosen unitIdentifier when "One unit" is picked', async () => {
    vi.mocked(useUnits).mockReturnValue({
      data: [unit('UNT1', '1'), unit('UNT2', '2')],
    } as unknown as ReturnType<typeof useUnits>);

    const { onSubmit } = renderForm();

    await userEvent.click(screen.getByRole('radio', { name: 'One unit' }));
    await userEvent.selectOptions(screen.getByLabelText('Unit'), 'UNT2');

    await userEvent.type(screen.getByPlaceholderText('0.00'), '100');
    await userEvent.type(
      screen.getByPlaceholderText('e.g., Plumbing repair in bathroom'),
      'Roof repair'
    );
    const dateInput = document.querySelector(
      'input[type="date"]'
    ) as HTMLInputElement;
    await userEvent.type(dateInput, '2026-01-15');

    await userEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ unitIdentifier: 'UNT2' })
    );
  });
});
