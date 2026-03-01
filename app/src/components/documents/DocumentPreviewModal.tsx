import { useEffect, useRef, useCallback } from 'react';
import {
  X,
  Download,
  ExternalLink,
  Pencil,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { DocumentResponse, PhotoResponse } from '@/types/property';
import { RichTextDisplay } from '../ui/RichTextDisplay';

interface DocumentPreviewModalProps {
  document: DocumentResponse | PhotoResponse;
  onClose: () => void;
  onEdit?: () => void;
  onNext?: () => void;
  onPrevious?: () => void;
  currentIndex?: number;
  totalCount?: number;
}

const SWIPE_THRESHOLD = 50;

export const DocumentPreviewModal = ({
  document,
  onClose,
  onEdit,
  onNext,
  onPrevious,
  currentIndex,
  totalCount,
}: DocumentPreviewModalProps) => {
  const touchStartX = useRef<number | null>(null);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
      if (e.key === 'ArrowRight' && onNext) {
        onNext();
      }
      if (e.key === 'ArrowLeft' && onPrevious) {
        onPrevious();
      }
    },
    [onClose, onNext, onPrevious]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null) {
      return;
    }
    const delta = e.changedTouches[0].clientX - touchStartX.current;
    touchStartX.current = null;
    if (delta > SWIPE_THRESHOLD && onPrevious) {
      onPrevious();
    }
    if (delta < -SWIPE_THRESHOLD && onNext) {
      onNext();
    }
  };

  const isImage = document.mimeType.startsWith('image/');
  const isPDF = document.mimeType === 'application/pdf';
  const canPreview = isImage || isPDF;
  const showPosition =
    currentIndex !== undefined && totalCount !== undefined && totalCount > 1;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" onClick={onClose}>
      <div className="flex items-center justify-center min-h-screen px-4 py-8">
        {/* Background overlay */}
        <div className="fixed inset-0 bg-black bg-opacity-60" />

        {/* Previous button */}
        {onPrevious && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onPrevious();
            }}
            className="fixed left-4 top-1/2 -translate-y-1/2 z-[60] p-2 rounded-full bg-black/40 text-white/70 hover:bg-black/60 hover:text-white transition-colors"
            title="Previous"
          >
            <ChevronLeft className="h-6 w-6" />
          </button>
        )}

        {/* Next button */}
        {onNext && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onNext();
            }}
            className="fixed right-4 top-1/2 -translate-y-1/2 z-[60] p-2 rounded-full bg-black/40 text-white/70 hover:bg-black/60 hover:text-white transition-colors"
            title="Next"
          >
            <ChevronRight className="h-6 w-6" />
          </button>
        )}

        {/* Modal panel */}
        <div
          className="relative bg-white dark:bg-[#14161f] rounded-lg text-left overflow-hidden shadow-xl dark:shadow-black/20 w-full max-w-4xl"
          onClick={(e) => e.stopPropagation()}
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
        >
          {/* Header */}
          <div className="bg-white dark:bg-[#14161f] px-4 py-3 border-b border-[#e2e6f0] dark:border-[#2a2e3f] flex items-center justify-between">
            <div className="flex-1 min-w-0 flex items-center gap-3">
              <div className="min-w-0">
                <h3 className="text-lg font-medium text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                  {document.title ?? document.fileName}
                </h3>
                {document.title && document.title !== document.fileName ? (
                  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] truncate">
                    {document.fileName}
                  </p>
                ) : null}
              </div>
              {showPosition && (
                <span className="shrink-0 text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] bg-[#f1f3f9] dark:bg-[#1e2130] px-2 py-1 rounded-full">
                  {currentIndex! + 1} / {totalCount}
                </span>
              )}
            </div>
            <div className="flex items-center gap-2 ml-4">
              {onEdit && (
                <button
                  onClick={onEdit}
                  className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md"
                  title="Edit title & notes"
                >
                  <Pencil className="h-5 w-5" />
                </button>
              )}
              <a
                href={document.downloadUrl ?? undefined}
                download
                className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md"
                title="Download"
              >
                <Download className="h-5 w-5" />
              </a>
              <a
                href={document.downloadUrl ?? undefined}
                target="_blank"
                rel="noopener noreferrer"
                className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md"
                title="Open in new tab"
              >
                <ExternalLink className="h-5 w-5" />
              </a>
              <button
                onClick={onClose}
                className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md"
                title="Close"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          {/* Preview Content */}
          <div
            className="bg-[#f1f3f9] dark:bg-[#1e2130] p-4"
            style={{ maxHeight: '70vh', overflow: 'auto' }}
          >
            {canPreview ? (
              <>
                {isImage && (
                  <div className="flex justify-center">
                    <img
                      src={document.downloadUrl ?? undefined}
                      alt={document.title || document.fileName}
                      className="max-w-full h-auto rounded-lg shadow-lg"
                      crossOrigin="anonymous"
                    />
                  </div>
                )}
                {isPDF && (
                  <div className="w-full h-full min-h-[600px]">
                    <iframe
                      src={document.downloadUrl ?? undefined}
                      title={document.title || document.fileName}
                      className="w-full h-full border-0 rounded-lg shadow-lg"
                      style={{ minHeight: '600px' }}
                    />
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12">
                <p className="text-[#6b7194] dark:text-[#8b90a8] mb-4">
                  Preview not available for this file type ({document.mimeType})
                </p>
                <a
                  href={document.downloadUrl ?? undefined}
                  download
                  className="inline-flex items-center gap-2 px-4 py-2 bg-[#5c7cfa] text-white rounded-md hover:bg-blue-700"
                >
                  <Download className="h-4 w-4" />
                  Download File
                </a>
              </div>
            )}
          </div>

          {/* Footer with document info */}
          {document.notes ? (
            <div className="bg-white dark:bg-[#14161f] px-4 py-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
              <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                <span className="font-medium">Notes:</span>
                <RichTextDisplay
                  html={document.notes}
                  className="mt-1 text-sm"
                />
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};
