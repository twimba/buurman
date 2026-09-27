import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { PropertyExpensesTab } from '../PropertyExpensesTab';
import { useExpensesByProperty } from '@/hooks/useExpenseHooks';
import { useProperty } from '@/hooks/usePropertyHooks';
import { useExpenseAllocations } from '@/hooks/useAllocationHooks';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';

vi.mock('@/hooks/useExpenseHooks', () => ({ useExpensesByProperty: vi.fn() }));
vi.mock('@/hooks/usePropertyHooks', () => ({ useProperty: vi.fn() }));
vi.mock('@/hooks/useAllocationHooks', () => ({
  useExpenseAllocations: vi.fn(),
  useUnitDetailsBatch: vi.fn(() => []),
  useUpdatePropertyAllocation: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));
vi.mock('@/context/TeamContext', () => ({ useTeam: vi.fn() }));
vi.mock('@/hooks/useFormatDate', () => ({ useFormatDate: vi.fn() }));

const expense = (overrides = {}) => ({
  identifier: 'EXP01',
  category: 'MAINTENANCE',
  amount: 120,
  currency: 'EUR',
  expenseDate: '2026-01-01',
  description: 'Roof repair',
  documents: [],
  ...overrides,
});

const buildingProperty = (unitCount: number) => ({
  identifier: 'PRO1',
  unitCount,
  allocationBasis: 'EQUAL',
  units: Array.from({ length: unitCount }, (_, i) => ({
    identifier: `UNT${i + 1}`,
    unitNumber: String(i + 1),
    unitType: 'APARTMENT',
    status: 'OCCUPIED',
  })),
});

beforeEach(() => {
  vi.mocked(useTeam).mockReturnValue({
    canEditData: true,
  } as unknown as ReturnType<typeof useTeam>);
  vi.mocked(useFormatDate).mockReturnValue({
    formatDate: (d: string) => d,
  } as unknown as ReturnType<typeof useFormatDate>);
});

describe('PropertyExpensesTab allocation', () => {
  it('shows no allocation UI at all for a single-unit property', () => {
    vi.mocked(useProperty).mockReturnValue({
      data: buildingProperty(1),
    } as unknown as ReturnType<typeof useProperty>);
    vi.mocked(useExpensesByProperty).mockReturnValue({
      data: [expense()],
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useExpensesByProperty>);

    renderWithProviders(<PropertyExpensesTab propertyId="PRO1" />);

    expect(
      screen.queryByText('Cost allocation settings')
    ).not.toBeInTheDocument();
    expect(screen.queryByText('View split')).not.toBeInTheDocument();
  });

  it('offers the cost allocation settings toggle for a multi-unit property', async () => {
    vi.mocked(useProperty).mockReturnValue({
      data: buildingProperty(2),
    } as unknown as ReturnType<typeof useProperty>);
    vi.mocked(useExpensesByProperty).mockReturnValue({
      data: [expense()],
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useExpensesByProperty>);

    renderWithProviders(<PropertyExpensesTab propertyId="PRO1" />);

    const toggle = screen.getByText('Cost allocation settings');
    await userEvent.click(toggle);

    expect(
      screen.getByLabelText(/Split building costs by/)
    ).toBeInTheDocument();
  });

  it("shows a building-level expense's per-unit split when expanded", async () => {
    vi.mocked(useProperty).mockReturnValue({
      data: buildingProperty(2),
    } as unknown as ReturnType<typeof useProperty>);
    vi.mocked(useExpensesByProperty).mockReturnValue({
      data: [expense({ unitIdentifier: undefined })],
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useExpensesByProperty>);
    vi.mocked(useExpenseAllocations).mockReturnValue({
      data: [
        {
          identifier: 'ALLOC1',
          unitIdentifier: 'UNT1',
          unitNumber: '1',
          amount: 60,
          amountCurrency: 'EUR',
          basis: 'EQUAL',
          requestedBasis: 'EQUAL',
          warnings: [],
          createdAt: '2026-01-01',
        },
        {
          identifier: 'ALLOC2',
          unitIdentifier: 'UNT2',
          unitNumber: '2',
          amount: 60,
          amountCurrency: 'EUR',
          basis: 'EQUAL',
          requestedBasis: 'EQUAL',
          warnings: [],
          createdAt: '2026-01-01',
        },
      ],
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useExpenseAllocations>);

    renderWithProviders(<PropertyExpensesTab propertyId="PRO1" />);

    await userEvent.click(screen.getByText('View split'));

    expect(screen.getByText('Unit 1')).toBeInTheDocument();
    expect(screen.getByText('Unit 2')).toBeInTheDocument();
  });
});
