import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  Table2,
} from 'lucide-react';
import { ConfirmDialog } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import {
  useTakeouts,
  useRequestTakeout,
  useDeleteTakeout,
} from '@/hooks/useTakeoutHooks';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import { useGoogleSheetsExport } from '@/hooks/useGoogleSheetsExport';
import { exportTakeoutGoogleSheet } from '@/generated/api/data-takeout/data-takeout';
import { GOOGLE_SHEET_TAKEOUT_TIMEOUT_MS } from '@/utils/googleSheetExport';
import { GoogleSheetExportPill } from '@/components/common/GoogleSheetExportPill';
import { useFormatDate } from '@/hooks/useFormatDate';
import type { TakeoutResponse } from '@/generated/models';

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
  const { t } = useTranslation('settings');
  switch (status) {
    case 'PENDING':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-warning-bg text-warning-text">
          <Clock className="h-3 w-3" />
          {t('dataExport.statuses.pending')}
        </span>
      );
    case 'PROCESSING':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-info-bg text-info-text">
          <Loader2 className="h-3 w-3 animate-spin" />
          {t('dataExport.statuses.processing')}
        </span>
      );
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-success-bg text-success-text">
          <CheckCircle className="h-3 w-3" />
          {t('dataExport.statuses.completed')}
        </span>
      );
    case 'FAILED':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-error-bg text-error-text">
          <XCircle className="h-3 w-3" />
          {t('dataExport.statuses.failed')}
        </span>
      );
  }
};

const ProgressBar = ({ progress }: { progress: number }) => (
  <div className="w-full bg-border-default rounded-full h-2 mt-2">
    <div
      className="bg-primary-500 dark:bg-primary-300 h-2 rounded-full transition-all duration-500"
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
  const { t } = useTranslation('settings');
  const { formatDateTime } = useFormatDate();
  const isActive =
    takeout.status === 'PENDING' || takeout.status === 'PROCESSING';

  return (
    <div className="bg-surface-card rounded-lg border border-border-default p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3 min-w-0 flex-1">
          <div className="flex-shrink-0 h-10 w-10 bg-surface-inset rounded-full flex items-center justify-center">
            <FileArchive className="h-5 w-5 text-primary-500" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-semibold text-text-primary text-sm">
                {t('dataExport.dataExportLabel')}
              </span>
              <StatusBadge status={takeout.status} />
            </div>
            <div className="flex items-center gap-3 mt-1 text-xs text-text-secondary flex-wrap">
              <span>
                {t('dataExport.requested', {
                  date: formatDateTime(takeout.createdAt),
                })}
              </span>
              {takeout.completedAt && (
                <span>
                  {t('dataExport.completed', {
                    date: formatDateTime(takeout.completedAt),
                  })}
                </span>
              )}
              {takeout.fileSize != null && (
                <span>{formatFileSize(takeout.fileSize)}</span>
              )}
              {takeout.expiresAt && takeout.status === 'COMPLETED' && (
                <span className="text-warning-text">
                  {t('dataExport.expires', {
                    date: formatDateTime(takeout.expiresAt),
                  })}
                </span>
              )}
            </div>
            {isActive && <ProgressBar progress={takeout.progress} />}
            {isActive && (
              <p className="text-xs text-text-secondary mt-1">
                {t('dataExport.percentComplete', { percent: takeout.progress })}
              </p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          {takeout.status === 'COMPLETED' && takeout.downloadUrl && (
            <a
              href={takeout.downloadUrl}
              download
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-lg transition-colors"
            >
              <Download className="h-4 w-4" />
              {t('common:buttons.download')}
            </a>
          )}
          <button
            onClick={() => onDelete(takeout.identifier)}
            disabled={isDeleting}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-error-text bg-error-bg hover:opacity-90 rounded-lg transition-colors disabled:opacity-50"
            title={
              isActive
                ? t('dataExport.deleteTitle')
                : t('dataExport.deleteTitle')
            }
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
  const { t } = useTranslation('settings');
  const { canEditTeamSettings } = useTeam();
  const { data: takeoutsPage, isLoading, isError, refetch } = useTakeouts();
  const requestMutation = useRequestTakeout();
  const deleteMutation = useDeleteTakeout();
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const { isEnabled } = useFeatureFlags();
  const {
    triggerExport: triggerGoogleSheet,
    isExporting: isGoogleExporting,
    lastResult: lastGoogleSheet,
    clearLastResult: clearLastGoogleSheet,
  } = useGoogleSheetsExport();

  const takeouts = takeoutsPage?.content ?? [];
  const hasActiveTakeout = takeouts.some(
    (t) => t.status === 'PENDING' || t.status === 'PROCESSING'
  );

  if (!canEditTeamSettings) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm p-6">
        <div className="flex items-center gap-3 text-text-secondary">
          <ShieldAlert className="h-5 w-5" />
          <p className="text-sm">{t('dataExport.adminOnly')}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Card */}
      <div className="bg-surface-card rounded-lg shadow-sm">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('dataExport.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('dataExport.subtitle')}
              </p>
            </div>
            <div className="flex-shrink-0 flex items-center gap-2 flex-wrap">
              <GoogleSheetExportPill
                result={lastGoogleSheet}
                onDismiss={clearLastGoogleSheet}
              />
              {isEnabled(FeatureFlags.GOOGLE_SHEETS_EXPORT) && (
                <button
                  onClick={() =>
                    triggerGoogleSheet((token) =>
                      exportTakeoutGoogleSheet(
                        { accessToken: token },
                        { timeout: GOOGLE_SHEET_TAKEOUT_TIMEOUT_MS }
                      )
                    )
                  }
                  disabled={isGoogleExporting}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-lg hover:bg-surface-inset transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isGoogleExporting ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Table2 className="h-4 w-4" />
                  )}
                  Export to Google Sheets
                </button>
              )}
              <button
                onClick={() => requestMutation.mutate()}
                disabled={requestMutation.isPending || hasActiveTakeout}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {requestMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Download className="h-4 w-4" />
                )}
                {t('dataExport.requestExport')}
              </button>
            </div>
          </div>
          {hasActiveTakeout && (
            <p className="text-xs text-warning-text mt-2">
              {t('dataExport.inProgress')}
            </p>
          )}
        </div>

        {/* Takeout List */}
        <div className="p-6">
          {isLoading && (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-primary-500 dark:text-primary-300" />
            </div>
          )}

          {isError && (
            <div className="flex flex-col items-center justify-center py-8 gap-3">
              <AlertTriangle className="h-6 w-6 text-warning-text" />
              <p className="text-sm text-text-secondary">
                {t('dataExport.failedToLoad')}
              </p>
              <button
                onClick={() => refetch()}
                className="px-3 py-1.5 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
              >
                {t('dataExport.tryAgain')}
              </button>
            </div>
          )}

          {!isLoading && !isError && takeouts.length === 0 && (
            <div className="text-center py-8">
              <FileArchive className="h-10 w-10 text-text-muted mx-auto mb-3" />
              <p className="text-sm text-text-secondary">
                {t('dataExport.empty')}
              </p>
            </div>
          )}

          {!isLoading && !isError && takeouts.length > 0 && (
            <div className="space-y-3">
              {takeouts.map((takeout) => (
                <TakeoutRow
                  key={takeout.identifier}
                  takeout={takeout}
                  onDelete={setDeleteTarget}
                  isDeleting={
                    deleteTarget === takeout.identifier &&
                    deleteMutation.isPending
                  }
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title={t('dataExport.deleteTitle')}
          message={t('dataExport.deleteMessage')}
          confirmLabel={t('common:buttons.delete')}
          variant="danger"
          isLoading={deleteMutation.isPending}
          onConfirm={async () => {
            await deleteMutation.mutateAsync(deleteTarget);
            setDeleteTarget(null);
          }}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
};
