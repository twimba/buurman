import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '@buurman/ui';
import { ContractLeaseAgreementTab } from '../ContractLeaseAgreementTab';
import * as leaseAgreementApi from '@/generated/api/lease-agreement/lease-agreement';
import * as analytics from '@/utils/analytics';
import { renderWithProviders, createTestQueryClient } from '@/test/test-utils';
import { QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import {
  LeaseAvailability,
  type LeaseClausesResponse,
  type ResolvedLeaseClauseResponse,
} from '@/generated/models';

const documentEnvelope = (
  clauses: ResolvedLeaseClauseResponse[],
  availability: LeaseAvailability = LeaseAvailability.AVAILABLE_DOCUMENT
): LeaseClausesResponse => ({ availability, countryCode: 'NL', clauses });

const CLAUSES: ResolvedLeaseClauseResponse[] = [
  {
    templateIdentifier: 'LCT00000000000000000000001',
    clauseKey: 'parties',
    title: 'Parties',
    body: 'This clause names the landlord and tenant.',
    included: true,
    optional: false,
    sortOrder: 1,
    pinned: true,
    articleNumber: 1,
  },
  {
    templateIdentifier: 'LCT00000000000000000000002',
    clauseKey: 'furnished-addendum',
    title: 'Furnished addendum',
    body: 'This clause describes the furnished-vs-unfurnished addendum.',
    included: true,
    optional: true,
    sortOrder: 2,
    pinned: false,
    articleNumber: 2,
  },
];

describe('ContractLeaseAgreementTab', () => {
  it('renders the resolved clause list with the correct checked state', async () => {
    vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
      documentEnvelope(CLAUSES)
    );

    renderWithProviders(
      <ToastProvider>
        <ContractLeaseAgreementTab
          contractId="CON00000000000000000000001"
          onGoToDocuments={vi.fn()}
          onEditProperty={vi.fn()}
        />
      </ToastProvider>
    );

    const partiesCheckbox = await screen.findByRole('checkbox', {
      name: 'Parties',
    });
    const addendumCheckbox = await screen.findByRole('checkbox', {
      name: 'Furnished addendum',
    });

    expect(partiesCheckbox).toBeChecked();
    expect(addendumCheckbox).toBeChecked();
  });

  it('disables the checkbox for a non-optional clause', async () => {
    vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
      documentEnvelope(CLAUSES)
    );

    renderWithProviders(
      <ToastProvider>
        <ContractLeaseAgreementTab
          contractId="CON00000000000000000000001"
          onGoToDocuments={vi.fn()}
          onEditProperty={vi.fn()}
        />
      </ToastProvider>
    );

    const partiesCheckbox = await screen.findByRole('checkbox', {
      name: 'Parties',
    });
    const addendumCheckbox = await screen.findByRole('checkbox', {
      name: 'Furnished addendum',
    });

    expect(partiesCheckbox).toBeDisabled();
    expect(addendumCheckbox).not.toBeDisabled();
  });

  it('generates the lease agreement and shows a success toast', async () => {
    vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
      documentEnvelope(CLAUSES)
    );
    const generateSpy = vi
      .spyOn(leaseAgreementApi, 'generateLeaseAgreement')
      .mockResolvedValue({
        identifier: 'DOC00000000000000000000001',
        entityType: 'CONTRACT',
        entityIdentifier: 'CON00000000000000000000001',
        fileKey: 'lease-agreement.pdf',
        fileName: 'lease-agreement.pdf',
        uploadedAt: '2026-03-01T12:00:00Z',
      });

    renderWithProviders(
      <ToastProvider>
        <ContractLeaseAgreementTab
          contractId="CON00000000000000000000001"
          onGoToDocuments={vi.fn()}
          onEditProperty={vi.fn()}
        />
      </ToastProvider>
    );

    const generateButton = await screen.findByRole('button', {
      name: /generate lease agreement/i,
    });
    await userEvent.click(generateButton);

    await waitFor(() => {
      expect(generateSpy).toHaveBeenCalledWith('CON00000000000000000000001');
    });
    expect(
      await screen.findByText(/find it in the documents tab/i)
    ).toBeInTheDocument();
  });

  describe('clause ordering', () => {
    const ORDERED: ResolvedLeaseClauseResponse[] = [
      { ...CLAUSES[0] },
      { ...CLAUSES[1], articleNumber: 2 },
      {
        templateIdentifier: 'LCT00000000000000000000003',
        clauseKey: 'pets',
        title: 'Pets',
        body: 'Pets clause.',
        included: false,
        optional: true,
        sortOrder: 3,
        pinned: false,
        articleNumber: 0,
      },
      {
        templateIdentifier: 'LCT00000000000000000000004',
        clauseKey: 'parking',
        title: 'Parking',
        body: 'Parking clause.',
        included: true,
        optional: true,
        sortOrder: 4,
        pinned: false,
        articleNumber: 3,
      },
    ];

    const renderTab = async () => {
      renderWithProviders(
        <ToastProvider>
          <ContractLeaseAgreementTab
            contractId="CON00000000000000000000001"
            onGoToDocuments={vi.fn()}
            onEditProperty={vi.fn()}
          />
        </ToastProvider>
      );
      await screen.findByRole('checkbox', { name: 'Parties' });
    };

    const renderTabWithClient = async () => {
      const queryClient = createTestQueryClient();
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <ToastProvider>
              <ContractLeaseAgreementTab
                contractId="CON00000000000000000000001"
                onGoToDocuments={vi.fn()}
                onEditProperty={vi.fn()}
              />
            </ToastProvider>
          </MemoryRouter>
        </QueryClientProvider>
      );
      await screen.findByRole('checkbox', { name: 'Parties' });
      return { queryClient };
    };

    const rowFor = (title: string) =>
      screen
        .getByRole('checkbox', { name: title })
        .closest('li') as HTMLElement;

    it('renders move buttons per clause and disables them where a move is impossible', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(ORDERED)
      );
      await renderTab();

      const up = (title: string) =>
        within(rowFor(title)).getByRole('button', { name: /Move up/ });
      const down = (title: string) =>
        within(rowFor(title)).getByRole('button', { name: /Move down/ });

      expect(up('Parties')).toBeDisabled();
      expect(down('Parties')).toBeDisabled();
      expect(within(rowFor('Parties')).getByText('Pinned')).toBeInTheDocument();
      // first movable clause cannot move into the pinned block
      expect(up('Furnished addendum')).toBeDisabled();
      expect(down('Furnished addendum')).not.toBeDisabled();
      expect(up('Parking')).not.toBeDisabled();
      // last clause cannot move down
      expect(down('Parking')).toBeDisabled();
    });

    it('sends swapped sortOrder for the moved pair only and keeps pinned sortOrder', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(ORDERED)
      );
      const updateSpy = vi
        .spyOn(leaseAgreementApi, 'updateLeaseClauses')
        .mockResolvedValue(documentEnvelope(ORDERED));
      await renderTab();

      await userEvent.click(
        within(rowFor('Furnished addendum')).getByRole('button', {
          name: /Move down/,
        })
      );
      await userEvent.click(
        screen.getByRole('button', { name: /save selection/i })
      );

      await waitFor(() => {
        expect(updateSpy).toHaveBeenCalledWith('CON00000000000000000000001', {
          clauses: [
            {
              templateIdentifier: 'LCT00000000000000000000001',
              included: true,
              sortOrder: 1,
            },
            {
              templateIdentifier: 'LCT00000000000000000000003',
              included: false,
              sortOrder: 2,
            },
            {
              templateIdentifier: 'LCT00000000000000000000002',
              included: true,
              sortOrder: 3,
            },
            {
              templateIdentifier: 'LCT00000000000000000000004',
              included: true,
              sortOrder: 4,
            },
          ],
        });
      });
    });

    it('shows article numbers for included clauses only and updates them locally', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(ORDERED)
      );
      await renderTab();

      const number = (title: string) =>
        within(rowFor(title)).queryByTestId('article-number');

      expect(number('Parties')).toHaveTextContent('1');
      expect(number('Furnished addendum')).toHaveTextContent('2');
      expect(number('Pets')).toBeNull();
      expect(number('Parking')).toHaveTextContent('3');

      // excluded optional clause stays toggleable; including it renumbers immediately
      const pets = screen.getByRole('checkbox', { name: 'Pets' });
      expect(pets).not.toBeDisabled();
      await userEvent.click(pets);
      expect(number('Pets')).toHaveTextContent('3');
      expect(number('Parking')).toHaveTextContent('4');

      await userEvent.click(
        within(rowFor('Parking')).getByRole('button', { name: /Move up/ })
      );
      expect(number('Parking')).toHaveTextContent('3');
      expect(number('Pets')).toHaveTextContent('4');
    });

    it('keeps server sortOrder untouched when nothing was reordered', async () => {
      const gapped = ORDERED.map((c, i) => ({ ...c, sortOrder: (i + 1) * 10 }));
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(gapped)
      );
      const updateSpy = vi
        .spyOn(leaseAgreementApi, 'updateLeaseClauses')
        .mockResolvedValue(documentEnvelope(gapped));
      await renderTab();

      await userEvent.click(
        screen.getByRole('button', { name: /save selection/i })
      );

      await waitFor(() => {
        expect(updateSpy).toHaveBeenCalled();
      });
      expect(
        updateSpy.mock.calls[0][1].clauses.map((c) => c.sortOrder)
      ).toEqual([10, 20, 30, 40]);
    });

    it('labels move buttons with the clause title', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(ORDERED)
      );
      await renderTab();
      expect(
        screen.getByRole('button', { name: 'Move up Parking' })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Move down Furnished addendum' })
      ).toBeInTheDocument();
    });

    it('shows and sends a clause added by a refetch while a reorder is pending', async () => {
      const getSpy = vi
        .spyOn(leaseAgreementApi, 'getLeaseClauses')
        .mockResolvedValue(documentEnvelope(ORDERED));
      const updateSpy = vi
        .spyOn(leaseAgreementApi, 'updateLeaseClauses')
        .mockResolvedValue(documentEnvelope(ORDERED));
      const { queryClient } = await renderTabWithClient();

      await userEvent.click(
        within(rowFor('Parking')).getByRole('button', { name: /Move up/ })
      );
      const added: ResolvedLeaseClauseResponse = {
        templateIdentifier: 'LCT00000000000000000000005',
        clauseKey: 'garden',
        title: 'Garden',
        body: 'Garden clause.',
        included: true,
        optional: true,
        sortOrder: 5,
        pinned: false,
        articleNumber: 4,
      };
      getSpy.mockResolvedValue(documentEnvelope([...ORDERED, added]));
      await queryClient.invalidateQueries();

      expect(
        await screen.findByRole('checkbox', { name: 'Garden' })
      ).toBeInTheDocument();
      await userEvent.click(
        screen.getByRole('button', { name: /save selection/i })
      );
      await waitFor(() => {
        expect(updateSpy).toHaveBeenCalled();
      });
      expect(
        updateSpy.mock.calls[0][1].clauses.map((c) => c.templateIdentifier)
      ).toEqual([
        'LCT00000000000000000000001',
        'LCT00000000000000000000002',
        'LCT00000000000000000000004',
        'LCT00000000000000000000003',
        'LCT00000000000000000000005',
      ]);
    });

    it('clears local state after save and shows the server order', async () => {
      const getSpy = vi
        .spyOn(leaseAgreementApi, 'getLeaseClauses')
        .mockResolvedValue(documentEnvelope(ORDERED));
      const serverOrder: ResolvedLeaseClauseResponse[] = [
        ORDERED[0],
        { ...ORDERED[3], sortOrder: 2, articleNumber: 2 },
        { ...ORDERED[1], sortOrder: 3, articleNumber: 3 },
        ORDERED[2],
      ];
      vi.spyOn(leaseAgreementApi, 'updateLeaseClauses').mockImplementation(
        async () => {
          getSpy.mockResolvedValue(documentEnvelope(serverOrder));
          return documentEnvelope(serverOrder);
        }
      );
      await renderTab();

      await userEvent.click(
        within(rowFor('Parking')).getByRole('button', { name: /Move up/ })
      );
      await userEvent.click(
        screen.getByRole('button', { name: /save selection/i })
      );

      await waitFor(() => {
        const titles = screen
          .getAllByRole('checkbox')
          .map((el) => el.getAttribute('aria-label'));
        expect(titles).toEqual([
          'Parties',
          'Parking',
          'Furnished addendum',
          'Pets',
        ]);
      });
      expect(
        within(rowFor('Parking')).getByTestId('article-number')
      ).toHaveTextContent('2');
    });
  });
  describe('availability states', () => {
    const CONTRACT = 'CON00000000000000000000001';
    const renderTab = (
      props: { onGoToDocuments?: () => void; onEditProperty?: () => void } = {}
    ) =>
      renderWithProviders(
        <ToastProvider>
          <ContractLeaseAgreementTab
            contractId={CONTRACT}
            onGoToDocuments={props.onGoToDocuments ?? vi.fn()}
            onEditProperty={props.onEditProperty}
          />
        </ToastProvider>
      );

    it('shows the unavailable-country panel with the country row and hides actions', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        availability: LeaseAvailability.UNAVAILABLE_COUNTRY,
        countryCode: 'IT',
        clauses: [],
      });
      const onGoToDocuments = vi.fn();
      renderTab({ onGoToDocuments });

      expect(
        await screen.findByRole('heading', { name: 'Not available here yet' })
      ).toBeInTheDocument();
      expect(screen.getByText('Country')).toBeInTheDocument();
      expect(screen.getByText('Italy')).toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: /save selection/i })
      ).toBeNull();
      expect(
        screen.queryByRole('button', { name: /generate lease agreement/i })
      ).toBeNull();
      expect(screen.queryByRole('checkbox')).toBeNull();

      await userEvent.click(
        screen.getByRole('button', { name: 'Go to Documents' })
      );
      expect(onGoToDocuments).toHaveBeenCalledTimes(1);
    });

    it('shows the no-country panel and calls onEditProperty', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        availability: LeaseAvailability.UNAVAILABLE_NO_COUNTRY,
        clauses: [],
      });
      const onEditProperty = vi.fn();
      renderTab({ onEditProperty });

      expect(
        await screen.findByRole('heading', { name: 'Choose a country first' })
      ).toBeInTheDocument();
      expect(screen.queryByText('Country')).toBeNull();
      await userEvent.click(
        screen.getByRole('button', { name: 'Edit property' })
      );
      expect(onEditProperty).toHaveBeenCalledTimes(1);
    });

    it('hides the Edit property button when no edit handler is available', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        availability: LeaseAvailability.UNAVAILABLE_NO_COUNTRY,
        clauses: [],
      });
      renderTab();
      await screen.findByRole('heading', { name: 'Choose a country first' });
      expect(
        screen.queryByRole('button', { name: 'Edit property' })
      ).toBeNull();
    });

    it('shows an alert with a working Try again button on a query error', async () => {
      const getSpy = vi
        .spyOn(leaseAgreementApi, 'getLeaseClauses')
        .mockRejectedValueOnce(new Error('boom'))
        .mockResolvedValue(documentEnvelope(CLAUSES));
      renderTab();

      const alert = await screen.findByRole('alert');
      expect(alert).toHaveTextContent("We couldn't load the clauses");
      await userEvent.click(
        within(alert).getByRole('button', { name: 'Try again' })
      );
      expect(
        await screen.findByRole('checkbox', { name: 'Parties' })
      ).toBeInTheDocument();
      expect(getSpy).toHaveBeenCalledTimes(2);
    });

    it('shows the example-text note above the clause list and keeps the actions', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(CLAUSES, LeaseAvailability.AVAILABLE_EXAMPLE_TEXT)
      );
      renderTab();

      const note = await screen.findByRole('note');
      expect(note).toHaveTextContent('This is example text');
      expect(
        screen.getByRole('checkbox', { name: 'Parties' })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /save selection/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /generate lease agreement/i })
      ).toBeInTheDocument();
    });

    it('hides the country row in the example-text note when the region code is unknown', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        ...documentEnvelope(CLAUSES, LeaseAvailability.AVAILABLE_EXAMPLE_TEXT),
        countryCode: 'ZZ',
      });
      renderTab();

      expect(await screen.findByRole('note')).toBeInTheDocument();
      expect(screen.queryByText('Country')).toBeNull();
    });

    it('shows the country row in the example-text note for a known country', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        ...documentEnvelope(CLAUSES, LeaseAvailability.AVAILABLE_EXAMPLE_TEXT),
        countryCode: 'BE',
      });
      renderTab();

      await screen.findByRole('note');
      expect(screen.getByText('Country')).toBeInTheDocument();
      expect(screen.getByText('Belgium')).toBeInTheDocument();
    });

    it('shows no note for a full document', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(
        documentEnvelope(CLAUSES)
      );
      renderTab();
      await screen.findByRole('checkbox', { name: 'Parties' });
      expect(screen.queryByRole('note')).toBeNull();
    });

    it('tracks lease_unavailable_viewed once even when the query refetches', async () => {
      const getSpy = vi
        .spyOn(leaseAgreementApi, 'getLeaseClauses')
        .mockResolvedValue({
          availability: LeaseAvailability.UNAVAILABLE_COUNTRY,
          countryCode: 'IT',
          clauses: [],
        });
      const track = vi
        .spyOn(analytics, 'trackEvent')
        .mockImplementation(() => {});
      const queryClient = createTestQueryClient();
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <ToastProvider>
              <ContractLeaseAgreementTab
                contractId={CONTRACT}
                onGoToDocuments={vi.fn()}
              />
            </ToastProvider>
          </MemoryRouter>
        </QueryClientProvider>
      );
      await screen.findByRole('heading', { name: 'Not available here yet' });
      expect(track).toHaveBeenCalledTimes(1);
      expect(track).toHaveBeenCalledWith('lease_unavailable_viewed', {
        countryCode: 'IT',
        reason: 'unsupported_country',
      });

      // The refetch returns a changed payload (new countryCode, same reason): without the
      // once-per-reason guard the effect would fire again.
      getSpy.mockResolvedValue({
        availability: LeaseAvailability.UNAVAILABLE_COUNTRY,
        countryCode: 'FR',
        clauses: [],
      });
      await queryClient.invalidateQueries();
      await waitFor(() => {
        expect(getSpy).toHaveBeenCalledTimes(2);
      });
      await screen.findByText('France');
      expect(track).toHaveBeenCalledTimes(1);
    });

    it.each([
      LeaseAvailability.AVAILABLE_DOCUMENT,
      LeaseAvailability.AVAILABLE_EXAMPLE_TEXT,
    ])(
      'shows the empty text and a disabled Save for %s without clauses',
      async (availability) => {
        vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
          availability,
          countryCode: 'NL',
          clauses: [],
        });
        renderTab();

        expect(
          await screen.findByText(/no clauses are configured/i)
        ).toBeInTheDocument();
        expect(
          screen.getByRole('button', { name: /save selection/i })
        ).toBeDisabled();
      }
    );

    it('hides the country row when an unavailable envelope has no country code', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        availability: LeaseAvailability.UNAVAILABLE_COUNTRY,
        clauses: [],
      });
      renderTab();

      await screen.findByRole('heading', { name: 'Not available here yet' });
      expect(screen.queryByText('Country')).toBeNull();
    });

    it('does not fall into the error state when clauses is null', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue({
        availability: LeaseAvailability.UNAVAILABLE_COUNTRY,
        countryCode: 'IT',
        clauses: null as unknown as ResolvedLeaseClauseResponse[],
      });
      renderTab();

      expect(
        await screen.findByRole('heading', { name: 'Not available here yet' })
      ).toBeInTheDocument();
      expect(screen.queryByRole('alert')).toBeNull();
    });
  });
});
