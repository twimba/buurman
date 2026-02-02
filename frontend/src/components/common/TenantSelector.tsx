import { useState, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getTenants } from '@/api/tenants';
import { ChevronDown, Search } from 'lucide-react';
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
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);

    return () => clearTimeout(timer);
  }, [search]);

  const { data: tenants = [], isLoading } = useQuery({
    queryKey: ['tenants', debouncedSearch],
    queryFn: () => getTenants(debouncedSearch || undefined),
  });

  const selectedTenant = tenants.find((t) => t.id === value);

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

  const handleSelect = (tenantId: string) => {
    onChange(tenantId);
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
        className="w-full flex items-center justify-between border border-gray-300 rounded px-3 py-2 bg-white hover:border-blue-600 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 disabled:bg-gray-100 disabled:cursor-not-allowed text-left"
      >
        {selectedTenant ? (
          <div className="flex items-center gap-2 min-w-0 flex-1">
            <Avatar
              firstName={selectedTenant.firstName}
              lastName={selectedTenant.lastName}
              photoUrl={selectedTenant.mainPhotoUrl}
              size="sm"
            />
            <div className="min-w-0 flex-1">
              <div className="text-sm font-medium truncate">
                {selectedTenant.firstName} {selectedTenant.lastName}
              </div>
              <div className="text-xs text-gray-500 truncate">
                {selectedTenant.email} • #{selectedTenant.identifier}
              </div>
            </div>
          </div>
        ) : (
          <span className="text-sm text-gray-500">Select a tenant</span>
        )}
        <ChevronDown
          className={`h-4 w-4 text-gray-400 transition-transform flex-shrink-0 ml-2 ${isOpen ? 'rotate-180' : ''}`}
        />
      </button>

      {isOpen && (
        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg max-h-96 overflow-hidden">
          {/* Search input */}
          <div className="p-2 border-b border-gray-200">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                ref={inputRef}
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search tenants..."
                className="w-full pl-9 pr-3 py-2 text-sm border border-gray-300 rounded focus:border-blue-600 focus:ring-1 focus:ring-blue-600 focus:outline-none"
              />
            </div>
          </div>

          {/* Tenant list */}
          <div className="overflow-y-auto max-h-80">
            {isLoading ? (
              <div className="px-3 py-8 text-center text-sm text-gray-500">
                Searching tenants...
              </div>
            ) : tenants.length === 0 ? (
              <div className="px-3 py-8 text-center text-sm text-gray-500">
                No tenants found
              </div>
            ) : (
              tenants.map((tenant) => (
                <button
                  key={tenant.id}
                  type="button"
                  onClick={() => handleSelect(tenant.id)}
                  className={`w-full text-left px-3 py-3 hover:bg-blue-50 flex items-center gap-3 ${
                    tenant.id === value ? 'bg-blue-100' : ''
                  }`}
                >
                  <Avatar
                    firstName={tenant.firstName}
                    lastName={tenant.lastName}
                    photoUrl={tenant.mainPhotoUrl}
                    size="md"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-gray-900">
                      {tenant.firstName} {tenant.lastName}
                    </div>
                    <div className="text-xs text-gray-500 truncate">
                      {tenant.email}
                    </div>
                    {tenant.phone && (
                      <div className="text-xs text-gray-400">
                        {tenant.phone}
                      </div>
                    )}
                    <div className="text-xs text-gray-400">
                      #{tenant.identifier}
                    </div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
