import { useState, useEffect, useMemo } from 'react';
import { Search, RotateCcw, ScrollText, Settings2, Regex } from 'lucide-react';
import { ConfirmDialog, RefreshButton } from '@buurman/ui';
import {
  useLoggers,
  useSetLogLevel,
  useResetAllLogLevels,
} from '../hooks/useLoggers';
import { LoadingSpinner } from '../components/LoadingSpinner';

const LOG_LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'] as const;

const levelBadgeConfig: Record<string, { label: string; className: string }> = {
  TRACE: {
    label: 'TRACE',
    className: 'bg-slate-50 text-slate-700 ring-1 ring-slate-200',
  },
  DEBUG: {
    label: 'DEBUG',
    className: 'bg-blue-50 text-blue-700 ring-1 ring-blue-200',
  },
  INFO: {
    label: 'INFO',
    className: 'bg-success-bg text-success-text ring-1 ring-success-border',
  },
  WARN: {
    label: 'WARN',
    className: 'bg-warning-bg text-warning-text ring-1 ring-warning-border',
  },
  ERROR: {
    label: 'ERROR',
    className: 'bg-error-bg text-error-text ring-1 ring-error-border',
  },
  OFF: {
    label: 'OFF',
    className: 'bg-gray-50 text-gray-700 ring-1 ring-gray-200',
  },
};

const LevelBadge = ({ level }: { level?: string }) => {
  if (!level) {
    return <span className="text-xs text-text-secondary">---</span>;
  }
  const c = levelBadgeConfig[level] ?? levelBadgeConfig.INFO;
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${c.className}`}
    >
      {c.label}
    </span>
  );
};

const selectClass =
  'px-2 py-1 text-xs rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors cursor-pointer';

const thClass =
  'text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary';

export const LoggersPage = () => {
  const { data: loggers, isLoading, isFetching, refetch } = useLoggers();
  const setLogLevel = useSetLogLevel();
  const resetAll = useResetAllLogLevels();

  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [isRegex, setIsRegex] = useState(false);
  const [levelFilter, setLevelFilter] = useState<string>('ALL');
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  useEffect(() => {
    const timeout = setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => clearTimeout(timeout);
  }, [searchInput]);

  const regexError = useMemo(() => {
    if (!isRegex || !debouncedSearch) {
      return null;
    }
    try {
      new RegExp(debouncedSearch, 'i');
      return null;
    } catch (e) {
      return (e as Error).message;
    }
  }, [debouncedSearch, isRegex]);

  const filtered = useMemo(() => {
    if (!loggers) {
      return [];
    }
    let result = loggers;

    if (levelFilter !== 'ALL') {
      result = result.filter((l) => l.effectiveLevel === levelFilter);
    }

    if (debouncedSearch) {
      if (isRegex) {
        try {
          const re = new RegExp(debouncedSearch, 'i');
          result = result.filter((l) => re.test(l.name));
        } catch {
          // invalid regex — don't filter
        }
      } else {
        const lower = debouncedSearch.toLowerCase();
        result = result.filter((l) => l.name.toLowerCase().includes(lower));
      }
    }

    return result;
  }, [loggers, debouncedSearch, isRegex, levelFilter]);

  const configuredCount = useMemo(
    () => loggers?.filter((l) => l.configuredLevel != null).length ?? 0,
    [loggers]
  );

  const handleLevelChange = (loggerName: string, level: string) => {
    setLogLevel.mutate({
      loggerName,
      level: level === 'RESET' ? null : level,
    });
  };

  const handleResetAll = () => {
    resetAll.mutate(undefined, {
      onSuccess: () => setShowResetConfirm(false),
    });
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading loggers..." />;
  }

  return (
    <div className="min-h-screen bg-surface-page p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Loggers</h1>
          <p className="text-sm text-text-secondary mt-1">
            Runtime log level management
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <div className="bg-surface-card rounded-lg border border-border-default p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gradient-to-br from-primary-500 to-primary-400 text-white">
              <ScrollText className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold text-text-primary">
                {loggers?.length ?? 0}
              </p>
              <p className="text-xs text-text-secondary">Total Loggers</p>
            </div>
          </div>
        </div>
        <div className="bg-surface-card rounded-lg border border-border-default p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gradient-to-br from-amber-400 to-amber-500 text-white">
              <Settings2 className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold text-text-primary">
                {configuredCount}
              </p>
              <p className="text-xs text-text-secondary">
                Configured (non-default)
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="relative flex-1 min-w-[200px] max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-secondary " />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder={
              isRegex
                ? 'Regex pattern (e.g. com\\.buurman\\..*Service)'
                : 'Filter loggers by name...'
            }
            className={`w-full pl-9 pr-3 py-2 text-sm rounded-lg border bg-surface-card text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 transition-colors ${
              regexError
                ? 'border-error-border focus:border-error-border focus:ring-error-border/20'
                : 'border-border-default focus:border-primary-500 focus:ring-primary-500/20'
            }`}
          />
        </div>
        <button
          onClick={() => setIsRegex((prev) => !prev)}
          title={
            isRegex ? 'Switch to plain text search' : 'Switch to regex search'
          }
          className={`flex items-center justify-center h-9 w-9 rounded-lg border transition-colors ${
            isRegex
              ? 'bg-primary-500 border-primary-500 text-white'
              : 'bg-surface-card border-border-default text-text-secondary hover:border-primary-500 hover:text-primary-500'
          }`}
        >
          <Regex className="h-4 w-4" />
        </button>
        <select
          value={levelFilter}
          onChange={(e) => setLevelFilter(e.target.value)}
          className={selectClass + ' py-2'}
        >
          <option value="ALL">All Levels</option>
          {LOG_LEVELS.map((level) => (
            <option key={level} value={level}>
              {level}
            </option>
          ))}
        </select>
        {(debouncedSearch || levelFilter !== 'ALL') && (
          <span className="text-xs text-text-secondary">
            {filtered.length} result{filtered.length !== 1 ? 's' : ''}
          </span>
        )}
        {regexError && (
          <span className="text-xs text-error-text">Invalid regex</span>
        )}
        <div className="flex-1" />
        {configuredCount > 0 && (
          <button
            onClick={() => setShowResetConfirm(true)}
            disabled={resetAll.isPending}
            className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-error-text bg-error-bg border border-error-border rounded-lg hover:bg-error-bg transition-colors disabled:opacity-50"
          >
            <RotateCcw className="h-4 w-4" />
            Reset All
          </button>
        )}
      </div>

      {/* Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
                <th className={thClass}>Logger Name</th>
                <th className={`${thClass} w-36`}>Configured</th>
                <th className={`${thClass} w-36`}>Effective</th>
                <th className={`${thClass} w-40`}>Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {filtered.map((logger) => (
                <tr
                  key={logger.name}
                  className="hover:bg-surface-page transition-colors"
                >
                  <td className="px-4 py-3">
                    <span
                      className="text-sm font-mono text-text-primary"
                      title={logger.name}
                    >
                      {logger.name || (
                        <em className="text-text-secondary">ROOT</em>
                      )}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <LevelBadge level={logger.configuredLevel} />
                  </td>
                  <td className="px-4 py-3">
                    <LevelBadge level={logger.effectiveLevel} />
                  </td>
                  <td className="px-4 py-3">
                    <select
                      value={logger.configuredLevel ?? ''}
                      onChange={(e) =>
                        handleLevelChange(
                          logger.name,
                          e.target.value || 'RESET'
                        )
                      }
                      className={selectClass}
                      disabled={setLogLevel.isPending}
                    >
                      <option value="">Default</option>
                      {LOG_LEVELS.map((level) => (
                        <option key={level} value={level}>
                          {level}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-12 text-center text-sm text-text-secondary"
                  >
                    {debouncedSearch
                      ? 'No loggers match your search'
                      : 'No loggers found'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Reset All Confirmation */}
      {showResetConfirm && (
        <ConfirmDialog
          onCancel={() => setShowResetConfirm(false)}
          onConfirm={handleResetAll}
          title="Reset All Log Levels"
          message={`This will reset ${configuredCount} configured logger${configuredCount !== 1 ? 's' : ''} back to their default levels. This action takes effect immediately.`}
          confirmLabel="Reset All"
          variant="danger"
          isLoading={resetAll.isPending}
        />
      )}
    </div>
  );
};
