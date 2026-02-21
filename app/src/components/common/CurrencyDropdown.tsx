import { useState, useRef, useEffect } from 'react';
import { X } from 'lucide-react';
import { useCurrencies, splitCurrencies } from '@/hooks/useCurrencies';
import { getCurrencyFlag } from '@/utils/currencyFlags';
import { useIsMobile } from '@/hooks/useIsMobile';

interface CurrencyDropdownProps {
  value?: string;
  onSelect: (code: string) => void;
  onClose: () => void;
  /** Ref to the trigger element — clicks inside it won't close the dropdown. */
  triggerRef?: React.RefObject<HTMLElement | null>;
}

export const CurrencyDropdown = ({
  value,
  onSelect,
  onClose,
  triggerRef,
}: CurrencyDropdownProps) => {
  const isMobile = useIsMobile();
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

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
    const timer = setTimeout(() => inputRef.current?.focus(), 50);
    return () => clearTimeout(timer);
  }, []);

  // Scroll highlighted item into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[role="option"]');
      items[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  // Lock body scroll on mobile
  useEffect(() => {
    if (!isMobile) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [isMobile]);

  // Outside click detection (desktop only — mobile uses backdrop)
  useEffect(() => {
    if (isMobile) return;
    const handler = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        dropdownRef.current?.contains(target) ||
        triggerRef?.current?.contains(target)
      ) {
        return;
      }
      onClose();
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [isMobile, onClose, triggerRef]);

  const handleSelect = (code: string) => {
    onSelect(code);
    onClose();
  };

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

  const highlightedId =
    highlightedIndex >= 0 && highlightedIndex < allFiltered.length
      ? `currency-option-${allFiltered[highlightedIndex].code}`
      : undefined;

  const renderOption = (
    currency: { symbol: string; name: string; code: string },
    flatIndex: number
  ) => (
    <div
      key={currency.code}
      role="option"
      aria-selected={currency.code === value}
      id={`currency-option-${currency.code}`}
      onMouseDown={(e) => e.preventDefault()}
      onClick={() => handleSelect(currency.code)}
      onMouseEnter={() => setHighlightedIndex(flatIndex)}
      className={`w-full text-left px-3 py-2 text-sm cursor-pointer ${
        highlightedIndex === flatIndex
          ? 'bg-blue-50 dark:bg-blue-900/30'
          : currency.code === value
            ? 'bg-primary-100 dark:bg-primary-500/10'
            : ''
      }`}
    >
      {formatCurrency(currency)}
    </div>
  );

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
              {filteredTop.map((currency, localIndex) =>
                renderOption(currency, localIndex)
              )}
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
              {filteredOther.map((currency, localIndex) =>
                renderOption(currency, filteredTop.length + localIndex)
              )}
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
        role="dialog"
        aria-modal="true"
        aria-label="Select Currency"
        className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex flex-col justify-end"
        onMouseDown={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div
          ref={dropdownRef}
          className="bg-white dark:bg-[#14161f] rounded-t-xl max-h-[80vh] flex flex-col animate-slide-up"
        >
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
              aria-label="Search currencies"
              aria-activedescendant={highlightedId}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md px-3 py-2 bg-white dark:bg-[#1e2130] text-sm text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa] focus:border-[#5c7cfa]"
            />
          </div>

          {/* List */}
          <div
            ref={listRef}
            role="listbox"
            aria-label="Currencies"
            className="overflow-y-auto flex-1 pb-[env(safe-area-inset-bottom)]"
          >
            {listContent}
          </div>
        </div>
      </div>
    );
  }

  // Desktop: popover dropdown
  return (
    <div
      ref={dropdownRef}
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
          aria-label="Search currencies"
          aria-activedescendant={highlightedId}
          className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-2 py-1.5 bg-white dark:bg-[#1e2130] text-sm text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa] focus:border-[#5c7cfa]"
        />
      </div>

      {/* List */}
      <div
        ref={listRef}
        role="listbox"
        aria-label="Currencies"
        className="overflow-y-auto flex-1"
      >
        {listContent}
      </div>
    </div>
  );
};
