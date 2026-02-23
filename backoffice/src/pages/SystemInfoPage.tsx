import { useState } from "react";
import {
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  Lock,
  Users,
  XCircle,
} from "lucide-react";
import { RefreshButton } from "@buurman/ui";
import { useSystemInfo, useAppBuildInfo } from "../hooks/useSystemInfo";
import { useAuth } from "../contexts/AuthContext";
import type {
  ConfigEntry,
  GcInfo,
  HttpLatencyStats,
  MetricEntry,
  MetricsSnapshot,
  ServiceHealth,
  ServiceHealthStatus,
  SessionInfo,
  MigrationEntry,
} from "../types";

const BUILD_INFO = {
  version: __APP_VERSION__,
  gitCommit: __GIT_COMMIT__,
  gitCommitFull: __GIT_COMMIT_FULL__,
  gitBranch: __GIT_BRANCH__,
  buildTime: __BUILD_TIME__,
};

const GITHUB_REPO = "https://github.com/twimba/buurman";

function hasRole(
  keycloak: { tokenParsed?: Record<string, unknown> },
  role: string,
): boolean {
  const roles = (keycloak.tokenParsed?.realm_access as { roles?: string[] })
    ?.roles;
  return roles?.includes(role) ?? false;
}

function formatRelativeTime(iso: string | null): string {
  if (!iso) {
    return "\u2014";
  }
  const diff = Date.now() - new Date(iso).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) {
    return `${seconds}s ago`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}m ago`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours}h ago`;
  }
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function formatUptime(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const parts: string[] = [];
  if (days > 0) {
    parts.push(`${days}d`);
  }
  if (hours > 0) {
    parts.push(`${hours}h`);
  }
  parts.push(`${minutes}m`);
  return parts.join(" ");
}

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(0)} KB`;
  }
  if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
  }
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

function formatNumber(n: number): string {
  if (Number.isInteger(n)) {
    return n.toLocaleString();
  }
  return n.toFixed(2);
}

const statusDotClass: Record<ServiceHealthStatus, string> = {
  UP: "bg-emerald-500",
  DOWN: "bg-red-500",
  DISABLED: "bg-slate-400 dark:bg-slate-600",
  UNKNOWN: "bg-amber-500 animate-pulse",
};

const statusLabel: Record<ServiceHealthStatus, string> = {
  UP: "UP",
  DOWN: "DOWN",
  DISABLED: "Disabled",
  UNKNOWN: "Checking...",
};

const migrationBadgeClass: Record<string, string> = {
  SUCCESS:
    "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
  PENDING:
    "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
  FAILED:
    "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
  OUT_OF_ORDER:
    "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
};

function getMigrationBadge(state: string): string {
  return (
    migrationBadgeClass[state] ??
    "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700"
  );
}

const metricTypeBadge: Record<string, string> = {
  COUNTER:
    "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
  GAUGE:
    "bg-violet-50 text-violet-700 ring-1 ring-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:ring-violet-700",
  TIMER:
    "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
  DISTRIBUTION:
    "bg-teal-50 text-teal-700 ring-1 ring-teal-200 dark:bg-teal-900/30 dark:text-teal-300 dark:ring-teal-700",
};

// --- Skeleton components ---

function SkeletonBlock({ className = "" }: { className?: string }) {
  return (
    <div
      className={`animate-pulse bg-[#e2e6f0] dark:bg-[#2a2e3f] rounded ${className}`}
    />
  );
}

function SkeletonCard({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
      {children}
    </div>
  );
}

// --- Collapsible panel ---

function CollapsiblePanel({
  title,
  defaultExpanded = true,
  headerRight,
  noPadding = false,
  children,
}: {
  title: string;
  defaultExpanded?: boolean;
  headerRight?: React.ReactNode;
  noPadding?: boolean;
  children: React.ReactNode;
}) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] mb-6">
      <button
        type="button"
        className="w-full px-5 py-4 flex items-center justify-between cursor-pointer hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors rounded-t-xl"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center gap-2">
          {expanded ? (
            <ChevronDown className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
          ) : (
            <ChevronRight className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
          )}
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
            {title}
          </h2>
        </div>
        {headerRight && (
          <div
            className="flex items-center gap-2"
            onClick={(e) => e.stopPropagation()}
          >
            {headerRight}
          </div>
        )}
      </button>
      {expanded && (
        <div className={noPadding ? "" : "px-5 pb-5"}>{children}</div>
      )}
    </div>
  );
}

// --- Progress bar helper ---

function ProgressBar({
  label,
  used,
  max,
  formatFn = formatBytes,
}: {
  label: string;
  used: number;
  max: number;
  formatFn?: (n: number) => string;
}) {
  const pct = max > 0 ? (used / max) * 100 : 0;
  const color =
    pct > 80 ? "bg-red-500" : pct > 60 ? "bg-amber-500" : "bg-emerald-500";

  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
          {label}
        </p>
        <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] font-mono">
          {formatFn(used)} / {formatFn(max)} ({pct.toFixed(0)}%)
        </p>
      </div>
      <div className="h-2.5 rounded-full bg-[#e2e6f0] dark:bg-[#2a2e3f] overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-500 ${color}`}
          style={{ width: `${Math.min(pct, 100)}%` }}
        />
      </div>
    </div>
  );
}

// --- Sub-components ---

function AlertBanner({
  data,
}: {
  data: { pendingCount: number; failedCount: number; downServices: number };
}) {
  const alerts: { message: string; type: "warning" | "error" }[] = [];

  if (data.failedCount > 0) {
    alerts.push({
      message: `${data.failedCount} failed migration(s) \u2014 immediate attention required`,
      type: "error",
    });
  }
  if (data.pendingCount > 0) {
    alerts.push({
      message: `${data.pendingCount} pending migration(s) detected`,
      type: "warning",
    });
  }
  if (data.downServices > 0) {
    alerts.push({
      message: `${data.downServices} service(s) unreachable`,
      type: "error",
    });
  }

  if (alerts.length === 0) {
    return null;
  }

  return (
    <div className="space-y-2 mb-6">
      {alerts.map((alert, i) => {
        const isError = alert.type === "error";
        return (
          <div
            key={i}
            className={`rounded-lg px-4 py-3 flex items-center gap-3 text-sm font-medium ${
              isError
                ? "bg-red-50 text-red-800 border border-red-200 dark:bg-red-900/20 dark:text-red-300 dark:border-red-800"
                : "bg-amber-50 text-amber-800 border border-amber-200 dark:bg-amber-900/20 dark:text-amber-300 dark:border-amber-800"
            }`}
          >
            {isError ? (
              <XCircle className="h-4 w-4 flex-shrink-0" />
            ) : (
              <AlertTriangle className="h-4 w-4 flex-shrink-0" />
            )}
            {alert.message}
          </div>
        );
      })}
    </div>
  );
}

function SessionsCard({ sessions }: { sessions: SessionInfo }) {
  return (
    <CollapsiblePanel title="Active Sessions">
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-4 py-4 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center">
              <Users className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {sessions.appActiveUsers}
              </p>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                App Users Online
              </p>
            </div>
          </div>
        </div>
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-4 py-4 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-violet-50 dark:bg-violet-900/30 flex items-center justify-center">
              <Users className="h-5 w-5 text-violet-600 dark:text-violet-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {sessions.backofficeActiveUsers}
              </p>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                Backoffice Users Online
              </p>
            </div>
          </div>
        </div>
      </div>
    </CollapsiblePanel>
  );
}

function VersionsCard({
  backendBuild,
  appBuild,
}: {
  backendBuild: {
    version: string;
    gitCommit: string | null;
    gitCommitFull: string | null;
    gitBranch: string | null;
    buildTime: string | null;
  };
  appBuild?: {
    version: string;
    gitCommit: string;
    gitCommitFull?: string;
    gitBranch: string;
    buildTime: string;
  } | null;
}) {
  const rows = [
    {
      component: "Backend",
      version: backendBuild.version,
      commit: backendBuild.gitCommit,
      commitFull: backendBuild.gitCommitFull,
      branch: backendBuild.gitBranch,
      buildTime: backendBuild.buildTime,
    },
    {
      component: "App",
      version: appBuild?.version ?? "\u2014",
      commit: appBuild?.gitCommit ?? null,
      commitFull: appBuild?.gitCommitFull ?? null,
      branch: appBuild?.gitBranch ?? null,
      buildTime: appBuild?.buildTime ?? null,
    },
    {
      component: "Backoffice",
      version: BUILD_INFO.version,
      commit: BUILD_INFO.gitCommit,
      commitFull: BUILD_INFO.gitCommitFull,
      branch: BUILD_INFO.gitBranch,
      buildTime: BUILD_INFO.buildTime,
    },
  ];

  return (
    <CollapsiblePanel title="Application Versions" noPadding>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Component
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Version
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Commit
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Branch
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Built
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                key={row.component}
                className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
              >
                <td className="px-5 py-3 text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {row.component}
                </td>
                <td className="px-5 py-3 text-sm font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
                  {row.version}
                </td>
                <td className="px-5 py-3 text-sm font-mono">
                  {row.commitFull && row.commitFull !== "unknown" ? (
                    <a
                      href={`${GITHUB_REPO}/commit/${row.commitFull}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[#5c7cfa] dark:text-[#91a7ff] hover:underline"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {row.commit}
                    </a>
                  ) : (
                    <span className="text-[#5c7cfa] dark:text-[#91a7ff]">
                      {row.commit ?? "\u2014"}
                    </span>
                  )}
                </td>
                <td className="px-5 py-3 text-sm">
                  {row.branch && row.branch !== "unknown" ? (
                    <a
                      href={`${GITHUB_REPO}/tree/${row.branch}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:underline"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {row.branch}
                    </a>
                  ) : (
                    <span className="text-[#6b7194] dark:text-[#8b90a8]">
                      {"\u2014"}
                    </span>
                  )}
                </td>
                <td
                  className="px-5 py-3 text-sm text-[#6b7194] dark:text-[#8b90a8]"
                  title={row.buildTime ?? undefined}
                >
                  {formatRelativeTime(row.buildTime)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </CollapsiblePanel>
  );
}

function ServiceHealthCard({ services }: { services: ServiceHealth[] }) {
  const upCount = services.filter((s) => s.status === "UP").length;
  return (
    <CollapsiblePanel
      title="Service Health"
      headerRight={
        <span className="text-xs font-mono text-[#6b7194] dark:text-[#8b90a8]">
          {upCount}/{services.length} up
        </span>
      }
    >
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
        {services.map((svc) => (
          <div
            key={svc.name}
            className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-4 py-3 border border-[#e2e6f0] dark:border-[#2a2e3f] hover:shadow-md transition-shadow"
            title={svc.error ?? svc.details ?? undefined}
          >
            <div className="flex items-center gap-2 mb-1">
              <div
                className={`h-2.5 w-2.5 rounded-full flex-shrink-0 ${statusDotClass[svc.status]}`}
              />
              <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                {svc.name}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span
                className={`text-xs ${svc.status === "UP" ? "text-emerald-600 dark:text-emerald-400" : svc.status === "DOWN" ? "text-red-600 dark:text-red-400" : "text-[#9ca0b8] dark:text-[#5c6180]"}`}
              >
                {statusLabel[svc.status]}
              </span>
              {svc.latencyMs != null && svc.status === "UP" && (
                <span className="text-xs text-[#9ca0b8] dark:text-[#5c6180] font-mono">
                  {svc.latencyMs}ms
                </span>
              )}
            </div>
            {svc.details && svc.status === "UP" && (
              <p className="text-[11px] text-[#9ca0b8] dark:text-[#5c6180] mt-1 truncate">
                {svc.details}
              </p>
            )}
          </div>
        ))}
      </div>
    </CollapsiblePanel>
  );
}

function RuntimeCard({
  runtime,
}: {
  runtime: {
    javaVersion: string;
    springBootVersion: string;
    activeProfiles: string;
    uptimeMs: number;
    heapUsedBytes: number;
    heapMaxBytes: number;
    nonHeapUsedBytes: number;
    nonHeapMaxBytes: number;
    cpuUsage: number;
    availableProcessors: number;
    threadCount: number;
    peakThreadCount: number;
    daemonThreadCount: number;
    garbageCollectors: GcInfo[];
    pid: number;
    serverTime: string;
  };
}) {
  const cpuPct = runtime.cpuUsage >= 0 ? runtime.cpuUsage * 100 : -1;
  const cpuColor =
    cpuPct > 80
      ? "bg-red-500"
      : cpuPct > 60
        ? "bg-amber-500"
        : "bg-emerald-500";

  const items = [
    { label: "Java", value: runtime.javaVersion },
    { label: "Spring Boot", value: runtime.springBootVersion },
    { label: "PID", value: String(runtime.pid) },
    { label: "Profiles", value: runtime.activeProfiles },
    { label: "Processors", value: String(runtime.availableProcessors) },
    { label: "Uptime", value: formatUptime(runtime.uptimeMs) },
    {
      label: "Server Time",
      value: new Date(runtime.serverTime).toLocaleString(),
    },
  ];

  return (
    <CollapsiblePanel title="Runtime">
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-x-6 gap-y-3 mb-5">
        {items.map((item) => (
          <div key={item.label}>
            <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
              {item.label}
            </p>
            <p className="text-sm font-mono text-[#1a1d2e] dark:text-[#eef0f6] mt-0.5">
              {item.value}
            </p>
          </div>
        ))}
      </div>

      {/* CPU bar */}
      {cpuPct >= 0 && (
        <div className="mb-3">
          <div className="flex items-center justify-between mb-1.5">
            <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
              CPU Usage
            </p>
            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] font-mono">
              {cpuPct.toFixed(1)}%
            </p>
          </div>
          <div className="h-2.5 rounded-full bg-[#e2e6f0] dark:bg-[#2a2e3f] overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${cpuColor}`}
              style={{ width: `${Math.min(cpuPct, 100)}%` }}
            />
          </div>
        </div>
      )}

      {/* Heap bar */}
      <div className="mb-3">
        <ProgressBar
          label="Heap Memory"
          used={runtime.heapUsedBytes}
          max={runtime.heapMaxBytes}
        />
      </div>

      {/* Non-heap bar */}
      {runtime.nonHeapMaxBytes > 0 && (
        <div className="mb-5">
          <ProgressBar
            label="Non-Heap Memory"
            used={runtime.nonHeapUsedBytes}
            max={runtime.nonHeapMaxBytes}
          />
        </div>
      )}

      {/* Threads */}
      <div className="grid grid-cols-3 gap-4 mb-5">
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-3 py-2 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">Threads</p>
          <p className="text-lg font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
            {runtime.threadCount}
          </p>
        </div>
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-3 py-2 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">Peak</p>
          <p className="text-lg font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
            {runtime.peakThreadCount}
          </p>
        </div>
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-3 py-2 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">Daemon</p>
          <p className="text-lg font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
            {runtime.daemonThreadCount}
          </p>
        </div>
      </div>

      {/* GC table */}
      {runtime.garbageCollectors.length > 0 && (
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-2">
            Garbage Collectors
          </p>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                  <th className="text-left px-3 py-2 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                    Collector
                  </th>
                  <th className="text-right px-3 py-2 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                    Collections
                  </th>
                  <th className="text-right px-3 py-2 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                    Time
                  </th>
                </tr>
              </thead>
              <tbody>
                {runtime.garbageCollectors.map((gc) => (
                  <tr
                    key={gc.name}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0"
                  >
                    <td className="px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      {gc.name}
                    </td>
                    <td className="px-3 py-2 text-sm font-mono text-right text-[#1a1d2e] dark:text-[#eef0f6]">
                      {gc.collectionCount.toLocaleString()}
                    </td>
                    <td className="px-3 py-2 text-sm font-mono text-right text-[#6b7194] dark:text-[#8b90a8]">
                      {gc.collectionTimeMs.toLocaleString()}ms
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </CollapsiblePanel>
  );
}

function LatencyStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="text-center">
      <p className="text-[10px] font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
        {label}
      </p>
      <p className="text-sm font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
        {value.toFixed(1)}
        <span className="text-[10px] font-normal text-[#9ca0b8] dark:text-[#5c6180] ml-0.5">
          ms
        </span>
      </p>
    </div>
  );
}

function LatencyBar({ latency }: { latency: HttpLatencyStats }) {
  // Normalize bars relative to p99 (or max, whichever is larger)
  const maxVal = Math.max(latency.p99Ms, latency.maxMs, 1);
  const bars = [
    { label: "Min", value: latency.minMs, color: "bg-emerald-400" },
    { label: "p50", value: latency.p50Ms, color: "bg-blue-400" },
    { label: "Mean", value: latency.meanMs, color: "bg-sky-400" },
    { label: "p75", value: latency.p75Ms, color: "bg-amber-400" },
    { label: "p95", value: latency.p95Ms, color: "bg-orange-400" },
    { label: "p99", value: latency.p99Ms, color: "bg-red-400" },
    { label: "Max", value: latency.maxMs, color: "bg-red-600" },
  ];

  return (
    <div className="space-y-1.5">
      {bars.map((bar) => (
        <div key={bar.label} className="flex items-center gap-2">
          <span className="text-[10px] font-mono text-[#9ca0b8] dark:text-[#5c6180] w-8 text-right">
            {bar.label}
          </span>
          <div className="flex-1 h-3 rounded bg-[#e2e6f0] dark:bg-[#2a2e3f] overflow-hidden">
            <div
              className={`h-full rounded transition-all duration-500 ${bar.color}`}
              style={{ width: `${Math.max((bar.value / maxVal) * 100, 0.5)}%` }}
            />
          </div>
          <span className="text-[10px] font-mono text-[#6b7194] dark:text-[#8b90a8] w-16 text-right">
            {bar.value.toFixed(1)}ms
          </span>
        </div>
      ))}
    </div>
  );
}

function MetricsCard({ metrics }: { metrics: MetricsSnapshot }) {
  // Group custom metrics by prefix (e.g., buurman.db, buurman.s3, etc.)
  const grouped: Record<string, MetricEntry[]> = {};
  for (const m of metrics.custom) {
    const parts = m.name.split(".");
    const group = parts.length >= 3 ? parts.slice(0, 2).join(".") : m.name;
    if (!grouped[group]) {
      grouped[group] = [];
    }
    grouped[group].push(m);
  }

  const { httpLatency } = metrics;

  return (
    <CollapsiblePanel title="Metrics">
      {/* HTTP summary */}
      <div className="grid grid-cols-3 gap-4 mb-5">
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-3 py-2 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
            HTTP Requests
          </p>
          <p className="text-lg font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
            {metrics.httpRequestCount.toLocaleString()}
          </p>
        </div>
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-3 py-2 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
            Total Time
          </p>
          <p className="text-lg font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
            {metrics.httpRequestTotalTimeSeconds.toFixed(1)}s
          </p>
        </div>
        <div className="bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg px-3 py-2 border border-[#e2e6f0] dark:border-[#2a2e3f]">
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
            Mean Latency
          </p>
          <p className="text-lg font-bold font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
            {httpLatency.meanMs.toFixed(1)}ms
          </p>
        </div>
      </div>

      {/* Latency distribution */}
      {metrics.httpRequestCount > 0 && (
        <div className="mb-5">
          <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-3">
            Response Time Distribution
          </p>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* Bar visualization */}
            <LatencyBar latency={httpLatency} />
            {/* Stat grid */}
            <div className="grid grid-cols-4 gap-3 content-start">
              <LatencyStat label="Min" value={httpLatency.minMs} />
              <LatencyStat label="Mean" value={httpLatency.meanMs} />
              <LatencyStat label="p50" value={httpLatency.p50Ms} />
              <LatencyStat label="p75" value={httpLatency.p75Ms} />
              <LatencyStat label="p95" value={httpLatency.p95Ms} />
              <LatencyStat label="p99" value={httpLatency.p99Ms} />
              <LatencyStat label="Max" value={httpLatency.maxMs} />
            </div>
          </div>
        </div>
      )}

      {/* Custom metrics */}
      {Object.keys(grouped).length > 0 && (
        <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
          <table className="w-full">
            <thead className="sticky top-0 bg-white dark:bg-[#14161f]">
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className="text-left px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Metric
                </th>
                <th className="text-left px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Type
                </th>
                <th className="text-right px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Value
                </th>
                <th className="text-left px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Tags
                </th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(grouped).map(([group, entries]) =>
                entries.map((m, i) => (
                  <tr
                    key={`${m.name}-${i}`}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28]"
                  >
                    <td className="px-3 py-1.5 text-xs font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="text-[#9ca0b8] dark:text-[#5c6180]">
                        {group}.
                      </span>
                      {m.name.replace(group + ".", "")}
                    </td>
                    <td className="px-3 py-1.5">
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${metricTypeBadge[m.type] ?? metricTypeBadge.COUNTER}`}
                      >
                        {m.type}
                      </span>
                    </td>
                    <td className="px-3 py-1.5 text-xs font-mono text-right text-[#1a1d2e] dark:text-[#eef0f6]">
                      {formatNumber(m.value)}
                    </td>
                    <td className="px-3 py-1.5 text-[11px] text-[#6b7194] dark:text-[#8b90a8] max-w-xs truncate">
                      {Object.entries(m.tags)
                        .filter(
                          ([k, v]) => !(k === "application" && v === "buurman"),
                        )
                        .map(([k, v]) => `${k}=${v}`)
                        .join(", ") || "\u2014"}
                    </td>
                  </tr>
                )),
              )}
            </tbody>
          </table>
        </div>
      )}

      {metrics.custom.length === 0 && (
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] text-center py-4">
          No custom metrics collected yet.
        </p>
      )}
    </CollapsiblePanel>
  );
}

function ConfigurationCard({
  configuration,
}: {
  configuration: ConfigEntry[];
}) {
  // Group by category
  const grouped: Record<string, ConfigEntry[]> = {};
  for (const entry of configuration) {
    if (!grouped[entry.category]) {
      grouped[entry.category] = [];
    }
    grouped[entry.category].push(entry);
  }

  return (
    <CollapsiblePanel
      title="Configuration"
      defaultExpanded={false}
      noPadding
      headerRight={
        <span className="text-xs font-mono text-[#6b7194] dark:text-[#8b90a8]">
          {configuration.length} entries
        </span>
      }
    >
      <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
        <table className="w-full">
          <thead className="sticky top-0 bg-white dark:bg-[#14161f]">
            <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Category
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Key
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Value
              </th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(grouped).map(([category, entries]) =>
              entries.map((entry, i) => (
                <tr
                  key={`${category}-${entry.key}`}
                  className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                >
                  <td className="px-5 py-2.5 text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    {i === 0 ? category : ""}
                  </td>
                  <td className="px-5 py-2.5 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                    {entry.key}
                  </td>
                  <td className="px-5 py-2.5 text-sm font-mono text-[#1a1d2e] dark:text-[#eef0f6] max-w-md truncate">
                    {entry.value.includes("***") ? (
                      <span className="text-[#9ca0b8] dark:text-[#5c6180]">
                        {entry.value}
                      </span>
                    ) : (
                      entry.value
                    )}
                  </td>
                </tr>
              )),
            )}
          </tbody>
        </table>
      </div>
    </CollapsiblePanel>
  );
}

function MigrationsCard({
  migrations,
}: {
  migrations: {
    currentVersion: string | null;
    appliedCount: number;
    pendingCount: number;
    failedCount: number;
    entries: MigrationEntry[];
  };
}) {
  const sorted = [...migrations.entries].sort((a, b) => {
    if (!a.version || !b.version) {
      return 0;
    }
    return b.version.localeCompare(a.version, undefined, { numeric: true });
  });

  return (
    <CollapsiblePanel
      title="Database Migrations"
      defaultExpanded={false}
      noPadding
      headerRight={
        <div className="flex items-center gap-2 flex-wrap">
          <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700">
            {migrations.appliedCount} Applied
          </span>
          {migrations.pendingCount > 0 && (
            <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700">
              {migrations.pendingCount} Pending
            </span>
          )}
          {migrations.failedCount > 0 && (
            <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700">
              {migrations.failedCount} Failed
            </span>
          )}
        </div>
      }
    >
      <div className="overflow-x-auto max-h-[480px] overflow-y-auto">
        <table className="w-full">
          <thead className="sticky top-0 bg-white dark:bg-[#14161f]">
            <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Version
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Description
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                State
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Duration
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                Installed
              </th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((m, i) => (
              <tr
                key={i}
                className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
              >
                <td className="px-5 py-3 text-sm font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
                  {m.version ?? "\u2014"}
                </td>
                <td className="px-5 py-3 text-sm text-[#1a1d2e] dark:text-[#eef0f6] max-w-xs truncate">
                  {m.description}
                </td>
                <td className="px-5 py-3">
                  <span
                    className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${getMigrationBadge(m.state)}`}
                  >
                    {m.state}
                  </span>
                </td>
                <td className="px-5 py-3 text-sm font-mono text-[#6b7194] dark:text-[#8b90a8]">
                  {m.executionTimeMs != null
                    ? `${m.executionTimeMs}ms`
                    : "\u2014"}
                </td>
                <td
                  className="px-5 py-3 text-sm text-[#6b7194] dark:text-[#8b90a8]"
                  title={m.installedOn ?? undefined}
                >
                  {formatRelativeTime(m.installedOn)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </CollapsiblePanel>
  );
}

// --- Loading skeleton ---

function LoadingSkeleton() {
  return (
    <div>
      <div className="mb-6 flex items-start justify-between">
        <div>
          <SkeletonBlock className="h-7 w-48 mb-2" />
          <SkeletonBlock className="h-4 w-72" />
        </div>
        <SkeletonBlock className="h-9 w-9 rounded-lg" />
      </div>
      <SkeletonCard>
        <SkeletonBlock className="h-32 w-full" />
      </SkeletonCard>
      <div className="mt-6">
        <SkeletonCard>
          <SkeletonBlock className="h-16 w-full" />
        </SkeletonCard>
      </div>
      <div className="mt-6">
        <SkeletonCard>
          <SkeletonBlock className="h-40 w-full" />
        </SkeletonCard>
      </div>
      <div className="mt-6">
        <SkeletonCard>
          <SkeletonBlock className="h-28 w-full" />
        </SkeletonCard>
      </div>
      <div className="mt-6">
        <SkeletonCard>
          <SkeletonBlock className="h-28 w-full" />
        </SkeletonCard>
      </div>
      <div className="mt-6">
        <SkeletonCard>
          <SkeletonBlock className="h-64 w-full" />
        </SkeletonCard>
      </div>
    </div>
  );
}

// --- Access denied ---

function AccessDenied() {
  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-8 text-center max-w-sm">
        <div className="w-12 h-12 rounded-full bg-red-50 dark:bg-red-900/20 flex items-center justify-center mx-auto mb-4">
          <Lock className="h-6 w-6 text-red-600 dark:text-red-400" />
        </div>
        <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
          Insufficient Permissions
        </h2>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          You need the{" "}
          <span className="font-mono text-xs bg-[#f8f9fc] dark:bg-[#1a1d28] px-1.5 py-0.5 rounded">
            BACKOFFICE_SYSTEM
          </span>{" "}
          role to view this page.
        </p>
      </div>
    </div>
  );
}

// --- Main page ---

export const SystemInfoPage = () => {
  const { keycloak } = useAuth();
  const canView = hasRole(keycloak, "BACKOFFICE_SYSTEM");

  if (!canView) {
    return <AccessDenied />;
  }

  return <SystemInfoContent />;
};

function SystemInfoContent() {
  const { data, isLoading, isFetching, error, refetch } = useSystemInfo();
  const { data: appBuild } = useAppBuildInfo();

  if (isLoading) {
    return <LoadingSkeleton />;
  }

  if (error || !data) {
    return (
      <div>
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            System Information
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Build versions, runtime, metrics, and service health.
          </p>
        </div>

        {/* Show backoffice build info even when backend is unreachable */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5 mb-6">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] mb-3">
            Backoffice Build
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                Version
              </p>
              <p className="text-sm font-mono text-[#1a1d2e] dark:text-[#eef0f6]">
                {BUILD_INFO.version}
              </p>
            </div>
            <div>
              <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                Commit
              </p>
              <p className="text-sm font-mono text-[#5c7cfa] dark:text-[#91a7ff]">
                {BUILD_INFO.gitCommit}
              </p>
            </div>
            <div>
              <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                Branch
              </p>
              <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {BUILD_INFO.gitBranch}
              </p>
            </div>
            <div>
              <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                Built
              </p>
              <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {formatRelativeTime(BUILD_INFO.buildTime)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg px-4 py-3 bg-red-50 text-red-800 border border-red-200 dark:bg-red-900/20 dark:text-red-300 dark:border-red-800 flex items-center gap-3 text-sm font-medium">
          <XCircle className="h-4 w-4 flex-shrink-0" />
          Failed to load backend system information. The API may be unreachable.
        </div>
      </div>
    );
  }

  const downServices = data.services.filter((s) => s.status === "DOWN").length;

  return (
    <div>
      {/* Header */}
      <div
        className="mb-6"
        style={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            System Information
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Build versions, runtime, metrics, and service health.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Alert banners */}
      <AlertBanner
        data={{
          pendingCount: data.migrations.pendingCount,
          failedCount: data.migrations.failedCount,
          downServices,
        }}
      />

      {/* Versions */}
      <VersionsCard backendBuild={data.build} appBuild={appBuild} />

      {/* Active Sessions */}
      <SessionsCard sessions={data.sessions} />

      {/* Service Health */}
      <ServiceHealthCard services={data.services} />

      {/* Runtime */}
      <RuntimeCard runtime={data.runtime} />

      {/* Metrics */}
      <MetricsCard metrics={data.metrics} />

      {/* Configuration */}
      <ConfigurationCard configuration={data.configuration} />

      {/* Migrations */}
      <MigrationsCard migrations={data.migrations} />
    </div>
  );
}
