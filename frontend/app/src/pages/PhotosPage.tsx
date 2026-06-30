import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Search, Image as ImageIcon, X, Folder } from 'lucide-react';
import {
  usePhotos,
  useDeletePhoto,
  useBulkDownloadPhotos,
  useUpdatePhoto,
} from '@/hooks/usePhotoHooks';
import { usePagination } from '@/hooks/usePagination';
import {
  ConfirmDialog,
  FilterSelectPopover,
  ListPageHeader,
  Pagination,
  RefreshButton,
  SelectionBar,
  type ListPageHeaderAction,
  type SelectionBarAction,
} from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { Download, RefreshCw, Trash2 } from 'lucide-react';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { EditMetadataModal } from '@/components/ui/EditMetadataModal';
import { PhotoResponse } from '@/types/property';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { usePhotoSelection } from '@/hooks/usePhotoSelection';
import { useTeam } from '@/context/TeamContext';

export const PhotosPage = () => {
  const { t } = useTranslation('documents');
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

  const photos = photosData?.content ?? [];

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

  const headerActions: ListPageHeaderAction[] = [
    {
      label: t('common:refresh', 'Refresh'),
      icon: RefreshCw,
      onClick: () => refetch(),
      showOn: 'mobile',
    },
    {
      label: 'desktop-actions',
      showOn: 'desktop',
      render: () => (
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      ),
    },
  ];

  return (
    <div className="px-4 py-4 md:py-8">
      <ListPageHeader
        title={t('photosPage.title')}
        subtitle={t('photosPage.subtitle')}
        icon={ImageIcon}
        mobileLeading={<MobileMenuButton />}
        actions={headerActions}
      />

      {/* Toolbar — search + type filter on one tidy row */}
      <div className="flex flex-wrap items-center gap-2 mb-4">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted" />
          <input
            type="text"
            placeholder={t('photosPage.searchPlaceholder')}
            aria-label={t('photosPage.searchPlaceholder')}
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              resetPage();
            }}
            className="w-full h-10 pl-10 pr-9 border border-border-strong rounded-lg focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
          />
          {searchTerm && (
            <button
              onClick={() => {
                setSearchTerm('');
                resetPage();
              }}
              aria-label={t('common:buttons.clear', 'Clear')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 p-0.5 rounded text-text-muted hover:text-text-secondary focus-ring"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        <FilterSelectPopover
          icon={Folder}
          label={t('photosPage.entityTypeFilter.label', {
            defaultValue: 'Type',
          })}
          options={[
            'PROPERTY',
            'CONTACT',
            'CONTRACT',
            'PAYMENT',
            'EXPENSE',
          ].map((type) => ({
            value: type,
            label: t(`photosPage.entityTypeFilter.${type.toLowerCase()}`),
          }))}
          value={entityTypeFilter || undefined}
          onChange={(next) => {
            setEntityTypeFilter(next ?? '');
            resetPage();
          }}
          allLabel={t('photosPage.entityTypeFilter.all')}
          align="end"
        />
      </div>

      {/* Active filter chip */}
      {entityTypeFilter && (
        <div className="flex flex-wrap items-center gap-2 mb-6">
          <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300">
            {t(`photosPage.entityTypeFilter.${entityTypeFilter.toLowerCase()}`)}
            <button
              onClick={() => {
                setEntityTypeFilter('');
                resetPage();
              }}
              aria-label={t('common:buttons.clear', 'Clear')}
              className="rounded-full hover:text-primary-900 focus-ring"
            >
              <X className="h-3 w-3" />
            </button>
          </span>
        </div>
      )}

      {/* Phone-only sticky selection bar (overlays bottom tab bar) */}
      <SelectionBar
        open={selectedPhotos.size > 0}
        count={selectedPhotos.size}
        label={t('photosPage.selectedCount', {
          count: selectedPhotos.size,
          defaultValue: '{{count}} selected',
        })}
        onCancel={clearSelection}
        actions={
          [
            {
              label: t('buttons.download', {
                ns: 'common',
                defaultValue: 'Download',
              }),
              icon: Download,
              onClick: () => handleBulkDownload(Array.from(selectedPhotos)),
            },
            ...(canEditData
              ? [
                  {
                    label: t('buttons.delete', {
                      ns: 'common',
                      defaultValue: 'Delete',
                    }),
                    icon: Trash2,
                    tone: 'danger' as const,
                    onClick: () => handleBulkDelete(Array.from(selectedPhotos)),
                  },
                ]
              : []),
          ] as SelectionBarAction[]
        }
      />

      {/* Photo Grid */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <PhotoGrid
          photos={photos}
          isLoading={isLoading}
          emptyMessage={t('photosPage.empty')}
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

      {photosData && (photosData.totalPages ?? 0) > 1 && (
        <div className="mt-4">
          <Pagination
            page={page}
            totalPages={photosData.totalPages ?? 0}
            totalElements={photosData.totalElements ?? 0}
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
          title={t('photosPage.editModal')}
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
