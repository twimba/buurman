import { useState, useMemo } from "react";
import {
  Search,
  UserPlus,
  UserX,
  UserCheck,
  KeyRound,
  UserPen,
  Trash2,
  MoreHorizontal,
  ShieldCheck,
  ShieldOff,
  XCircle,
} from "lucide-react";
import { RefreshButton, Pagination, ConfirmDialog, Button } from "@buurman/ui";
import { useAuth } from "../contexts/AuthContext";
import { format } from "date-fns";
import {
  useBuurmies,
  useDisableBuurmy,
  useEnableBuurmy,
  useDeleteBuurmy,
  useForcePasswordUpdate,
  useForceProfileUpdate,
  useVerifyBuurmy,
  useUnverifyBuurmy,
  useRemovePasswordReset,
  useRemoveProfileReset,
} from "../hooks/useBuurmies";
import { usePagination } from "../hooks/usePagination";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { CreateBuurmyModal } from "../components/CreateBuurmyModal";
import { SortableHeader } from "../components/SortableHeader";
import type { Buurmy } from "../types";

type ActionType =
  | "disable"
  | "enable"
  | "delete"
  | "forcePassword"
  | "forceProfile"
  | "verify"
  | "unverify"
  | "removePasswordReset"
  | "removeProfileReset";

interface ActionTarget {
  buurmy: Buurmy;
  action: ActionType;
}

const ACTION_CONFIG: Record<
  ActionType,
  {
    title: string;
    message: (email: string) => string;
    confirmLabel: string;
    variant: "danger" | "default";
  }
> = {
  disable: {
    title: "Disable Buurmy",
    message: (email) =>
      `Are you sure you want to disable "${email}"? They will not be able to log in.`,
    confirmLabel: "Disable",
    variant: "danger",
  },
  enable: {
    title: "Enable Buurmy",
    message: (email) =>
      `Are you sure you want to enable "${email}"? They will be able to log in again.`,
    confirmLabel: "Enable",
    variant: "default",
  },
  delete: {
    title: "Delete Buurmy",
    message: (email) =>
      `Are you sure you want to permanently delete "${email}"? This action cannot be undone and the user will lose all access.`,
    confirmLabel: "Delete permanently",
    variant: "danger",
  },
  forcePassword: {
    title: "Reset Password",
    message: (email) =>
      `"${email}" will be required to set a new password on their next login.`,
    confirmLabel: "Reset Password",
    variant: "default",
  },
  forceProfile: {
    title: "Reset Profile",
    message: (email) =>
      `"${email}" will be required to update their profile on their next login.`,
    confirmLabel: "Reset Profile",
    variant: "default",
  },
  verify: {
    title: "Verify Email",
    message: (email) => `Mark "${email}" as email-verified?`,
    confirmLabel: "Verify",
    variant: "default",
  },
  unverify: {
    title: "Unverify Email",
    message: (email) =>
      `Mark "${email}" as email-unverified? They may need to re-verify.`,
    confirmLabel: "Unverify",
    variant: "danger",
  },
  removePasswordReset: {
    title: "Remove Password Reset",
    message: (email) =>
      `Remove the password reset requirement for "${email}"? They will no longer be prompted to change their password on next login.`,
    confirmLabel: "Remove",
    variant: "default",
  },
  removeProfileReset: {
    title: "Remove Profile Reset",
    message: (email) =>
      `Remove the profile update requirement for "${email}"? They will no longer be prompted to update their profile on next login.`,
    confirmLabel: "Remove",
    variant: "default",
  },
};

const TH_CLASS =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]";

const ACTION_LABELS: Record<string, string> = {
  UPDATE_PASSWORD: "Password Reset",
  UPDATE_PROFILE: "Profile Update",
  VERIFY_EMAIL: "Verify Email",
  CONFIGURE_TOTP: "Configure OTP",
  UPDATE_EMAIL: "Update Email",
};

export const BuurmiesPage = () => {
  const { keycloak } = useAuth();
  const currentUserId = keycloak.subject;
  const {
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
  } = usePagination({ defaultSort: "email", defaultDirection: "asc" });
  const [search, setSearch] = useState("");
  const [actionTarget, setActionTarget] = useState<ActionTarget | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  const { data, isLoading, isFetching, error, refetch } = useBuurmies({
    page,
    size,
    sort,
    direction,
  });

  const disableBuurmy = useDisableBuurmy();
  const enableBuurmy = useEnableBuurmy();
  const deleteBuurmy = useDeleteBuurmy();
  const forcePasswordUpdate = useForcePasswordUpdate();
  const forceProfileUpdate = useForceProfileUpdate();
  const verifyBuurmy = useVerifyBuurmy();
  const unverifyBuurmy = useUnverifyBuurmy();
  const removePasswordReset = useRemovePasswordReset();
  const removeProfileReset = useRemoveProfileReset();

  const handleAction = () => {
    if (!actionTarget) { return; }
    const { buurmy, action } = actionTarget;
    const mutationMap = {
      disable: disableBuurmy,
      enable: enableBuurmy,
      delete: deleteBuurmy,
      forcePassword: forcePasswordUpdate,
      forceProfile: forceProfileUpdate,
      verify: verifyBuurmy,
      unverify: unverifyBuurmy,
      removePasswordReset,
      removeProfileReset,
    };
    mutationMap[action].mutate(buurmy.id, {
      onSuccess: () => setActionTarget(null),
    });
  };

  const isActionPending =
    disableBuurmy.isPending ||
    enableBuurmy.isPending ||
    deleteBuurmy.isPending ||
    forcePasswordUpdate.isPending ||
    forceProfileUpdate.isPending ||
    verifyBuurmy.isPending ||
    unverifyBuurmy.isPending ||
    removePasswordReset.isPending ||
    removeProfileReset.isPending;

  const buurmies = useMemo(() => {
    const all = data?.content ?? [];
    if (!search.trim()) { return all; }
    const q = search.toLowerCase();
    return all.filter(
      (b) =>
        b.email?.toLowerCase().includes(q) ||
        b.username?.toLowerCase().includes(q) ||
        b.firstName?.toLowerCase().includes(q) ||
        b.lastName?.toLowerCase().includes(q),
    );
  }, [data?.content, search]);

  if (isLoading) {
    return <LoadingSpinner message="Loading buurmies..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">
          Failed to load buurmies.
        </p>
      </div>
    );
  }

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
            Buurmies
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Manage users registered in the Keycloak backoffice realm.
          </p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
          <Button
            variant="primary"
            size="sm"
            leftIcon={<UserPlus />}
            onClick={() => setShowCreateModal(true)}
          >
            New Buurmy
          </Button>
        </div>
      </div>

      {/* Search */}
      <div className="mb-4 relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8]" />
        <input
          type="search"
          placeholder="Search by email, username or name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#9ca0b8] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
        />
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                <SortableHeader
                  field="email"
                  label="Email"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <SortableHeader
                  field="username"
                  label="Username"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <SortableHeader
                  field="firstName"
                  label="Name"
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
                <th className={TH_CLASS}>Verified</th>
                <SortableHeader
                  field="createdAt"
                  label="Created"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <th className={TH_CLASS}>Last Login</th>
                <th className={TH_CLASS}>Required Actions</th>
                <th className={`${TH_CLASS} text-right`}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {buurmies.length === 0 ? (
                <tr>
                  <td
                    colSpan={9}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No buurmies found.
                  </td>
                </tr>
              ) : (
                buurmies.map((buurmy) => (
                  <BuurmyRow
                    key={buurmy.id}
                    buurmy={buurmy}
                    isSelf={buurmy.id === currentUserId}
                    openMenuId={openMenuId}
                    onToggleMenu={(id) =>
                      setOpenMenuId(openMenuId === id ? null : id)
                    }
                    onAction={(action) => {
                      setActionTarget({ buurmy, action });
                      setOpenMenuId(null);
                    }}
                  />
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

      {/* Action confirmation dialog */}
      {actionTarget && (
        <ConfirmDialog
          title={ACTION_CONFIG[actionTarget.action].title}
          message={ACTION_CONFIG[actionTarget.action].message(
            actionTarget.buurmy.email,
          )}
          confirmLabel={ACTION_CONFIG[actionTarget.action].confirmLabel}
          cancelLabel="Cancel"
          variant={ACTION_CONFIG[actionTarget.action].variant}
          isLoading={isActionPending}
          onConfirm={handleAction}
          onCancel={() => setActionTarget(null)}
        />
      )}

      {/* Create modal */}
      {showCreateModal && (
        <CreateBuurmyModal onClose={() => setShowCreateModal(false)} />
      )}
    </div>
  );
};

// --- Row component with dropdown menu ---

interface BuurmyRowProps {
  buurmy: Buurmy;
  isSelf: boolean;
  openMenuId: string | null;
  onToggleMenu: (id: string) => void;
  onAction: (action: ActionType) => void;
}

const BuurmyRow = ({
  buurmy,
  isSelf,
  openMenuId,
  onToggleMenu,
  onAction,
}: BuurmyRowProps) => {
  const isOpen = openMenuId === buurmy.id;
  const [menuPos, setMenuPos] = useState<{ top: number; right: number } | null>(
    null,
  );

  const handleToggle = (id: string) => {
    if (openMenuId !== id) {
      // Opening — snapshot the button position before toggling
      const btn = document.querySelector<HTMLElement>(
        `[data-menu-trigger="${id}"]`,
      );
      if (btn) {
        const rect = btn.getBoundingClientRect();
        setMenuPos({
          top: rect.bottom + 4,
          right: window.innerWidth - rect.right,
        });
      }
    }
    onToggleMenu(id);
  };

  return (
    <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors">
      <td className="px-4 py-3">
        <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
          {buurmy.email}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          {buurmy.username}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
          {buurmy.firstName} {buurmy.lastName}
        </span>
      </td>
      <td className="px-4 py-3">
        {buurmy.enabled ? (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700">
            Active
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700">
            Disabled
          </span>
        )}
      </td>
      <td className="px-4 py-3">
        {buurmy.emailVerified ? (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700">
            Verified
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700">
            Unverified
          </span>
        )}
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          {buurmy.createdAt
            ? format(new Date(buurmy.createdAt), "dd MMM yyyy")
            : "-"}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          {buurmy.lastLogin
            ? format(new Date(buurmy.lastLogin), "dd MMM yyyy HH:mm")
            : "Never"}
        </span>
      </td>
      <td className="px-4 py-3">
        <div className="flex flex-wrap gap-1">
          {buurmy.requiredActions?.map((action) => (
            <span
              key={action}
              className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-orange-50 text-orange-700 ring-1 ring-orange-200 dark:bg-orange-900/30 dark:text-orange-300 dark:ring-orange-700"
            >
              {ACTION_LABELS[action] ?? action}
            </span>
          ))}
        </div>
      </td>
      <td className="px-4 py-3 text-right">
        <button
          data-menu-trigger={buurmy.id}
          onClick={() => handleToggle(buurmy.id)}
          aria-label={`Actions for ${buurmy.email}`}
          aria-expanded={isOpen}
          aria-haspopup="menu"
          className="p-1.5 rounded-md text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
        >
          <MoreHorizontal className="h-4 w-4" />
        </button>

        {isOpen && menuPos && (
          <>
            {/* Backdrop to close menu */}
            <div
              className="fixed inset-0 z-40"
              onClick={() => onToggleMenu(buurmy.id)}
            />
            <div
              role="menu"
              style={{
                position: "fixed",
                top: menuPos.top,
                right: menuPos.right,
              }}
              className="z-50 w-56 text-left bg-white dark:bg-[#1a1d28] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-lg py-1"
            >
              {!isSelf &&
                (buurmy.enabled ? (
                  <MenuButton
                    icon={<UserX className="h-3.5 w-3.5" />}
                    label="Disable"
                    onClick={() => onAction("disable")}
                    variant="danger"
                  />
                ) : (
                  <MenuButton
                    icon={<UserCheck className="h-3.5 w-3.5" />}
                    label="Enable"
                    onClick={() => onAction("enable")}
                    variant="success"
                  />
                ))}
              <MenuButton
                icon={<KeyRound className="h-3.5 w-3.5" />}
                label="Reset Password"
                onClick={() => onAction("forcePassword")}
              />
              <MenuButton
                icon={<UserPen className="h-3.5 w-3.5" />}
                label="Reset Profile"
                onClick={() => onAction("forceProfile")}
              />
              {buurmy.requiredActions?.includes("UPDATE_PASSWORD") && (
                <MenuButton
                  icon={<XCircle className="h-3.5 w-3.5" />}
                  label="Clear Password Reset"
                  onClick={() => onAction("removePasswordReset")}
                  variant="success"
                />
              )}
              {buurmy.requiredActions?.includes("UPDATE_PROFILE") && (
                <MenuButton
                  icon={<XCircle className="h-3.5 w-3.5" />}
                  label="Clear Profile Reset"
                  onClick={() => onAction("removeProfileReset")}
                  variant="success"
                />
              )}
              {buurmy.emailVerified ? (
                <MenuButton
                  icon={<ShieldOff className="h-3.5 w-3.5" />}
                  label="Unverify Email"
                  onClick={() => onAction("unverify")}
                  variant="danger"
                />
              ) : (
                <MenuButton
                  icon={<ShieldCheck className="h-3.5 w-3.5" />}
                  label="Verify Email"
                  onClick={() => onAction("verify")}
                  variant="success"
                />
              )}
              {!isSelf && (
                <>
                  <div className="my-1 border-t border-[#e2e6f0] dark:border-[#2a2e3f]" />
                  <MenuButton
                    icon={<Trash2 className="h-3.5 w-3.5" />}
                    label="Delete"
                    onClick={() => onAction("delete")}
                    variant="danger"
                  />
                </>
              )}
            </div>
          </>
        )}
      </td>
    </tr>
  );
};

// --- Dropdown menu button ---

interface MenuButtonProps {
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
  variant?: "default" | "danger" | "success";
}

const MenuButton = ({
  icon,
  label,
  onClick,
  variant = "default",
}: MenuButtonProps) => {
  const colorClasses = {
    default:
      "text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#262a3a]",
    danger:
      "text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20",
    success:
      "text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-900/20",
  };

  return (
    <button
      role="menuitem"
      onClick={onClick}
      className={`w-full flex items-center gap-2 px-3 py-1.5 text-sm transition-colors ${colorClasses[variant]}`}
    >
      <span className="flex-shrink-0 w-4 flex items-center justify-center">
        {icon}
      </span>
      {label}
    </button>
  );
};
