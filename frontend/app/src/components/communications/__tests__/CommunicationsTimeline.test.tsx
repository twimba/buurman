import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { CommunicationsTimeline } from '../CommunicationsTimeline';

// The house pattern for components that format dates (see contracts/__tests__/DepositCard.test.tsx).
// Stubbing the hook rather than the output keeps the assertion sharp: the test proves the
// component routes through the preference-aware formatter instead of calling toLocaleDateString,
// which would silently use the browser's timezone and disagree with the rest of the page.
vi.mock('@/hooks/useFormatDate', () => ({
  useFormatDate: () => ({
    formatDate: (value: string) => `date:${value}`,
    formatDateTime: (value: string) => `datetime:${value}`,
    formatRelative: (value: string) => `relative:${value}`,
  }),
}));

const row = (overrides = {}) => ({
  identifier: 'ntf_01JTEST000000000000000001',
  notificationType: 'PAYMENT_REMINDER',
  channel: 'EMAIL',
  audience: 'CONTACT',
  status: 'DELIVERED',
  opened: false,
  resend: false,
  createdAt: '2026-10-15T09:00:00Z',
  ...overrides,
});

describe('CommunicationsTimeline', () => {
  it('says why it is empty, not merely that it is', () => {
    renderWithProviders(
      <CommunicationsTimeline communications={[]} isLoading={false} />
    );

    // No backfill exists, so a pre-existing payment shows nothing. Without this line a
    // landlord concludes no reminder went out and sends a duplicate.
    expect(
      screen.getByText(/only messages sent from now on/i)
    ).toBeInTheDocument();
  });

  it('shows Opened when the message was read', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ status: 'DELIVERED', opened: true })]}
        isLoading={false}
      />
    );

    expect(screen.getByText('Opened')).toBeInTheDocument();
  });

  it.each([
    ['PENDING', 'Queued'],
    ['QUEUED', 'Queued'],
    ['SENT', 'Sent'],
    ['DELIVERED', 'Delivered'],
    ['BOUNCED', 'Not delivered'],
    ['REJECTED', 'Not delivered'],
    ['FAILED', 'Failed'],
  ])('renders %s as %s', (status, label) => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ status })]}
        isLoading={false}
      />
    );

    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('does not claim an SMS was unopened', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[
          row({ channel: 'SMS', status: 'SENT', opened: false }),
        ]}
        isLoading={false}
      />
    );

    // Twilio never reports opens. Showing an email-shaped "not opened" state for an SMS
    // would read as a failure when nothing is wrong.
    expect(screen.getByText('Sent')).toBeInTheDocument();
    expect(screen.queryByText('Not delivered')).not.toBeInTheDocument();
  });

  it('shows when each message was sent, in the user’s own date format', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ createdAt: '2026-10-15T09:00:00Z' })]}
        isLoading={false}
      />
    );

    // Without a date the landlord cannot tell this morning's reminder from March's,
    // which is the whole reason the panel exists. The visible label is relative; the exact
    // timestamp lives in the title, formatted with the user's preferences rather than the
    // browser's timezone, so this panel agrees with every other date on the page.
    const time = screen.getByText('relative:2026-10-15T09:00:00Z');
    expect(time).toHaveAttribute('datetime', '2026-10-15T09:00:00Z');
    expect(time).toHaveAttribute('title', 'datetime:2026-10-15T09:00:00Z');
  });

  it('does not label a demo-blocked message as failed', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ status: 'DEMO_BLOCKED' })]}
        isLoading={false}
      />
    );

    // Every row of every timeline on a demo team would otherwise read "Failed".
    expect(screen.queryByText('Failed')).not.toBeInTheDocument();
    expect(screen.getByText('Not sent (demo)')).toBeInTheDocument();
  });

  it('distinguishes a failed request from an empty timeline', () => {
    renderWithProviders(
      <CommunicationsTimeline communications={[]} isLoading={false} isError />
    );

    // "Nothing sent yet" is a confident claim; a 403 or 500 must not make it.
    expect(
      screen.queryByText(/only messages sent from now on/i)
    ).not.toBeInTheDocument();
    expect(screen.getByText(/could not be loaded/i)).toBeInTheDocument();
  });

  it('translates the notification type rather than showing the enum', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[
          row({ notificationType: 'CONTRACT_ROLLED_OVER_TO_INDEFINITE' }),
        ]}
        isLoading={false}
      />
    );

    expect(
      screen.queryByText('CONTRACT_ROLLED_OVER_TO_INDEFINITE')
    ).not.toBeInTheDocument();
    expect(screen.getByText('Contract rolled over')).toBeInTheDocument();
  });

  it('shows the subject so a row is identifiable beyond its type', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[
          row({ subject: 'Payment overdue for Keizersgracht 123-B' }),
        ]}
        isLoading={false}
      />
    );

    expect(
      screen.getByText('Payment overdue for Keizersgracht 123-B')
    ).toBeInTheDocument();
  });

  it('explains a bounce in words, keeping the provider string for whoever needs it', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[
          row({
            status: 'BOUNCED',
            providerError: '550 5.1.1 mailbox does not exist',
          }),
        ]}
        isLoading={false}
      />
    );

    // A bounce the landlord cannot explain is one they cannot act on — but "550 5.1.1" is
    // no more actionable than silence, so the visible line says what it means for them and
    // the raw string stays available on hover.
    const reason = screen.getByText(/did not accept it/i);
    expect(reason).toBeInTheDocument();
    expect(reason).toHaveAttribute('title', '550 5.1.1 mailbox does not exist');
  });

  describe('audience', () => {
    it('shows what went to the tenant, not the internal copies of the same send', () => {
      renderWithProviders(
        <CommunicationsTimeline
          communications={[
            row({
              identifier: 'ntf_tenant',
              audience: 'CONTACT',
              recipientEmail: 'tenant@example.com',
            }),
            row({
              identifier: 'ntf_admin',
              audience: 'TEAM',
              recipientEmail: 'admin@landlord.example',
            }),
            row({
              identifier: 'ntf_editor',
              audience: 'TEAM',
              recipientEmail: 'editor@landlord.example',
            }),
          ]}
          isLoading={false}
        />
      );

      // sendToTeam emits one row per team member per channel. Rendering those as timeline
      // entries presents "we told your colleague" as "we told your tenant".
      expect(screen.getByText(/tenant@example\.com/)).toBeInTheDocument();
      expect(
        screen.queryByText(/admin@landlord\.example/)
      ).not.toBeInTheDocument();
      expect(
        screen.getByText(/Also copied to your team \(2\)/)
      ).toBeInTheDocument();
    });

    it('does not offer resend on an internal copy', async () => {
      const onResend = vi.fn();
      renderWithProviders(
        <CommunicationsTimeline
          communications={[row({ audience: 'TEAM' })]}
          isLoading={false}
          onResend={onResend}
        />
      );

      // Resending an internal copy sends a colleague a duplicate while the landlord
      // believes they are chasing the tenant.
      expect(
        screen.queryByRole('button', { name: /resend/i })
      ).not.toBeInTheDocument();
    });

    it('says so when everything sent so far was internal', () => {
      renderWithProviders(
        <CommunicationsTimeline
          communications={[row({ audience: 'TEAM' })]}
          isLoading={false}
        />
      );

      expect(
        screen.getByText(/nothing sent to your tenant yet/i)
      ).toBeInTheDocument();
    });

    it('never presents a recipient-less system record as tenant contact', () => {
      renderWithProviders(
        <CommunicationsTimeline
          communications={[row({ audience: 'UNKNOWN' })]}
          isLoading={false}
        />
      );

      expect(
        screen.getByText(/nothing sent to your tenant yet/i)
      ).toBeInTheDocument();
    });
  });

  describe('resend', () => {
    it('confirms before sending, naming what goes where', async () => {
      const user = userEvent.setup();
      const onResend = vi.fn();
      renderWithProviders(
        <CommunicationsTimeline
          communications={[row({ recipientEmail: 'tenant@example.com' })]}
          isLoading={false}
          onResend={onResend}
        />
      );

      await user.click(screen.getByRole('button', { name: /resend/i }));

      // Resending costs money and cannot be undone, so the click must not be terminal.
      expect(onResend).not.toHaveBeenCalled();
      expect(screen.getByText(/resend this message\?/i)).toBeInTheDocument();
      expect(
        screen.getByText(/will be sent again to tenant@example\.com/i)
      ).toBeInTheDocument();
    });

    it('sends the row it was asked about once confirmed', async () => {
      const user = userEvent.setup();
      const onResend = vi.fn();
      renderWithProviders(
        <CommunicationsTimeline
          communications={[
            row({ identifier: 'ntf_first' }),
            row({ identifier: 'ntf_second' }),
          ]}
          isLoading={false}
          onResend={onResend}
        />
      );

      const buttons = screen.getAllByRole('button', { name: /resend/i });
      await user.click(buttons[1]);
      await user.click(
        screen.getByRole('button', { name: /^resend$/i, hidden: false })
      );

      expect(onResend).toHaveBeenCalledExactlyOnceWith('ntf_second');
    });

    it('gives each row a resend label that names its recipient', () => {
      renderWithProviders(
        <CommunicationsTimeline
          communications={[
            row({ identifier: 'ntf_a', recipientEmail: 'anna@example.com' }),
            row({ identifier: 'ntf_b', recipientEmail: 'bram@example.com' }),
          ]}
          isLoading={false}
          onResend={vi.fn()}
        />
      );

      // Twenty identical "Resend" buttons are unusable by keyboard or screen reader.
      expect(
        screen.getByRole('button', { name: /resend .* to anna@example\.com/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /resend .* to bram@example\.com/i })
      ).toBeInTheDocument();
    });

    it('disables the row being resent so a second click cannot double-send', () => {
      renderWithProviders(
        <CommunicationsTimeline
          communications={[row({ identifier: 'ntf_busy' })]}
          isLoading={false}
          onResend={vi.fn()}
          resendingIdentifier="ntf_busy"
        />
      );

      expect(screen.getByRole('button', { name: /resend/i })).toBeDisabled();
    });

    it('offers nothing to a viewer who cannot resend', () => {
      renderWithProviders(
        <CommunicationsTimeline communications={[row()]} isLoading={false} />
      );

      expect(
        screen.queryByRole('button', { name: /resend/i })
      ).not.toBeInTheDocument();
    });
  });

  describe('long timelines', () => {
    const many = (count: number, audience = 'CONTACT') =>
      Array.from({ length: count }, (_, index) =>
        row({ identifier: `ntf_${index}`, audience })
      );

    it('collapses to the most recent few, with a way to see the rest', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <CommunicationsTimeline communications={many(9)} isLoading={false} />
      );

      expect(screen.getAllByRole('listitem')).toHaveLength(5);

      await user.click(screen.getByRole('button', { name: /show all \(9\)/i }));

      expect(screen.getAllByRole('listitem')).toHaveLength(9);
    });

    it('admits when the server has truncated the history', () => {
      renderWithProviders(
        <CommunicationsTimeline communications={many(100)} isLoading={false} />
      );

      // At the cap the list is not the whole story, and a landlord who believes it is
      // concludes a message was never sent.
      expect(
        screen.getByText(/showing the most recent 100 messages/i)
      ).toBeInTheDocument();
    });

    it('does not claim truncation on a short timeline', () => {
      renderWithProviders(
        <CommunicationsTimeline communications={many(3)} isLoading={false} />
      );

      expect(screen.queryByText(/most recent/i)).not.toBeInTheDocument();
    });
  });

  it('renders a row that has neither an email nor a phone number', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ recipientEmail: undefined })]}
        isLoading={false}
      />
    );

    expect(screen.getByText('Payment reminder')).toBeInTheDocument();
  });
});
