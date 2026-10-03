import { screen, waitFor } from '@testing-library/react';
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
});
