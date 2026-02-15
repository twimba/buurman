import { useState, useRef, useEffect, useCallback } from "react";
import { createPortal } from "react-dom";
import {
  Pause,
  Play,
  Zap,
  Timer,
  Clock,
  Check,
  ChevronDown,
  Pencil,
  Loader2,
  X,
} from "lucide-react";
import { format } from "date-fns";
import cronstrue from "cronstrue";
import { Pagination, ConfirmDialog, RefreshButton } from "@buurman/ui";
import { SortableHeader } from "../components/SortableHeader";
import {
  useScheduledJobs,
  usePauseJob,
  useResumeJob,
  useTriggerJob,
  useRescheduleJob,
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

const describeCron = (expr: string): string => {
  try {
    return cronstrue.toString(expr, { verbose: true });
  } catch {
    return expr;
  }
};

const CRON_FIELD_LABELS = ["SEC", "MIN", "HOUR", "DAY", "MON", "DOW", "YEAR"];
const CRON_FIELD_COLORS = [
  "from-slate-400 to-slate-500 dark:from-slate-500 dark:to-slate-400",
  "from-blue-400 to-blue-500 dark:from-blue-500 dark:to-blue-400",
  "from-indigo-400 to-indigo-500 dark:from-indigo-500 dark:to-indigo-400",
  "from-violet-400 to-violet-500 dark:from-violet-500 dark:to-violet-400",
  "from-purple-400 to-purple-500 dark:from-purple-500 dark:to-purple-400",
  "from-fuchsia-400 to-fuchsia-500 dark:from-fuchsia-500 dark:to-fuchsia-400",
  "from-slate-400 to-slate-500 dark:from-slate-500 dark:to-slate-400",
];

const CronTooltip = ({ expression }: { expression: string }) => {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const triggerRef = useRef<HTMLSpanElement>(null);
  const description = describeCron(expression);
  if (description === expression) return null;

  const parts = expression.split(/\s+/);

  const updatePosition = useCallback(() => {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    setPos({
      top: rect.top + window.scrollY,
      left: rect.left + rect.width / 2 + window.scrollX,
    });
  }, []);

  return (
    <span
      ref={triggerRef}
      className="inline-flex ml-1.5"
      onMouseEnter={() => {
        updatePosition();
        setOpen(true);
      }}
      onMouseLeave={() => setOpen(false)}
    >
      <span className="cursor-help p-0.5 rounded-md text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#5c7cfa]/10 transition-all duration-200">
        <Clock className="h-3.5 w-3.5" />
      </span>
      {open &&
        pos &&
        createPortal(
          <div
            className="fixed z-[9999] pointer-events-none"
            style={{
              top: pos.top,
              left: pos.left,
              transform: "translate(-50%, -100%)",
            }}
          >
            <div className="mb-2.5">
              <div className="relative bg-[#1a1d2e] dark:bg-[#0c0d14] rounded-xl shadow-xl shadow-black/20 border border-[#2a2e3f] dark:border-[#1e2130] px-4 py-3.5 min-w-[260px]">
                {/* Arrow */}
                <div className="absolute left-1/2 -translate-x-1/2 -bottom-1.5 w-3 h-3 rotate-45 bg-[#1a1d2e] dark:bg-[#0c0d14] border-r border-b border-[#2a2e3f] dark:border-[#1e2130]" />

                {/* Human-readable description */}
                <p className="text-[13px] font-medium text-white leading-snug mb-3">
                  {description}
                </p>

                {/* Cron field breakdown */}
                <div className="flex gap-1">
                  {parts.slice(0, 7).map((part, i) => (
                    <div
                      key={i}
                      className="flex flex-col items-center gap-1 flex-1 min-w-0"
                    >
                      <span className="text-[9px] font-bold uppercase tracking-wider text-[#6b7194]">
                        {CRON_FIELD_LABELS[i]}
                      </span>
                      <span
                        className={`w-full text-center text-[11px] font-mono font-semibold text-white rounded-md py-0.5 px-1 bg-gradient-to-b ${CRON_FIELD_COLORS[i]} bg-opacity-80`}
                      >
                        {part}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>,
          document.body,
        )}
    </span>
  );
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
  const rescheduleJob = useRescheduleJob();

  const [confirmAction, setConfirmAction] = useState<{
    type: "pause" | "resume" | "trigger";
    jobName: string;
    group: string;
  } | null>(null);

  const [editingJob, setEditingJob] = useState<{
    jobName: string;
    group: string;
    currentExpression: string;
  } | null>(null);
  const [cronInput, setCronInput] = useState("");

  // Execution history
  const [selectedJobs, setSelectedJobs] = useState<string[] | null>(null);
  const [historyStatusFilter, setHistoryStatusFilter] = useState("");
  const [jobDropdownOpen, setJobDropdownOpen] = useState(false);
  const jobDropdownRef = useRef<HTMLDivElement>(null);

  // Initialize selected jobs: all except notificationOutboxJob
  const allJobs = jobs ?? [];
  const uniqueJobNames = [...new Set(allJobs.map((j) => j.jobName))];
  const defaultSelectedJobs = uniqueJobNames.filter(
    (name) => name !== "notificationOutboxJob",
  );

  // Use default selection until user interacts
  const activeSelectedJobs = selectedJobs ?? defaultSelectedJobs;

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        jobDropdownRef.current &&
        !jobDropdownRef.current.contains(e.target as Node)
      ) {
        setJobDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const toggleJob = (name: string) => {
    const current = activeSelectedJobs;
    const next = current.includes(name)
      ? current.filter((n) => n !== name)
      : [...current, name];
    setSelectedJobs(next);
    handlePageChange(0);
  };

  const toggleAllJobs = () => {
    if (activeSelectedJobs.length === uniqueJobNames.length) {
      setSelectedJobs([]);
    } else {
      setSelectedJobs([...uniqueJobNames]);
    }
    handlePageChange(0);
  };
  const {
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
  } = usePagination({ defaultSort: "startedAt" });

  const {
    data: historyData,
    refetch: refetchHistory,
    isFetching: isHistoryFetching,
  } = useJobExecutionHistory({
    jobName:
      activeSelectedJobs.length === uniqueJobNames.length
        ? undefined
        : activeSelectedJobs,
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
            {allJobs.length}
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
            {allJobs.filter((j) => j.triggerState === "NORMAL").length}
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
            {allJobs.filter((j) => j.triggerState === "PAUSED").length}
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
            {allJobs.filter((j) => j.triggerState === "BLOCKED").length}
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
              {allJobs.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No scheduled jobs found.
                  </td>
                </tr>
              ) : (
                allJobs.map((job) => (
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
                      <span className="inline-flex items-center">
                        <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] font-mono">
                          {job.scheduleExpression ?? "-"}
                        </span>
                        {job.triggerType === "cron" &&
                          job.scheduleExpression && (
                            <CronTooltip expression={job.scheduleExpression} />
                          )}
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
                        {job.triggerType === "cron" && (
                          <button
                            onClick={() => {
                              setEditingJob({
                                jobName: job.jobName,
                                group: job.jobGroup,
                                currentExpression:
                                  job.scheduleExpression ?? "",
                              });
                              setCronInput(job.scheduleExpression ?? "");
                            }}
                            className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                            title="Edit schedule"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                        )}
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
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Execution History
          </h2>
          <RefreshButton
            onClick={() => refetchHistory()}
            isRefreshing={isHistoryFetching}
          />
        </div>
        <div className="flex flex-wrap gap-3 mb-4">
          <div className="relative" ref={jobDropdownRef}>
            <button
              onClick={() => setJobDropdownOpen(!jobDropdownOpen)}
              className={`${selectClass} flex items-center gap-2 min-w-[200px] justify-between`}
            >
              <span className="truncate">
                {activeSelectedJobs.length === uniqueJobNames.length
                  ? "All Jobs"
                  : activeSelectedJobs.length === 0
                    ? "No Jobs"
                    : `${activeSelectedJobs.length} job${activeSelectedJobs.length > 1 ? "s" : ""} selected`}
              </span>
              <ChevronDown className="h-4 w-4 shrink-0" />
            </button>
            {jobDropdownOpen && (
              <div className="absolute z-50 mt-1 w-64 bg-white dark:bg-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg shadow-lg max-h-64 overflow-y-auto">
                <button
                  onClick={toggleAllJobs}
                  className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors border-b border-[#e2e6f0] dark:border-[#2a2e3f]"
                >
                  <span
                    className={`flex items-center justify-center h-4 w-4 rounded border ${activeSelectedJobs.length === uniqueJobNames.length ? "bg-[#5c7cfa] border-[#5c7cfa] text-white" : "border-[#d1d5e0] dark:border-[#3a3f52]"}`}
                  >
                    {activeSelectedJobs.length === uniqueJobNames.length && (
                      <Check className="h-3 w-3" />
                    )}
                  </span>
                  <span className="text-[#1a1d2e] dark:text-[#eef0f6] font-medium">
                    Select All
                  </span>
                </button>
                {uniqueJobNames.map((name) => (
                  <button
                    key={name}
                    onClick={() => toggleJob(name)}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                  >
                    <span
                      className={`flex items-center justify-center h-4 w-4 rounded border ${activeSelectedJobs.includes(name) ? "bg-[#5c7cfa] border-[#5c7cfa] text-white" : "border-[#d1d5e0] dark:border-[#3a3f52]"}`}
                    >
                      {activeSelectedJobs.includes(name) && (
                        <Check className="h-3 w-3" />
                      )}
                    </span>
                    <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                      {name}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>
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
                <SortableHeader
                  field="startedAt"
                  label="Started"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <SortableHeader
                  field="durationMs"
                  label="Duration"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <SortableHeader
                  field="status"
                  label="Status"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
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

      {/* Edit Schedule Modal */}
      {editingJob && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-2xl w-full max-w-lg mx-4">
            <div className="flex items-center justify-between p-5 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <div>
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Edit Schedule
                </h3>
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-0.5">
                  {editingJob.jobName}
                </p>
              </div>
              <button
                onClick={() => setEditingJob(null)}
                className="p-1.5 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                  Cron Expression
                </label>
                <input
                  type="text"
                  value={cronInput}
                  onChange={(e) => setCronInput(e.target.value)}
                  placeholder="0 0 * * * ?"
                  className="w-full px-3 py-2.5 text-sm font-mono border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] outline-none focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
                />
              </div>

              {/* Cron Explanation Panel */}
              <div className="rounded-lg bg-[#f8f9fc] dark:bg-[#0c0d14] border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
                <div className="text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] mb-2">
                  Explanation
                </div>
                <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                  {cronInput.trim()
                    ? describeCron(cronInput.trim())
                    : "Enter a cron expression above"}
                </p>
                {editingJob.currentExpression !== cronInput.trim() &&
                  cronInput.trim() && (
                    <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-2">
                      Current:{" "}
                      <span className="font-mono">
                        {editingJob.currentExpression}
                      </span>{" "}
                      ({describeCron(editingJob.currentExpression)})
                    </p>
                  )}
              </div>

              <div className="rounded-lg bg-[#f8f9fc] dark:bg-[#0c0d14] border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
                <div className="text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] mb-2">
                  Quartz Cron Format
                </div>
                <div className="grid grid-cols-7 gap-1 text-center text-[11px]">
                  {[
                    "SEC",
                    "MIN",
                    "HOUR",
                    "DAY",
                    "MON",
                    "DOW",
                    "YEAR",
                  ].map((f) => (
                    <span
                      key={f}
                      className="font-mono text-[#6b7194] dark:text-[#8b90a8]"
                    >
                      {f}
                    </span>
                  ))}
                </div>
                <div className="grid grid-cols-7 gap-1 text-center text-[11px] mt-1">
                  {(cronInput.trim() || "* * * * * ? *")
                    .split(/\s+/)
                    .slice(0, 7)
                    .map((part, i) => (
                      <span
                        key={i}
                        className="font-mono font-medium text-[#1a1d2e] dark:text-[#eef0f6] bg-white dark:bg-[#1e2130] rounded px-1 py-0.5 border border-[#e2e6f0] dark:border-[#2a2e3f]"
                      >
                        {part}
                      </span>
                    ))}
                </div>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 p-5 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
              <button
                onClick={() => setEditingJob(null)}
                className="px-4 py-2 text-sm font-medium text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  rescheduleJob.mutate(
                    {
                      jobName: editingJob.jobName,
                      group: editingJob.group,
                      cronExpression: cronInput.trim(),
                    },
                    { onSuccess: () => setEditingJob(null) },
                  );
                }}
                disabled={
                  rescheduleJob.isPending ||
                  !cronInput.trim() ||
                  cronInput.trim() === editingJob.currentExpression
                }
                className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50 text-sm font-medium"
              >
                {rescheduleJob.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Pencil className="h-4 w-4" />
                )}
                Save Schedule
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
