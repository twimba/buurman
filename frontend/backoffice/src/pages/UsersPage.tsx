import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search, Eye, UserX, UserCheck } from 'lucide-react';
import { RefreshButton } from '@buurman/ui';
import { SortableHeader } from '../components/SortableHeader';
import { formatDate } from '../utils/dateFormatting';
import { Pagination, ConfirmDialog } from '@buurman/ui';
import { useUsers, useDisableUser, useEnableUser } from '../hooks/useUsers';
import { usePagination } from '../hooks/usePagination';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { AsyncSelect, type AsyncSelectOption } from '../components/AsyncSelect';
import { useTeamSearch } from '../hooks/useTeams';

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
  } = usePagination({ defaultSort: 'createdAt' });
  const [searchParams, setSearchParams] = useSearchParams();
  const teamSearch = useTeamSearch();
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [selectedTeams, setSelectedTeams] = useState<AsyncSelectOption[]>(
    () => {
      const teamParam = searchParams.get('team') ?? '';
      if (!teamParam) {
        return [];
      }
      return teamParam
        .split(',')
        .filter(Boolean)
        .map((id) => ({ value: id, label: id }));
    }
  );
  const [actionTarget, setActionTarget] = useState<{
    identifier: string;
    action: 'disable' | 'enable';
  } | null>(null);

  const handleTeamFilterChange = (options: AsyncSelectOption[]) => {
    setSelectedTeams(options);
    const newParams = new URLSearchParams(searchParams);
    if (options.length > 0) {
      newParams.set('team', options.map((o) => o.value).join(','));
    } else {
      newParams.delete('team');
    }
    setSearchParams(newParams, { replace: true });
    handlePageChange(0);
  };

  const teamFilterValue =
    selectedTeams.map((t) => t.value).join(',') || undefined;

  const { data, isLoading, isFetching, error, refetch } = useUsers({
    page,
    size,
    search: search || undefined,
    sort,
    direction,
    team: teamFilterValue,
  });
  const disableUser = useDisableUser();
  const enableUser = useEnableUser();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    handlePageChange(0);
  };

  const handleAction = () => {
    if (!actionTarget) {
      return;
    }
    const mutation =
      actionTarget.action === 'disable' ? disableUser : enableUser;
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
        <p className="text-error-text">Failed to load users.</p>
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
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Users</h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage all platform users across teams.
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
            placeholder="Search by email or name..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary placeholder-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
          />
        </div>
      </form>

      {/* Team Filter */}
      <div className="mb-4">
        <label className="block text-xs font-medium text-text-muted uppercase tracking-wider mb-1.5">
          Filter by Team
        </label>
        <AsyncSelect
          selected={selectedTeams}
          onSelect={handleTeamFilterChange}
          search={teamSearch}
          placeholder="Search teams..."
          className="max-w-md"
        />
      </div>

      {/* Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
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
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Phone
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Verified
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Status
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary">
                  Teams
                </th>
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
              {users.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-sm text-text-muted"
                  >
                    No users found.
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr
                    key={user.identifier}
                    onClick={() => navigate(`/users/${user.identifier}`)}
                    className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors cursor-pointer"
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-text-primary">
                        {user.email}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div
                          className={`h-2 w-2 rounded-full flex-shrink-0 ${user.online ? 'bg-emerald-500' : 'bg-slate-300'}`}
                          title={user.online ? 'Online' : 'Offline'}
                        />
                        <span className="text-sm text-text-secondary">
                          {user.firstName} {user.lastName}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary">
                        {user.phone || '-'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {user.emailVerified ? (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-success-bg text-success-text ring-1 ring-success-border">
                          Verified
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-error-bg text-error-text ring-1 ring-error-border">
                          Unverified
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {user.disabled ? (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-error-bg text-error-text ring-1 ring-error-border">
                          Disabled
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-success-bg text-success-text ring-1 ring-success-border">
                          Active
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary">
                        {user.teamCount}
                        {user.demoTeamCount > 0 && (
                          <span className="ml-1.5 inline-flex items-center rounded-full px-1.5 py-0.5 text-xs font-medium bg-warning-bg text-warning-text ring-1 ring-warning-border">
                            {user.demoTeamCount} demo
                          </span>
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-text-secondary">
                        {formatDate(user.createdAt)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div
                        onClick={(e) => e.stopPropagation()}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'flex-end',
                          gap: '0.25rem',
                        }}
                      >
                        <button
                          onClick={() => navigate(`/users/${user.identifier}`)}
                          className="p-2 rounded-lg text-text-secondary hover:text-primary-500 hover:bg-surface-inset transition-colors"
                          title="View user"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {user.disabled ? (
                          <button
                            onClick={() =>
                              setActionTarget({
                                identifier: user.identifier,
                                action: 'enable',
                              })
                            }
                            className="p-2 rounded-lg text-text-secondary hover:text-success-text hover:bg-success-bg transition-colors"
                            title="Enable user"
                          >
                            <UserCheck className="h-4 w-4" />
                          </button>
                        ) : (
                          <button
                            onClick={() =>
                              setActionTarget({
                                identifier: user.identifier,
                                action: 'disable',
                              })
                            }
                            className="p-2 rounded-lg text-text-secondary hover:text-error-text hover:bg-error-bg transition-colors"
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
            actionTarget.action === 'disable' ? 'Disable User' : 'Enable User'
          }
          message={
            actionTarget.action === 'disable'
              ? 'Are you sure you want to disable this user? They will no longer be able to log in.'
              : 'Are you sure you want to enable this user? They will be able to log in again.'
          }
          confirmLabel={
            actionTarget.action === 'disable' ? 'Disable' : 'Enable'
          }
          cancelLabel="Cancel"
          variant={actionTarget.action === 'disable' ? 'danger' : 'default'}
          isLoading={disableUser.isPending || enableUser.isPending}
          onConfirm={handleAction}
          onCancel={() => setActionTarget(null)}
        />
      )}
    </div>
  );
};
