import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Eye, Trash2 } from "lucide-react";
import { RefreshButton } from "@buurman/ui";
import { formatDate } from "../utils/dateFormatting";
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
    if (!deleteIdentifier) {
      return;
    }
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
        <p className="text-error-text">Failed to load teams.</p>
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
          <h1 className="text-2xl font-bold text-text-primary">Teams</h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage all registered teams across the platform.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Search */}
      <form onSubmit={handleSearch} className="mb-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
          <input
            type="search"
            placeholder="Search teams by name..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary placeholder-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
          />
        </div>
      </form>

      {/* Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Team Name
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Members
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Owner
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Created
                </th>
                <th className="text-right px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {teams.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-12 text-center text-sm text-text-muted"
                  >
                    No teams found.
                  </td>
                </tr>
              ) : (
                teams.map((team) => (
                  <tr
                    key={team.identifier}
                    className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-text-primary">
                          {team.teamName}
                        </span>
                        {team.demo && (
                          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-warning-bg text-warning-text ring-1 ring-warning-border">
                            Demo
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary">
                        {team.memberCount}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary">
                        {team.ownerEmail}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary">
                        {formatDate(team.createdAt)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => navigate(`/teams/${team.identifier}`)}
                          className="p-2 rounded-lg text-text-secondary hover:text-primary-500 hover:bg-surface-inset transition-colors"
                          title="View team"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeleteIdentifier(team.identifier)}
                          className="p-2 rounded-lg text-text-secondary hover:text-error-text hover:bg-error-bg transition-colors"
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
          message="Are you sure you want to delete this team? This action cannot be undone. All team data including properties, contacts, contracts, and financial records will be permanently removed."
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
