import { useState, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getTenants } from '@/api/tenants';
import { ChevronDown } from 'lucide-react';
import { Avatar } from './Avatar';

interface TenantSelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export const TenantSelector = ({
  value,
  onChange,
  disabled = false,
}: TenantSelectorProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const { data: tenantsData, isLoading } = useQuery({
    queryKey: ['tenants', debouncedSearch],
    queryFn: () => getTenants({ search: debouncedSearch || undefined }),
  });
  const tenants = tenantsData?.content ?? [];

  const selectedTenant = tenants.find((t) => t.identifier === value);
  const displayValue = selectedTenant
    ? `${selectedTenant.firstName} ${selectedTenant.lastName}`
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

  const handleSelect = (tenantId: string) => {
    onChange(tenantId);
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
        setHighlightedIndex((i) => Math.min(i + 1, tenants.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < tenants.length) {
          handleSelect(tenants[highlightedIndex].identifier);
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
          placeholder={isOpen ? 'Type to search...' : 'Select a tenant'}
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
              Searching tenants...
            </div>
          ) : tenants.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-text-secondary">
              No tenants found
            </div>
          ) : (
            tenants.map((tenant, index) => (
              <button
                key={tenant.identifier}
                type="button"
                data-option
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => handleSelect(tenant.identifier)}
                onMouseEnter={() => setHighlightedIndex(index)}
                className={`w-full text-left px-3 py-3 flex items-center gap-3 ${
                  highlightedIndex === index
                    ? 'bg-primary-50'
                    : tenant.identifier === value
                      ? 'bg-primary-100'
                      : ''
                }`}
              >
                <Avatar
                  firstName={tenant.firstName}
                  lastName={tenant.lastName}
                  photoUrl={tenant.mainPhotoThumbnailUrl ?? tenant.mainPhotoUrl}
                  size="md"
                />
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium text-text-primary">
                    {tenant.firstName} {tenant.lastName}
                  </div>
                  <div className="text-xs text-text-secondary truncate">
                    {tenant.email}
                  </div>
                  {tenant.phone && (
                    <div className="text-xs text-text-muted">
                      {tenant.phone}
                    </div>
                  )}
                  <div className="text-xs text-text-muted">
                    #{tenant.identifier}
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
};
