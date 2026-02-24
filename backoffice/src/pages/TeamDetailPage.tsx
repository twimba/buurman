import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import {
  Users,
  Calendar,
  Building2,
  UserCheck,
  FileText,
  Receipt,
  CreditCard,
  Files,
  Shield,
  Globe,
  Settings,
  Crown,
  Clock,
} from "lucide-react";
import { PageHeader, Button, ConfirmDialog, RefreshButton } from "@buurman/ui";
import { useTeam, useUpdateTeam, useDeleteTeam } from "../hooks/useTeams";
import { LoadingSpinner } from "../components/LoadingSpinner";

function formatMoney(value: number, currencyCode: string | null): string {
  if (!currencyCode) {
    return value.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  }
  try {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: currencyCode,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  } catch {
    return `${currencyCode} ${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}

export const TeamDetailPage = () => {
  const { identifier } = useParams<{ identifier: string }>();
  const navigate = useNavigate();
  const {
    data: team,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useTeam(identifier!);
  const updateTeam = useUpdateTeam();
  const deleteTeam = useDeleteTeam();

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  if (isLoading) {
    return <LoadingSpinner message="Loading team..." />;
  }

  if (error || !team) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">Failed to load team.</p>
        <button
          onClick={() => navigate("/teams")}
          className="mt-4 text-sm text-[#5c7cfa] hover:underline"
        >
          Back to teams
        </button>
      </div>
    );
  }

  const startEditing = () => {
    setEditName(team.teamName);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setEditName("");
  };

  const handleSave = () => {
    if (!editName.trim()) {
      return;
    }
    updateTeam.mutate(
      { identifier: identifier!, data: { name: editName.trim() } },
      {
        onSuccess: () => setIsEditing(false),
      },
    );
  };

  const handleDelete = () => {
    deleteTeam.mutate(identifier!, {
      onSuccess: () => navigate("/teams"),
    });
  };

  const owner = team.members.find((m) => m.isOwner);

  return (
    <div>
      <PageHeader
        title={team.teamName}
        subtitle={`#${identifier}`}
        backTo="/teams"
        actions={
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            {!isEditing && (
              <Button variant="secondary" onClick={startEditing}>
                Edit
              </Button>
            )}
            <Button variant="danger" onClick={() => setShowDeleteDialog(true)}>
              Delete
            </Button>
          </div>
        }
      />

      {/* Inline Edit Form */}
      {isEditing && (
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5 mb-6">
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
            Team Name
          </label>
          <div className="flex items-center gap-3">
            <input
              type="text"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              className="w-full max-w-md px-3 py-2.5 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
              autoFocus
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  handleSave();
                }
                if (e.key === "Escape") {
                  cancelEditing();
                }
              }}
            />
            <Button onClick={handleSave} isLoading={updateTeam.isPending}>
              Save
            </Button>
            <Button variant="secondary" onClick={cancelEditing}>
              Cancel
            </Button>
          </div>
        </div>
      )}

      {/* Identity Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <IdentityCard
          icon={Crown}
          label="Owner"
          value={
            owner
              ? `${owner.firstName ?? ""} ${owner.lastName ?? ""}`.trim() ||
                owner.email ||
                "Unknown"
              : "No owner"
          }
          subtitle={owner?.email ?? undefined}
          color="amber"
        />
        <IdentityCard
          icon={Users}
          label="Members"
          value={String(team.members.length)}
          subtitle={`${team.members.filter((m) => !m.disabled).length} active`}
          color="blue"
        />
        <IdentityCard
          icon={Calendar}
          label="Created"
          value={format(new Date(team.createdAt), "dd MMM yyyy")}
          subtitle={format(new Date(team.createdAt), "HH:mm")}
          color="emerald"
        />
        <IdentityCard
          icon={Clock}
          label="Last Updated"
          value={format(new Date(team.updatedAt), "dd MMM yyyy")}
          subtitle={format(new Date(team.updatedAt), "HH:mm")}
          color="purple"
        />
      </div>

      {/* Data Volume */}
      <SectionTitle title="Data Overview" />
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
        <DataCard
          icon={Building2}
          label="Properties"
          value={team.dataCounts.properties}
          color="blue"
        />
        <DataCard
          icon={UserCheck}
          label="Tenants"
          value={team.dataCounts.tenants}
          color="emerald"
        />
        <DataCard
          icon={FileText}
          label="Contracts"
          value={team.dataCounts.contracts}
          color="purple"
        />
        <DataCard
          icon={Receipt}
          label="Expenses"
          value={team.dataCounts.expenses}
          color="amber"
        />
        <DataCard
          icon={CreditCard}
          label="Payments"
          value={team.dataCounts.payments}
          color="teal"
        />
        <DataCard
          icon={Files}
          label="Documents"
          value={team.dataCounts.documents}
          color="slate"
        />
      </div>

      {/* Financial Snapshot */}
      <SectionTitle title="Financial Snapshot" />
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5 mb-6">
        <div className="flex flex-col lg:flex-row lg:items-start gap-6">
          {/* Total Active Rent */}
          <div className="flex-shrink-0">
            <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-1">
              Monthly Active Rent
            </p>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold text-emerald-700 dark:text-emerald-300">
                {team.financialSnapshot.currency
                  ? formatMoney(
                      team.financialSnapshot.totalActiveRent,
                      team.financialSnapshot.currency,
                    )
                  : "\u2013"}
              </span>
            </div>
          </div>

          {/* Distributions */}
          <div className="flex-1 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatusDistribution
              title="Property Status"
              data={team.financialSnapshot.propertyStatusDistribution}
              colorMap={propertyStatusColors}
            />
            <StatusDistribution
              title="Property Category"
              data={team.financialSnapshot.propertyCategoryDistribution}
              colorMap={propertyCategoryColors}
            />
            <StatusDistribution
              title="Contracts"
              data={team.financialSnapshot.contractStatusDistribution}
              colorMap={contractStatusColors}
            />
            <StatusDistribution
              title="Payments"
              data={team.financialSnapshot.paymentStatusDistribution}
              colorMap={paymentStatusColors}
            />
          </div>
        </div>
      </div>

      {/* Members Table */}
      <SectionTitle title="Team Members" />
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden mb-6">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <th className="text-left px-5 py-3 text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Member
                </th>
                <th className="text-left px-5 py-3 text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Email
                </th>
                <th className="text-left px-5 py-3 text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Role
                </th>
                <th className="text-left px-5 py-3 text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Joined
                </th>
                <th className="text-left px-5 py-3 text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {team.members.map((member, idx) => (
                <tr
                  key={idx}
                  className={`border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 ${member.disabled ? "opacity-50" : ""}`}
                >
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {member.firstName ?? ""} {member.lastName ?? ""}
                      </span>
                      {member.isOwner && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wider bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 px-1.5 py-0.5 rounded">
                          <Crown className="h-2.5 w-2.5" />
                          Owner
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-5 py-3 text-[#6b7194] dark:text-[#8b90a8]">
                    {member.email ?? "—"}
                  </td>
                  <td className="px-5 py-3">
                    <RoleBadge role={member.role} />
                  </td>
                  <td className="px-5 py-3 text-[#6b7194] dark:text-[#8b90a8]">
                    {member.joinedAt
                      ? format(new Date(member.joinedAt), "dd MMM yyyy")
                      : "—"}
                  </td>
                  <td className="px-5 py-3">
                    {member.disabled ? (
                      <span className="inline-flex items-center text-xs font-medium bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 px-2 py-0.5 rounded-full">
                        Disabled
                      </span>
                    ) : (
                      <span className="inline-flex items-center text-xs font-medium bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 px-2 py-0.5 rounded-full">
                        Active
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Team Settings */}
      <SectionTitle title="Team Settings" />
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        {/* Payment Settings */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
          <div className="flex items-center gap-2 mb-4">
            <Settings className="h-4 w-4 text-[#5c7cfa] dark:text-[#91a7ff]" />
            <h3 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Payment Settings
            </h3>
          </div>
          <div className="space-y-3">
            <SettingsRow
              label="Payments Ahead Count"
              value={String(team.settings.paymentsAheadCount)}
            />
            <SettingsRow
              label="Auto Generation"
              value={
                team.settings.autoGenerationEnabled ? (
                  <span className="text-emerald-600 dark:text-emerald-400">
                    Enabled
                  </span>
                ) : (
                  <span className="text-red-600 dark:text-red-400">
                    Disabled
                  </span>
                )
              }
            />
          </div>
        </div>

        {/* Regional Settings */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
          <div className="flex items-center gap-2 mb-4">
            <Globe className="h-4 w-4 text-[#5c7cfa] dark:text-[#91a7ff]" />
            <h3 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Regional Settings
            </h3>
          </div>
          <div className="space-y-3">
            <SettingsRow
              label="Currency"
              value={team.settings.defaultCurrency}
            />
            <SettingsRow label="Country" value={team.settings.defaultCountry} />
            <SettingsRow label="Timezone" value={team.settings.timezone} />
            <SettingsRow label="Date Format" value={team.settings.dateFormat} />
            <SettingsRow
              label="Fiscal Year Start"
              value={`Month ${team.settings.fiscalYearStartMonth}`}
            />
          </div>
        </div>
      </div>

      {/* Delete confirmation dialog */}
      {showDeleteDialog && (
        <ConfirmDialog
          title="Delete Team"
          message={`Are you sure you want to delete "${team.teamName}"? This action cannot be undone. All team data including properties, tenants, contracts, and financial records will be permanently removed.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteTeam.isPending}
          onConfirm={handleDelete}
          onCancel={() => setShowDeleteDialog(false)}
        />
      )}
    </div>
  );
};

// --- Sub-components ---

const colorClasses: Record<
  string,
  { bg: string; icon: string; value: string }
> = {
  blue: {
    bg: "bg-blue-50 dark:bg-blue-900/20",
    icon: "text-blue-600 dark:text-blue-400",
    value: "text-blue-700 dark:text-blue-300",
  },
  emerald: {
    bg: "bg-emerald-50 dark:bg-emerald-900/20",
    icon: "text-emerald-600 dark:text-emerald-400",
    value: "text-emerald-700 dark:text-emerald-300",
  },
  purple: {
    bg: "bg-purple-50 dark:bg-purple-900/20",
    icon: "text-purple-600 dark:text-purple-400",
    value: "text-purple-700 dark:text-purple-300",
  },
  amber: {
    bg: "bg-amber-50 dark:bg-amber-900/20",
    icon: "text-amber-600 dark:text-amber-400",
    value: "text-amber-700 dark:text-amber-300",
  },
  teal: {
    bg: "bg-teal-50 dark:bg-teal-900/20",
    icon: "text-teal-600 dark:text-teal-400",
    value: "text-teal-700 dark:text-teal-300",
  },
  slate: {
    bg: "bg-slate-100 dark:bg-slate-800/30",
    icon: "text-slate-600 dark:text-slate-400",
    value: "text-slate-700 dark:text-slate-300",
  },
};

const IdentityCard = ({
  icon: Icon,
  label,
  value,
  subtitle,
  color,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  subtitle?: string;
  color: string;
}) => {
  const c = colorClasses[color] ?? colorClasses.blue;
  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-5">
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180]">
            {label}
          </p>
          <p className={`text-lg font-semibold mt-1 truncate ${c.value}`}>
            {value}
          </p>
          {subtitle && (
            <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-0.5 truncate">
              {subtitle}
            </p>
          )}
        </div>
        <div
          className={`flex-shrink-0 w-10 h-10 rounded-lg ${c.bg} flex items-center justify-center`}
        >
          <Icon className={`h-5 w-5 ${c.icon}`} />
        </div>
      </div>
    </div>
  );
};

const DataCard = ({
  icon: Icon,
  label,
  value,
  color,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: number;
  color: string;
}) => {
  const c = colorClasses[color] ?? colorClasses.blue;
  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4 text-center">
      <div
        className={`w-9 h-9 rounded-lg ${c.bg} flex items-center justify-center mx-auto mb-2`}
      >
        <Icon className={`h-4 w-4 ${c.icon}`} />
      </div>
      <p className={`text-2xl font-bold ${c.value}`}>{value}</p>
      <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-0.5">
        {label}
      </p>
    </div>
  );
};

const SectionTitle = ({ title }: { title: string }) => (
  <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
    {title}
  </h2>
);

const propertyStatusColors: Record<string, string> = {
  VACANT:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
  OCCUPIED: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
  MAINTENANCE:
    "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
  UNAVAILABLE:
    "bg-slate-100 text-slate-600 dark:bg-slate-800/30 dark:text-slate-400",
};

const propertyCategoryColors: Record<string, string> = {
  RESIDENTIAL:
    "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
  COMMERCIAL:
    "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300",
  INDUSTRIAL:
    "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300",
  AGRICULTURAL:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
  MIXED_USE:
    "bg-slate-100 text-slate-600 dark:bg-slate-800/30 dark:text-slate-400",
};

const contractStatusColors: Record<string, string> = {
  ACTIVE:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
  DRAFT: "bg-slate-100 text-slate-600 dark:bg-slate-800/30 dark:text-slate-400",
  PENDING_SIGNATURE:
    "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
  EXPIRED:
    "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300",
  TERMINATED: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300",
};

const paymentStatusColors: Record<string, string> = {
  PAID: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
  PENDING:
    "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
  OVERDUE: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300",
  CANCELLED:
    "bg-slate-100 text-slate-600 dark:bg-slate-800/30 dark:text-slate-400",
  PARTIALLY_PAID:
    "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
};

const StatusDistribution = ({
  title,
  data,
  colorMap,
}: {
  title: string;
  data: Record<string, number>;
  colorMap: Record<string, string>;
}) => {
  const entries = Object.entries(data);
  if (entries.length === 0) {
    return (
      <div>
        <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-2">
          {title}
        </p>
        <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">No data</p>
      </div>
    );
  }

  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-2">
        {title}
      </p>
      <div className="flex flex-wrap gap-1.5">
        {entries.map(([status, count]) => (
          <span
            key={status}
            className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-1 rounded ${colorMap[status] ?? "bg-slate-100 text-slate-600 dark:bg-slate-800/30 dark:text-slate-400"}`}
          >
            {formatStatus(status)}
            <span className="font-bold">{count}</span>
          </span>
        ))}
      </div>
    </div>
  );
};

const roleColors: Record<string, string> = {
  TEAM_ADMIN:
    "bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300",
  TEAM_EDITOR:
    "bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300",
  TEAM_VIEWER:
    "bg-slate-100 dark:bg-slate-800/30 text-slate-600 dark:text-slate-400",
};

const RoleBadge = ({ role }: { role: string }) => (
  <span
    className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${roleColors[role] ?? roleColors.TEAM_VIEWER}`}
  >
    <Shield className="h-3 w-3" />
    {formatStatus(role.replace("TEAM_", ""))}
  </span>
);

const SettingsRow = ({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) => (
  <div className="flex items-center justify-between py-1.5 border-b border-[#f1f3f9] dark:border-[#1e2130] last:border-b-0">
    <span className="text-xs text-[#6b7194] dark:text-[#8b90a8]">{label}</span>
    <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
      {value}
    </span>
  </div>
);

const formatStatus = (status: string) =>
  status
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
