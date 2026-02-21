import { useState } from 'react';
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
import { Pagination } from '@/components/ui/Pagination';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EditMetadataModal } from '@/components/ui/EditMetadataModal';
import { DocumentResponse } from '@/types/property';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useDocumentSelection } from '@/hooks/useDocumentSelection';
import { useTeam } from '@/context/TeamContext';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { RichTextDisplay } from '@/components/ui/RichTextDisplay';
import { RefreshButton } from '@/components/ui/RefreshButton';

export const DocumentsPage = () => {
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

  const documents = documentsData?.content || [];

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
    if (selectedDocuments.size === 0) return;
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
    if (selectedDocuments.size === 0) return;
    bulkDownloadMutation.mutate(Array.from(selectedDocuments));
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) {
      return <ImageIcon className="h-8 w-8 text-blue-500" />;
    }
    return <FileText className="h-8 w-8 text-[#6b7194] dark:text-[#8b90a8]" />;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getSortIcon = (field: string) => {
    if (sort !== field) return null;
    return direction === 'asc' ? (
      <ChevronUp className="h-4 w-4" />
    ) : (
      <ChevronDown className="h-4 w-4" />
    );
  };

  const hasSelection = selectedDocuments.size > 0;

  return (
    <div className="px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <Folder className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Document Library
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Search and manage all your documents in one place
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Search and Filter Bar */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm p-4 mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              placeholder="Search documents by title, filename, or notes..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                resetPage();
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6]"
            />
          </div>

          {/* Entity Type Filter */}
          <div className="w-full md:w-48 relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <select
              value={entityTypeFilter}
              onChange={(e) => {
                setEntityTypeFilter(e.target.value);
                resetPage();
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6]"
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

      {/* Document List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <LoadingSpinner />
        </div>
      ) : !documents || documents.length === 0 ? (
        <div className="text-center py-12 bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
          <FileText className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
          <p className="text-[#6b7194] dark:text-[#8b90a8]">
            No documents found
          </p>
        </div>
      ) : (
        <>
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden mb-4">
            {/* Selection bar */}
            <div className="px-6 py-3 border-b border-[#edf0f7] dark:border-[#2a2e3f] flex items-center justify-between">
              <div className="flex items-center gap-2">
                <button
                  onClick={handleSelectAll}
                  className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
                >
                  {selectedDocuments.size === documents.length &&
                  documents.length > 0 ? (
                    <CheckSquare className="h-5 w-5" />
                  ) : (
                    <Square className="h-5 w-5" />
                  )}
                </button>
                <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  {hasSelection
                    ? `${selectedDocuments.size} of ${documentsData?.totalElements ?? documents.length} selected`
                    : `${documentsData?.totalElements ?? documents.length} document${(documentsData?.totalElements ?? documents.length) !== 1 ? 's' : ''}`}
                </span>
              </div>

              {/* Bulk actions */}
              {hasSelection && (
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleBulkDownload}
                    disabled={bulkDownloadMutation.isPending}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#2a2e3f] transition-colors disabled:opacity-50"
                  >
                    {bulkDownloadMutation.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Download className="h-4 w-4" />
                    )}
                    {bulkDownloadMutation.isPending
                      ? 'Downloading...'
                      : 'Download'}
                  </button>
                  {canEditData && (
                    <button
                      onClick={handleBulkDelete}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-600 bg-white dark:bg-[#1e2130] border border-red-200 dark:border-red-900/30 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                    >
                      <Trash2 className="h-4 w-4" />
                      Delete
                    </button>
                  )}
                </div>
              )}
            </div>

            <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
              <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                <tr>
                  <th className="w-12 px-6 py-3" />
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSortChange('title')}
                  >
                    <div className="flex items-center gap-1">
                      Document
                      {getSortIcon('title')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSortChange('entityType')}
                  >
                    <div className="flex items-center gap-1">
                      Type
                      {getSortIcon('entityType')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSortChange('fileSize')}
                  >
                    <div className="flex items-center gap-1">
                      Size
                      {getSortIcon('fileSize')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSortChange('uploadedAt')}
                  >
                    <div className="flex items-center gap-1">
                      Uploaded
                      {getSortIcon('uploadedAt')}
                    </div>
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                {documents.map((doc) => {
                  const isSelected = selectedDocuments.has(doc.identifier);
                  return (
                    <tr
                      key={doc.identifier}
                      className={`hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer ${
                        isSelected ? 'bg-blue-50 dark:bg-blue-900/20' : ''
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
                          className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
                        >
                          {isSelected ? (
                            <div className="w-5 h-5 rounded bg-blue-500 flex items-center justify-center">
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
                            <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                              {doc.title ?? doc.fileName}
                            </div>
                            {doc.title && doc.title !== doc.fileName ? (
                              <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                                {doc.fileName}
                              </div>
                            ) : null}
                            {doc.notes ? (
                              <RichTextDisplay
                                html={doc.notes}
                                className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1"
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
                                : doc.entityType.toLowerCase() === 'tenant'
                                  ? `/tenants/${doc.entityIdentifier}`
                                  : doc.entityType.toLowerCase() === 'contract'
                                    ? `/contracts/${doc.entityIdentifier}`
                                    : doc.entityType.toLowerCase() === 'payment'
                                      ? `/payments/${doc.entityIdentifier}`
                                      : doc.entityType.toLowerCase() ===
                                          'expense'
                                        ? `/expenses/${doc.entityIdentifier}`
                                        : '#';
                            if (entityPath !== '#') navigate(entityPath);
                          }}
                          className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db] hover:bg-blue-100 hover:text-blue-800 transition-colors"
                        >
                          {doc.entityType}
                        </button>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        {formatFileSize(doc.fileSize)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
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
                            className="p-1.5 text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md transition-colors"
                            title="Preview"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                          {canEditData && (
                            <button
                              onClick={() => setEditingDocument(doc)}
                              className="p-1.5 text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded-md transition-colors"
                              title="Edit title & notes"
                            >
                              <Pencil className="h-4 w-4" />
                            </button>
                          )}
                          <a
                            href={doc.downloadUrl || undefined}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="p-1.5 text-[#5c7cfa] hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded-md transition-colors"
                            title="Download"
                          >
                            <Download className="h-4 w-4" />
                          </a>
                          {canEditData && (
                            <button
                              onClick={() => handleDelete(doc.identifier)}
                              className="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-md transition-colors"
                              title="Delete"
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
          title="Edit Document"
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
              ? 'Delete document'
              : 'Delete documents'
          }
          message={
            pendingBulkDelete.length === 1
              ? 'Are you sure you want to delete this document? This action cannot be undone.'
              : `You are about to delete ${pendingBulkDelete.length} documents. This action cannot be undone.`
          }
          confirmLabel={
            pendingBulkDelete.length === 1
              ? 'Delete'
              : `Delete ${pendingBulkDelete.length} documents`
          }
          variant="danger"
          onConfirm={confirmDelete}
          onCancel={() => setPendingBulkDelete(null)}
        />
      )}
    </div>
  );
};
