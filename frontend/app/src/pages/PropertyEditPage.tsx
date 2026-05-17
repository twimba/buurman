import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
import { LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const PropertyEditPage = () => {
  const { t } = useTranslation('properties');
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: property, isLoading, error } = useProperty(id);
  const updatePropertyMutation = useUpdateProperty(id);

  // Characteristics sub-resources
  const { data: outdoorAreas = [] } = useOutdoorAreas(id);
  const createOutdoorAreaMutation = useCreateOutdoorArea(id);
  const deleteOutdoorAreaMutation = useDeleteOutdoorArea(id);
  const { data: allAmenities = {} } = useAmenities(property?.propertyCategory);
  const { data: propertyAmenities = [] } = usePropertyAmenities(id);
  const addAmenityMutation = useAddPropertyAmenity(id);
  const removeAmenityMutation = useRemovePropertyAmenity(id);

  const handleSubmit = async (data: UpdatePropertyRequest) => {
    await updatePropertyMutation.mutateAsync(data);
  };

  if (isLoading) {
    return (
      <div className="min-h-[100dvh] bg-surface-page flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="min-h-[100dvh] bg-surface-page p-8">
        <ErrorMessage message={t('edit.notFound')} />
      </div>
    );
  }

  return (
    <div className="min-h-[100dvh] bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/properties/${id}`)}
            className="p-2 hover:bg-surface-inset rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">
            {t('edit.title')}
          </h1>
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
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
