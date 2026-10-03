import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '@buurman/ui';
import { SignatureRequestPanel } from '../SignatureRequestPanel';
import * as signaturesApi from '@/generated/api/signatures/signatures';
import { renderWithProviders } from '@/test/test-utils';

describe('SignatureRequestPanel', () => {
  it('shows a Send for signature button, then the status badge after sending', async () => {
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([]);
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

    const button = await screen.findByRole('button', {
      name: /send for signature/i,
    });
    await userEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
      expect(screen.getByText('0/1 signed')).toBeInTheDocument();
    });
  });

  it('a failed send shows an error toast and leaves the Send button clickable again, without an unhandled rejection', async () => {
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([]);
    vi.spyOn(signaturesApi, 'createSignatureRequest').mockRejectedValue(
      new Error('network error')
    );

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    const button = await screen.findByRole('button', {
      name: /send for signature/i,
    });
    await userEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument();
    });
    expect(button).not.toBeDisabled();
  });

  it('rehydrates from an existing in-flight request instead of offering to send again', async () => {
    // The duplicate-send hazard: on mount the panel has no local state, so without the list
    // query it would show "Send for signature" on a document already out for signing.
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
      {
        identifier: 'SGR00000000000000000000009',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'PARTIALLY_SIGNED',
        signers: [
          { email: 'landlord@example.com', role: 'LANDLORD', status: 'SIGNED' },
          { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
        ],
        createdAt: '2026-03-01T12:00:00Z',
        updatedAt: '2026-03-01T12:05:00Z',
      },
    ]);
    const createSpy = vi.spyOn(signaturesApi, 'createSignatureRequest');
    vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000009',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PARTIALLY_SIGNED',
      signers: [
        { email: 'landlord@example.com', role: 'LANDLORD', status: 'SIGNED' },
        { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
      ],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:05:00Z',
    });

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    expect(await screen.findByText('Partially signed')).toBeInTheDocument();
    expect(screen.getByText('1/2 signed')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /send for signature/i })
    ).not.toBeInTheDocument();
    expect(createSpy).not.toHaveBeenCalled();
  });

  it('picks the request with the latest createdAt as "most recent", not whichever is first in the list', async () => {
    // listSignatureRequests returned oldest-first here on purpose — the component must not
    // trust array order and must sort by createdAt itself.
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
      {
        identifier: 'SGR00000000000000000000001',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'DECLINED',
        signers: [],
        createdAt: '2026-01-01T12:00:00Z',
        updatedAt: '2026-01-01T12:05:00Z',
      },
      {
        identifier: 'SGR00000000000000000000002',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'PARTIALLY_SIGNED',
        signers: [
          { email: 'landlord@example.com', role: 'LANDLORD', status: 'SIGNED' },
          { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
        ],
        createdAt: '2026-03-01T12:00:00Z',
        updatedAt: '2026-03-01T12:05:00Z',
      },
    ]);
    vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000002',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PARTIALLY_SIGNED',
      signers: [
        { email: 'landlord@example.com', role: 'LANDLORD', status: 'SIGNED' },
        { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
      ],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:05:00Z',
    });

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    expect(await screen.findByText('Partially signed')).toBeInTheDocument();
    expect(screen.getByText('1/2 signed')).toBeInTheDocument();
  });

  it('offers to send again when the most recent request was declined', async () => {
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
      {
        identifier: 'SGR00000000000000000000010',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'DECLINED',
        signers: [
          { email: 'tenant@example.com', role: 'TENANT', status: 'DECLINED' },
        ],
        createdAt: '2026-03-01T12:00:00Z',
        updatedAt: '2026-03-01T12:05:00Z',
      },
    ]);

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    expect(
      await screen.findByRole('button', { name: /send for signature/i })
    ).toBeInTheDocument();
  });

  it('shows a disabled, non-actionable state instead of "Send for signature" when the existing-requests fetch fails — a failed check is not "no request exists"', async () => {
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockRejectedValue(
      new Error('network error')
    );
    const createSpy = vi.spyOn(signaturesApi, 'createSignatureRequest');

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    await waitFor(() => {
      expect(
        screen.queryByRole('button', { name: /send for signature/i })
      ).not.toBeInTheDocument();
    });
    expect(createSpy).not.toHaveBeenCalled();
  });

  it('offers to retract a request that is still pending, and sends the typed reason', async () => {
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
      {
        identifier: 'SGR00000000000000000000011',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'PENDING',
        signers: [
          { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
        ],
        createdAt: '2026-03-01T12:00:00Z',
        updatedAt: '2026-03-01T12:00:00Z',
      },
    ]);
    vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000011',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'PENDING',
      signers: [
        { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
      ],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:00:00Z',
    });
    const cancelSpy = vi
      .spyOn(signaturesApi, 'cancelSignatureRequest')
      .mockResolvedValue({
        identifier: 'SGR00000000000000000000011',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'CANCELLED',
        signers: [
          { email: 'tenant@example.com', role: 'TENANT', status: 'PENDING' },
        ],
        createdAt: '2026-03-01T12:00:00Z',
        updatedAt: '2026-03-01T12:10:00Z',
      });

    renderWithProviders(
      <ToastProvider>
        <SignatureRequestPanel
          contractId="CON00000000000000000000001"
          documentId="DOC00000000000000000000001"
        />
      </ToastProvider>
    );

    const retractButton = await screen.findByRole('button', {
      name: /retract signature request/i,
    });
    await userEvent.click(retractButton);

    const reasonField =
      await screen.findByPlaceholderText(/tenant backed out/i);
    await userEvent.type(reasonField, 'Tenant moved out');
    await userEvent.click(screen.getByRole('button', { name: /^retract$/i }));

    await waitFor(() => {
      expect(cancelSpy).toHaveBeenCalledWith(
        'CON00000000000000000000001',
        'DOC00000000000000000000001',
        'SGR00000000000000000000011',
        { reason: 'Tenant moved out' }
      );
    });
  });

  it('does not offer to retract a request that already reached a final state', async () => {
    vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
      {
        identifier: 'SGR00000000000000000000012',
        documentIdentifier: 'DOC00000000000000000000001',
        status: 'COMPLETED',
        signers: [
          { email: 'tenant@example.com', role: 'TENANT', status: 'SIGNED' },
        ],
        createdAt: '2026-03-01T12:00:00Z',
        updatedAt: '2026-03-01T12:00:00Z',
      },
    ]);
    vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue({
      identifier: 'SGR00000000000000000000012',
      documentIdentifier: 'DOC00000000000000000000001',
      status: 'COMPLETED',
      signers: [
        { email: 'tenant@example.com', role: 'TENANT', status: 'SIGNED' },
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

    expect(await screen.findByText('Signed')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /retract signature request/i })
    ).not.toBeInTheDocument();
  });

  describe('signing links', () => {
    const requestWith = (
      status: 'PENDING' | 'PARTIALLY_SIGNED' | 'COMPLETED'
    ) => ({
      identifier: 'SGR00000000000000000000020',
      documentIdentifier: 'DOC00000000000000000000001',
      status,
      signers: [
        {
          email: 'tenant@example.com',
          role: 'TENANT' as const,
          status: 'PENDING' as const,
        },
      ],
      createdAt: '2026-03-01T12:00:00Z',
      updatedAt: '2026-03-01T12:00:00Z',
    });

    const renderPanel = () =>
      renderWithProviders(
        <ToastProvider>
          <SignatureRequestPanel
            contractId="CON00000000000000000000001"
            documentId="DOC00000000000000000000001"
            messageLanguage="nl"
          />
        </ToastProvider>
      );

    it.each(['PENDING', 'PARTIALLY_SIGNED'] as const)(
      'shows labelled signing-links and retract icon buttons for a %s request',
      async (status) => {
        vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
          requestWith(status),
        ]);
        vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue(
          requestWith(status)
        );
        renderPanel();

        expect(
          await screen.findByRole('button', { name: 'Get signing links' })
        ).toHaveAttribute('aria-haspopup', 'dialog');
        expect(
          screen.getByRole('button', { name: 'Retract signature request' })
        ).toHaveAttribute('aria-label', 'Retract signature request');
      }
    );

    it('does not show the signing-links button once the request is completed', async () => {
      vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
        requestWith('COMPLETED'),
      ]);
      vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue(
        requestWith('COMPLETED')
      );
      renderPanel();

      await screen.findByText('Signed');
      expect(
        screen.queryByRole('button', { name: 'Get signing links' })
      ).not.toBeInTheDocument();
    });

    it('opens the sheet on click, fetches only then, and returns focus to the trigger on close', async () => {
      vi.spyOn(signaturesApi, 'listSignatureRequests').mockResolvedValue([
        requestWith('PENDING'),
      ]);
      vi.spyOn(signaturesApi, 'getSignatureRequest').mockResolvedValue(
        requestWith('PENDING')
      );
      const linksSpy = vi
        .spyOn(signaturesApi, 'getSigningLinks')
        .mockResolvedValue([
          {
            name: 'Maria Jansen',
            email: 'tenant@example.com',
            role: 'TENANT',
            status: 'PENDING',
            signingUrl: 'https://sign.example.com/sign/abc',
            signed: false,
          },
        ]);
      renderPanel();
      const user = userEvent.setup();

      const trigger = await screen.findByRole('button', {
        name: 'Get signing links',
      });
      expect(linksSpy).not.toHaveBeenCalled();
      await user.click(trigger);

      expect(await screen.findByText('Maria Jansen')).toBeInTheDocument();
      expect(linksSpy).toHaveBeenCalledWith(
        'CON00000000000000000000001',
        'DOC00000000000000000000001',
        'SGR00000000000000000000020'
      );

      await user.click(screen.getByRole('button', { name: 'Done' }));
      await waitFor(() => expect(trigger).toHaveFocus());
      expect(screen.queryByText('Maria Jansen')).not.toBeInTheDocument();
    });
  });
});
