import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  FileSpreadsheet,
  Undo2,
  Eye,
  Download,
  ChevronUp,
  AlertTriangle,
} from 'lucide-react';
import { ConfirmDialog, EmptyState, LoadingSpinner, Pagination } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  useImports,
  useImportDetail,
  useRevertImport,
  useDownloadErrorReport,
} from '@/hooks/useImportHooks';
import { useTeam } from '@/context/TeamContext';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import type { DataImportResponse } from '@/generated/models';

const STATUS_STYLES: Record<string, string> = {
  PROCESSING: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
  COMPLETED:
    'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
  PARTIALLY_COMPLETED:
    'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300',
  FAILED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300',
  REVERTED:
    'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-300',
};

const STATUS_LABELS: Record<string, string> = {
  PROCESSING: 'Processing',
  COMPLETED: 'Completed',
  PARTIALLY_COMPLETED: 'Partial',
  FAILED: 'Failed',
  REVERTED: 'Reverted',
};

const FORMAT_STYLES: Record<string, string> = {
  CSV: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
  XLSX: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export const ImportHistory = () => {
  const { canEditData } = useTeam();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [revertTarget, setRevertTarget] = useState<DataImportResponse | null>(
    null
  );

  const { data: importsData, isLoading, error } = useImports(page, size);
  const { data: detail, isLoading: detailLoading } =
    useImportDetail(expandedId);
  const revertMutation = useRevertImport();
  const downloadMutation = useDownloadErrorReport();

  const handleRevert = async () => {
    if (!revertTarget) {
      return;
    }
    await revertMutation.mutateAsync(revertTarget.identifier);
    trackEvent(AnalyticsEvent.CONTACT_IMPORT_REVERTED);
    setRevertTarget(null);
    setExpandedId(null);
  };

  const handleDownloadErrors = async (identifier: string, fileName: string) => {
    const blob = await downloadMutation.mutateAsync(identifier);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `errors-${fileName}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const toggleExpand = (identifier: string) => {
    setExpandedId((prev) => (prev === identifier ? null : identifier));
  };

  const canRevert = (imp: DataImportResponse): boolean => {
    return (
      canEditData &&
      (imp.status as string) !== 'REVERTED' &&
      (imp.status as string) !== 'FAILED' &&
      !imp.revertedAt
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorMessage message="Failed to load import history" />;
  }

  if (!importsData?.content || importsData.content.length === 0) {
    return (
      <div className="bg-surface-card rounded-lg">
        <EmptyState
          icon={<FileSpreadsheet className="h-12 w-12" />}
          title="No imports yet"
          description="Import contacts from CSV or XLSX files. Your import history will appear here."
          variant="page"
        />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="bg-surface-card border border-border-default rounded-lg overflow-hidden">
        {/* Table header */}
        <div className="hidden md:grid md:grid-cols-[1fr_80px_100px_120px_140px_100px] gap-4 px-4 py-3 bg-surface-inset border-b border-border-default text-xs font-medium text-text-muted uppercase tracking-wider">
          <span>File</span>
          <span>Format</span>
          <span>Status</span>
          <span>Rows</span>
          <span>Date</span>
          <span>Actions</span>
        </div>

        {/* Rows */}
        <div className="divide-y divide-border-default">
          {importsData.content.map((imp) => (
            <div key={imp.identifier}>
              {/* Main row */}
              <div className="grid grid-cols-1 md:grid-cols-[1fr_80px_100px_120px_140px_100px] gap-2 md:gap-4 px-4 py-3 items-center hover:bg-surface-inset transition-colors">
                <div className="flex items-center gap-2">
                  <FileSpreadsheet className="h-4 w-4 text-text-muted flex-shrink-0" />
                  <span className="text-sm text-text-primary font-medium truncate">
                    {imp.fileName}
                  </span>
                </div>
                <div>
                  <span
                    className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                      FORMAT_STYLES[imp.fileFormat] ?? FORMAT_STYLES.CSV
                    }`}
                  >
                    {imp.fileFormat}
                  </span>
                </div>
                <div>
                  <span
                    className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                      STATUS_STYLES[imp.status as string] ??
                      STATUS_STYLES.COMPLETED
                    }`}
                  >
                    {STATUS_LABELS[imp.status as string] ?? imp.status}
                  </span>
                </div>
                <div className="text-sm text-text-secondary">
                  <span className="text-green-600 dark:text-green-400">
                    {imp.importedRows}
                  </span>
                  <span className="text-text-muted">/{imp.totalRows}</span>
                  {imp.errorRows > 0 && (
                    <span className="text-red-500 ml-1">
                      ({imp.errorRows} err)
                    </span>
                  )}
                </div>
                <div className="text-xs text-text-muted">
                  {formatDate(imp.createdAt)}
                  {imp.createdByName && (
                    <div className="truncate">{imp.createdByName}</div>
                  )}
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => toggleExpand(imp.identifier)}
                    className="p-1.5 rounded hover:bg-surface-inset text-text-muted hover:text-text-primary transition-colors"
                    title="View details"
                  >
                    {expandedId === imp.identifier ? (
                      <ChevronUp className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                  {imp.errorRows > 0 &&
                    (imp.status as string) !== 'REVERTED' && (
                      <button
                        type="button"
                        onClick={() =>
                          handleDownloadErrors(imp.identifier, imp.fileName)
                        }
                        disabled={downloadMutation.isPending}
                        className="p-1.5 rounded hover:bg-surface-inset text-text-muted hover:text-text-primary transition-colors disabled:opacity-50"
                        title="Download error report"
                      >
                        <Download className="h-4 w-4" />
                      </button>
                    )}
                  {canRevert(imp) && (
                    <button
                      type="button"
                      onClick={() => setRevertTarget(imp)}
                      className="p-1.5 rounded hover:bg-red-50 dark:hover:bg-red-950 text-text-muted hover:text-red-600 dark:hover:text-red-400 transition-colors"
                      title="Revert import"
                    >
                      <Undo2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>

              {/* Expanded detail */}
              {expandedId === imp.identifier && (
                <div className="px-4 pb-4 bg-surface-inset">
                  {detailLoading ? (
                    <div className="flex items-center justify-center py-4">
                      <div className="h-5 w-5 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
                    </div>
                  ) : detail ? (
                    <div className="space-y-3">
                      {/* Revert info */}
                      {imp.revertedAt && (
                        <div className="flex items-center gap-2 text-sm text-text-muted bg-surface-card border border-border-default rounded-lg px-3 py-2">
                          <AlertTriangle className="h-4 w-4 text-yellow-500" />
                          Reverted on {formatDate(imp.revertedAt)}
                        </div>
                      )}

                      {/* Imported items */}
                      {detail.items.length > 0 && (
                        <div>
                          <p className="text-xs font-medium text-text-muted mb-2">
                            Imported Contacts ({detail.items.length})
                          </p>
                          <div className="grid gap-1 max-h-48 overflow-y-auto">
                            {detail.items.map((item) => (
                              <Link
                                key={item.entityIdentifier}
                                to={`/contacts/${item.entityIdentifier}`}
                                className="flex items-center justify-between bg-surface-card border border-border-default rounded px-3 py-1.5 text-sm hover:border-primary-500 transition-colors"
                              >
                                <span className="text-text-primary">
                                  {item.displayName}
                                </span>
                                <span className="text-xs text-text-muted">
                                  Row {item.rowNumber}
                                </span>
                              </Link>
                            ))}
                          </div>
                        </div>
                      )}

                      {detail.items.length === 0 && (
                        <p className="text-sm text-text-muted py-2">
                          No imported contacts to display.
                        </p>
                      )}
                    </div>
                  ) : null}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Pagination */}
      {importsData && (
        <Pagination
          page={page}
          totalPages={importsData.totalPages}
          totalElements={importsData.totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={(newSize) => {
            setSize(newSize);
            setPage(0);
          }}
        />
      )}

      {/* Revert confirmation */}
      {revertTarget && (
        <ConfirmDialog
          onCancel={() => setRevertTarget(null)}
          onConfirm={handleRevert}
          title="Revert Import?"
          message={`This will permanently delete all ${revertTarget.importedRows} contacts created by the import "${revertTarget.fileName}". This action cannot be undone.`}
          confirmLabel="Revert Import"
          variant="danger"
          isLoading={revertMutation.isPending}
        />
      )}
    </div>
  );
};
