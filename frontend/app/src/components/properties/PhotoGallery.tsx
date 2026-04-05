import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PhotoResponse } from '@/types/property';
import { Upload, X, Loader2 } from 'lucide-react';
import { ConfirmDialog, RichTextEditor } from '@buurman/ui';
import { PhotoGrid } from '../photos/PhotoGrid';
import { DocumentPreviewModal } from '../documents/DocumentPreviewModal';
import { EditMetadataModal } from '../ui/EditMetadataModal';
import { usePhotoSelection } from '@/hooks/usePhotoSelection';
import { useBulkDownloadPhotos, useUpdatePhoto } from '@/hooks/usePhotoHooks';

interface PhotoGalleryProps {
  photos: PhotoResponse[];
  isLoading: boolean;
  error: unknown;
  onUpload: (file: File, title?: string, notes?: string) => Promise<void>;
  onSetMain: (photoId: string) => Promise<void>;
  onDelete: (photoId: string) => Promise<void>;
  isUploading: boolean;
  isDeleting: boolean;
  readOnly?: boolean;
}

export const PhotoGallery = ({
  photos,
  isLoading,
  error,
  onUpload,
  onSetMain,
  onDelete,
  isUploading,
  isDeleting,
  readOnly = false,
}: PhotoGalleryProps) => {
  const { t } = useTranslation('properties');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadNotes, setUploadNotes] = useState('');
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const [uploadProgress, setUploadProgress] = useState<{
    current: number;
    total: number;
  } | null>(null);
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);
  const [pendingBulkDelete, setPendingBulkDelete] = useState<string[] | null>(
    null
  );
  const [editingPhoto, setEditingPhoto] = useState<PhotoResponse | null>(null);

  const { selectedPhotos, handleSelectPhoto, handleSelectAll, clearSelection } =
    usePhotoSelection(photos);
  const bulkDownloadMutation = useBulkDownloadPhotos();
  const updatePhotoMutation = useUpdatePhoto();

  const handleBulkDownload = (ids: string[]) => {
    bulkDownloadMutation.mutate(ids);
  };

  const handleDeleteSingle = (id: string) => {
    setPendingBulkDelete([id]);
  };

  const handleBulkDelete = (ids: string[]) => {
    setPendingBulkDelete(ids);
  };

  const confirmDelete = () => {
    if (pendingBulkDelete) {
      pendingBulkDelete.forEach((id) => onDelete(id));
      clearSelection();
      setPendingBulkDelete(null);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      const fileArray = Array.from(files);
      setSelectedFiles(fileArray);
      setUploadTitle('');
      setUploadNotes('');
      setPreviewUrls(fileArray.map((f) => URL.createObjectURL(f)));
      setShowUploadModal(true);
    }
    e.target.value = '';
  };

  const handleCmdEnterUpload = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      handleUpload();
    }
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) {
      return;
    }
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
    previewUrls.forEach((url) => URL.revokeObjectURL(url));
    setShowUploadModal(false);
    setSelectedFiles([]);
    setPreviewUrls([]);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleCancelUpload = () => {
    previewUrls.forEach((url) => URL.revokeObjectURL(url));
    setShowUploadModal(false);
    setSelectedFiles([]);
    setPreviewUrls([]);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleRemoveFile = (index: number) => {
    URL.revokeObjectURL(previewUrls[index]);
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
    setPreviewUrls((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-text-primary">{t('photos.title')}</h3>
        {!readOnly && (
          <label className="cursor-pointer bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2">
            <Upload className="h-4 w-4" />
            {t('photos.uploadButton')}
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileSelect}
              className="hidden"
              disabled={isUploading}
            />
          </label>
        )}
      </div>

      {/* Photo Grid */}
      <PhotoGrid
        photos={photos}
        isLoading={isLoading}
        error={error}
        emptyMessage={t('photos.empty')}
        selectedPhotos={selectedPhotos}
        onSelectPhoto={handleSelectPhoto}
        onSelectAll={handleSelectAll}
        onSetMain={readOnly ? undefined : onSetMain}
        onDelete={readOnly ? undefined : handleDeleteSingle}
        onEdit={readOnly ? undefined : setEditingPhoto}
        onBulkDownload={handleBulkDownload}
        onBulkDelete={readOnly ? undefined : handleBulkDelete}
        isBulkDownloading={bulkDownloadMutation.isPending}
        onPreview={(photo) => setPreviewIndex(photos.indexOf(photo))}
        showMainBadge
        readOnly={readOnly}
        disableActions={isUploading || isDeleting}
      />

      {/* Preview Modal */}
      {previewIndex !== null && photos[previewIndex] && (
        <DocumentPreviewModal
          document={photos[previewIndex]}
          onClose={() => setPreviewIndex(null)}
          onEdit={
            readOnly
              ? undefined
              : () => {
                  const photo = photos[previewIndex];
                  setPreviewIndex(null);
                  setEditingPhoto(photo);
                }
          }
          onPrevious={
            previewIndex > 0
              ? () => setPreviewIndex(previewIndex - 1)
              : undefined
          }
          onNext={
            previewIndex < photos.length - 1
              ? () => setPreviewIndex(previewIndex + 1)
              : undefined
          }
          currentIndex={previewIndex}
          totalCount={photos.length}
        />
      )}

      {/* Edit Modal */}
      {editingPhoto && (
        <EditMetadataModal
          title={t('photos.editModal')}
          currentTitle={editingPhoto.title}
          currentNotes={editingPhoto.notes}
          onSave={(title, notes) => {
            updatePhotoMutation.mutate(
              { id: editingPhoto.identifier, data: { title, notes } },
              { onSuccess: () => setEditingPhoto(null) }
            );
          }}
          onCancel={() => setEditingPhoto(null)}
          isLoading={updatePhotoMutation.isPending}
        />
      )}

      {/* Delete Confirmation */}
      {pendingBulkDelete &&
        (() => {
          const includesMain = pendingBulkDelete.some(
            (id) => photos.find((p) => p.identifier === id)?.isMainPhoto
          );
          const isSingle = pendingBulkDelete.length === 1;
          return (
            <ConfirmDialog
              title={t('photos.delete.title', { count: pendingBulkDelete.length })}
              message={
                includesMain
                  ? t('photos.delete.mainPhotoWarning', { count: pendingBulkDelete.length })
                  : t('photos.delete.message', { count: pendingBulkDelete.length })
              }
              confirmLabel={
                isSingle
                  ? t('buttons.delete', { ns: 'common' })
                  : t('photos.delete.confirmLabel', { count: pendingBulkDelete.length })
              }
              variant="danger"
              onConfirm={confirmDelete}
              onCancel={() => setPendingBulkDelete(null)}
            />
          );
        })()}

      {/* Upload Modal */}
      {showUploadModal && (
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4"
          onKeyDown={handleCmdEnterUpload}
        >
          <div className="bg-surface-card rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-semibold text-text-primary">
                  {t('photos.upload.title', { count: selectedFiles.length })}
                </h3>
                {selectedFiles.length > 1 && (
                  <span className="px-2 py-0.5 text-xs font-medium bg-primary-500/10 text-primary-500 rounded-full">
                    {t('photos.upload.files', { count: selectedFiles.length })}
                  </span>
                )}
              </div>
              <button
                onClick={handleCancelUpload}
                className="p-2 hover:bg-surface-inset rounded transition-colors"
                disabled={!!uploadProgress}
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {previewUrls.length > 0 && (
              <div className="mb-4 grid grid-cols-3 sm:grid-cols-4 gap-2">
                {previewUrls.map((url, index) => (
                  <div key={index} className="relative group aspect-square">
                    <img
                      src={url}
                      alt={selectedFiles[index]?.name}
                      className="w-full h-full object-cover rounded-lg"
                    />
                    {!uploadProgress && (
                      <button
                        onClick={() => handleRemoveFile(index)}
                        className="absolute top-1 right-1 p-1 bg-black/60 rounded-full text-white opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <X className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('photos.upload.titleLabel')}
                </label>
                <input
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  className="w-full border border-border-strong rounded px-3 py-2"
                  placeholder={t('photos.upload.titlePlaceholder')}
                  disabled={!!uploadProgress}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('photos.upload.notesLabel')}
                </label>
                <RichTextEditor
                  value={uploadNotes}
                  onChange={setUploadNotes}
                  placeholder={t('photos.upload.notesPlaceholder')}
                  readOnly={!!uploadProgress}
                  onSubmit={handleUpload}
                />
              </div>
            </div>

            <div className="flex gap-2 justify-end mt-6">
              <button
                onClick={handleCancelUpload}
                className="border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors"
                disabled={!!uploadProgress}
              >
                {t('buttons.cancel', { ns: 'common' })}
              </button>
              <button
                onClick={handleUpload}
                className="relative overflow-hidden bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
                disabled={!!uploadProgress || selectedFiles.length === 0}
                aria-busy={!!uploadProgress}
              >
                {uploadProgress ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {t('photos.upload.uploading', { current: uploadProgress.current, total: uploadProgress.total })}
                    <div
                      role="progressbar"
                      aria-valuenow={uploadProgress.current}
                      aria-valuemin={0}
                      aria-valuemax={uploadProgress.total}
                      className="absolute bottom-0 left-0 h-0.5 bg-surface-card/30 transition-all duration-300"
                      style={{
                        width: `${(uploadProgress.current / uploadProgress.total) * 100}%`,
                      }}
                    />
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4" />
                    {selectedFiles.length > 1
                      ? t('photos.upload.uploadCount', { count: selectedFiles.length })
                      : t('buttons.upload', { ns: 'common' })}
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
