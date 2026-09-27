import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { SelfOccupancyModal } from '../SelfOccupancyModal';
import {
  useCreateOccupancyPeriod,
  useOccupancyPeriods,
} from '@/hooks/useOccupancyPeriodHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { useUnits } from '@/hooks/useUnitHooks';

vi.mock('@/hooks/useOccupancyPeriodHooks', () => ({
  useOccupancyPeriods: vi.fn(),
  useCreateOccupancyPeriod: vi.fn(),
}));
vi.mock('@/hooks/useContractHooks', () => ({ useContracts: vi.fn() }));
vi.mock('@/hooks/useUnitHooks', () => ({ useUnits: vi.fn() }));
vi.mock('@/hooks/useFormatDate', () => ({
  useFormatDate: () => ({ formatDate: (d: string) => d }),
}));

const unit = (identifier: string, unitNumber: string) => ({
  identifier,
  unitNumber,
  name: undefined,
  unitType: 'APARTMENT',
  status: 'VACANT',
});

beforeEach(() => {
  vi.mocked(useOccupancyPeriods).mockReturnValue({
    data: [],
  } as unknown as ReturnType<typeof useOccupancyPeriods>);
  vi.mocked(useContracts).mockReturnValue({
    data: { content: [] },
  } as unknown as ReturnType<typeof useContracts>);
});

describe('SelfOccupancyModal unit choice', () => {
  it('never asks for a unit on a single-unit property', () => {
    vi.mocked(useUnits).mockReturnValue({
      data: [unit('UNT1', '1')],
    } as unknown as ReturnType<typeof useUnits>);
    vi.mocked(useCreateOccupancyPeriod).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useCreateOccupancyPeriod>);

    renderWithProviders(
      <SelfOccupancyModal propertyIdentifier="PRO1" onClose={vi.fn()} />
    );

    expect(screen.queryByText('Unit')).not.toBeInTheDocument();
  });

  it('blocks submitting without a unit chosen on a multi-unit property', async () => {
    vi.mocked(useUnits).mockReturnValue({
      data: [unit('UNT1', '1'), unit('UNT2', '2')],
    } as unknown as ReturnType<typeof useUnits>);
    const mutate = vi.fn();
    vi.mocked(useCreateOccupancyPeriod).mockReturnValue({
      mutate,
      isPending: false,
    } as unknown as ReturnType<typeof useCreateOccupancyPeriod>);

    renderWithProviders(
      <SelfOccupancyModal propertyIdentifier="PRO1" onClose={vi.fn()} />
    );

    await userEvent.click(screen.getByRole('button', { name: /confirm/i }));

    expect(
      screen.getByText('Choose which unit this occupancy period is for.')
    ).toBeInTheDocument();
    expect(mutate).not.toHaveBeenCalled();
  });

  it('submits the chosen unitIdentifier', async () => {
    vi.mocked(useUnits).mockReturnValue({
      data: [unit('UNT1', '1'), unit('UNT2', '2')],
    } as unknown as ReturnType<typeof useUnits>);
    const mutate = vi.fn();
    vi.mocked(useCreateOccupancyPeriod).mockReturnValue({
      mutate,
      isPending: false,
    } as unknown as ReturnType<typeof useCreateOccupancyPeriod>);

    renderWithProviders(
      <SelfOccupancyModal propertyIdentifier="PRO1" onClose={vi.fn()} />
    );

    await userEvent.selectOptions(screen.getByLabelText('Unit'), 'UNT2');
    await userEvent.click(screen.getByRole('button', { name: /confirm/i }));

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ unitIdentifier: 'UNT2' }),
      expect.anything()
    );
  });
});
