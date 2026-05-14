import { useState, useRef, useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { getProperties } from '@/api/properties';
import { PropertyCategory, PropertyResponse } from '@/types/property';
import { usePropertyLabels } from '@/hooks/usePropertyLabels';
import { ChevronDown } from 'lucide-react';
import {
  PROPERTY_CATEGORY_ICONS,
  PROPERTY_TYPE_ICONS,
} from '@/utils/propertyIcons';

interface PropertySelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  /** Show a "clear" option to deselect. */
  clearable?: boolean;
  /** Placeholder text when no property is selected. */
  placeholder?: string;
}

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
  const { t } = useTranslation('common');
  const { typeLabel, categoryLabel } = usePropertyLabels();
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
      if (!groups.has(cat)) {
        groups.set(cat, []);
      }
      groups.get(cat)?.push(p);
    }
    // Sort groups by category order
    const sorted: { category: string; items: PropertyResponse[] }[] = [];
    for (const cat of categoryOrder) {
      const items = groups.get(cat);
      if (items) {
        sorted.push({ category: cat, items });
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
    const Icon =
      PROPERTY_TYPE_ICONS[property.propertyType] ??
      PROPERTY_CATEGORY_ICONS[property.propertyCategory as PropertyCategory] ??
      PROPERTY_CATEGORY_ICONS.RESIDENTIAL;
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
            ? 'bg-primary-50'
            : property.identifier === value
              ? 'bg-primary-100'
              : ''
        }`}
      >
        {(property.mainPhotoThumbnailUrl ?? property.mainPhotoUrl) ? (
          <img
            src={
              property.mainPhotoThumbnailUrl ??
              property.mainPhotoUrl ??
              undefined
            }
            alt={property.street}
            className="w-10 h-10 rounded object-cover flex-shrink-0"
            loading="lazy"
          />
        ) : (
          <div className="w-10 h-10 rounded bg-surface-inset flex items-center justify-center flex-shrink-0">
            <Icon className="h-5 w-5 text-text-muted " />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <div className="text-sm font-medium text-text-primary">
            {property.street}
          </div>
          <div className="text-xs text-text-secondary">
            {property.city}, {property.postalCode}
          </div>
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className="text-[10px] text-text-muted font-mono">
              #{property.identifier}
            </span>
            <span className="text-[10px] bg-surface-inset text-text-secondary px-1.5 py-0.5 rounded">
              {typeLabel(property.propertyType)}
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
            isOpen
              ? t('selectors.typeToSearch')
              : (placeholder ?? t('selectors.selectProperty'))
          }
          autoComplete="off"
          className="w-full border border-border-strong rounded px-3 py-2 pr-8 bg-surface-card hover:border-primary-500 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:bg-surface-inset disabled:cursor-not-allowed text-left text-sm text-text-primary"
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
          {isLoading ? (
            <div className="px-3 py-8 text-center text-sm text-text-secondary">
              {t('selectors.loadingProperties')}
            </div>
          ) : filtered.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-text-secondary">
              {t('selectors.noPropertiesFound')}
            </div>
          ) : (
            <>
              {clearable && (
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSelect('')}
                  className={`w-full text-left px-3 py-2.5 text-sm hover:bg-surface-inset border-b border-border-default ${
                    !value
                      ? 'text-text-primary font-medium'
                      : 'text-text-secondary'
                  }`}
                >
                  {t('selectors.allProperties')}
                </button>
              )}
              {grouped.map((group) => {
                const CatIcon =
                  PROPERTY_CATEGORY_ICONS[group.category as PropertyCategory] ??
                  PROPERTY_CATEGORY_ICONS.RESIDENTIAL;
                const header = showGroupHeaders ? (
                  <div
                    key={`header-${group.category}`}
                    className="px-3 py-2 text-xs font-semibold text-text-secondary uppercase tracking-wide bg-surface-page sticky top-0 border-b border-border-default flex items-center gap-1.5"
                  >
                    <CatIcon size={11} />
                    {categoryLabel(group.category)}
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
