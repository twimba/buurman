import { useState } from 'react';
import {
  Database,
  Clock,
  TrendingUp,
  Trash2,
  ChevronDown,
  ChevronRight,
  Loader2,
  Zap,
} from 'lucide-react';
import { RefreshButton, ConfirmDialog } from '@buurman/ui';
import {
  useCachesList,
  useCacheDetail,
  useInvalidateCache,
} from '../hooks/useCaches';
import type { CacheInfoResponse } from '../generated/models';

// Helper to format relative time
const formatRelativeTime = (isoString: string | null): string => {
  if (!isoString) {
    return 'Never';
  }
  const diff = Date.now() - new Date(isoString).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) {
    return `${seconds}s ago`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}m ago`;
  }
  const hours = Math.floor(minutes / 60);
  return `${hours}h ago`;
};

// Hit rate color
const hitRateColor = (rate: number): string => {
  if (rate >= 0.9) {
    return 'text-success-text';
  }
  if (rate >= 0.5) {
    return 'text-warning-text';
  }
  return 'text-error-text';
};

// Stat cell
const StatCell = ({
  icon: Icon,
  label,
  value,
  valueClass,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  valueClass?: string;
}) => (
  <div>
    <div className="flex items-center gap-1.5 mb-1">
      <Icon className="h-3.5 w-3.5 text-text-muted" />
      <span className="text-[10px] font-bold uppercase tracking-widest text-text-muted">
        {label}
      </span>
    </div>
    <p className={`text-lg font-semibold ${valueClass ?? 'text-text-primary'}`}>
      {value}
    </p>
  </div>
);

// Cache entry view
const CacheEntryView = ({
  entry,
}: {
  entry: { key: string; summary: string; details: Record<string, unknown>[] };
}) => {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="rounded-lg border border-border-default bg-surface-card overflow-hidden">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between px-4 py-2.5 text-left hover:bg-surface-inset/50 transition-colors"
      >
        <div className="flex items-center gap-2">
          {expanded ? (
            <ChevronDown className="h-3.5 w-3.5 text-text-muted" />
          ) : (
            <ChevronRight className="h-3.5 w-3.5 text-text-muted" />
          )}
          <code className="text-xs font-mono text-text-primary">
            {entry.key}
          </code>
        </div>
        <span className="text-xs text-text-muted">{entry.summary}</span>
      </button>
      {expanded && entry.details.length > 0 && (
        <div className="border-t border-border-default overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-border-default bg-surface-inset">
                {Object.keys(entry.details[0]).map((col) => (
                  <th
                    key={col}
                    className="text-left px-3 py-2 font-semibold text-text-muted uppercase tracking-wider"
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {entry.details.map((row, idx) => (
                <tr
                  key={idx}
                  className="border-b border-border-default last:border-b-0"
                >
                  {Object.values(row).map((val, colIdx) => (
                    <td
                      key={colIdx}
                      className="px-3 py-2 text-text-secondary font-mono"
                    >
                      {val === null ? (
                        <span className="text-text-muted">null</span>
                      ) : typeof val === 'boolean' ? (
                        <span
                          className={
                            val ? 'text-success-text' : 'text-error-text'
                          }
                        >
                          {String(val)}
                        </span>
                      ) : (
                        String(val)
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// CacheCard component
const CacheCard = ({ cache }: { cache: CacheInfoResponse }) => {
  const [expanded, setExpanded] = useState(false);
  const [showInvalidateDialog, setShowInvalidateDialog] = useState(false);
  const invalidateCache = useInvalidateCache();
  const { data: detail, isLoading: detailLoading } = useCacheDetail(
    expanded ? cache.name : null
  );

  const handleInvalidate = () => {
    invalidateCache.mutate(cache.name, {
      onSuccess: () => setShowInvalidateDialog(false),
    });
  };

  const hasActivity = cache.hitCount + cache.missCount > 0;

  return (
    <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
      {/* Header */}
      <div className="px-5 py-4 border-b border-border-default">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Database className="h-5 w-5 text-primary-500" />
            <div>
              <div className="flex items-center gap-2">
                <code className="text-sm font-mono font-semibold text-text-primary">
                  {cache.name}
                </code>
                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-primary-50 text-primary-600 border border-primary-200">
                  {cache.type}
                </span>
              </div>
              <p className="text-xs text-text-muted mt-0.5">
                {cache.description}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setExpanded(!expanded)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-text-secondary hover:bg-surface-inset border border-border-default transition-colors"
            >
              {expanded ? (
                <ChevronDown className="h-3.5 w-3.5" />
              ) : (
                <ChevronRight className="h-3.5 w-3.5" />
              )}
              Entries
            </button>
            <button
              onClick={() => setShowInvalidateDialog(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-error-text hover:bg-error-bg border border-border-default transition-colors"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Invalidate
            </button>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="px-5 py-4">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <StatCell
            icon={Database}
            label="Entries"
            value={`${cache.entryCount} / ${cache.maxSize}`}
          />
          <StatCell
            icon={TrendingUp}
            label="Hit Rate"
            value={hasActivity ? `${(cache.hitRate * 100).toFixed(1)}%` : 'N/A'}
            valueClass={
              hasActivity ? hitRateColor(cache.hitRate) : 'text-text-muted'
            }
          />
          <StatCell
            icon={Zap}
            label="Hits / Misses"
            value={`${cache.hitCount.toLocaleString()} / ${cache.missCount.toLocaleString()}`}
          />
          <StatCell
            icon={Clock}
            label="Avg Load"
            value={
              cache.loadCount > 0
                ? `${cache.averageLoadTimeMs.toFixed(1)}ms`
                : 'N/A'
            }
          />
        </div>
        <div className="flex items-center gap-6 mt-3 pt-3 border-t border-border-subtle text-xs text-text-muted">
          <span>
            TTL:{' '}
            <span className="font-medium text-text-secondary">
              {cache.ttlSeconds}s
            </span>{' '}
            ({cache.refreshPolicy})
          </span>
          <span>
            Evictions:{' '}
            <span className="font-medium text-text-secondary">
              {cache.evictionCount}
            </span>
          </span>
          <span>
            Loads:{' '}
            <span className="font-medium text-text-secondary">
              {cache.loadCount}
            </span>
          </span>
          <span>
            Last Invalidated:{' '}
            <span className="font-medium text-text-secondary">
              {formatRelativeTime(cache.lastInvalidatedAt ?? null)}
            </span>
          </span>
        </div>
      </div>

      {/* Expanded entries */}
      {expanded && (
        <div className="border-t border-border-default bg-surface-inset px-5 py-4">
          {detailLoading ? (
            <div className="flex items-center gap-2 text-sm text-text-muted">
              <Loader2 className="h-4 w-4 animate-spin" />
              Loading entries...
            </div>
          ) : detail?.entries && detail.entries.length > 0 ? (
            <div className="space-y-3">
              {detail.entries.map((entry, idx) => (
                <CacheEntryView key={idx} entry={entry} />
              ))}
            </div>
          ) : (
            <p className="text-sm text-text-muted">Cache is empty</p>
          )}
        </div>
      )}

      {/* Invalidate confirmation */}
      {showInvalidateDialog && (
        <ConfirmDialog
          title="Invalidate Cache"
          message={`Are you sure you want to invalidate the "${cache.name}" cache? The cache will be repopulated on next access.`}
          confirmLabel="Invalidate"
          variant="danger"
          isLoading={invalidateCache.isPending}
          onConfirm={handleInvalidate}
          onCancel={() => setShowInvalidateDialog(false)}
        />
      )}
    </div>
  );
};

// Main page export
export const CachesPage = () => {
  const { data: caches, isLoading, isFetching, refetch } = useCachesList();

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Caches</h1>
          <p className="mt-1 text-sm text-text-secondary">
            Monitor and manage application caches
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-text-muted text-sm">
          Loading caches...
        </div>
      ) : caches && caches.length > 0 ? (
        <div className="space-y-4">
          {caches.map((cache) => (
            <CacheCard key={cache.name} cache={cache} />
          ))}
        </div>
      ) : (
        <div className="bg-surface-card rounded-lg border border-border-default p-12 text-center text-text-muted">
          <Database className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p className="text-sm">No caches found</p>
        </div>
      )}
    </div>
  );
};
