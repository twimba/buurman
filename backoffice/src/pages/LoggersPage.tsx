import { useState, useEffect, useMemo } from "react";
import { Search, RotateCcw, ScrollText, Settings2, Regex } from "lucide-react";
import { ConfirmDialog, RefreshButton } from "@buurman/ui";
import {
  useLoggers,
  useSetLogLevel,
  useResetAllLogLevels,
} from "../hooks/useLoggers";
import { LoadingSpinner } from "../components/LoadingSpinner";

const LOG_LEVELS = ["TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF"] as const;

const levelBadgeConfig: Record<string, { label: string; className: string }> = {
  TRACE: {
    label: "TRACE",
    className:
      "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
  },
  DEBUG: {
    label: "DEBUG",
    className:
      "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
  },
  INFO: {
    label: "INFO",
    className:
      "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
  },
  WARN: {
    label: "WARN",
    className:
      "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
  },
  ERROR: {
    label: "ERROR",
    className:
      "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
  },
  OFF: {
    label: "OFF",
    className:
      "bg-gray-50 text-gray-700 ring-1 ring-gray-200 dark:bg-gray-900/30 dark:text-gray-300 dark:ring-gray-700",
  },
};

const LevelBadge = ({ level }: { level: string | null }) => {
  if (!level) {
    return (
      <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">---</span>
    );
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
  "px-2 py-1 text-xs rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors cursor-pointer";

const thClass =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]";

export const LoggersPage = () => {
  const { data: loggers, isLoading, isFetching, refetch } = useLoggers();
  const setLogLevel = useSetLogLevel();
  const resetAll = useResetAllLogLevels();

  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [isRegex, setIsRegex] = useState(false);
  const [regexError, setRegexError] = useState<string | null>(null);
  const [levelFilter, setLevelFilter] = useState<string>("ALL");
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  useEffect(() => {
    const timeout = setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => clearTimeout(timeout);
  }, [searchInput]);

  useEffect(() => {
    if (!isRegex || !debouncedSearch) {
      setRegexError(null);
      return;
    }
    try {
      new RegExp(debouncedSearch, "i");
      setRegexError(null);
    } catch (e) {
      setRegexError((e as Error).message);
    }
  }, [debouncedSearch, isRegex]);

  const filtered = useMemo(() => {
    if (!loggers) return [];
    let result = loggers;

    if (levelFilter !== "ALL") {
      result = result.filter((l) => l.effectiveLevel === levelFilter);
    }

    if (debouncedSearch) {
      if (isRegex) {
        try {
          const re = new RegExp(debouncedSearch, "i");
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
    () => loggers?.filter((l) => l.configuredLevel !== null).length ?? 0,
    [loggers],
  );

  const handleLevelChange = (loggerName: string, level: string) => {
    setLogLevel.mutate({
      loggerName,
      level: level === "RESET" ? null : level,
    });
  };

  const handleResetAll = () => {
    resetAll.mutate(undefined, {
      onSuccess: () => setShowResetConfirm(false),
    });
  };

  if (isLoading) return <LoadingSpinner message="Loading loggers..." />;

  return (
    <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14] p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Loggers
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Runtime log level management
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gradient-to-br from-[#5c7cfa] to-[#748ffc] text-white">
              <ScrollText className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {loggers?.length ?? 0}
              </p>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Total Loggers
              </p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gradient-to-br from-amber-400 to-amber-500 text-white">
              <Settings2 className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {configuredCount}
              </p>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Configured (non-default)
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="relative flex-1 min-w-[200px] max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder={
              isRegex
                ? "Regex pattern (e.g. com\\.buurman\\..*Service)"
                : "Filter loggers by name..."
            }
            className={`w-full pl-9 pr-3 py-2 text-sm rounded-lg border bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#6b7194] dark:placeholder-[#8b90a8] focus:outline-none focus:ring-2 transition-colors ${
              regexError
                ? "border-red-400 dark:border-red-600 focus:border-red-500 focus:ring-red-500/20"
                : "border-[#e2e6f0] dark:border-[#2a2e3f] focus:border-[#5c7cfa] focus:ring-[#5c7cfa]/20"
            }`}
          />
        </div>
        <button
          onClick={() => setIsRegex((prev) => !prev)}
          title={
            isRegex ? "Switch to plain text search" : "Switch to regex search"
          }
          className={`flex items-center justify-center h-9 w-9 rounded-lg border transition-colors ${
            isRegex
              ? "bg-[#5c7cfa] border-[#5c7cfa] text-white"
              : "bg-white dark:bg-[#14161f] border-[#e2e6f0] dark:border-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8] hover:border-[#5c7cfa] hover:text-[#5c7cfa]"
          }`}
        >
          <Regex className="h-4 w-4" />
        </button>
        <select
          value={levelFilter}
          onChange={(e) => setLevelFilter(e.target.value)}
          className={selectClass + " py-2"}
        >
          <option value="ALL">All Levels</option>
          {LOG_LEVELS.map((level) => (
            <option key={level} value={level}>
              {level}
            </option>
          ))}
        </select>
        {(debouncedSearch || levelFilter !== "ALL") && (
          <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
            {filtered.length} result{filtered.length !== 1 ? "s" : ""}
          </span>
        )}
        {regexError && (
          <span className="text-xs text-red-500 dark:text-red-400">
            Invalid regex
          </span>
        )}
        <div className="flex-1" />
        {configuredCount > 0 && (
          <button
            onClick={() => setShowResetConfirm(true)}
            disabled={resetAll.isPending}
            className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors disabled:opacity-50"
          >
            <RotateCcw className="h-4 w-4" />
            Reset All
          </button>
        )}
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className={thClass}>Logger Name</th>
                <th className={`${thClass} w-36`}>Configured</th>
                <th className={`${thClass} w-36`}>Effective</th>
                <th className={`${thClass} w-40`}>Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e2e6f0] dark:divide-[#2a2e3f]">
              {filtered.map((logger) => (
                <tr
                  key={logger.name}
                  className="hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                >
                  <td className="px-4 py-3">
                    <span
                      className="text-sm font-mono text-[#1a1d2e] dark:text-[#eef0f6]"
                      title={logger.name}
                    >
                      {logger.name || (
                        <em className="text-[#6b7194] dark:text-[#8b90a8]">
                          ROOT
                        </em>
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
                      value={logger.configuredLevel ?? ""}
                      onChange={(e) =>
                        handleLevelChange(
                          logger.name,
                          e.target.value || "RESET",
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
                    className="px-4 py-12 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]"
                  >
                    {debouncedSearch
                      ? "No loggers match your search"
                      : "No loggers found"}
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
          message={`This will reset ${configuredCount} configured logger${configuredCount !== 1 ? "s" : ""} back to their default levels. This action takes effect immediately.`}
          confirmLabel="Reset All"
          variant="danger"
          isLoading={resetAll.isPending}
        />
      )}
    </div>
  );
};
