import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/test/test-utils';
import { ContractTimeline } from '../ContractTimeline';
import type { TimelineEventResponse } from '@/generated/models';

// The house pattern for components that format dates (see communications/__tests__/CommunicationsTimeline.test.tsx).
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

const event = (
  overrides: Partial<TimelineEventResponse> = {}
): TimelineEventResponse => ({
  type: 'CONTRACT_CREATED',
  timestamp: '2026-01-01T00:00:00Z',
  title: 'Contract created',
  ...overrides,
});

describe('ContractTimeline', () => {
  it('shows the loading spinner while isLoading is true', () => {
    renderWithProviders(<ContractTimeline events={[]} isLoading={true} />);

    expect(screen.queryByText('No history available')).not.toBeInTheDocument();
  });

  it('distinguishes a failed request from an empty timeline', () => {
    renderWithProviders(
      <ContractTimeline events={[]} isLoading={false} isError />
    );

    expect(screen.getByText('Failed to load history')).toBeInTheDocument();
    expect(screen.queryByText('No history available')).not.toBeInTheDocument();
  });

  it('shows the empty state when there are no events', () => {
    renderWithProviders(<ContractTimeline events={[]} isLoading={false} />);

    expect(screen.getByText('No history available')).toBeInTheDocument();
    expect(
      screen.getByText('Changes to this contract will appear here')
    ).toBeInTheDocument();
  });

  it('renders one row per event with its title', () => {
    const events = [
      event({ type: 'CONTRACT_CREATED', title: 'Contract created' }),
      event({
        type: 'SIGNATURE_COMPLETED',
        timestamp: '2026-02-01T00:00:00Z',
        title: 'Signed: addendum.pdf',
      }),
    ];
    renderWithProviders(<ContractTimeline events={events} isLoading={false} />);

    expect(screen.getByText('Contract created')).toBeInTheDocument();
    expect(screen.getByText('Signed: addendum.pdf')).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
  });

  it('shows the description when the event carries one', () => {
    renderWithProviders(
      <ContractTimeline
        events={[event({ description: 'Rent changed from €1,000 to €1,050' })]}
        isLoading={false}
      />
    );

    expect(
      screen.getByText('Rent changed from €1,000 to €1,050')
    ).toBeInTheDocument();
  });

  it('shows when each event happened, in the user’s own date format', () => {
    renderWithProviders(
      <ContractTimeline
        events={[event({ timestamp: '2026-10-15T09:00:00Z' })]}
        isLoading={false}
      />
    );

    const time = screen.getByText('relative:2026-10-15T09:00:00Z');
    expect(time).toHaveAttribute('datetime', '2026-10-15T09:00:00Z');
    expect(time).toHaveAttribute('title', 'datetime:2026-10-15T09:00:00Z');
  });

  it('resolves an icon and tint for every timeline event type', () => {
    const types: TimelineEventResponse['type'][] = [
      'CONTRACT_CREATED',
      'CONTRACT_STATUS_CHANGED',
      'RENT_CHANGED',
      'EXTENSION_CREATED',
      'EXTENSION_ACTIVATED',
      'EXTENSION_DECLINED',
      'DOCUMENT_UPLOADED',
      'DOCUMENT_GENERATED',
      'SIGNATURE_SENT',
      'SIGNATURE_COMPLETED',
      'SIGNATURE_DECLINED',
      'AUDIT_OTHER',
    ];
    const events = types.map((type, index) =>
      event({
        type,
        title: `Event ${index}`,
        timestamp: `2026-01-0${(index % 9) + 1}T00:00:00Z`,
      })
    );

    // A type missing from ICON_BY_TYPE/TINT_BY_TYPE would render `undefined` as the icon
    // component, which React throws on rather than silently swallowing — every type in this
    // list must render without throwing.
    renderWithProviders(<ContractTimeline events={events} isLoading={false} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(types.length);
  });
});
