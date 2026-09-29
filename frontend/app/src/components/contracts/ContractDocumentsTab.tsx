import {
  useContractDocuments,
  useUploadContractDocument,
  useDeleteContractDocument,
} from '@/hooks/useContractHooks';
import { DocumentList } from '@/components/properties/DocumentList';
import { SignatureRequestPanel } from '@/components/documents/SignatureRequestPanel';
import { FeatureGate } from '@/components/FeatureGate';
import { FeatureFlags } from '@/constants/featureFlags';
import { useTeam } from '@/context/TeamContext';

interface ContractDocumentsTabProps {
  contractId: string;
}

export const ContractDocumentsTab = ({
  contractId,
}: ContractDocumentsTabProps) => {
  const { canEditData } = useTeam();

  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = useContractDocuments(contractId);
  const uploadDocumentMutation = useUploadContractDocument(contractId);
  const deleteDocumentMutation = useDeleteContractDocument(contractId);

  const handleUploadDocument = async (
    file: File,
    title?: string,
    notes?: string
  ) => {
    await uploadDocumentMutation.mutateAsync({ file, title, notes });
  };

  const handleDeleteDocument = async (documentId: string) => {
    await deleteDocumentMutation.mutateAsync(documentId);
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <DocumentList
        documents={documents}
        onUpload={handleUploadDocument}
        onDelete={handleDeleteDocument}
        isLoading={docsLoading}
        error={docsError}
        isUploading={uploadDocumentMutation.isPending}
        isDeleting={deleteDocumentMutation.isPending}
        readOnly={!canEditData}
        renderRowAction={
          canEditData
            ? (doc) =>
                // The signing provider only accepts PDFs, so offering "Send for signature" on an
                // image or spreadsheet row would only ever produce a provider-side failure.
                doc.mimeType === 'application/pdf' ? (
                  <FeatureGate flag={FeatureFlags.ESIGNATURE_ENABLED}>
                    <SignatureRequestPanel
                      contractId={contractId}
                      documentId={doc.identifier}
                    />
                  </FeatureGate>
                ) : null
            : undefined
        }
      />
    </div>
  );
};
