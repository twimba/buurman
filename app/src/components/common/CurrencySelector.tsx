import { useState, useRef, useEffect } from 'react';
import { useCurrencies, splitCurrencies } from '@/hooks/useCurrencies';
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
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const { data: currencies = [], isLoading } = useCurrencies();
  const { top: topCurrencies, other: otherCurrencies } =
    splitCurrencies(currencies);

  const filteredTop = topCurrencies.filter(
    (c) =>
      c.code.toLowerCase().includes(search.toLowerCase()) ||
      c.name.toLowerCase().includes(search.toLowerCase())
  );

  const filteredOther = otherCurrencies.filter(
    (c) =>
      c.code.toLowerCase().includes(search.toLowerCase()) ||
      c.name.toLowerCase().includes(search.toLowerCase())
  );

  const allFiltered = [...filteredTop, ...filteredOther];

  const selectedCurrency = value
    ? currencies.find((c) => c.code === value)
    : undefined;
  const displayValue = selectedCurrency
    ? `${selectedCurrency.symbol} ${selectedCurrency.name} (${selectedCurrency.code})`
    : '';

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

  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[data-option]');
      items[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  const handleSelect = (code: string) => {
    onChange(code);
    setIsOpen(false);
    setSearch('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        setIsOpen(true);
        setSearch('');
        setHighlightedIndex(0);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex((i) => Math.min(i + 1, allFiltered.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < allFiltered.length) {
          handleSelect(allFiltered[highlightedIndex].code);
        }
        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        setSearch('');
        break;
      case 'Tab':
        setIsOpen(false);
        setSearch('');
        break;
    }
  };

  const formatCurrency = (c: { symbol: string; name: string; code: string }) =>
    `${c.symbol} ${c.name} (${c.code})`;

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={isOpen ? search : displayValue}
          onChange={(e) => {
            setSearch(e.target.value);
            setHighlightedIndex(0);
            if (!isOpen) setIsOpen(true);
          }}
          onFocus={() => {
            if (!disabled) {
              setIsOpen(true);
              setSearch('');
              setHighlightedIndex(0);
            }
          }}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder={isOpen ? 'Type to search...' : 'Select currency'}
          autoComplete="off"
          className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 pr-8 bg-white dark:bg-[#14161f] hover:border-[#5c7cfa] dark:hover:border-[#748ffc] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] disabled:bg-[#f1f3f9] dark:disabled:bg-[#1e2130] disabled:cursor-not-allowed text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
        />
        <ChevronDown
          className={`absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] pointer-events-none transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </div>

      {isOpen && (
        <div
          ref={listRef}
          className="absolute z-50 w-full mt-1 bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md shadow-lg max-h-80 overflow-y-auto"
        >
          {isLoading ? (
            <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
              Loading currencies...
            </div>
          ) : (
            <>
              {filteredTop.length > 0 && (
                <div>
                  <div className="px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] bg-[#f8f9fc] dark:bg-[#1e2130] sticky top-0">
                    Common
                  </div>
                  {filteredTop.map((currency, localIndex) => (
                    <button
                      key={currency.code}
                      type="button"
                      data-option
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => handleSelect(currency.code)}
                      onMouseEnter={() => setHighlightedIndex(localIndex)}
                      className={`w-full text-left px-3 py-2 text-sm ${
                        highlightedIndex === localIndex
                          ? 'bg-blue-50 dark:bg-blue-900/30'
                          : currency.code === value
                            ? 'bg-primary-100 dark:bg-primary-500/10'
                            : ''
                      }`}
                    >
                      {formatCurrency(currency)}
                    </button>
                  ))}
                </div>
              )}

              {filteredTop.length > 0 && filteredOther.length > 0 && (
                <div className="border-t border-[#e2e6f0] dark:border-[#2a2e3f] my-1" />
              )}

              {filteredOther.length > 0 && (
                <div>
                  <div className="px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] bg-[#f8f9fc] dark:bg-[#1e2130] sticky top-0">
                    Other Currencies
                  </div>
                  {filteredOther.map((currency, localIndex) => {
                    const flatIndex = filteredTop.length + localIndex;
                    return (
                      <button
                        key={currency.code}
                        type="button"
                        data-option
                        onMouseDown={(e) => e.preventDefault()}
                        onClick={() => handleSelect(currency.code)}
                        onMouseEnter={() => setHighlightedIndex(flatIndex)}
                        className={`w-full text-left px-3 py-2 text-sm ${
                          highlightedIndex === flatIndex
                            ? 'bg-blue-50 dark:bg-blue-900/30'
                            : currency.code === value
                              ? 'bg-primary-100 dark:bg-primary-500/10'
                              : ''
                        }`}
                      >
                        {formatCurrency(currency)}
                      </button>
                    );
                  })}
                </div>
              )}

              {allFiltered.length === 0 && (
                <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  No currencies found
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};
