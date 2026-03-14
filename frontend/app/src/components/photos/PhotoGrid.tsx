import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Star,
  StarOff,
  Trash2,
  Download,
  CheckSquare,
  Square,
  AlertCircle,
  Upload,
  Check,
  Pencil,
  Loader2,
} from 'lucide-react';
import { PhotoResponse } from '@/types/property';
import { LoadingSpinner } from '../LoadingSpinner';
import { ErrorMessage } from '../ErrorMessage';
import { useFormatDate } from '@/hooks/useFormatDate';
import { RichTextDisplay } from '../ui/RichTextDisplay';

interface PhotoGridProps {
  photos: PhotoResponse[];
  isLoading: boolean;
  error?: unknown;
  emptyMessage?: string;
  totalCount?: number;

  // Selection
  selectedPhotos: Set<string>;
  onSelectPhoto: (id: string, shiftKey?: boolean) => void;
  onSelectAll: () => void;

  // Bulk actions (render bar only if callbacks provided and selection > 0)
  onBulkDownload?: (ids: string[]) => void;
  onBulkDelete?: (ids: string[]) => void;
  isBulkDownloading?: boolean;

  // Actions (render buttons only if callback provided)
  onSetMain?: (id: string) => void;
  onDelete?: (id: string) => void;
  onEdit?: (photo: PhotoResponse) => void;
  onPreview?: (photo: PhotoResponse) => void;

  // Display options
  showEntityLink?: boolean;
  showMainBadge?: boolean;
  readOnly?: boolean;
  disableActions?: boolean;
}

const ENTITY_PATHS: Record<string, string> = {
  property: '/properties',
  tenant: '/tenants',
  contract: '/contracts',
  payment: '/payments',
  expense: '/expenses',
};

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

export const PhotoGrid = ({
  photos,
  isLoading,
  error,
  emptyMessage = 'No photos yet',
  totalCount,
  selectedPhotos,
  onSelectPhoto,
  onSelectAll,
  onBulkDownload,
  onBulkDelete,
  isBulkDownloading = false,
  onSetMain,
  onDelete,
  onEdit,
  onPreview,
  showEntityLink = false,
  showMainBadge = true,
  readOnly = false,
  disableActions = false,
}: PhotoGridProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const [imageErrors, setImageErrors] = useState<Set<string>>(new Set());

  const displayCount = totalCount ?? photos.length;
  const hasActions = !readOnly && (onSetMain || onDelete || onEdit);
  const hasSelection = selectedPhotos.size > 0;
  const selectedIds = Array.from(selectedPhotos);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorMessage message="Failed to load photos" />;
  }

  if (photos.length === 0) {
    return (
      <div className="text-center py-12 bg-surface-page rounded-lg">
        <Upload className="h-12 w-12 text-text-muted mx-auto mb-3" />
        <p className="text-text-secondary">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Select all + counter + bulk actions */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={onSelectAll}
            className="text-text-secondary hover:text-text-secondary"
          >
            {selectedPhotos.size === photos.length && photos.length > 0 ? (
              <CheckSquare className="h-5 w-5" />
            ) : (
              <Square className="h-5 w-5" />
            )}
          </button>
          <span className="text-sm text-text-secondary">
            {hasSelection
              ? `${selectedPhotos.size} of ${displayCount} selected`
              : `${displayCount} photo${displayCount !== 1 ? 's' : ''}`}
          </span>
        </div>

        {/* Bulk actions */}
        {hasSelection && (onBulkDownload || onBulkDelete) && (
          <div className="flex items-center gap-2">
            {onBulkDownload && (
              <button
                onClick={() => onBulkDownload(selectedIds)}
                disabled={isBulkDownloading}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50"
              >
                {isBulkDownloading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Download className="h-4 w-4" />
                )}
                {isBulkDownloading ? 'Downloading...' : 'Download'}
              </button>
            )}
            {onBulkDelete && (
              <button
                onClick={() => onBulkDelete(selectedIds)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-error-text bg-surface-card border border-error-border rounded-md hover:bg-error-bg transition-colors"
              >
                <Trash2 className="h-4 w-4" />
                Delete
              </button>
            )}
          </div>
        )}
      </div>

      {/* Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
        {photos.map((photo) => {
          const hasError = imageErrors.has(photo.identifier);
          const isSelected = selectedPhotos.has(photo.identifier);

          return (
            <div
              key={photo.identifier}
              className={`relative group rounded-lg overflow-hidden border-2 transition-all ${
                isSelected
                  ? 'border-primary-500'
                  : showMainBadge && photo.isMainPhoto
                    ? 'border-primary-500'
                    : 'border-border-default hover:border-border-strong'
              }`}
            >
              {/* Image */}
              <div
                className={`aspect-square bg-surface-inset ${onPreview && !hasError ? 'cursor-pointer' : ''}`}
                onClick={() => onPreview && !hasError && onPreview(photo)}
              >
                {!photo.downloadUrl ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="text-center p-4">
                      <AlertCircle className="h-8 w-8 text-text-muted mx-auto mb-2" />
                      <p className="text-xs text-text-secondary">No URL</p>
                    </div>
                  </div>
                ) : hasError ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="text-center p-4">
                      <AlertCircle className="h-8 w-8 text-error-text mx-auto mb-2" />
                      <p className="text-xs text-text-secondary font-medium">
                        Failed to load
                      </p>
                    </div>
                  </div>
                ) : (
                  <img
                    src={photo.thumbnailUrl ?? photo.downloadUrl}
                    alt={photo.title ?? photo.fileName}
                    className="w-full h-full object-cover"
                    loading="lazy"
                    crossOrigin="anonymous"
                    onError={() => {
                      setImageErrors((prev) =>
                        new Set(prev).add(photo.identifier)
                      );
                    }}
                  />
                )}
              </div>

              {/* Main photo badge */}
              {showMainBadge && photo.isMainPhoto && (
                <div className="absolute top-2 left-2 bg-primary-500 text-white px-2 py-1 rounded text-xs font-semibold flex items-center gap-1 z-10">
                  <Star className="h-3 w-3 fill-white" />
                  Main
                </div>
              )}

              {/* Selection indicator — persistent when selected, hover when not */}
              {isSelected ? (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelectPhoto(photo.identifier, e.shiftKey);
                  }}
                  className={`absolute ${showMainBadge && photo.isMainPhoto ? 'top-10' : 'top-2'} left-2 z-20`}
                >
                  <div className="w-6 h-6 rounded bg-primary-500 flex items-center justify-center shadow">
                    <Check className="h-4 w-4 text-white" strokeWidth={3} />
                  </div>
                </button>
              ) : (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelectPhoto(photo.identifier, e.shiftKey);
                  }}
                  className={`absolute ${showMainBadge && photo.isMainPhoto ? 'top-10' : 'top-2'} left-2 opacity-0 group-hover:opacity-100 transition-opacity z-20`}
                >
                  <Square className="h-5 w-5 text-white drop-shadow-md" />
                </button>
              )}

              {/* Action buttons - top right */}
              {hasActions && (
                <div className="absolute top-2 right-2 flex gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                  {onSetMain && !photo.isMainPhoto && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSetMain(photo.identifier);
                      }}
                      className="p-1.5 bg-surface-card/80 backdrop-blur-sm text-text-primary rounded-md hover:bg-surface-card transition-colors shadow"
                      title="Set as main photo"
                      disabled={disableActions}
                    >
                      <StarOff className="h-4 w-4" />
                    </button>
                  )}
                  {photo.downloadUrl && (
                    <a
                      href={photo.downloadUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      className="p-1.5 bg-surface-card/80 backdrop-blur-sm text-text-secondary rounded-md hover:bg-surface-card transition-colors shadow"
                      title="Download"
                    >
                      <Download className="h-4 w-4" />
                    </a>
                  )}
                  {onEdit && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onEdit(photo);
                      }}
                      className="p-1.5 bg-surface-card/80 backdrop-blur-sm text-text-secondary rounded-md hover:bg-surface-card transition-colors shadow"
                      title="Edit title & notes"
                      disabled={disableActions}
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                  )}
                  {onDelete && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDelete(photo.identifier);
                      }}
                      className="p-1.5 bg-surface-card/80 backdrop-blur-sm text-error-text rounded-md hover:bg-error-bg transition-colors shadow"
                      title="Delete photo"
                      disabled={disableActions}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              )}

              {/* Info bar */}
              <div className="p-2 bg-surface-card">
                <p className="text-sm font-medium text-text-primary truncate">
                  {photo.title ?? photo.fileName}
                </p>
                {photo.notes && (
                  <div className="mt-0.5">
                    <RichTextDisplay
                      html={photo.notes}
                      className="text-xs text-text-secondary"
                    />
                  </div>
                )}
                {showEntityLink && (
                  <>
                    <div className="flex items-center justify-between mt-1">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          const basePath =
                            ENTITY_PATHS[photo.entityType.toLowerCase()];
                          if (basePath) {
                            navigate(`${basePath}/${photo.entityIdentifier}`);
                          }
                        }}
                        className="text-xs text-primary-500 hover:underline"
                      >
                        {photo.entityType}
                      </button>
                      <span className="text-xs text-text-secondary">
                        {formatFileSize(photo.fileSize)}
                      </span>
                    </div>
                    <p className="text-xs text-text-secondary mt-1">
                      {formatDate(photo.uploadedAt)}
                    </p>
                  </>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
