import { render, screen, fireEvent } from '@testing-library/react';
import { LetterPreviewStep } from '../LetterPreviewStep';

const baseProps = {
  givenBy: 'LANDLORD' as const,
  noticeDate: '2026-01-01',
  groundCode: 'RENOVATION',
  computedEndDate: '2026-03-01',
  effectiveEndDate: '',
  overrideReason: '',
  inspectionDate: '',
  onInspectionDateChange: vi.fn(),
  onBack: vi.fn(),
  onConfirm: vi.fn(),
  isSubmitting: false,
};

describe('LetterPreviewStep', () => {
  it('renders a summary of the termination details', () => {
    render(<LetterPreviewStep {...baseProps} />);

    expect(screen.getByText('Landlord')).toBeInTheDocument();
    expect(screen.getByText('2026-01-01')).toBeInTheDocument();
    expect(screen.getByText('RENOVATION')).toBeInTheDocument();
    expect(screen.getAllByText('2026-03-01').length).toBeGreaterThan(0);
  });

  it('falls back to the computed end date when no override was given', () => {
    render(<LetterPreviewStep {...baseProps} />);

    // computedEndDate appears twice: once as "computed end date", once as "effective end date"
    expect(screen.getAllByText('2026-03-01')).toHaveLength(2);
  });

  it('shows the overridden effective end date when provided', () => {
    render(
      <LetterPreviewStep
        {...baseProps}
        effectiveEndDate="2026-02-15"
        overrideReason="Tenant requested an earlier move-out"
      />
    );

    expect(screen.getByText('2026-02-15')).toBeInTheDocument();
    expect(
      screen.getByText('Tenant requested an earlier move-out')
    ).toBeInTheDocument();
  });

  it('calls onBack and onConfirm', () => {
    const onBack = vi.fn();
    const onConfirm = vi.fn();
    render(
      <LetterPreviewStep {...baseProps} onBack={onBack} onConfirm={onConfirm} />
    );

    fireEvent.click(screen.getByRole('button', { name: /back/i }));
    expect(onBack).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole('button', { name: /confirm/i }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('disables the confirm button while submitting', () => {
    render(<LetterPreviewStep {...baseProps} isSubmitting />);

    expect(screen.getByRole('button', { name: /terminating/i })).toBeDisabled();
  });
});
