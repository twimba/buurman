import { useNavigate } from 'react-router-dom';
import { useCreateProperty } from '@/hooks/usePropertyHooks';
import { PropertyForm } from '@/components/properties/PropertyForm';
import { CreatePropertyRequest } from '@/types/property';
import { ArrowLeft } from 'lucide-react';

export const PropertyCreatePage = () => {
  const navigate = useNavigate();
  const createPropertyMutation = useCreateProperty();

  const handleSubmit = async (data: CreatePropertyRequest) => {
    await createPropertyMutation.mutateAsync(data);
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/properties')}
            className="p-2 hover:bg-gray-200 dark:hover:bg-gray-700 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Add New Property
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <PropertyForm
            onSubmit={handleSubmit}
            isLoading={createPropertyMutation.isPending}
          />
        </div>
      </div>
    </div>
  );
};
