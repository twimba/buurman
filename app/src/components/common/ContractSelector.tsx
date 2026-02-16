import { useState, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getContracts } from '@/api/contracts';
import { ChevronDown, FileText } from 'lucide-react';

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
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const { data: contractsData, isLoading } = useQuery({
    queryKey: ['contracts'],
    queryFn: () => getContracts(),
  });
  const contracts = contractsData?.content ?? [];

  const selectedContract = contracts.find((c) => c.identifier === value);
  const displayValue = selectedContract
    ? `Contract #${selectedContract.identifier} - ${selectedContract.property.street}`
    : '';

  const filtered = contracts.filter(
    (contract) =>
      contract.identifier.toLowerCase().includes(search.toLowerCase()) ||
      contract.property.street.toLowerCase().includes(search.toLowerCase()) ||
      contract.primaryTenant.firstName
        .toLowerCase()
        .includes(search.toLowerCase()) ||
      contract.primaryTenant.lastName
        ?.toLowerCase()
        .includes(search.toLowerCase())
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

  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[data-option]');
      items[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  const handleSelect = (contractId: string) => {
    onChange(contractId);
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
          handleSelect(filtered[highlightedIndex].identifier);
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
          placeholder={isOpen ? 'Type to search...' : 'Select a contract'}
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
              Loading contracts...
            </div>
          ) : filtered.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-[#6b7194] dark:text-[#8b90a8]">
              No contracts found
            </div>
          ) : (
            filtered.map((contract, index) => (
              <button
                key={contract.identifier}
                type="button"
                data-option
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => handleSelect(contract.identifier)}
                onMouseEnter={() => setHighlightedIndex(index)}
                className={`w-full text-left px-3 py-3 flex items-center gap-3 ${
                  highlightedIndex === index
                    ? 'bg-blue-50 dark:bg-blue-900/30'
                    : contract.identifier === value
                      ? 'bg-blue-100 dark:bg-blue-900'
                      : ''
                }`}
              >
                <div className="w-10 h-10 rounded bg-blue-100 dark:bg-blue-900 flex items-center justify-center flex-shrink-0">
                  <FileText className="h-5 w-5 text-primary-500 dark:text-primary-300" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                    Contract #{contract.identifier}
                  </div>
                  <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                    {contract.property.street}, {contract.property.city}
                  </div>
                  <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                    {contract.primaryTenant.firstName}{' '}
                    {contract.primaryTenant.lastName} • {contract.status}
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
