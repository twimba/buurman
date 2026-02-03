import { useNavigate, useParams } from 'react-router-dom';
import { useTenant, useUpdateTenant } from '@/hooks/useTenantHooks';
import { TenantForm } from '@/components/tenants/TenantForm';
import { UpdateTenantRequest } from '@/types/tenant';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const TenantEditPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: tenant, isLoading, error } = useTenant(id);
  const updateTenantMutation = useUpdateTenant(id!);

  const handleSubmit = async (data: UpdateTenantRequest) => {
    await updateTenantMutation.mutateAsync(data);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
        <ErrorMessage message="Failed to load tenant" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/tenants/${id}`)}
            className="p-2 hover:bg-gray-200 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Edit Tenant
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white rounded-lg shadow p-6">
          <TenantForm
            tenant={tenant}
            onSubmit={handleSubmit}
            isLoading={updateTenantMutation.isPending}
          />
        </div>
      </div>
    </div>
  );
};
