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
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadNotes, setUploadNotes] = useState('');
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
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
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setUploadTitle('');
      setUploadNotes('');

      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result as string);
      };
      reader.readAsDataURL(file);

      setShowUploadModal(true);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    const cleanNotes = uploadNotes.trim();
    const notesValue = !cleanNotes || cleanNotes === '<p></p>' ? undefined : cleanNotes;

    await onUpload(
      selectedFile,
      uploadTitle || undefined,
      notesValue
    );
    setShowUploadModal(false);
    setSelectedFile(null);
    setPreviewUrl(null);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleCancelUpload = () => {
    setShowUploadModal(false);
    setSelectedFile(null);
    setPreviewUrl(null);
    setUploadTitle('');
    setUploadNotes('');
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
          onEdit={readOnly ? undefined : () => {
            const photo = previewPhoto;
            setPreviewPhoto(null);
            setEditingPhoto(photo);
          }}
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
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Upload Photo
              </h3>
              <button
                onClick={handleCancelUpload}
                className="p-2 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
                disabled={isUploading}
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {previewUrl && (
              <div className="mb-4">
                <img
                  src={previewUrl}
                  alt="Preview"
                  className="w-full max-h-96 object-contain rounded-lg"
                />
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
                  disabled={isUploading}
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
                  readOnly={isUploading}
                />
              </div>
            </div>

            <div className="flex gap-2 justify-end mt-6">
              <button
                onClick={handleCancelUpload}
                className="border border-[#c9cfd9] dark:border-[#3a3f54] dark:text-[#c4c8db] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                disabled={isUploading}
              >
                Cancel
              </button>
              <button
                onClick={handleUpload}
                className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 flex items-center gap-2"
                disabled={isUploading}
              >
                {isUploading ? (
                  <>
                    <LoadingSpinner />
                    Uploading...
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4" />
                    Upload
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
