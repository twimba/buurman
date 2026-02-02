import { useNavigate } from 'react-router-dom';
import { useCreateTenant } from '@/hooks/useTenantHooks';
import { TenantForm } from '@/components/tenants/TenantForm';
import { CreateTenantRequest } from '@/types/tenant';
import { ArrowLeft } from 'lucide-react';

export const TenantCreatePage = () => {
  const navigate = useNavigate();
  const createTenantMutation = useCreateTenant();

  const handleSubmit = async (data: CreateTenantRequest) => {
    await createTenantMutation.mutateAsync(data);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/tenants')}
            className="p-2 hover:bg-gray-200 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-gray-900">Add New Tenant</h1>
        </div>

        {/* Form */}
        <div className="bg-white rounded-lg shadow p-6">
          <TenantForm
            onSubmit={handleSubmit}
            isLoading={createTenantMutation.isPending}
          />
        </div>
      </div>
    </div>
  );
};
