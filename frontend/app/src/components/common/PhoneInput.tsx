import { useState, useRef, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';
import { parsePhoneNumber, isValidPhoneNumber } from 'libphonenumber-js/max';
import type { CountryCode } from 'libphonenumber-js/max';
import {
  countriesWithCallingCode,
  type CountryWithCallingCode,
} from '@/utils/countries';

interface PhoneInputProps {
  value: string | null;
  onChange: (e164: string | null) => void;
  defaultCountryCode?: string;
  disabled?: boolean;
  error?: string;
  allowedCountryCodes?: string[];
}

export const validatePhoneE164 = (
  value: string | null | undefined
): string | null => {
  if (!value || !value.trim()) {
    return null;
  }
  if (!isValidPhoneNumber(value)) {
    return 'Please enter a valid phone number';
  }
  return null;
};

export const PhoneInput = ({
  value,
  onChange,
  defaultCountryCode = 'NL',
  disabled = false,
  error,
  allowedCountryCodes,
}: PhoneInputProps) => {
  const [selectedCountryCode, setSelectedCountryCode] =
    useState<string>(defaultCountryCode);
  const [nationalNumber, setNationalNumber] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);

  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const isInternalChange = useRef(false);

  const availableCountries = allowedCountryCodes
    ? countriesWithCallingCode.filter((c) =>
        allowedCountryCodes.includes(c.code)
      )
    : countriesWithCallingCode;

  const selectedCountry =
    availableCountries.find((c) => c.code === selectedCountryCode) ||
    countriesWithCallingCode.find((c) => c.code === selectedCountryCode);

  const filtered = availableCountries.filter(
    (c) =>
      c.name.toLowerCase().includes(search.toLowerCase()) ||
      c.code.toLowerCase().includes(search.toLowerCase()) ||
      `+${c.callingCode}`.includes(search)
  );

  // Decompose E.164 value into country + national number
  useEffect(() => {
    if (isInternalChange.current) {
      isInternalChange.current = false;
      return;
    }
    if (value) {
      try {
        const parsed = parsePhoneNumber(value);
        if (parsed && parsed.country) {
          // eslint-disable-next-line react-hooks/set-state-in-effect -- prop-to-state sync
          setSelectedCountryCode(parsed.country);
          setNationalNumber(parsed.nationalNumber);
          return;
        }
      } catch {
        // Malformed — fall through
      }
    }
    if (!value) {
      setNationalNumber('');
    }
  }, [value]);

  // Click-outside handler
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setDropdownOpen(false);
        setSearch('');
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Scroll highlighted item into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[data-option]');
      items[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  // Focus search input when dropdown opens
  useEffect(() => {
    if (dropdownOpen && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [dropdownOpen]);

  const composeAndEmit = (countryCode: string, national: string) => {
    const cleaned = national.replace(/[\s\-()]/g, '');
    if (!cleaned) {
      onChange(null);
      return;
    }
    try {
      const parsed = parsePhoneNumber(cleaned, countryCode as CountryCode);
      if (parsed) {
        isInternalChange.current = true;
        onChange(parsed.format('E.164'));
        return;
      }
    } catch {
      // Fall through to manual composition
    }
    const country = countriesWithCallingCode.find(
      (c) => c.code === countryCode
    );
    if (country) {
      isInternalChange.current = true;
      onChange(`+${country.callingCode}${cleaned.replace(/^0+/, '')}`);
    }
  };

  const handleNationalNumberChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const input = e.target.value;
    setNationalNumber(input);
    composeAndEmit(selectedCountryCode, input);
  };

  const handleCountrySelect = (country: CountryWithCallingCode) => {
    setSelectedCountryCode(country.code);
    setDropdownOpen(false);
    setSearch('');
    composeAndEmit(country.code, nationalNumber);
  };

  const handleDropdownKeyDown = (e: React.KeyboardEvent) => {
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
          handleCountrySelect(filtered[highlightedIndex]);
        }
        break;
      case 'Escape':
      case 'Tab':
        e.preventDefault();
        setDropdownOpen(false);
        setSearch('');
        break;
    }
  };

  return (
    <div ref={containerRef} className="relative">
      <div
        className={`flex border rounded ${
          error ? 'border-error-border' : 'border-border-strong'
        } focus-within:border-primary-500 focus-within:ring-1 focus-within:ring-primary-500 bg-surface-card ${
          disabled ? 'bg-surface-inset cursor-not-allowed opacity-70' : ''
        }`}
      >
        <button
          type="button"
          onClick={() => {
            if (!disabled) {
              setDropdownOpen(!dropdownOpen);
              setSearch('');
              setHighlightedIndex(-1);
            }
          }}
          disabled={disabled}
          className="flex items-center gap-1 px-3 py-2 border-r border-border-strong text-sm shrink-0 hover:bg-surface-page dark:hover:bg-surface-card disabled:hover:bg-transparent dark:disabled:hover:bg-transparent disabled:cursor-not-allowed"
        >
          <span>{selectedCountry?.flag || '🏳️'}</span>
          <span className="text-text-secondary">
            +{selectedCountry?.callingCode || '?'}
          </span>
          <ChevronDown
            className={`h-3 w-3 text-text-muted transition-transform ${
              dropdownOpen ? 'rotate-180' : ''
            }`}
          />
        </button>

        <input
          type="tel"
          value={nationalNumber}
          onChange={handleNationalNumberChange}
          disabled={disabled}
          placeholder="Phone number"
          className="flex-1 px-3 py-2 bg-transparent text-sm text-text-primary outline-none disabled:cursor-not-allowed"
        />
      </div>

      {dropdownOpen && (
        <div className="absolute z-50 w-full mt-1 bg-surface-card border border-border-strong rounded-md shadow-lg max-h-80 overflow-hidden flex flex-col">
          <div className="p-2 border-b border-border-default">
            <input
              ref={searchInputRef}
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setHighlightedIndex(0);
              }}
              onKeyDown={handleDropdownKeyDown}
              placeholder="Search country or code..."
              autoComplete="off"
              className="w-full px-2 py-1.5 text-sm border border-border-strong rounded bg-surface-card text-text-primary outline-none focus:border-primary-500"
            />
          </div>

          <div ref={listRef} className="overflow-y-auto">
            {filtered.map((country, index) => (
              <button
                key={country.code}
                type="button"
                data-option
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => handleCountrySelect(country)}
                onMouseEnter={() => setHighlightedIndex(index)}
                className={`w-full text-left px-3 py-2 text-sm flex items-center gap-2 ${
                  highlightedIndex === index
                    ? 'bg-primary-50'
                    : country.code === selectedCountryCode
                      ? 'bg-primary-100 dark:bg-primary-500/10'
                      : ''
                }`}
              >
                <span>{country.flag}</span>
                <span className="text-text-secondary w-12">
                  +{country.callingCode}
                </span>
                <span className="text-text-primary">{country.name}</span>
              </button>
            ))}

            {filtered.length === 0 && (
              <div className="px-3 py-8 text-center text-sm text-text-secondary">
                No countries found
              </div>
            )}
          </div>
        </div>
      )}

      {error && <p className="text-error-text text-xs mt-1">{error}</p>}
    </div>
  );
};
