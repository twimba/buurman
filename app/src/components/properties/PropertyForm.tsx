import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  PropertyResponse,
  CreatePropertyRequest,
  PropertyType,
  PropertyStatus,
  OutdoorAreaResponse,
  AmenityResponse,
  PropertyAmenityResponse,
} from '@/types/property';
import { PropertyMap } from './PropertyMap';
import { PropertyCharacteristicsForm } from './PropertyCharacteristicsForm';
import { CountrySelector } from '../common/CountrySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useGeocode } from '@/hooks/useGeocodingHooks';

interface PropertyFormProps {
  property?: PropertyResponse;
  onSubmit: (data: CreatePropertyRequest) => Promise<void>;
  isLoading: boolean;
  // Characteristics sub-resources (edit mode only)
  outdoorAreas?: OutdoorAreaResponse[];
  onCreateOutdoorArea?: (area: {
    type: string;
    areaValue: number | null;
    areaUnit?: string;
  }) => void;
  onDeleteOutdoorArea?: (id: string) => void;
  allAmenities?: Record<string, AmenityResponse[]>;
  propertyAmenities?: PropertyAmenityResponse[];
  onAddAmenity?: (amenityIdentifier: string, notes?: string | null) => void;
  onRemoveAmenity?: (amenityIdentifier: string) => void;
}

export const PropertyForm = ({
  property,
  onSubmit,
  isLoading,
  outdoorAreas,
  onCreateOutdoorArea,
  onDeleteOutdoorArea,
  allAmenities,
  propertyAmenities,
  onAddAmenity,
  onRemoveAmenity,
}: PropertyFormProps) => {
  const navigate = useNavigate();
  const { defaultCountry } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const geocodeMutation = useGeocode();

  const [formData, setFormData] = useState<CreatePropertyRequest>({
    street: property?.street || '',
    city: property?.city || '',
    postalCode: property?.postalCode || '',
    country: property?.country || defaultCountry || '',
    latitude: property?.latitude || null,
    longitude: property?.longitude || null,
    bedrooms: property?.bedrooms || null,
    bathrooms: property?.bathrooms || null,
    areaValue: property?.areaValue || null,
    areaUnit: property?.areaUnit || 'sqm',
    propertyType:
      (property?.propertyType as PropertyType) || PropertyType.APARTMENT,
    status: (property?.status as PropertyStatus) || PropertyStatus.VACANT,
    // Characteristics
    yearBuilt: property?.yearBuilt ?? null,
    yearLastRenovated: property?.yearLastRenovated ?? null,
    constructionType: property?.constructionType ?? null,
    foundationType: property?.foundationType ?? null,
    roofType: property?.roofType ?? null,
    wallConstruction: property?.wallConstruction ?? null,
    flooringType: property?.flooringType ?? null,
    windowType: property?.windowType ?? null,
    numberOfFloors: property?.numberOfFloors ?? null,
    structuralNotes: property?.structuralNotes ?? null,
    energyEfficiencyRating: property?.energyEfficiencyRating ?? null,
    energyCertificateExpiryDate: property?.energyCertificateExpiryDate ?? null,
    heatingType: property?.heatingType ?? null,
    coolingType: property?.coolingType ?? null,
    hotWaterSystem: property?.hotWaterSystem ?? null,
    insulationNotes: property?.insulationNotes ?? null,
    electricityConnectionType: property?.electricityConnectionType ?? null,
    electricityCapacityAmps: property?.electricityCapacityAmps ?? null,
    waterConnectionType: property?.waterConnectionType ?? null,
    hasGasConnection: property?.hasGasConnection ?? false,
    sewageType: property?.sewageType ?? null,
    internetConnectionType: property?.internetConnectionType ?? null,
    internetMaxSpeedMbps: property?.internetMaxSpeedMbps ?? null,
    internetStatus: property?.internetStatus ?? null,
    parkingSpaces: property?.parkingSpaces ?? null,
    parkingType: property?.parkingType ?? null,
    hasSmokeDetectors: property?.hasSmokeDetectors ?? false,
    hasCoDetectors: property?.hasCoDetectors ?? false,
    hasFireExtinguisher: property?.hasFireExtinguisher ?? false,
    hasSprinklerSystem: property?.hasSprinklerSystem ?? false,
    hasAlarmSystem: property?.hasAlarmSystem ?? false,
    hasSecurityCameras: property?.hasSecurityCameras ?? false,
    hasSecureEntry: property?.hasSecureEntry ?? false,
    safetyNotes: property?.safetyNotes ?? null,
    isWheelchairAccessible: property?.isWheelchairAccessible ?? false,
    hasElevator: property?.hasElevator ?? false,
    hasStepFreeEntrance: property?.hasStepFreeEntrance ?? false,
    hasAdaptedBathroom: property?.hasAdaptedBathroom ?? false,
    accessibilityNotes: property?.accessibilityNotes ?? null,
  });

  const [propertyIdentifier, setPropertyIdentifier] = useState(
    property?.identifier
  );

  useEffect(() => {
    // Only update if property identifier changed (editing a different property)

    if (property && property.identifier !== propertyIdentifier) {
      setPropertyIdentifier(property.identifier);
      setFormData({
        street: property.street,
        city: property.city,
        postalCode: property.postalCode,
        country: property.country,
        latitude: property.latitude,
        longitude: property.longitude,
        bedrooms: property.bedrooms,
        bathrooms: property.bathrooms,
        areaValue: property.areaValue,
        areaUnit: property.areaUnit || 'sqm',
        propertyType: property.propertyType as PropertyType,
        status: property.status as PropertyStatus,
        yearBuilt: property.yearBuilt ?? null,
        yearLastRenovated: property.yearLastRenovated ?? null,
        constructionType: property.constructionType ?? null,
        foundationType: property.foundationType ?? null,
        roofType: property.roofType ?? null,
        wallConstruction: property.wallConstruction ?? null,
        flooringType: property.flooringType ?? null,
        windowType: property.windowType ?? null,
        numberOfFloors: property.numberOfFloors ?? null,
        structuralNotes: property.structuralNotes ?? null,
        energyEfficiencyRating: property.energyEfficiencyRating ?? null,
        energyCertificateExpiryDate:
          property.energyCertificateExpiryDate ?? null,
        heatingType: property.heatingType ?? null,
        coolingType: property.coolingType ?? null,
        hotWaterSystem: property.hotWaterSystem ?? null,
        insulationNotes: property.insulationNotes ?? null,
        electricityConnectionType: property.electricityConnectionType ?? null,
        electricityCapacityAmps: property.electricityCapacityAmps ?? null,
        waterConnectionType: property.waterConnectionType ?? null,
        hasGasConnection: property.hasGasConnection ?? false,
        sewageType: property.sewageType ?? null,
        internetConnectionType: property.internetConnectionType ?? null,
        internetMaxSpeedMbps: property.internetMaxSpeedMbps ?? null,
        internetStatus: property.internetStatus ?? null,
        parkingSpaces: property.parkingSpaces ?? null,
        parkingType: property.parkingType ?? null,
        hasSmokeDetectors: property.hasSmokeDetectors ?? false,
        hasCoDetectors: property.hasCoDetectors ?? false,
        hasFireExtinguisher: property.hasFireExtinguisher ?? false,
        hasSprinklerSystem: property.hasSprinklerSystem ?? false,
        hasAlarmSystem: property.hasAlarmSystem ?? false,
        hasSecurityCameras: property.hasSecurityCameras ?? false,
        hasSecureEntry: property.hasSecureEntry ?? false,
        safetyNotes: property.safetyNotes ?? null,
        isWheelchairAccessible: property.isWheelchairAccessible ?? false,
        hasElevator: property.hasElevator ?? false,
        hasStepFreeEntrance: property.hasStepFreeEntrance ?? false,
        hasAdaptedBathroom: property.hasAdaptedBathroom ?? false,
        accessibilityNotes: property.accessibilityNotes ?? null,
      });
    }
  }, [property, propertyIdentifier]);

  // Debounce address changes for geocoding via backend (2 seconds)
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (formData.street && formData.city && formData.country) {
        const hasChanged = property
          ? formData.street !== property.street ||
            formData.city !== property.city ||
            formData.postalCode !== property.postalCode ||
            formData.country !== property.country
          : true;

        if (hasChanged) {
          geocodeMutation.mutate(
            {
              street: formData.street,
              city: formData.city,
              postalCode: formData.postalCode,
              country: formData.country,
            },
            {
              onSuccess: (result) => {
                if (result) {
                  setFormData((prev) => ({
                    ...prev,
                    latitude: result.latitude,
                    longitude: result.longitude,
                  }));
                }
              },
            }
          );
        }
      }
    }, 2000);

    return () => clearTimeout(timeoutId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    formData.street,
    formData.city,
    formData.postalCode,
    formData.country,
    property,
  ]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.street.trim()) newErrors.street = 'Street is required';
    if (!formData.city.trim()) newErrors.city = 'City is required';
    if (!formData.postalCode.trim())
      newErrors.postalCode = 'Postal code is required';
    if (!formData.country.trim()) newErrors.country = 'Country is required';

    if (formData.bedrooms !== null && formData.bedrooms < 0) {
      newErrors.bedrooms = 'Bedrooms must be non-negative';
    }
    if (formData.bathrooms !== null && formData.bathrooms < 0) {
      newErrors.bathrooms = 'Bathrooms must be non-negative';
    }
    if (
      formData.areaValue !== null &&
      formData.areaValue !== undefined &&
      formData.areaValue <= 0
    ) {
      newErrors.areaValue = 'Area must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      await onSubmit(formData);
      // Navigate to property detail page if editing, otherwise to list
      if (property) {
        navigate(`/properties/${property.identifier}`);
      } else {
        navigate('/properties');
      }
    } catch (error) {
      console.error('Failed to save property:', error);
    }
  };

  const handleChange = (
    field: keyof CreatePropertyRequest,
    value: string | number | boolean | null | unknown
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field as string]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Address Section */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Address
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Street <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.street}
              onChange={(e) => handleChange('street', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="Main Street 123"
            />
            {errors.street && (
              <p className="text-red-600 text-sm mt-1">{errors.street}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              City <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.city}
              onChange={(e) => handleChange('city', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="Amsterdam"
            />
            {errors.city && (
              <p className="text-red-600 text-sm mt-1">{errors.city}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Postal Code <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.postalCode}
              onChange={(e) => handleChange('postalCode', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1012 AB"
            />
            {errors.postalCode && (
              <p className="text-red-600 text-sm mt-1">{errors.postalCode}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Country <span className="text-red-500">*</span>
            </label>
            <CountrySelector
              value={formData.country}
              onChange={(v) => handleChange('country', v)}
            />
            {errors.country && (
              <p className="text-red-600 text-sm mt-1">{errors.country}</p>
            )}
          </div>
        </div>

        {/* Location Preview */}
        {formData.street && formData.city && formData.country && (
          <div className="mt-6">
            <h4 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-3">
              Location Preview
            </h4>
            <PropertyMap
              street={formData.street}
              city={formData.city}
              latitude={formData.latitude}
              longitude={formData.longitude}
            />
          </div>
        )}
      </div>

      {/* Specifications Section */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Specifications
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Bedrooms
            </label>
            <input
              type="number"
              min="0"
              value={formData.bedrooms ?? ''}
              onChange={(e) =>
                handleChange(
                  'bedrooms',
                  e.target.value ? parseInt(e.target.value) : null
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="2"
            />
            {errors.bedrooms && (
              <p className="text-red-600 text-sm mt-1">{errors.bedrooms}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Bathrooms
            </label>
            <input
              type="number"
              min="0"
              value={formData.bathrooms ?? ''}
              onChange={(e) =>
                handleChange(
                  'bathrooms',
                  e.target.value ? parseInt(e.target.value) : null
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1"
            />
            {errors.bathrooms && (
              <p className="text-red-600 text-sm mt-1">{errors.bathrooms}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Area
            </label>
            <div className="flex gap-2">
              <input
                type="number"
                min="0"
                step="0.01"
                value={formData.areaValue ?? ''}
                onChange={(e) =>
                  handleChange(
                    'areaValue',
                    e.target.value ? parseFloat(e.target.value) : null
                  )
                }
                className="flex-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
                placeholder="75.5"
              />
              <select
                value={formData.areaUnit ?? 'sqm'}
                onChange={(e) => handleChange('areaUnit', e.target.value)}
                className="w-20 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-2 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              >
                <option value="sqm">m²</option>
                <option value="sqft">ft²</option>
              </select>
            </div>
            {errors.areaValue && (
              <p className="text-red-600 text-sm mt-1">{errors.areaValue}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Property Type <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.propertyType}
              onChange={(e) =>
                handleChange('propertyType', e.target.value as PropertyType)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            >
              <option value={PropertyType.APARTMENT}>Apartment</option>
              <option value={PropertyType.HOUSE}>House</option>
              <option value={PropertyType.STUDIO}>Studio</option>
              <option value={PropertyType.COMMERCIAL}>Commercial</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Status <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.status}
              onChange={(e) =>
                handleChange('status', e.target.value as PropertyStatus)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            >
              <option value={PropertyStatus.VACANT}>Vacant</option>
              <option value={PropertyStatus.OCCUPIED}>Occupied</option>
              <option value={PropertyStatus.MAINTENANCE}>Maintenance</option>
              <option value={PropertyStatus.UNAVAILABLE}>Unavailable</option>
            </select>
          </div>
        </div>
      </div>

      {/* Property Characteristics */}
      <PropertyCharacteristicsForm
        formData={formData}
        property={property}
        onChange={handleChange}
        outdoorAreas={outdoorAreas}
        onCreateOutdoorArea={onCreateOutdoorArea}
        onDeleteOutdoorArea={onDeleteOutdoorArea}
        allAmenities={allAmenities}
        propertyAmenities={propertyAmenities}
        onAddAmenity={onAddAmenity}
        onRemoveAmenity={onRemoveAmenity}
      />

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <button
          type="button"
          onClick={() => navigate('/properties')}
          className="border border-[#c9cfd9] dark:border-[#3a3f54] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading
            ? 'Saving...'
            : property
              ? 'Update Property'
              : 'Create Property'}
        </button>
      </div>
    </form>
  );
};
