import { useState } from 'react';
import { DocumentResponse } from '@/types/property';
import { Upload, Star, StarOff, Trash2, X } from 'lucide-react';
import { LoadingSpinner } from '../LoadingSpinner';
import { ErrorMessage } from '../ErrorMessage';

// Simple gray placeholder SVG
const PLACEHOLDER_IMAGE =
  'data:image/svg+xml,%3Csvg width="200" height="200" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg"%3E%3Crect width="200" height="200" fill="%23F3F4F6"/%3E%3Cpath d="M97 90C101.418 90 105 86.4183 105 82C105 77.5817 101.418 74 97 74C92.5817 74 89 77.5817 89 82C89 86.4183 92.5817 90 97 90Z" fill="%239CA3AF"/%3E%3Cpath d="M110 105H84C79.5817 105 76 108.582 76 113V122C76 126.418 79.5817 130 84 130H110C114.418 130 118 126.418 118 122V113C118 108.582 114.418 105 110 105Z" fill="%239CA3AF"/%3E%3C/svg%3E';

interface PhotoGalleryProps {
  propertyId: string;
  photos: DocumentResponse[];
  isLoading: boolean;
  error: any;
  onUpload: (file: File, title?: string, notes?: string) => Promise<void>;
  onSetMain: (photoId: string) => Promise<void>;
  onDelete: (photoId: string) => Promise<void>;
  isUploading: boolean;
  isDeleting: boolean;
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
}: PhotoGalleryProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadNotes, setUploadNotes] = useState('');
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setUploadTitle('');
      setUploadNotes('');

      // Create preview
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

    await onUpload(
      selectedFile,
      uploadTitle || undefined,
      uploadNotes || undefined
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

  return (
    <div className="space-y-4">
      {/* Upload Button */}
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-gray-900">Photos</h3>
        <label className="cursor-pointer bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2">
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
      </div>

      {/* Photo Grid */}
      {photos.length > 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {photos.map((photo) => (
            <div
              key={photo.id}
              className="relative group rounded-lg overflow-hidden border-2 transition-all"
              style={{
                borderColor: photo.isMainPhoto ? '#3b82f6' : '#e5e7eb',
              }}
            >
              {/* Photo */}
              <div className="aspect-square bg-gray-100">
                <img
                  src={photo.downloadUrl || PLACEHOLDER_IMAGE}
                  alt={photo.title || photo.fileName}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    if (target.src !== PLACEHOLDER_IMAGE) {
                      target.src = PLACEHOLDER_IMAGE;
                    }
                  }}
                />
              </div>

              {/* Main Photo Badge */}
              {photo.isMainPhoto && (
                <div className="absolute top-2 left-2 bg-blue-600 text-white px-2 py-1 rounded text-xs font-semibold flex items-center gap-1">
                  <Star className="h-3 w-3 fill-white" />
                  Main
                </div>
              )}

              {/* Actions Overlay */}
              <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-50 transition-all flex items-center justify-center gap-2 opacity-0 group-hover:opacity-100">
                {!photo.isMainPhoto && (
                  <button
                    onClick={() => onSetMain(photo.id)}
                    className="bg-white text-gray-900 p-2 rounded-full hover:bg-gray-100 transition-colors"
                    title="Set as main photo"
                    disabled={isUploading || isDeleting}
                  >
                    <StarOff className="h-4 w-4" />
                  </button>
                )}
                <button
                  onClick={() => onDelete(photo.id)}
                  className="bg-red-600 text-white p-2 rounded-full hover:bg-red-700 transition-colors"
                  title="Delete photo"
                  disabled={isUploading || isDeleting}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>

              {/* Title */}
              {photo.title && (
                <div className="p-2 bg-white text-sm text-gray-700 truncate">
                  {photo.title}
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <Upload className="h-12 w-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">No photos yet</p>
          <p className="text-sm text-gray-400 mt-1">
            Upload photos to showcase this property
          </p>
        </div>
      )}

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-900">
                Upload Photo
              </h3>
              <button
                onClick={handleCancelUpload}
                className="p-2 hover:bg-gray-100 rounded transition-colors"
                disabled={isUploading}
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Preview */}
            {previewUrl && (
              <div className="mb-4">
                <img
                  src={previewUrl}
                  alt="Preview"
                  className="w-full max-h-96 object-contain rounded-lg"
                />
              </div>
            )}

            {/* Form Fields */}
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Title (optional)
                </label>
                <input
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2"
                  placeholder="e.g., Living room"
                  disabled={isUploading}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Notes (optional)
                </label>
                <textarea
                  value={uploadNotes}
                  onChange={(e) => setUploadNotes(e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2"
                  placeholder="Additional notes about this photo"
                  rows={3}
                  disabled={isUploading}
                />
              </div>
            </div>

            {/* Actions */}
            <div className="flex gap-2 justify-end mt-6">
              <button
                onClick={handleCancelUpload}
                className="border border-gray-300 px-4 py-2 rounded hover:bg-gray-50 transition-colors"
                disabled={isUploading}
              >
                Cancel
              </button>
              <button
                onClick={handleUpload}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center gap-2"
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
