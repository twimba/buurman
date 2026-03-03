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
  const currentLabel =
    unitOptions.find((o) => o.value === unit)?.label ?? unit;

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
        className="flex-1 min-w-0 px-3 py-2 border rounded-l-md border-[#c9cfd9] dark:border-[#3a3f54] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa] focus:border-[#5c7cfa]"
      />
      {unitOptions.length > 1 ? (
        <div className="relative">
          <select
            value={unit}
            onChange={(e) => onUnitChange(e.target.value)}
            disabled={disabled}
            className="appearance-none inline-flex items-center gap-1 px-3 pr-7 h-full rounded-r-md border border-l-0 border-[#c9cfd9] dark:border-[#3a3f54] bg-[#f5f6fa] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] text-sm hover:bg-[#eef0f6] dark:hover:bg-[#262a3a] cursor-pointer transition-colors focus:outline-none focus:ring-1 focus:ring-[#5c7cfa]"
          >
            {unitOptions.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 h-3 w-3 text-[#6b7194] dark:text-[#8b90a8]" />
        </div>
      ) : (
        <span className="inline-flex items-center px-3 h-full rounded-r-md border border-l-0 border-[#c9cfd9] dark:border-[#3a3f54] bg-[#f5f6fa] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] text-sm">
          {currentLabel}
        </span>
      )}
    </div>
  );
};
