import {
  usePropertyDocuments,
  useUploadPropertyDocument,
  useDeleteDocument,
} from '@/hooks/usePropertyHooks';
import { DocumentList } from '@/components/properties/DocumentList';
import { useTeam } from '@/context/TeamContext';

interface PropertyDocumentsTabProps {
  propertyId: string;
}

export const PropertyDocumentsTab = ({
  propertyId,
}: PropertyDocumentsTabProps) => {
  const { canEditData } = useTeam();
  const {
    data: documents = [],
    isLoading,
    error,
  } = usePropertyDocuments(propertyId);
  const uploadMutation = useUploadPropertyDocument(propertyId);
  const deleteMutation = useDeleteDocument(propertyId);

  const handleUpload = async (file: File, title?: string, notes?: string) => {
    await uploadMutation.mutateAsync({ file, title, notes });
  };

  const handleDelete = async (documentId: string) => {
    await deleteMutation.mutateAsync(documentId);
  };

  return (
    <DocumentList
      documents={documents}
      isLoading={isLoading}
      error={error}
      onUpload={handleUpload}
      onDelete={handleDelete}
      isUploading={uploadMutation.isPending}
      isDeleting={deleteMutation.isPending}
      readOnly={!canEditData}
    />
  );
};
