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
});
