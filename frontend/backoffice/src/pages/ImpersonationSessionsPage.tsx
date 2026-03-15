import { Fragment, useState, useMemo } from "react";
import { format } from "date-fns";
import { ChevronDown, ChevronRight, Clock, ExternalLink } from "lucide-react";
import {
  PageHeader,
  Button,
  StatusBadge,
  FilterBar,
  Pagination,
  RefreshButton,
} from "@buurman/ui";
import type { BadgeColorVariant, FilterDef } from "@buurman/ui";
import {
  useImpersonationSessions,
  useRejoinImpersonation,
  useTerminateImpersonation,
} from "../hooks/useImpersonation";
import type { ImpersonationSessionResponseStatus } from "../generated/models";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { RichTextDisplay } from "../components/RichTextDisplay";

const statusBadgeColor: Record<
  ImpersonationSessionResponseStatus,
  BadgeColorVariant
> = {
  PENDING: "yellow",
  ACTIVE: "green",
  ENDED: "gray",
  EXPIRED: "red",
};

const PAGE_SIZE = 25;

const STATUS_FILTER: FilterDef = {
  type: "toggle",
  key: "status",
  label: "Status",
  options: [
    { value: undefined, label: "All" },
    { value: "ACTIVE", label: "Active" },
    { value: "PENDING", label: "Pending" },
    { value: "ENDED", label: "Ended" },
    { value: "EXPIRED", label: "Expired" },
  ],
};

const ExpandableReason = ({ html }: { html: string }) => {
  const [expanded, setExpanded] = useState(false);
  const plainText = html.replace(/<[^>]*>/g, "");
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
    { label: "Session ID", value: session.identifier },
    { label: "Admin Name", value: session.adminName },
    { label: "Target User ID", value: session.targetUserIdentifier },
    { label: "Target Team ID", value: session.targetTeamIdentifier },
    {
      label: "Created",
      value: format(new Date(session.createdAt), "dd MMM yyyy HH:mm:ss"),
    },
    {
      label: "Activated",
      value: session.activatedAt
        ? format(new Date(session.activatedAt), "dd MMM yyyy HH:mm:ss")
        : "-",
    },
    {
      label: "Expires",
      value: format(new Date(session.expiresAt), "dd MMM yyyy HH:mm:ss"),
    },
    {
      label: "Ended",
      value: session.endedAt
        ? format(new Date(session.endedAt), "dd MMM yyyy HH:mm:ss")
        : "-",
    },
    { label: "End Reason", value: session.endReason ?? "-" },
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
    data: sessions,
    isLoading,
    isFetching,
    refetch,
  } = useImpersonationSessions();
  const terminateMutation = useTerminateImpersonation();
  const rejoinMutation = useRejoinImpersonation();
  const [filterValues, setFilterValues] = useState<
    Record<string, string | undefined>
  >({});
  const [expandedRow, setExpandedRow] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const statusFilter = filterValues.status;

  const filteredSessions = useMemo(() => {
    if (!sessions) {
      return [];
    }
    if (!statusFilter) {
      return sessions;
    }
    return sessions.filter((s) => s.status === statusFilter);
  }, [sessions, statusFilter]);

  const totalElements = filteredSessions.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
  const pagedSessions = filteredSessions.slice(
    page * PAGE_SIZE,
    (page + 1) * PAGE_SIZE,
  );

  // Reset page when filter changes
  const handleFilterChange = (values: Record<string, string | undefined>) => {
    setFilterValues(values);
    setPage(0);
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading sessions..." />;
  }

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
      <div className="mb-4">
        <FilterBar
          filters={[STATUS_FILTER]}
          values={filterValues}
          onChange={handleFilterChange}
          onReset={() => {
            setFilterValues({});
            setPage(0);
          }}
        />
      </div>

      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <table className="min-w-full divide-y divide-border-default">
          <thead className="bg-surface-secondary">
            <tr>
              <th className="w-8 px-2 py-3" />
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Admin
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Target User
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Mode
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Reason
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Created
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border-default">
            {pagedSessions.map((session) => {
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
                        label={session.mode === "FULL" ? "Full" : "Read Only"}
                        color={session.mode === "FULL" ? "red" : "blue"}
                        size="xs"
                      />
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <StatusBadge
                        label={session.status}
                        color={
                          statusBadgeColor[
                            session.status as ImpersonationSessionResponseStatus
                          ] ?? "gray"
                        }
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
                        {format(
                          new Date(session.createdAt),
                          "dd MMM yyyy HH:mm",
                        )}
                      </div>
                    </td>
                    <td
                      className="px-4 py-3 text-sm"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {(session.status === "ACTIVE" ||
                        session.status === "PENDING") && (
                        <div className="flex items-center gap-2">
                          <Button
                            variant="secondary"
                            size="sm"
                            leftIcon={<ExternalLink />}
                            onClick={() =>
                              rejoinMutation.mutate(session.identifier, {
                                onSuccess: (data) => {
                                  window.open(data.redirectUrl, "_blank");
                                },
                              })
                            }
                            disabled={rejoinMutation.isPending}
                            isLoading={rejoinMutation.isPending}
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
            {pagedSessions.length === 0 && (
              <tr>
                <td
                  colSpan={8}
                  className="px-4 py-8 text-center text-sm text-text-muted"
                >
                  {statusFilter
                    ? `No ${statusFilter.toLowerCase()} impersonation sessions found.`
                    : "No impersonation sessions found."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalElements > PAGE_SIZE && (
        <div className="mt-4">
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            size={PAGE_SIZE}
            onPageChange={setPage}
            onSizeChange={() => {
              /* fixed page size */
            }}
          />
        </div>
      )}
    </div>
  );
};
