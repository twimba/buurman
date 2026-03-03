import { useState } from 'react';
import {
  Download,
  Loader2,
  Trash2,
  AlertTriangle,
  Clock,
  CheckCircle,
  XCircle,
  FileArchive,
  ShieldAlert,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import {
  useTakeouts,
  useRequestTakeout,
  useDeleteTakeout,
} from '@/hooks/useTakeoutHooks';
import { useFormatDate } from '@/hooks/useFormatDate';
import type { TakeoutResponse } from '@/api/takeouts';

const formatFileSize = (bytes: number | null): string => {
  if (bytes === null || bytes === 0) {
    return '--';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
};

const StatusBadge = ({ status }: { status: TakeoutResponse['status'] }) => {
  switch (status) {
    case 'PENDING':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300">
          <Clock className="h-3 w-3" />
          Pending
        </span>
      );
    case 'PROCESSING':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
          <Loader2 className="h-3 w-3 animate-spin" />
          Processing
        </span>
      );
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300">
          <CheckCircle className="h-3 w-3" />
          Completed
        </span>
      );
    case 'FAILED':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300">
          <XCircle className="h-3 w-3" />
          Failed
        </span>
      );
  }
};

const ProgressBar = ({ progress }: { progress: number }) => (
  <div className="w-full bg-[#e2e6f0] dark:bg-[#2a2e3f] rounded-full h-2 mt-2">
    <div
      className="bg-[#5c7cfa] dark:bg-[#91a7ff] h-2 rounded-full transition-all duration-500"
      style={{ width: `${Math.min(progress, 100)}%` }}
    />
  </div>
);

const TakeoutRow = ({
  takeout,
  onDelete,
  isDeleting,
}: {
  takeout: TakeoutResponse;
  onDelete: (identifier: string) => void;
  isDeleting: boolean;
}) => {
  const { formatDateTime } = useFormatDate();
  const isActive =
    takeout.status === 'PENDING' || takeout.status === 'PROCESSING';

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3 min-w-0 flex-1">
          <div className="flex-shrink-0 h-10 w-10 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-full flex items-center justify-center">
            <FileArchive className="h-5 w-5 text-[#5c7cfa]" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6] text-sm">
                Data Export
              </span>
              <StatusBadge status={takeout.status} />
            </div>
            <div className="flex items-center gap-3 mt-1 text-xs text-[#6b7194] dark:text-[#8b90a8] flex-wrap">
              <span>Requested {formatDateTime(takeout.createdAt)}</span>
              {takeout.completedAt && (
                <span>Completed {formatDateTime(takeout.completedAt)}</span>
              )}
              {takeout.fileSize !== null && (
                <span>{formatFileSize(takeout.fileSize)}</span>
              )}
              {takeout.expiresAt && takeout.status === 'COMPLETED' && (
                <span className="text-amber-600 dark:text-amber-400">
                  Expires {formatDateTime(takeout.expiresAt)}
                </span>
              )}
            </div>
            {isActive && <ProgressBar progress={takeout.progress} />}
            {isActive && (
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                {takeout.progress}% complete
              </p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          {takeout.status === 'COMPLETED' && takeout.downloadUrl && (
            <a
              href={takeout.downloadUrl}
              download
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-white bg-[#5c7cfa] hover:bg-[#4c6ef5] rounded-lg transition-colors"
            >
              <Download className="h-4 w-4" />
              Download
            </a>
          )}
          <button
            onClick={() => onDelete(takeout.identifier)}
            disabled={isDeleting}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/30 rounded-lg transition-colors disabled:opacity-50"
            title={isActive ? 'Cancel export' : 'Delete export'}
          >
            {isDeleting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Trash2 className="h-4 w-4" />
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export const DataExportSection = () => {
  const { canEditTeamSettings } = useTeam();
  const { data: takeoutsPage, isLoading, isError, refetch } = useTakeouts();
  const requestMutation = useRequestTakeout();
  const deleteMutation = useDeleteTakeout();
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const takeouts = takeoutsPage?.content ?? [];
  const hasActiveTakeout = takeouts.some(
    (t) => t.status === 'PENDING' || t.status === 'PROCESSING'
  );

  const handleDelete = (identifier: string) => {
    setDeletingId(identifier);
    deleteMutation.mutate(identifier, {
      onSettled: () => setDeletingId(null),
    });
  };

  if (!canEditTeamSettings) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm p-6">
        <div className="flex items-center gap-3 text-[#6b7194] dark:text-[#8b90a8]">
          <ShieldAlert className="h-5 w-5" />
          <p className="text-sm">Only team admins can export data.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Card */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Data Export
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Export all your team data as a downloadable archive. Exports
                include properties, tenants, contracts, payments, expenses, and
                documents.
              </p>
            </div>
            <button
              onClick={() => requestMutation.mutate()}
              disabled={requestMutation.isPending || hasActiveTakeout}
              className="flex-shrink-0 inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] hover:bg-[#4c6ef5] rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {requestMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Download className="h-4 w-4" />
              )}
              Request Export
            </button>
          </div>
          {hasActiveTakeout && (
            <p className="text-xs text-amber-600 dark:text-amber-400 mt-2">
              An export is already in progress. Please wait for it to complete
              before requesting another.
            </p>
          )}
        </div>

        {/* Takeout List */}
        <div className="p-6">
          {isLoading && (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-[#5c7cfa] dark:text-[#91a7ff]" />
            </div>
          )}

          {isError && (
            <div className="flex flex-col items-center justify-center py-8 gap-3">
              <AlertTriangle className="h-6 w-6 text-amber-500" />
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Failed to load exports
              </p>
              <button
                onClick={() => refetch()}
                className="px-3 py-1.5 text-sm bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors"
              >
                Try Again
              </button>
            </div>
          )}

          {!isLoading && !isError && takeouts.length === 0 && (
            <div className="text-center py-8">
              <FileArchive className="h-10 w-10 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-3" />
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                No exports yet. Request your first data export above.
              </p>
            </div>
          )}

          {!isLoading && !isError && takeouts.length > 0 && (
            <div className="space-y-3">
              {takeouts.map((takeout) => (
                <TakeoutRow
                  key={takeout.identifier}
                  takeout={takeout}
                  onDelete={handleDelete}
                  isDeleting={deletingId === takeout.identifier}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
