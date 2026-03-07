import { useState, useRef, useEffect } from 'react';
import { countries } from '@/utils/countries';
import { ChevronDown } from 'lucide-react';

interface CountrySelectorProps {
  value?: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  disabled?: boolean;
  placeholder?: string;
}

export const CountrySelector = ({
  value,
  onChange,
  onBlur,
  disabled = false,
  placeholder = 'Select a country',
}: CountrySelectorProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const selected = value ? countries.find((c) => c.code === value) : undefined;
  const displayValue = selected ? `${selected.flag} ${selected.name}` : '';

  const filtered = countries.filter(
    (c) =>
      c.name.toLowerCase().includes(search.toLowerCase()) ||
      c.code.toLowerCase().includes(search.toLowerCase())
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
        setSearch('');
        onBlur?.();
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onBlur]);

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
        setHighlightedIndex((i) => Math.min(i + 1, filtered.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < filtered.length) {
          handleSelect(filtered[highlightedIndex].code);
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
        onBlur?.();
        break;
    }
  };

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
            if (!isOpen) {
              setIsOpen(true);
            }
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
          placeholder={isOpen ? 'Type to search...' : placeholder}
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
          {filtered.map((country, index) => (
            <button
              key={country.code}
              type="button"
              data-option
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => handleSelect(country.code)}
              onMouseEnter={() => setHighlightedIndex(index)}
              className={`w-full text-left px-3 py-2 text-sm ${
                highlightedIndex === index
                  ? 'bg-blue-50 dark:bg-blue-900/30'
                  : country.code === value
                    ? 'bg-primary-100 dark:bg-primary-500/10'
                    : ''
              }`}
            >
              {country.flag} {country.name}
            </button>
          ))}

          {filtered.length === 0 && (
            <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
              No countries found
            </div>
          )}
        </div>
      )}
    </div>
  );
};
