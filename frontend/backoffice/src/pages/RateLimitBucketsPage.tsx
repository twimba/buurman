import { useState } from 'react';
import {
  Shield,
  Clock,
  Trash2,
  Search,
  Filter,
  Loader2,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { RefreshButton, ConfirmDialog } from '@buurman/ui';
import {
  useRateLimitSummary,
  useRateLimitBuckets,
  useDeleteRateLimitBucket,
  useDeleteRateLimitBucketsByConfigKey,
} from '../hooks/useRateLimitBuckets';
import type {
  RateLimitConfigSummary,
  RateLimitBucketEntry,
} from '../api/rateLimitBuckets';

// Format future relative time ("in 45s", "in 2m", "in 1h")
const formatExpiresIn = (isoString: string): string => {
  const diff = new Date(isoString).getTime() - Date.now();
  if (diff <= 0) {
    return 'expired';
  }
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) {
    return `in ${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `in ${minutes}m`;
  }
  const hours = Math.floor(minutes / 60);
  return `in ${hours}h`;
};

// Token usage color based on percentage remaining
const tokenColor = (available: number, max: number): string => {
  const pct = available / max;
  if (pct > 0.5) {
    return 'text-success-text';
  }
  if (pct > 0.2) {
    return 'text-warning-text';
  }
  return 'text-error-text';
};

const tokenBarBg = (available: number, max: number): string => {
  const pct = available / max;
  if (pct > 0.5) {
    return 'bg-success-text';
  }
  if (pct > 0.2) {
    return 'bg-warning-text';
  }
  return 'bg-error-text';
};

// Config summary card
const ConfigSummaryCard = ({
  config,
  onResetAll,
  isResetting,
}: {
  config: RateLimitConfigSummary;
  onResetAll: () => void;
  isResetting: boolean;
}) => {
  const [showResetDialog, setShowResetDialog] = useState(false);

  return (
    <div className="bg-surface-card rounded-lg border border-border-default p-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <Shield className="h-4 w-4 text-primary-500" />
          <span className="text-sm font-semibold text-text-primary">
            {config.displayName}
          </span>
          <span
            className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
              config.enabled
                ? 'bg-success-bg text-success-text border-success-border'
                : 'bg-neutral-50 text-text-muted border-border-default'
            }`}
          >
            {config.enabled ? 'Enabled' : 'Disabled'}
          </span>
        </div>
        {config.activeBuckets > 0 && (
          <button
            onClick={() => setShowResetDialog(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-error-text hover:bg-error-bg border border-border-default transition-colors"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Reset All
          </button>
        )}
      </div>
      <div className="flex items-center gap-6 text-xs text-text-muted">
        <span>
          Limit:{' '}
          <span className="font-medium text-text-secondary">
            {config.maxRequests} req / {config.periodSeconds}s
          </span>
        </span>
        <span>
          Active buckets:{' '}
          <span className="font-medium text-text-secondary">
            {config.activeBuckets}
          </span>
        </span>
      </div>

      {showResetDialog && (
        <ConfirmDialog
          title="Reset All Buckets"
          message={`Are you sure you want to reset all ${config.activeBuckets} bucket(s) for "${config.displayName}"? This will remove all active rate limit tracking for this config.`}
          confirmLabel="Reset All"
          variant="danger"
          isLoading={isResetting}
          onConfirm={() => {
            onResetAll();
            setShowResetDialog(false);
          }}
          onCancel={() => setShowResetDialog(false)}
        />
      )}
    </div>
  );
};

// Bucket table row
const BucketRow = ({
  bucket,
  onDelete,
  isDeleting,
}: {
  bucket: RateLimitBucketEntry;
  onDelete: () => void;
  isDeleting: boolean;
}) => {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const hasTokenInfo =
    bucket.availableTokens !== null && bucket.maxTokens !== null;
  const pct = hasTokenInfo
    ? // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by hasTokenInfo
      bucket.availableTokens! / bucket.maxTokens!
    : 0;

  return (
    <tr className="border-b border-border-default last:border-b-0 hover:bg-surface-inset/30 transition-colors">
      <td className="px-4 py-3">
        <code className="text-xs font-mono text-text-primary">
          {bucket.clientIdentifier}
        </code>
      </td>
      <td className="px-4 py-3">
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-primary-50 text-primary-600 border border-primary-200">
          {bucket.configKey}
        </span>
      </td>
      <td className="px-4 py-3">
        {hasTokenInfo ? (
          <div className="flex items-center gap-2">
            <span
              className={`text-xs font-medium ${tokenColor(bucket.availableTokens ?? 0, bucket.maxTokens ?? 0)}`}
            >
              {bucket.availableTokens} / {bucket.maxTokens}
            </span>
            <div className="w-16 h-1.5 bg-surface-inset rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${tokenBarBg(bucket.availableTokens ?? 0, bucket.maxTokens ?? 0)}`}
                style={{ width: `${Math.max(pct * 100, 2)}%` }}
              />
            </div>
          </div>
        ) : (
          <span className="text-xs text-text-muted">N/A</span>
        )}
      </td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-1.5">
          <Clock className="h-3.5 w-3.5 text-text-muted" />
          <span className="text-xs text-text-secondary">
            {formatExpiresIn(bucket.expiresAt)}
          </span>
        </div>
      </td>
      <td className="px-4 py-3 text-right">
        <button
          onClick={() => setShowDeleteDialog(true)}
          className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-error-text hover:bg-error-bg transition-colors"
        >
          <Trash2 className="h-3 w-3" />
          Reset
        </button>

        {showDeleteDialog && (
          <ConfirmDialog
            title="Reset Bucket"
            message={`Are you sure you want to reset the rate limit bucket for "${bucket.clientIdentifier}" (${bucket.configKey})? The bucket will be recreated on the next request.`}
            confirmLabel="Reset"
            variant="danger"
            isLoading={isDeleting}
            onConfirm={() => {
              onDelete();
              setShowDeleteDialog(false);
            }}
            onCancel={() => setShowDeleteDialog(false)}
          />
        )}
      </td>
    </tr>
  );
};

// Main page
export const RateLimitBucketsPage = () => {
  const [configKeyFilter, setConfigKeyFilter] = useState<string>('');
  const [clientIpFilter, setClientIpFilter] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const {
    data: summary,
    isLoading: summaryLoading,
    isFetching: summaryFetching,
    refetch: refetchSummary,
  } = useRateLimitSummary();

  const {
    data: bucketsData,
    isLoading: bucketsLoading,
    isFetching: bucketsFetching,
    refetch: refetchBuckets,
  } = useRateLimitBuckets({
    configKey: configKeyFilter || undefined,
    clientIp: clientIpFilter || undefined,
    page,
    size: pageSize,
  });

  const deleteBucket = useDeleteRateLimitBucket();
  const deleteByConfigKey = useDeleteRateLimitBucketsByConfigKey();

  const handleRefresh = () => {
    refetchSummary();
    refetchBuckets();
  };

  const isFetching = summaryFetching || bucketsFetching;
  const totalPages = bucketsData
    ? Math.ceil(bucketsData.totalElements / pageSize)
    : 0;

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">
            Rate Limit Buckets
          </h1>
          <p className="mt-1 text-sm text-text-secondary">
            Monitor and manage active rate limit buckets
          </p>
        </div>
        <RefreshButton onClick={handleRefresh} isRefreshing={isFetching} />
      </div>

      {/* Summary cards */}
      {summaryLoading ? (
        <div className="text-center py-8 text-text-muted text-sm">
          Loading summary...
        </div>
      ) : summary && summary.configs.length > 0 ? (
        <div className="space-y-3">
          <div className="flex items-center gap-2 text-xs text-text-muted">
            <Shield className="h-3.5 w-3.5" />
            <span>
              {summary.totalActiveBuckets} active bucket
              {summary.totalActiveBuckets !== 1 ? 's' : ''} across{' '}
              {summary.configs.length} config
              {summary.configs.length !== 1 ? 's' : ''}
            </span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {summary.configs.map((config) => (
              <ConfigSummaryCard
                key={config.configKey}
                config={config}
                onResetAll={() => deleteByConfigKey.mutate(config.configKey)}
                isResetting={deleteByConfigKey.isPending}
              />
            ))}
          </div>
        </div>
      ) : (
        !summaryLoading && null
      )}

      {/* Filters */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-text-muted" />
          <select
            value={configKeyFilter}
            onChange={(e) => {
              setConfigKeyFilter(e.target.value);
              setPage(0);
            }}
            className="text-sm rounded-lg border border-border-default bg-surface-card px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">All configs</option>
            {summary?.configs.map((config) => (
              <option key={config.configKey} value={config.configKey}>
                {config.displayName}
              </option>
            ))}
          </select>
        </div>
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
          <input
            type="text"
            placeholder="Filter by client IP..."
            value={clientIpFilter}
            onChange={(e) => {
              setClientIpFilter(e.target.value);
              setPage(0);
            }}
            className="w-full text-sm rounded-lg border border-border-default bg-surface-card pl-9 pr-3 py-2 text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      {/* Table */}
      {bucketsLoading ? (
        <div className="text-center py-12 text-text-muted text-sm">
          <Loader2 className="h-5 w-5 animate-spin mx-auto mb-2" />
          Loading buckets...
        </div>
      ) : bucketsData && bucketsData.content.length > 0 ? (
        <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-default bg-surface-inset">
                  <th className="text-left px-4 py-3 font-semibold text-[10px] uppercase tracking-wider text-text-muted">
                    Client
                  </th>
                  <th className="text-left px-4 py-3 font-semibold text-[10px] uppercase tracking-wider text-text-muted">
                    Config
                  </th>
                  <th className="text-left px-4 py-3 font-semibold text-[10px] uppercase tracking-wider text-text-muted">
                    Tokens
                  </th>
                  <th className="text-left px-4 py-3 font-semibold text-[10px] uppercase tracking-wider text-text-muted">
                    Expires
                  </th>
                  <th className="text-right px-4 py-3 font-semibold text-[10px] uppercase tracking-wider text-text-muted">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {bucketsData.content.map((bucket) => (
                  <BucketRow
                    key={bucket.bucketId}
                    bucket={bucket}
                    onDelete={() => deleteBucket.mutate(bucket.bucketId)}
                    isDeleting={deleteBucket.isPending}
                  />
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-border-default bg-surface-inset">
              <span className="text-xs text-text-muted">
                Page {page + 1} of {totalPages} ({bucketsData.totalElements}{' '}
                total)
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-border-default transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:bg-surface-card text-text-secondary"
                >
                  <ChevronLeft className="h-3.5 w-3.5" />
                  Prev
                </button>
                <button
                  onClick={() =>
                    setPage((p) => Math.min(totalPages - 1, p + 1))
                  }
                  disabled={page >= totalPages - 1}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-border-default transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:bg-surface-card text-text-secondary"
                >
                  Next
                  <ChevronRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="bg-surface-card rounded-lg border border-border-default p-12 text-center text-text-muted">
          <Shield className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p className="text-sm">No active rate limit buckets</p>
        </div>
      )}
    </div>
  );
};
