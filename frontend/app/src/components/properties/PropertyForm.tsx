import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
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
} from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { InteractiveMap } from '../common/InteractiveMap';
import { PropertyCharacteristicsForm } from './PropertyCharacteristicsForm';
import { CountrySelector } from '../common/CountrySelector';
import {
  FormStepGate,
  MobileFormStepperFooter,
  MobileFormStepperProvider,
  MobileStepperHeader,
} from '../common/MobileFormStepper';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useGeocode } from '@/hooks/useGeocodingHooks';
import { IconSelect } from '../common/IconSelect';
import type { IconSelectOption } from '../common/IconSelect';
import {
  PROPERTY_CATEGORY_ICONS,
  PROPERTY_TYPE_ICONS,
} from '@/utils/propertyIcons';
import { getCountries, getCountryDetail } from '@/api/rentRegulations';

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
  'w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500';
const inputCls = selectCls;
const labelCls = 'block text-sm font-medium text-text-secondary mb-1';

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
  const { t } = useTranslation('properties');
  const navigate = useNavigate();
  const { defaultCountryCode } = useTeamDefaults();
  const { statusLabel, typeLabel, categoryLabel } = usePropertyLabels();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const geocodeMutation = useGeocode();
  const [geocodedAddress, setGeocodedAddress] = useState(() =>
    property
      ? `${property.street}|${property.city}|${property.postalCode}|${property.countryCode}`
      : ''
  );
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
    countryCode: property?.countryCode || defaultCountryCode || '',
    regionCode: property?.regionCode,
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
    electricityCapacityValue: property?.electricityCapacityValue,
    electricityCapacityUnit: property?.electricityCapacityUnit ?? 'a',
    waterConnectionType: property?.waterConnectionType,
    hasGasConnection: property?.hasGasConnection ?? false,
    sewageType: property?.sewageType,
    internetConnectionType: property?.internetConnectionType,
    internetMaxSpeedValue: property?.internetMaxSpeedValue,
    internetMaxSpeedUnit: property?.internetMaxSpeedUnit ?? 'mbps',
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

  // Fetch rent regulation countries to know which ones have regional regulations
  const { data: regulationCountries } = useQuery({
    queryKey: ['rentRegulationCountries'],
    queryFn: getCountries,
    staleTime: 5 * 60 * 1000,
  });

  const selectedRegCountry = regulationCountries?.find(
    (c) => c.countryCode === formData.countryCode
  );
  const hasRegions = selectedRegCountry?.hasRegionalRegulations ?? false;

  // Fetch regions for the selected country (only if it has regional regulations)
  const { data: countryDetail } = useQuery({
    queryKey: ['rentRegulationCountryDetail', formData.countryCode],
    queryFn: () => getCountryDetail(formData.countryCode),
    enabled: hasRegions && !!formData.countryCode,
    staleTime: 5 * 60 * 1000,
  });

  const availableRegions = countryDetail?.regions ?? [];

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
        label: categoryLabel(cat),
        Icon: PROPERTY_CATEGORY_ICONS[cat],
      })),
    [categoryLabel]
  );

  const typeOptions = useMemo<IconSelectOption[]>(
    () =>
      availableTypes.map((type) => ({
        value: type,
        label: typeLabel(type),
        Icon: PROPERTY_TYPE_ICONS[type],
      })),
    [availableTypes, typeLabel]
  );

  // Sync form state when a different property is loaded (render-time derived state update)
  if (property && property.identifier !== propertyIdentifier) {
    setPropertyIdentifier(property.identifier);
    setGeocodedAddress(
      `${property.street}|${property.city}|${property.postalCode}|${property.countryCode}`
    );
    setFormData({
      propertyCategory:
        (property.propertyCategory as PropertyCategory) ??
        PropertyCategory.RESIDENTIAL,
      propertyType: property.propertyType as PropertyType,
      status: property.status as PropertyStatus,
      street: property.street,
      city: property.city,
      postalCode: property.postalCode,
      countryCode: property.countryCode,
      regionCode: property.regionCode,
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
      electricityCapacityValue: property.electricityCapacityValue,
      electricityCapacityUnit: property.electricityCapacityUnit ?? 'a',
      waterConnectionType: property.waterConnectionType,
      hasGasConnection: property.hasGasConnection ?? false,
      sewageType: property.sewageType,
      internetConnectionType: property.internetConnectionType,
      internetMaxSpeedValue: property.internetMaxSpeedValue,
      internetMaxSpeedUnit: property.internetMaxSpeedUnit ?? 'mbps',
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

  const currentAddressKey = `${formData.street}|${formData.city}|${formData.postalCode}|${formData.countryCode}`;
  const addressDirty =
    !!(formData.street && formData.city && formData.countryCode) &&
    currentAddressKey !== geocodedAddress;

  // Debounce address changes for geocoding via backend (2 seconds)
  useEffect(() => {
    const addrKey = `${formData.street}|${formData.city}|${formData.postalCode}|${formData.countryCode}`;
    const timeoutId = setTimeout(() => {
      if (formData.street && formData.city && formData.countryCode) {
        const hasChanged = property
          ? formData.street !== property.street ||
            formData.city !== property.city ||
            formData.postalCode !== property.postalCode ||
            formData.countryCode !== property.countryCode
          : true;

        if (hasChanged) {
          geocodeMutation.mutate(
            {
              street: formData.street,
              city: formData.city,
              postalCode: formData.postalCode,
              countryCode: formData.countryCode,
            },
            {
              onSuccess: (result) => {
                setGeocodedAddress(addrKey);
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
                setGeocodedAddress(addrKey);
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
    formData.countryCode,
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
      newErrors.street = t('form.validation.streetRequired');
    }
    if (!formData.city.trim()) {
      newErrors.city = t('form.validation.cityRequired');
    }
    if (!formData.postalCode.trim()) {
      newErrors.postalCode = t('form.validation.postalCodeRequired');
    }
    if (!formData.countryCode.trim()) {
      newErrors.countryCode = t('form.validation.countryRequired');
    }

    if (formData.areaValue != null && formData.areaValue <= 0) {
      newErrors.areaValue = t('form.validation.areaPositive');
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
    <MobileFormStepperProvider totalSteps={3}>
      <form
        onSubmit={handleSubmit}
        onKeyDown={handleCmdEnter}
        className="space-y-6"
      >
        <MobileStepperHeader
          labels={[
            t('form.address'),
            t('form.specifications'),
            t('form.characteristics', { defaultValue: 'Characteristics' }),
          ]}
        />
        {/* Address Section */}
        <FormStepGate step={0}>
          <div>
            <h3 className="text-lg font-semibold text-text-primary mb-4">
              {t('form.address')}
            </h3>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>
                  {t('form.street')} <span className="text-error-text">*</span>
                </label>
                <input
                  type="text"
                  value={formData.street}
                  onChange={(e) => handleChange('street', e.target.value)}
                  className={inputCls}
                  placeholder={t('form.streetPlaceholder')}
                />
                {errors.street && (
                  <p className="text-error-text text-sm mt-1">
                    {errors.street}
                  </p>
                )}
              </div>

              <div>
                <label className={labelCls}>
                  {t('form.city')} <span className="text-error-text">*</span>
                </label>
                <input
                  type="text"
                  value={formData.city}
                  onChange={(e) => handleChange('city', e.target.value)}
                  className={inputCls}
                  placeholder={t('form.cityPlaceholder')}
                />
                {errors.city && (
                  <p className="text-error-text text-sm mt-1">{errors.city}</p>
                )}
              </div>

              <div>
                <label className={labelCls}>
                  {t('form.postalCode')}{' '}
                  <span className="text-error-text">*</span>
                </label>
                <input
                  type="text"
                  value={formData.postalCode}
                  onChange={(e) => handleChange('postalCode', e.target.value)}
                  className={inputCls}
                  placeholder={t('form.postalCodePlaceholder')}
                />
                {errors.postalCode && (
                  <p className="text-error-text text-sm mt-1">
                    {errors.postalCode}
                  </p>
                )}
              </div>

              <div>
                <label className={labelCls}>
                  {t('form.country')} <span className="text-error-text">*</span>
                </label>
                <CountrySelector
                  value={formData.countryCode}
                  onChange={(v) => {
                    setFormData((prev) => ({
                      ...prev,
                      countryCode: v,
                      regionCode: undefined,
                    }));
                    if (errors.countryCode) {
                      setErrors((prev) => ({ ...prev, countryCode: '' }));
                    }
                  }}
                />
                {errors.countryCode && (
                  <p className="text-error-text text-sm mt-1">
                    {errors.countryCode}
                  </p>
                )}
              </div>

              {/* Region selector — only shown when country has regional regulations */}
              {hasRegions && availableRegions.length > 0 && (
                <div>
                  <label className={labelCls}>{t('form.region')}</label>
                  <select
                    value={formData.regionCode ?? ''}
                    onChange={(e) =>
                      handleChange('regionCode', e.target.value || undefined)
                    }
                    className={selectCls}
                  >
                    <option value="">{t('form.regionOther')}</option>
                    {availableRegions.map((r) => (
                      <option key={r.regionCode} value={r.regionCode}>
                        {r.regionName}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>

            {/* Location Preview */}
            {formData.street && formData.city && formData.countryCode && (
              <div className="mt-6">
                <h4 className="text-sm font-semibold text-text-secondary mb-3">
                  {t('form.locationPreview')}
                </h4>
                <InteractiveMap
                  street={formData.street}
                  city={formData.city}
                  latitude={formData.latitude}
                  longitude={formData.longitude}
                  geocodeAccuracy={formData.geocodeAccuracy}
                  isGeocoding={addressDirty || geocodeMutation.isPending}
                  defaultCountryCode={
                    formData.countryCode || defaultCountryCode
                  }
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
        </FormStepGate>

        {/* Specifications Section */}
        <FormStepGate step={1}>
          <div>
            <h3 className="text-lg font-semibold text-text-primary mb-4">
              {t('form.specifications')}
            </h3>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {/* Property Category */}
              <div>
                <label className={labelCls}>
                  {t('form.category')}{' '}
                  <span className="text-error-text">*</span>
                </label>
                {isEditMode ? (
                  <div className="px-3 py-2 bg-surface-inset border border-border-strong rounded flex items-center gap-2 text-text-secondary">
                    {(() => {
                      const cat =
                        formData.propertyCategory ??
                        PropertyCategory.RESIDENTIAL;
                      const CatIcon = PROPERTY_CATEGORY_ICONS[cat];
                      return CatIcon ? (
                        <CatIcon size={14} className="text-text-muted" />
                      ) : null;
                    })()}
                    {categoryLabel(
                      formData.propertyCategory ?? PropertyCategory.RESIDENTIAL
                    )}
                    <span className="text-xs text-text-muted ml-1">
                      {t('form.categoryCannotChange')}
                    </span>
                  </div>
                ) : (
                  <IconSelect
                    value={formData.propertyCategory ?? ''}
                    options={categoryOptions}
                    onChange={(v) =>
                      handleCategoryChange(v as PropertyCategory)
                    }
                  />
                )}
              </div>

              {/* Property Type (filtered by category) */}
              <div>
                <label className={labelCls}>
                  {t('form.type')} <span className="text-error-text">*</span>
                </label>
                <IconSelect
                  value={formData.propertyType ?? ''}
                  options={typeOptions}
                  onChange={(v) =>
                    handleChange('propertyType', v as PropertyType)
                  }
                />
              </div>

              {/* Status */}
              <div>
                <label className={labelCls}>
                  {t('form.status')} <span className="text-error-text">*</span>
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
                      {statusLabel(status)}
                    </option>
                  ))}
                </select>
              </div>

              {/* Area */}
              <div>
                <label className={labelCls}>{t('form.area')}</label>
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
                    placeholder={t('form.areaPlaceholder')}
                  />
                  <select
                    value={formData.areaUnit ?? 'sqm'}
                    onChange={(e) => handleChange('areaUnit', e.target.value)}
                    className="w-20 border border-border-strong rounded px-2 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                  >
                    <option value="sqm">m²</option>
                    <option value="sqft">ft²</option>
                  </select>
                </div>
                {errors.areaValue && (
                  <p className="text-error-text text-sm mt-1">
                    {errors.areaValue}
                  </p>
                )}
              </div>
            </div>
          </div>
        </FormStepGate>

        {/* Property Characteristics */}
        <FormStepGate step={2}>
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
        </FormStepGate>

        {/* Actions — md+ inline save bar (single-scroll desktop UX) */}
        <div className="hidden md:flex gap-2 justify-end mt-6 pt-6 border-t border-border-default">
          <button
            type="button"
            onClick={() => navigate('/properties')}
            className="border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors flex items-center gap-2"
            disabled={isLoading}
          >
            <X className="h-4 w-4" />
            {t('common:buttons.cancel')}
          </button>
          <button
            type="submit"
            className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
            disabled={isLoading}
          >
            <Save className="h-4 w-4" />
            {isLoading
              ? t('form.saving')
              : property
                ? t('form.updateProperty')
                : t('form.createProperty')}
          </button>
        </div>

        {/* Phone: sticky stepper footer with Back/Continue/Save */}
        <MobileFormStepperFooter
          onSubmit={() => {
            // form.requestSubmit() is unavailable in older Safari; trigger
            // submit by relaying to handleSubmit via the form element.
            const f = document.activeElement?.closest(
              'form'
            ) as HTMLFormElement | null;
            if (f) {
              f.requestSubmit();
            }
          }}
          onCancel={() => navigate('/properties')}
          isSubmitting={isLoading}
          saveLabel={
            isLoading
              ? t('form.saving')
              : property
                ? t('form.updateProperty')
                : t('form.createProperty')
          }
        />
      </form>
    </MobileFormStepperProvider>
  );
};
