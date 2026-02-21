import { useState, useRef, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import { useCurrencies, splitCurrencies } from '@/hooks/useCurrencies';
import { getCurrencyFlag } from '@/utils/currencyFlags';
import { useIsMobile } from '@/hooks/useIsMobile';

interface CurrencyDropdownProps {
  value?: string;
  onSelect: (code: string) => void;
  onClose: () => void;
}

export const CurrencyDropdown = ({
  value,
  onSelect,
  onClose,
}: CurrencyDropdownProps) => {
  const isMobile = useIsMobile();
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
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

  // Auto-focus search input on open
  useEffect(() => {
    // Small delay to ensure the element is rendered
    const timer = setTimeout(() => inputRef.current?.focus(), 50);
    return () => clearTimeout(timer);
  }, []);

  // Scroll highlighted item into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[data-option]');
      items[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  const handleSelect = useCallback(
    (code: string) => {
      onSelect(code);
      onClose();
    },
    [onSelect, onClose]
  );

  const handleKeyDown = (e: React.KeyboardEvent) => {
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
        onClose();
        break;
      case 'Tab':
        onClose();
        break;
    }
  };

  const formatCurrency = (c: { symbol: string; name: string; code: string }) =>
    `${getCurrencyFlag(c.code)} ${c.name} (${c.code})`;

  const listContent = (
    <>
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
    </>
  );

  // Mobile: full-screen bottom sheet
  if (isMobile) {
    return (
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex flex-col justify-end"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div className="bg-white dark:bg-[#14161f] rounded-t-xl max-h-[80vh] flex flex-col animate-slide-up">
          {/* Header */}
          <div className="flex items-center justify-between px-4 pt-4 pb-2">
            <h3 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Select Currency
            </h3>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#262a3a]"
            >
              <X className="h-4 w-4 text-[#9ca0b8]" />
            </button>
          </div>

          {/* Search */}
          <div className="px-4 pb-2">
            <input
              ref={inputRef}
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setHighlightedIndex(0);
              }}
              onKeyDown={handleKeyDown}
              placeholder="Search currencies..."
              autoComplete="off"
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 bg-white dark:bg-[#1e2130] text-sm text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa] focus:border-[#5c7cfa]"
            />
          </div>

          {/* List */}
          <div ref={listRef} className="overflow-y-auto flex-1 pb-safe">
            {listContent}
          </div>
        </div>
      </div>
    );
  }

  // Desktop: popover dropdown
  return (
    <div
      ref={listRef}
      className="absolute z-50 left-0 top-full mt-1 w-72 bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md shadow-lg flex flex-col max-h-80"
    >
      {/* Search */}
      <div className="p-2 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
        <input
          ref={inputRef}
          type="text"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setHighlightedIndex(0);
          }}
          onKeyDown={handleKeyDown}
          placeholder="Search currencies..."
          autoComplete="off"
          className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-2 py-1.5 bg-white dark:bg-[#1e2130] text-sm text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa] focus:border-[#5c7cfa]"
        />
      </div>

      {/* List */}
      <div className="overflow-y-auto flex-1">{listContent}</div>
    </div>
  );
};
