import { Fragment, useState, useCallback } from 'react';
import { formatDateTime, formatDateTimeFull } from '../utils/dateFormatting';
import { ChevronDown, ChevronRight, Clock, ExternalLink } from 'lucide-react';
import {
  PageHeader,
  Button,
  StatusBadge,
  Pagination,
  RefreshButton,
  RichTextDisplay,
} from '@buurman/ui';
import type { BadgeColorVariant } from '@buurman/ui';
import {
  useImpersonationSessions,
  useRejoinImpersonation,
  useTerminateImpersonation,
} from '../hooks/useImpersonation';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { PasswordConfirmationDialog } from '../components/PasswordConfirmationDialog';
import { SortableHeader } from '../components/SortableHeader';
import { usePagination } from '../hooks/usePagination';
import { AsyncSelect, type AsyncSelectOption } from '../components/AsyncSelect';
import { useTeamSearch } from '../hooks/useTeams';
import { useUserSearch } from '../hooks/useUsers';

const statusBadgeColor: Record<string, BadgeColorVariant> = {
  PENDING: 'yellow',
  ACTIVE: 'green',
  ENDED: 'gray',
  EXPIRED: 'red',
};

const selectClass =
  'px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';

const ExpandableReason = ({ html }: { html: string }) => {
  const [expanded, setExpanded] = useState(false);
  const plainText = html.replace(/<[^>]*>/g, '');
  const isLong = plainText.length > 80;

  if (!html || plainText.trim().length === 0) {
    return <span className="text-text-muted">-</span>;
  }

  if (!isLong) {
    return <RichTextDisplay content={html} className="text-sm" />;
  }

  return (
    <div>
      {expanded ? (
        <>
          <RichTextDisplay content={html} className="text-sm" />
          <button
            onClick={() => setExpanded(false)}
            className="text-xs text-primary-500 hover:underline mt-1"
          >
            Show less
          </button>
        </>
      ) : (
        <>
          <span className="text-sm text-text-secondary">
            {plainText.substring(0, 80)}...
          </span>
          <button
            onClick={() => setExpanded(true)}
            className="text-xs text-primary-500 hover:underline ml-1"
          >
            Show more
          </button>
        </>
      )}
    </div>
  );
};

const SessionDetailPanel = ({
  session,
}: {
  session: {
    identifier: string;
    adminName: string;
    targetUserIdentifier: string;
    targetTeamIdentifier: string;
    reason: string;
    createdAt: string;
    activatedAt?: string;
    expiresAt: string;
    endedAt?: string;
    endReason?: string;
  };
}) => {
  const details = [
    { label: 'Session ID', value: session.identifier },
    { label: 'Admin Name', value: session.adminName },
    { label: 'Target User ID', value: session.targetUserIdentifier },
    { label: 'Target Team ID', value: session.targetTeamIdentifier },
    {
      label: 'Created',
      value: formatDateTimeFull(session.createdAt),
    },
    {
      label: 'Activated',
      value: session.activatedAt
        ? formatDateTimeFull(session.activatedAt)
        : '-',
    },
    {
      label: 'Expires',
      value: formatDateTimeFull(session.expiresAt),
    },
    {
      label: 'Ended',
      value: session.endedAt ? formatDateTimeFull(session.endedAt) : '-',
    },
    { label: 'End Reason', value: session.endReason ?? '-' },
  ];

  return (
    <div className="px-4 py-3 bg-surface-page border-t border-border-default">
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
        {details.map((d) => (
          <div key={d.label}>
            <span className="text-[10px] font-bold uppercase tracking-widest text-text-muted">
              {d.label}
            </span>
            <p className="text-sm text-text-primary font-mono break-all">
              {d.value}
            </p>
          </div>
        ))}
      </div>
      {session.reason && (
        <div className="mt-3">
          <span className="text-[10px] font-bold uppercase tracking-widest text-text-muted">
            Full Reason
          </span>
          <RichTextDisplay content={session.reason} className="text-sm mt-1" />
        </div>
      )}
    </div>
  );
};

export const ImpersonationSessionsPage = () => {
  const {
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
  } = usePagination({ defaultSort: 'createdAt' });

  const teamSearch = useTeamSearch();
  const userSearch = useUserSearch();

  const [selectedAdmin, setSelectedAdmin] = useState<AsyncSelectOption[]>([]);
  const [selectedTeam, setSelectedTeam] = useState<AsyncSelectOption[]>([]);
  const [selectedTargetUser, setSelectedTargetUser] = useState<
    AsyncSelectOption[]
  >([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [modeFilter, setModeFilter] = useState('');
  const [expandedRow, setExpandedRow] = useState<string | null>(null);
  const [rejoinTarget, setRejoinTarget] = useState<string | null>(null);
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const { data, isLoading, isFetching, refetch } = useImpersonationSessions({
    page,
    size,
    sort,
    direction,
    adminEmail: selectedAdmin.length > 0 ? selectedAdmin[0].label : undefined,
    teamIdentifier: selectedTeam.length > 0 ? selectedTeam[0].value : undefined,
    targetUserEmail:
      selectedTargetUser.length > 0 ? selectedTargetUser[0].label : undefined,
    status: statusFilter || undefined,
    mode: modeFilter || undefined,
  });

  const terminateMutation = useTerminateImpersonation();
  const rejoinMutation = useRejoinImpersonation();

  const handleRejoinClick = useCallback((sessionIdentifier: string) => {
    setRejoinTarget(sessionIdentifier);
    setPasswordError(null);
    setShowPasswordDialog(true);
  }, []);

  const handlePasswordConfirm = useCallback(
    (password: string) => {
      if (!rejoinTarget) {
        return;
      }

      const newWindow = window.open('about:blank', '_blank');

      rejoinMutation.mutate(
        { identifier: rejoinTarget, password },
        {
          onSuccess: (data) => {
            setShowPasswordDialog(false);
            setRejoinTarget(null);
            if (newWindow && !newWindow.closed) {
              newWindow.location.href = data.redirectUrl;
            } else {
              window.open(data.redirectUrl, '_blank');
            }
          },
          onError: (err) => {
            newWindow?.close();
            const axiosError = err as { response?: { status?: number } };
            if (axiosError.response?.status === 403) {
              setPasswordError('Invalid password. Please try again.');
            } else {
              setPasswordError('Failed to rejoin impersonation session.');
            }
          },
        }
      );
    },
    [rejoinTarget, rejoinMutation]
  );

  if (isLoading) {
    return <LoadingSpinner message="Loading sessions..." />;
  }

  const sessions = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Impersonation Sessions"
        subtitle="View and manage user impersonation sessions"
        backTo="/dashboard"
        actions={
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
        }
      />

      {/* Filters */}
      <div className="mb-4 flex flex-wrap gap-3 items-end">
        <div>
          <label className="block text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
            Admin
          </label>
          <AsyncSelect
            selected={selectedAdmin}
            onSelect={(options) => {
              setSelectedAdmin(options);
              handlePageChange(0);
            }}
            search={userSearch}
            placeholder="Search admin..."
            className="w-56"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
            Team
          </label>
          <AsyncSelect
            selected={selectedTeam}
            onSelect={(options) => {
              setSelectedTeam(options);
              handlePageChange(0);
            }}
            search={teamSearch}
            placeholder="Search team..."
            className="w-56"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
            Target User
          </label>
          <AsyncSelect
            selected={selectedTargetUser}
            onSelect={(options) => {
              setSelectedTargetUser(options);
              handlePageChange(0);
            }}
            search={userSearch}
            placeholder="Search user..."
            className="w-56"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
            Status
          </label>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="ACTIVE">Active</option>
            <option value="ENDED">Ended</option>
            <option value="EXPIRED">Expired</option>
          </select>
        </div>
        <div>
          <label className="block text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
            Mode
          </label>
          <select
            value={modeFilter}
            onChange={(e) => {
              setModeFilter(e.target.value);
              handlePageChange(0);
            }}
            className={selectClass}
          >
            <option value="">All</option>
            <option value="FULL">Full</option>
            <option value="READ_ONLY">Read Only</option>
          </select>
        </div>
      </div>

      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <table className="min-w-full divide-y divide-border-default">
          <thead className="bg-surface-secondary">
            <tr>
              <th className="w-8 px-2 py-3" />
              <SortableHeader
                field="adminEmail"
                label="Admin"
                sort={sort}
                direction={direction}
                onSortChange={handleSortChange}
              />
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Target User
              </th>
              <SortableHeader
                field="mode"
                label="Mode"
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
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Reason
              </th>
              <SortableHeader
                field="createdAt"
                label="Created"
                sort={sort}
                direction={direction}
                onSortChange={handleSortChange}
              />
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border-default">
            {sessions.map((session) => {
              const isExpanded = expandedRow === session.identifier;
              return (
                <Fragment key={session.identifier}>
                  <tr
                    className="hover:bg-surface-inset/50 cursor-pointer transition-colors"
                    onClick={() =>
                      setExpandedRow(isExpanded ? null : session.identifier)
                    }
                  >
                    <td className="px-2 py-3 text-text-muted">
                      {isExpanded ? (
                        <ChevronDown className="h-4 w-4" />
                      ) : (
                        <ChevronRight className="h-4 w-4" />
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-primary">
                      {session.adminEmail}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-primary">
                      {session.targetUserEmail}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <StatusBadge
                        label={session.mode === 'FULL' ? 'Full' : 'Read Only'}
                        color={session.mode === 'FULL' ? 'red' : 'blue'}
                        size="xs"
                      />
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <StatusBadge
                        label={session.status}
                        color={statusBadgeColor[session.status] ?? 'gray'}
                        size="xs"
                        dot
                      />
                    </td>
                    <td
                      className="px-4 py-3 text-sm text-text-secondary max-w-[250px]"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <ExpandableReason html={session.reason} />
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <Clock className="h-3.5 w-3.5 text-text-muted" />
                        {formatDateTime(session.createdAt)}
                      </div>
                    </td>
                    <td
                      className="px-4 py-3 text-sm"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {(session.status === 'ACTIVE' ||
                        session.status === 'PENDING') && (
                        <div className="flex items-center gap-2">
                          <Button
                            variant="secondary"
                            size="sm"
                            leftIcon={<ExternalLink />}
                            onClick={() =>
                              handleRejoinClick(session.identifier)
                            }
                            disabled={rejoinMutation.isPending}
                          >
                            Rejoin
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() =>
                              terminateMutation.mutate(session.identifier)
                            }
                            disabled={terminateMutation.isPending}
                          >
                            Terminate
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                  {isExpanded && (
                    <tr>
                      <td colSpan={8} className="p-0">
                        <SessionDetailPanel session={session} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
            {sessions.length === 0 && (
              <tr>
                <td
                  colSpan={8}
                  className="px-4 py-8 text-center text-sm text-text-muted"
                >
                  No impersonation sessions found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
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

      {/* Password confirmation for rejoin */}
      <PasswordConfirmationDialog
        open={showPasswordDialog}
        onClose={() => {
          setShowPasswordDialog(false);
          setRejoinTarget(null);
          setPasswordError(null);
        }}
        onConfirm={handlePasswordConfirm}
        isLoading={rejoinMutation.isPending}
        error={passwordError}
        title="Confirm your identity"
        subtitle="Enter your password to rejoin the impersonation session."
      />
    </div>
  );
};
