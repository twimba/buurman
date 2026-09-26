import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { EntityExportControls } from '../EntityExportControls';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { downloadBlob } from '@/utils/downloadBlob';

vi.mock('@/context/FeatureFlagContext', () => ({ useFeatureFlags: vi.fn() }));
vi.mock('@/utils/downloadBlob', () => ({ downloadBlob: vi.fn() }));
vi.mock('@/hooks/useGoogleSheetsExport', () => ({
  useGoogleSheetsExport: () => ({
    triggerExport: vi.fn(),
    isExporting: false,
    lastResult: null,
    clearLastResult: vi.fn(),
  }),
}));

const mockFlags = vi.mocked(useFeatureFlags);

function flags(enabled: boolean) {
  mockFlags.mockReturnValue({
    flags: {},
    isEnabled: () => enabled,
    getValue: () => null,
    isLoading: false,
  });
}

describe('EntityExportControls', () => {
  it('labels the trigger so several exports can sit side by side', () => {
    flags(true);
    renderWithProviders(
      <EntityExportControls
        filenameStem="deposits"
        label="Export deposits"
        csv={vi.fn()}
        xlsx={vi.fn()}
        googleSheet={vi.fn()}
      />
    );
    expect(
      screen.getByRole('button', { name: /Export deposits/ })
    ).toBeInTheDocument();
  });

  it('downloads the CSV under the filename stem when only CSV is available', async () => {
    const user = userEvent.setup();
    flags(false);
    const blob = new Blob(['a,b'], { type: 'text/csv' });
    const csv = vi.fn().mockResolvedValue(blob);
    renderWithProviders(
      <EntityExportControls
        filenameStem="deposits"
        csv={csv}
        xlsx={vi.fn()}
        googleSheet={vi.fn()}
      />
    );
    await user.click(screen.getByRole('button', { name: /CSV/ }));
    await waitFor(() =>
      expect(downloadBlob).toHaveBeenCalledWith(blob, 'deposits.csv')
    );
  });
});
