import { useTranslation } from 'react-i18next';
import { Info, Split } from 'lucide-react';
import { CollapsibleSection } from '../properties/CollapsibleSection';
import { MeasurementInput } from '@/components/common/MeasurementInput';
import { FeatureGate } from '@/components/FeatureGate';
import { FeatureFlags } from '@/constants/featureFlags';
import type { UpdateUnitRequest } from '@/types/unit';
import {
  FLOORING_TYPES,
  WINDOW_TYPES,
  HEATING_TYPES,
  COOLING_TYPES,
  HOT_WATER_SYSTEMS,
  ENERGY_EFFICIENCY_RATINGS,
} from '@/types/property';

// --- Shared small helpers (mirrors PropertyCharacteristicsForm's local helpers) ---

const labelCls = 'block text-sm font-medium text-text-secondary mb-1';
const inputCls =
  'w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60 disabled:cursor-not-allowed';

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

/** Build dropdown options from a readonly const array, humanized (no translation table). */
function arrayOptions(arr: readonly string[]): Record<string, string> {
  return Object.fromEntries(arr.map((v) => [v, humanize(v)]));
}

/** Build translated dropdown options, falling back to humanized value. */
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
  onChange: (v: string | undefined) => void;
  disabled?: boolean;
  tooltip?: string;
}

const SelectField = ({
  label,
  value,
  options,
  onChange,
  disabled,
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
        disabled={disabled}
        onChange={(e) => onChange(e.target.value || undefined)}
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

interface ToggleFieldProps {
  label: string;
  value: boolean | undefined;
  onChange: (v: boolean) => void;
  disabled?: boolean;
}

const ToggleField = ({
  label,
  value,
  onChange,
  disabled,
}: ToggleFieldProps) => (
  <label
    className={`flex items-center gap-3 py-1 ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
  >
    <button
      type="button"
      role="switch"
      aria-checked={!!value}
      disabled={disabled}
      onClick={() => onChange(!value)}
      className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
        value ? 'bg-primary-500' : 'bg-neutral-200 dark:bg-neutral-700'
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

interface TextAreaFieldProps {
  label: string;
  value: string | null | undefined;
  onChange: (v: string | undefined) => void;
  disabled?: boolean;
  tooltip?: string;
}

const TextAreaField = ({
  label,
  value,
  onChange,
  disabled,
  tooltip,
}: TextAreaFieldProps) => (
  <div className="col-span-full">
    <label className={labelCls}>
      {label}
      {tooltip && <Tooltip text={tooltip} />}
    </label>
    <textarea
      value={value ?? ''}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value || undefined)}
      rows={3}
      className={inputCls}
    />
  </div>
);

const AREA_UNIT_OPTIONS = [
  { value: 'sqm', label: 'm²' },
  { value: 'sqft', label: 'ft²' },
];

// --- Main component ---

interface UnitCharacteristicsFormProps {
  value: UpdateUnitRequest;
  onChange: <K extends keyof UpdateUnitRequest>(
    field: K,
    value: UpdateUnitRequest[K]
  ) => void;
  disabled?: boolean;
  /**
   * When provided, the form renders its own header row (title + optional split action)
   * instead of relying on the caller to render one -- see `onSplit`. Callers that render their
   * own heading (e.g. the per-unit detail page, which is already past the split) omit this and
   * get no internal header, unchanged from before.
   */
  title?: string;
  /**
   * Offers the 1 -> N split transition inline in the header, next to the title. Only relevant
   * for a property's sole implicit unit -- omit entirely once a property has real units. Gated
   * behind FeatureFlags.MULTI_UNIT regardless of whether the caller already gates it.
   */
  onSplit?: () => void;
}

/**
 * Fully controlled: the parent owns the `UpdateUnitRequest` draft and decides when (and
 * whether) to persist it. This component never calls a mutation and never holds its own
 * copy of the data -- every keystroke flows straight back through `onChange`.
 *
 * Renders the dwelling attributes that PREAMBLE moved off Property onto Unit: floor area,
 * energy label + certificate expiry, heating/cooling/hot water/insulation, flooring and
 * window type, smoke/CO detectors, fire extinguisher, adapted bathroom and accessibility
 * notes. `unitNumber`/`name`/`floor`/`unitType`/`status` (identity) and `wozValue`/
 * `wozValueCurrency` (valuation) are deliberately out of scope for a *characteristics* form.
 */
export const UnitCharacteristicsForm = ({
  value,
  onChange,
  disabled = false,
  title,
  onSplit,
}: UnitCharacteristicsFormProps) => {
  const { t } = useTranslation(['properties', 'units']);

  const floorAreaFields = [value.areaValue];
  const energyFields = [
    value.energyEfficiencyRating,
    value.energyCertificateExpiryDate,
    value.heatingType,
    value.coolingType,
    value.hotWaterSystem,
    value.insulationNotes,
  ];
  const finishesFields = [value.flooringType, value.windowType];
  const safetyFields = [
    value.hasSmokeDetectors,
    value.hasCoDetectors,
    value.hasFireExtinguisher,
  ];
  const accessibilityFields = [
    value.hasAdaptedBathroom,
    value.accessibilityNotes,
  ];

  return (
    <div className="space-y-3">
      {title && (
        <div className="flex items-center justify-between mb-1">
          <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide">
            {title}
          </h3>
          {onSplit && (
            <FeatureGate flag={FeatureFlags.MULTI_UNIT}>
              <button
                type="button"
                onClick={onSplit}
                className="inline-flex items-center gap-1.5 text-sm font-medium text-primary-500 hover:text-primary-600"
              >
                <Split className="h-3.5 w-3.5" />
                {t('units:split.action')}
              </button>
            </FeatureGate>
          )}
        </div>
      )}

      <CollapsibleSection
        title={t('units:sections.floorArea')}
        filledCount={countFilled(floorAreaFields)}
        totalCount={floorAreaFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>{t('units:fields.areaValue')}</label>
            <MeasurementInput
              value={value.areaValue}
              onChange={(v) => onChange('areaValue', v)}
              unit={value.areaUnit ?? 'sqm'}
              unitOptions={AREA_UNIT_OPTIONS}
              onUnitChange={(u) => onChange('areaUnit', u)}
              min={0}
              step={0.1}
              disabled={disabled}
            />
          </div>
        </div>
      </CollapsibleSection>

      <CollapsibleSection
        title={t('detail.energy.title')}
        filledCount={countFilled(energyFields)}
        totalCount={energyFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SelectField
            label={t('characteristics.energy.energyEfficiencyRating')}
            value={value.energyEfficiencyRating}
            options={arrayOptions(ENERGY_EFFICIENCY_RATINGS)}
            onChange={(v) => onChange('energyEfficiencyRating', v)}
            disabled={disabled}
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
              value={value.energyCertificateExpiryDate ?? ''}
              disabled={disabled}
              onChange={(e) =>
                onChange(
                  'energyCertificateExpiryDate',
                  e.target.value || undefined
                )
              }
              className={inputCls}
            />
          </div>
          <SelectField
            label={t('characteristics.energy.heatingType')}
            value={value.heatingType}
            options={translatedOptions(
              HEATING_TYPES,
              t,
              'enums.characteristics.heatingType'
            )}
            onChange={(v) => onChange('heatingType', v)}
            disabled={disabled}
          />
          <SelectField
            label={t('characteristics.energy.coolingType')}
            value={value.coolingType}
            options={translatedOptions(
              COOLING_TYPES,
              t,
              'enums.characteristics.coolingType'
            )}
            onChange={(v) => onChange('coolingType', v)}
            disabled={disabled}
          />
          <SelectField
            label={t('characteristics.energy.hotWaterSystem')}
            value={value.hotWaterSystem}
            options={translatedOptions(
              HOT_WATER_SYSTEMS,
              t,
              'enums.characteristics.hotWaterSystem'
            )}
            onChange={(v) => onChange('hotWaterSystem', v)}
            disabled={disabled}
          />
          <TextAreaField
            label={t('characteristics.energy.insulationNotes')}
            value={value.insulationNotes}
            onChange={(v) => onChange('insulationNotes', v)}
            disabled={disabled}
            tooltip={t('characteristics.energy.insulationTooltip')}
          />
        </div>
      </CollapsibleSection>

      <CollapsibleSection
        title={t('units:sections.finishes')}
        filledCount={countFilled(finishesFields)}
        totalCount={finishesFields.length}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SelectField
            label={t('characteristics.construction.flooringType')}
            value={value.flooringType}
            options={translatedOptions(
              FLOORING_TYPES,
              t,
              'enums.characteristics.flooringType'
            )}
            onChange={(v) => onChange('flooringType', v)}
            disabled={disabled}
          />
          <SelectField
            label={t('characteristics.construction.windowType')}
            value={value.windowType}
            options={translatedOptions(
              WINDOW_TYPES,
              t,
              'enums.characteristics.windowType'
            )}
            onChange={(v) => onChange('windowType', v)}
            disabled={disabled}
          />
        </div>
      </CollapsibleSection>

      <CollapsibleSection
        title={t('units:sections.safety')}
        filledCount={countFilled(safetyFields)}
        totalCount={safetyFields.length}
      >
        <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1">
          <ToggleField
            label={t('characteristics.safety.smokeDetectors')}
            value={value.hasSmokeDetectors}
            onChange={(v) => onChange('hasSmokeDetectors', v)}
            disabled={disabled}
          />
          <ToggleField
            label={t('characteristics.safety.coDetectors')}
            value={value.hasCoDetectors}
            onChange={(v) => onChange('hasCoDetectors', v)}
            disabled={disabled}
          />
          <ToggleField
            label={t('characteristics.safety.fireExtinguisher')}
            value={value.hasFireExtinguisher}
            onChange={(v) => onChange('hasFireExtinguisher', v)}
            disabled={disabled}
          />
        </div>
      </CollapsibleSection>

      <CollapsibleSection
        title={t('units:sections.accessibilityDetails')}
        filledCount={countFilled(accessibilityFields)}
        totalCount={accessibilityFields.length}
      >
        <div className="grid grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1">
          <ToggleField
            label={t('characteristics.accessibility.adaptedBathroom')}
            value={value.hasAdaptedBathroom}
            onChange={(v) => onChange('hasAdaptedBathroom', v)}
            disabled={disabled}
          />
        </div>
        <TextAreaField
          label={t('characteristics.accessibility.accessibilityNotes')}
          value={value.accessibilityNotes}
          onChange={(v) => onChange('accessibilityNotes', v)}
          disabled={disabled}
          tooltip={t('characteristics.accessibility.accessibilityNotesTooltip')}
        />
      </CollapsibleSection>
    </div>
  );
};
