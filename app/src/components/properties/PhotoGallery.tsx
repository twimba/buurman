import { useState } from 'react';
import { PhotoResponse } from '@/types/property';
import { Upload, X } from 'lucide-react';
import { LoadingSpinner } from '../LoadingSpinner';
import { RichTextEditor } from '../common/RichTextEditor';
import { PhotoGrid } from '../photos/PhotoGrid';
import { DocumentPreviewModal } from '../documents/DocumentPreviewModal';
import { ConfirmDialog } from '../ui/ConfirmDialog';
import { EditMetadataModal } from '../ui/EditMetadataModal';
import { usePhotoSelection } from '@/hooks/usePhotoSelection';
import { useBulkDownloadPhotos, useUpdatePhoto } from '@/hooks/usePhotoHooks';

interface PhotoGalleryProps {
  propertyId: string;
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
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadNotes, setUploadNotes] = useState('');
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const [uploadProgress, setUploadProgress] = useState<{
    current: number;
    total: number;
  } | null>(null);
  const [previewPhoto, setPreviewPhoto] = useState<PhotoResponse | null>(null);
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
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          Photos
        </h3>
        {!readOnly && (
          <label className="cursor-pointer bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2">
            <Upload className="h-4 w-4" />
            Upload Photo
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
        emptyMessage="No photos yet"
        selectedPhotos={selectedPhotos}
        onSelectPhoto={handleSelectPhoto}
        onSelectAll={handleSelectAll}
        onSetMain={readOnly ? undefined : onSetMain}
        onDelete={readOnly ? undefined : handleDeleteSingle}
        onEdit={readOnly ? undefined : setEditingPhoto}
        onBulkDownload={handleBulkDownload}
        onBulkDelete={readOnly ? undefined : handleBulkDelete}
        isBulkDownloading={bulkDownloadMutation.isPending}
        onPreview={setPreviewPhoto}
        showMainBadge
        readOnly={readOnly}
        disableActions={isUploading || isDeleting}
      />

      {/* Preview Modal */}
      {previewPhoto && (
        <DocumentPreviewModal
          document={previewPhoto}
          onClose={() => setPreviewPhoto(null)}
          onEdit={
            readOnly
              ? undefined
              : () => {
                  const photo = previewPhoto;
                  setPreviewPhoto(null);
                  setEditingPhoto(photo);
                }
          }
        />
      )}

      {/* Edit Modal */}
      {editingPhoto && (
        <EditMetadataModal
          title="Edit Photo"
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
              title={isSingle ? 'Delete photo' : 'Delete photos'}
              message={
                includesMain
                  ? isSingle
                    ? 'This is the main photo. Deleting it means this property will no longer have a main photo. This action cannot be undone.'
                    : `You are about to delete ${pendingBulkDelete.length} photos, including the main photo. This property will no longer have a main photo. This action cannot be undone.`
                  : isSingle
                    ? 'Are you sure you want to delete this photo? This action cannot be undone.'
                    : `You are about to delete ${pendingBulkDelete.length} photos. This action cannot be undone.`
              }
              confirmLabel={
                isSingle
                  ? 'Delete'
                  : `Delete ${pendingBulkDelete.length} photos`
              }
              variant="danger"
              onConfirm={confirmDelete}
              onCancel={() => setPendingBulkDelete(null)}
            />
          );
        })()}

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-[#14161f] rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Upload {selectedFiles.length === 1 ? 'Photo' : 'Photos'}
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
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Title (optional)
                </label>
                <input
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded px-3 py-2"
                  placeholder="e.g., Living room"
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
                  placeholder="Additional notes about this photo"
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
                    Uploading {uploadProgress.current} of {uploadProgress.total}
                    ...
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4" />
                    Upload
                    {selectedFiles.length > 1
                      ? ` ${selectedFiles.length} Photos`
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
