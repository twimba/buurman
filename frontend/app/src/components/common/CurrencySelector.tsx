import { useState, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown } from 'lucide-react';
import { useCurrencies, getCurrencySymbol } from '@/hooks/useCurrencies';
import { getCurrencyFlag } from '@/utils/currencyFlags';
import { CurrencyDropdown } from './CurrencyDropdown';

interface CurrencySelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export const CurrencySelector = ({
  value,
  onChange,
  disabled = false,
}: CurrencySelectorProps) => {
  const { t } = useTranslation('common');
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { data: currencies } = useCurrencies();

  const symbol = getCurrencySymbol(currencies, value ?? '');
  const flag = value ? getCurrencyFlag(value) : '';
  const displayValue = value ? `${flag} ${symbol} ${value}` : '';

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => {
          if (!disabled) {
            setIsOpen((o) => !o);
          }
        }}
        disabled={disabled}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        className="w-full border border-border-strong rounded px-3 py-2 pr-8 bg-surface-card hover:border-primary-500 dark:hover:border-primary-400 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:bg-surface-inset disabled:cursor-not-allowed text-sm text-text-primary text-left relative"
      >
        {displayValue || (
          <span className="text-text-muted">{t('selectors.selectCurrency')}</span>
        )}
        <ChevronDown
          className={`absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </button>

      {isOpen && (
        <CurrencyDropdown
          value={value}
          onSelect={onChange}
          onClose={() => setIsOpen(false)}
          triggerRef={containerRef}
        />
      )}
    </div>
  );
};
