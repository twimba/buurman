import { useState } from 'react';
import { Info, Plus, Trash2 } from 'lucide-react';
import { CollapsibleSection } from './CollapsibleSection';
import { RichTextEditor } from '@/components/common/RichTextEditor';
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

const labelCls =
  'block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1';
const inputCls =
  'w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]';
function humanize(val: string): string {
  return val
    .replace(/_/g, ' ')
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

const Tooltip = ({ text }: { text: string }) => (
  <span className="relative group ml-1 inline-flex">
    <Info className="h-3.5 w-3.5 text-[#9ca0b8] dark:text-[#5c6180] cursor-help" />
    <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 px-2 py-1 text-xs bg-[#1a1d2e] dark:bg-[#eef0f6] text-white dark:text-[#1a1d2e] rounded whitespace-nowrap opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-10">
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
}: SelectFieldProps) => (
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
      <option value="">-- Select --</option>
      {Object.entries(options).map(([k, v]) => (
        <option key={k} value={k}>
          {v}
        </option>
      ))}
    </select>
  </div>
);

interface NumberFieldProps {
  label: string;
  value: number | null | undefined;
  onChange: (v: number | null) => void;
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
          onChange(e.target.value ? parseFloat(e.target.value) : null)
        }
        className={inputCls}
      />
      {suffix && (
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-[#9ca0b8] dark:text-[#5c6180]">
          {suffix}
        </span>
      )}
    </div>
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
        value ? 'bg-[#5c7cfa]' : 'bg-[#c9cfd9] dark:bg-[#3a3f54]'
      }`}
    >
      <span
        className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${
          value ? 'translate-x-4' : 'translate-x-0.5'
        }`}
      />
    </button>
    <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">{label}</span>
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
    areaValue: number | null;
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
    formData.electricityCapacityAmps,
    formData.waterConnectionType,
    formData.hasGasConnection,
    formData.sewageType,
    formData.internetConnectionType,
    formData.internetMaxSpeedMbps,
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
      <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
        Property Characteristics
      </h3>

      {/* Construction & Structure */}
      <CollapsibleSection
        title="Construction & Structure"
        filledCount={countFilled(constructionFields)}
        totalCount={constructionFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <NumberField
            label="Year Built"
            value={formData.yearBuilt}
            onChange={(v) => onChange('yearBuilt', v)}
            min={1600}
            step={1}
          />
          <NumberField
            label="Year Last Renovated"
            value={formData.yearLastRenovated}
            onChange={(v) => onChange('yearLastRenovated', v)}
            min={1600}
            step={1}
          />
          <SelectField
            label="Construction Type"
            value={formData.constructionType}
            options={arrayOptions(CONSTRUCTION_TYPES)}
            onChange={(v) => onChange('constructionType', v)}
          />
          <SelectField
            label="Foundation Type"
            value={formData.foundationType}
            options={arrayOptions(FOUNDATION_TYPES)}
            onChange={(v) => onChange('foundationType', v)}
          />
          <SelectField
            label="Roof Type"
            value={formData.roofType}
            options={arrayOptions(ROOF_TYPES)}
            onChange={(v) => onChange('roofType', v)}
          />
          <SelectField
            label="Wall Construction"
            value={formData.wallConstruction}
            options={arrayOptions(CONSTRUCTION_TYPES)}
            onChange={(v) => onChange('wallConstruction', v)}
          />
          <SelectField
            label="Flooring Type"
            value={formData.flooringType}
            options={arrayOptions(FLOORING_TYPES)}
            onChange={(v) => onChange('flooringType', v)}
          />
          <SelectField
            label="Window Type"
            value={formData.windowType}
            options={arrayOptions(WINDOW_TYPES)}
            onChange={(v) => onChange('windowType', v)}
          />
          <NumberField
            label="Number of Floors"
            value={formData.numberOfFloors}
            onChange={(v) => onChange('numberOfFloors', v)}
            min={1}
            step={1}
          />
          <RichTextNotesField
            label="Structural Notes"
            value={formData.structuralNotes}
            onChange={(v) => onChange('structuralNotes', v)}
            tooltip="Any notable structural details, renovations, or issues"
          />
        </div>
      </CollapsibleSection>

      {/* Energy & Climate */}
      <CollapsibleSection
        title="Energy & Climate"
        filledCount={countFilled(energyFields)}
        totalCount={energyFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SelectField
            label="Energy Efficiency Rating"
            value={formData.energyEfficiencyRating}
            options={arrayOptions(ENERGY_EFFICIENCY_RATINGS)}
            onChange={(v) => onChange('energyEfficiencyRating', v)}
            tooltip="Official energy label (A++ to G)"
          />
          <div>
            <label className={labelCls}>
              Energy Certificate Expiry
              <Tooltip text="When the current energy certificate expires" />
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
            label="Heating Type"
            value={formData.heatingType}
            options={arrayOptions(HEATING_TYPES)}
            onChange={(v) => onChange('heatingType', v)}
          />
          <SelectField
            label="Cooling Type"
            value={formData.coolingType}
            options={arrayOptions(COOLING_TYPES)}
            onChange={(v) => onChange('coolingType', v)}
          />
          <SelectField
            label="Hot Water System"
            value={formData.hotWaterSystem}
            options={arrayOptions(HOT_WATER_SYSTEMS)}
            onChange={(v) => onChange('hotWaterSystem', v)}
          />
          <RichTextNotesField
            label="Insulation Notes"
            value={formData.insulationNotes}
            onChange={(v) => onChange('insulationNotes', v)}
            tooltip="Wall, roof, floor insulation details"
          />
        </div>
      </CollapsibleSection>

      {/* Utilities & Connections */}
      <CollapsibleSection
        title="Utilities & Connections"
        filledCount={countFilled(utilityFields)}
        totalCount={utilityFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SelectField
            label="Electricity Connection"
            value={formData.electricityConnectionType}
            options={arrayOptions(ELECTRICITY_CONNECTION_TYPES)}
            onChange={(v) => onChange('electricityConnectionType', v)}
          />
          <NumberField
            label="Electricity Capacity"
            value={formData.electricityCapacityAmps}
            onChange={(v) => onChange('electricityCapacityAmps', v)}
            min={0}
            suffix="Amps"
            tooltip="Main fuse capacity in amperes"
          />
          <SelectField
            label="Water Connection"
            value={formData.waterConnectionType}
            options={arrayOptions(WATER_CONNECTION_TYPES)}
            onChange={(v) => onChange('waterConnectionType', v)}
          />
          <ToggleField
            label="Has Gas Connection"
            value={formData.hasGasConnection}
            onChange={(v) => onChange('hasGasConnection', v)}
          />
          <SelectField
            label="Sewage Type"
            value={formData.sewageType}
            options={arrayOptions(SEWAGE_TYPES)}
            onChange={(v) => onChange('sewageType', v)}
          />
          <SelectField
            label="Internet Connection"
            value={formData.internetConnectionType}
            options={arrayOptions(INTERNET_CONNECTION_TYPES)}
            onChange={(v) => onChange('internetConnectionType', v)}
          />
          <NumberField
            label="Internet Max Speed"
            value={formData.internetMaxSpeedMbps}
            onChange={(v) => onChange('internetMaxSpeedMbps', v)}
            min={0}
            suffix="Mbps"
          />
          <SelectField
            label="Internet Status"
            value={formData.internetStatus}
            options={arrayOptions(INTERNET_STATUSES)}
            onChange={(v) => onChange('internetStatus', v)}
          />
        </div>
      </CollapsibleSection>

      {/* Parking */}
      <CollapsibleSection
        title="Parking"
        filledCount={countFilled(parkingFields)}
        totalCount={parkingFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <NumberField
            label="Parking Spaces"
            value={formData.parkingSpaces}
            onChange={(v) => onChange('parkingSpaces', v)}
            min={0}
            step={1}
          />
          <SelectField
            label="Parking Type"
            value={formData.parkingType}
            options={arrayOptions(PARKING_TYPES)}
            onChange={(v) => onChange('parkingType', v)}
          />
        </div>
      </CollapsibleSection>

      {/* Safety & Security */}
      <CollapsibleSection
        title="Safety & Security"
        filledCount={countFilled(safetyFields)}
        totalCount={safetyFields.length}
      >
        <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1">
          <ToggleField
            label="Smoke Detectors"
            value={formData.hasSmokeDetectors}
            onChange={(v) => onChange('hasSmokeDetectors', v)}
          />
          <ToggleField
            label="CO Detectors"
            value={formData.hasCoDetectors}
            onChange={(v) => onChange('hasCoDetectors', v)}
          />
          <ToggleField
            label="Fire Extinguisher"
            value={formData.hasFireExtinguisher}
            onChange={(v) => onChange('hasFireExtinguisher', v)}
          />
          <ToggleField
            label="Sprinkler System"
            value={formData.hasSprinklerSystem}
            onChange={(v) => onChange('hasSprinklerSystem', v)}
          />
          <ToggleField
            label="Alarm System"
            value={formData.hasAlarmSystem}
            onChange={(v) => onChange('hasAlarmSystem', v)}
          />
          <ToggleField
            label="Security Cameras"
            value={formData.hasSecurityCameras}
            onChange={(v) => onChange('hasSecurityCameras', v)}
          />
          <ToggleField
            label="Secure Entry"
            value={formData.hasSecureEntry}
            onChange={(v) => onChange('hasSecureEntry', v)}
          />
        </div>
        <RichTextNotesField
          label="Safety Notes"
          value={formData.safetyNotes}
          onChange={(v) => onChange('safetyNotes', v)}
          tooltip="Fire escape routes, last inspection date, etc."
        />
      </CollapsibleSection>

      {/* Accessibility */}
      <CollapsibleSection
        title="Accessibility"
        filledCount={countFilled(accessibilityFields)}
        totalCount={accessibilityFields.length}
      >
        <div className="grid grid-cols-2 md:grid-cols-2 gap-x-4 gap-y-1">
          <ToggleField
            label="Wheelchair Accessible"
            value={formData.isWheelchairAccessible}
            onChange={(v) => onChange('isWheelchairAccessible', v)}
          />
          <ToggleField
            label="Elevator"
            value={formData.hasElevator}
            onChange={(v) => onChange('hasElevator', v)}
          />
          <ToggleField
            label="Step-free Entrance"
            value={formData.hasStepFreeEntrance}
            onChange={(v) => onChange('hasStepFreeEntrance', v)}
          />
          <ToggleField
            label="Adapted Bathroom"
            value={formData.hasAdaptedBathroom}
            onChange={(v) => onChange('hasAdaptedBathroom', v)}
          />
        </div>
        <RichTextNotesField
          label="Accessibility Notes"
          value={formData.accessibilityNotes}
          onChange={(v) => onChange('accessibilityNotes', v)}
          tooltip="Door widths, ramp availability, etc."
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
    areaValue: number | null;
    areaUnit?: string;
  }) => void;
  onDelete?: (id: string) => void;
}

const OutdoorAreasSection = ({
  areas,
  onCreate,
  onDelete,
}: OutdoorAreasSectionProps) => {
  const [newType, setNewType] = useState('GARDEN');
  const [newValue, setNewValue] = useState<number | null>(null);
  const [newUnit, setNewUnit] = useState('sqm');
  const [showAdd, setShowAdd] = useState(false);

  const handleAdd = () => {
    onCreate?.({ type: newType, areaValue: newValue, areaUnit: newUnit });
    setNewType('GARDEN');
    setNewValue(null);
    setNewUnit('sqm');
    setShowAdd(false);
  };

  return (
    <CollapsibleSection
      title="Outdoor Areas"
      filledCount={areas.length}
      totalCount={areas.length || 0}
    >
      {areas.length > 0 && (
        <div className="space-y-2">
          {areas.map((area) => (
            <div
              key={area.identifier}
              className="flex items-center gap-3 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded px-3 py-2"
            >
              <span className="text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] flex-1">
                {humanize(area.type)}
              </span>
              {area.areaValue && (
                <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  {area.areaValue} {area.areaUnit === 'sqft' ? 'ft²' : 'm²'}
                </span>
              )}
              <button
                type="button"
                onClick={() => onDelete?.(area.identifier)}
                className="text-red-500 hover:text-red-700 p-1"
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
            label="Type"
            value={newType}
            options={arrayOptions(OUTDOOR_AREA_TYPES)}
            onChange={(v) => setNewType(v || 'GARDEN')}
          />
          <NumberField
            label="Area"
            value={newValue}
            onChange={setNewValue}
            min={0}
            step={0.1}
          />
          <SelectField
            label="Unit"
            value={newUnit}
            options={{ sqm: 'm²', sqft: 'ft²' }}
            onChange={(v) => setNewUnit(v || 'sqm')}
          />
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleAdd}
              className="bg-[#5c7cfa] text-white px-3 py-2 rounded text-sm hover:bg-[#4c6ef5] transition-colors"
            >
              Add
            </button>
            <button
              type="button"
              onClick={() => setShowAdd(false)}
              className="border border-[#c9cfd9] dark:border-[#3a3f54] px-3 py-2 rounded text-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setShowAdd(true)}
          className="flex items-center gap-1.5 text-sm text-[#5c7cfa] hover:text-[#4c6ef5] pt-1"
        >
          <Plus className="h-3.5 w-3.5" />
          Add outdoor area
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
      title="Residential Details"
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <NumberField
          label="Bedrooms"
          value={details.bedrooms}
          onChange={(v) => update('bedrooms', v)}
          min={0}
          step={1}
        />
        <NumberField
          label="Bathrooms"
          value={details.bathrooms}
          onChange={(v) => update('bathrooms', v)}
          min={0}
          step={1}
        />
        <ToggleField
          label="Furnished"
          value={details.furnished ?? false}
          onChange={(v) => update('furnished', v)}
        />
        <SelectField
          label="Pet Policy"
          value={details.petPolicy ?? null}
          options={arrayOptions(PET_POLICIES)}
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
  const update = (field: keyof CommercialDetailsRequest, value: unknown) =>
    onChange({ ...details, [field]: value } as CommercialDetailsRequest);

  const fields = [
    details.usableAreaValue,
    details.commonAreaValue,
    details.floorLevel,
    details.ceilingHeightM,
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
      title="Commercial Details"
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <NumberField
          label="Usable Area"
          value={details.usableAreaValue}
          onChange={(v) => update('usableAreaValue', v)}
          min={0}
          step={0.01}
          suffix={details.usableAreaUnit === 'sqft' ? 'ft²' : 'm²'}
        />
        <NumberField
          label="Common Area"
          value={details.commonAreaValue}
          onChange={(v) => update('commonAreaValue', v)}
          min={0}
          step={0.01}
          suffix={details.commonAreaUnit === 'sqft' ? 'ft²' : 'm²'}
        />
        <NumberField
          label="Floor Level"
          value={details.floorLevel}
          onChange={(v) => update('floorLevel', v)}
          step={1}
        />
        <NumberField
          label="Ceiling Height"
          value={details.ceilingHeightM}
          onChange={(v) => update('ceilingHeightM', v)}
          min={0}
          step={0.1}
          suffix="m"
        />
        <NumberField
          label="Max Occupancy"
          value={details.maxOccupancy}
          onChange={(v) => update('maxOccupancy', v)}
          min={0}
          step={1}
        />
        <NumberField
          label="Restroom Count"
          value={details.restroomCount}
          onChange={(v) => update('restroomCount', v)}
          min={0}
          step={1}
        />
        <div>
          <label className={labelCls}>Zoning Classification</label>
          <input
            type="text"
            value={details.zoningClassification ?? ''}
            onChange={(e) =>
              update('zoningClassification', e.target.value || null)
            }
            className={inputCls}
            placeholder="e.g. C-2, Mixed Commercial"
          />
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1 mt-3">
        <ToggleField
          label="Has Storefront"
          value={details.hasStorefront ?? false}
          onChange={(v) => update('hasStorefront', v)}
        />
        <ToggleField
          label="Signage Rights"
          value={details.hasSignageRights ?? false}
          onChange={(v) => update('hasSignageRights', v)}
        />
        <ToggleField
          label="Kitchen Facility"
          value={details.hasKitchenFacility ?? false}
          onChange={(v) => update('hasKitchenFacility', v)}
        />
        <ToggleField
          label="Accessibility Compliant"
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
  const update = (field: keyof IndustrialDetailsRequest, value: unknown) =>
    onChange({ ...details, [field]: value } as IndustrialDetailsRequest);

  const fields = [
    details.clearHeightM,
    details.loadingDocks,
    details.driveInDoors,
    details.floorLoadCapacityKgSqm,
    details.powerCapacityKva,
    details.hasThreePhasePower,
    details.hasCrane,
    details.craneCapacityTons,
    details.hasHazmatCertification,
    details.hasVentilationSystem,
    details.hasClimateControl,
    details.yardAreaValue,
    details.zoningClassification,
  ];

  return (
    <CollapsibleSection
      title="Industrial Details"
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <NumberField
          label="Clear Height"
          value={details.clearHeightM}
          onChange={(v) => update('clearHeightM', v)}
          min={0}
          step={0.1}
          suffix="m"
        />
        <NumberField
          label="Loading Docks"
          value={details.loadingDocks}
          onChange={(v) => update('loadingDocks', v)}
          min={0}
          step={1}
        />
        <NumberField
          label="Drive-in Doors"
          value={details.driveInDoors}
          onChange={(v) => update('driveInDoors', v)}
          min={0}
          step={1}
        />
        <NumberField
          label="Floor Load Capacity"
          value={details.floorLoadCapacityKgSqm}
          onChange={(v) => update('floorLoadCapacityKgSqm', v)}
          min={0}
          step={1}
          suffix="kg/m²"
        />
        <NumberField
          label="Power Capacity"
          value={details.powerCapacityKva}
          onChange={(v) => update('powerCapacityKva', v)}
          min={0}
          step={1}
          suffix="kVA"
        />
        <NumberField
          label="Crane Capacity"
          value={details.craneCapacityTons}
          onChange={(v) => update('craneCapacityTons', v)}
          min={0}
          step={0.1}
          suffix="tons"
          tooltip="Only relevant if crane is available"
        />
        <NumberField
          label="Yard Area"
          value={details.yardAreaValue}
          onChange={(v) => update('yardAreaValue', v)}
          min={0}
          step={0.01}
          suffix={details.yardAreaUnit === 'sqft' ? 'ft²' : 'm²'}
        />
        <div>
          <label className={labelCls}>Zoning Classification</label>
          <input
            type="text"
            value={details.zoningClassification ?? ''}
            onChange={(e) =>
              update('zoningClassification', e.target.value || null)
            }
            className={inputCls}
            placeholder="e.g. I-1, Light Industrial"
          />
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1 mt-3">
        <ToggleField
          label="Three-Phase Power"
          value={details.hasThreePhasePower ?? false}
          onChange={(v) => update('hasThreePhasePower', v)}
        />
        <ToggleField
          label="Crane Available"
          value={details.hasCrane ?? false}
          onChange={(v) => update('hasCrane', v)}
        />
        <ToggleField
          label="Hazmat Certified"
          value={details.hasHazmatCertification ?? false}
          onChange={(v) => update('hasHazmatCertification', v)}
        />
        <ToggleField
          label="Ventilation System"
          value={details.hasVentilationSystem ?? false}
          onChange={(v) => update('hasVentilationSystem', v)}
        />
        <ToggleField
          label="Climate Control"
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
      title="Agricultural Details"
      filledCount={countFilled(fields)}
      totalCount={fields.length}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <NumberField
          label="Total Land Area"
          value={details.totalLandAreaValue}
          onChange={(v) => update('totalLandAreaValue', v)}
          min={0}
          step={0.01}
          suffix={details.totalLandAreaUnit === 'sqft' ? 'ft²' : 'm²'}
        />
        <NumberField
          label="Arable Area"
          value={details.arableAreaValue}
          onChange={(v) => update('arableAreaValue', v)}
          min={0}
          step={0.01}
          suffix={details.arableAreaUnit === 'sqft' ? 'ft²' : 'm²'}
        />
        <SelectField
          label="Soil Type"
          value={details.soilType ?? null}
          options={arrayOptions(SOIL_TYPES)}
          onChange={(v) => update('soilType', v)}
        />
        <SelectField
          label="Water Source"
          value={details.waterSource ?? null}
          options={arrayOptions(WATER_SOURCES)}
          onChange={(v) => update('waterSource', v)}
        />
        <SelectField
          label="Irrigation Type"
          value={details.irrigationType ?? null}
          options={arrayOptions(IRRIGATION_TYPES)}
          onChange={(v) => update('irrigationType', v)}
        />
        <SelectField
          label="Fencing Type"
          value={details.fencingType ?? null}
          options={arrayOptions(FENCING_TYPES)}
          onChange={(v) => update('fencingType', v)}
        />
        <div>
          <label className={labelCls}>Current Use</label>
          <input
            type="text"
            value={details.currentUse ?? ''}
            onChange={(e) => update('currentUse', e.target.value || null)}
            className={inputCls}
            placeholder="e.g. Crop farming, Livestock grazing"
          />
        </div>
        <div>
          <label className={labelCls}>Zoning Classification</label>
          <input
            type="text"
            value={details.zoningClassification ?? ''}
            onChange={(e) =>
              update('zoningClassification', e.target.value || null)
            }
            className={inputCls}
            placeholder="e.g. A-1, Agricultural"
          />
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-2 gap-x-4 gap-y-1 mt-3">
        <ToggleField
          label="Water Rights"
          value={details.hasWaterRights ?? false}
          onChange={(v) => update('hasWaterRights', v)}
        />
        <ToggleField
          label="Has Outbuildings"
          value={details.hasOutbuildings ?? false}
          onChange={(v) => update('hasOutbuildings', v)}
        />
      </div>
      {details.hasOutbuildings && (
        <div className="mt-3">
          <label className={labelCls}>Outbuilding Details</label>
          <input
            type="text"
            value={details.outbuildingDetails ?? ''}
            onChange={(e) =>
              update('outbuildingDetails', e.target.value || null)
            }
            className={inputCls}
            placeholder="e.g. Barn, Storage shed, Equipment garage"
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
      title="Amenities"
      filledCount={selectedIds.size}
      totalCount={totalAmenities}
    >
      <div className="space-y-4">
        {Object.entries(grouped)
          .sort(([a], [b]) => a.localeCompare(b))
          .map(([category, amenities]) => (
            <div key={category}>
              <h4 className="text-sm font-semibold text-[#6b7194] dark:text-[#8b90a8] mb-2 uppercase tracking-wide">
                {humanize(category)}
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
                          className="rounded border-[#c9cfd9] dark:border-[#3a3f54] text-[#5c7cfa] focus:ring-[#5c7cfa]"
                        />
                        <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                          {amenity.name}
                        </span>
                      </label>
                      {checked && existing?.notes && (
                        <p className="ml-6 text-xs text-[#9ca0b8] dark:text-[#5c6180] mb-1">
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
