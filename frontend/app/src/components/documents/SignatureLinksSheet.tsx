import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { isAxiosError } from 'axios';
import {
  Check,
  Clock,
  Copy,
  Eye,
  ShieldAlert,
  Share2,
  XCircle,
} from 'lucide-react';
import { Button, Sheet, Skeleton, StatusBadge } from '@buurman/ui';
import type { BadgeColorVariant } from '@buurman/ui';
import { useSignatureSigningLinks } from '@/hooks/useSignatureRequestHooks';
import type { SignatureSigningLinkResponse } from '@/generated/models';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import { trackEvent } from '@/utils/analytics';
import { canShare, copyText } from '@/utils/clipboard';

interface SignatureLinksSheetProps {
  open: boolean;
  onClose: () => void;
  contractId: string;
  documentId: string;
  signatureRequestId: string;
  /** Document language of the contract's tenants; the copy-all message is written in it. */
  messageLanguage?: string;
}

type Signer = SignatureSigningLinkResponse;

const STATUS_COLOR: Record<Signer['status'], BadgeColorVariant> = {
  PENDING: 'amber',
  VIEWED: 'blue',
  SIGNED: 'green',
  DECLINED: 'red',
};

const STATUS_ICON: Record<Signer['status'], typeof Clock> = {
  PENDING: Clock,
  VIEWED: Eye,
  SIGNED: Check,
  DECLINED: XCircle,
};

const COPIED_RESET_MS = 2500;

/** A link is only actionable for a signer who has not signed and has a URL ready. */
const linkOf = (signer: Signer): string | undefined =>
  signer.signed ? undefined : (signer.signingUrl ?? undefined);

const errorKind = (
  error: unknown
): 'inactive' | 'forbidden' | 'provider' | 'generic' => {
  const status = isAxiosError(error) ? error.response?.status : undefined;
  if (status === 409) {
    return 'inactive';
  }
  if (status === 403) {
    return 'forbidden';
  }
  if (status === 502) {
    return 'provider';
  }
  return 'generic';
};

export const SignatureLinksSheet = ({
  open,
  onClose,
  contractId,
  documentId,
  signatureRequestId,
  messageLanguage,
}: SignatureLinksSheetProps) => {
  const { t, i18n } = useTranslation('documents');
  const [copiedKey, setCopiedKey] = useState<string>();
  const [announcement, setAnnouncement] = useState('');
  const [fallback, setFallback] = useState<{ key: string; url: string }>();
  const resetTimer = useRef<ReturnType<typeof setTimeout>>(undefined);

  const { data, isPending, error, refetch, isFetching } =
    useSignatureSigningLinks(contractId, documentId, signatureRequestId, {
      enabled: open,
    });

  useEffect(() => {
    if (open) {
      trackEvent(AnalyticsEvent.SIGNATURE_LINKS_OPENED);
      if (messageLanguage) {
        // Warm the message language now: the copy handler must stay synchronous to keep the
        // browser's user-activation, so it cannot await a locale download.
        void i18n.loadLanguages(messageLanguage.toLowerCase());
      }
    }
  }, [open, messageLanguage, i18n]);

  useEffect(() => () => clearTimeout(resetTimer.current), []);

  // Reset here rather than in an effect on `open`: every close path (Done, Esc, overlay) lands
  // in this handler, and nothing here may outlive the sheet, least of all a copied link.
  const handleClose = () => {
    clearTimeout(resetTimer.current);
    setCopiedKey(undefined);
    setAnnouncement('');
    setFallback(undefined);
    onClose();
  };

  const flashCopied = useCallback((key: string, message: string) => {
    setCopiedKey(key);
    setAnnouncement(message);
    clearTimeout(resetTimer.current);
    resetTimer.current = setTimeout(() => {
      setCopiedKey(undefined);
      setAnnouncement('');
    }, COPIED_RESET_MS);
  }, []);

  const signers = [...(data ?? [])].sort(
    (a, b) => Number(a.signed) - Number(b.signed)
  );
  const copyable = signers.filter((s) => linkOf(s));

  const handleCopyOne = async (signer: Signer) => {
    const url = linkOf(signer);
    if (!url) {
      return;
    }
    setFallback(undefined);
    if (await copyText(url)) {
      trackEvent(AnalyticsEvent.SIGNATURE_LINK_COPIED, { scope: 'single' });
      flashCopied(
        signer.email,
        t('signatureLinks.copiedAnnouncement', { name: signer.name })
      );
    } else {
      setFallback({ key: signer.email, url });
    }
  };

  const handleCopyAll = async () => {
    const tMessage = i18n.getFixedT(
      messageLanguage ? messageLanguage.toLowerCase() : null,
      'documents'
    );
    const message = copyable
      .map((s) =>
        tMessage('signatureLinks.message', {
          name: s.name,
          url: linkOf(s),
        })
      )
      .join('\n');
    setFallback(undefined);
    if (await copyText(message)) {
      trackEvent(AnalyticsEvent.SIGNATURE_LINK_COPIED, { scope: 'all' });
      flashCopied('all', t('signatureLinks.copiedAllAnnouncement'));
    } else {
      setFallback({ key: 'all', url: message });
    }
  };

  const handleShare = async (signer: Signer) => {
    const url = linkOf(signer);
    if (!url) {
      return;
    }
    try {
      await navigator.share({
        title: t('signatureLinks.shareTitle'),
        text: t('signatureLinks.shareText', { name: signer.name }),
        url,
      });
      trackEvent(AnalyticsEvent.SIGNATURE_LINK_SHARED);
    } catch {
      // The user dismissing the share sheet rejects with AbortError; nothing to report.
    }
  };

  const renderBody = () => {
    if (error) {
      const kind = errorKind(error);
      return (
        <div className="px-6 py-8 text-center space-y-3" role="alert">
          <p className="text-sm text-text-primary">
            {t(`signatureLinks.error.${kind}`)}
          </p>
          {kind !== 'inactive' && kind !== 'forbidden' && (
            <Button
              variant="secondary"
              size="sm"
              isLoading={isFetching}
              onClick={() => void refetch()}
            >
              {t('signatureLinks.retry')}
            </Button>
          )}
        </div>
      );
    }
    if (isPending) {
      return (
        <div
          data-testid="signing-links-skeleton"
          className="px-6 py-3 space-y-3"
        >
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      );
    }
    if (signers.length === 0) {
      return (
        <p
          className="px-6 py-8 text-center text-sm text-text-primary"
          role="alert"
        >
          {t('signatureLinks.error.generic')}
        </p>
      );
    }
    return (
      <>
        <div className="mx-6 my-3 flex items-start gap-2 rounded-md bg-warning-bg px-3 py-2 text-sm text-warning-text">
          <ShieldAlert
            className="mt-0.5 h-4 w-4 flex-shrink-0"
            aria-hidden="true"
          />
          <p>{t('signatureLinks.warning')}</p>
        </div>
        <ul aria-label={t('signatureLinks.signers')} className="text-left">
          {signers.map((signer) => {
            const url = linkOf(signer);
            const StatusIcon = STATUS_ICON[signer.status];
            const isCopied = copiedKey === signer.email;
            return (
              <li
                key={signer.email}
                className={`px-6 py-3 border-b border-border-default ${signer.signed ? 'opacity-70' : ''}`}
              >
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-medium text-text-primary">
                        {signer.name}
                      </span>
                      <StatusBadge
                        size="xs"
                        color="gray"
                        label={t(`signatureLinks.role.${signer.role}`)}
                      />
                      <StatusBadge
                        size="xs"
                        color={STATUS_COLOR[signer.status]}
                        label={t(`signatureLinks.status.${signer.status}`)}
                        icon={
                          <StatusIcon className="h-3 w-3" aria-hidden="true" />
                        }
                      />
                    </div>
                    <p className="truncate text-xs text-text-secondary">
                      {signer.email}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {signer.signed && (
                      <span className="text-xs text-text-secondary">
                        {t('signatureLinks.alreadySigned')}
                      </span>
                    )}
                    {!signer.signed && !url && (
                      <span className="text-xs text-text-secondary">
                        {signer.status === 'DECLINED'
                          ? t('signatureLinks.unavailable')
                          : t('signatureLinks.notReady')}
                      </span>
                    )}
                    {url && (
                      <>
                        <Button
                          variant="secondary"
                          size="sm"
                          className="max-md:min-h-touch"
                          aria-label={t('signatureLinks.copyLinkFor', {
                            name: signer.name,
                          })}
                          leftIcon={
                            isCopied ? (
                              <Check aria-hidden="true" />
                            ) : (
                              <Copy aria-hidden="true" />
                            )
                          }
                          onClick={() => void handleCopyOne(signer)}
                        >
                          {isCopied
                            ? t('signatureLinks.copied')
                            : t('signatureLinks.copyLink')}
                        </Button>
                        {canShare() && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="max-md:min-h-touch"
                            aria-label={t('signatureLinks.shareFor', {
                              name: signer.name,
                            })}
                            leftIcon={<Share2 aria-hidden="true" />}
                            onClick={() => void handleShare(signer)}
                          >
                            {t('signatureLinks.share')}
                          </Button>
                        )}
                      </>
                    )}
                  </div>
                </div>
                {fallback?.key === signer.email && (
                  <FallbackField
                    label={t('signatureLinks.copyFailed')}
                    value={fallback.url}
                  />
                )}
              </li>
            );
          })}
        </ul>
        {fallback?.key === 'all' && (
          <div className="px-6 py-3">
            <FallbackField
              label={t('signatureLinks.copyFailed')}
              value={fallback.url}
              multiline
            />
          </div>
        )}
      </>
    );
  };

  return (
    <Sheet
      open={open}
      onClose={handleClose}
      title={t('signatureLinks.title')}
      description={t('signatureLinks.description')}
      snapPoints={[0.6, 0.92]}
      footer={
        <>
          {copyable.length >= 2 && (
            <Button
              variant="secondary"
              size="md"
              className="max-md:min-h-touch"
              leftIcon={<Copy aria-hidden="true" />}
              onClick={() => void handleCopyAll()}
            >
              {t('signatureLinks.copyAll')}
            </Button>
          )}
          <Button
            variant="primary"
            size="md"
            className="max-md:min-h-touch"
            onClick={handleClose}
          >
            {t('signatureLinks.done')}
          </Button>
        </>
      }
    >
      {renderBody()}
      <div role="status" aria-live="polite" className="sr-only">
        {announcement}
      </div>
    </Sheet>
  );
};

const FallbackField = ({
  label,
  value,
  multiline,
}: {
  label: string;
  value: string;
  multiline?: boolean;
}) => {
  const ref = useRef<HTMLInputElement & HTMLTextAreaElement>(null);
  useEffect(() => {
    ref.current?.focus();
    ref.current?.select();
  }, []);
  const className =
    'mt-2 w-full rounded border border-border-strong bg-surface-inset px-2 py-1 text-xs text-text-primary';
  return (
    <label className="block text-xs text-text-secondary">
      {label}
      {multiline ? (
        <textarea
          ref={ref}
          readOnly
          rows={3}
          value={value}
          className={className}
          onFocus={(e) => e.currentTarget.select()}
        />
      ) : (
        <input
          ref={ref}
          readOnly
          value={value}
          className={className}
          onFocus={(e) => e.currentTarget.select()}
        />
      )}
    </label>
  );
};
