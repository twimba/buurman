import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Download,
  Trash2,
  FileText,
  Image as ImageIcon,
  Filter,
  CheckSquare,
  Square,
  ChevronUp,
  ChevronDown,
  Folder,
  Check,
  Eye,
  Pencil,
  Loader2,
} from 'lucide-react';
import {
  useDocuments,
  useDeleteDocument,
  useBulkDownload,
  useUpdateDocument,
} from '@/hooks/useDocumentHooks';
import { usePagination } from '@/hooks/usePagination';
import {
  ConfirmDialog,
  ListPageHeader,
  LoadingSpinner,
  Pagination,
  RefreshButton,
  RichTextDisplay,
  type ListPageHeaderAction,
} from '@buurman/ui';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { RefreshCw } from 'lucide-react';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { EditMetadataModal } from '@/components/ui/EditMetadataModal';
import { DocumentResponse } from '@/types/property';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useDocumentSelection } from '@/hooks/useDocumentSelection';
import { useTeam } from '@/context/TeamContext';

export const DocumentsPage = () => {
  const { t } = useTranslation('documents');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [searchTerm, setSearchTerm] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('');
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);
  const [pendingBulkDelete, setPendingBulkDelete] = useState<string[] | null>(
    null
  );
  const [editingDocument, setEditingDocument] =
    useState<DocumentResponse | null>(null);

  const {
    pageParams,
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({ defaultSort: 'uploadedAt' });

  const {
    data: documentsData,
    isLoading,
    isFetching,
    refetch,
  } = useDocuments({
    search: searchTerm || undefined,
    entityType: entityTypeFilter || undefined,
    ...pageParams,
  });

  const documents = documentsData?.content ?? [];

  const {
    selectedDocuments,
    handleSelectDocument,
    handleSelectAll,
    clearSelection,
  } = useDocumentSelection(documents);

  const deleteMutation = useDeleteDocument();
  const bulkDownloadMutation = useBulkDownload();
  const updateDocumentMutation = useUpdateDocument();

  const handleDelete = (id: string) => {
    setPendingBulkDelete([id]);
  };

  const handleBulkDelete = () => {
    if (selectedDocuments.size === 0) {
      return;
    }
    setPendingBulkDelete(Array.from(selectedDocuments));
  };

  const confirmDelete = () => {
    if (pendingBulkDelete) {
      pendingBulkDelete.forEach((id) => deleteMutation.mutate(id));
      clearSelection();
      setPendingBulkDelete(null);
    }
  };

  const handleBulkDownload = () => {
    if (selectedDocuments.size === 0) {
      return;
    }
    bulkDownloadMutation.mutate(Array.from(selectedDocuments));
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) {
      return <ImageIcon className="h-8 w-8 text-info-text" />;
    }
    return <FileText className="h-8 w-8 text-text-secondary " />;
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

  const getSortIcon = (field: string) => {
    if (sort !== field) {
      return null;
    }
    return direction === 'asc' ? (
      <ChevronUp className="h-4 w-4" />
    ) : (
      <ChevronDown className="h-4 w-4" />
    );
  };

  const hasSelection = selectedDocuments.size > 0;

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
        title={t('documentsPage.title')}
        subtitle={t('documentsPage.subtitle')}
        icon={Folder}
        mobileLeading={<MobileMenuButton />}
        actions={headerActions}
      />

      {/* Search and Filter Bar */}
      <div className="bg-surface-card rounded-lg shadow-sm p-4 mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <input
              type="text"
              placeholder={t('documentsPage.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                resetPage();
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-surface-card text-text-primary"
            />
          </div>

          {/* Entity Type Filter */}
          <div className="w-full md:w-48 relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <select
              value={entityTypeFilter}
              onChange={(e) => {
                setEntityTypeFilter(e.target.value);
                resetPage();
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-surface-card text-text-primary"
            >
              <option value="">
                {t('documentsPage.entityTypeFilter.all')}
              </option>
              <option value="PROPERTY">
                {t('documentsPage.entityTypeFilter.property')}
              </option>
              <option value="CONTACT">
                {t('documentsPage.entityTypeFilter.contact')}
              </option>
              <option value="CONTRACT">
                {t('documentsPage.entityTypeFilter.contract')}
              </option>
              <option value="PAYMENT">
                {t('documentsPage.entityTypeFilter.payment')}
              </option>
              <option value="EXPENSE">
                {t('documentsPage.entityTypeFilter.expense')}
              </option>
            </select>
          </div>
        </div>
      </div>

      {/* Document List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <LoadingSpinner />
        </div>
      ) : !documents || documents.length === 0 ? (
        <div className="text-center py-12 bg-surface-card rounded-lg shadow-sm">
          <FileText className="h-12 w-12 text-text-muted mx-auto mb-4" />
          <p className="text-text-secondary">{t('documentsPage.empty')}</p>
        </div>
      ) : (
        <>
          <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden mb-4">
            {/* Selection bar */}
            <div className="px-6 py-3 border-b border-border-default flex items-center justify-between">
              <div className="flex items-center gap-2">
                <button
                  onClick={handleSelectAll}
                  className="text-text-secondary hover:text-text-secondary"
                >
                  {selectedDocuments.size === documents.length &&
                  documents.length > 0 ? (
                    <CheckSquare className="h-5 w-5" />
                  ) : (
                    <Square className="h-5 w-5" />
                  )}
                </button>
                <span className="text-sm text-text-secondary">
                  {hasSelection
                    ? t('documentsPage.selection.selected', {
                        selected: selectedDocuments.size,
                        total: documentsData?.totalElements ?? documents.length,
                      })
                    : t('documentsPage.selection.count', {
                        count: documentsData?.totalElements ?? documents.length,
                      })}
                </span>
              </div>

              {/* Bulk actions */}
              {hasSelection && (
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleBulkDownload}
                    disabled={bulkDownloadMutation.isPending}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50"
                  >
                    {bulkDownloadMutation.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Download className="h-4 w-4" />
                    )}
                    {bulkDownloadMutation.isPending
                      ? t('common:buttons.loading')
                      : t('common:buttons.download')}
                  </button>
                  {canEditData && (
                    <button
                      onClick={handleBulkDelete}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-error-text bg-surface-card border border-error-border rounded-md hover:bg-error-bg transition-colors"
                    >
                      <Trash2 className="h-4 w-4" />
                      {t('common:buttons.delete')}
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-border-default">
                <thead className="bg-surface-page">
                  <tr>
                    <th className="w-12 px-6 py-3" />
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('title')}
                    >
                      <div className="flex items-center gap-1">
                        {t('documentsPage.table.document')}
                        {getSortIcon('title')}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('entityType')}
                    >
                      <div className="flex items-center gap-1">
                        {t('documentsPage.table.type')}
                        {getSortIcon('entityType')}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('fileSize')}
                    >
                      <div className="flex items-center gap-1">
                        {t('documentsPage.table.size')}
                        {getSortIcon('fileSize')}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSortChange('uploadedAt')}
                    >
                      <div className="flex items-center gap-1">
                        {t('documentsPage.table.uploaded')}
                        {getSortIcon('uploadedAt')}
                      </div>
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                      {t('documentsPage.table.actions')}
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-surface-card divide-y divide-border-default">
                  {documents.map((doc) => {
                    const isSelected = selectedDocuments.has(doc.identifier);
                    return (
                      <tr
                        key={doc.identifier}
                        className={`hover:bg-surface-inset cursor-pointer ${
                          isSelected ? 'bg-info-bg' : ''
                        }`}
                        onClick={() => setPreviewIndex(documents.indexOf(doc))}
                      >
                        <td
                          className="px-6 py-4"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <button
                            onClick={(e) =>
                              handleSelectDocument(doc.identifier, e.shiftKey)
                            }
                            className="text-text-secondary hover:text-text-secondary"
                          >
                            {isSelected ? (
                              <div className="w-5 h-5 rounded bg-primary-500 flex items-center justify-center">
                                <Check
                                  className="h-3.5 w-3.5 text-white"
                                  strokeWidth={3}
                                />
                              </div>
                            ) : (
                              <Square className="h-5 w-5" />
                            )}
                          </button>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            {getFileIcon(doc.mimeType)}
                            <div>
                              <div className="text-sm font-medium text-text-primary">
                                {doc.title ?? doc.fileName}
                              </div>
                              {doc.title && doc.title !== doc.fileName ? (
                                <div className="text-xs text-text-secondary">
                                  {doc.fileName}
                                </div>
                              ) : null}
                              {doc.notes ? (
                                <RichTextDisplay
                                  html={doc.notes}
                                  className="text-xs text-text-secondary mt-1"
                                />
                              ) : null}
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              const entityPath =
                                doc.entityType.toLowerCase() === 'property'
                                  ? `/properties/${doc.entityIdentifier}`
                                  : doc.entityType.toLowerCase() === 'contact'
                                    ? `/contacts/${doc.entityIdentifier}`
                                    : doc.entityType.toLowerCase() ===
                                        'contract'
                                      ? `/contracts/${doc.entityIdentifier}`
                                      : doc.entityType.toLowerCase() ===
                                          'payment'
                                        ? `/payments/${doc.entityIdentifier}`
                                        : doc.entityType.toLowerCase() ===
                                            'expense'
                                          ? `/expenses/${doc.entityIdentifier}`
                                          : '#';
                              if (entityPath !== '#') {
                                navigate(entityPath);
                              }
                            }}
                            className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-surface-inset text-text-primary hover:bg-info-bg hover:text-info-text transition-colors"
                          >
                            {doc.entityType}
                          </button>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
                          {formatFileSize(doc.fileSize)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
                          {formatDate(doc.uploadedAt)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <div
                            className="flex justify-end gap-1.5"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <button
                              onClick={() =>
                                setPreviewIndex(documents.indexOf(doc))
                              }
                              className="p-1.5 text-text-secondary hover:bg-surface-inset rounded-md transition-colors"
                              title={t('tooltips.preview')}
                            >
                              <Eye className="h-4 w-4" />
                            </button>
                            {canEditData && (
                              <button
                                onClick={() => setEditingDocument(doc)}
                                className="p-1.5 text-text-secondary hover:bg-surface-inset rounded-md transition-colors"
                                title={t('tooltips.editTitleNotes')}
                              >
                                <Pencil className="h-4 w-4" />
                              </button>
                            )}
                            <a
                              href={doc.downloadUrl ?? undefined}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 text-primary-500 hover:bg-info-bg rounded-md transition-colors"
                              title={t('tooltips.download')}
                            >
                              <Download className="h-4 w-4" />
                            </a>
                            {canEditData && (
                              <button
                                onClick={() => handleDelete(doc.identifier)}
                                className="p-1.5 text-error-text hover:bg-error-bg rounded-md transition-colors"
                                title={t('tooltips.delete')}
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {documentsData && (
            <Pagination
              page={page}
              totalPages={documentsData.totalPages}
              totalElements={documentsData.totalElements}
              size={size}
              onPageChange={handlePageChange}
              onSizeChange={handleSizeChange}
            />
          )}
        </>
      )}

      {/* Preview Modal */}
      {previewIndex !== null && documents[previewIndex] && (
        <DocumentPreviewModal
          document={documents[previewIndex]}
          onClose={() => setPreviewIndex(null)}
          onEdit={() => {
            const doc = documents[previewIndex];
            setPreviewIndex(null);
            setEditingDocument(doc);
          }}
          onPrevious={
            previewIndex > 0
              ? () => setPreviewIndex(previewIndex - 1)
              : undefined
          }
          onNext={
            previewIndex < documents.length - 1
              ? () => setPreviewIndex(previewIndex + 1)
              : undefined
          }
          currentIndex={previewIndex}
          totalCount={documents.length}
        />
      )}

      {/* Edit Modal */}
      {editingDocument && (
        <EditMetadataModal
          title={t('documentsPage.editModal')}
          currentTitle={editingDocument.title}
          currentNotes={editingDocument.notes}
          onSave={(title, notes) => {
            updateDocumentMutation.mutate(
              { id: editingDocument.identifier, data: { title, notes } },
              { onSuccess: () => setEditingDocument(null) }
            );
          }}
          onCancel={() => setEditingDocument(null)}
          isLoading={updateDocumentMutation.isPending}
        />
      )}

      {/* Delete Confirmation */}
      {pendingBulkDelete && (
        <ConfirmDialog
          title={
            pendingBulkDelete.length === 1
              ? t('deleteDialog.titleSingle')
              : t('deleteDialog.titlePlural')
          }
          message={
            pendingBulkDelete.length === 1
              ? t('deleteDialog.messageSingle')
              : t('deleteDialog.messagePlural', {
                  count: pendingBulkDelete.length,
                })
          }
          confirmLabel={
            pendingBulkDelete.length === 1
              ? t('deleteDialog.confirmSingle')
              : t('deleteDialog.confirmPlural', {
                  count: pendingBulkDelete.length,
                })
          }
          variant="danger"
          onConfirm={confirmDelete}
          onCancel={() => setPendingBulkDelete(null)}
        />
      )}
    </div>
  );
};
