import { useState } from 'react';
import {
  useCurrencies,
  getFractionalDigits,
  getCurrencySymbol,
} from '@/hooks/useCurrencies';

interface MoneyInputProps {
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  currency: string;
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

  const handleBlur = () => {
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

  const defaultPlaceholder =
    placeholder ??
    (fractionalDigits > 0 ? `0.${'0'.repeat(fractionalDigits)}` : '0');

  return (
    <div className="flex">
      <span className="inline-flex items-center px-3 rounded-l-md border border-r-0 border-[#c9cfd9] dark:border-[#3a3f54] bg-[#f5f6fa] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] text-sm">
        {symbol}
      </span>
      <input
        id={id}
        type="text"
        inputMode="decimal"
        value={rawValue}
        onChange={handleChange}
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
