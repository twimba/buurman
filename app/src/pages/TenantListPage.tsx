import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTenants } from '@/hooks/useTenantHooks';
import { TenantCard } from '@/components/tenants/TenantCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Users, User, Search } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { Pagination, RefreshButton } from '@buurman/ui';

export const TenantListPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    resetPage,
  } = usePagination({ defaultSize: 12 });

  const {
    data: tenantsData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = useTenants({ search: debouncedSearch || undefined, ...pageParams });

  // Debounce search
  const handleSearch = (value: string) => {
    setSearchTerm(value);
    setTimeout(() => {
      setDebouncedSearch(value);
      resetPage();
    }, 500);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load tenants" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <Users className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">Tenants</h1>
            </div>
            <p className="text-text-secondary ml-11">
              Manage your tenants and their information
            </p>
          </div>
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <button
              onClick={() => navigate('/tenants/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              Add Tenant
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-text-muted " />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search by name, email, or phone..."
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
            />
          </div>
        </div>

        {/* Tenant Count */}
        <p className="text-sm text-text-secondary mb-4">
          {tenantsData?.totalElements ?? 0}{' '}
          {tenantsData?.totalElements === 1 ? 'tenant' : 'tenants'}
        </p>

        {/* Tenants Grid */}
        {tenantsData?.content && tenantsData.content.length > 0 ? (
          <>
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {tenantsData.content.map((tenant) => (
                <TenantCard key={tenant.identifier} tenant={tenant} />
              ))}
            </div>
            {tenantsData && (
              <div className="mt-6">
                <Pagination
                  page={page}
                  totalPages={tenantsData.totalPages}
                  totalElements={tenantsData.totalElements}
                  size={size}
                  onPageChange={handlePageChange}
                  onSizeChange={handleSizeChange}
                />
              </div>
            )}
          </>
        ) : (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-surface-card rounded-lg">
            <User className="h-16 w-16 text-text-disabled mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              No tenants yet
            </h3>
            <p className="text-text-secondary mb-6">
              Get started by adding your first tenant
            </p>
            <button
              onClick={() => navigate('/tenants/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              Add Tenant
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
