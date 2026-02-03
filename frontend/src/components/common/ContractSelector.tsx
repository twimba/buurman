import { useState, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getContracts } from '@/api/contracts';
import { ChevronDown, FileText, Search } from 'lucide-react';

interface ContractSelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export const ContractSelector = ({
  value,
  onChange,
  disabled = false,
}: ContractSelectorProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const { data: contracts = [], isLoading } = useQuery({
    queryKey: ['contracts'],
    queryFn: () => getContracts(),
  });

  const selectedContract = contracts.find((c) => c.id === value);

  const filteredContracts = contracts.filter(
    (contract) =>
      contract.identifier.toLowerCase().includes(search.toLowerCase()) ||
      contract.property.street.toLowerCase().includes(search.toLowerCase()) ||
      contract.tenant.firstName.toLowerCase().includes(search.toLowerCase()) ||
      contract.tenant.lastName?.toLowerCase().includes(search.toLowerCase())
  );

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

  const handleSelect = (contractId: string) => {
    onChange(contractId);
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
        {selectedContract ? (
          <div className="flex items-center gap-2 min-w-0 flex-1">
            <div className="w-8 h-8 rounded bg-blue-100 flex items-center justify-center flex-shrink-0">
              <FileText className="h-4 w-4 text-blue-600" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-sm font-medium truncate">
                Contract #{selectedContract.identifier}
              </div>
              <div className="text-xs text-gray-500 truncate">
                {selectedContract.property.street} •{' '}
                {selectedContract.tenant.firstName}{' '}
                {selectedContract.tenant.lastName}
              </div>
            </div>
          </div>
        ) : (
          <span className="text-sm text-gray-500">Select a contract</span>
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
                placeholder="Search contracts..."
                className="w-full pl-9 pr-3 py-2 text-sm border border-gray-300 rounded focus:border-blue-600 focus:ring-1 focus:ring-blue-600 focus:outline-none"
              />
            </div>
          </div>

          {/* Contract list */}
          <div className="overflow-y-auto max-h-80">
            {isLoading ? (
              <div className="px-3 py-8 text-center text-sm text-gray-500">
                Loading contracts...
              </div>
            ) : filteredContracts.length === 0 ? (
              <div className="px-3 py-8 text-center text-sm text-gray-500">
                No contracts found
              </div>
            ) : (
              filteredContracts.map((contract) => (
                <button
                  key={contract.id}
                  type="button"
                  onClick={() => handleSelect(contract.id)}
                  className={`w-full text-left px-3 py-3 hover:bg-blue-50 flex items-center gap-3 ${
                    contract.id === value ? 'bg-blue-100' : ''
                  }`}
                >
                  <div className="w-10 h-10 rounded bg-blue-100 flex items-center justify-center flex-shrink-0">
                    <FileText className="h-5 w-5 text-blue-600" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-gray-900">
                      Contract #{contract.identifier}
                    </div>
                    <div className="text-xs text-gray-500">
                      {contract.property.street}, {contract.property.city}
                    </div>
                    <div className="text-xs text-gray-500">
                      {contract.tenant.firstName} {contract.tenant.lastName} •{' '}
                      {contract.status}
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
