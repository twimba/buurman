import { useState, useRef } from 'react';
import { ChevronDown } from 'lucide-react';
import { useCurrencies, getCurrencySymbol } from '@/hooks/useCurrencies';
import { getCurrencyFlag } from '@/utils/currencyFlags';
import { CurrencyDropdown } from './CurrencyDropdown';

interface CurrencySelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export const CurrencySelector = ({
  value,
  onChange,
  disabled = false,
}: CurrencySelectorProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { data: currencies } = useCurrencies();

  const symbol = getCurrencySymbol(currencies, value ?? '');
  const flag = value ? getCurrencyFlag(value) : '';
  const displayValue = value ? `${flag} ${symbol} ${value}` : '';

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => {
          if (!disabled) setIsOpen((o) => !o);
        }}
        disabled={disabled}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 pr-8 bg-white dark:bg-[#14161f] hover:border-[#5c7cfa] dark:hover:border-[#748ffc] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] disabled:bg-[#f1f3f9] dark:disabled:bg-[#1e2130] disabled:cursor-not-allowed text-sm text-[#1a1d2e] dark:text-[#eef0f6] text-left relative"
      >
        {displayValue || (
          <span className="text-[#9ca0b8] dark:text-[#5c6180]">
            Select currency
          </span>
        )}
        <ChevronDown
          className={`absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </button>

      {isOpen && (
        <CurrencyDropdown
          value={value}
          onSelect={onChange}
          onClose={() => setIsOpen(false)}
          triggerRef={containerRef}
        />
      )}
    </div>
  );
};
