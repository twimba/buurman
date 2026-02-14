import { useState } from "react";
import {
  Pause,
  Play,
  Zap,
  Timer,
  Clock,
  ChevronUp,
  ChevronDown,
  ArrowUpDown,
} from "lucide-react";
import { format } from "date-fns";
import { Pagination, ConfirmDialog, RefreshButton } from "@buurman/ui";
import {
  useScheduledJobs,
  usePauseJob,
  useResumeJob,
  useTriggerJob,
  useJobExecutionHistory,
} from "../hooks/useScheduler";
import { usePagination } from "../hooks/usePagination";
import { LoadingSpinner } from "../components/LoadingSpinner";

const triggerStateBadgeConfig: Record<
  string,
  { label: string; className: string }
> = {
  NORMAL: {
    label: "Normal",
    className:
      "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
  },
  PAUSED: {
    label: "Paused",
    className:
      "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
  },
  BLOCKED: {
    label: "Blocked",
    className:
      "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
  },
  COMPLETE: {
    label: "Complete",
    className:
      "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
  },
  ERROR: {
    label: "Error",
    className:
      "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
  },
  NONE: {
    label: "None",
    className:
      "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
  },
};

const execStatusBadgeConfig: Record<
  string,
  { label: string; className: string }
> = {
  SUCCESS: {
    label: "Success",
    className:
      "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
  },
  FAILED: {
    label: "Failed",
    className:
      "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
  },
  RUNNING: {
    label: "Running",
    className:
      "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
  },
};

const triggerTypeBadgeConfig: Record<
  string,
  { label: string; className: string }
> = {
  cron: {
    label: "Cron",
    className:
      "bg-violet-50 text-violet-700 ring-1 ring-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:ring-violet-700",
  },
  simple: {
    label: "Simple",
    className:
      "bg-sky-50 text-sky-700 ring-1 ring-sky-200 dark:bg-sky-900/30 dark:text-sky-300 dark:ring-sky-700",
  },
};

const StatusBadge = ({
  status,
  config,
}: {
  status: string;
  config: Record<string, { label: string; className: string }>;
}) => {
  const c = config[status] ?? {
    label: status,
    className:
      "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
  };
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${c.className}`}
    >
      {c.label}
    </span>
  );
};

const selectClass =
  "px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors";

const thClass =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]";

const formatDuration = (ms: number | null): string => {
  if (ms == null) return "-";
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.round((ms % 60000) / 1000);
  return `${minutes}m ${seconds}s`;
};

const formatFireTime = (iso: string | null): string => {
  if (!iso) return "-";
  try {
    return format(new Date(iso), "dd MMM yyyy HH:mm:ss");
  } catch {
    return iso;
  }
};

export const SchedulerPage = () => {
  const {
    data: jobs,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useScheduledJobs();
  const pauseJob = usePauseJob();
  const resumeJob = useResumeJob();
  const triggerJob = useTriggerJob();

  const [confirmAction, setConfirmAction] = useState<{
    type: "pause" | "resume" | "trigger";
    jobName: string;
    group: string;
  } | null>(null);

  // Execution history
  const [historyJobFilter, setHistoryJobFilter] = useState("");
  const [historyStatusFilter, setHistoryStatusFilter] = useState("");
  const {
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
  } = usePagination({ defaultSort: "startedAt" });

  const { data: historyData } = useJobExecutionHistory({
    jobName: historyJobFilter || undefined,
    status: historyStatusFilter || undefined,
    page,
    size,
    sort,
    direction,
  });

  const handleConfirmAction = () => {
    if (!confirmAction) return;
    const { type, jobName, group } = confirmAction;
    const mutation =
      type === "pause" ? pauseJob : type === "resume" ? resumeJob : triggerJob;
    mutation.mutate(
      { jobName, group },
      { onSuccess: () => setConfirmAction(null) },
    );
  };

  const confirmLabels = {
    pause: {
      title: "Pause Job",
      message: "This will pause the job. It will not execute until resumed.",
      confirmLabel: "Pause",
      variant: "danger" as const,
    },
    resume: {
      title: "Resume Job",
      message:
        "This will resume the paused job. It will execute on its next scheduled time.",
      confirmLabel: "Resume",
      variant: "default" as const,
    },
    trigger: {
      title: "Trigger Job Now",
      message:
        "This will immediately trigger the job execution outside of its normal schedule.",
      confirmLabel: "Trigger",
      variant: "default" as const,
    },
  };

  const SortableHeader = ({
    field,
    label,
  }: {
    field: string;
    label: string;
  }) => (
    <th
      onClick={() => handleSortChange(field)}
      className="cursor-pointer select-none text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] transition-colors"
    >
      <div style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
        {label}
        {sort === field ? (
          direction === "asc" ? (
            <ChevronUp className="h-3.5 w-3.5" />
          ) : (
            <ChevronDown className="h-3.5 w-3.5" />
          )
        ) : (
          <ArrowUpDown className="h-3.5 w-3.5 opacity-30" />
        )}
      </div>
    </th>
  );

  if (isLoading) {
    return <LoadingSpinner message="Loading scheduler..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">
          Failed to load scheduler data.
        </p>
      </div>
    );
  }

  const jobList = jobs ?? [];
  const uniqueJobNames = [...new Set(jobList.map((j) => j.jobName))];
  const historyRecords = historyData?.content ?? [];

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
            Scheduler
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            View and manage Quartz scheduled jobs across the platform.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div
            style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
          >
            <Timer className="h-4 w-4" />
            Total Jobs
          </div>
          <div className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            {jobList.length}
          </div>
        </div>
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div
            style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
          >
            <Play className="h-4 w-4 text-emerald-500" />
            Active
          </div>
          <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            {jobList.filter((j) => j.triggerState === "NORMAL").length}
          </div>
        </div>
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div
            style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
          >
            <Pause className="h-4 w-4 text-amber-500" />
            Paused
          </div>
          <div className="text-2xl font-bold text-amber-600 dark:text-amber-400">
            {jobList.filter((j) => j.triggerState === "PAUSED").length}
          </div>
        </div>
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div
            style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
          >
            <Clock className="h-4 w-4 text-blue-500" />
            Blocked
          </div>
          <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
            {jobList.filter((j) => j.triggerState === "BLOCKED").length}
          </div>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden mb-8">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className={thClass}>Job Name</th>
                <th className={thClass}>Group</th>
                <th className={thClass}>Type</th>
                <th className={thClass}>Schedule</th>
                <th className={thClass}>Status</th>
                <th className={thClass}>Next Fire</th>
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {jobList.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No scheduled jobs found.
                  </td>
                </tr>
              ) : (
                jobList.map((job) => (
                  <tr
                    key={`${job.jobGroup}.${job.jobName}.${job.triggerName}`}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {job.jobName}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        {job.jobGroup}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {job.triggerType && (
                        <StatusBadge
                          status={job.triggerType}
                          config={triggerTypeBadgeConfig}
                        />
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] font-mono">
                        {job.scheduleExpression ?? "-"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge
                        status={job.triggerState}
                        config={triggerStateBadgeConfig}
                      />
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8] whitespace-nowrap">
                        {formatFireTime(job.nextFireTime)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "flex-end",
                          gap: "0.25rem",
                        }}
                      >
                        {job.triggerState === "PAUSED" ? (
                          <button
                            onClick={() =>
                              setConfirmAction({
                                type: "resume",
                                jobName: job.jobName,
                                group: job.jobGroup,
                              })
                            }
                            className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                            title="Resume job"
                          >
                            <Play className="h-4 w-4" />
                          </button>
                        ) : (
                          <button
                            onClick={() =>
                              setConfirmAction({
                                type: "pause",
                                jobName: job.jobName,
                                group: job.jobGroup,
                              })
                            }
                            className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-amber-600 dark:hover:text-amber-400 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                            title="Pause job"
                          >
                            <Pause className="h-4 w-4" />
                          </button>
                        )}
                        <button
                          onClick={() =>
                            setConfirmAction({
                              type: "trigger",
                              jobName: job.jobName,
                              group: job.jobGroup,
                            })
                          }
                          className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                          title="Trigger now"
                        >
                          <Zap className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Execution History Section */}
      <div className="mb-4">
        <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
          Execution History
        </h2>
        <div className="flex flex-wrap gap-3 mb-4">
          <select
            value={historyJobFilter}
            onChange={(e) => {
              setHistoryJobFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All Jobs</option>
            {uniqueJobNames.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <select
            value={historyStatusFilter}
            onChange={(e) => {
              setHistoryStatusFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All Statuses</option>
            <option value="SUCCESS">Success</option>
            <option value="FAILED">Failed</option>
            <option value="RUNNING">Running</option>
          </select>
        </div>
      </div>

      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className={thClass}>Job Name</th>
                <th className={thClass}>Group</th>
                <SortableHeader field="startedAt" label="Started" />
                <SortableHeader field="durationMs" label="Duration" />
                <SortableHeader field="status" label="Status" />
                <th className={thClass}>Error</th>
              </tr>
            </thead>
            <tbody>
              {historyRecords.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No execution history found.
                  </td>
                </tr>
              ) : (
                historyRecords.map((exec) => (
                  <tr
                    key={exec.id}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {exec.jobName}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        {exec.jobGroup}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8] whitespace-nowrap">
                        {format(
                          new Date(exec.startedAt),
                          "dd MMM yyyy HH:mm:ss",
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] font-mono">
                        {formatDuration(exec.durationMs)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge
                        status={exec.status}
                        config={execStatusBadgeConfig}
                      />
                    </td>
                    <td className="px-4 py-3 max-w-[300px]">
                      {exec.errorMessage ? (
                        <span
                          className="text-sm text-red-600 dark:text-red-400 truncate block"
                          title={exec.errorMessage}
                        >
                          {exec.errorMessage}
                        </span>
                      ) : (
                        <span className="text-sm text-[#9ca0b8] dark:text-[#5c6180]">
                          -
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* History Pagination */}
      {historyData && historyData.totalElements > 0 && (
        <div className="mt-4">
          <Pagination
            page={historyData.page}
            totalPages={historyData.totalPages}
            totalElements={historyData.totalElements}
            size={historyData.size}
            onPageChange={handlePageChange}
            onSizeChange={handleSizeChange}
          />
        </div>
      )}

      {/* Confirm Dialog */}
      {confirmAction && (
        <ConfirmDialog
          title={confirmLabels[confirmAction.type].title}
          message={`${confirmLabels[confirmAction.type].message}\n\nJob: ${confirmAction.jobName} (${confirmAction.group})`}
          confirmLabel={confirmLabels[confirmAction.type].confirmLabel}
          cancelLabel="Cancel"
          variant={confirmLabels[confirmAction.type].variant}
          isLoading={
            pauseJob.isPending || resumeJob.isPending || triggerJob.isPending
          }
          onConfirm={handleConfirmAction}
          onCancel={() => setConfirmAction(null)}
        />
      )}
    </div>
  );
};
