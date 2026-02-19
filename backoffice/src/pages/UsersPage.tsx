import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Eye, UserX, UserCheck } from "lucide-react";
import { RefreshButton } from "@buurman/ui";
import { SortableHeader } from "../components/SortableHeader";
import { format } from "date-fns";
import { Pagination, ConfirmDialog } from "@buurman/ui";
import { useUsers, useDisableUser, useEnableUser } from "../hooks/useUsers";
import { usePagination } from "../hooks/usePagination";
import { LoadingSpinner } from "../components/LoadingSpinner";

export const UsersPage = () => {
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
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [actionTarget, setActionTarget] = useState<{
    identifier: string;
    action: "disable" | "enable";
  } | null>(null);

  const { data, isLoading, isFetching, error, refetch } = useUsers({
    page,
    size,
    search: search || undefined,
    sort,
    direction,
  });
  const disableUser = useDisableUser();
  const enableUser = useEnableUser();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    handlePageChange(0);
  };

  const handleAction = () => {
    if (!actionTarget) return;
    const mutation =
      actionTarget.action === "disable" ? disableUser : enableUser;
    mutation.mutate(actionTarget.identifier, {
      onSuccess: () => setActionTarget(null),
    });
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading users..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">Failed to load users.</p>
      </div>
    );
  }

  const users = data?.content ?? [];

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
            Users
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Manage all platform users across teams.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Search */}
      <form onSubmit={handleSearch} className="mb-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8]" />
          <input
            type="search"
            placeholder="Search by email or name..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#9ca0b8] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
          />
        </div>
      </form>

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
                  field="firstName"
                  label="Name"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Phone
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Verified
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Status
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Teams
                </th>
                <SortableHeader
                  field="createdAt"
                  label="Created"
                  sort={sort}
                  direction={direction}
                  onSortChange={handleSortChange}
                />
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No users found.
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr
                    key={user.identifier}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {user.email}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div
                          className={`h-2 w-2 rounded-full flex-shrink-0 ${user.online ? "bg-emerald-500" : "bg-slate-300 dark:bg-slate-600"}`}
                          title={user.online ? "Online" : "Offline"}
                        />
                        <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                          {user.firstName} {user.lastName}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        {user.phone || "-"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {user.emailVerified ? (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700">
                          Verified
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700">
                          Unverified
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {user.disabled ? (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700">
                          Disabled
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700">
                          Active
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                        {user.teamCount}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        {format(new Date(user.createdAt), "dd MMM yyyy")}
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
                          onClick={() => navigate(`/users/${user.identifier}`)}
                          className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                          title="View user"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {user.disabled ? (
                          <button
                            onClick={() =>
                              setActionTarget({
                                identifier: user.identifier,
                                action: "enable",
                              })
                            }
                            className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 transition-colors"
                            title="Enable user"
                          >
                            <UserCheck className="h-4 w-4" />
                          </button>
                        ) : (
                          <button
                            onClick={() =>
                              setActionTarget({
                                identifier: user.identifier,
                                action: "disable",
                              })
                            }
                            className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                            title="Disable user"
                          >
                            <UserX className="h-4 w-4" />
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

      {/* Action confirmation dialog */}
      {actionTarget && (
        <ConfirmDialog
          title={
            actionTarget.action === "disable" ? "Disable User" : "Enable User"
          }
          message={
            actionTarget.action === "disable"
              ? "Are you sure you want to disable this user? They will no longer be able to log in."
              : "Are you sure you want to enable this user? They will be able to log in again."
          }
          confirmLabel={
            actionTarget.action === "disable" ? "Disable" : "Enable"
          }
          cancelLabel="Cancel"
          variant={actionTarget.action === "disable" ? "danger" : "default"}
          isLoading={disableUser.isPending || enableUser.isPending}
          onConfirm={handleAction}
          onCancel={() => setActionTarget(null)}
        />
      )}
    </div>
  );
};
