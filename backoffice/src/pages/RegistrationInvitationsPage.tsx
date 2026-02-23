import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Search,
  Plus,
  Copy,
  Send,
  Ban,
  MoreHorizontal,
  Ticket,
  StickyNote,
} from "lucide-react";
import { RefreshButton, Pagination, ConfirmDialog } from "@buurman/ui";
import { format } from "date-fns";
import {
  useRegistrationInvitations,
  useRevokeRegistrationInvitation,
} from "../hooks/useRegistrationInvitations";
import { usePagination } from "../hooks/usePagination";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { SortableHeader } from "../components/SortableHeader";
import { CreateRegistrationInvitationModal } from "../components/CreateRegistrationInvitationModal";
import { SendRegistrationInvitationModal } from "../components/SendRegistrationInvitationModal";
import type { RegistrationInvitation } from "../api/registrationInvitations";

const STATUS_STYLES: Record<string, string> = {
  ACTIVE:
    "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/20 dark:text-emerald-400 dark:border-emerald-800",
  EXPIRED:
    "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/20 dark:text-amber-400 dark:border-amber-800",
  EXHAUSTED:
    "bg-zinc-50 text-zinc-600 border-zinc-200 dark:bg-zinc-800/40 dark:text-zinc-400 dark:border-zinc-700",
  REVOKED:
    "bg-red-50 text-red-700 border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-800",
};

export function RegistrationInvitationsPage() {
  const [search, setSearch] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [sendTarget, setSendTarget] = useState<RegistrationInvitation | null>(
    null,
  );
  const [revokeTarget, setRevokeTarget] =
    useState<RegistrationInvitation | null>(null);
  const [openMenu, setOpenMenu] = useState<string | null>(null);
  const [menuPos, setMenuPos] = useState<{
    top: number;
    right: number;
  } | null>(null);

  const handleToggleMenu = (identifier: string) => {
    if (openMenu !== identifier) {
      const btn = document.querySelector<HTMLElement>(
        `[data-menu-trigger="${identifier}"]`,
      );
      if (btn) {
        const rect = btn.getBoundingClientRect();
        setMenuPos({
          top: rect.bottom + 4,
          right: window.innerWidth - rect.right,
        });
      }
    }
    setOpenMenu(openMenu === identifier ? null : identifier);
  };

  const {
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({
    defaultSort: "createdAt",
    defaultDirection: "desc",
  });

  const { data, isLoading, refetch } = useRegistrationInvitations({
    page,
    size,
    search: search || undefined,
    sort,
    direction,
  });

  const revokeMutation = useRevokeRegistrationInvitation();

  const handleCopyLink = (code: string) => {
    const url = `${window.location.protocol}//app.${window.location.hostname.replace(/^backoffice\./, "")}/register?code=${code}`;
    navigator.clipboard.writeText(url);
  };

  const handleRevoke = async () => {
    if (!revokeTarget) return;
    await revokeMutation.mutateAsync(revokeTarget.identifier);
    setRevokeTarget(null);
  };

  const formatUsage = (inv: RegistrationInvitation) => {
    if (inv.maxUsages === null) return `${inv.usageCount} / \u221E`;
    return `${inv.usageCount} / ${inv.maxUsages}`;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Ticket className="h-6 w-6 text-indigo-500" />
          <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">
            Registration Invitations
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <RefreshButton onClick={() => refetch()} />
          <button
            onClick={() => setShowCreate(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
          >
            <Plus className="h-4 w-4" />
            Create Invitation
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
        <input
          type="text"
          placeholder="Search by code or creator..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            resetPage();
          }}
          className="w-full rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 pl-10 pr-4 py-2 text-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-colors"
        />
      </div>

      {/* Table */}
      {isLoading ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="overflow-hidden rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800/50">
            <table className="min-w-full divide-y divide-zinc-200 dark:divide-zinc-700">
              <thead className="bg-zinc-50 dark:bg-zinc-800">
                <tr>
                  <SortableHeader
                    label="Code"
                    field="code"
                    sort={sort}
                    direction={direction}
                    onSortChange={handleSortChange}
                  />
                  <th className="px-4 py-3 text-left text-xs font-medium text-zinc-500 uppercase tracking-wider">
                    Status
                  </th>
                  <SortableHeader
                    label="Usages"
                    field="usageCount"
                    sort={sort}
                    direction={direction}
                    onSortChange={handleSortChange}
                  />
                  <th className="px-4 py-3 text-left text-xs font-medium text-zinc-500 uppercase tracking-wider">
                    Expires
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-zinc-500 uppercase tracking-wider">
                    Created By
                  </th>
                  <SortableHeader
                    label="Created"
                    field="createdAt"
                    sort={sort}
                    direction={direction}
                    onSortChange={handleSortChange}
                  />
                  <th className="px-4 py-3 text-right text-xs font-medium text-zinc-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-700/50">
                {data?.content?.map((inv) => (
                  <tr
                    key={inv.identifier}
                    className="hover:bg-zinc-50/50 dark:hover:bg-zinc-800/30 transition-colors"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1.5">
                        <Link
                          to={`/registration-invitations/${inv.identifier}`}
                          className="font-mono text-sm font-semibold text-indigo-600 hover:text-indigo-800 dark:text-indigo-400 dark:hover:text-indigo-300 hover:underline"
                        >
                          {inv.code}
                        </Link>
                        {inv.hasNote && (
                          <span title="Has internal note">
                            <StickyNote className="h-3.5 w-3.5 text-amber-500 flex-shrink-0" />
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[inv.status] || ""}`}
                      >
                        {inv.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400 font-mono">
                      {formatUsage(inv)}
                    </td>
                    <td className="px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400">
                      {inv.expiresAt
                        ? format(new Date(inv.expiresAt), "MMM d, yyyy HH:mm")
                        : "Never"}
                    </td>
                    <td className="px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400">
                      {inv.createdBy}
                    </td>
                    <td className="px-4 py-3 text-sm text-zinc-500 dark:text-zinc-400">
                      {format(new Date(inv.createdAt), "MMM d, yyyy HH:mm")}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        data-menu-trigger={inv.identifier}
                        onClick={() => handleToggleMenu(inv.identifier)}
                        className="p-1.5 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-700 transition-colors"
                      >
                        <MoreHorizontal className="h-4 w-4 text-zinc-500" />
                      </button>
                      {openMenu === inv.identifier && menuPos && (
                        <>
                          <div
                            className="fixed inset-0 z-40"
                            onClick={() => setOpenMenu(null)}
                          />
                          <div
                            style={{
                              position: "fixed",
                              top: menuPos.top,
                              right: menuPos.right,
                            }}
                            className="z-50 w-48 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 shadow-lg py-1"
                          >
                            <button
                              onClick={() => {
                                handleCopyLink(inv.code);
                                setOpenMenu(null);
                              }}
                              className="flex w-full items-center gap-2 px-3 py-2 text-sm text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700"
                            >
                              <Copy className="h-4 w-4" />
                              Copy Link
                            </button>
                            {inv.status === "ACTIVE" && (
                              <>
                                <button
                                  onClick={() => {
                                    setSendTarget(inv);
                                    setOpenMenu(null);
                                  }}
                                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700"
                                >
                                  <Send className="h-4 w-4" />
                                  Send Invitation
                                </button>
                                <button
                                  onClick={() => {
                                    setRevokeTarget(inv);
                                    setOpenMenu(null);
                                  }}
                                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                                >
                                  <Ban className="h-4 w-4" />
                                  Revoke
                                </button>
                              </>
                            )}
                          </div>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
                {data?.content?.length === 0 && (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-12 text-center text-sm text-zinc-500"
                    >
                      No invitations found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {data && data.totalPages > 1 && (
            <Pagination
              page={page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={size}
              onPageChange={handlePageChange}
              onSizeChange={handleSizeChange}
            />
          )}
        </>
      )}

      {/* Create Modal */}
      {showCreate && (
        <CreateRegistrationInvitationModal
          onClose={() => setShowCreate(false)}
        />
      )}

      {/* Send Modal */}
      {sendTarget && (
        <SendRegistrationInvitationModal
          invitation={sendTarget}
          onClose={() => setSendTarget(null)}
        />
      )}

      {/* Revoke Confirm */}
      {revokeTarget && (
        <ConfirmDialog
          title="Revoke Invitation"
          message={`Are you sure you want to revoke "${revokeTarget.code}"? It will no longer be usable.`}
          confirmLabel="Revoke"
          variant="danger"
          isLoading={revokeMutation.isPending}
          onConfirm={handleRevoke}
          onCancel={() => setRevokeTarget(null)}
        />
      )}
    </div>
  );
}
