import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  PropertyResponse,
  CreatePropertyRequest,
  PropertyType,
  PropertyStatus,
  PropertyCategory,
  OutdoorAreaResponse,
  AmenityResponse,
  PropertyAmenityResponse,
  PROPERTY_TYPES_BY_CATEGORY,
  PROPERTY_CATEGORY_LABELS,
  PROPERTY_TYPE_LABELS,
  PROPERTY_STATUS_LABELS,
} from '@/types/property';
import { InteractiveMap } from '../common/InteractiveMap';
import { PropertyCharacteristicsForm } from './PropertyCharacteristicsForm';
import { CountrySelector } from '../common/CountrySelector';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useGeocode } from '@/hooks/useGeocodingHooks';
import { IconSelect } from '../common/IconSelect';
import type { IconSelectOption } from '../common/IconSelect';
import {
  PROPERTY_CATEGORY_ICONS,
  PROPERTY_TYPE_ICONS,
} from '@/utils/propertyIcons';

interface PropertyFormProps {
  property?: PropertyResponse;
  onSubmit: (data: CreatePropertyRequest) => Promise<void>;
  isLoading: boolean;
  // Characteristics sub-resources (edit mode only)
  outdoorAreas?: OutdoorAreaResponse[];
  onCreateOutdoorArea?: (area: {
    type: string;
    areaValue?: number;
    areaUnit?: string;
  }) => void;
  onDeleteOutdoorArea?: (id: string) => void;
  allAmenities?: Record<string, AmenityResponse[]>;
  propertyAmenities?: PropertyAmenityResponse[];
  onAddAmenity?: (amenityIdentifier: string, notes?: string | null) => void;
  onRemoveAmenity?: (amenityIdentifier: string) => void;
}

const selectCls =
  'w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]';
const inputCls = selectCls;
const labelCls =
  'block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1';

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
  const [addressDirty, setAddressDirty] = useState(false);
  const isEditMode = !!property;

  const resolveCategory = (): PropertyCategory =>
    (property?.propertyCategory as PropertyCategory) ??
    PropertyCategory.RESIDENTIAL;

  const resolveType = (): PropertyType => {
    if (property?.propertyType) {
      return property.propertyType as PropertyType;
    }
    const types = PROPERTY_TYPES_BY_CATEGORY[PropertyCategory.RESIDENTIAL];
    return types[0];
  };

  const [formData, setFormData] = useState<CreatePropertyRequest>({
    propertyCategory: resolveCategory(),
    propertyType: resolveType(),
    status: (property?.status as PropertyStatus) ?? PropertyStatus.VACANT,
    street: property?.street ?? '',
    city: property?.city ?? '',
    postalCode: property?.postalCode ?? '',
    country: property?.country || defaultCountry || '',
    latitude: property?.latitude ?? undefined,
    longitude: property?.longitude ?? undefined,
    geocodeAccuracy: property?.geocodeAccuracy ?? undefined,
    areaValue: property?.areaValue ?? undefined,
    areaUnit: property?.areaUnit ?? 'sqm',
    // Characteristics
    yearBuilt: property?.yearBuilt,
    yearLastRenovated: property?.yearLastRenovated,
    constructionType: property?.constructionType,
    foundationType: property?.foundationType,
    roofType: property?.roofType,
    wallConstruction: property?.wallConstruction,
    flooringType: property?.flooringType,
    windowType: property?.windowType,
    numberOfFloors: property?.numberOfFloors,
    structuralNotes: property?.structuralNotes,
    energyEfficiencyRating: property?.energyEfficiencyRating,
    energyCertificateExpiryDate: property?.energyCertificateExpiryDate,
    heatingType: property?.heatingType,
    coolingType: property?.coolingType,
    hotWaterSystem: property?.hotWaterSystem,
    insulationNotes: property?.insulationNotes,
    electricityConnectionType: property?.electricityConnectionType,
    electricityCapacityAmps: property?.electricityCapacityAmps,
    waterConnectionType: property?.waterConnectionType,
    hasGasConnection: property?.hasGasConnection ?? false,
    sewageType: property?.sewageType,
    internetConnectionType: property?.internetConnectionType,
    internetMaxSpeedMbps: property?.internetMaxSpeedMbps,
    internetStatus: property?.internetStatus,
    parkingSpaces: property?.parkingSpaces,
    parkingType: property?.parkingType,
    hasSmokeDetectors: property?.hasSmokeDetectors ?? false,
    hasCoDetectors: property?.hasCoDetectors ?? false,
    hasFireExtinguisher: property?.hasFireExtinguisher ?? false,
    hasSprinklerSystem: property?.hasSprinklerSystem ?? false,
    hasAlarmSystem: property?.hasAlarmSystem ?? false,
    hasSecurityCameras: property?.hasSecurityCameras ?? false,
    hasSecureEntry: property?.hasSecureEntry ?? false,
    safetyNotes: property?.safetyNotes,
    isWheelchairAccessible: property?.isWheelchairAccessible ?? false,
    hasElevator: property?.hasElevator ?? false,
    hasStepFreeEntrance: property?.hasStepFreeEntrance ?? false,
    hasAdaptedBathroom: property?.hasAdaptedBathroom ?? false,
    accessibilityNotes: property?.accessibilityNotes,
    // Category-specific details
    residentialDetails:
      property?.residentialDetails as CreatePropertyRequest['residentialDetails'],
    commercialDetails:
      property?.commercialDetails as CreatePropertyRequest['commercialDetails'],
    industrialDetails:
      property?.industrialDetails as CreatePropertyRequest['industrialDetails'],
    agriculturalDetails:
      property?.agriculturalDetails as CreatePropertyRequest['agriculturalDetails'],
  });

  const [propertyIdentifier, setPropertyIdentifier] = useState(
    property?.identifier
  );

  // Available sub-types based on selected category
  const availableTypes = useMemo(
    () =>
      PROPERTY_TYPES_BY_CATEGORY[
        formData.propertyCategory ?? PropertyCategory.RESIDENTIAL
      ] ?? [],
    [formData.propertyCategory]
  );

  const categoryOptions = useMemo<IconSelectOption[]>(
    () =>
      Object.values(PropertyCategory).map((cat) => ({
        value: cat,
        label: PROPERTY_CATEGORY_LABELS[cat],
        Icon: PROPERTY_CATEGORY_ICONS[cat],
      })),
    []
  );

  const typeOptions = useMemo<IconSelectOption[]>(
    () =>
      availableTypes.map((type) => ({
        value: type,
        label: PROPERTY_TYPE_LABELS[type] ?? type,
        Icon: PROPERTY_TYPE_ICONS[type],
      })),
    [availableTypes]
  );

  useEffect(() => {
    // Only update if property identifier changed (editing a different property)
    if (property && property.identifier !== propertyIdentifier) {
      setPropertyIdentifier(property.identifier);
      setFormData({
        propertyCategory:
          (property.propertyCategory as PropertyCategory) ??
          PropertyCategory.RESIDENTIAL,
        propertyType: property.propertyType as PropertyType,
        status: property.status as PropertyStatus,
        street: property.street,
        city: property.city,
        postalCode: property.postalCode,
        country: property.country,
        latitude: property.latitude,
        longitude: property.longitude,
        geocodeAccuracy: property.geocodeAccuracy,
        areaValue: property.areaValue,
        areaUnit: property.areaUnit ?? 'sqm',
        yearBuilt: property.yearBuilt,
        yearLastRenovated: property.yearLastRenovated,
        constructionType: property.constructionType,
        foundationType: property.foundationType,
        roofType: property.roofType,
        wallConstruction: property.wallConstruction,
        flooringType: property.flooringType,
        windowType: property.windowType,
        numberOfFloors: property.numberOfFloors,
        structuralNotes: property.structuralNotes,
        energyEfficiencyRating: property.energyEfficiencyRating,
        energyCertificateExpiryDate: property.energyCertificateExpiryDate,
        heatingType: property.heatingType,
        coolingType: property.coolingType,
        hotWaterSystem: property.hotWaterSystem,
        insulationNotes: property.insulationNotes,
        electricityConnectionType: property.electricityConnectionType,
        electricityCapacityAmps: property.electricityCapacityAmps,
        waterConnectionType: property.waterConnectionType,
        hasGasConnection: property.hasGasConnection ?? false,
        sewageType: property.sewageType,
        internetConnectionType: property.internetConnectionType,
        internetMaxSpeedMbps: property.internetMaxSpeedMbps,
        internetStatus: property.internetStatus,
        parkingSpaces: property.parkingSpaces,
        parkingType: property.parkingType,
        hasSmokeDetectors: property.hasSmokeDetectors ?? false,
        hasCoDetectors: property.hasCoDetectors ?? false,
        hasFireExtinguisher: property.hasFireExtinguisher ?? false,
        hasSprinklerSystem: property.hasSprinklerSystem ?? false,
        hasAlarmSystem: property.hasAlarmSystem ?? false,
        hasSecurityCameras: property.hasSecurityCameras ?? false,
        hasSecureEntry: property.hasSecureEntry ?? false,
        safetyNotes: property.safetyNotes,
        isWheelchairAccessible: property.isWheelchairAccessible ?? false,
        hasElevator: property.hasElevator ?? false,
        hasStepFreeEntrance: property.hasStepFreeEntrance ?? false,
        hasAdaptedBathroom: property.hasAdaptedBathroom ?? false,
        accessibilityNotes: property.accessibilityNotes,
        residentialDetails:
          property.residentialDetails as CreatePropertyRequest['residentialDetails'],
        commercialDetails:
          property.commercialDetails as CreatePropertyRequest['commercialDetails'],
        industrialDetails:
          property.industrialDetails as CreatePropertyRequest['industrialDetails'],
        agriculturalDetails:
          property.agriculturalDetails as CreatePropertyRequest['agriculturalDetails'],
      });
    }
  }, [property, propertyIdentifier]);

  // Debounce address changes for geocoding via backend (2 seconds)
  useEffect(() => {
    if (formData.street && formData.city && formData.country) {
      const hasChanged = property
        ? formData.street !== property.street ||
          formData.city !== property.city ||
          formData.postalCode !== property.postalCode ||
          formData.country !== property.country
        : true;

      if (hasChanged) {
        setAddressDirty(true);
      }
    }

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
                setAddressDirty(false);
                if (result) {
                  setFormData((prev) => ({
                    ...prev,
                    latitude: result.latitude,
                    longitude: result.longitude,
                    geocodeAccuracy: result.accuracy ?? undefined,
                  }));
                } else {
                  setFormData((prev) => ({
                    ...prev,
                    latitude: undefined,
                    longitude: undefined,
                    geocodeAccuracy: undefined,
                  }));
                }
              },
              onError: () => {
                setAddressDirty(false);
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

  const handleCategoryChange = (category: PropertyCategory) => {
    const types = PROPERTY_TYPES_BY_CATEGORY[category];
    setFormData((prev) => ({
      ...prev,
      propertyCategory: category,
      propertyType: types[0],
      // Clear detail objects when switching category
      residentialDetails: undefined,
      commercialDetails: undefined,
      industrialDetails: undefined,
      agriculturalDetails: undefined,
    }));
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.street.trim()) {
      newErrors.street = 'Street is required';
    }
    if (!formData.city.trim()) {
      newErrors.city = 'City is required';
    }
    if (!formData.postalCode.trim()) {
      newErrors.postalCode = 'Postal code is required';
    }
    if (!formData.country.trim()) {
      newErrors.country = 'Country is required';
    }

    if (formData.areaValue != null && formData.areaValue <= 0) {
      newErrors.areaValue = 'Area must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }

    try {
      await onSubmit(formData);
      if (property) {
        navigate(`/properties/${property.identifier}`);
      } else {
        navigate('/properties');
      }
    } catch (error) {
      console.error('Failed to save property:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  const handleChange = (
    field: keyof CreatePropertyRequest,
    value: string | number | boolean | undefined | unknown
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field as string]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-6"
    >
      {/* Address Section */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Address
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>
              Street <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.street}
              onChange={(e) => handleChange('street', e.target.value)}
              className={inputCls}
              placeholder="Main Street 123"
            />
            {errors.street && (
              <p className="text-red-600 text-sm mt-1">{errors.street}</p>
            )}
          </div>

          <div>
            <label className={labelCls}>
              City <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.city}
              onChange={(e) => handleChange('city', e.target.value)}
              className={inputCls}
              placeholder="Amsterdam"
            />
            {errors.city && (
              <p className="text-red-600 text-sm mt-1">{errors.city}</p>
            )}
          </div>

          <div>
            <label className={labelCls}>
              Postal Code <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.postalCode}
              onChange={(e) => handleChange('postalCode', e.target.value)}
              className={inputCls}
              placeholder="1012 AB"
            />
            {errors.postalCode && (
              <p className="text-red-600 text-sm mt-1">{errors.postalCode}</p>
            )}
          </div>

          <div>
            <label className={labelCls}>
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
            <InteractiveMap
              street={formData.street}
              city={formData.city}
              latitude={formData.latitude}
              longitude={formData.longitude}
              geocodeAccuracy={formData.geocodeAccuracy}
              isGeocoding={addressDirty || geocodeMutation.isPending}
              defaultCountry={formData.country || defaultCountry}
              onLocationChange={(lat, lng) => {
                setFormData((prev) => ({
                  ...prev,
                  latitude: lat,
                  longitude: lng,
                  geocodeAccuracy: 'MANUAL',
                }));
              }}
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
          {/* Property Category */}
          <div>
            <label className={labelCls}>
              Category <span className="text-red-500">*</span>
            </label>
            {isEditMode ? (
              <div className="px-3 py-2 bg-[#f1f3f9] dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded flex items-center gap-2 text-[#3d4463] dark:text-[#c4c8db]">
                {(() => {
                  const cat = formData.propertyCategory ?? PropertyCategory.RESIDENTIAL;
                  const CatIcon = PROPERTY_CATEGORY_ICONS[cat];
                  return CatIcon ? (
                    <CatIcon size={14} className="text-[#9ca0b8]" />
                  ) : null;
                })()}
                {PROPERTY_CATEGORY_LABELS[formData.propertyCategory ?? PropertyCategory.RESIDENTIAL]}
                <span className="text-xs text-[#9ca0b8] ml-1">
                  (cannot be changed)
                </span>
              </div>
            ) : (
              <IconSelect
                value={formData.propertyCategory ?? ''}
                options={categoryOptions}
                onChange={(v) => handleCategoryChange(v as PropertyCategory)}
              />
            )}
          </div>

          {/* Property Type (filtered by category) */}
          <div>
            <label className={labelCls}>
              Type <span className="text-red-500">*</span>
            </label>
            <IconSelect
              value={formData.propertyType ?? ''}
              options={typeOptions}
              onChange={(v) => handleChange('propertyType', v as PropertyType)}
            />
          </div>

          {/* Status */}
          <div>
            <label className={labelCls}>
              Status <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.status}
              onChange={(e) =>
                handleChange('status', e.target.value as PropertyStatus)
              }
              className={selectCls}
            >
              {Object.values(PropertyStatus).map((status) => (
                <option key={status} value={status}>
                  {PROPERTY_STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </div>

          {/* Area */}
          <div>
            <label className={labelCls}>Area</label>
            <div className="flex gap-2">
              <input
                type="number"
                min="0"
                step="0.01"
                value={formData.areaValue ?? ''}
                onChange={(e) =>
                  handleChange(
                    'areaValue',
                    e.target.value ? parseFloat(e.target.value) : undefined
                  )
                }
                className={`flex-1 ${inputCls}`}
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
