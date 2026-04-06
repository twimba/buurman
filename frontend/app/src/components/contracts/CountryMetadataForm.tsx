import { useContractMetadataSchema } from '../../hooks/useContractHooks';
import { useCurrencies, getFractionalDigits } from '../../hooks/useCurrencies';
import type { MetadataFieldSchema } from '../../types/contract';
import { MoneyInput } from '../common/MoneyInput';
import { useTranslation } from 'react-i18next';

interface CountryMetadataFormProps {
  countryCode: string;
  value: Record<string, unknown>;
  onChange: (metadata: Record<string, unknown>) => void;
  currency: string;
  disabled?: boolean;
}

export default function CountryMetadataForm({
  countryCode,
  value,
  onChange,
  currency,
  disabled = false,
}: CountryMetadataFormProps) {
  const { t } = useTranslation('contracts');
  const { data: schema, isLoading } = useContractMetadataSchema(countryCode);
  const { data: currencies } = useCurrencies();
  const digits = getFractionalDigits(currencies, currency);

  if (isLoading) {
    return (
      <div className="animate-pulse space-y-3">
        <div className="h-4 bg-surface-inset rounded w-1/3" />
        <div className="h-10 bg-surface-inset rounded" />
        <div className="h-10 bg-surface-inset rounded" />
      </div>
    );
  }

  if (!schema || schema.fields.length === 0) {
    return (
      <p className="text-sm text-text-secondary italic">
        {t('countryMetadata.noFields', { countryCode })}
      </p>
    );
  }

  const handleFieldChange = (fieldName: string, fieldValue: unknown) => {
    const updated = { ...value };
    if (fieldValue === '' || fieldValue === null || fieldValue === undefined) {
      delete updated[fieldName];
    } else {
      updated[fieldName] = fieldValue;
    }
    onChange(updated);
  };

  // Group fields
  const fieldsByGroup: Record<string, MetadataFieldSchema[]> = {};
  for (const field of schema.fields) {
    if (!fieldsByGroup[field.group]) {
      fieldsByGroup[field.group] = [];
    }
    fieldsByGroup[field.group].push(field);
  }

  return (
    <div className="space-y-6">
      {schema.groups.map((group) => {
        const fields = fieldsByGroup[group.key];
        if (!fields || fields.length === 0) {
          return null;
        }
        return (
          <div key={group.key}>
            <h4 className="text-sm font-medium text-text-primary mb-3">
              {t(`countryMetadata.groups.${group.key}`, { defaultValue: group.label })}
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {fields.map((field) => (
                <MetadataField
                  key={field.name}
                  field={field}
                  value={value[field.name]}
                  onChange={(v) => handleFieldChange(field.name, v)}
                  currency={currency}
                  fractionalDigits={digits}
                  disabled={disabled}
                />
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}

/** Expose the schema's countryName for use in parent section headings. */
export function useCountryName(countryCode: string | undefined) {
  const { data: schema } = useContractMetadataSchema(countryCode);
  return schema?.countryName;
}

interface MetadataFieldProps {
  field: MetadataFieldSchema;
  value: unknown;
  onChange: (value: unknown) => void;
  currency: string;
  fractionalDigits: number;
  disabled: boolean;
}

function HelpText({ text }: { text?: string }) {
  if (!text) {
    return null;
  }
  return <p className="text-sm text-text-secondary mt-1">{text}</p>;
}

function FieldLabel({
  label,
  unit,
  required,
}: {
  label: string;
  unit?: string;
  required?: boolean;
}) {
  return (
    <label className="block text-sm text-text-secondary mb-1">
      {label}
      {required && <span className="text-error-text ml-0.5">*</span>}
      {unit && <span className="ml-1 text-xs text-text-muted">({unit})</span>}
    </label>
  );
}

/** Extract major-unit amount from a minor-unit MoneyAmount object. */
function moneyValueToMajor(val: unknown, digits: number): number | undefined {
  if (val == null) {
    return undefined;
  }
  if (typeof val === 'object' && 'value' in (val as Record<string, unknown>)) {
    const minor = (val as { value: number }).value;
    return minor / 10 ** digits;
  }
  // Legacy: plain number (already major units)
  if (typeof val === 'number') {
    return val;
  }
  return undefined;
}

/** Build a minor-unit MoneyAmount object from a major-unit amount. */
function majorToMoneyValue(
  majorUnits: number | undefined,
  currency: string,
  digits: number
): { value: number; currency: string } | undefined {
  if (majorUnits === undefined) {
    return undefined;
  }
  return { value: Math.round(majorUnits * 10 ** digits), currency };
}

function MetadataField({
  field,
  value,
  onChange,
  currency,
  fractionalDigits,
  disabled,
}: MetadataFieldProps) {
  const { t } = useTranslation('contracts');
  const tLabel = t(`countryMetadata.fields.${field.name}`, { defaultValue: field.label });
  const tHelpText = field.helpText
    ? t(`countryMetadata.helpText.${field.name}`, { defaultValue: field.helpText })
    : undefined;
  const tUnit = field.unit
    ? t(`countryMetadata.units.${field.name}`, { defaultValue: field.unit })
    : undefined;
  const inputClasses =
    'block w-full rounded-md border border-border-strong bg-surface-card px-3 py-2 text-sm text-text-primary placeholder-text-muted focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:bg-surface-inset disabled:cursor-not-allowed';

  switch (field.type) {
    case 'MONEY': {
      const moneyObj = value as
        | { value: number; currency: string }
        | null
        | undefined;
      const fieldCurrency = moneyObj?.currency || currency;
      const majorAmount = moneyValueToMajor(value, fractionalDigits);

      return (
        <div>
          <FieldLabel label={tLabel} required={field.required} />
          <MoneyInput
            value={majorAmount}
            onChange={(v) =>
              onChange(majorToMoneyValue(v, fieldCurrency, fractionalDigits))
            }
            currency={fieldCurrency}
            disabled={disabled}
          />
          <HelpText text={tHelpText} />
        </div>
      );
    }

    case 'BOOLEAN':
      return (
        <div>
          <div className="flex items-center gap-3">
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={value === true}
                onChange={(e) => onChange(e.target.checked)}
                disabled={disabled}
                className="sr-only peer"
                aria-label={tLabel}
              />
              <div className="w-9 h-5 bg-surface-inset peer-focus:ring-2 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-surface-card after:border-border-default after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary-500 peer-disabled:opacity-50" />
              <span className="ml-3 text-sm text-text-primary">
                {tLabel}
              </span>
            </label>
          </div>
          <HelpText text={tHelpText} />
        </div>
      );

    case 'ENUM':
      return (
        <div>
          <FieldLabel label={tLabel} required={field.required} />
          <select
            value={(value as string) ?? ''}
            onChange={(e) => onChange(e.target.value || null)}
            disabled={disabled}
            className={inputClasses}
          >
            <option value="">{t('countryMetadata.selectPlaceholder')}</option>
            {field.enumValues.map((ev) => (
              <option key={ev.value} value={ev.value}>
                {ev.label}
              </option>
            ))}
          </select>
          <HelpText text={tHelpText} />
        </div>
      );

    case 'INTEGER':
      return (
        <div>
          <FieldLabel
            label={tLabel}
            unit={tUnit}
            required={field.required}
          />
          <input
            type="number"
            step="1"
            min={field.validation.min ?? undefined}
            max={field.validation.max ?? undefined}
            value={value != null ? String(value) : ''}
            onChange={(e) => {
              const v = e.target.value;
              const parsed = parseInt(v, 10);
              onChange(v === '' || Number.isNaN(parsed) ? null : parsed);
            }}
            disabled={disabled}
            placeholder={tLabel}
            className={inputClasses}
          />
          <HelpText text={tHelpText} />
        </div>
      );

    case 'DECIMAL':
      return (
        <div>
          <FieldLabel
            label={tLabel}
            unit={tUnit}
            required={field.required}
          />
          <input
            type="number"
            step="0.01"
            min={field.validation.min ?? undefined}
            max={field.validation.max ?? undefined}
            value={value != null ? String(value) : ''}
            onChange={(e) => {
              const v = e.target.value;
              const parsed = parseFloat(v);
              onChange(v === '' || Number.isNaN(parsed) ? null : parsed);
            }}
            disabled={disabled}
            placeholder={tLabel}
            className={inputClasses}
          />
          <HelpText text={tHelpText} />
        </div>
      );

    case 'STRING':
    default:
      return (
        <div>
          <FieldLabel label={tLabel} required={field.required} />
          <input
            type="text"
            value={(value as string) ?? ''}
            onChange={(e) => onChange(e.target.value || null)}
            disabled={disabled}
            placeholder={tLabel}
            pattern={field.validation.pattern ?? undefined}
            className={inputClasses}
          />
          <HelpText text={tHelpText} />
        </div>
      );
  }
}
