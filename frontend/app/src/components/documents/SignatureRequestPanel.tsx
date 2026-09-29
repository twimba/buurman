import { useEffect, useState } from 'react';
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
  const [signatureRequestId, setSignatureRequestId] = useState<string>();
  const createMutation = useCreateSignatureRequest(contractId);

  // Without this the panel only ever knew about a request it created itself in this page's
  // lifetime, so after a reload it offered "Send for signature" again on a document that was
  // already out for signing — one click away from a second envelope and a second round of
  // tenant emails.
  const { data: existingRequests, isLoading: isLoadingExisting } =
    useSignatureRequests(contractId, documentId);

  const { data: request, isLoading } = useSignatureRequest(
    contractId,
    documentId,
    signatureRequestId
  );

  useEffect(() => {
    if (signatureRequestId || !existingRequests?.length) {
      return;
    }
    // The list is ordered newest-first by the API.
    const mostRecent = existingRequests[0];
    if (RESUMABLE_STATUSES.includes(mostRecent.status)) {
      setSignatureRequestId(mostRecent.identifier);
    }
  }, [existingRequests, signatureRequestId]);

  const handleSend = async () => {
    const created = await createMutation.mutateAsync(documentId);
    setSignatureRequestId(created.identifier);
  };

  if (!signatureRequestId) {
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

  const shown =
    request ?? existingRequests?.find((r) => r.identifier === signatureRequestId);

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
