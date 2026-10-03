import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '@buurman/ui';
import { ContractLeaseAgreementTab } from '../ContractLeaseAgreementTab';
import * as leaseAgreementApi from '@/generated/api/lease-agreement/lease-agreement';
import { renderWithProviders } from '@/test/test-utils';
import type { ResolvedLeaseClauseResponse } from '@/generated/models';

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
    vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(CLAUSES);

    renderWithProviders(
      <ToastProvider>
        <ContractLeaseAgreementTab contractId="CON00000000000000000000001" />
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
    vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(CLAUSES);

    renderWithProviders(
      <ToastProvider>
        <ContractLeaseAgreementTab contractId="CON00000000000000000000001" />
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
    vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(CLAUSES);
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
        <ContractLeaseAgreementTab contractId="CON00000000000000000000001" />
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
          <ContractLeaseAgreementTab contractId="CON00000000000000000000001" />
        </ToastProvider>
      );
      await screen.findByRole('checkbox', { name: 'Parties' });
    };

    const rowFor = (title: string) =>
      screen
        .getByRole('checkbox', { name: title })
        .closest('li') as HTMLElement;

    it('renders move buttons per clause and disables them where a move is impossible', async () => {
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(ORDERED);
      await renderTab();

      const up = (title: string) =>
        within(rowFor(title)).getByRole('button', { name: 'Move up' });
      const down = (title: string) =>
        within(rowFor(title)).getByRole('button', { name: 'Move down' });

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
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(ORDERED);
      const updateSpy = vi
        .spyOn(leaseAgreementApi, 'updateLeaseClauses')
        .mockResolvedValue(ORDERED);
      await renderTab();

      await userEvent.click(
        within(rowFor('Furnished addendum')).getByRole('button', {
          name: 'Move down',
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
      vi.spyOn(leaseAgreementApi, 'getLeaseClauses').mockResolvedValue(ORDERED);
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
        within(rowFor('Parking')).getByRole('button', { name: 'Move up' })
      );
      expect(number('Parking')).toHaveTextContent('3');
      expect(number('Pets')).toHaveTextContent('4');
    });

    it('clears local state after save and shows the server order', async () => {
      const getSpy = vi
        .spyOn(leaseAgreementApi, 'getLeaseClauses')
        .mockResolvedValue(ORDERED);
      const serverOrder: ResolvedLeaseClauseResponse[] = [
        ORDERED[0],
        { ...ORDERED[3], sortOrder: 2, articleNumber: 2 },
        { ...ORDERED[1], sortOrder: 3, articleNumber: 3 },
        ORDERED[2],
      ];
      vi.spyOn(leaseAgreementApi, 'updateLeaseClauses').mockImplementation(
        async () => {
          getSpy.mockResolvedValue(serverOrder);
          return serverOrder;
        }
      );
      await renderTab();

      await userEvent.click(
        within(rowFor('Parking')).getByRole('button', { name: 'Move up' })
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
});
