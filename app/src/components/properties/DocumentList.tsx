import { useState } from 'react';
import { DocumentResponse } from '@/types/property';
import {
  Upload,
  X,
  FileText,
  Image as ImageIcon,
  Download,
  Trash2,
  Eye,
  Pencil,
  CheckSquare,
  Square,
  Check,
} from 'lucide-react';
import { LoadingSpinner } from '../LoadingSpinner';
import { ErrorMessage } from '../ErrorMessage';
import { DocumentPreviewModal } from '../documents/DocumentPreviewModal';
import { ConfirmDialog } from '../ui/ConfirmDialog';
import { EditMetadataModal } from '../ui/EditMetadataModal';
import { useDocumentSelection } from '@/hooks/useDocumentSelection';
import { useBulkDownload, useUpdateDocument } from '@/hooks/useDocumentHooks';
import { getDownloadUrl } from '@/api/documents';
import { useFormatDate } from '@/hooks/useFormatDate';
import { RichTextDisplay } from '../ui/RichTextDisplay';
import { RichTextEditor } from '../common/RichTextEditor';

interface DocumentListProps {
  propertyId?: string;
  documents: DocumentResponse[];
  isLoading: boolean;
  error: Error | null;
  onUpload: (file: File, title?: string, notes?: string) => Promise<void>;
  onDelete: (documentId: string) => Promise<void>;
  isUploading: boolean;
  isDeleting: boolean;
  readOnly?: boolean;
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const getFileIcon = (mimeType: string) => {
  if (mimeType.startsWith('image/')) {
    return <ImageIcon className="h-8 w-8 text-[#5c7cfa] dark:text-[#91a7ff]" />;
  }
  return <FileText className="h-8 w-8 text-[#6b7194] dark:text-[#8b90a8]" />;
};

export const DocumentList = ({
  documents,
  isLoading,
  error,
  onUpload,
  onDelete,
  isUploading,
  isDeleting,
  readOnly = false,
}: DocumentListProps) => {
  const { formatDate } = useFormatDate();
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadNotes, setUploadNotes] = useState('');
  const [uploadProgress, setUploadProgress] = useState<{
    current: number;
    total: number;
  } | null>(null);
  const [previewDocument, setPreviewDocument] =
    useState<DocumentResponse | null>(null);
  const [pendingBulkDelete, setPendingBulkDelete] = useState<string[] | null>(
    null
  );
  const [editingDocument, setEditingDocument] =
    useState<DocumentResponse | null>(null);

  const {
    selectedDocuments,
    handleSelectDocument,
    handleSelectAll,
    clearSelection,
  } = useDocumentSelection(documents);
  const bulkDownloadMutation = useBulkDownload();
  const updateDocumentMutation = useUpdateDocument();

  const handleBulkDownload = () => {
    if (selectedDocuments.size === 0) return;
    bulkDownloadMutation.mutate(Array.from(selectedDocuments));
  };

  const handleDeleteSingle = (id: string) => {
    setPendingBulkDelete([id]);
  };

  const handleBulkDelete = () => {
    if (selectedDocuments.size === 0) return;
    setPendingBulkDelete(Array.from(selectedDocuments));
  };

  const confirmDelete = () => {
    if (pendingBulkDelete) {
      pendingBulkDelete.forEach((id) => onDelete(id));
      clearSelection();
      setPendingBulkDelete(null);
    }
  };

  const handleDownload = async (documentId: string) => {
    try {
      const url = await getDownloadUrl(documentId);
      window.open(url, '_blank');
    } catch (err) {
      console.error('Download error:', err);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      setSelectedFiles(Array.from(files));
      setUploadTitle('');
      setUploadNotes('');
      setShowUploadModal(true);
    }
    e.target.value = '';
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) return;
    const cleanNotes = uploadNotes.trim();
    const notesValue =
      !cleanNotes || cleanNotes === '<p></p>' ? undefined : cleanNotes;

    setUploadProgress({ current: 0, total: selectedFiles.length });
    for (let i = 0; i < selectedFiles.length; i++) {
      setUploadProgress({ current: i + 1, total: selectedFiles.length });
      try {
        await onUpload(selectedFiles[i], uploadTitle || undefined, notesValue);
      } catch {
        // Error handled by mutation hook; continue remaining uploads
      }
    }
    setUploadProgress(null);
    setShowUploadModal(false);
    setSelectedFiles([]);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleCancelUpload = () => {
    setShowUploadModal(false);
    setSelectedFiles([]);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const hasSelection = selectedDocuments.size > 0;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorMessage message="Failed to load documents" />;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          Documents
        </h3>
        {!readOnly && (
          <label className="cursor-pointer bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2">
            <Upload className="h-4 w-4" />
            Upload Document
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx"
              multiple
              onChange={handleFileSelect}
              className="hidden"
              disabled={isUploading}
            />
          </label>
        )}
      </div>

      {/* Document Table */}
      {documents.length === 0 ? (
        <div className="text-center py-12 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg">
          <FileText className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-3" />
          <p className="text-[#6b7194] dark:text-[#8b90a8]">
            No documents uploaded yet
          </p>
        </div>
      ) : (
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden">
          {/* Selection bar */}
          <div className="px-6 py-3 border-b border-[#edf0f7] dark:border-[#2a2e3f] flex items-center justify-between">
            <div className="flex items-center gap-2">
              <button
                onClick={handleSelectAll}
                className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
              >
                {selectedDocuments.size === documents.length &&
                documents.length > 0 ? (
                  <CheckSquare className="h-5 w-5" />
                ) : (
                  <Square className="h-5 w-5" />
                )}
              </button>
              <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                {hasSelection
                  ? `${selectedDocuments.size} of ${documents.length} selected`
                  : `${documents.length} document${documents.length !== 1 ? 's' : ''}`}
              </span>
            </div>

            {/* Bulk actions */}
            {hasSelection && (
              <div className="flex items-center gap-2">
                <button
                  onClick={handleBulkDownload}
                  disabled={bulkDownloadMutation.isPending}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#2a2e3f] transition-colors disabled:opacity-50"
                >
                  {bulkDownloadMutation.isPending ? (
                    <LoadingSpinner />
                  ) : (
                    <Download className="h-4 w-4" />
                  )}
                  {bulkDownloadMutation.isPending
                    ? 'Downloading...'
                    : 'Download'}
                </button>
                {!readOnly && (
                  <button
                    onClick={handleBulkDelete}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-600 bg-white dark:bg-[#1e2130] border border-red-200 dark:border-red-900/30 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                    Delete
                  </button>
                )}
              </div>
            )}
          </div>

          <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
            <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
              <tr>
                <th className="w-12 px-6 py-3" />
                <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Document
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Size
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Uploaded
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
              {documents.map((doc) => {
                const isSelected = selectedDocuments.has(doc.identifier);
                return (
                  <tr
                    key={doc.identifier}
                    className={`hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer ${
                      isSelected ? 'bg-blue-50 dark:bg-blue-900/20' : ''
                    }`}
                    onClick={() => setPreviewDocument(doc)}
                  >
                    <td
                      className="px-6 py-4"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <button
                        onClick={(e) =>
                          handleSelectDocument(doc.identifier, e.shiftKey)
                        }
                        className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
                      >
                        {isSelected ? (
                          <div className="w-5 h-5 rounded bg-blue-500 flex items-center justify-center">
                            <Check
                              className="h-3.5 w-3.5 text-white"
                              strokeWidth={3}
                            />
                          </div>
                        ) : (
                          <Square className="h-5 w-5" />
                        )}
                      </button>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        {getFileIcon(doc.mimeType)}
                        <div>
                          <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                            {doc.title ?? doc.fileName}
                          </div>
                          {doc.title && doc.title !== doc.fileName ? (
                            <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                              {doc.fileName}
                            </div>
                          ) : null}
                          {doc.notes ? (
                            <RichTextDisplay
                              html={doc.notes}
                              className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1"
                            />
                          ) : null}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      {formatFileSize(doc.fileSize)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      {formatDate(doc.uploadedAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div
                        className="flex justify-end gap-1.5"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <button
                          onClick={() => setPreviewDocument(doc)}
                          className="p-1.5 text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md transition-colors"
                          title="Preview"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {!readOnly && (
                          <button
                            onClick={() => setEditingDocument(doc)}
                            className="p-1.5 text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md transition-colors"
                            title="Edit title & notes"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                        )}
                        <button
                          onClick={() => handleDownload(doc.identifier)}
                          className="p-1.5 text-[#5c7cfa] hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded-md transition-colors"
                          title="Download"
                        >
                          <Download className="h-4 w-4" />
                        </button>
                        {!readOnly && (
                          <button
                            onClick={() => handleDeleteSingle(doc.identifier)}
                            disabled={isDeleting}
                            className="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-md transition-colors disabled:opacity-50"
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Preview Modal */}
      {previewDocument && (
        <DocumentPreviewModal
          document={previewDocument}
          onClose={() => setPreviewDocument(null)}
          onEdit={
            readOnly
              ? undefined
              : () => {
                  const doc = previewDocument;
                  setPreviewDocument(null);
                  setEditingDocument(doc);
                }
          }
        />
      )}

      {/* Edit Modal */}
      {editingDocument && (
        <EditMetadataModal
          title="Edit Document"
          currentTitle={editingDocument.title}
          currentNotes={editingDocument.notes}
          onSave={(title, notes) => {
            updateDocumentMutation.mutate(
              { id: editingDocument.identifier, data: { title, notes } },
              { onSuccess: () => setEditingDocument(null) }
            );
          }}
          onCancel={() => setEditingDocument(null)}
          isLoading={updateDocumentMutation.isPending}
        />
      )}

      {/* Delete Confirmation */}
      {pendingBulkDelete && (
        <ConfirmDialog
          title={
            pendingBulkDelete.length === 1
              ? 'Delete document'
              : 'Delete documents'
          }
          message={
            pendingBulkDelete.length === 1
              ? 'Are you sure you want to delete this document? This action cannot be undone.'
              : `You are about to delete ${pendingBulkDelete.length} documents. This action cannot be undone.`
          }
          confirmLabel={
            pendingBulkDelete.length === 1
              ? 'Delete'
              : `Delete ${pendingBulkDelete.length} documents`
          }
          variant="danger"
          onConfirm={confirmDelete}
          onCancel={() => setPendingBulkDelete(null)}
        />
      )}

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-[#14161f] rounded-lg p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Upload{' '}
                  {selectedFiles.length === 1 ? 'Document' : 'Documents'}
                </h3>
                {selectedFiles.length > 1 && (
                  <span className="px-2 py-0.5 text-xs font-medium bg-[#5c7cfa]/10 text-[#5c7cfa] rounded-full">
                    {selectedFiles.length} files
                  </span>
                )}
              </div>
              <button
                onClick={handleCancelUpload}
                className="p-2 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
                disabled={!!uploadProgress}
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {selectedFiles.length > 0 && (
              <div className="mb-4 space-y-1.5">
                {selectedFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between px-3 py-2 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <FileText className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8] shrink-0" />
                      <span className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                        {file.name}
                      </span>
                      <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] shrink-0">
                        {formatFileSize(file.size)}
                      </span>
                    </div>
                    {!uploadProgress && (
                      <button
                        onClick={() => handleRemoveFile(index)}
                        className="p-1 hover:bg-[#edf0f7] dark:hover:bg-[#2a2e3f] rounded transition-colors shrink-0"
                      >
                        <X className="h-3.5 w-3.5 text-[#6b7194] dark:text-[#8b90a8]" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Title (optional)
                </label>
                <input
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded px-3 py-2"
                  placeholder="e.g., Floor Plan"
                  disabled={!!uploadProgress}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Notes (optional)
                </label>
                <RichTextEditor
                  value={uploadNotes}
                  onChange={setUploadNotes}
                  placeholder="Additional notes about this document"
                  readOnly={!!uploadProgress}
                />
              </div>
            </div>

            <div className="flex gap-2 justify-end mt-6">
              <button
                onClick={handleCancelUpload}
                className="border border-[#c9cfd9] dark:border-[#3a3f54] dark:text-[#c4c8db] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                disabled={!!uploadProgress}
              >
                Cancel
              </button>
              <button
                onClick={handleUpload}
                className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 flex items-center gap-2"
                disabled={!!uploadProgress || selectedFiles.length === 0}
              >
                {uploadProgress ? (
                  <>
                    <LoadingSpinner />
                    Uploading {uploadProgress.current} of{' '}
                    {uploadProgress.total}...
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4" />
                    Upload
                    {selectedFiles.length > 1
                      ? ` ${selectedFiles.length} Documents`
                      : ''}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
