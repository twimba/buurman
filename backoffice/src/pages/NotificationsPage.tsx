import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Search,
  Eye,
  RefreshCw,
  Mail,
  Phone,
  CheckCircle,
  Clock,
  AlertTriangle,
  ChevronUp,
  ChevronDown,
  ArrowUpDown,
} from "lucide-react";
import { format } from "date-fns";
import { Pagination, ConfirmDialog, RefreshButton } from "@buurman/ui";
import {
  useNotifications,
  useNotificationStats,
  useResendNotification,
} from "../hooks/useNotifications";
import { usePagination } from "../hooks/usePagination";
import { LoadingSpinner } from "../components/LoadingSpinner";

const NOTIFICATION_TYPES = [
  "WELCOME",
  "VERIFICATION_CODE",
  "TEAM_INVITATION",
  "INVITATION_ACCEPTED",
  "PASSWORD_CHANGED",
  "PAYMENT_REMINDER",
  "CONTRACT_EXPIRY",
  "PROPERTY_CREATED",
  "CONTRACT_CREATED",
  "CONTRACT_STATUS_CHANGED",
  "CONTRACT_REOPENED",
  "PAYMENT_PAID",
  "PAYMENT_RECEIVAL",
  "EXPENSE_CREATED",
];

const CHANNELS = ["EMAIL", "SMS"];

const STATUSES = [
  "PENDING",
  "QUEUED",
  "SENT",
  "DELIVERED",
  "FAILED",
  "BOUNCED",
  "REJECTED",
];

const typeLabels: Record<string, string> = {
  WELCOME: "Welcome",
  VERIFICATION_CODE: "Verification",
  TEAM_INVITATION: "Invitation",
  INVITATION_ACCEPTED: "Accepted",
  PASSWORD_CHANGED: "Password",
  PAYMENT_REMINDER: "Payment",
  CONTRACT_EXPIRY: "Contract",
  PROPERTY_CREATED: "Property",
  CONTRACT_CREATED: "New Contract",
  CONTRACT_STATUS_CHANGED: "Status Change",
  CONTRACT_REOPENED: "Reopened",
  PAYMENT_PAID: "Paid",
  PAYMENT_RECEIVAL: "Receival",
  EXPENSE_CREATED: "Expense",
};

const statusBadgeConfig: Record<string, { label: string; className: string }> =
  {
    PENDING: {
      label: "Pending",
      className:
        "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
    },
    QUEUED: {
      label: "Queued",
      className:
        "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
    },
    SENT: {
      label: "Sent",
      className:
        "bg-sky-50 text-sky-700 ring-1 ring-sky-200 dark:bg-sky-900/30 dark:text-sky-300 dark:ring-sky-700",
    },
    DELIVERED: {
      label: "Delivered",
      className:
        "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
    },
    FAILED: {
      label: "Failed",
      className:
        "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
    },
    BOUNCED: {
      label: "Bounced",
      className:
        "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
    },
    REJECTED: {
      label: "Rejected",
      className:
        "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
    },
  };

const StatusBadge = ({ status }: { status: string }) => {
  const config = statusBadgeConfig[status] ?? {
    label: status,
    className:
      "bg-slate-50 text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/30 dark:text-slate-300 dark:ring-slate-700",
  };
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
};

const selectClass =
  "px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors";

const inputClass =
  "px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#9ca0b8] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors";

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
  } = usePagination({ defaultSort: "createdAt" });

  const [recipientEmail, setRecipientEmail] = useState("");
  const [recipientInput, setRecipientInput] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [channelFilter, setChannelFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [teamIdentifier, setTeamIdentifier] = useState("");
  const [teamInput, setTeamInput] = useState("");
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
    teamIdentifier: teamIdentifier || undefined,
    sort,
    direction,
  });
  const { data: stats } = useNotificationStats();
  const resendMutation = useResendNotification();

  const handleRecipientSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setRecipientEmail(recipientInput);
    setTeamIdentifier(teamInput);
    handlePageChange(0);
  };

  const handleResend = () => {
    if (!resendTarget) return;
    resendMutation.mutate(resendTarget, {
      onSuccess: () => setResendTarget(null),
    });
  };

  const canResend = (status: string) =>
    ["FAILED", "BOUNCED", "REJECTED"].includes(status);

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
    return <LoadingSpinner message="Loading notifications..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">
          Failed to load notifications.
        </p>
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
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Notifications
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            View and manage all notifications across the platform.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
            >
              <Mail className="h-4 w-4" />
              Total
            </div>
            <div className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              {stats.totalCount}
            </div>
          </div>
          <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
            >
              <CheckCircle className="h-4 w-4 text-emerald-500" />
              Delivered
            </div>
            <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
              {stats.deliveredCount}
            </div>
          </div>
          <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
            >
              <Clock className="h-4 w-4 text-blue-500" />
              Pending
            </div>
            <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {stats.pendingCount}
            </div>
          </div>
          <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              className="text-[#6b7194] dark:text-[#8b90a8] text-sm mb-1"
            >
              <AlertTriangle className="h-4 w-4 text-red-500" />
              Failed
            </div>
            <div className="text-2xl font-bold text-red-600 dark:text-red-400">
              {stats.failedCount}
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <form onSubmit={handleRecipientSearch} className="mb-4">
        <div className="flex flex-wrap gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8]" />
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
          <input
            type="text"
            placeholder="Team identifier..."
            value={teamInput}
            onChange={(e) => setTeamInput(e.target.value)}
            className={`${inputClass} w-44`}
          />
          <button
            type="submit"
            className="px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] hover:bg-[#4c6ef5] rounded-lg transition-colors"
          >
            Search
          </button>
        </div>
      </form>

      {/* Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <SortableHeader field="notificationType" label="Type" />
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Channel
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Recipient
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Team
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Subject
                </th>
                <SortableHeader field="status" label="Status" />
                <SortableHeader field="createdAt" label="Created" />
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {notifications.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No notifications found.
                  </td>
                </tr>
              ) : (
                notifications.map((notif) => (
                  <tr
                    key={notif.identifier}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {typeLabels[notif.notificationType] ??
                          notif.notificationType}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center gap-1 text-sm text-[#3d4463] dark:text-[#c4c8db]">
                        {notif.channel === "EMAIL" ? (
                          <Mail className="h-3.5 w-3.5" />
                        ) : (
                          <Phone className="h-3.5 w-3.5" />
                        )}
                        {notif.channel}
                      </span>
                    </td>
                    <td className="px-4 py-3 max-w-[200px]">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] truncate block">
                        {notif.channel === "SMS"
                          ? notif.recipientPhone || notif.recipientEmail || "-"
                          : notif.recipientEmail || "-"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className="text-sm text-[#6b7194] dark:text-[#8b90a8]"
                        title={notif.teamIdentifier}
                      >
                        {notif.teamName}
                      </span>
                    </td>
                    <td className="px-4 py-3 max-w-[200px]">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] truncate block">
                        {notif.subject || "-"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={notif.status} />
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8] whitespace-nowrap">
                        {format(new Date(notif.createdAt), "dd MMM yyyy HH:mm")}
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
                        <button
                          onClick={() =>
                            navigate(`/notifications/${notif.identifier}`)
                          }
                          className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                          title="View notification"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {canResend(notif.status) && (
                          <button
                            onClick={() => setResendTarget(notif.identifier)}
                            className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
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
