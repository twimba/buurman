import { useParams, useNavigate } from 'react-router-dom';
import { useProperty, useUpdateProperty } from '@/hooks/usePropertyHooks';
import { PropertyForm } from '@/components/properties/PropertyForm';
import { UpdatePropertyRequest } from '@/types/property';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const PropertyEditPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: property, isLoading, error } = useProperty(id);
  const updatePropertyMutation = useUpdateProperty(id!);

  const handleSubmit = async (data: UpdatePropertyRequest) => {
    await updatePropertyMutation.mutateAsync(data);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
        <ErrorMessage message="Property not found" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/properties/${id}`)}
            className="p-2 hover:bg-gray-200 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Edit Property
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white rounded-lg shadow p-6">
          <PropertyForm
            property={property}
            onSubmit={handleSubmit}
            isLoading={updatePropertyMutation.isPending}
          />
        </div>
      </div>
    </div>
  );
};
