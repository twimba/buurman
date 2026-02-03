import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTenants } from '@/hooks/useTenantHooks';
import { TenantCard } from '@/components/tenants/TenantCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Users, User, Search } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';

export const TenantListPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const { data: tenants, isLoading, error } = useTenants(debouncedSearch);

  // Debounce search
  const handleSearch = (value: string) => {
    setSearchTerm(value);
    setTimeout(() => {
      setDebouncedSearch(value);
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
              <Users className="h-8 w-8 text-blue-600 dark:text-blue-400" />
              <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
                Tenants
              </h1>
            </div>
            <p className="text-gray-600 dark:text-gray-400 ml-11">
              Manage your tenants and their information
            </p>
          </div>
          <button
            onClick={() => navigate('/tenants/new')}
            disabled={!canEditData}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-blue-600"
          >
            <Plus className="h-5 w-5" />
            Add Tenant
          </button>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search by name, email, or phone..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded focus:border-blue-600 focus:ring-1 focus:ring-blue-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100"
            />
          </div>
        </div>

        {/* Tenant Count */}
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
          {tenants?.length || 0} {tenants?.length === 1 ? 'tenant' : 'tenants'}
        </p>

        {/* Tenants Grid */}
        {tenants && tenants.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {tenants.map((tenant) => (
              <TenantCard key={tenant.id} tenant={tenant} />
            ))}
          </div>
        ) : (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-white dark:bg-gray-800 rounded-lg">
            <User className="h-16 w-16 text-gray-300 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
              No tenants yet
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Get started by adding your first tenant
            </p>
            <button
              onClick={() => navigate('/tenants/new')}
              disabled={!canEditData}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-blue-600"
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
