import { act, render, screen, waitFor, within } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import i18n from 'i18next';
import documentsNl from '../../../../public/locales/nl/documents.json';
import { queryKeys } from '@/lib/queryKeys';
import { createTestQueryClient } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { AxiosError } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SignatureLinksSheet } from '../SignatureLinksSheet';
import * as signaturesApi from '@/generated/api/signatures/signatures';
import type { SignatureSigningLinkResponse } from '@/generated/models';
import * as analytics from '@/utils/analytics';
import { renderWithProviders } from '@/test/test-utils';

const URL_MARIA = 'https://sign.example.com/sign/token-maria';
const URL_JAN = 'https://sign.example.com/sign/token-jan';

const maria: SignatureSigningLinkResponse = {
  name: 'Maria Jansen',
  email: 'maria@example.com',
  role: 'TENANT',
  status: 'PENDING',
  signingUrl: URL_MARIA,
  signed: false,
};
const jan: SignatureSigningLinkResponse = {
  name: 'Jan Jansen',
  email: 'jan@example.com',
  role: 'TENANT',
  status: 'VIEWED',
  signingUrl: URL_JAN,
  signed: false,
};
const luis: SignatureSigningLinkResponse = {
  name: 'Luis Santos',
  email: 'luis@example.com',
  role: 'LANDLORD',
  status: 'SIGNED',
  signingUrl: null,
  signed: true,
};

const setClipboard = (value: unknown) =>
  Object.defineProperty(navigator, 'clipboard', {
    value,
    configurable: true,
  });

const renderSheet = (open = true, onClose = vi.fn()) =>
  renderWithProviders(
    <SignatureLinksSheet
      open={open}
      onClose={onClose}
      contractId="CON1"
      documentId="DOC1"
      signatureRequestId="SGR1"
    />
  );

const httpError = (status: number) =>
  new AxiosError('failed', 'ERR_BAD_RESPONSE', undefined, undefined, {
    status,
    data: {},
    statusText: '',
    headers: {},
    config: {} as never,
  });

describe('SignatureLinksSheet', () => {
  let writeText: ReturnType<typeof vi.fn>;
  let trackSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    // jsdom lacks scrollIntoView, which the Sheet calls when an input inside it gains focus.
    Element.prototype.scrollIntoView = vi.fn();
    writeText = vi.fn().mockResolvedValue(undefined);
    setClipboard({ writeText });
    trackSpy = vi.spyOn(analytics, 'trackEvent').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
    setClipboard(undefined);
    Reflect.deleteProperty(navigator, 'share');
  });

  it('does not fetch while closed', () => {
    const spy = vi
      .spyOn(signaturesApi, 'getSigningLinks')
      .mockResolvedValue([maria]);
    renderSheet(false);
    expect(spy).not.toHaveBeenCalled();
  });

  it('shows a loading skeleton, then one row per signer with chips and the one-time warning', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria, luis]);
    renderSheet();

    expect(screen.getByTestId('signing-links-skeleton')).toBeInTheDocument();
    expect(await screen.findByText('Maria Jansen')).toBeInTheDocument();
    expect(
      screen.getByText(/share each link only with its signer/i)
    ).toBeInTheDocument();

    const list = screen.getByRole('list', { name: 'Signers' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows).toHaveLength(2);
    // pending first, signed last
    expect(within(rows[0]).getByText('Maria Jansen')).toBeInTheDocument();
    expect(within(rows[0]).getByText('Tenant')).toBeInTheDocument();
    expect(within(rows[0]).getByText('Pending')).toBeInTheDocument();
    expect(within(rows[0]).getByText('maria@example.com')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Landlord')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Signed')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Already signed')).toBeInTheDocument();
    expect(within(rows[1]).queryByRole('button', { name: /copy/i })).toBeNull();
  });

  it('copies a single link, announces it in one live region, tracks scope=single without the URL, and never renders the URL', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria, jan]);
    renderSheet();
    const user = userEvent.setup();
    setClipboard({ writeText });

    const button = await screen.findByRole('button', {
      name: 'Copy signing link for Maria Jansen',
    });
    await user.click(button);

    expect(writeText).toHaveBeenCalledWith(URL_MARIA);
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(/copied/i)
    );
    expect(screen.getAllByRole('status')).toHaveLength(1);
    expect(trackSpy).toHaveBeenCalledWith('signature_link_copied', {
      scope: 'single',
    });
    expect(document.body.textContent).not.toContain('token-maria');
    expect(screen.queryByDisplayValue(URL_MARIA)).toBeNull();
    expect(JSON.stringify(trackSpy.mock.calls)).not.toContain('token-');
  });

  it('shows a selectable read-only field with the link only when copying fails', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria]);
    renderSheet();
    const user = userEvent.setup();
    setClipboard({ writeText: vi.fn().mockRejectedValue(new Error('no')) });
    Object.defineProperty(document, 'execCommand', {
      value: vi.fn().mockReturnValue(false),
      configurable: true,
    });

    await user.click(
      await screen.findByRole('button', {
        name: 'Copy signing link for Maria Jansen',
      })
    );

    const field = await screen.findByDisplayValue(URL_MARIA);
    expect(field).toHaveAttribute('readonly');
    expect(trackSpy).not.toHaveBeenCalledWith(
      'signature_link_copied',
      expect.anything()
    );
    Reflect.deleteProperty(document, 'execCommand');
  });

  it('offers Share only where navigator.share exists and tracks signature_link_shared without the URL', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria]);
    const { unmount } = renderSheet();
    await screen.findByText('Maria Jansen');
    expect(screen.queryByRole('button', { name: /share/i })).toBeNull();
    unmount();

    const share = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'share', {
      value: share,
      configurable: true,
    });
    renderSheet();
    const user = userEvent.setup();
    await user.click(
      await screen.findByRole('button', {
        name: 'Share signing link for Maria Jansen',
      })
    );

    expect(share).toHaveBeenCalledTimes(1);
    expect(share.mock.calls[0][0].url).toBe(URL_MARIA);
    expect(trackSpy).toHaveBeenCalledWith('signature_link_shared');
  });

  it('copies a localised message for every unsigned signer when 2+ are pending', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
      maria,
      jan,
      luis,
    ]);
    renderSheet();
    const user = userEvent.setup();
    setClipboard({ writeText });

    await user.click(
      await screen.findByRole('button', { name: /copy all as message/i })
    );

    expect(writeText).toHaveBeenCalledWith(
      `Hi Maria Jansen, please sign here: ${URL_MARIA}\nHi Jan Jansen, please sign here: ${URL_JAN}`
    );
    expect(trackSpy).toHaveBeenCalledWith('signature_link_copied', {
      scope: 'all',
    });
  });

  it('hides Copy all with a single pending signer', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria, luis]);
    renderSheet();
    await screen.findByText('Maria Jansen');
    expect(
      screen.queryByRole('button', { name: /copy all as message/i })
    ).toBeNull();
  });

  it('tracks signature_links_opened once per open, with no payload', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria]);
    renderSheet();
    await screen.findByText('Maria Jansen');
    expect(
      trackSpy.mock.calls.filter((c) => c[0] === 'signature_links_opened')
    ).toHaveLength(1);
  });

  it('shows link-not-ready for an unsigned signer without a URL, with no Copy button', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
      { ...maria, signingUrl: undefined },
      { ...jan, signingUrl: null },
    ]);
    renderSheet();
    await screen.findByText('Maria Jansen');
    expect(screen.getAllByText(/link not ready yet/i)).toHaveLength(2);
    expect(
      screen.queryByRole('button', { name: /copy signing link/i })
    ).toBeNull();
  });

  it('shows an unavailable-link message for a declined signer', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
      { ...maria, status: 'DECLINED', signingUrl: null },
    ]);
    renderSheet();
    expect(await screen.findByText(/no longer available/i)).toBeInTheDocument();
    expect(screen.getByText('Declined')).toBeInTheDocument();
  });

  it.each([
    [409, /no longer active/i],
    [403, /permission/i],
    [502, /signing service/i],
    [500, /could not load signing links/i],
  ])('maps a %s response to a distinct error message', async (status, text) => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockRejectedValue(
      httpError(status)
    );
    renderSheet();
    expect(await screen.findByText(text)).toBeInTheDocument();
  });

  it('offers retry on retryable errors and not on 409', async () => {
    const spy = vi
      .spyOn(signaturesApi, 'getSigningLinks')
      .mockRejectedValueOnce(httpError(502))
      .mockResolvedValue([maria]);
    renderSheet();
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /retry/i }));
    expect(await screen.findByText('Maria Jansen')).toBeInTheDocument();
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('does not offer retry when the request is no longer active', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockRejectedValue(
      httpError(409)
    );
    renderSheet();
    await screen.findByText(/no longer active/i);
    expect(screen.queryByRole('button', { name: /retry/i })).toBeNull();
  });

  it('polls every 15 seconds while open', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const spy = vi
      .spyOn(signaturesApi, 'getSigningLinks')
      .mockResolvedValue([maria]);
    renderSheet();
    await screen.findByText('Maria Jansen');
    expect(spy).toHaveBeenCalledTimes(1);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15_000);
    });
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('calls onClose from Done', async () => {
    vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria]);
    const onClose = vi.fn();
    renderSheet(true, onClose);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Done' }));
    expect(onClose).toHaveBeenCalled();
  });

  describe('credential hygiene and robustness', () => {
    const Harness = ({
      client,
      language,
    }: {
      client: ReturnType<typeof createTestQueryClient>;
      language?: string;
    }) => {
      const [open, setOpen] = useState(true);
      return (
        <QueryClientProvider client={client}>
          <button onClick={() => setOpen(true)}>reopen</button>
          <SignatureLinksSheet
            open={open}
            onClose={() => setOpen(false)}
            contractId="CON1"
            documentId="DOC1"
            signatureRequestId="SGR1"
            messageLanguage={language}
          />
        </QueryClientProvider>
      );
    };

    it('drops the links from the cache on close and shows the skeleton again on reopen', async () => {
      let resolveSecond: (v: SignatureSigningLinkResponse[]) => void = () => {};
      vi.spyOn(signaturesApi, 'getSigningLinks')
        .mockResolvedValueOnce([maria])
        .mockReturnValueOnce(
          new Promise((resolve) => {
            resolveSecond = resolve;
          })
        );
      const client = createTestQueryClient();
      render(<Harness client={client} />);
      const user = userEvent.setup();
      await screen.findByText('Maria Jansen');
      const key = queryKeys.signatureRequests.signingLinks(
        'CON1',
        'DOC1',
        'SGR1'
      );
      expect(client.getQueryData(key)).toBeDefined();

      await user.click(screen.getByRole('button', { name: 'Done' }));
      expect(client.getQueryData(key)).toBeUndefined();

      await user.click(screen.getByRole('button', { name: 'reopen' }));
      expect(
        await screen.findByTestId('signing-links-skeleton')
      ).toBeInTheDocument();
      expect(screen.queryByText('Maria Jansen')).toBeNull();
      await act(async () => resolveSecond([maria]));
      expect(await screen.findByText('Maria Jansen')).toBeInTheDocument();
    });

    it('stops polling after close', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      const spy = vi
        .spyOn(signaturesApi, 'getSigningLinks')
        .mockResolvedValue([maria]);
      render(<Harness client={createTestQueryClient()} />);
      await screen.findByText('Maria Jansen');
      await act(async () => {
        await userEvent
          .setup({ advanceTimers: vi.advanceTimersByTime })
          .click(screen.getByRole('button', { name: 'Done' }));
      });
      const calls = spy.mock.calls.length;
      await act(async () => {
        await vi.advanceTimersByTimeAsync(45_000);
      });
      expect(spy).toHaveBeenCalledTimes(calls);
    });

    it('removes every copy action when a later poll fails, even though React Query keeps the old data', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      vi.spyOn(signaturesApi, 'getSigningLinks')
        .mockResolvedValueOnce([maria, jan])
        .mockRejectedValue(httpError(409));
      renderSheet();
      await screen.findByRole('button', { name: /copy all as message/i });
      await act(async () => {
        await vi.advanceTimersByTimeAsync(15_000);
      });
      expect(await screen.findByText(/no longer active/i)).toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: /copy all as message/i })
      ).toBeNull();
      expect(
        screen.queryByRole('button', { name: /copy signing link/i })
      ).toBeNull();
      expect(screen.queryByText('Maria Jansen')).toBeNull();
    });

    it('shows a neutral message with retry for an empty signer list, not a load error', async () => {
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([]);
      renderSheet();
      expect(
        await screen.findByText('No links to share for this request.')
      ).toBeInTheDocument();
      expect(screen.queryByText(/could not load/i)).toBeNull();
      expect(
        screen.getByRole('button', { name: /retry/i })
      ).toBeInTheDocument();
    });

    it('shows the copy-all fallback field only when copying fails', async () => {
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
        maria,
        jan,
      ]);
      renderSheet();
      const user = userEvent.setup();
      setClipboard({ writeText });
      await user.click(
        await screen.findByRole('button', { name: /copy all as message/i })
      );
      expect(screen.queryByRole('textbox')).toBeNull();

      setClipboard({ writeText: vi.fn().mockRejectedValue(new Error('no')) });
      Object.defineProperty(document, 'execCommand', {
        value: vi.fn().mockReturnValue(false),
        configurable: true,
      });
      await user.click(
        screen.getByRole('button', { name: /copy all as message/i })
      );
      const field = await screen.findByRole('textbox');
      expect(field).toHaveAttribute('readonly');
      expect((field as HTMLTextAreaElement).value).toContain(URL_JAN);
      Reflect.deleteProperty(document, 'execCommand');
    });

    it('omits signers whose link is not ready from the copied message', async () => {
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
        maria,
        jan,
        {
          ...maria,
          name: 'Pending Pete',
          email: 'pete@example.com',
          signingUrl: null,
        },
      ]);
      renderSheet();
      const user = userEvent.setup();
      setClipboard({ writeText });
      await user.click(
        await screen.findByRole('button', { name: /copy all as message/i })
      );
      expect(writeText.mock.calls[0][0]).not.toContain('Pete');
    });

    it('writes the copied message in the document language when its bundle is loaded', async () => {
      i18n.addResourceBundle('nl', 'documents', documentsNl, true, true);
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
        maria,
        jan,
      ]);
      render(<Harness client={createTestQueryClient()} language="nl" />);
      const user = userEvent.setup();
      setClipboard({ writeText });
      await user.click(
        await screen.findByRole('button', { name: /copy all as message/i })
      );
      expect(writeText.mock.calls[0][0]).toContain(
        `Beste Maria Jansen, onderteken hier alstublieft: ${URL_MARIA}`
      );
      i18n.removeResourceBundle('nl', 'documents');
    });

    it('falls back to the UI language when the document-language bundle is not loaded', async () => {
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([
        maria,
        jan,
      ]);
      render(<Harness client={createTestQueryClient()} language="de" />);
      const user = userEvent.setup();
      setClipboard({ writeText });
      await user.click(
        await screen.findByRole('button', { name: /copy all as message/i })
      );
      expect(writeText.mock.calls[0][0]).toContain(
        'Hi Maria Jansen, please sign here:'
      );
    });

    it('tracks signature_links_opened once per open even if the message language arrives later', async () => {
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria]);
      const client = createTestQueryClient();
      const { rerender } = render(<Harness client={client} />);
      await screen.findByText('Maria Jansen');
      rerender(<Harness client={client} language="nl" />);
      await screen.findByText('Maria Jansen');
      expect(
        trackSpy.mock.calls.filter((c) => c[0] === 'signature_links_opened')
      ).toHaveLength(1);
    });

    it('announces a repeat copy again', async () => {
      vi.spyOn(signaturesApi, 'getSigningLinks').mockResolvedValue([maria]);
      renderSheet();
      const user = userEvent.setup();
      setClipboard({ writeText });
      const button = await screen.findByRole('button', {
        name: 'Copy signing link for Maria Jansen',
      });
      await user.click(button);
      await waitFor(() =>
        expect(screen.getByRole('status')).toHaveTextContent(/copied/i)
      );
      const seen: string[] = [];
      const observer = new MutationObserver(() =>
        seen.push(screen.getByRole('status').textContent ?? '')
      );
      observer.observe(screen.getByRole('status'), {
        childList: true,
        characterData: true,
        subtree: true,
      });
      await user.click(button);
      await waitFor(() =>
        expect(screen.getByRole('status')).toHaveTextContent(/copied/i)
      );
      observer.disconnect();
      expect(seen).toContain('');
    });
  });
});
