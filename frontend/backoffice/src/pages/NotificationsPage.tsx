import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search, Eye, RefreshCw, Mail, Phone } from 'lucide-react';
import { formatDateTime } from '../utils/dateFormatting';
import type { NotificationStats } from '../types';
import { Pagination, ConfirmDialog, RefreshButton } from '@buurman/ui';
import { SortableHeader } from '../components/SortableHeader';
import {
  useNotifications,
  useNotificationStats,
  useResendNotification,
} from '../hooks/useNotifications';
import { usePagination } from '../hooks/usePagination';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { AsyncSelect, type AsyncSelectOption } from '../components/AsyncSelect';
import { useTeamSearch } from '../hooks/useTeams';

const NOTIFICATION_TYPES = [
  'WELCOME',
  'VERIFICATION_CODE',
  'TEAM_INVITATION',
  'INVITATION_ACCEPTED',
  'PASSWORD_CHANGED',
  'PAYMENT_REMINDER',
  'CONTRACT_EXPIRY',
  'PROPERTY_CREATED',
  'CONTRACT_CREATED',
  'CONTRACT_STATUS_CHANGED',
  'CONTRACT_REOPENED',
  'PAYMENT_PAID',
  'PAYMENT_RECEIVAL',
  'EXPENSE_CREATED',
];

const CHANNELS = ['EMAIL', 'SMS'];

const STATUSES = [
  'PENDING',
  'QUEUED',
  'SENT',
  'DELIVERED',
  'FAILED',
  'BOUNCED',
  'REJECTED',
];

const typeLabels: Record<string, string> = {
  WELCOME: 'Welcome',
  VERIFICATION_CODE: 'Verification',
  TEAM_INVITATION: 'Invitation',
  INVITATION_ACCEPTED: 'Accepted',
  PASSWORD_CHANGED: 'Password',
  PAYMENT_REMINDER: 'Payment',
  CONTRACT_EXPIRY: 'Contract',
  PROPERTY_CREATED: 'Property',
  CONTRACT_CREATED: 'New Contract',
  CONTRACT_STATUS_CHANGED: 'Status Change',
  CONTRACT_REOPENED: 'Reopened',
  PAYMENT_PAID: 'Paid',
  PAYMENT_RECEIVAL: 'Receival',
  EXPENSE_CREATED: 'Expense',
};

const statusBadgeConfig: Record<string, { label: string; className: string }> =
  {
    PENDING: {
      label: 'Pending',
      className: 'bg-slate-50 text-slate-700 ring-1 ring-slate-200',
    },
    QUEUED: {
      label: 'Queued',
      className: 'bg-blue-50 text-blue-700 ring-1 ring-blue-200',
    },
    SENT: {
      label: 'Sent',
      className: 'bg-sky-50 text-sky-700 ring-1 ring-sky-200',
    },
    DELIVERED: {
      label: 'Delivered',
      className: 'bg-success-bg text-success-text ring-1 ring-success-border',
    },
    FAILED: {
      label: 'Failed',
      className: 'bg-error-bg text-error-text ring-1 ring-error-border',
    },
    BOUNCED: {
      label: 'Bounced',
      className: 'bg-warning-bg text-warning-text ring-1 ring-warning-border',
    },
    REJECTED: {
      label: 'Rejected',
      className: 'bg-error-bg text-error-text ring-1 ring-error-border',
    },
  };

const StatusBadge = ({ status }: { status: string }) => {
  const config = statusBadgeConfig[status] ?? {
    label: status,
    className: 'bg-slate-50 text-slate-700 ring-1 ring-slate-200',
  };
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
};

/** Solid colour per delivery state (matches the badge hues), used by the hero bar + dots. */
const STATUS_COLOR: Record<string, string> = {
  PENDING: '#64748b',
  QUEUED: '#3b82f6',
  SENT: '#0ea5e9',
  DELIVERED: '#059669',
  FAILED: '#dc2626',
  BOUNCED: '#d97706',
  REJECTED: '#e11d48',
};

/**
 * Hero bar: total notifications + a proportional stacked bar and a count per delivery state. Each
 * state is a button that filters the table to that status (click the active one again to clear).
 */
const NotificationStatsHero = ({
  stats,
  activeStatus,
  onSelectStatus,
}: {
  stats: NotificationStats;
  activeStatus: string;
  onSelectStatus: (status: string) => void;
}) => {
  const total = stats.total ?? 0;
  const states = STATUSES.map((status) => ({
    status,
    label: statusBadgeConfig[status]?.label ?? status,
    color: STATUS_COLOR[status] ?? '#64748b',
    count: stats.byStatus?.[status] ?? 0,
  }));
  const delivered = stats.byStatus?.DELIVERED ?? 0;
  const deliveredPct = total > 0 ? Math.round((delivered / total) * 100) : 0;

  return (
    <div className="mb-6 rounded-xl border border-border-default bg-surface-card p-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
            Total notifications
          </p>
          <p className="text-3xl font-bold leading-none text-text-primary tabular-nums">
            {total.toLocaleString()}
          </p>
        </div>
        <p className="text-sm text-text-secondary tabular-nums">
          <span className="font-semibold text-success-text">
            {deliveredPct}%
          </span>{' '}
          delivered
        </p>
      </div>

      {/* Proportional stacked bar */}
      <div
        className="mt-4 flex h-2.5 w-full overflow-hidden rounded-full bg-surface-page"
        role="img"
        aria-label={`Notifications by state: ${states
          .map((s) => `${s.label} ${s.count}`)
          .join(', ')}`}
      >
        {total > 0 &&
          states
            .filter((s) => s.count > 0)
            .map((s) => (
              <div
                key={s.status}
                style={{
                  width: `${(s.count / total) * 100}%`,
                  background: s.color,
                }}
                title={`${s.label}: ${s.count.toLocaleString()}`}
              />
            ))}
      </div>

      {/* Per-state counts (click to filter) */}
      <div className="mt-4 flex flex-wrap gap-2">
        {states.map((s) => {
          const active = activeStatus === s.status;
          return (
            <button
              key={s.status}
              type="button"
              onClick={() => onSelectStatus(s.status)}
              aria-pressed={active}
              className={`focus-ring inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-sm transition-colors ${
                active
                  ? 'border-primary-400 bg-primary-50'
                  : 'border-border-default hover:bg-surface-page'
              }`}
            >
              <span
                className="h-2 w-2 rounded-full"
                style={{ background: s.color }}
                aria-hidden="true"
              />
              <span className="text-text-secondary">{s.label}</span>
              <span className="font-semibold text-text-primary tabular-nums">
                {s.count.toLocaleString()}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
};

const selectClass =
  'px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';

const inputClass =
  'px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary placeholder-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';

export const NotificationsPage = () => {
  const navigate = useNavigate();
  const {
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
  } = usePagination({ defaultSort: 'createdAt' });

  const [searchParams] = useSearchParams();
  const [recipientEmail, setRecipientEmail] = useState('');
  const [recipientInput, setRecipientInput] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [channelFilter, setChannelFilter] = useState('');
  // Seed from a deep link (e.g. dashboard action queue -> /notifications?status=FAILED).
  const [statusFilter, setStatusFilter] = useState(
    () => searchParams.get('status') ?? ''
  );
  const teamSearch = useTeamSearch();
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [selectedTeam, setSelectedTeam] = useState<AsyncSelectOption[]>(() => {
    const initialTeamId = searchParams.get('teamIdentifier');
    if (!initialTeamId) {
      return [];
    }
    return [{ value: initialTeamId, label: initialTeamId }];
  });
  const [resendTarget, setResendTarget] = useState<string | null>(null);

  const { data, isLoading, isFetching, error, refetch } = useNotifications({
    page,
    size,
    recipientEmail: recipientEmail || undefined,
    type: typeFilter || undefined,
    channel: channelFilter || undefined,
    status: statusFilter || undefined,
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
    teamIdentifier: selectedTeam.length > 0 ? selectedTeam[0].value : undefined,
    sort,
    direction,
  });
  const { data: stats } = useNotificationStats();
  const resendMutation = useResendNotification();

  const handleRecipientSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setRecipientEmail(recipientInput);
    handlePageChange(0);
  };

  const handleResend = () => {
    if (!resendTarget) {
      return;
    }
    resendMutation.mutate(resendTarget, {
      onSuccess: () => setResendTarget(null),
    });
  };

  const canResend = (status: string) =>
    ['FAILED', 'BOUNCED', 'REJECTED'].includes(status);

  if (isLoading) {
    return <LoadingSpinner message="Loading notifications..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load notifications.</p>
      </div>
    );
  }

  const notifications = data?.content ?? [];

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
          <h1 className="text-2xl font-bold text-text-primary">
            Notifications
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            View and manage all notifications across the platform.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stats hero — count per delivery state, click a state to filter the table */}
      {stats && (
        <NotificationStatsHero
          stats={stats}
          activeStatus={statusFilter}
          onSelectStatus={(s) => {
            setStatusFilter((prev) => (prev === s ? '' : s));
            handlePageChange(0);
          }}
        />
      )}

      {/* Filters */}
      <form onSubmit={handleRecipientSearch} className="mb-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
            <input
              type="search"
              placeholder="Recipient email..."
              value={recipientInput}
              onChange={(e) => setRecipientInput(e.target.value)}
              className={`${inputClass} pl-10 w-56`}
            />
          </div>
          <select
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All Types</option>
            {NOTIFICATION_TYPES.map((t) => (
              <option key={t} value={t}>
                {typeLabels[t] || t}
              </option>
            ))}
          </select>
          <select
            value={channelFilter}
            onChange={(e) => {
              setChannelFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All Channels</option>
            {CHANNELS.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All Statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => {
              setDateFrom(e.target.value);
              handlePageChange(0);
            }}
            placeholder="From"
            className={inputClass}
          />
          <input
            type="date"
            value={dateTo}
            onChange={(e) => {
              setDateTo(e.target.value);
              handlePageChange(0);
            }}
            placeholder="To"
            className={inputClass}
          />
          <AsyncSelect
            selected={selectedTeam}
            onSelect={(options) => {
              setSelectedTeam(options);
              handlePageChange(0);
            }}
            search={teamSearch}
            placeholder="Search teams..."
            className="w-64"
          />
          <button
            type="submit"
            className="px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-lg transition-colors"
          >
            Search
          </button>
        </div>
      </form>

      {/* Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
                <SortableHeader
                  field="notificationType"
                  label="Type"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Channel
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Recipient
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Team
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Subject
                </th>
                <SortableHeader
                  field="status"
                  label="Status"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <SortableHeader
                  field="createdAt"
                  label="Created"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {notifications.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-sm text-text-muted"
                  >
                    No notifications found.
                  </td>
                </tr>
              ) : (
                notifications.map((notif) => (
                  <tr
                    key={notif.identifier}
                    className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-text-primary">
                        {typeLabels[notif.notificationType] ??
                          notif.notificationType}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center gap-1 text-sm text-text-secondary">
                        {notif.channel === 'EMAIL' ? (
                          <Mail className="h-3.5 w-3.5" />
                        ) : (
                          <Phone className="h-3.5 w-3.5" />
                        )}
                        {notif.channel}
                      </span>
                    </td>
                    <td className="px-4 py-3 max-w-[200px]">
                      <span className="text-sm text-text-secondary truncate block">
                        {notif.channel === 'SMS'
                          ? notif.recipientPhone || notif.recipientEmail || '-'
                          : notif.recipientEmail || '-'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className="text-sm text-text-secondary"
                        title={notif.teamIdentifier}
                      >
                        {notif.teamName}
                      </span>
                    </td>
                    <td className="px-4 py-3 max-w-[200px]">
                      <span className="text-sm text-text-secondary truncate block">
                        {notif.subject || '-'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={notif.status} />
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary whitespace-nowrap">
                        {formatDateTime(notif.createdAt)}
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
                        <button
                          onClick={() =>
                            navigate(`/notifications/${notif.identifier}`)
                          }
                          className="p-2 rounded-lg text-text-secondary hover:text-primary-500 hover:bg-surface-inset transition-colors"
                          title="View notification"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {canResend(notif.status) && (
                          <button
                            onClick={() => setResendTarget(notif.identifier)}
                            className="p-2 rounded-lg text-text-secondary hover:text-primary-500 hover:bg-surface-inset transition-colors"
                            title="Resend notification"
                          >
                            <RefreshCw className="h-4 w-4" />
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

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="mt-4">
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            onPageChange={handlePageChange}
            onSizeChange={handleSizeChange}
          />
        </div>
      )}

      {/* Resend confirmation dialog */}
      {resendTarget && (
        <ConfirmDialog
          title="Resend Notification"
          message="This will create a new notification and attempt delivery again. The original notification will remain in the log."
          confirmLabel="Resend"
          cancelLabel="Cancel"
          variant="default"
          isLoading={resendMutation.isPending}
          onConfirm={handleResend}
          onCancel={() => setResendTarget(null)}
        />
      )}
    </div>
  );
};
