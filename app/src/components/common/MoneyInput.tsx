import { useState, useRef } from 'react';
import { ChevronDown } from 'lucide-react';
import {
  useCurrencies,
  getFractionalDigits,
  getCurrencySymbol,
} from '@/hooks/useCurrencies';
import { CurrencyDropdown } from './CurrencyDropdown';

interface MoneyInputProps {
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  currency: string;
  /** When provided, the currency prefix becomes a clickable selector. */
  onCurrencyChange?: (currency: string) => void;
  disabled?: boolean;
  error?: boolean;
  placeholder?: string;
  min?: number;
  max?: number;
  className?: string;
  id?: string;
}

export const MoneyInput = ({
  value,
  onChange,
  currency,
  onCurrencyChange,
  disabled = false,
  error = false,
  placeholder,
  min,
  max,
  className = '',
  id,
}: MoneyInputProps) => {
  const { data: currencies } = useCurrencies();
  const fractionalDigits = getFractionalDigits(currencies, currency);
  const symbol = getCurrencySymbol(currencies, currency);

  const [rawValue, setRawValue] = useState(() =>
    value !== undefined ? String(value) : ''
  );
  const [lastEmitted, setLastEmitted] = useState(value);
  const [prevFractionalDigits, setPrevFractionalDigits] =
    useState(fractionalDigits);
  const [isFocused, setIsFocused] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const prefixRef = useRef<HTMLDivElement>(null);

  const formatWithThousands = (numStr: string): string => {
    if (!numStr) return '';
    const num = parseFloat(numStr);
    if (isNaN(num)) return numStr;
    const parts = num.toFixed(fractionalDigits).split('.');
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return fractionalDigits > 0 ? parts.join('.') : parts[0];
  };

  // Sync external value → rawValue when parent changes it
  if (value !== lastEmitted) {
    setLastEmitted(value);
    setRawValue(value !== undefined ? String(value) : '');
  }

  // Truncate when currency decimal places decrease
  if (fractionalDigits !== prevFractionalDigits) {
    setPrevFractionalDigits(fractionalDigits);
    if (rawValue !== '' && !rawValue.endsWith('.')) {
      const num = parseFloat(rawValue);
      if (!isNaN(num)) {
        const parts = rawValue.split('.');
        if (parts[1] !== undefined && parts[1].length > fractionalDigits) {
          const formatted = num.toFixed(fractionalDigits);
          setRawValue(formatted);
          const parsed = parseFloat(formatted);
          setLastEmitted(parsed);
          onChange(parsed);
        }
      }
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.replace(',', '.');

    if (raw === '') {
      setRawValue('');
      setLastEmitted(undefined);
      onChange(undefined);
      return;
    }

    // Only allow digits, one dot, and valid decimal length
    if (!/^\d*\.?\d*$/.test(raw)) return;
    if (fractionalDigits === 0 && raw.includes('.')) return;
    const parts = raw.split('.');
    if (parts[1] !== undefined && parts[1].length > fractionalDigits) return;

    setRawValue(raw);
    const num = parseFloat(raw);
    if (!isNaN(num)) {
      setLastEmitted(num);
      onChange(num);
    }
  };

  const handleFocus = () => {
    setIsFocused(true);
  };

  const handleBlur = () => {
    setIsFocused(false);
    if (rawValue === '' || rawValue === '.') return;
    const num = parseFloat(rawValue);
    if (isNaN(num)) return;
    const formatted = num.toFixed(fractionalDigits);
    setRawValue(formatted);
    const parsed = parseFloat(formatted);
    if (parsed !== lastEmitted) {
      setLastEmitted(parsed);
      onChange(parsed);
    }
  };

  const displayValue = isFocused ? rawValue : formatWithThousands(rawValue);

  const defaultPlaceholder =
    placeholder ??
    (fractionalDigits > 0 ? `0.${'0'.repeat(fractionalDigits)}` : '0');

  const isClickable = !!onCurrencyChange && !disabled;

  return (
    <div className="flex">
      {/* Currency prefix — static span or clickable button */}
      <div ref={prefixRef} className="relative">
        {isClickable ? (
          <button
            type="button"
            onClick={() => setDropdownOpen((o) => !o)}
            className="inline-flex items-center gap-1 px-3 h-full rounded-l-md border border-r-0 border-[#c9cfd9] dark:border-[#3a3f54] bg-[#f5f6fa] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] text-sm hover:bg-[#eef0f6] dark:hover:bg-[#262a3a] cursor-pointer transition-colors"
          >
            {symbol}
            <ChevronDown
              className={`h-3 w-3 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`}
            />
          </button>
        ) : (
          <span className="inline-flex items-center px-3 h-full rounded-l-md border border-r-0 border-[#c9cfd9] dark:border-[#3a3f54] bg-[#f5f6fa] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] text-sm">
            {symbol}
          </span>
        )}

        {dropdownOpen && onCurrencyChange && (
          <CurrencyDropdown
            value={currency}
            onSelect={onCurrencyChange}
            onClose={() => setDropdownOpen(false)}
            triggerRef={prefixRef}
          />
        )}
      </div>

      <input
        id={id}
        type="text"
        inputMode="decimal"
        value={displayValue}
        onChange={handleChange}
        onFocus={handleFocus}
        onBlur={handleBlur}
        placeholder={defaultPlaceholder}
        disabled={disabled}
        min={min}
        max={max}
        className={`flex-1 px-3 py-2 border rounded-r-md ${
          error ? 'border-red-500' : 'border-[#c9cfd9] dark:border-[#3a3f54]'
        } bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa] focus:border-[#5c7cfa] ${className}`}
      />
    </div>
  );
};
