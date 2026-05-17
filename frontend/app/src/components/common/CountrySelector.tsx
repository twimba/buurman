import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { countries } from '@/utils/countries';
import { ChevronDown, Search, Check } from 'lucide-react';
import { Sheet } from '@buurman/ui';

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
  placeholder,
}: CountrySelectorProps) => {
  const { t } = useTranslation('common');
  const resolvedPlaceholder = placeholder ?? t('selectors.selectCountry');
  const [isOpen, setIsOpen] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  // Detect phone viewport so we can route the picker to a Sheet on mobile while
  // keeping the existing combobox behavior on tablet/desktop pixel-equivalently.
  const [isPhone, setIsPhone] = useState(() =>
    typeof window !== 'undefined'
      ? window.matchMedia('(max-width: 767px)').matches
      : false
  );
  useEffect(() => {
    const mq = window.matchMedia('(max-width: 767px)');
    const handler = (e: MediaQueryListEvent) => setIsPhone(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  const selected = value ? countries.find((c) => c.code === value) : undefined;
  const displayValue = selected ? `${selected.flag} ${selected.name}` : '';

  const filtered = countries.filter(
    (c) =>
      c.name.toLowerCase().includes(search.toLowerCase()) ||
      c.code.toLowerCase().includes(search.toLowerCase())
  );

  useEffect(() => {
    if (isPhone) {
      return;
    }
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
  }, [onBlur, isPhone]);

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

  // Phone: render a tap-to-open button that opens the Sheet picker.
  if (isPhone) {
    return (
      <>
        <button
          type="button"
          onClick={() => {
            if (!disabled) {
              setSearch('');
              setSheetOpen(true);
            }
          }}
          disabled={disabled}
          aria-haspopup="dialog"
          aria-expanded={sheetOpen}
          className="w-full text-left flex items-center justify-between min-h-touch border border-border-strong rounded px-3 py-2 bg-surface-card hover:border-primary-500 dark:hover:border-primary-400 focus-ring disabled:bg-surface-inset disabled:cursor-not-allowed text-sm"
        >
          <span
            className={displayValue ? 'text-text-primary' : 'text-text-muted'}
          >
            {displayValue || resolvedPlaceholder}
          </span>
          <ChevronDown className="h-4 w-4 text-text-muted flex-shrink-0 ml-2" />
        </button>

        <Sheet
          open={sheetOpen}
          onClose={() => {
            setSheetOpen(false);
            setSearch('');
            onBlur?.();
          }}
          title={resolvedPlaceholder}
        >
          <div className="space-y-3">
            <label className="block">
              <span className="sr-only">{t('selectors.typeToSearch')}</span>
              <span className="relative block">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted pointer-events-none" />
                <input
                  type="search"
                  autoFocus
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder={t('selectors.typeToSearch')}
                  inputMode="search"
                  className="w-full pl-10 pr-3 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500 text-base"
                />
              </span>
            </label>

            <ul className="-mx-2" role="listbox">
              {filtered.length === 0 ? (
                <li className="px-3 py-8 text-center text-sm text-text-secondary">
                  {t('selectors.noCountriesFound')}
                </li>
              ) : (
                filtered.map((country) => {
                  const isSelected = country.code === value;
                  return (
                    <li key={country.code}>
                      <button
                        type="button"
                        role="option"
                        aria-selected={isSelected}
                        onClick={() => {
                          onChange(country.code);
                          setSheetOpen(false);
                          setSearch('');
                        }}
                        className={`w-full text-left px-3 py-3 min-h-touch flex items-center gap-3 rounded ${
                          isSelected
                            ? 'bg-primary-50 dark:bg-primary-500/10 text-primary-700 dark:text-primary-300'
                            : 'hover:bg-surface-inset'
                        }`}
                      >
                        <span className="text-lg" aria-hidden>
                          {country.flag}
                        </span>
                        <span className="flex-1 truncate">{country.name}</span>
                        {isSelected && (
                          <Check className="h-4 w-4 text-primary-500" />
                        )}
                      </button>
                    </li>
                  );
                })
              )}
            </ul>
          </div>
        </Sheet>
      </>
    );
  }

  // Tablet / Desktop: original combobox (pixel-equivalent to pre-change behavior).
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
          placeholder={
            isOpen ? t('selectors.typeToSearch') : resolvedPlaceholder
          }
          autoComplete="off"
          className="w-full border border-border-strong rounded px-3 py-2 pr-8 bg-surface-card hover:border-primary-500 dark:hover:border-primary-400 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:bg-surface-inset disabled:cursor-not-allowed text-sm text-text-primary"
        />
        <ChevronDown
          className={`absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted pointer-events-none transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </div>

      {isOpen && (
        <div
          ref={listRef}
          className="absolute z-50 w-full mt-1 bg-surface-card border border-border-strong rounded-md shadow-lg max-h-80 overflow-y-auto"
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
                  ? 'bg-primary-50'
                  : country.code === value
                    ? 'bg-primary-100 dark:bg-primary-500/10'
                    : ''
              }`}
            >
              {country.flag} {country.name}
            </button>
          ))}

          {filtered.length === 0 && (
            <div className="px-3 py-8 text-center text-sm text-text-secondary">
              {t('selectors.noCountriesFound')}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
