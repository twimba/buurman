import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { getContracts } from '@/api/contracts';
import { getCurrencySymbol } from '@/utils/currencies';
import { ChevronDown, FileText } from 'lucide-react';

interface ContractSelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  /** Filter contracts by status. Defaults to 'ACTIVE'. Pass undefined to load all. */
  status?: string;
  /** Show a "clear" option to deselect. */
  clearable?: boolean;
  /** Placeholder text when no contract is selected. */
  placeholder?: string;
}

export const ContractSelector = ({
  value,
  onChange,
  disabled = false,
  status = 'ACTIVE',
  clearable = false,
  placeholder,
}: ContractSelectorProps) => {
  const { t } = useTranslation('common');
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const { data: contractsData, isLoading } = useQuery({
    queryKey: ['contracts', status ?? 'ALL'],
    queryFn: () => getContracts(status ? { status } : undefined),
  });
  const contracts = contractsData?.content ?? [];

  const selectedContract = contracts.find((c) => c.identifier === value);
  const displayValue = selectedContract
    ? `${selectedContract.property.street} — ${getCurrencySymbol(selectedContract.rentAmountCurrency)} ${selectedContract.rentAmount.toFixed(2)}/mo`
    : '';

  const filtered = contracts.filter(
    (contract) =>
      contract.identifier.toLowerCase().includes(search.toLowerCase()) ||
      contract.property.street.toLowerCase().includes(search.toLowerCase()) ||
      `${contract.primaryContact.firstName} ${contract.primaryContact.lastName ?? ''}`
        .toLowerCase()
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
              : (placeholder ?? t('selectors.selectContract'))
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
              {t('selectors.loadingContracts')}
            </div>
          ) : filtered.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-text-secondary">
              {contracts.length === 0
                ? t('selectors.noContractsAvailable')
                : t('selectors.noContractsFound')}
            </div>
          ) : (
            <>
              {clearable && value && !search && (
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSelect('')}
                  className="w-full text-left px-3 py-2.5 text-sm text-text-secondary hover:bg-surface-inset border-b border-border-default"
                >
                  {t('selectors.allContracts')}
                </button>
              )}
              {filtered.map((contract, index) => (
                <button
                  key={contract.identifier}
                  type="button"
                  data-option
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSelect(contract.identifier)}
                  onMouseEnter={() => setHighlightedIndex(index)}
                  className={`w-full text-left px-3 py-3 flex items-center gap-3 ${
                    highlightedIndex === index
                      ? 'bg-primary-50'
                      : contract.identifier === value
                        ? 'bg-primary-100'
                        : ''
                  }`}
                >
                  <div className="w-10 h-10 rounded bg-info-bg flex items-center justify-center flex-shrink-0">
                    <FileText className="h-5 w-5 text-primary-500 dark:text-primary-300" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-text-primary">
                      {contract.property.street}
                    </div>
                    <div className="text-xs text-text-secondary">
                      {contract.primaryContact.firstName}{' '}
                      {contract.primaryContact.lastName} &middot;{' '}
                      {getCurrencySymbol(contract.rentAmountCurrency)}{' '}
                      {contract.rentAmount.toFixed(2)}/mo
                    </div>
                  </div>
                </button>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
};
