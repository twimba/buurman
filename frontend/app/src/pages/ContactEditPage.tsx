import { useNavigate, useParams } from 'react-router-dom';
import { useTenant, useUpdateTenant } from '@/hooks/useTenantHooks';
import { TenantForm } from '@/components/tenants/TenantForm';
import { UpdateTenantRequest } from '@/types/tenant';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const TenantEditPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: tenant, isLoading, error } = useTenant(id);
  const updateTenantMutation = useUpdateTenant(id);

  const handleSubmit = async (data: UpdateTenantRequest) => {
    await updateTenantMutation.mutateAsync(data);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-surface-page flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="min-h-screen bg-surface-page p-8">
        <ErrorMessage message="Failed to load tenant" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/tenants/${id}`)}
            className="p-2 hover:bg-neutral-100 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">Edit Tenant</h1>
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
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
