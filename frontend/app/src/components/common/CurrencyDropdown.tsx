import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { useCurrencies, splitCurrencies } from '@/hooks/useCurrencies';
import type { CurrencyInfo } from '@/generated/models';
import { getCurrencyFlag } from '@/utils/currencyFlags';
import { useIsMobile } from '@/hooks/useIsMobile';

interface CurrencyDropdownProps {
  value?: string;
  onSelect: (code: string) => void;
  onClose: () => void;
  /** Ref to the trigger element — clicks inside it won't close the dropdown. */
  triggerRef?: React.RefObject<HTMLElement | null>;
}

export const CurrencyDropdown = ({
  value,
  onSelect,
  onClose,
  triggerRef,
}: CurrencyDropdownProps) => {
  const { t } = useTranslation('common');
  const isMobile = useIsMobile();
  const [search, setSearch] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const { data: currencies = [], isLoading } = useCurrencies();
  const { top: topCurrencies, other: otherCurrencies } =
    splitCurrencies(currencies);

  const filteredTop = topCurrencies.filter(
    (c) =>
      (c.code ?? '').toLowerCase().includes(search.toLowerCase()) ||
      (c.name ?? '').toLowerCase().includes(search.toLowerCase())
  );

  const filteredOther = otherCurrencies.filter(
    (c) =>
      (c.code ?? '').toLowerCase().includes(search.toLowerCase()) ||
      (c.name ?? '').toLowerCase().includes(search.toLowerCase())
  );

  const allFiltered = [...filteredTop, ...filteredOther];

  // Auto-focus search input on open
  useEffect(() => {
    const timer = setTimeout(() => inputRef.current?.focus(), 50);
    return () => clearTimeout(timer);
  }, []);

  // Scroll highlighted item into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const items = listRef.current.querySelectorAll('[role="option"]');
      items[highlightedIndex]?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  // Lock body scroll on mobile
  useEffect(() => {
    if (!isMobile) {
      return;
    }
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = prev;
    };
  }, [isMobile]);

  // Outside click detection (desktop only — mobile uses backdrop)
  useEffect(() => {
    if (isMobile) {
      return;
    }
    const handler = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        dropdownRef.current?.contains(target) ||
        triggerRef?.current?.contains(target)
      ) {
        return;
      }
      onClose();
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [isMobile, onClose, triggerRef]);

  // Desktop portal position — track trigger element
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);

  useEffect(() => {
    if (isMobile) {
      return;
    }
    const anchor = triggerRef?.current;
    if (!anchor) {
      return;
    }

    const update = () => {
      const rect = anchor.getBoundingClientRect();
      setPos({
        top: rect.bottom + window.scrollY + 4,
        left: rect.left + window.scrollX,
      });
    };
    update();

    window.addEventListener('scroll', update, true);
    window.addEventListener('resize', update);
    return () => {
      window.removeEventListener('scroll', update, true);
      window.removeEventListener('resize', update);
    };
  }, [isMobile, triggerRef]);

  const handleSelect = (code: string) => {
    onSelect(code);
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex((i) => Math.min(i + 1, allFiltered.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < allFiltered.length) {
          handleSelect(allFiltered[highlightedIndex].code ?? '');
        }
        break;
      case 'Escape':
        e.preventDefault();
        onClose();
        break;
      case 'Tab':
        onClose();
        break;
    }
  };

  const formatCurrency = (c: CurrencyInfo) =>
    `${getCurrencyFlag(c.code ?? '')} ${c.name ?? ''} (${c.code ?? ''})`;

  const highlightedId =
    highlightedIndex >= 0 && highlightedIndex < allFiltered.length
      ? `currency-option-${allFiltered[highlightedIndex].code}`
      : undefined;

  const renderOption = (currency: CurrencyInfo, flatIndex: number) => (
    <div
      key={currency.code}
      role="option"
      aria-selected={currency.code === value}
      id={`currency-option-${currency.code}`}
      onMouseDown={(e) => e.preventDefault()}
      onClick={() => handleSelect(currency.code ?? '')}
      onMouseEnter={() => setHighlightedIndex(flatIndex)}
      className={`w-full text-left px-3 py-2 text-sm cursor-pointer ${
        highlightedIndex === flatIndex
          ? 'bg-primary-50'
          : currency.code === value
            ? 'bg-primary-100 dark:bg-primary-500/10'
            : ''
      }`}
    >
      {formatCurrency(currency)}
    </div>
  );

  const listContent = (
    <>
      {isLoading ? (
        <div className="px-3 py-8 text-center text-sm text-text-secondary">
          Loading currencies...
        </div>
      ) : (
        <>
          {filteredTop.length > 0 && (
            <div>
              <div className="px-3 py-2 text-xs font-semibold text-text-secondary bg-surface-page sticky top-0">
                Common
              </div>
              {filteredTop.map((currency, localIndex) =>
                renderOption(currency, localIndex)
              )}
            </div>
          )}

          {filteredTop.length > 0 && filteredOther.length > 0 && (
            <div className="border-t border-border-default my-1" />
          )}

          {filteredOther.length > 0 && (
            <div>
              <div className="px-3 py-2 text-xs font-semibold text-text-secondary bg-surface-page sticky top-0">
                Other Currencies
              </div>
              {filteredOther.map((currency, localIndex) =>
                renderOption(currency, filteredTop.length + localIndex)
              )}
            </div>
          )}

          {allFiltered.length === 0 && (
            <div className="px-3 py-8 text-center text-sm text-text-secondary">
              No currencies found
            </div>
          )}
        </>
      )}
    </>
  );

  // Mobile: full-screen bottom sheet
  if (isMobile) {
    return (
      <div
        role="dialog"
        aria-modal="true"
        aria-label={t('selectors.selectCurrency')}
        className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex flex-col justify-end"
        onMouseDown={(e) => {
          if (e.target === e.currentTarget) {
            onClose();
          }
        }}
      >
        <div
          ref={dropdownRef}
          className="bg-surface-card rounded-t-xl max-h-[80vh] flex flex-col animate-slide-up"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 pt-4 pb-2">
            <h3 className="text-sm font-semibold text-text-primary">
              Select Currency
            </h3>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded hover:bg-surface-inset dark:hover:bg-surface-raised"
            >
              <X className="h-4 w-4 text-text-muted" />
            </button>
          </div>

          {/* Search */}
          <div className="px-4 pb-2">
            <input
              ref={inputRef}
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setHighlightedIndex(0);
              }}
              onKeyDown={handleKeyDown}
              placeholder={t('selectors.searchCurrencies')}
              autoComplete="off"
              aria-label={t('selectors.searchCurrencies')}
              aria-activedescendant={highlightedId}
              className="w-full border border-border-strong rounded-md px-3 py-2 bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>

          {/* List */}
          <div
            ref={listRef}
            role="listbox"
            aria-label={t('selectors.currencies')}
            className="overflow-y-auto flex-1 pb-safe"
          >
            {listContent}
          </div>
        </div>
      </div>
    );
  }

  // Desktop: portal-rendered popover (escapes overflow:hidden ancestors)
  if (!pos) {
    return null;
  }

  return createPortal(
    <div
      ref={dropdownRef}
      style={{ position: 'absolute', top: pos.top, left: pos.left }}
      className="z-50 w-72 bg-surface-card border border-border-strong rounded-md shadow-lg flex flex-col max-h-80"
    >
      {/* Search */}
      <div className="p-2 border-b border-border-default">
        <input
          ref={inputRef}
          type="text"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setHighlightedIndex(0);
          }}
          onKeyDown={handleKeyDown}
          placeholder={t('selectors.searchCurrencies')}
          autoComplete="off"
          aria-label={t('selectors.searchCurrencies')}
          aria-activedescendant={highlightedId}
          className="w-full border border-border-strong rounded px-2 py-1.5 bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
        />
      </div>

      {/* List */}
      <div
        ref={listRef}
        role="listbox"
        aria-label={t('selectors.currencies')}
        className="overflow-y-auto flex-1"
      >
        {listContent}
      </div>
    </div>,
    document.body
  );
};
