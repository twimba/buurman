import { render, screen, fireEvent } from '@testing-library/react';
import { WhoGivesNoticeStep } from '../WhoGivesNoticeStep';

describe('WhoGivesNoticeStep', () => {
  it('renders both landlord and tenant options', () => {
    render(
      <WhoGivesNoticeStep
        givenBy={undefined}
        onGivenByChange={vi.fn()}
        onNext={vi.fn()}
      />
    );

    expect(screen.getByText('Landlord')).toBeInTheDocument();
    expect(screen.getByText('Tenant')).toBeInTheDocument();
  });

  it('disables Next until a selection is made', () => {
    render(
      <WhoGivesNoticeStep
        givenBy={undefined}
        onGivenByChange={vi.fn()}
        onNext={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
  });

  it('calls onGivenByChange when an option is clicked', () => {
    const onGivenByChange = vi.fn();
    render(
      <WhoGivesNoticeStep
        givenBy={undefined}
        onGivenByChange={onGivenByChange}
        onNext={vi.fn()}
      />
    );

    fireEvent.click(screen.getByText('Tenant'));
    expect(onGivenByChange).toHaveBeenCalledWith('TENANT');
  });

  it('calls onNext when Next is clicked after a selection', () => {
    const onNext = vi.fn();
    render(
      <WhoGivesNoticeStep
        givenBy="LANDLORD"
        onGivenByChange={vi.fn()}
        onNext={onNext}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(onNext).toHaveBeenCalledTimes(1);
  });
});
