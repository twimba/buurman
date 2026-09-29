import { useState } from 'react';
import { PenLine } from 'lucide-react';
import { SignatureStatusBadge } from './SignatureStatusBadge';
import {
  useSignatureRequest,
  useSignatureRequests,
  useCreateSignatureRequest,
} from '@/hooks/useSignatureRequestHooks';
import type { SignatureRequestResponse } from '@/generated/models';
import { LoadingSpinner } from '@buurman/ui';

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

export const SignatureRequestPanel = ({
  contractId,
  documentId,
}: SignatureRequestPanelProps) => {
  const [createdRequestId, setCreatedRequestId] = useState<string>();
  const createMutation = useCreateSignatureRequest(contractId);

  // Without this the panel only ever knew about a request it created itself in this page's
  // lifetime, so after a reload it offered "Send for signature" again on a document that was
  // already out for signing — one click away from a second envelope and a second round of
  // tenant emails.
  const { data: existingRequests, isLoading: isLoadingExisting } =
    useSignatureRequests(contractId, documentId);

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

  if (!activeRequestId) {
    if (isLoadingExisting) {
      return <LoadingSpinner className="p-0" />;
    }
    return (
      <button
        type="button"
        onClick={handleSend}
        disabled={createMutation.isPending}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-primary-500 hover:bg-primary-50 rounded-md transition-colors disabled:opacity-50"
      >
        <PenLine className="h-4 w-4" />
        {createMutation.isPending ? 'Sending…' : 'Send for signature'}
      </button>
    );
  }

  // Fall back to the list's copy while the per-request poll is still in flight, so a rehydrated
  // panel renders its status immediately rather than flashing a spinner.
  const shown =
    polled ??
    existingRequests?.find((r) => r.identifier === activeRequestId);

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
    </div>
  );
};
