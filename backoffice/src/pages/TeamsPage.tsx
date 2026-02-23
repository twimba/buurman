import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Eye, Trash2 } from "lucide-react";
import { RefreshButton } from "@buurman/ui";
import { format } from "date-fns";
import { Pagination, ConfirmDialog } from "@buurman/ui";
import { useTeams, useDeleteTeam } from "../hooks/useTeams";
import { LoadingSpinner } from "../components/LoadingSpinner";

export const TeamsPage = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [deleteIdentifier, setDeleteIdentifier] = useState<string | null>(null);

  const { data, isLoading, isFetching, error, refetch } = useTeams({
    page,
    size,
    search: search || undefined,
  });
  const deleteTeam = useDeleteTeam();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    setPage(0);
  };

  const handleDelete = () => {
    if (!deleteIdentifier) { return; }
    deleteTeam.mutate(deleteIdentifier, {
      onSuccess: () => setDeleteIdentifier(null),
    });
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading teams..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">Failed to load teams.</p>
      </div>
    );
  }

  const teams = data?.content ?? [];

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
            Teams
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Manage all registered teams across the platform.
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
            placeholder="Search teams by name..."
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
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Team Name
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Members
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Owner
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Created
                </th>
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {teams.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No teams found.
                  </td>
                </tr>
              ) : (
                teams.map((team) => (
                  <tr
                    key={team.identifier}
                    className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {team.teamName}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                        {team.memberCount}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                        {team.ownerEmail}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        {format(new Date(team.createdAt), "dd MMM yyyy")}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => navigate(`/teams/${team.identifier}`)}
                          className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-[#91a7ff] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                          title="View team"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeleteIdentifier(team.identifier)}
                          className="p-2 rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                          title="Delete team"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
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
            onPageChange={setPage}
            onSizeChange={(newSize) => {
              setSize(newSize);
              setPage(0);
            }}
          />
        </div>
      )}

      {/* Delete confirmation dialog */}
      {deleteIdentifier && (
        <ConfirmDialog
          title="Delete Team"
          message="Are you sure you want to delete this team? This action cannot be undone. All team data including properties, tenants, contracts, and financial records will be permanently removed."
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteTeam.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteIdentifier(null)}
        />
      )}
    </div>
  );
};
