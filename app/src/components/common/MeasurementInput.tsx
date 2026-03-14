import { ChevronDown } from 'lucide-react';

interface UnitOption {
  value: string;
  label: string;
}

interface MeasurementInputProps {
  value: number | null | undefined;
  onChange: (value: number | undefined) => void;
  unit: string;
  unitOptions: UnitOption[];
  onUnitChange: (unit: string) => void;
  min?: number;
  step?: number;
  disabled?: boolean;
  placeholder?: string;
}

export const MeasurementInput = ({
  value,
  onChange,
  unit,
  unitOptions,
  onUnitChange,
  min,
  step,
  disabled = false,
  placeholder,
}: MeasurementInputProps) => {
  const currentLabel = unitOptions.find((o) => o.value === unit)?.label ?? unit;

  return (
    <div className="flex">
      <input
        type="number"
        min={min}
        step={step}
        value={value ?? ''}
        onChange={(e) =>
          onChange(e.target.value ? parseFloat(e.target.value) : undefined)
        }
        disabled={disabled}
        placeholder={placeholder}
        className="flex-1 min-w-0 px-3 py-2 border rounded-l-md border-border-strong bg-surface-card text-text-primary focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
      />
      {unitOptions.length > 1 ? (
        <div className="relative">
          <select
            value={unit}
            onChange={(e) => onUnitChange(e.target.value)}
            disabled={disabled}
            className="appearance-none inline-flex items-center gap-1 px-3 pr-7 h-full rounded-r-md border border-l-0 border-border-strong bg-surface-inset text-text-secondary text-sm hover:bg-neutral-100 dark:hover:bg-surface-raised cursor-pointer transition-colors focus:outline-none focus:ring-1 focus:ring-primary-500"
          >
            {unitOptions.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 h-3 w-3 text-text-secondary " />
        </div>
      ) : (
        <span className="inline-flex items-center px-3 h-full rounded-r-md border border-l-0 border-border-strong bg-surface-inset text-text-secondary text-sm">
          {currentLabel}
        </span>
      )}
    </div>
  );
};
