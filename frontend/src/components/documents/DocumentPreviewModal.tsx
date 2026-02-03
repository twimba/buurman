import { X, Download, ExternalLink } from 'lucide-react';
import { DocumentResponse } from '@/types/property';

interface DocumentPreviewModalProps {
  document: DocumentResponse;
  onClose: () => void;
}

export const DocumentPreviewModal = ({
  document,
  onClose,
}: DocumentPreviewModalProps) => {
  const isImage = document.mimeType.startsWith('image/');
  const isPDF = document.mimeType === 'application/pdf';
  const canPreview = isImage || isPDF;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" onClick={onClose}>
      <div className="flex items-center justify-center min-h-screen px-4 py-8">
        {/* Background overlay */}
        <div className="fixed inset-0 bg-black bg-opacity-60" />

        {/* Modal panel */}
        <div
          className="relative bg-white dark:bg-gray-800 rounded-lg text-left overflow-hidden shadow-xl dark:shadow-gray-900 w-full max-w-4xl"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="bg-white dark:bg-gray-800 px-4 py-3 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
            <div className="flex-1 min-w-0">
              <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 truncate">
                {document.title ?? document.fileName}
              </h3>
              {document.title && document.title !== document.fileName ? (
                <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                  {document.fileName}
                </p>
              ) : null}
            </div>
            <div className="flex items-center gap-2 ml-4">
              <a
                href={document.downloadUrl || undefined}
                download
                className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md"
                title="Download"
              >
                <Download className="h-5 w-5" />
              </a>
              <a
                href={document.downloadUrl || undefined}
                target="_blank"
                rel="noopener noreferrer"
                className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md"
                title="Open in new tab"
              >
                <ExternalLink className="h-5 w-5" />
              </a>
              <button
                onClick={onClose}
                className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md"
                title="Close"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          {/* Preview Content */}
          <div
            className="bg-gray-100 p-4"
            style={{ maxHeight: '70vh', overflow: 'auto' }}
          >
            {canPreview ? (
              <>
                {isImage && (
                  <div className="flex justify-center">
                    <img
                      src={document.downloadUrl || undefined}
                      alt={document.title || document.fileName}
                      className="max-w-full h-auto rounded-lg shadow-lg"
                      crossOrigin="anonymous"
                    />
                  </div>
                )}
                {isPDF && (
                  <div className="w-full h-full min-h-[600px]">
                    <iframe
                      src={document.downloadUrl || undefined}
                      title={document.title || document.fileName}
                      className="w-full h-full border-0 rounded-lg shadow-lg"
                      style={{ minHeight: '600px' }}
                    />
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12">
                <p className="text-gray-600 mb-4">
                  Preview not available for this file type ({document.mimeType})
                </p>
                <a
                  href={document.downloadUrl || undefined}
                  download
                  className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  <Download className="h-4 w-4" />
                  Download File
                </a>
              </div>
            )}
          </div>

          {/* Footer with document info */}
          {document.notes ? (
            <div className="bg-white px-4 py-3 border-t border-gray-200">
              <p className="text-sm text-gray-600">
                <span className="font-medium">Notes:</span> {document.notes}
              </p>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};
