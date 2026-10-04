import { render, screen, fireEvent } from '@testing-library/react';
import { NoticeDateAndGroundStep } from '../NoticeDateAndGroundStep';

describe('NoticeDateAndGroundStep', () => {
  it('renders the notice date and ground code inputs', () => {
    render(
      <NoticeDateAndGroundStep
        noticeDate="2026-01-01"
        onNoticeDateChange={vi.fn()}
        groundCode=""
        onGroundCodeChange={vi.fn()}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.getByLabelText(/notice date/i)).toHaveValue('2026-01-01');
    expect(screen.getByLabelText(/ground/i)).toHaveValue('');
  });

  it('calls onNoticeDateChange and onGroundCodeChange on edits', () => {
    const onNoticeDateChange = vi.fn();
    const onGroundCodeChange = vi.fn();
    render(
      <NoticeDateAndGroundStep
        noticeDate="2026-01-01"
        onNoticeDateChange={onNoticeDateChange}
        groundCode=""
        onGroundCodeChange={onGroundCodeChange}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    fireEvent.change(screen.getByLabelText(/notice date/i), {
      target: { value: '2026-02-01' },
    });
    expect(onNoticeDateChange).toHaveBeenCalledWith('2026-02-01');

    fireEvent.change(screen.getByLabelText(/ground/i), {
      target: { value: 'RENOVATION' },
    });
    expect(onGroundCodeChange).toHaveBeenCalledWith('RENOVATION');
  });

  it('calls onNext and onBack', () => {
    const onNext = vi.fn();
    const onBack = vi.fn();
    render(
      <NoticeDateAndGroundStep
        noticeDate="2026-01-01"
        onNoticeDateChange={vi.fn()}
        groundCode=""
        onGroundCodeChange={vi.fn()}
        onNext={onNext}
        onBack={onBack}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /back/i }));
    expect(onBack).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(onNext).toHaveBeenCalledTimes(1);
  });

  it('disables Next when notice date is empty', () => {
    render(
      <NoticeDateAndGroundStep
        noticeDate=""
        onNoticeDateChange={vi.fn()}
        groundCode=""
        onGroundCodeChange={vi.fn()}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
  });
});
