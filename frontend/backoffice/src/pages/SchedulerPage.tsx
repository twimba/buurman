import {
  useState,
  useRef,
  useEffect,
  useCallback,
  useMemo,
  Fragment,
} from 'react';
import { createPortal } from 'react-dom';
import {
  Pause,
  Play,
  PlayCircle,
  Timer,
  Clock,
  Check,
  ChevronDown,
  ChevronRight,
  Pencil,
  Loader2,
  X,
  AlertCircle,
  Layers,
} from 'lucide-react';
import { formatDateTimeFull } from '../utils/dateFormatting';
import {
  jobLabel,
  jobDescription,
  groupLabel,
  groupDescription,
} from '../utils/jobDisplay';
import cronstrue from 'cronstrue';
import { Pagination, ConfirmDialog, RefreshButton } from '@buurman/ui';
import { SortableHeader } from '../components/SortableHeader';
import {
  useScheduledJobs,
  usePauseJob,
  useResumeJob,
  useTriggerJob,
  useRescheduleJob,
  useJobExecutionHistory,
} from '../hooks/useScheduler';
import { usePagination } from '../hooks/usePagination';
import { LoadingSpinner } from '../components/LoadingSpinner';

const triggerStateBadgeConfig: Record<
  string,
  { label: string; className: string }
> = {
  NORMAL: {
    label: 'Normal',
    className: 'bg-success-bg text-success-text ring-1 ring-success-border',
  },
  PAUSED: {
    label: 'Paused',
    className: 'bg-warning-bg text-warning-text ring-1 ring-warning-border',
  },
  BLOCKED: {
    label: 'Blocked',
    className: 'bg-info-bg text-info-text ring-1 ring-info-border',
  },
  COMPLETE: {
    label: 'Complete',
    className: 'bg-slate-50 text-slate-700 ring-1 ring-slate-200',
  },
  ERROR: {
    label: 'Error',
    className: 'bg-error-bg text-error-text ring-1 ring-error-border',
  },
  NONE: {
    label: 'None',
    className: 'bg-slate-50 text-slate-700 ring-1 ring-slate-200',
  },
};

const execStatusBadgeConfig: Record<
  string,
  { label: string; className: string }
> = {
  SUCCESS: {
    label: 'Success',
    className: 'bg-success-bg text-success-text ring-1 ring-success-border',
  },
  FAILED: {
    label: 'Failed',
    className: 'bg-error-bg text-error-text ring-1 ring-error-border',
  },
  RUNNING: {
    label: 'Running',
    className: 'bg-info-bg text-info-text ring-1 ring-info-border',
  },
};

const triggerTypeBadgeConfig: Record<
  string,
  { label: string; className: string }
> = {
  cron: {
    label: 'Cron',
    className: 'bg-violet-50 text-violet-700 ring-1 ring-violet-200',
  },
  simple: {
    label: 'Simple',
    className: 'bg-sky-50 text-sky-700 ring-1 ring-sky-200',
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
    className: 'bg-slate-50 text-slate-700 ring-1 ring-slate-200',
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
  'px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';

const thClass =
  'text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary';

const formatDuration = (ms?: number): string => {
  if (ms == null) {
    return '-';
  }
  if (ms < 1000) {
    return `${ms}ms`;
  }
  if (ms < 60000) {
    return `${(ms / 1000).toFixed(1)}s`;
  }
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.round((ms % 60000) / 1000);
  return `${minutes}m ${seconds}s`;
};

const formatFireTime = (iso?: string): string => {
  if (!iso) {
    return '-';
  }
  try {
    return formatDateTimeFull(iso);
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

const CRON_FIELD_LABELS = ['SEC', 'MIN', 'HOUR', 'DAY', 'MON', 'DOW', 'YEAR'];
const CRON_FIELD_COLORS = [
  'from-slate-400 to-slate-500',
  'from-blue-400 to-blue-500',
  'from-primary-400 to-primary-500',
  'from-violet-400 to-violet-500',
  'from-purple-400 to-purple-500',
  'from-fuchsia-400 to-fuchsia-500',
  'from-slate-400 to-slate-500',
];

const CronTooltip = ({ expression }: { expression: string }) => {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const triggerRef = useRef<HTMLSpanElement>(null);

  const updatePosition = useCallback(() => {
    if (!triggerRef.current) {
      return;
    }
    const rect = triggerRef.current.getBoundingClientRect();
    setPos({
      top: rect.top + window.scrollY,
      left: rect.left + rect.width / 2 + window.scrollX,
    });
  }, []);

  const description = describeCron(expression);
  if (description === expression) {
    return null;
  }

  const parts = expression.split(/\s+/);

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
      <span className="cursor-help p-0.5 rounded-md text-text-muted hover:text-primary-500 hover:bg-primary-500/10 transition-all duration-200">
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
              transform: 'translate(-50%, -100%)',
            }}
          >
            <div className="mb-2.5">
              <div className="relative bg-neutral-900 rounded-lg shadow-xl shadow-black/20 border border-border-default px-4 py-3.5 min-w-[260px]">
                {/* Arrow */}
                <div className="absolute left-1/2 -translate-x-1/2 -bottom-1.5 w-3 h-3 rotate-45 bg-neutral-900 border-r border-b border-border-default" />

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
                      <span className="text-[9px] font-bold uppercase tracking-wider text-text-secondary">
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
          document.body
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
    type: 'pause' | 'resume' | 'trigger';
    jobName: string;
    group: string;
  } | null>(null);

  const [editingJob, setEditingJob] = useState<{
    jobName: string;
    group: string;
    currentExpression: string;
  } | null>(null);
  const [cronInput, setCronInput] = useState('');

  // Execution history
  const [selectedJobs, setSelectedJobs] = useState<string[] | null>(null);
  const [historyStatusFilter, setHistoryStatusFilter] = useState('');
  const [expandedErrors, setExpandedErrors] = useState<Set<string>>(new Set());
  const [jobDropdownOpen, setJobDropdownOpen] = useState(false);
  const jobDropdownRef = useRef<HTMLDivElement>(null);

  // Initialize selected jobs: all except notificationOutboxJob and databaseMetricsRefreshJob
  const allJobs = useMemo(() => jobs ?? [], [jobs]);

  // Client-side sorting for the scheduled-jobs table (clicking a header toggles asc/desc).
  const [jobSort, setJobSort] = useState<{
    field: string;
    dir: 'asc' | 'desc';
  }>({
    field: 'jobName',
    dir: 'asc',
  });
  const handleJobSort = (field: string) =>
    setJobSort((s) =>
      s.field === field
        ? { field, dir: s.dir === 'asc' ? 'desc' : 'asc' }
        : { field, dir: 'asc' }
    );
  const sortedJobs = useMemo(() => {
    const value = (job: (typeof allJobs)[number], field: string): string => {
      switch (field) {
        case 'jobGroup':
          return job.jobGroup ?? '';
        case 'triggerType':
          return job.triggerType ?? '';
        case 'scheduleExpression':
          return job.scheduleExpression ?? '';
        case 'triggerState':
          return job.triggerState ?? '';
        case 'nextFireTime':
          return job.nextFireTime ?? '';
        default:
          return job.jobName ?? '';
      }
    };
    return [...allJobs].sort((a, b) => {
      const cmp = value(a, jobSort.field).localeCompare(
        value(b, jobSort.field),
        undefined,
        {
          numeric: true,
        }
      );
      return jobSort.dir === 'asc' ? cmp : -cmp;
    });
  }, [allJobs, jobSort]);

  // Group the (already sorted) jobs by their Quartz group so the table mirrors
  // the structure that naturally exists in the data. Groups are ordered by their
  // human label; jobs inside each group keep the active sort.
  const groupedJobs = useMemo(() => {
    const map = new Map<string, typeof sortedJobs>();
    for (const job of sortedJobs) {
      const key = job.jobGroup ?? '';
      const bucket = map.get(key);
      if (bucket) {
        bucket.push(job);
      } else {
        map.set(key, [job]);
      }
    }
    return [...map.entries()].sort((a, b) =>
      groupLabel(a[0]).localeCompare(groupLabel(b[0]))
    );
  }, [sortedJobs]);

  const uniqueJobNames = [...new Set(allJobs.map((j) => j.jobName))];
  const defaultSelectedJobs = uniqueJobNames.filter(
    (name) =>
      name !== 'notificationOutboxJob' &&
      name !== 'databaseMetricsRefreshJob' &&
      name !== 'thumbnailBackfillJob' &&
      name !== 'impersonationSessionCleanupJob'
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
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
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
  } = usePagination({ defaultSort: 'startedAt' });

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
    if (!confirmAction) {
      return;
    }
    const { type, jobName, group } = confirmAction;
    const mutation =
      type === 'pause' ? pauseJob : type === 'resume' ? resumeJob : triggerJob;
    mutation.mutate(
      { jobName, group },
      { onSuccess: () => setConfirmAction(null) }
    );
  };

  const confirmLabels = {
    pause: {
      title: 'Pause Job',
      message: 'This will pause the job. It will not execute until resumed.',
      confirmLabel: 'Pause',
      variant: 'danger' as const,
    },
    resume: {
      title: 'Resume Job',
      message:
        'This will resume the paused job. It will execute on its next scheduled time.',
      confirmLabel: 'Resume',
      variant: 'default' as const,
    },
    trigger: {
      title: 'Run Job Now',
      message:
        'This will immediately run the job outside of its normal schedule.',
      confirmLabel: 'Run now',
      variant: 'default' as const,
    },
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading scheduler..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load scheduler data.</p>
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
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Scheduler</h1>
          <p className="text-sm text-text-secondary mt-1">
            View and manage Quartz scheduled jobs across the platform.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-surface-card rounded-lg border border-border-default p-4">
          <div
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            className="text-text-secondary text-sm mb-1"
          >
            <Timer className="h-4 w-4" />
            Total Jobs
          </div>
          <div className="text-2xl font-bold text-text-primary">
            {allJobs.length}
          </div>
        </div>
        <div className="bg-surface-card rounded-lg border border-border-default p-4">
          <div
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            className="text-text-secondary text-sm mb-1"
          >
            <Play className="h-4 w-4 text-success-text" />
            Active
          </div>
          <div className="text-2xl font-bold text-success-text">
            {allJobs.filter((j) => j.triggerState === 'NORMAL').length}
          </div>
        </div>
        <div className="bg-surface-card rounded-lg border border-border-default p-4">
          <div
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            className="text-text-secondary text-sm mb-1"
          >
            <Pause className="h-4 w-4 text-warning-text" />
            Paused
          </div>
          <div className="text-2xl font-bold text-warning-text">
            {allJobs.filter((j) => j.triggerState === 'PAUSED').length}
          </div>
        </div>
        <div className="bg-surface-card rounded-lg border border-border-default p-4">
          <div
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            className="text-text-secondary text-sm mb-1"
          >
            <Clock className="h-4 w-4 text-info-text" />
            Blocked
          </div>
          <div className="text-2xl font-bold text-info-text">
            {allJobs.filter((j) => j.triggerState === 'BLOCKED').length}
          </div>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden mb-8">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
                <SortableHeader
                  field="jobName"
                  label="Job"
                  sort={jobSort.field}
                  direction={jobSort.dir}
                  onSortChange={handleJobSort}
                />
                <SortableHeader
                  field="triggerType"
                  label="Type"
                  sort={jobSort.field}
                  direction={jobSort.dir}
                  onSortChange={handleJobSort}
                />
                <SortableHeader
                  field="scheduleExpression"
                  label="Schedule"
                  sort={jobSort.field}
                  direction={jobSort.dir}
                  onSortChange={handleJobSort}
                />
                <SortableHeader
                  field="triggerState"
                  label="Status"
                  sort={jobSort.field}
                  direction={jobSort.dir}
                  onSortChange={handleJobSort}
                />
                <SortableHeader
                  field="nextFireTime"
                  label="Next Fire"
                  sort={jobSort.field}
                  direction={jobSort.dir}
                  onSortChange={handleJobSort}
                />
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {allJobs.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-4 py-12 text-center text-sm text-text-muted"
                  >
                    No scheduled jobs found.
                  </td>
                </tr>
              ) : (
                groupedJobs.map(([group, groupJobs]) => (
                  <Fragment key={group}>
                    <tr className="bg-surface-page/60 border-b border-border-default">
                      <td colSpan={6} className="px-4 py-2.5">
                        <div className="flex items-center gap-2">
                          <Layers className="h-4 w-4 text-text-muted shrink-0" />
                          <span className="text-sm font-semibold text-text-primary">
                            {groupLabel(group)}
                          </span>
                          <span className="inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1.5 rounded-full bg-surface-inset text-xs font-medium text-text-secondary">
                            {groupJobs.length}
                          </span>
                          {groupDescription(group) && (
                            <span className="text-xs text-text-muted truncate">
                              {groupDescription(group)}
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                    {groupJobs.map((job) => (
                      <tr
                        key={`${job.jobGroup}.${job.jobName}.${job.triggerName}`}
                        className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors"
                      >
                        <td className="px-4 py-3 pl-10">
                          <div className="flex flex-col">
                            <span
                              className="text-sm font-medium text-text-primary"
                              title={jobDescription(job.jobName)}
                            >
                              {jobLabel(job.jobName)}
                            </span>
                            <span className="text-[11px] font-mono text-text-muted/50 underline decoration-dotted decoration-text-muted/25 underline-offset-2">
                              {job.jobName}
                            </span>
                          </div>
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
                            <span className="text-sm text-text-secondary font-mono">
                              {job.scheduleExpression ?? '-'}
                            </span>
                            {job.triggerType === 'cron' &&
                              job.scheduleExpression && (
                                <CronTooltip
                                  expression={job.scheduleExpression}
                                />
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
                          <span className="text-sm text-text-secondary whitespace-nowrap">
                            {formatFireTime(job.nextFireTime)}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'flex-end',
                              gap: '0.25rem',
                            }}
                          >
                            {job.triggerState === 'PAUSED' ? (
                              <button
                                onClick={() =>
                                  setConfirmAction({
                                    type: 'resume',
                                    jobName: job.jobName,
                                    group: job.jobGroup,
                                  })
                                }
                                className="p-2 rounded-lg text-text-secondary hover:text-success-text hover:bg-surface-inset transition-colors"
                                title="Resume job"
                              >
                                <Play className="h-4 w-4" />
                              </button>
                            ) : (
                              <button
                                onClick={() =>
                                  setConfirmAction({
                                    type: 'pause',
                                    jobName: job.jobName,
                                    group: job.jobGroup,
                                  })
                                }
                                className="p-2 rounded-lg text-text-secondary hover:text-warning-text hover:bg-surface-inset transition-colors"
                                title="Pause job"
                              >
                                <Pause className="h-4 w-4" />
                              </button>
                            )}
                            <button
                              onClick={() =>
                                setConfirmAction({
                                  type: 'trigger',
                                  jobName: job.jobName,
                                  group: job.jobGroup,
                                })
                              }
                              className="p-2 rounded-lg text-text-secondary hover:text-primary-500 hover:bg-surface-inset transition-colors"
                              title="Run now"
                            >
                              <PlayCircle className="h-4 w-4" />
                            </button>
                            {job.triggerType === 'cron' && (
                              <button
                                onClick={() => {
                                  setEditingJob({
                                    jobName: job.jobName,
                                    group: job.jobGroup,
                                    currentExpression:
                                      job.scheduleExpression ?? '',
                                  });
                                  setCronInput(job.scheduleExpression ?? '');
                                }}
                                className="p-2 rounded-lg text-text-secondary hover:text-primary-500 hover:bg-surface-inset transition-colors"
                                title="Edit schedule"
                              >
                                <Pencil className="h-4 w-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </Fragment>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Execution History Section */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-text-primary">
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
                  ? 'All Jobs'
                  : activeSelectedJobs.length === 0
                    ? 'No Jobs'
                    : `${activeSelectedJobs.length} job${activeSelectedJobs.length > 1 ? 's' : ''} selected`}
              </span>
              <ChevronDown className="h-4 w-4 shrink-0" />
            </button>
            {jobDropdownOpen && (
              <div className="absolute z-50 mt-1 w-64 bg-surface-card border border-border-default rounded-lg shadow-lg max-h-64 overflow-y-auto">
                <button
                  onClick={toggleAllJobs}
                  className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-surface-inset transition-colors border-b border-border-default"
                >
                  <span
                    className={`flex items-center justify-center h-4 w-4 rounded border ${activeSelectedJobs.length === uniqueJobNames.length ? 'bg-primary-500 border-primary-500 text-white' : 'border-border-strong'}`}
                  >
                    {activeSelectedJobs.length === uniqueJobNames.length && (
                      <Check className="h-3 w-3" />
                    )}
                  </span>
                  <span className="text-text-primary font-medium">
                    Select All
                  </span>
                </button>
                {uniqueJobNames.map((name) => (
                  <button
                    key={name}
                    onClick={() => toggleJob(name)}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-surface-inset transition-colors"
                  >
                    <span
                      className={`flex items-center justify-center h-4 w-4 rounded border ${activeSelectedJobs.includes(name) ? 'bg-primary-500 border-primary-500 text-white' : 'border-border-strong'}`}
                    >
                      {activeSelectedJobs.includes(name) && (
                        <Check className="h-3 w-3" />
                      )}
                    </span>
                    <span className="flex flex-col items-start min-w-0">
                      <span className="text-text-primary truncate">
                        {jobLabel(name)}
                      </span>
                      <span className="text-[11px] font-mono text-text-muted truncate">
                        {name}
                      </span>
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

      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
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
                <th className={`${thClass} w-[1%]`} />
              </tr>
            </thead>
            <tbody>
              {historyRecords.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-4 py-12 text-center text-sm text-text-muted"
                  >
                    No execution history found.
                  </td>
                </tr>
              ) : (
                historyRecords.map((exec) => {
                  const isExpanded = expandedErrors.has(exec.id);
                  const hasError = !!exec.errorMessage;
                  return (
                    <Fragment key={exec.id}>
                      <tr
                        className={`border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors ${hasError ? 'cursor-pointer' : ''} ${isExpanded ? '!border-b-0' : ''}`}
                        onClick={
                          hasError
                            ? () => {
                                setExpandedErrors((prev) => {
                                  const next = new Set(prev);
                                  if (next.has(exec.id)) {
                                    next.delete(exec.id);
                                  } else {
                                    next.add(exec.id);
                                  }
                                  return next;
                                });
                              }
                            : undefined
                        }
                      >
                        <td className="px-4 py-3">
                          <div className="flex flex-col">
                            <span
                              className="text-sm font-medium text-text-primary"
                              title={jobDescription(exec.jobName)}
                            >
                              {jobLabel(exec.jobName)}
                            </span>
                            <span className="text-[11px] font-mono text-text-muted/50">
                              {exec.jobName}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-text-secondary">
                            {groupLabel(exec.jobGroup)}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-text-secondary whitespace-nowrap">
                            {formatDateTimeFull(exec.startedAt)}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-text-secondary font-mono">
                            {formatDuration(exec.durationMs)}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge
                            status={exec.status}
                            config={execStatusBadgeConfig}
                          />
                        </td>
                        <td className="px-4 py-3">
                          {hasError && (
                            <div className="flex items-center gap-1.5 text-error-text">
                              <AlertCircle className="h-4 w-4 shrink-0" />
                              {isExpanded ? (
                                <ChevronDown className="h-3.5 w-3.5 shrink-0" />
                              ) : (
                                <ChevronRight className="h-3.5 w-3.5 shrink-0" />
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                      {isExpanded && (
                        <tr className="border-b border-border-default last:border-b-0">
                          <td colSpan={6} className="px-4 pb-4 pt-0">
                            <div className="rounded-lg border border-error-border bg-error-bg p-4">
                              <pre className="text-xs font-mono text-error-text whitespace-pre-wrap break-words leading-relaxed">
                                {exec.errorMessage}
                              </pre>
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })
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
          <div className="bg-surface-card rounded-lg border border-border-default shadow-2xl w-full max-w-lg mx-4">
            <div className="flex items-center justify-between p-5 border-b border-border-default">
              <div>
                <h3 className="text-lg font-semibold text-text-primary">
                  Edit Schedule
                </h3>
                <p className="text-sm text-text-secondary mt-0.5">
                  {editingJob.jobName}
                </p>
              </div>
              <button
                onClick={() => setEditingJob(null)}
                className="p-1.5 rounded-lg text-text-secondary hover:bg-surface-inset transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1.5">
                  Cron Expression
                </label>
                <input
                  type="text"
                  value={cronInput}
                  onChange={(e) => setCronInput(e.target.value)}
                  placeholder="0 0 * * * ?"
                  className="w-full px-3 py-2.5 text-sm font-mono border border-border-strong rounded-lg bg-surface-card text-text-primary outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                />
              </div>

              {/* Cron Explanation Panel */}
              <div className="rounded-lg bg-surface-page border border-border-default p-4">
                <div className="text-xs font-semibold uppercase tracking-wider text-text-secondary mb-2">
                  Explanation
                </div>
                <p className="text-sm text-text-primary">
                  {cronInput.trim()
                    ? describeCron(cronInput.trim())
                    : 'Enter a cron expression above'}
                </p>
                {editingJob.currentExpression !== cronInput.trim() &&
                  cronInput.trim() && (
                    <p className="text-xs text-text-muted mt-2">
                      Current:{' '}
                      <span className="font-mono">
                        {editingJob.currentExpression}
                      </span>{' '}
                      ({describeCron(editingJob.currentExpression)})
                    </p>
                  )}
              </div>

              <div className="rounded-lg bg-surface-page border border-border-default p-4">
                <div className="text-xs font-semibold uppercase tracking-wider text-text-secondary mb-2">
                  Quartz Cron Format
                </div>
                <div className="grid grid-cols-7 gap-1 text-center text-[11px]">
                  {['SEC', 'MIN', 'HOUR', 'DAY', 'MON', 'DOW', 'YEAR'].map(
                    (f) => (
                      <span key={f} className="font-mono text-text-secondary">
                        {f}
                      </span>
                    )
                  )}
                </div>
                <div className="grid grid-cols-7 gap-1 text-center text-[11px] mt-1">
                  {(cronInput.trim() || '* * * * * ? *')
                    .split(/\s+/)
                    .slice(0, 7)
                    .map((part, i) => (
                      <span
                        key={i}
                        className="font-mono font-medium text-text-primary bg-surface-card rounded px-1 py-0.5 border border-border-default"
                      >
                        {part}
                      </span>
                    ))}
                </div>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 p-5 border-t border-border-default">
              <button
                onClick={() => setEditingJob(null)}
                className="px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary transition-colors"
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
                    { onSuccess: () => setEditingJob(null) }
                  );
                }}
                disabled={
                  rescheduleJob.isPending ||
                  !cronInput.trim() ||
                  cronInput.trim() === editingJob.currentExpression
                }
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 text-sm font-medium"
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
