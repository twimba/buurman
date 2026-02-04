import { useState, useRef, useEffect } from 'react';
import {
  allCurrencies,
  topCurrencies,
  formatCurrency,
} from '@/utils/currencies';
import { ChevronDown } from 'lucide-react';

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
  const [search, setSearch] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const topCurrencyCodes = new Set(topCurrencies.map((c) => c.code));
  const otherCurrencies = allCurrencies.filter(
    (c) => !topCurrencyCodes.has(c.code)
  );

  const filteredTopCurrencies = topCurrencies.filter(
    (currency) =>
      currency.code.toLowerCase().includes(search.toLowerCase()) ||
      currency.name.toLowerCase().includes(search.toLowerCase())
  );

  const filteredOtherCurrencies = otherCurrencies.filter(
    (currency) =>
      currency.code.toLowerCase().includes(search.toLowerCase()) ||
      currency.name.toLowerCase().includes(search.toLowerCase())
  );

  const selectedCurrency = value
    ? allCurrencies.find((c) => c.code === value)
    : undefined;

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
        setSearch('');
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (code: string) => {
    onChange(code);
    setIsOpen(false);
    setSearch('');
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => {
          setIsOpen(!isOpen);
          if (!isOpen) {
            setTimeout(() => inputRef.current?.focus(), 100);
          }
        }}
        disabled={disabled}
        className="w-full flex items-center justify-between border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 bg-white dark:bg-[#14161f] hover:border-[#5c7cfa] dark:hover:border-[#748ffc] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] disabled:bg-[#f1f3f9] dark:bg-[#1e2130] dark:disabled:bg-[#1e2130] disabled:cursor-not-allowed"
      >
        <span className="text-sm">
          {selectedCurrency
            ? formatCurrency(selectedCurrency.code)
            : 'Select currency'}
        </span>
        <ChevronDown
          className={`h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </button>

      {isOpen && (
        <div className="absolute z-50 w-full mt-1 bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md shadow-lg max-h-96 overflow-hidden">
          {/* Search input */}
          <div className="p-2 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <input
              ref={inputRef}
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search currencies..."
              className="w-full px-3 py-2 text-sm border border-[#c9cfd9] dark:border-[#3a3f54] rounded bg-white dark:bg-[#14161f] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] focus:outline-none"
            />
          </div>

          {/* Currency list */}
          <div className="overflow-y-auto max-h-80">
            {/* Top currencies */}
            {filteredTopCurrencies.length > 0 && (
              <div>
                <div className="px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] sticky top-0">
                  Common
                </div>
                {filteredTopCurrencies.map((currency) => (
                  <button
                    key={currency.code}
                    type="button"
                    onClick={() => handleSelect(currency.code)}
                    className={`w-full text-left px-3 py-2 text-sm hover:bg-blue-50 dark:hover:bg-blue-900/30 ${
                      currency.code === value
                        ? 'bg-primary-100 dark:bg-primary-500/10'
                        : ''
                    }`}
                  >
                    {formatCurrency(currency.code)}
                  </button>
                ))}
              </div>
            )}

            {/* Divider */}
            {filteredTopCurrencies.length > 0 &&
              filteredOtherCurrencies.length > 0 && (
                <div className="border-t border-[#e2e6f0] dark:border-[#2a2e3f] my-1" />
              )}

            {/* Other currencies */}
            {filteredOtherCurrencies.length > 0 && (
              <div>
                <div className="px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] sticky top-0">
                  Other Currencies
                </div>
                {filteredOtherCurrencies.map((currency) => (
                  <button
                    key={currency.code}
                    type="button"
                    onClick={() => handleSelect(currency.code)}
                    className={`w-full text-left px-3 py-2 text-sm hover:bg-blue-50 dark:hover:bg-blue-900/30 ${
                      currency.code === value
                        ? 'bg-primary-100 dark:bg-primary-500/10'
                        : ''
                    }`}
                  >
                    {formatCurrency(currency.code)}
                  </button>
                ))}
              </div>
            )}

            {/* No results */}
            {filteredTopCurrencies.length === 0 &&
              filteredOtherCurrencies.length === 0 && (
                <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  No currencies found
                </div>
              )}
          </div>
        </div>
      )}
    </div>
  );
};
