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

function formatRelativeTime(iso?: string): string {
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
  return parts.join("");
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
  UP: "bg-success-text",
  DOWN: "bg-error-text",
  DISABLED: "bg-slate-400",
  UNKNOWN: "bg-warning-text animate-pulse",
};

const statusLabel: Record<ServiceHealthStatus, string> = {
  UP: "UP",
  DOWN: "DOWN",
  DISABLED: "Disabled",
  UNKNOWN: "Checking...",
};

const migrationBadgeClass: Record<string, string> = {
  SUCCESS: "bg-success-bg text-success-text ring-1 ring-success-border",
  PENDING: "bg-warning-bg text-warning-text ring-1 ring-warning-border",
  FAILED: "bg-error-bg text-error-text ring-1 ring-error-border",
  OUT_OF_ORDER: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
};

function getMigrationBadge(state: string): string {
  return (
    migrationBadgeClass[state] ??
    "bg-slate-50 text-slate-700 ring-1 ring-slate-200"
  );
}

const metricTypeBadge: Record<string, string> = {
  COUNTER: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
  GAUGE: "bg-violet-50 text-violet-700 ring-1 ring-violet-200",
  TIMER: "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
  DISTRIBUTION: "bg-teal-50 text-teal-700 ring-1 ring-teal-200",
};

// --- Skeleton components ---

function SkeletonBlock({ className = "" }: { className?: string }) {
  return (
    <div className={`animate-pulse bg-surface-inset rounded ${className}`} />
  );
}

function SkeletonCard({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-surface-card rounded-lg border border-border-default p-5">
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
    <div className="bg-surface-card rounded-lg border border-border-default mb-6">
      <button
        type="button"
        className="w-full px-5 py-4 flex items-center justify-between cursor-pointer hover:bg-surface-page transition-colors rounded-t-xl"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center gap-2">
          {expanded ? (
            <ChevronDown className="h-4 w-4 text-text-secondary " />
          ) : (
            <ChevronRight className="h-4 w-4 text-text-secondary " />
          )}
          <h2 className="text-sm font-semibold uppercase tracking-wider text-text-secondary">
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
        <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
          {label}
        </p>
        <p className="text-xs text-text-secondary font-mono">
          {formatFn(used)} / {formatFn(max)} ({pct.toFixed(0)}%)
        </p>
      </div>
      <div className="h-2.5 rounded-full bg-surface-inset overflow-hidden">
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
                ? "bg-error-bg text-error-text border border-error-border"
                : "bg-warning-bg text-warning-text border border-warning-border"
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
        <div className="bg-surface-page rounded-lg px-4 py-4 border border-border-default">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
              <Users className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <p className="text-2xl font-bold text-text-primary">
                {sessions.appActiveUsers}
              </p>
              <p className="text-xs text-text-secondary">App Users Online</p>
            </div>
          </div>
        </div>
        <div className="bg-surface-page rounded-lg px-4 py-4 border border-border-default">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-violet-50 flex items-center justify-center">
              <Users className="h-5 w-5 text-violet-600" />
            </div>
            <div>
              <p className="text-2xl font-bold text-text-primary">
                {sessions.backofficeActiveUsers}
              </p>
              <p className="text-xs text-text-secondary">
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
    gitCommit?: string;
    gitCommitFull?: string;
    gitBranch?: string;
    buildTime?: string;
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
      buildTime: appBuild?.buildTime,
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
            <tr className="border-b border-border-default">
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Component
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Version
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Commit
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Branch
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Built
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                key={row.component}
                className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors"
              >
                <td className="px-5 py-3 text-sm font-medium text-text-primary">
                  {row.component}
                </td>
                <td className="px-5 py-3 text-sm font-mono text-text-primary">
                  {row.version}
                </td>
                <td className="px-5 py-3 text-sm font-mono">
                  {row.commitFull && row.commitFull !== "unknown" ? (
                    <a
                      href={`${GITHUB_REPO}/commit/${row.commitFull}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary-500 hover:underline"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {row.commit}
                    </a>
                  ) : (
                    <span className="text-primary-500">
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
                      className="text-text-secondary hover:text-primary-500 hover:underline"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {row.branch}
                    </a>
                  ) : (
                    <span className="text-text-secondary">{"\u2014"}</span>
                  )}
                </td>
                <td
                  className="px-5 py-3 text-sm text-text-secondary"
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
        <span className="text-xs font-mono text-text-secondary">
          {upCount}/{services.length} up
        </span>
      }
    >
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
        {services.map((svc) => (
          <div
            key={svc.name}
            className="bg-surface-page rounded-lg px-4 py-3 border border-border-default hover:shadow-md transition-shadow"
            title={svc.error ?? svc.details ?? undefined}
          >
            <div className="flex items-center gap-2 mb-1">
              <div
                className={`h-2.5 w-2.5 rounded-full flex-shrink-0 ${statusDotClass[svc.status]}`}
              />
              <span className="text-sm font-medium text-text-primary truncate">
                {svc.name}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span
                className={`text-xs ${svc.status === "UP" ? "text-success-text" : svc.status === "DOWN" ? "text-error-text" : "text-text-muted "}`}
              >
                {statusLabel[svc.status]}
              </span>
              {svc.latencyMs != null && svc.status === "UP" && (
                <span className="text-xs text-text-muted font-mono">
                  {svc.latencyMs}ms
                </span>
              )}
            </div>
            {svc.details && svc.status === "UP" && (
              <p className="text-[11px] text-text-muted mt-1 truncate">
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
            <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
              {item.label}
            </p>
            <p className="text-sm font-mono text-text-primary mt-0.5">
              {item.value}
            </p>
          </div>
        ))}
      </div>

      {/* CPU bar */}
      {cpuPct >= 0 && (
        <div className="mb-3">
          <div className="flex items-center justify-between mb-1.5">
            <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
              CPU Usage
            </p>
            <p className="text-xs text-text-secondary font-mono">
              {cpuPct.toFixed(1)}%
            </p>
          </div>
          <div className="h-2.5 rounded-full bg-surface-inset overflow-hidden">
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
        <div className="bg-surface-page rounded-lg px-3 py-2 border border-border-default">
          <p className="text-xs text-text-muted">Threads</p>
          <p className="text-lg font-bold font-mono text-text-primary">
            {runtime.threadCount}
          </p>
        </div>
        <div className="bg-surface-page rounded-lg px-3 py-2 border border-border-default">
          <p className="text-xs text-text-muted">Peak</p>
          <p className="text-lg font-bold font-mono text-text-primary">
            {runtime.peakThreadCount}
          </p>
        </div>
        <div className="bg-surface-page rounded-lg px-3 py-2 border border-border-default">
          <p className="text-xs text-text-muted">Daemon</p>
          <p className="text-lg font-bold font-mono text-text-primary">
            {runtime.daemonThreadCount}
          </p>
        </div>
      </div>

      {/* GC table */}
      {runtime.garbageCollectors.length > 0 && (
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-text-muted mb-2">
            Garbage Collectors
          </p>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-default">
                  <th className="text-left px-3 py-2 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                    Collector
                  </th>
                  <th className="text-right px-3 py-2 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                    Collections
                  </th>
                  <th className="text-right px-3 py-2 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                    Time
                  </th>
                </tr>
              </thead>
              <tbody>
                {runtime.garbageCollectors.map((gc) => (
                  <tr
                    key={gc.name}
                    className="border-b border-border-default last:border-b-0"
                  >
                    <td className="px-3 py-2 text-sm text-text-primary">
                      {gc.name}
                    </td>
                    <td className="px-3 py-2 text-sm font-mono text-right text-text-primary">
                      {gc.collectionCount.toLocaleString()}
                    </td>
                    <td className="px-3 py-2 text-sm font-mono text-right text-text-secondary">
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
      <p className="text-[10px] font-medium uppercase tracking-wider text-text-muted">
        {label}
      </p>
      <p className="text-sm font-bold font-mono text-text-primary">
        {value.toFixed(1)}
        <span className="text-[10px] font-normal text-text-muted ml-0.5">
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
          <span className="text-[10px] font-mono text-text-muted w-8 text-right">
            {bar.label}
          </span>
          <div className="flex-1 h-3 rounded bg-surface-inset overflow-hidden">
            <div
              className={`h-full rounded transition-all duration-500 ${bar.color}`}
              style={{ width: `${Math.max((bar.value / maxVal) * 100, 0.5)}%` }}
            />
          </div>
          <span className="text-[10px] font-mono text-text-secondary w-16 text-right">
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
        <div className="bg-surface-page rounded-lg px-3 py-2 border border-border-default">
          <p className="text-xs text-text-muted">HTTP Requests</p>
          <p className="text-lg font-bold font-mono text-text-primary">
            {metrics.httpRequestCount.toLocaleString()}
          </p>
        </div>
        <div className="bg-surface-page rounded-lg px-3 py-2 border border-border-default">
          <p className="text-xs text-text-muted">Total Time</p>
          <p className="text-lg font-bold font-mono text-text-primary">
            {metrics.httpRequestTotalTimeSeconds.toFixed(1)}s
          </p>
        </div>
        <div className="bg-surface-page rounded-lg px-3 py-2 border border-border-default">
          <p className="text-xs text-text-muted">Mean Latency</p>
          <p className="text-lg font-bold font-mono text-text-primary">
            {httpLatency.meanMs.toFixed(1)}ms
          </p>
        </div>
      </div>

      {/* Latency distribution */}
      {metrics.httpRequestCount > 0 && (
        <div className="mb-5">
          <p className="text-xs font-medium uppercase tracking-wider text-text-muted mb-3">
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
            <thead className="sticky top-0 bg-surface-card">
              <tr className="border-b border-border-default">
                <th className="text-left px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Metric
                </th>
                <th className="text-left px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Type
                </th>
                <th className="text-right px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Value
                </th>
                <th className="text-left px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Tags
                </th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(grouped).map(([group, entries]) =>
                entries.map((m, i) => (
                  <tr
                    key={`${m.name}-${i}`}
                    className="border-b border-border-default last:border-b-0 hover:bg-surface-page"
                  >
                    <td className="px-3 py-1.5 text-xs font-mono text-text-primary">
                      <span className="text-text-muted">{group}.</span>
                      {m.name.replace(group + ".", "")}
                    </td>
                    <td className="px-3 py-1.5">
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${metricTypeBadge[m.type] ?? metricTypeBadge.COUNTER}`}
                      >
                        {m.type}
                      </span>
                    </td>
                    <td className="px-3 py-1.5 text-xs font-mono text-right text-text-primary">
                      {formatNumber(m.value)}
                    </td>
                    <td className="px-3 py-1.5 text-[11px] text-text-secondary max-w-xs truncate">
                      {Object.entries(m.tags)
                        .filter(
                          ([k, v]) => !(k === "application" && v === "buurman"),
                        )
                        .map(([k, v]) => `${k}=${v}`)
                        .join(",") || "\u2014"}
                    </td>
                  </tr>
                )),
              )}
            </tbody>
          </table>
        </div>
      )}

      {metrics.custom.length === 0 && (
        <p className="text-sm text-text-secondary text-center py-4">
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
        <span className="text-xs font-mono text-text-secondary">
          {configuration.length} entries
        </span>
      }
    >
      <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
        <table className="w-full">
          <thead className="sticky top-0 bg-surface-card">
            <tr className="border-b border-border-default">
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Category
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Key
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Value
              </th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(grouped).map(([category, entries]) =>
              entries.map((entry, i) => (
                <tr
                  key={`${category}-${entry.key}`}
                  className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors"
                >
                  <td className="px-5 py-2.5 text-sm font-medium text-text-primary">
                    {i === 0 ? category : ""}
                  </td>
                  <td className="px-5 py-2.5 text-sm text-text-secondary">
                    {entry.key}
                  </td>
                  <td className="px-5 py-2.5 text-sm font-mono text-text-primary max-w-md truncate">
                    {entry.value.includes("***") ? (
                      <span className="text-text-muted">{entry.value}</span>
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
    currentVersion?: string;
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
          <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-success-bg text-success-text ring-1 ring-success-border">
            {migrations.appliedCount} Applied
          </span>
          {migrations.pendingCount > 0 && (
            <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-warning-bg text-warning-text ring-1 ring-warning-border">
              {migrations.pendingCount} Pending
            </span>
          )}
          {migrations.failedCount > 0 && (
            <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-error-bg text-error-text ring-1 ring-error-border">
              {migrations.failedCount} Failed
            </span>
          )}
        </div>
      }
    >
      <div className="overflow-x-auto max-h-[480px] overflow-y-auto">
        <table className="w-full">
          <thead className="sticky top-0 bg-surface-card">
            <tr className="border-b border-border-default">
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Version
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Description
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                State
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Duration
              </th>
              <th className="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                Installed
              </th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((m, i) => (
              <tr
                key={i}
                className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors"
              >
                <td className="px-5 py-3 text-sm font-mono text-text-primary">
                  {m.version ?? "\u2014"}
                </td>
                <td className="px-5 py-3 text-sm text-text-primary max-w-xs truncate">
                  {m.description}
                </td>
                <td className="px-5 py-3">
                  <span
                    className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${getMigrationBadge(m.state)}`}
                  >
                    {m.state}
                  </span>
                </td>
                <td className="px-5 py-3 text-sm font-mono text-text-secondary">
                  {m.executionTimeMs != null
                    ? `${m.executionTimeMs}ms`
                    : "\u2014"}
                </td>
                <td
                  className="px-5 py-3 text-sm text-text-secondary"
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
      <div className="bg-surface-card rounded-lg border border-border-default p-8 text-center max-w-sm">
        <div className="w-12 h-12 rounded-full bg-error-bg flex items-center justify-center mx-auto mb-4">
          <Lock className="h-6 w-6 text-error-text" />
        </div>
        <h2 className="text-lg font-semibold text-text-primary mb-2">
          Insufficient Permissions
        </h2>
        <p className="text-sm text-text-secondary">
          You need the{" "}
          <span className="font-mono text-xs bg-surface-page px-1.5 py-0.5 rounded">
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
          <h1 className="text-2xl font-bold text-text-primary">
            System Information
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Build versions, runtime, metrics, and service health.
          </p>
        </div>

        {/* Show backoffice build info even when backend is unreachable */}
        <div className="bg-surface-card rounded-lg border border-border-default p-5 mb-6">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-text-secondary mb-3">
            Backoffice Build
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <p className="text-xs text-text-muted">Version</p>
              <p className="text-sm font-mono text-text-primary">
                {BUILD_INFO.version}
              </p>
            </div>
            <div>
              <p className="text-xs text-text-muted">Commit</p>
              <p className="text-sm font-mono text-primary-500">
                {BUILD_INFO.gitCommit}
              </p>
            </div>
            <div>
              <p className="text-xs text-text-muted">Branch</p>
              <p className="text-sm text-text-primary">
                {BUILD_INFO.gitBranch}
              </p>
            </div>
            <div>
              <p className="text-xs text-text-muted">Built</p>
              <p className="text-sm text-text-primary">
                {formatRelativeTime(BUILD_INFO.buildTime)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg px-4 py-3 bg-error-bg text-error-text border border-error-border flex items-center gap-3 text-sm font-medium">
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
          <h1 className="text-2xl font-bold text-text-primary">
            System Information
          </h1>
          <p className="text-sm text-text-secondary mt-1">
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
