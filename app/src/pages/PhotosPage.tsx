import { useState } from 'react';
import { Search, Image as ImageIcon, Filter } from 'lucide-react';
import {
  usePhotos,
  useDeletePhoto,
  useBulkDownloadPhotos,
  useUpdatePhoto,
} from '@/hooks/usePhotoHooks';
import { usePagination } from '@/hooks/usePagination';
import { Pagination } from '@/components/ui/Pagination';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { EditMetadataModal } from '@/components/ui/EditMetadataModal';
import { PhotoResponse } from '@/types/property';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { usePhotoSelection } from '@/hooks/usePhotoSelection';
import { useTeam } from '@/context/TeamContext';
import { RefreshButton } from '@/components/ui/RefreshButton';

export const PhotosPage = () => {
  const { canEditData } = useTeam();
  const [searchTerm, setSearchTerm] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('');
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);
  const [pendingBulkDelete, setPendingBulkDelete] = useState<string[] | null>(
    null
  );
  const [editingPhoto, setEditingPhoto] = useState<PhotoResponse | null>(null);

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    resetPage,
  } = usePagination({ defaultSort: 'uploadedAt', defaultSize: 24 });

  const {
    data: photosData,
    isLoading,
    isFetching,
    refetch,
  } = usePhotos({
    search: searchTerm || undefined,
    entityType: entityTypeFilter || undefined,
    ...pageParams,
  });

  const photos = photosData?.content || [];

  const { selectedPhotos, handleSelectPhoto, handleSelectAll, clearSelection } =
    usePhotoSelection(photos);

  const deleteMutation = useDeletePhoto();
  const bulkDownloadMutation = useBulkDownloadPhotos();
  const updatePhotoMutation = useUpdatePhoto();

  const handleDelete = (id: string) => {
    setPendingBulkDelete([id]);
  };

  const handleBulkDownload = (ids: string[]) => {
    bulkDownloadMutation.mutate(ids);
  };

  const handleBulkDelete = (ids: string[]) => {
    setPendingBulkDelete(ids);
  };

  const confirmDelete = () => {
    if (pendingBulkDelete) {
      pendingBulkDelete.forEach((id) => deleteMutation.mutate(id));
      clearSelection();
      setPendingBulkDelete(null);
    }
  };

  return (
    <div className="px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <ImageIcon className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Photo Library
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Browse and manage all your photos in one place
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Search and Filter Bar */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm p-4 mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              placeholder="Search photos by title, filename, or notes..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                resetPage();
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
            />
          </div>

          <div className="w-full md:w-48 relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <select
              value={entityTypeFilter}
              onChange={(e) => {
                setEntityTypeFilter(e.target.value);
                resetPage();
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
            >
              <option value="">All Types</option>
              <option value="PROPERTY">Properties</option>
              <option value="TENANT">Tenants</option>
              <option value="CONTRACT">Contracts</option>
              <option value="PAYMENT">Payments</option>
              <option value="EXPENSE">Expenses</option>
            </select>
          </div>
        </div>
      </div>

      {/* Photo Grid */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
        <PhotoGrid
          photos={photos}
          isLoading={isLoading}
          emptyMessage="No photos found"
          totalCount={photosData?.totalElements}
          selectedPhotos={selectedPhotos}
          onSelectPhoto={handleSelectPhoto}
          onSelectAll={handleSelectAll}
          onDelete={canEditData ? handleDelete : undefined}
          onEdit={canEditData ? setEditingPhoto : undefined}
          onBulkDownload={handleBulkDownload}
          onBulkDelete={canEditData ? handleBulkDelete : undefined}
          isBulkDownloading={bulkDownloadMutation.isPending}
          onPreview={(photo) => setPreviewIndex(photos.indexOf(photo))}
          showEntityLink
          showMainBadge={false}
          readOnly={!canEditData}
        />
      </div>

      {photosData && photosData.totalPages > 1 && (
        <div className="mt-4">
          <Pagination
            page={page}
            totalPages={photosData.totalPages}
            totalElements={photosData.totalElements}
            size={size}
            onPageChange={handlePageChange}
            onSizeChange={handleSizeChange}
          />
        </div>
      )}

      {/* Preview Modal */}
      {previewIndex !== null && photos[previewIndex] && (
        <DocumentPreviewModal
          document={photos[previewIndex]}
          onClose={() => setPreviewIndex(null)}
          onEdit={() => {
            const photo = photos[previewIndex];
            setPreviewIndex(null);
            setEditingPhoto(photo);
          }}
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
    </div>
  );
};
