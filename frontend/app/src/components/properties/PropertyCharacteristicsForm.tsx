import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Info, Plus, Trash2 } from 'lucide-react';
import { CollapsibleSection } from './CollapsibleSection';
import { MeasurementInput } from '@/components/common/MeasurementInput';
import { RichTextEditor } from '@buurman/ui';
import {
  CreatePropertyRequest,
  PropertyResponse,
  PropertyCategory,
  OutdoorAreaResponse,
  AmenityResponse,
  PropertyAmenityResponse,
  ResidentialDetailsRequest,
  CommercialDetailsRequest,
  IndustrialDetailsRequest,
  AgriculturalDetailsRequest,
  CONSTRUCTION_TYPES,
  FOUNDATION_TYPES,
  ROOF_TYPES,
  FLOORING_TYPES,
  WINDOW_TYPES,
  ENERGY_EFFICIENCY_RATINGS,
  HEATING_TYPES,
  COOLING_TYPES,
  HOT_WATER_SYSTEMS,
  ELECTRICITY_CONNECTION_TYPES,
  WATER_CONNECTION_TYPES,
  SEWAGE_TYPES,
  INTERNET_CONNECTION_TYPES,
  INTERNET_STATUSES,
  PARKING_TYPES,
  OUTDOOR_AREA_TYPES,
} from '@/types/property';

// --- Shared small helpers ---

const labelCls = 'block text-sm font-medium text-text-secondary mb-1';
const inputCls =
  'w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500';
function humanize(val: string): string {
  return val
    .replace(/_/g, '')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase())
    .replace(/\bAc\b/g, 'AC')
    .replace(/\bDsl\b/g, 'DSL')
    .replace(/\bCo\b/g, 'CO');
}

function countFilled(values: (unknown | null | undefined)[]): number {
  return values.filter((v) => v != null && v !== '' && v !== false).length;
}

/** Build dropdown options from a readonly const array */
function arrayOptions(arr: readonly string[]): Record<string, string> {
  return Object.fromEntries(arr.map((v) => [v, humanize(v)]));
}

/** Build translated dropdown options, falling back to humanized value */
function translatedOptions(
  arr: readonly string[],
  t: (key: string, options?: { defaultValue: string }) => string,
  prefix: string
): Record<string, string> {
  return Object.fromEntries(
    arr.map((v) => [v, t(`${prefix}.${v}`, { defaultValue: humanize(v) })])
  );
}

const Tooltip = ({ text }: { text: string }) => (
  <span className="relative group ml-1 inline-flex">
    <Info className="h-3.5 w-3.5 text-text-muted cursor-help" />
    <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 px-2 py-1 text-xs bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 rounded whitespace-nowrap opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-10">
      {text}
    </span>
  </span>
);

interface SelectFieldProps {
  label: string;
  value: string | null | undefined;
  options: Record<string, string>;
  onChange: (v: string | null) => void;
  tooltip?: string;
}

const SelectField = ({
  label,
  value,
  options,
  onChange,
  tooltip,
}: SelectFieldProps) => {
  const { t } = useTranslation('properties');
  return (
    <div>
      <label className={labelCls}>
        {label}
        {tooltip && <Tooltip text={tooltip} />}
      </label>
      <select
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value || null)}
        className={inputCls}
      >
        <option value="">{t('characteristics.selectPlaceholder')}</option>
        {Object.entries(options).map(([k, v]) => (
          <option key={k} value={k}>
            {v}
          </option>
        ))}
      </select>
    </div>
  );
};

interface NumberFieldProps {
  label: string;
  value: number | null | undefined;
  onChange: (v: number | undefined) => void;
  min?: number;
  step?: number;
  tooltip?: string;
  suffix?: string;
}

const NumberField = ({
  label,
  value,
  onChange,
  min,
  step,
  tooltip,
  suffix,
}: NumberFieldProps) => (
  <div>
    <label className={labelCls}>
      {label}
      {tooltip && <Tooltip text={tooltip} />}
    </label>
    <div className="relative">
      <input
        type="number"
        min={min}
        step={step}
        value={value ?? ''}
        onChange={(e) =>
          onChange(e.target.value ? parseFloat(e.target.value) : undefined)
        }
        className={inputCls}
      />
      {suffix && (
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-text-muted">
          {suffix}
        </span>
      )}
    </div>
  </div>
);

const AREA_UNITS = [
  { value: 'sqm', label: 'm²' },
  { value: 'sqft', label: 'ft²' },
];
const HEIGHT_UNITS = [
  { value: 'm', label: 'm' },
  { value: 'ft', label: 'ft' },
];
const FLOOR_LOAD_UNITS = [
  { value: 'kg_sqm', label: 'kg/m²' },
  { value: 'lbs_sqft', label: 'lbs/ft²' },
];
const WEIGHT_UNITS = [
  { value: 'metric_tons', label: 't' },
  { value: 'us_tons', label: 'US tons' },
];
const ELECTRICITY_UNITS = [
  { value: 'a', label: 'A' },
  { value: 'ka', label: 'kA' },
];
const INTERNET_SPEED_UNITS = [
  { value: 'mbps', label: 'Mbps' },
  { value: 'gbps', label: 'Gbps' },
  { value: 'tbps', label: 'Tbps' },
];
const POWER_UNITS = [
  { value: 'kva', label: 'kVA' },
  { value: 'mva', label: 'MVA' },
  { value: 'hp', label: 'HP' },
];

interface MeasurementFieldProps {
  label: string;
  value: number | null | undefined;
  onChange: (v: number | undefined) => void;
  unit: string;
  unitOptions: { value: string; label: string }[];
  onUnitChange: (unit: string) => void;
  min?: number;
  step?: number;
  tooltip?: string;
}

const MeasurementField = ({
  label,
  value,
  onChange,
  unit,
  unitOptions,
  onUnitChange,
  min,
  step,
  tooltip,
}: MeasurementFieldProps) => (
  <div>
    <label className={labelCls}>
      {label}
      {tooltip && <Tooltip text={tooltip} />}
    </label>
    <MeasurementInput
      value={value}
      onChange={onChange}
      unit={unit}
      unitOptions={unitOptions}
      onUnitChange={onUnitChange}
      min={min}
      step={step}
    />
  </div>
);

interface ToggleFieldProps {
  label: string;
  value: boolean | undefined;
  onChange: (v: boolean) => void;
}

const ToggleField = ({ label, value, onChange }: ToggleFieldProps) => (
  <label className="flex items-center gap-3 cursor-pointer py-1">
    <button
      type="button"
      role="switch"
      aria-checked={!!value}
      onClick={() => onChange(!value)}
      className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
        value ? 'bg-primary-500' : 'bg-neutral-200'
      }`}
    >
      <span
        className={`inline-block h-3.5 w-3.5 rounded-full bg-surface-card transition-transform ${
          value ? 'translate-x-4' : 'translate-x-0.5'
        }`}
      />
    </button>
    <span className="text-sm text-text-secondary">{label}</span>
  </label>
);

interface RichTextNotesFieldProps {
  label: string;
  value: string | null | undefined;
  onChange: (v: string | null) => void;
  tooltip?: string;
}

const RichTextNotesField = ({
  label,
  value,
  onChange,
  tooltip,
}: RichTextNotesFieldProps) => (
  <div className="col-span-full">
    <label className={labelCls}>
      {label}
      {tooltip && <Tooltip text={tooltip} />}
    </label>
    <RichTextEditor
      value={value ?? ''}
      onChange={(v) => onChange(v === '<p></p>' || !v ? null : v)}
    />
  </div>
);

// --- Main Component ---

interface PropertyCharacteristicsFormProps {
  formData: CreatePropertyRequest;
  property?: PropertyResponse;
  onChange: (field: keyof CreatePropertyRequest, value: unknown) => void;
  // Outdoor areas (only in edit mode)
  outdoorAreas?: OutdoorAreaResponse[];
  onCreateOutdoorArea?: (area: {
    type: string;
    areaValue?: number;
    areaUnit?: string;
  }) => void;
  onDeleteOutdoorArea?: (id: string) => void;
  // Amenities (only in edit mode) — grouped by category from GET /api/amenities
  allAmenities?: Record<string, AmenityResponse[]>;
  propertyAmenities?: PropertyAmenityResponse[];
  onAddAmenity?: (amenityIdentifier: string, notes?: string | null) => void;
  onRemoveAmenity?: (amenityIdentifier: string) => void;
}

export const PropertyCharacteristicsForm = ({
  formData,
  property,
  onChange,
  outdoorAreas = [],
  onCreateOutdoorArea,
  onDeleteOutdoorArea,
  allAmenities = {},
  propertyAmenities = [],
  onAddAmenity,
  onRemoveAmenity,
}: PropertyCharacteristicsFormProps) => {
  const { t } = useTranslation('properties');
  const constructionFields = [
    formData.yearBuilt,
    formData.yearLastRenovated,
    formData.constructionType,
    formData.foundationType,
    formData.roofType,
    formData.wallConstruction,
    formData.flooringType,
    formData.windowType,
    formData.numberOfFloors,
    formData.structuralNotes,
  ];
  const energyFields = [
    formData.energyEfficiencyRating,
    formData.energyCertificateExpiryDate,
    formData.heatingType,
    formData.coolingType,
    formData.hotWaterSystem,
    formData.insulationNotes,
  ];
  const utilityFields = [
    formData.electricityConnectionType,
    formData.electricityCapacityValue,
    formData.waterConnectionType,
    formData.hasGasConnection,
    formData.sewageType,
    formData.internetConnectionType,
    formData.internetMaxSpeedValue,
    formData.internetStatus,
  ];
  const parkingFields = [formData.parkingSpaces, formData.parkingType];
  const safetyFields = [
    formData.hasSmokeDetectors,
    formData.hasCoDetectors,
    formData.hasFireExtinguisher,
    formData.hasSprinklerSystem,
    formData.hasAlarmSystem,
    formData.hasSecurityCameras,
    formData.hasSecureEntry,
    formData.safetyNotes,
  ];
  const accessibilityFields = [
    formData.isWheelchairAccessible,
    formData.hasElevator,
    formData.hasStepFreeEntrance,
    formData.hasAdaptedBathroom,
    formData.accessibilityNotes,
  ];

  const totalAmenities = Object.values(allAmenities).reduce(
    (sum, arr) => sum + arr.length,
    0
  );

  return (
    <div className="space-y-3">
      <h3 className="text-lg font-semibold text-text-primary mb-2">
        {t('detail.characteristics.title')}
      </h3>

      {/* Construction & Structure */}
      <CollapsibleSection
        title={t('detail.construction.title')}
        filledCount={countFilled(constructionFields)}
        totalCount={constructionFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <NumberField
            label={t('characteristics.construction.yearBuilt')}
            value={formData.yearBuilt}
            onChange={(v) => onChange('yearBuilt', v)}
            min={1600}
            step={1}
          />
          <NumberField
            label={t('characteristics.construction.yearLastRenovated')}
            value={formData.yearLastRenovated}
            onChange={(v) => onChange('yearLastRenovated', v)}
            min={1600}
            step={1}
          />
          <SelectField
            label={t('characteristics.construction.constructionType')}
            value={formData.constructionType}
            options={translatedOptions(
              CONSTRUCTION_TYPES,
              t,
              'enums.characteristics.constructionType'
            )}
            onChange={(v) => onChange('constructionType', v)}
          />
          <SelectField
            label={t('characteristics.construction.foundationType')}
            value={formData.foundationType}
            options={translatedOptions(
              FOUNDATION_TYPES,
              t,
              'enums.characteristics.foundationType'
            )}
            onChange={(v) => onChange('foundationType', v)}
          />
          <SelectField
            label={t('characteristics.construction.roofType')}
            value={formData.roofType}
            options={translatedOptions(
              ROOF_TYPES,
              t,
              'enums.characteristics.roofType'
            )}
            onChange={(v) => onChange('roofType', v)}
          />
          <SelectField
            label={t('characteristics.construction.wallConstruction')}
            value={formData.wallConstruction}
            options={translatedOptions(
              CONSTRUCTION_TYPES,
              t,
              'enums.characteristics.constructionType'
            )}
            onChange={(v) => onChange('wallConstruction', v)}
          />
          <SelectField
            label={t('characteristics.construction.flooringType')}
            value={formData.flooringType}
            options={translatedOptions(
              FLOORING_TYPES,
              t,
              'enums.characteristics.flooringType'
            )}
            onChange={(v) => onChange('flooringType', v)}
          />
          <SelectField
            label={t('characteristics.construction.windowType')}
            value={formData.windowType}
            options={translatedOptions(
              WINDOW_TYPES,
              t,
              'enums.characteristics.windowType'
            )}
            onChange={(v) => onChange('windowType', v)}
          />
          <NumberField
            label={t('characteristics.construction.numberOfFloors')}
            value={formData.numberOfFloors}
            onChange={(v) => onChange('numberOfFloors', v)}
            min={1}
            step={1}
          />
          <RichTextNotesField
            label={t('characteristics.construction.structuralNotes')}
            value={formData.structuralNotes}
            onChange={(v) => onChange('structuralNotes', v)}
            tooltip={t('characteristics.construction.structuralNotesTooltip')}
          />
        </div>
      </CollapsibleSection>

      {/* Energy & Climate */}
      <CollapsibleSection
        title={t('detail.energy.title')}
        filledCount={countFilled(energyFields)}
        totalCount={energyFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SelectField
            label={t('characteristics.energy.energyEfficiencyRating')}
            value={formData.energyEfficiencyRating}
            options={arrayOptions(ENERGY_EFFICIENCY_RATINGS)}
            onChange={(v) => onChange('energyEfficiencyRating', v)}
            tooltip={t('characteristics.energy.energyRatingTooltip')}
          />
          <div>
            <label className={labelCls}>
              {t('characteristics.energy.certificateExpiry')}
              <Tooltip
                text={t('characteristics.energy.certificateExpiryTooltip')}
              />
            </label>
            <input
              type="date"
              value={formData.energyCertificateExpiryDate ?? ''}
              onChange={(e) =>
                onChange('energyCertificateExpiryDate', e.target.value || null)
              }
              className={inputCls}
            />
          </div>
          <SelectField
            label={t('characteristics.energy.heatingType')}
            value={formData.heatingType}
            options={translatedOptions(
              HEATING_TYPES,
              t,
              'enums.characteristics.heatingType'
            )}
            onChange={(v) => onChange('heatingType', v)}
          />
          <SelectField
            label={t('characteristics.energy.coolingType')}
            value={formData.coolingType}
            options={translatedOptions(
              COOLING_TYPES,
              t,
              'enums.characteristics.coolingType'
            )}
            onChange={(v) => onChange('coolingType', v)}
          />
          <SelectField
            label={t('characteristics.energy.hotWaterSystem')}
            value={formData.hotWaterSystem}
            options={translatedOptions(
              HOT_WATER_SYSTEMS,
              t,
              'enums.characteristics.hotWaterSystem'
            )}
            onChange={(v) => onChange('hotWaterSystem', v)}
          />
          <RichTextNotesField
            label={t('characteristics.energy.insulationNotes')}
            value={formData.insulationNotes}
            onChange={(v) => onChange('insulationNotes', v)}
            tooltip={t('characteristics.energy.insulationTooltip')}
          />
        </div>
      </CollapsibleSection>

      {/* Utilities & Connections */}
      <CollapsibleSection
        title={t('detail.utilities.title')}
        filledCount={countFilled(utilityFields)}
        totalCount={utilityFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SelectField
            label={t('characteristics.utilities.electricityConnection')}
            value={formData.electricityConnectionType}
            options={translatedOptions(
              ELECTRICITY_CONNECTION_TYPES,
              t,
              'enums.characteristics.electricityConnectionType'
            )}
            onChange={(v) => onChange('electricityConnectionType', v)}
          />
          <MeasurementField
            label={t('characteristics.utilities.electricityCapacity')}
            value={formData.electricityCapacityValue}
            onChange={(v) => onChange('electricityCapacityValue', v)}
            unit={formData.electricityCapacityUnit ?? 'a'}
            unitOptions={ELECTRICITY_UNITS}
            onUnitChange={(u) => onChange('electricityCapacityUnit', u)}
            min={0}
            tooltip={t('characteristics.utilities.electricityCapacityTooltip')}
          />
          <SelectField
            label={t('characteristics.utilities.waterConnection')}
            value={formData.waterConnectionType}
            options={translatedOptions(
              WATER_CONNECTION_TYPES,
              t,
              'enums.characteristics.waterConnectionType'
            )}
            onChange={(v) => onChange('waterConnectionType', v)}
          />
          <ToggleField
            label={t('characteristics.utilities.hasGasConnection')}
            value={formData.hasGasConnection}
            onChange={(v) => onChange('hasGasConnection', v)}
          />
          <SelectField
            label={t('characteristics.utilities.sewageType')}
            value={formData.sewageType}
            options={translatedOptions(
              SEWAGE_TYPES,
              t,
              'enums.characteristics.sewageType'
            )}
            onChange={(v) => onChange('sewageType', v)}
          />
          <SelectField
            label={t('characteristics.utilities.internetConnection')}
            value={formData.internetConnectionType}
            options={translatedOptions(
              INTERNET_CONNECTION_TYPES,
              t,
              'enums.characteristics.internetConnectionType'
            )}
            onChange={(v) => onChange('internetConnectionType', v)}
          />
          <MeasurementField
            label={t('characteristics.utilities.internetMaxSpeed')}
            value={formData.internetMaxSpeedValue}
            onChange={(v) => onChange('internetMaxSpeedValue', v)}
            unit={formData.internetMaxSpeedUnit ?? 'mbps'}
            unitOptions={INTERNET_SPEED_UNITS}
            onUnitChange={(u) => onChange('internetMaxSpeedUnit', u)}
            min={0}
          />
          <SelectField
            label={t('characteristics.utilities.internetStatus')}
            value={formData.internetStatus}
            options={translatedOptions(
              INTERNET_STATUSES,
              t,
              'enums.characteristics.internetStatus'
            )}
            onChange={(v) => onChange('internetStatus', v)}
          />
        </div>
      </CollapsibleSection>

      {/* Parking */}
      <CollapsibleSection
        title={t('detail.parking.title')}
        filledCount={countFilled(parkingFields)}
        totalCount={parkingFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <NumberField
            label={t('characteristics.parking.parkingSpaces')}
            value={formData.parkingSpaces}
            onChange={(v) => onChange('parkingSpaces', v)}
            min={0}
            step={1}
          />
          <SelectField
            label={t('characteristics.parking.parkingType')}
            value={formData.parkingType}
            options={translatedOptions(
              PARKING_TYPES,
              t,
              'enums.characteristics.parkingType'
            )}
            onChange={(v) => onChange('parkingType', v)}
          />
        </div>
      </CollapsibleSection>

      {/* Safety & Security */}
      <CollapsibleSection
        title={t('detail.safety.title')}
        filledCount={countFilled(safetyFields)}
        totalCount={safetyFields.length}
      >
        <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1">
          <ToggleField
            label={t('characteristics.safety.smokeDetectors')}
            value={formData.hasSmokeDetectors}
            onChange={(v) => onChange('hasSmokeDetectors', v)}
          />
          <ToggleField
            label={t('characteristics.safety.coDetectors')}
            value={formData.hasCoDetectors}
            onChange={(v) => onChange('hasCoDetectors', v)}
          />
          <ToggleField
            label={t('characteristics.safety.fireExtinguisher')}
            value={formData.hasFireExtinguisher}
            onChange={(v) => onChange('hasFireExtinguisher', v)}
          />
          <ToggleField
            label={t('characteristics.safety.sprinklerSystem')}
            value={formData.hasSprinklerSystem}
            onChange={(v) => onChange('hasSprinklerSystem', v)}
          />
          <ToggleField
            label={t('characteristics.safety.alarmSystem')}
            value={formData.hasAlarmSystem}
            onChange={(v) => onChange('hasAlarmSystem', v)}
          />
          <ToggleField
            label={t('characteristics.safety.securityCameras')}
            value={formData.hasSecurityCameras}
            onChange={(v) => onChange('hasSecurityCameras', v)}
          />
          <ToggleField
            label={t('characteristics.safety.secureEntry')}
            value={formData.hasSecureEntry}
            onChange={(v) => onChange('hasSecureEntry', v)}
          />
        </div>
        <RichTextNotesField
          label={t('characteristics.safety.safetyNotes')}
          value={formData.safetyNotes}
          onChange={(v) => onChange('safetyNotes', v)}
          tooltip={t('characteristics.safety.safetyNotesTooltip')}
        />
      </CollapsibleSection>

      {/* Accessibility */}
      <CollapsibleSection
        title={t('detail.accessibility.title')}
        filledCount={countFilled(accessibilityFields)}
        totalCount={accessibilityFields.length}
      >
        <div className="grid grid-cols-2 md:grid-cols-2 gap-x-4 gap-y-1">
          <ToggleField
            label={t('characteristics.accessibility.wheelchairAccessible')}
            value={formData.isWheelchairAccessible}
            onChange={(v) => onChange('isWheelchairAccessible', v)}
          />
          <ToggleField
            label={t('characteristics.accessibility.elevator')}
            value={formData.hasElevator}
            onChange={(v) => onChange('hasElevator', v)}
          />
          <ToggleField
            label={t('characteristics.accessibility.stepFreeEntrance')}
            value={formData.hasStepFreeEntrance}
            onChange={(v) => onChange('hasStepFreeEntrance', v)}
          />
          <ToggleField
            label={t('characteristics.accessibility.adaptedBathroom')}
            value={formData.hasAdaptedBathroom}
            onChange={(v) => onChange('hasAdaptedBathroom', v)}
          />
        </div>
        <RichTextNotesField
          label={t('characteristics.accessibility.accessibilityNotes')}
          value={formData.accessibilityNotes}
          onChange={(v) => onChange('accessibilityNotes', v)}
          tooltip={t('characteristics.accessibility.accessibilityNotesTooltip')}
        />
      </CollapsibleSection>

      {/* Category-Specific Details */}
      {formData.propertyCategory === PropertyCategory.RESIDENTIAL && (
        <ResidentialDetailsSection
          details={formData.residentialDetails ?? {}}
          onChange={(details) => onChange('residentialDetails', details)}
        />
      )}
      {formData.propertyCategory === PropertyCategory.COMMERCIAL && (
        <CommercialDetailsSection
          details={formData.commercialDetails ?? {}}
          onChange={(details) => onChange('commercialDetails', details)}
        />
      )}
      {formData.propertyCategory === PropertyCategory.INDUSTRIAL && (
        <IndustrialDetailsSection
          details={formData.industrialDetails ?? {}}
          onChange={(details) => onChange('industrialDetails', details)}
        />
      )}
      {formData.propertyCategory === PropertyCategory.AGRICULTURAL && (
        <AgriculturalDetailsSection
          details={formData.agriculturalDetails ?? {}}
          onChange={(details) => onChange('agriculturalDetails', details)}
        />
      )}

      {/* Outdoor Areas */}
      {property && (
        <OutdoorAreasSection
          areas={outdoorAreas}
          onCreate={onCreateOutdoorArea}
          onDelete={onDeleteOutdoorArea}
        />
      )}

      {/* Amenities */}
      {property && totalAmenities > 0 && (
        <AmenitiesSection
          grouped={allAmenities}
          propertyAmenities={propertyAmenities}
          onAdd={onAddAmenity}
          onRemove={onRemoveAmenity}
        />
      )}
    </div>
  );
};

// --- Outdoor Areas Section ---

interface OutdoorAreasSectionProps {
  areas: OutdoorAreaResponse[];
  onCreate?: (area: {
    type: string;
    areaValue?: number;
    areaUnit?: string;
  }) => void;
  onDelete?: (id: string) => void;
}

const OutdoorAreasSection = ({
  areas,
  onCreate,
  onDelete,
}: OutdoorAreasSectionProps) => {
  const { t } = useTranslation('properties');
  const [newType, setNewType] = useState('GARDEN');
  const [newValue, setNewValue] = useState<number | undefined>(undefined);
  const [newUnit, setNewUnit] = useState('sqm');
  const [showAdd, setShowAdd] = useState(false);

  const handleAdd = () => {
    onCreate?.({ type: newType, areaValue: newValue, areaUnit: newUnit });
    setNewType('GARDEN');
    setNewValue(undefined);
    setNewUnit('sqm');
    setShowAdd(false);
  };

  return (
    <CollapsibleSection
      title={t('detail.outdoorAreas.title')}
      filledCount={areas.length}
      totalCount={areas.length}
    >
      {areas.length > 0 && (
        <div className="space-y-2">
          {areas.map((area) => (
            <div
              key={area.identifier}
              className="flex items-center gap-3 bg-surface-page rounded px-3 py-2"
            >
              <span className="text-sm font-medium text-text-secondary flex-1">
                {t(`enums.characteristics.outdoorAreaType.${area.type}`, {
                  defaultValue: humanize(area.type),
                })}
              </span>
              {area.areaValue && (
                <span className="text-sm text-text-secondary">
                  {area.areaValue} {area.areaUnit === 'sqft' ? 'ft²' : 'm²'}
                </span>
              )}
              <button
                type="button"
                onClick={() => onDelete?.(area.identifier)}
                className="text-error-text hover:opacity-80 p-1"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}

      {showAdd ? (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end pt-2">
          <SelectField
            label={t('characteristics.outdoorAreas.type')}
            value={newType}
            options={translatedOptions(
              OUTDOOR_AREA_TYPES,
              t,
              'enums.characteristics.outdoorAreaType'
            )}
            onChange={(v) => setNewType(v || 'GARDEN')}
          />
          <NumberField
            label={t('characteristics.outdoorAreas.area')}
            value={newValue}
            onChange={setNewValue}
            min={0}
            step={0.1}
          />
          <SelectField
            label={t('characteristics.outdoorAreas.unit')}
            value={newUnit}
            options={{ sqm: 'm²', sqft: 'ft²' }}
            onChange={(v) => setNewUnit(v || 'sqm')}
          />
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleAdd}
              className="bg-primary-500 text-white px-3 py-2 rounded text-sm hover:bg-primary-600 transition-colors"
            >
              {t('characteristics.outdoorAreas.add')}
            </button>
            <button
              type="button"
              onClick={() => setShowAdd(false)}
              className="border border-border-strong px-3 py-2 rounded text-sm hover:bg-surface-inset transition-colors"
            >
              {t('common:actions.cancel')}
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setShowAdd(true)}
          className="flex items-center gap-1.5 text-sm text-primary-500 hover:text-primary-600 pt-1"
        >
          <Plus className="h-3.5 w-3.5" />
          {t('characteristics.outdoorAreas.addOutdoorArea')}
        </button>
      )}
    </CollapsibleSection>
  );
};

// --- Residential Details Section ---

const PET_POLICIES = [
  'ALLOWED',
  'NOT_ALLOWED',
  'NEGOTIABLE',
  'SMALL_PETS_ONLY',
] as const;

interface ResidentialDetailsSectionProps {
  details: Partial<ResidentialDetailsRequest>;
  onChange: (details: ResidentialDetailsRequest) => void;
}

const ResidentialDetailsSection = ({
  details,
  onChange,
}: ResidentialDetailsSectionProps) => {
  const { t } = useTranslation('properties');
  const update = (field: keyof ResidentialDetailsRequest, value: unknown) =>
    onChange({ ...details, [field]: value } as ResidentialDetailsRequest);

  const fields = [
    details.bedrooms,
    details.bathrooms,
    details.furnished,
    details.petPolicy,
  ];

  return (
    <CollapsibleSection
      title={t('characteristics.residential.title')}
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <NumberField
          label={t('characteristics.residential.bedrooms')}
          value={details.bedrooms}
          onChange={(v) => update('bedrooms', v)}
          min={0}
          step={1}
        />
        <NumberField
          label={t('characteristics.residential.bathrooms')}
          value={details.bathrooms}
          onChange={(v) => update('bathrooms', v)}
          min={0}
          step={1}
        />
        <ToggleField
          label={t('characteristics.residential.furnished')}
          value={details.furnished ?? false}
          onChange={(v) => update('furnished', v)}
        />
        <SelectField
          label={t('characteristics.residential.petPolicy')}
          value={details.petPolicy ?? null}
          options={translatedOptions(
            PET_POLICIES,
            t,
            'enums.characteristics.petPolicy'
          )}
          onChange={(v) => update('petPolicy', v)}
        />
      </div>
    </CollapsibleSection>
  );
};

// --- Commercial Details Section ---

interface CommercialDetailsSectionProps {
  details: Partial<CommercialDetailsRequest>;
  onChange: (details: CommercialDetailsRequest) => void;
}

const CommercialDetailsSection = ({
  details,
  onChange,
}: CommercialDetailsSectionProps) => {
  const { t } = useTranslation('properties');
  const update = (field: keyof CommercialDetailsRequest, value: unknown) =>
    onChange({ ...details, [field]: value } as CommercialDetailsRequest);

  const fields = [
    details.usableAreaValue,
    details.commonAreaValue,
    details.floorLevel,
    details.ceilingHeightValue,
    details.hasStorefront,
    details.hasSignageRights,
    details.zoningClassification,
    details.maxOccupancy,
    details.restroomCount,
    details.hasKitchenFacility,
    details.accessibilityCompliant,
  ];

  return (
    <CollapsibleSection
      title={t('characteristics.commercial.title')}
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MeasurementField
          label={t('characteristics.commercial.usableArea')}
          value={details.usableAreaValue}
          onChange={(v) => update('usableAreaValue', v)}
          min={0}
          step={0.01}
          unit={details.usableAreaUnit ?? 'sqm'}
          unitOptions={AREA_UNITS}
          onUnitChange={(u) => update('usableAreaUnit', u)}
        />
        <MeasurementField
          label={t('characteristics.commercial.commonArea')}
          value={details.commonAreaValue}
          onChange={(v) => update('commonAreaValue', v)}
          min={0}
          step={0.01}
          unit={details.commonAreaUnit ?? 'sqm'}
          unitOptions={AREA_UNITS}
          onUnitChange={(u) => update('commonAreaUnit', u)}
        />
        <NumberField
          label={t('characteristics.commercial.floorLevel')}
          value={details.floorLevel}
          onChange={(v) => update('floorLevel', v)}
          step={1}
        />
        <MeasurementField
          label={t('characteristics.commercial.ceilingHeight')}
          value={details.ceilingHeightValue}
          onChange={(v) => update('ceilingHeightValue', v)}
          min={0}
          step={0.1}
          unit={details.ceilingHeightUnit ?? 'm'}
          unitOptions={HEIGHT_UNITS}
          onUnitChange={(u) => update('ceilingHeightUnit', u)}
        />
        <NumberField
          label={t('characteristics.commercial.maxOccupancy')}
          value={details.maxOccupancy}
          onChange={(v) => update('maxOccupancy', v)}
          min={0}
          step={1}
        />
        <NumberField
          label={t('characteristics.commercial.restroomCount')}
          value={details.restroomCount}
          onChange={(v) => update('restroomCount', v)}
          min={0}
          step={1}
        />
        <div>
          <label className={labelCls}>
            {t('characteristics.commercial.zoningClassification')}
          </label>
          <input
            type="text"
            value={details.zoningClassification ?? ''}
            onChange={(e) =>
              update('zoningClassification', e.target.value || null)
            }
            className={inputCls}
            placeholder={t('characteristics.commercial.zoningPlaceholder')}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1 mt-3">
        <ToggleField
          label={t('characteristics.commercial.hasStorefront')}
          value={details.hasStorefront ?? false}
          onChange={(v) => update('hasStorefront', v)}
        />
        <ToggleField
          label={t('characteristics.commercial.signageRights')}
          value={details.hasSignageRights ?? false}
          onChange={(v) => update('hasSignageRights', v)}
        />
        <ToggleField
          label={t('characteristics.commercial.kitchenFacility')}
          value={details.hasKitchenFacility ?? false}
          onChange={(v) => update('hasKitchenFacility', v)}
        />
        <ToggleField
          label={t('characteristics.commercial.accessibilityCompliant')}
          value={details.accessibilityCompliant ?? false}
          onChange={(v) => update('accessibilityCompliant', v)}
        />
      </div>
    </CollapsibleSection>
  );
};

// --- Industrial Details Section ---

interface IndustrialDetailsSectionProps {
  details: Partial<IndustrialDetailsRequest>;
  onChange: (details: IndustrialDetailsRequest) => void;
}

const IndustrialDetailsSection = ({
  details,
  onChange,
}: IndustrialDetailsSectionProps) => {
  const { t } = useTranslation('properties');
  const update = (field: keyof IndustrialDetailsRequest, value: unknown) =>
    onChange({ ...details, [field]: value } as IndustrialDetailsRequest);

  const fields = [
    details.clearHeightValue,
    details.loadingDocks,
    details.driveInDoors,
    details.floorLoadCapacityValue,
    details.powerCapacityValue,
    details.hasThreePhasePower,
    details.hasCrane,
    details.craneCapacityValue,
    details.hasHazmatCertification,
    details.hasVentilationSystem,
    details.hasClimateControl,
    details.yardAreaValue,
    details.zoningClassification,
  ];

  return (
    <CollapsibleSection
      title={t('characteristics.industrial.title')}
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MeasurementField
          label={t('characteristics.industrial.clearHeight')}
          value={details.clearHeightValue}
          onChange={(v) => update('clearHeightValue', v)}
          min={0}
          step={0.1}
          unit={details.clearHeightUnit ?? 'm'}
          unitOptions={HEIGHT_UNITS}
          onUnitChange={(u) => update('clearHeightUnit', u)}
        />
        <NumberField
          label={t('characteristics.industrial.loadingDocks')}
          value={details.loadingDocks}
          onChange={(v) => update('loadingDocks', v)}
          min={0}
          step={1}
        />
        <NumberField
          label={t('characteristics.industrial.driveInDoors')}
          value={details.driveInDoors}
          onChange={(v) => update('driveInDoors', v)}
          min={0}
          step={1}
        />
        <MeasurementField
          label={t('characteristics.industrial.floorLoadCapacity')}
          value={details.floorLoadCapacityValue}
          onChange={(v) => update('floorLoadCapacityValue', v)}
          min={0}
          step={1}
          unit={details.floorLoadCapacityUnit ?? 'kg_sqm'}
          unitOptions={FLOOR_LOAD_UNITS}
          onUnitChange={(u) => update('floorLoadCapacityUnit', u)}
        />
        <MeasurementField
          label={t('characteristics.industrial.powerCapacity')}
          value={details.powerCapacityValue}
          onChange={(v) => update('powerCapacityValue', v)}
          unit={details.powerCapacityUnit ?? 'kva'}
          unitOptions={POWER_UNITS}
          onUnitChange={(u) => update('powerCapacityUnit', u)}
          min={0}
          step={1}
        />
        <MeasurementField
          label={t('characteristics.industrial.craneCapacity')}
          value={details.craneCapacityValue}
          onChange={(v) => update('craneCapacityValue', v)}
          min={0}
          step={0.1}
          unit={details.craneCapacityUnit ?? 'metric_tons'}
          unitOptions={WEIGHT_UNITS}
          onUnitChange={(u) => update('craneCapacityUnit', u)}
        />
        <MeasurementField
          label={t('characteristics.industrial.yardArea')}
          value={details.yardAreaValue}
          onChange={(v) => update('yardAreaValue', v)}
          min={0}
          step={0.01}
          unit={details.yardAreaUnit ?? 'sqm'}
          unitOptions={AREA_UNITS}
          onUnitChange={(u) => update('yardAreaUnit', u)}
        />
        <div>
          <label className={labelCls}>
            {t('characteristics.industrial.zoningClassification')}
          </label>
          <input
            type="text"
            value={details.zoningClassification ?? ''}
            onChange={(e) =>
              update('zoningClassification', e.target.value || null)
            }
            className={inputCls}
            placeholder={t('characteristics.industrial.zoningPlaceholder')}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1 mt-3">
        <ToggleField
          label={t('characteristics.industrial.threePhasePower')}
          value={details.hasThreePhasePower ?? false}
          onChange={(v) => update('hasThreePhasePower', v)}
        />
        <ToggleField
          label={t('characteristics.industrial.craneAvailable')}
          value={details.hasCrane ?? false}
          onChange={(v) => update('hasCrane', v)}
        />
        <ToggleField
          label={t('characteristics.industrial.hazmatCertified')}
          value={details.hasHazmatCertification ?? false}
          onChange={(v) => update('hasHazmatCertification', v)}
        />
        <ToggleField
          label={t('characteristics.industrial.ventilationSystem')}
          value={details.hasVentilationSystem ?? false}
          onChange={(v) => update('hasVentilationSystem', v)}
        />
        <ToggleField
          label={t('characteristics.industrial.climateControl')}
          value={details.hasClimateControl ?? false}
          onChange={(v) => update('hasClimateControl', v)}
        />
      </div>
    </CollapsibleSection>
  );
};

// --- Agricultural Details Section ---

const SOIL_TYPES = [
  'CLAY',
  'SANDY',
  'LOAM',
  'SILT',
  'PEAT',
  'CHALK',
  'MIXED',
  'OTHER',
] as const;
const IRRIGATION_TYPES = [
  'DRIP',
  'SPRINKLER',
  'FLOOD',
  'CENTER_PIVOT',
  'NONE',
  'OTHER',
] as const;
const WATER_SOURCES = [
  'RIVER',
  'WELL',
  'MUNICIPAL',
  'RAINWATER',
  'CANAL',
  'NONE',
  'OTHER',
] as const;
const FENCING_TYPES = [
  'WIRE',
  'WOODEN',
  'ELECTRIC',
  'HEDGE',
  'STONE',
  'NONE',
  'OTHER',
] as const;

interface AgriculturalDetailsSectionProps {
  details: Partial<AgriculturalDetailsRequest>;
  onChange: (details: AgriculturalDetailsRequest) => void;
}

const AgriculturalDetailsSection = ({
  details,
  onChange,
}: AgriculturalDetailsSectionProps) => {
  const { t } = useTranslation('properties');
  const update = (field: keyof AgriculturalDetailsRequest, value: unknown) =>
    onChange({ ...details, [field]: value } as AgriculturalDetailsRequest);

  const fields = [
    details.totalLandAreaValue,
    details.arableAreaValue,
    details.soilType,
    details.hasWaterRights,
    details.waterSource,
    details.irrigationType,
    details.fencingType,
    details.hasOutbuildings,
    details.outbuildingDetails,
    details.currentUse,
    details.zoningClassification,
  ];

  return (
    <CollapsibleSection
      title={t('characteristics.agricultural.title')}
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <MeasurementField
          label={t('characteristics.agricultural.totalLandArea')}
          value={details.totalLandAreaValue}
          onChange={(v) => update('totalLandAreaValue', v)}
          min={0}
          step={0.01}
          unit={details.totalLandAreaUnit ?? 'sqm'}
          unitOptions={AREA_UNITS}
          onUnitChange={(u) => update('totalLandAreaUnit', u)}
        />
        <MeasurementField
          label={t('characteristics.agricultural.arableArea')}
          value={details.arableAreaValue}
          onChange={(v) => update('arableAreaValue', v)}
          min={0}
          step={0.01}
          unit={details.arableAreaUnit ?? 'sqm'}
          unitOptions={AREA_UNITS}
          onUnitChange={(u) => update('arableAreaUnit', u)}
        />
        <SelectField
          label={t('characteristics.agricultural.soilType')}
          value={details.soilType ?? null}
          options={translatedOptions(
            SOIL_TYPES,
            t,
            'enums.characteristics.soilType'
          )}
          onChange={(v) => update('soilType', v)}
        />
        <SelectField
          label={t('characteristics.agricultural.waterSource')}
          value={details.waterSource ?? null}
          options={translatedOptions(
            WATER_SOURCES,
            t,
            'enums.characteristics.waterSource'
          )}
          onChange={(v) => update('waterSource', v)}
        />
        <SelectField
          label={t('characteristics.agricultural.irrigationType')}
          value={details.irrigationType ?? null}
          options={translatedOptions(
            IRRIGATION_TYPES,
            t,
            'enums.characteristics.irrigationType'
          )}
          onChange={(v) => update('irrigationType', v)}
        />
        <SelectField
          label={t('characteristics.agricultural.fencingType')}
          value={details.fencingType ?? null}
          options={translatedOptions(
            FENCING_TYPES,
            t,
            'enums.characteristics.fencingType'
          )}
          onChange={(v) => update('fencingType', v)}
        />
        <div>
          <label className={labelCls}>
            {t('characteristics.agricultural.currentUse')}
          </label>
          <input
            type="text"
            value={details.currentUse ?? ''}
            onChange={(e) => update('currentUse', e.target.value || null)}
            className={inputCls}
            placeholder={t(
              'characteristics.agricultural.currentUsePlaceholder'
            )}
          />
        </div>
        <div>
          <label className={labelCls}>
            {t('characteristics.agricultural.zoningClassification')}
          </label>
          <input
            type="text"
            value={details.zoningClassification ?? ''}
            onChange={(e) =>
              update('zoningClassification', e.target.value || null)
            }
            className={inputCls}
            placeholder={t('characteristics.agricultural.zoningPlaceholder')}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-2 gap-x-4 gap-y-1 mt-3">
        <ToggleField
          label={t('characteristics.agricultural.waterRights')}
          value={details.hasWaterRights ?? false}
          onChange={(v) => update('hasWaterRights', v)}
        />
        <ToggleField
          label={t('characteristics.agricultural.hasOutbuildings')}
          value={details.hasOutbuildings ?? false}
          onChange={(v) => update('hasOutbuildings', v)}
        />
      </div>
      {details.hasOutbuildings && (
        <div className="mt-3">
          <label className={labelCls}>
            {t('characteristics.agricultural.outbuildingDetails')}
          </label>
          <input
            type="text"
            value={details.outbuildingDetails ?? ''}
            onChange={(e) =>
              update('outbuildingDetails', e.target.value || null)
            }
            className={inputCls}
            placeholder={t(
              'characteristics.agricultural.outbuildingPlaceholder'
            )}
          />
        </div>
      )}
    </CollapsibleSection>
  );
};

// --- Amenities Section ---

interface AmenitiesSectionProps {
  grouped: Record<string, AmenityResponse[]>;
  propertyAmenities: PropertyAmenityResponse[];
  onAdd?: (amenityIdentifier: string, notes?: string | null) => void;
  onRemove?: (amenityIdentifier: string) => void;
}

const AmenitiesSection = ({
  grouped,
  propertyAmenities,
  onAdd,
  onRemove,
}: AmenitiesSectionProps) => {
  const { t } = useTranslation('properties');
  const selectedIds = new Set(
    propertyAmenities.map((a) => a.amenityIdentifier)
  );
  const totalAmenities = Object.values(grouped).reduce(
    (sum, arr) => sum + arr.length,
    0
  );

  const toggleAmenity = (id: string, checked: boolean) => {
    if (checked) {
      onAdd?.(id);
    } else onRemove?.(id);
  };

  return (
    <CollapsibleSection
      title={t('detail.amenities.title')}
      filledCount={selectedIds.size}
      totalCount={totalAmenities}
    >
      <div className="space-y-4">
        {Object.entries(grouped)
          .sort(([a], [b]) => a.localeCompare(b))
          .map(([category, amenities]) => (
            <div key={category}>
              <h4 className="text-sm font-semibold text-text-secondary mb-2 uppercase tracking-wide">
                {t(`enums.amenities.categories.${category}`, {
                  defaultValue: humanize(category),
                })}
              </h4>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1">
                {amenities.map((amenity) => {
                  const checked = selectedIds.has(amenity.identifier);
                  const existing = propertyAmenities.find(
                    (a) => a.amenityIdentifier === amenity.identifier
                  );
                  return (
                    <div key={amenity.identifier}>
                      <label className="flex items-center gap-2 cursor-pointer py-1">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() =>
                            toggleAmenity(amenity.identifier, !checked)
                          }
                          className="rounded border-border-strong text-primary-500 focus:ring-primary-500"
                        />
                        <span className="text-sm text-text-secondary">
                          {amenity.name}
                        </span>
                      </label>
                      {checked && existing?.notes && (
                        <p className="ml-6 text-xs text-text-muted mb-1">
                          {existing.notes}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
      </div>
    </CollapsibleSection>
  );
};
