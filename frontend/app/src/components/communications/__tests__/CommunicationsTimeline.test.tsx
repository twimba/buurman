import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { CommunicationsTimeline } from '../CommunicationsTimeline';

const row = (overrides = {}) => ({
  identifier: 'ntf_01JTEST000000000000000001',
  notificationType: 'PAYMENT_REMINDER',
  channel: 'EMAIL',
  status: 'DELIVERED',
  opened: false,
  createdAt: '2026-10-15T09:00:00Z',
  ...overrides,
});

describe('CommunicationsTimeline', () => {
  it('says why it is empty, not merely that it is', () => {
    renderWithProviders(<CommunicationsTimeline communications={[]} isLoading={false} />);

    // No backfill exists, so a pre-existing payment shows nothing. Without this line a
    // landlord concludes no reminder went out and sends a duplicate.
    expect(screen.getByText(/only messages sent from now on/i)).toBeInTheDocument();
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
      <CommunicationsTimeline communications={[row({ status })]} isLoading={false} />
    );

    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('does not claim an SMS was unopened', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ channel: 'SMS', status: 'SENT', opened: false })]}
        isLoading={false}
      />
    );

    // Twilio never reports opens. Showing an email-shaped "not opened" state for an SMS
    // would read as a failure when nothing is wrong.
    expect(screen.getByText('Sent')).toBeInTheDocument();
    expect(screen.queryByText('Not delivered')).not.toBeInTheDocument();
  });

  it('shows when each message was sent', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ createdAt: '2026-10-15T09:00:00Z' })]}
        isLoading={false}
      />
    );

    // Without a date the landlord cannot tell this morning's reminder from March's,
    // which is the whole reason the panel exists.
    expect(screen.getByText(/15/)).toBeInTheDocument();
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
    expect(screen.queryByText(/only messages sent from now on/i)).not.toBeInTheDocument();
    expect(screen.getByText(/could not be loaded/i)).toBeInTheDocument();
  });

  it('translates the notification type rather than showing the enum', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ notificationType: 'CONTRACT_ROLLED_OVER_TO_INDEFINITE' })]}
        isLoading={false}
      />
    );

    expect(screen.queryByText('CONTRACT_ROLLED_OVER_TO_INDEFINITE')).not.toBeInTheDocument();
    expect(screen.getByText('Contract rolled over')).toBeInTheDocument();
  });

  it('shows the subject so a row is identifiable beyond its type', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[row({ subject: 'Payment overdue for Keizersgracht 123-B' })]}
        isLoading={false}
      />
    );

    expect(screen.getByText('Payment overdue for Keizersgracht 123-B')).toBeInTheDocument();
  });

  it('shows why a message was not delivered', () => {
    renderWithProviders(
      <CommunicationsTimeline
        communications={[
          row({ status: 'BOUNCED', providerError: 'mailbox does not exist' }),
        ]}
        isLoading={false}
      />
    );

    // A bounce the landlord cannot explain is a bounce they cannot act on: the reason is
    // what tells them the address is wrong rather than the tenant ignoring them.
    expect(screen.getByText(/mailbox does not exist/i)).toBeInTheDocument();
  });
});
