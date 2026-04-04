import { render, screen } from '@testing-library/react';
import { FeatureGate } from '../FeatureGate';
import { useFeatureFlags } from '@/context/FeatureFlagContext';

vi.mock('@/context/FeatureFlagContext', () => ({
  useFeatureFlags: vi.fn(),
}));

const mockUseFeatureFlags = vi.mocked(useFeatureFlags);

describe('FeatureGate', () => {
  it('renders children when the flag is enabled', () => {
    mockUseFeatureFlags.mockReturnValue({
      flags: {},
      isEnabled: (key: string) => key === 'new-feature',
      getValue: () => null,
      isLoading: false,
    });

    render(
      <FeatureGate flag="new-feature">
        <div>Feature Content</div>
      </FeatureGate>
    );

    expect(screen.getByText('Feature Content')).toBeInTheDocument();
  });

  it('renders fallback when the flag is disabled', () => {
    mockUseFeatureFlags.mockReturnValue({
      flags: {},
      isEnabled: () => false,
      getValue: () => null,
      isLoading: false,
    });

    render(
      <FeatureGate flag="disabled-feature" fallback={<div>Coming Soon</div>}>
        <div>Feature Content</div>
      </FeatureGate>
    );

    expect(screen.queryByText('Feature Content')).not.toBeInTheDocument();
    expect(screen.getByText('Coming Soon')).toBeInTheDocument();
  });

  it('renders nothing when the flag is disabled and no fallback is provided', () => {
    mockUseFeatureFlags.mockReturnValue({
      flags: {},
      isEnabled: () => false,
      getValue: () => null,
      isLoading: false,
    });

    const { container } = render(
      <FeatureGate flag="disabled-feature">
        <div>Feature Content</div>
      </FeatureGate>
    );

    expect(screen.queryByText('Feature Content')).not.toBeInTheDocument();
    expect(container.innerHTML).toBe('');
  });
});
