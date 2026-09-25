import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { ReminderPreviewModal } from '../ReminderPreviewModal';
import { useReminderPreview } from '@/hooks/useTeamHooks';

vi.mock('@/hooks/useTeamHooks', () => ({ useReminderPreview: vi.fn() }));

const mockPreview = vi.mocked(useReminderPreview);

const loaded = (html: string, languageTag = 'en') =>
  ({
    data: {
      subject: 'Rent for 12 Example Street is overdue',
      html,
      languageTag,
      sampleTenantName: 'Sam Example',
    },
    isLoading: false,
    isError: false,
  }) as ReturnType<typeof useReminderPreview>;

const renderModal = () =>
  renderWithProviders(
    <ReminderPreviewModal
      open
      teamId="team_01TEST"
      tone="FIRM"
      offsetDays={14}
      onClose={vi.fn()}
    />
  );

describe('ReminderPreviewModal', () => {
  it('shows the rendered subject and body once loaded', () => {
    mockPreview.mockReturnValue(loaded('<p>Dear Sam Example</p>', 'nl'));

    renderModal();

    expect(
      screen.getByText('Rent for 12 Example Street is overdue')
    ).toBeInTheDocument();
    const frame = screen.getByTitle('Reminder preview');
    expect(frame).toHaveAttribute('srcdoc', '<p>Dear Sam Example</p>');
    // Sandboxed so the email's markup and CSS cannot reach the surrounding app.
    expect(frame).toHaveAttribute('sandbox', '');
  });

  it('says the preview is only an example and was not sent', () => {
    mockPreview.mockReturnValue(loaded('<p>body</p>'));

    renderModal();

    expect(screen.getByText(/Nothing has been sent/i)).toBeInTheDocument();
    expect(screen.getByText(/Sam Example/)).toBeInTheDocument();
  });

  it('reports a failure instead of rendering an empty frame', () => {
    mockPreview.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as ReturnType<typeof useReminderPreview>);

    renderModal();

    expect(screen.getByText(/could not be loaded/i)).toBeInTheDocument();
    expect(screen.queryByTitle('Reminder preview')).not.toBeInTheDocument();
  });
});
