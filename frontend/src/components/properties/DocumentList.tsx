import { useState } from 'react';
import { DocumentResponse } from '@/types/property';
import {
  Upload,
  FileText,
  Image as ImageIcon,
  Download,
  Trash2,
  Eye,
} from 'lucide-react';
import { LoadingSpinner } from '../LoadingSpinner';
import { ErrorMessage } from '../ErrorMessage';
import { getDocumentDownloadUrl } from '@/api/properties';
import { DocumentPreviewModal } from '../documents/DocumentPreviewModal';

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
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [notes, setNotes] = useState('');
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [previewDocument, setPreviewDocument] =
    useState<DocumentResponse | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setUploadError(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setUploadError('Please select a file');
      return;
    }

    try {
      await onUpload(selectedFile, title || undefined, notes || undefined);
      setSelectedFile(null);
      setTitle('');
      setNotes('');
      setUploadError(null);
      // Reset file input
      const fileInput = document.getElementById(
        'file-input'
      ) as HTMLInputElement;
      if (fileInput) fileInput.value = '';
    } catch (err) {
      setUploadError('Failed to upload document');
      console.error('Upload error:', err);
    }
  };

  const handleDownload = async (documentId: string) => {
    try {
      const { url } = await getDocumentDownloadUrl(documentId);
      window.open(url, '_blank');
    } catch (err) {
      console.error('Download error:', err);
    }
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) {
      return (
        <ImageIcon className="h-8 w-8 text-[#5c7cfa] dark:text-[#91a7ff]" />
      );
    }
    return <FileText className="h-8 w-8 text-[#6b7194] dark:text-[#8b90a8]" />;
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorMessage message="Failed to load documents" />;
  }

  return (
    <div className="space-y-6">
      {/* Upload Section */}
      {!readOnly && (
        <div className="bg-white dark:bg-[#14161f] border-2 border-dashed border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg p-6">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4 flex items-center gap-2">
            <Upload className="h-5 w-5" />
            Upload Document
          </h3>

          <div className="space-y-4">
            {/* File Input */}
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                Select File
              </label>
              <input
                id="file-input"
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,.gif"
                onChange={handleFileChange}
                className="block w-full text-sm text-[#6b7194] dark:text-[#8b90a8] file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-blue-50 dark:file:bg-blue-900 file:text-blue-700 dark:file:text-blue-300 hover:file:bg-blue-100 dark:hover:file:bg-blue-800"
              />
              {selectedFile && (
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-2">
                  Selected: {selectedFile.name} (
                  {formatFileSize(selectedFile.size)})
                </p>
              )}
            </div>

            {/* Title Input */}
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Title (optional)
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Floor Plan"
                className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              />
            </div>

            {/* Notes Input */}
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Notes (optional)
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Main floor layout"
                rows={3}
                className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              />
            </div>

            {/* Upload Button */}
            <button
              onClick={handleUpload}
              disabled={!selectedFile || isUploading}
              className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <Upload className="h-4 w-4" />
              {isUploading ? 'Uploading...' : 'Upload'}
            </button>

            {uploadError && (
              <p className="text-red-600 text-sm">{uploadError}</p>
            )}
          </div>
        </div>
      )}

      {/* Documents List */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Documents ({documents.length})
        </h3>

        {documents.length === 0 ? (
          <div className="text-center py-12 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg">
            <FileText className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
            <p className="text-[#6b7194] dark:text-[#8b90a8]">
              No documents uploaded yet
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {documents.map((doc) => (
              <div
                key={doc.id}
                className="bg-white dark:bg-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg p-4 hover:shadow-md dark:hover:shadow-black/20 transition-shadow cursor-pointer"
                onClick={() => setPreviewDocument(doc)}
              >
                <div className="flex items-start gap-3">
                  {/* File Icon */}
                  <div className="flex-shrink-0">
                    {getFileIcon(doc.mimeType)}
                  </div>

                  {/* Document Info */}
                  <div className="flex-1 min-w-0">
                    {doc.title && (
                      <h4 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-1">
                        {doc.title}
                      </h4>
                    )}
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] truncate mb-1">
                      {doc.fileName}
                    </p>
                    <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180]">
                      {formatFileSize(doc.fileSize)}
                    </p>
                    {doc.notes && (
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-2">
                        {doc.notes}
                      </p>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setPreviewDocument(doc);
                      }}
                      className="p-2 text-primary-500 dark:text-primary-300 hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded transition-colors"
                      title="Preview"
                    >
                      <Eye className="h-4 w-4" />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDownload(doc.id);
                      }}
                      className="p-2 text-primary-500 dark:text-primary-300 hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded transition-colors"
                      title="Download"
                    >
                      <Download className="h-4 w-4" />
                    </button>
                    {!readOnly && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onDelete(doc.id);
                        }}
                        disabled={isDeleting}
                        className="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-[#1e2130] rounded transition-colors disabled:opacity-50"
                        title="Delete"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Document Preview Modal */}
      {previewDocument && (
        <DocumentPreviewModal
          document={previewDocument}
          onClose={() => setPreviewDocument(null)}
        />
      )}
    </div>
  );
};
