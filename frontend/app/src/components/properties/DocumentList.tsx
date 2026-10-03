import { Fragment, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { DocumentResponse } from '@/types/property';
import {
  Upload,
  X,
  FileText,
  Image as ImageIcon,
  Download,
  Trash2,
  Eye,
  Pencil,
  CheckSquare,
  Square,
  Check,
  ChevronRight,
  Loader2,
} from 'lucide-react';
import { ErrorMessage } from '../ErrorMessage';
import { DocumentPreviewModal } from '../documents/DocumentPreviewModal';
import {
  ConfirmDialog,
  LoadingSpinner,
  RichTextDisplay,
  RichTextEditor,
  SwipeAction,
  type SwipeActionItem,
} from '@buurman/ui';
import { EditMetadataModal } from '../ui/EditMetadataModal';
import { useDocumentSelection } from '@/hooks/useDocumentSelection';
import { useBulkDownload, useUpdateDocument } from '@/hooks/useDocumentHooks';
import { getDocumentDownloadUrl } from '@/generated/api/documents/documents';
import { useFormatDate } from '@/hooks/useFormatDate';

interface DocumentListProps {
  documents: DocumentResponse[];
  isLoading: boolean;
  error: Error | null;
  onUpload: (file: File, title?: string, notes?: string) => Promise<void>;
  onDelete: (documentId: string) => Promise<void>;
  isUploading: boolean;
  isDeleting: boolean;
  readOnly?: boolean;
  renderRowAction?: (doc: DocumentResponse) => React.ReactNode;
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const getFileIcon = (mimeType: string) => {
  if (mimeType.startsWith('image/')) {
    return (
      <ImageIcon className="h-8 w-8 text-primary-500 dark:text-primary-300" />
    );
  }
  return <FileText className="h-8 w-8 text-text-secondary " />;
};

export const DocumentList = ({
  documents,
  isLoading,
  error,
  onUpload,
  onDelete,
  isUploading,
  isDeleting,
  readOnly = false,
  renderRowAction,
}: DocumentListProps) => {
  const { t } = useTranslation('properties');
  const { formatDate } = useFormatDate();
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadNotes, setUploadNotes] = useState('');
  const [uploadProgress, setUploadProgress] = useState<{
    current: number;
    total: number;
  } | null>(null);
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);
  const [pendingBulkDelete, setPendingBulkDelete] = useState<string[] | null>(
    null
  );
  const [editingDocument, setEditingDocument] =
    useState<DocumentResponse | null>(null);
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(
    new Set()
  );

  // Documents generated from another one (a signed copy, a signing certificate) are grouped
  // under that original instead of shown as unrelated flat rows — collapsed by default so a
  // document suddenly having 2-3 extra rows after it's signed doesn't read as "where did these
  // come from?". A sourceDocumentIdentifier that doesn't match anything in this list (e.g. the
  // original was deleted) falls back to top-level so the document never silently disappears.
  const { topLevelDocuments, childrenByParent } = useMemo(() => {
    const topLevelIds = new Set(documents.map((d) => d.identifier));
    const children: Record<string, DocumentResponse[]> = {};
    const topLevel: DocumentResponse[] = [];
    for (const doc of documents) {
      const parentId = doc.sourceDocumentIdentifier;
      if (parentId && topLevelIds.has(parentId)) {
        (children[parentId] ??= []).push(doc);
      } else {
        topLevel.push(doc);
      }
    }
    return { topLevelDocuments: topLevel, childrenByParent: children };
  }, [documents]);

  const toggleGroup = (id: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const {
    selectedDocuments,
    handleSelectDocument,
    handleSelectAll,
    clearSelection,
  } = useDocumentSelection(topLevelDocuments);
  const bulkDownloadMutation = useBulkDownload();
  const updateDocumentMutation = useUpdateDocument();

  const handleBulkDownload = () => {
    if (selectedDocuments.size === 0) {
      return;
    }
    bulkDownloadMutation.mutate(Array.from(selectedDocuments));
  };

  const handleDeleteSingle = (id: string) => {
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
      pendingBulkDelete.forEach((id) => onDelete(id));
      clearSelection();
      setPendingBulkDelete(null);
    }
  };

  const handleDownload = async (documentId: string) => {
    try {
      const url = await getDocumentDownloadUrl(documentId);
      window.open(url, '_blank');
    } catch (err) {
      console.error('Download error:', err);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      setSelectedFiles(Array.from(files));
      setUploadTitle('');
      setUploadNotes('');
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
    setShowUploadModal(false);
    setSelectedFiles([]);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleCancelUpload = () => {
    setShowUploadModal(false);
    setSelectedFiles([]);
    setUploadTitle('');
    setUploadNotes('');
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const hasSelection = selectedDocuments.size > 0;

  const renderTableRow = (doc: DocumentResponse, isChild = false) => {
    const isSelected = selectedDocuments.has(doc.identifier);
    return (
      <tr
        key={doc.identifier}
        className={`hover:bg-surface-inset cursor-pointer ${
          isSelected ? 'bg-info-bg' : ''
        }`}
        onClick={() => setPreviewIndex(documents.indexOf(doc))}
      >
        <td className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={(e) => handleSelectDocument(doc.identifier, e.shiftKey)}
            className="text-text-secondary hover:text-text-secondary"
          >
            {isSelected ? (
              <div className="w-5 h-5 rounded bg-primary-500 flex items-center justify-center">
                <Check className="h-3.5 w-3.5 text-white" strokeWidth={3} />
              </div>
            ) : (
              <Square className="h-5 w-5" />
            )}
          </button>
        </td>
        <td className={`px-6 py-4 ${isChild ? 'pl-12' : ''}`}>
          <div className="flex items-center gap-3">
            {getFileIcon(doc.mimeType ?? '')}
            <div>
              <div
                className={`text-sm font-medium ${isChild ? 'text-text-secondary' : 'text-text-primary'}`}
              >
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
        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
          {formatFileSize(doc.fileSize ?? 0)}
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
              onClick={() => setPreviewIndex(documents.indexOf(doc))}
              className="p-1.5 text-text-secondary hover:bg-surface-inset rounded-md transition-colors"
              title={t('documents.preview')}
            >
              <Eye className="h-4 w-4" />
            </button>
            {!readOnly && (
              <button
                onClick={() => setEditingDocument(doc)}
                className="p-1.5 text-text-secondary hover:bg-surface-inset rounded-md transition-colors"
                title={t('documents.editTitleNotes')}
              >
                <Pencil className="h-4 w-4" />
              </button>
            )}
            <button
              onClick={() => handleDownload(doc.identifier)}
              className="p-1.5 text-primary-500 hover:bg-primary-50 rounded-md transition-colors"
              title={t('buttons.download', { ns: 'common' })}
            >
              <Download className="h-4 w-4" />
            </button>
            {!readOnly && (
              <button
                onClick={() => handleDeleteSingle(doc.identifier)}
                disabled={isDeleting}
                className="p-1.5 text-error-text hover:bg-error-bg rounded-md transition-colors disabled:opacity-50"
                title={t('buttons.delete', { ns: 'common' })}
              >
                <Trash2 className="h-4 w-4" />
              </button>
            )}
            {renderRowAction?.(doc)}
          </div>
        </td>
      </tr>
    );
  };

  const renderGroupToggleRow = (doc: DocumentResponse, count: number) => {
    const isExpanded = expandedGroups.has(doc.identifier);
    return (
      <tr
        key={`toggle-${doc.identifier}`}
        className="hover:bg-surface-inset"
      >
        <td className="px-6 py-1.5" />
        <td colSpan={4} className="px-6 py-1.5">
          <button
            type="button"
            onClick={() => toggleGroup(doc.identifier)}
            className="inline-flex items-center gap-1 text-xs font-medium text-text-secondary hover:text-text-primary transition-colors"
          >
            <ChevronRight
              className={`h-3.5 w-3.5 transition-transform ${isExpanded ? 'rotate-90' : ''}`}
            />
            {t('documents.generated', { count })}
          </button>
        </td>
      </tr>
    );
  };

  const renderMobileCard = (doc: DocumentResponse, isChild = false) => {
    const isSelected = selectedDocuments.has(doc.identifier);
    const leftActions: SwipeActionItem[] = [];
    if (!readOnly) {
      leftActions.push({
        label: t('buttons.delete', { ns: 'common' }),
        icon: Trash2,
        tone: 'danger',
        onAction: () => handleDeleteSingle(doc.identifier),
      });
    }
    leftActions.push({
      label: t('buttons.download', { ns: 'common' }),
      icon: Download,
      tone: 'success',
      onAction: () => handleDownload(doc.identifier),
    });
    return (
      <SwipeAction
        leftActions={leftActions}
        onClick={() => setPreviewIndex(documents.indexOf(doc))}
      >
        <div
          className={`border p-3 ${isChild ? 'ml-6' : ''} ${
            isSelected
              ? 'border-primary-500 bg-info-bg'
              : 'border-border-default bg-surface-card'
          }`}
        >
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 mt-0.5">
              {getFileIcon(doc.mimeType ?? '')}
            </div>
            <div className="flex-1 min-w-0">
              <div
                className={`text-sm font-medium truncate ${isChild ? 'text-text-secondary' : 'text-text-primary'}`}
              >
                {doc.title ?? doc.fileName}
              </div>
              <div className="mt-0.5 text-xs text-text-secondary flex items-center gap-2 flex-wrap">
                <span>{formatFileSize(doc.fileSize ?? 0)}</span>
                <span aria-hidden>·</span>
                <span>{formatDate(doc.uploadedAt)}</span>
              </div>
            </div>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                handleDownload(doc.identifier);
              }}
              aria-label={t('buttons.download', { ns: 'common' })}
              className="flex-shrink-0 min-h-touch min-w-touch inline-flex items-center justify-center p-2 rounded text-primary-500 hover:bg-primary-50 focus-ring"
            >
              <Download className="h-5 w-5" />
            </button>
          </div>
        </div>
      </SwipeAction>
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorMessage message={t('documents.failedToLoad')} />;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-text-primary">
          {t('documents.title')}
        </h3>
        {!readOnly && (
          <label className="cursor-pointer bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2">
            <Upload className="h-4 w-4" />
            {t('documents.uploadButton')}
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx"
              multiple
              onChange={handleFileSelect}
              className="hidden"
              disabled={isUploading}
            />
          </label>
        )}
      </div>

      {/* Document Table */}
      {documents.length === 0 ? (
        <div className="text-center py-12 bg-surface-page rounded-lg">
          <FileText className="h-12 w-12 text-text-muted mx-auto mb-3" />
          <p className="text-text-secondary">{t('documents.empty')}</p>
        </div>
      ) : (
        <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
          {/* Selection bar */}
          <div className="px-6 py-3 border-b border-border-default flex items-center justify-between">
            <div className="flex items-center gap-2">
              <button
                onClick={handleSelectAll}
                className="text-text-secondary hover:text-text-secondary"
              >
                {selectedDocuments.size === topLevelDocuments.length &&
                topLevelDocuments.length > 0 ? (
                  <CheckSquare className="h-5 w-5" />
                ) : (
                  <Square className="h-5 w-5" />
                )}
              </button>
              <span className="text-sm text-text-secondary">
                {hasSelection
                  ? t('documents.selection.selected', {
                      selected: selectedDocuments.size,
                      total: topLevelDocuments.length,
                    })
                  : t('documents.selection.count', {
                      count: topLevelDocuments.length,
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
                    ? t('documents.downloading')
                    : t('buttons.download', { ns: 'common' })}
                </button>
                {!readOnly && (
                  <button
                    onClick={handleBulkDelete}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-error-text bg-surface-card border border-error-border rounded-md hover:bg-error-bg transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                    {t('buttons.delete', { ns: 'common' })}
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Mobile card list (<md) */}
          <ul className="md:hidden space-y-3">
            {topLevelDocuments.map((doc) => {
              const children = childrenByParent[doc.identifier] ?? [];
              const isExpanded = expandedGroups.has(doc.identifier);
              return (
                <li key={`m-${doc.identifier}`} className="space-y-2">
                  {renderMobileCard(doc)}
                  {children.length > 0 && (
                    <div className="pl-4">
                      <button
                        type="button"
                        onClick={() => toggleGroup(doc.identifier)}
                        className="inline-flex items-center gap-1 text-xs font-medium text-text-secondary hover:text-text-primary py-1"
                      >
                        <ChevronRight
                          className={`h-3.5 w-3.5 transition-transform ${isExpanded ? 'rotate-90' : ''}`}
                        />
                        {t('documents.generated', { count: children.length })}
                      </button>
                      {isExpanded && (
                        <ul className="space-y-2 mt-1">
                          {children.map((child) => (
                            <li key={`m-${child.identifier}`}>
                              {renderMobileCard(child, true)}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </li>
              );
            })}
          </ul>

          <div className="hidden md:block overflow-x-auto">
            <table className="min-w-full divide-y divide-border-default">
              <thead className="bg-surface-page">
                <tr>
                  <th className="w-12 px-6 py-3" />
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('documents.table.document')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('documents.table.size')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('documents.table.uploaded')}
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('documents.table.actions')}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-surface-card divide-y divide-border-default">
                {topLevelDocuments.map((doc) => {
                  const children = childrenByParent[doc.identifier] ?? [];
                  const isExpanded = expandedGroups.has(doc.identifier);
                  return (
                    <Fragment key={doc.identifier}>
                      {renderTableRow(doc)}
                      {children.length > 0 &&
                        renderGroupToggleRow(doc, children.length)}
                      {children.length > 0 &&
                        isExpanded &&
                        children.map((child) => renderTableRow(child, true))}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Preview Modal */}
      {previewIndex !== null && documents[previewIndex] && (
        <DocumentPreviewModal
          document={documents[previewIndex]}
          onClose={() => setPreviewIndex(null)}
          onEdit={
            readOnly
              ? undefined
              : () => {
                  const doc = documents[previewIndex];
                  setPreviewIndex(null);
                  setEditingDocument(doc);
                }
          }
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
          title={t('documents.editModal')}
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
          title={t('documents.delete.title', {
            count: pendingBulkDelete.length,
          })}
          message={t('documents.delete.message', {
            count: pendingBulkDelete.length,
          })}
          confirmLabel={
            pendingBulkDelete.length === 1
              ? t('buttons.delete', { ns: 'common' })
              : t('documents.delete.confirmLabel', {
                  count: pendingBulkDelete.length,
                })
          }
          variant="danger"
          onConfirm={confirmDelete}
          onCancel={() => setPendingBulkDelete(null)}
        />
      )}

      {/* Upload Modal */}
      {showUploadModal && (
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4"
          onKeyDown={handleCmdEnterUpload}
        >
          <div className="bg-surface-card rounded-lg p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-semibold text-text-primary">
                  {t('documents.upload.title', { count: selectedFiles.length })}
                </h3>
                {selectedFiles.length > 1 && (
                  <span className="px-2 py-0.5 text-xs font-medium bg-primary-500/10 text-primary-500 rounded-full">
                    {t('documents.upload.files', {
                      count: selectedFiles.length,
                    })}
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

            {selectedFiles.length > 0 && (
              <div className="mb-4 space-y-1.5">
                {selectedFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between px-3 py-2 bg-surface-page rounded-lg"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <FileText className="h-4 w-4 text-text-secondary shrink-0" />
                      <span className="text-sm text-text-primary truncate">
                        {file.name}
                      </span>
                      <span className="text-xs text-text-secondary shrink-0">
                        {formatFileSize(file.size)}
                      </span>
                    </div>
                    {!uploadProgress && (
                      <button
                        onClick={() => handleRemoveFile(index)}
                        className="p-1 hover:bg-surface-inset rounded transition-colors shrink-0"
                      >
                        <X className="h-3.5 w-3.5 text-text-secondary " />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('documents.upload.titleLabel')}
                </label>
                <input
                  type="text"
                  value={uploadTitle}
                  onChange={(e) => setUploadTitle(e.target.value)}
                  className="w-full border border-border-strong rounded px-3 py-2"
                  placeholder={t('documents.upload.titlePlaceholder')}
                  disabled={!!uploadProgress}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('documents.upload.notesLabel')}
                </label>
                <RichTextEditor
                  value={uploadNotes}
                  onChange={setUploadNotes}
                  placeholder={t('documents.upload.notesPlaceholder')}
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
                    {t('documents.upload.uploading', {
                      current: uploadProgress.current,
                      total: uploadProgress.total,
                    })}
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
                      ? t('documents.upload.uploadCount', {
                          count: selectedFiles.length,
                        })
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
