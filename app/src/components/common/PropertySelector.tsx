import { useState, useRef, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getProperties } from '@/api/properties';
import {
  PropertyCategory,
  PROPERTY_CATEGORY_LABELS,
  PROPERTY_TYPE_LABELS,
  PropertyResponse,
} from '@/types/property';
import { ChevronDown, Home, Building2, Factory, Tractor } from 'lucide-react';

interface PropertySelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  /** Show a "clear" option to deselect. */
  clearable?: boolean;
  /** Placeholder text when no property is selected. */
  placeholder?: string;
}

const categoryIcons: Record<string, typeof Home> = {
  [PropertyCategory.RESIDENTIAL]: Home,
  [PropertyCategory.COMMERCIAL]: Building2,
  [PropertyCategory.INDUSTRIAL]: Factory,
  [PropertyCategory.AGRICULTURAL]: Tractor,
  [PropertyCategory.MIXED_USE]: Building2,
};

const categoryOrder: PropertyCategory[] = [
  PropertyCategory.RESIDENTIAL,
  PropertyCategory.COMMERCIAL,
  PropertyCategory.INDUSTRIAL,
  PropertyCategory.AGRICULTURAL,
  PropertyCategory.MIXED_USE,
];

export const PropertySelector = ({
  value,
  onChange,
  disabled = false,
  clearable = false,
  placeholder,
}: PropertySelectorProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const { data: propertiesData, isLoading } = useQuery({
    queryKey: ['properties'],
    queryFn: () => getProperties(),
  });
  const properties = useMemo(
    () => propertiesData?.content ?? [],
    [propertiesData]
  );

  const selectedProperty = properties.find((p) => p.identifier === value);
  const displayValue = selectedProperty
    ? `${selectedProperty.street}, ${selectedProperty.city}`
    : '';

  const filtered = useMemo(
    () =>
      properties.filter(
        (property) =>
          property.street.toLowerCase().includes(search.toLowerCase()) ||
          property.city.toLowerCase().includes(search.toLowerCase()) ||
          property.identifier.toLowerCase().includes(search.toLowerCase()) ||
          property.postalCode?.toLowerCase().includes(search.toLowerCase())
      ),
    [properties, search]
  );

  // Group filtered properties by category
  const grouped = useMemo(() => {
    const groups = new Map<string, PropertyResponse[]>();
    for (const p of filtered) {
      const cat = p.propertyCategory ?? 'OTHER';
      if (!groups.has(cat)) groups.set(cat, []);
      groups.get(cat)!.push(p);
    }
    // Sort groups by category order
    const sorted: { category: string; items: PropertyResponse[] }[] = [];
    for (const cat of categoryOrder) {
      if (groups.has(cat)) {
        sorted.push({ category: cat, items: groups.get(cat)! });
        groups.delete(cat);
      }
    }
    // Any remaining categories
    for (const [cat, items] of groups) {
      sorted.push({ category: cat, items });
    }
    return sorted;
  }, [filtered]);

  // Flat list for keyboard navigation
  const flatList = useMemo(() => filtered, [filtered]);

  // Only show group headers when there's more than one category
  const showGroupHeaders = grouped.length > 1;

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

  const handleSelect = (propertyId: string) => {
    onChange(propertyId);
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
        setHighlightedIndex((i) => Math.min(i + 1, flatList.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < flatList.length) {
          handleSelect(flatList[highlightedIndex].identifier);
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

  const renderPropertyItem = (
    property: PropertyResponse,
    flatIndex: number
  ) => {
    const Icon = categoryIcons[property.propertyCategory] ?? Home;
    return (
      <button
        key={property.identifier}
        type="button"
        data-option
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => handleSelect(property.identifier)}
        onMouseEnter={() => setHighlightedIndex(flatIndex)}
        className={`w-full text-left px-3 py-3 flex items-center gap-3 ${
          highlightedIndex === flatIndex
            ? 'bg-blue-50 dark:bg-blue-900/30'
            : property.identifier === value
              ? 'bg-blue-100 dark:bg-blue-900'
              : ''
        }`}
      >
        {(property.mainPhotoThumbnailUrl ?? property.mainPhotoUrl) ? (
          <img
            src={(property.mainPhotoThumbnailUrl ?? property.mainPhotoUrl)!}
            alt={property.street}
            className="w-10 h-10 rounded object-cover flex-shrink-0"
            loading="lazy"
          />
        ) : (
          <div className="w-10 h-10 rounded bg-[#e8ecf4] dark:bg-[#3a3f54] flex items-center justify-center flex-shrink-0">
            <Icon className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
            {property.street}
          </div>
          <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
            {property.city}, {property.postalCode}
          </div>
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className="text-[10px] text-[#9ca0b8] dark:text-[#5c6180] font-mono">
              #{property.identifier}
            </span>
            <span className="text-[10px] bg-[#f1f3f9] dark:bg-[#1a1d28] text-[#6b7194] dark:text-[#8b90a8] px-1.5 py-0.5 rounded">
              {PROPERTY_TYPE_LABELS[property.propertyType] ??
                property.propertyType}
            </span>
          </div>
        </div>
      </button>
    );
  };

  // Build flat index map for grouped rendering
  let flatIndex = 0;

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
          placeholder={
            isOpen ? 'Type to search...' : (placeholder ?? 'Select a property')
          }
          autoComplete="off"
          className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 pr-8 bg-white dark:bg-[#1e2130] hover:border-[#5c7cfa] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] disabled:bg-[#f1f3f9] dark:disabled:bg-[#3a3f54] disabled:cursor-not-allowed text-left text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
        />
        <ChevronDown
          className={`absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180] pointer-events-none transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </div>

      {isOpen && (
        <div
          ref={listRef}
          className="absolute z-50 w-full mt-1 bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md shadow-lg max-h-80 overflow-y-auto"
        >
          {isLoading ? (
            <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
              Loading properties...
            </div>
          ) : filtered.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
              No properties found
            </div>
          ) : (
            <>
              {clearable && (
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSelect('')}
                  className={`w-full text-left px-3 py-2.5 text-sm hover:bg-[#f1f3f9] dark:hover:bg-[#14161f] border-b border-[#e2e6f0] dark:border-[#2a2e3f] ${
                    !value
                      ? 'text-[#1a1d2e] dark:text-[#eef0f6] font-medium'
                      : 'text-[#6b7194] dark:text-[#8b90a8]'
                  }`}
                >
                  All Properties
                </button>
              )}
              {grouped.map((group) => {
                const header = showGroupHeaders ? (
                  <div
                    key={`header-${group.category}`}
                    className="px-3 py-2 text-xs font-semibold text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide bg-[#f8f9fc] dark:bg-[#0c0d14] sticky top-0 border-b border-[#e2e6f0] dark:border-[#2a2e3f]"
                  >
                    {PROPERTY_CATEGORY_LABELS[
                      group.category as PropertyCategory
                    ] ?? group.category}
                  </div>
                ) : null;

                const items = group.items.map((property) => {
                  const idx = flatIndex++;
                  return renderPropertyItem(property, idx);
                });

                return (
                  <div key={group.category}>
                    {header}
                    {items}
                  </div>
                );
              })}
            </>
          )}
        </div>
      )}
    </div>
  );
};
