import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BulkCreateUnitsModal } from '../BulkCreateUnitsModal';
import type { PropertyIdentifier } from '@/types/unit';

const mutateMock = vi.fn();

let bulkCreateState: {
  mutate: typeof mutateMock;
  isPending: boolean;
  isSuccess: boolean;
  data: unknown;
  reset: () => void;
} = {
  mutate: mutateMock,
  isPending: false,
  isSuccess: false,
  data: undefined,
  reset: vi.fn(),
};

vi.mock('@/hooks/useUnitHooks', () => ({
  useBulkCreateUnits: () => bulkCreateState,
}));

const renderModal = (overrides: Partial<typeof bulkCreateState> = {}) => {
  bulkCreateState = {
    mutate: mutateMock,
    isPending: false,
    isSuccess: false,
    data: undefined,
    reset: vi.fn(),
    ...overrides,
  };
  return render(
    <BulkCreateUnitsModal
      propertyIdentifier={'PRO1' as PropertyIdentifier}
      street="Main Street 1"
      open
      onClose={vi.fn()}
      onCreated={vi.fn()}
    />
  );
};

describe('BulkCreateUnitsModal', () => {
  beforeEach(() => {
    mutateMock.mockClear();
  });

  it('previews the numbering before submitting', async () => {
    renderModal();
    await userEvent.clear(screen.getByLabelText('How many units?'));
    await userEvent.type(screen.getByLabelText('How many units?'), '3');
    expect(screen.getByText(/1, 2, 3/)).toBeInTheDocument();
  });

  it('always states that the existing unit keeps its history', () => {
    renderModal();
    expect(
      screen.getByText(/becomes #1 and keeps its contract, photos and history/)
    ).toBeInTheDocument();
  });

  it('submits count, pattern and type in one request', async () => {
    const mutate = vi.fn();
    renderModal({ mutate });
    await userEvent.type(screen.getByLabelText('How many units?'), '6');
    await userEvent.click(screen.getByRole('button', { name: 'Create units' }));
    expect(mutate).toHaveBeenCalledTimes(1);
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ count: 6, numberingPattern: 'NUMERIC' })
    );
  });

  it('rejects a count below 1', async () => {
    renderModal();
    await userEvent.clear(screen.getByLabelText('How many units?'));
    await userEvent.type(screen.getByLabelText('How many units?'), '0');
    expect(screen.getByRole('button', { name: 'Create units' })).toBeDisabled();
  });

  it('does not show the split reassurance when adding to an already multi-unit property', () => {
    render(
      <BulkCreateUnitsModal
        propertyIdentifier={'PRO1' as PropertyIdentifier}
        street="Main Street 1"
        open
        onClose={vi.fn()}
        onCreated={vi.fn()}
        mode="expand"
      />
    );
    expect(
      screen.queryByText(/becomes #1 and keeps its contract/)
    ).not.toBeInTheDocument();
  });
});
