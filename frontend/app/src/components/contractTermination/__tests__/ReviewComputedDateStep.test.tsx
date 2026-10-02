import { render, screen, fireEvent } from '@testing-library/react';
import { ReviewComputedDateStep } from '../ReviewComputedDateStep';
import { useTerminationPreview } from '@/hooks/useContractTerminationHooks';

vi.mock('@/hooks/useContractTerminationHooks', () => ({
  useTerminationPreview: vi.fn(),
}));

const mockedUseTerminationPreview =
  useTerminationPreview as unknown as ReturnType<typeof vi.fn>;

const baseProps = {
  contractId: 'CTR1',
  givenBy: 'LANDLORD' as const,
  noticeDate: '2026-01-01',
  groundCode: '',
  onGroundCodeChange: vi.fn(),
  effectiveEndDate: '',
  onEffectiveEndDateChange: vi.fn(),
  overrideReason: '',
  onOverrideReasonChange: vi.fn(),
  onComputedEndDateChange: vi.fn(),
  onNext: vi.fn(),
  onBack: vi.fn(),
};

describe('ReviewComputedDateStep', () => {
  beforeEach(() => {
    mockedUseTerminationPreview.mockReset();
  });

  it('shows a loading state while the preview is being calculated', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    });

    render(<ReviewComputedDateStep {...baseProps} />);

    expect(screen.getByText(/calculating notice period/i)).toBeInTheDocument();
  });

  it('displays the computed end date, notice days and plain-language source', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(<ReviewComputedDateStep {...baseProps} />);

    expect(screen.getByText('2026-03-01')).toBeInTheDocument();
    expect(screen.getByText(/30 days/i)).toBeInTheDocument();
    expect(
      screen.getByText(/based on your country's notice-period rules/i)
    ).toBeInTheDocument();
  });

  it('does NOT show the override reason field before the landlord edits the end date', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(<ReviewComputedDateStep {...baseProps} effectiveEndDate="" />);

    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();
  });

  it('does NOT reveal the override reason field when the edited date is the same as computed', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(
      <ReviewComputedDateStep {...baseProps} effectiveEndDate="2026-03-01" />
    );

    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();
  });

  it('does NOT reveal the override reason field when the edited date is LATER than computed', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(
      <ReviewComputedDateStep {...baseProps} effectiveEndDate="2026-04-01" />
    );

    expect(
      screen.queryByLabelText(/reason for the earlier end date/i)
    ).not.toBeInTheDocument();
  });

  it('REVEALS the override reason field once the edited date is earlier than computed', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(
      <ReviewComputedDateStep {...baseProps} effectiveEndDate="2026-02-15" />
    );

    expect(
      screen.getByLabelText(/reason for the earlier end date/i)
    ).toBeInTheDocument();
  });

  it('disables Next when the override is earlier but no reason has been given', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(
      <ReviewComputedDateStep
        {...baseProps}
        effectiveEndDate="2026-02-15"
        overrideReason=""
      />
    );

    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
  });

  it('enables Next once an override reason has been provided', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });

    render(
      <ReviewComputedDateStep
        {...baseProps}
        effectiveEndDate="2026-02-15"
        overrideReason="Tenant requested an earlier move-out"
      />
    );

    expect(screen.getByRole('button', { name: /next/i })).toBeEnabled();
  });

  it('clears a stale override reason once the date is no longer earlier than computed', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });
    const onOverrideReasonChange = vi.fn();

    const { rerender } = render(
      <ReviewComputedDateStep
        {...baseProps}
        effectiveEndDate="2026-02-15"
        overrideReason="Tenant requested an earlier move-out"
        onOverrideReasonChange={onOverrideReasonChange}
      />
    );
    expect(onOverrideReasonChange).not.toHaveBeenCalled();

    // Landlord edits the date back to on/after the computed date — the reason is now stale.
    rerender(
      <ReviewComputedDateStep
        {...baseProps}
        effectiveEndDate="2026-03-01"
        overrideReason="Tenant requested an earlier move-out"
        onOverrideReasonChange={onOverrideReasonChange}
      />
    );

    expect(onOverrideReasonChange).toHaveBeenCalledWith('');
  });

  it('never carries a non-empty override reason while the field is hidden', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });
    const onOverrideReasonChange = vi.fn();

    // Simulates state that is already stale by the time this component mounts (e.g. a remount
    // with leftover parent state) — the invariant must hold regardless of how it got here.
    render(
      <ReviewComputedDateStep
        {...baseProps}
        effectiveEndDate=""
        overrideReason="stale text"
        onOverrideReasonChange={onOverrideReasonChange}
      />
    );

    expect(onOverrideReasonChange).toHaveBeenCalledWith('');
  });

  it('calls onBack when Back is clicked', () => {
    mockedUseTerminationPreview.mockReturnValue({
      data: {
        computedEndDate: '2026-03-01',
        noticeDays: 30,
        groundsRequired: false,
        source: 'CATALOG_RULE',
      },
      isLoading: false,
      isError: false,
    });
    const onBack = vi.fn();

    render(<ReviewComputedDateStep {...baseProps} onBack={onBack} />);

    fireEvent.click(screen.getByRole('button', { name: /back/i }));
    expect(onBack).toHaveBeenCalledTimes(1);
  });
});
