import { useContractMetadataSchema } from '../../hooks/useContractHooks';
import type { MetadataFieldSchema } from '../../types/contract';

interface CountryMetadataFormProps {
  countryCode: string;
  value: Record<string, unknown>;
  onChange: (metadata: Record<string, unknown>) => void;
  disabled?: boolean;
}

export default function CountryMetadataForm({
  countryCode,
  value,
  onChange,
  disabled = false,
}: CountryMetadataFormProps) {
  const { data: schema, isLoading } = useContractMetadataSchema(countryCode);

  if (isLoading) {
    return (
      <div className="animate-pulse space-y-3">
        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-1/3" />
        <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded" />
        <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded" />
      </div>
    );
  }

  if (!schema || schema.fields.length === 0) {
    return (
      <p className="text-sm text-gray-500 dark:text-gray-400 italic">
        No country-specific fields available for {countryCode}.
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
            <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
              {group.label}
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {fields.map((field) => (
                <MetadataField
                  key={field.name}
                  field={field}
                  value={value[field.name]}
                  onChange={(v) => handleFieldChange(field.name, v)}
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
  disabled: boolean;
}

function HelpText({ text }: { text?: string }) {
  if (!text) {
    return null;
  }
  return (
    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{text}</p>
  );
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
    <label className="block text-sm text-gray-600 dark:text-gray-400 mb-1">
      {label}
      {required && <span className="text-red-500 ml-0.5">*</span>}
      {unit && (
        <span className="ml-1 text-xs text-gray-400 dark:text-gray-500">
          ({unit})
        </span>
      )}
    </label>
  );
}

function MetadataField({
  field,
  value,
  onChange,
  disabled,
}: MetadataFieldProps) {
  const inputClasses =
    'block w-full rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100 dark:disabled:bg-gray-700 disabled:cursor-not-allowed';

  switch (field.type) {
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
                aria-label={field.label}
              />
              <div className="w-9 h-5 bg-gray-200 dark:bg-gray-700 peer-focus:ring-2 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600 peer-disabled:opacity-50" />
              <span className="ml-3 text-sm text-gray-700 dark:text-gray-300">
                {field.label}
              </span>
            </label>
          </div>
          <HelpText text={field.helpText} />
        </div>
      );

    case 'ENUM':
      return (
        <div>
          <FieldLabel label={field.label} required={field.required} />
          <select
            value={(value as string) ?? ''}
            onChange={(e) => onChange(e.target.value || null)}
            disabled={disabled}
            className={inputClasses}
          >
            <option value="">— Select —</option>
            {field.enumValues.map((ev) => (
              <option key={ev.value} value={ev.value}>
                {ev.label}
              </option>
            ))}
          </select>
          <HelpText text={field.helpText} />
        </div>
      );

    case 'INTEGER':
      return (
        <div>
          <FieldLabel
            label={field.label}
            unit={field.unit}
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
            placeholder={field.label}
            className={inputClasses}
          />
          <HelpText text={field.helpText} />
        </div>
      );

    case 'DECIMAL':
      return (
        <div>
          <FieldLabel
            label={field.label}
            unit={field.unit}
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
            placeholder={field.label}
            className={inputClasses}
          />
          <HelpText text={field.helpText} />
        </div>
      );

    case 'STRING':
    default:
      return (
        <div>
          <FieldLabel label={field.label} required={field.required} />
          <input
            type="text"
            value={(value as string) ?? ''}
            onChange={(e) => onChange(e.target.value || null)}
            disabled={disabled}
            placeholder={field.label}
            pattern={field.validation.pattern ?? undefined}
            className={inputClasses}
          />
          <HelpText text={field.helpText} />
        </div>
      );
  }
}
