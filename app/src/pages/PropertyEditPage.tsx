import { useParams, useNavigate } from 'react-router-dom';
import {
  useProperty,
  useUpdateProperty,
  useOutdoorAreas,
  useCreateOutdoorArea,
  useDeleteOutdoorArea,
  useAmenities,
  usePropertyAmenities,
  useAddPropertyAmenity,
  useRemovePropertyAmenity,
} from '@/hooks/usePropertyHooks';
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

  // Characteristics sub-resources
  const { data: outdoorAreas = [] } = useOutdoorAreas(id);
  const createOutdoorAreaMutation = useCreateOutdoorArea(id!);
  const deleteOutdoorAreaMutation = useDeleteOutdoorArea(id!);
  const { data: allAmenities = {} } = useAmenities();
  const { data: propertyAmenities = [] } = usePropertyAmenities(id);
  const addAmenityMutation = useAddPropertyAmenity(id!);
  const removeAmenityMutation = useRemovePropertyAmenity(id!);

  const handleSubmit = async (data: UpdatePropertyRequest) => {
    await updatePropertyMutation.mutateAsync(data);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14] flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14] p-8">
        <ErrorMessage message="Property not found" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f8f9fc] dark:bg-[#0c0d14]">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/properties/${id}`)}
            className="p-2 hover:bg-[#e8ecf4] dark:bg-[#1e2130] rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Edit Property
          </h1>
        </div>

        {/* Form */}
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
          <PropertyForm
            property={property}
            onSubmit={handleSubmit}
            isLoading={updatePropertyMutation.isPending}
            outdoorAreas={outdoorAreas}
            onCreateOutdoorArea={(area) =>
              createOutdoorAreaMutation.mutate(area)
            }
            onDeleteOutdoorArea={(areaId) =>
              deleteOutdoorAreaMutation.mutate(areaId)
            }
            allAmenities={allAmenities}
            propertyAmenities={propertyAmenities}
            onAddAmenity={(amenityIdentifier, notes) =>
              addAmenityMutation.mutate({ amenityIdentifier, notes })
            }
            onRemoveAmenity={(amenityIdentifier) =>
              removeAmenityMutation.mutate(amenityIdentifier)
            }
          />
        </div>
      </div>
    </div>
  );
};
