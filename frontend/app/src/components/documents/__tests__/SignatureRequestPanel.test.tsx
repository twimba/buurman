import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '@buurman/ui';
import { SignatureRequestPanel } from '../SignatureRequestPanel';
import * as signaturesApi from '@/generated/api/signatures/signatures';
import { renderWithProviders } from '@/test/test-utils';

describe('SignatureRequestPanel', () => {
  it('shows a Send for signature button, then the status badge after sending', async () => {
    vi.spyOn(signaturesApi, 'createSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000001',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PENDING',
      signers: [],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:00:00Z',
    });
    vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000001',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PENDING',
      signers: [
        {
          email: 'tenant@example.com',
          role: 'TENANT',
          status: 'PENDING',
        },
      ],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:00:00Z',
    });

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    const button = screen.getByRole('button', { name: /send for signature/i });
    await userEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
      expect(screen.getByText('0/1 signed')).toBeInTheDocument();
    });
  });
});
