import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { FileSignature, Ban, AlertTriangle, X } from 'lucide-react';
import { SignatureStatusBadge } from './SignatureStatusBadge';
import {
  useSignatureRequest,
  useSignatureRequests,
  useCreateSignatureRequest,
  useCancelSignatureRequest,
} from '@/hooks/useSignatureRequestHooks';
import type { SignatureRequestResponse } from '@/generated/models';
import { Button, LoadingSpinner } from '@buurman/ui';

interface SignatureRequestPanelProps {
  contractId: string;
  documentId: string;
}

/**
 * Statuses whose request should be shown rather than offering to send again. DECLINED, CANCELLED
 * and FAILED are deliberately absent: re-offering "Send for signature" so the landlord can retry
 * is the correct behaviour there.
 */
const RESUMABLE_STATUSES: SignatureRequestResponse['status'][] = [
  'PENDING',
  'PARTIALLY_SIGNED',
  'COMPLETED',
];

/** Only a request still in flight can be retracted — everything else already has a final outcome. */
const CANCELLABLE_STATUSES: SignatureRequestResponse['status'][] = [
  'PENDING',
  'PARTIALLY_SIGNED',
];

const CancelSignatureRequestModal = ({
  isLoading,
  onConfirm,
  onCancel,
}: {
  isLoading: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}) => {
  const [reason, setReason] = useState('');
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    cancelRef.current?.focus();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onCancel]);

  // This panel is invoked from inside a table row (DocumentList's actions <td>, which sets
  // text-right and whitespace-nowrap to lay out its row of icon buttons). Both properties
  // inherit, and position: fixed does not reset inheritance — only a portal actually detaches
  // this subtree from that ancestor, so the modal can't silently pick up text-align/white-space
  // (or anything else) from wherever its trigger happens to be rendered.
  return createPortal(
    <div className="fixed inset-0 z-50 overflow-y-auto" onClick={onCancel}>
      <div className="flex items-center justify-center min-h-[100dvh] px-4 py-8">
        <div className="fixed inset-0 bg-surface-overlay backdrop-blur-sm" />
        <div
          className="relative bg-surface-card rounded-lg shadow-xl w-full max-w-md text-left"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="px-6 pt-6 pb-2">
            <div className="flex items-start gap-4">
              <div className="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center bg-error-bg">
                <AlertTriangle className="h-5 w-5 text-error-text" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-lg font-semibold text-text-primary">
                  Retract signature request
                </h3>
                <p className="mt-2 text-sm text-text-secondary leading-relaxed">
                  Signers will no longer be able to sign this document. You can
                  send it for signature again afterwards.
                </p>
              </div>
              <button
                onClick={onCancel}
                className="flex-shrink-0 p-1 text-text-muted hover:text-text-primary rounded transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          <div className="px-6 py-4">
            <label
              htmlFor="retract-signature-reason"
              className="block text-sm font-medium text-text-secondary mb-1"
            >
              Reason (optional)
            </label>
            <textarea
              id="retract-signature-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g. Tenant backed out, document needs a correction..."
              rows={3}
              maxLength={500}
              disabled={isLoading}
              className="w-full border border-border-strong rounded px-3 py-2 text-sm"
            />
          </div>

          <div className="px-6 py-4 flex justify-end gap-3">
            <Button
              ref={cancelRef}
              variant="secondary"
              size="md"
              onClick={onCancel}
              disabled={isLoading}
            >
              Keep request
            </Button>
            <Button
              variant="danger"
              size="md"
              onClick={() => onConfirm(reason.trim())}
              disabled={isLoading}
            >
              Retract
            </Button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};

export const SignatureRequestPanel = ({
  contractId,
  documentId,
}: SignatureRequestPanelProps) => {
  const [createdRequestId, setCreatedRequestId] = useState<string>();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const createMutation = useCreateSignatureRequest(contractId);
  const cancelMutation = useCancelSignatureRequest(contractId, documentId);

  // Without this the panel only ever knew about a request it created itself in this page's
  // lifetime, so after a reload it offered "Send for signature" again on a document that was
  // already out for signing — one click away from a second envelope and a second round of
  // tenant emails.
  const {
    data: existingRequests,
    isLoading: isLoadingExisting,
    isError: isErrorExisting,
  } = useSignatureRequests(contractId, documentId);

  // The list is ordered newest-first by the API. Derived, not copied into state, so there is no
  // setState-in-an-effect and no render where a known request is briefly forgotten.
  const mostRecent = existingRequests?.[0];
  const resumable =
    mostRecent && RESUMABLE_STATUSES.includes(mostRecent.status)
      ? mostRecent
      : undefined;
  const activeRequestId = createdRequestId ?? resumable?.identifier;

  const { data: polled, isLoading } = useSignatureRequest(
    contractId,
    documentId,
    activeRequestId
  );

  const handleSend = async () => {
    const created = await createMutation.mutateAsync(documentId);
    setCreatedRequestId(created.identifier);
  };

  const handleConfirmCancel = (reason: string) => {
    if (!activeRequestId) {
      return;
    }
    cancelMutation.mutate(
      { signatureRequestId: activeRequestId, reason: reason || undefined },
      { onSuccess: () => setShowCancelModal(false) }
    );
  };

  if (!activeRequestId) {
    if (isLoadingExisting) {
      return <LoadingSpinner className="p-0" />;
    }
    // A failed fetch is NOT "no existing request" — it's "unknown". Defaulting to the plain Send
    // button here would be the exact duplicate-send hazard this whole existing-requests lookup
    // exists to prevent (see the comment above): a document already out for signing would offer
    // to send it again the moment its status check happens to fail.
    if (isErrorExisting) {
      return (
        <span
          className="p-1.5 text-text-muted inline-flex"
          title="Couldn't check this document's signature status — reload the page to try again"
        >
          <AlertTriangle className="h-4 w-4" />
        </span>
      );
    }
    return (
      <button
        type="button"
        onClick={handleSend}
        disabled={createMutation.isPending}
        className="p-1.5 text-primary-500 hover:bg-primary-50 rounded-md transition-colors disabled:opacity-50"
        title="Send for signature"
      >
        <FileSignature className="h-4 w-4" />
      </button>
    );
  }

  // Fall back to the list's copy while the per-request poll is still in flight, so a rehydrated
  // panel renders its status immediately rather than flashing a spinner.
  const shown =
    polled ?? existingRequests?.find((r) => r.identifier === activeRequestId);

  if (!shown) {
    return isLoading ? <LoadingSpinner className="p-0" /> : null;
  }

  return (
    <div className="flex items-center gap-2">
      <SignatureStatusBadge status={shown.status} />
      <span className="text-xs text-text-secondary">
        {shown.signers.filter((s) => s.status === 'SIGNED').length}/
        {shown.signers.length} signed
      </span>
      {CANCELLABLE_STATUSES.includes(shown.status) && (
        <button
          type="button"
          onClick={() => setShowCancelModal(true)}
          className="p-1.5 text-error-text hover:bg-error-bg rounded-md transition-colors"
          title="Retract signature request"
        >
          <Ban className="h-4 w-4" />
        </button>
      )}
      {showCancelModal && (
        <CancelSignatureRequestModal
          isLoading={cancelMutation.isPending}
          onConfirm={handleConfirmCancel}
          onCancel={() => setShowCancelModal(false)}
        />
      )}
    </div>
  );
};
