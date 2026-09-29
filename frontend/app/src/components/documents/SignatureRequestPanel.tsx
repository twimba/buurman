import { useState } from 'react';
import { PenLine } from 'lucide-react';
import { SignatureStatusBadge } from './SignatureStatusBadge';
import {
  useSignatureRequest,
  useCreateSignatureRequest,
} from '@/hooks/useSignatureRequestHooks';
import { LoadingSpinner } from '@buurman/ui';

interface SignatureRequestPanelProps {
  contractId: string;
  documentId: string;
}

export const SignatureRequestPanel = ({
  contractId,
  documentId,
}: SignatureRequestPanelProps) => {
  const [signatureRequestId, setSignatureRequestId] = useState<string>();
  const createMutation = useCreateSignatureRequest(contractId);
  const { data: request, isLoading } = useSignatureRequest(
    contractId,
    documentId,
    signatureRequestId
  );

  const handleSend = async () => {
    const created = await createMutation.mutateAsync(documentId);
    setSignatureRequestId(created.identifier);
  };

  if (!signatureRequestId) {
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

  if (isLoading || !request) {
    return <LoadingSpinner className="p-0" />;
  }

  return (
    <div className="flex items-center gap-2">
      <SignatureStatusBadge status={request.status} />
      <span className="text-xs text-text-secondary">
        {request.signers.filter((s) => s.status === 'SIGNED').length}/
        {request.signers.length} signed
      </span>
    </div>
  );
};
