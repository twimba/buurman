import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '@buurman/ui';
import { SavedFiltersDropdown } from '../SavedFiltersDropdown';
import * as savedFiltersApi from '@/generated/api/saved-contract-filters/saved-contract-filters';
import { renderWithProviders } from '@/test/test-utils';

describe('SavedFiltersDropdown', () => {
  it("applies a saved filter's criteria when selected", async () => {
    vi.spyOn(savedFiltersApi, 'getSavedContractFilters').mockResolvedValue([
      {
        identifier: 'SCF00000000000000000000001',
        name: 'Ending soon',
        criteria: { endingWithinDays: 90 },
        createdAt: '2026-03-01T12:00:00Z',
      },
    ]);
    const onApply = vi.fn();
    renderWithProviders(
      <ToastProvider>
        <SavedFiltersDropdown currentCriteria={{}} onApply={onApply} />
      </ToastProvider>
    );

    await userEvent.click(
      screen.getByRole('button', { name: /saved filters/i })
    );
    await waitFor(() => screen.getByText('Ending soon'));
    await userEvent.click(screen.getByText('Ending soon'));

    expect(onApply).toHaveBeenCalledWith({ endingWithinDays: 90 });
  });
});
