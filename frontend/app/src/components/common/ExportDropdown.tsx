import { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { Download, ChevronDown } from 'lucide-react';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';

export interface ExportOption {
  label: string;
  onExport: () => void | Promise<void>;
}

export interface ExportDropdownProps {
  options: ExportOption[];
  disabled?: boolean;
  size?: 'sm' | 'md';
  exporting?: boolean;
}

export const ExportDropdown = ({
  options,
  disabled = false,
  size = 'md',
  exporting = false,
}: ExportDropdownProps) => {
  const [open, setOpen] = useState(false);
  const [dropdownPos, setDropdownPos] = useState<{
    top: number;
    right: number;
  } | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const { isEnabled } = useFeatureFlags();
  const { t } = useTranslation('common');

  const recalcPos = useCallback(() => {
    if (!triggerRef.current) {
      return;
    }
    const rect = triggerRef.current.getBoundingClientRect();
    setDropdownPos({
      top: rect.bottom + window.scrollY + 4,
      right: window.innerWidth - rect.right,
    });
  }, []);

  useEffect(() => {
    if (open) {
      recalcPos();
    }
  }, [open, recalcPos]);

  const showDropdown =
    isEnabled(FeatureFlags.EXCEL_EXPORT) && options.length > 1;

  if (!showDropdown) {
    const firstOption = options[0];
    if (size === 'sm') {
      return (
        <button
          onClick={() => firstOption.onExport()}
          disabled={disabled}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50"
        >
          <Download className="h-3.5 w-3.5" />
          {exporting ? t('buttons.exporting') : firstOption.label}
        </button>
      );
    }
    return (
      <button
        onClick={() => firstOption.onExport()}
        disabled={disabled}
        className="flex items-center gap-2 px-4 py-2 bg-surface-card border border-border-strong text-text-secondary rounded-md hover:bg-surface-inset transition-colors"
      >
        <Download className="h-4 w-4" />
        {exporting ? t('buttons.exporting') : firstOption.label}
      </button>
    );
  }

  const dropdownMenu =
    open && dropdownPos
      ? createPortal(
          <>
            <div
              className="fixed inset-0 z-[9998]"
              onClick={() => setOpen(false)}
            />
            <div
              className="fixed z-[9999] bg-surface-card border rounded-md shadow-lg"
              style={{
                top: dropdownPos.top,
                right: dropdownPos.right,
                width: size === 'sm' ? '8rem' : '9rem',
                borderColor:
                  size === 'sm'
                    ? 'var(--color-border-default)'
                    : 'var(--color-border-strong)',
              }}
            >
              {options.map((option) => (
                <button
                  key={option.label}
                  onClick={() => {
                    setOpen(false);
                    option.onExport();
                  }}
                  className={`flex w-full items-center gap-2 text-text-secondary hover:bg-surface-inset transition-colors ${
                    size === 'sm'
                      ? 'px-3 py-2 text-xs'
                      : 'px-4 py-2 text-sm'
                  }`}
                >
                  <Download className={size === 'sm' ? 'h-3.5 w-3.5' : 'h-3.5 w-3.5'} />
                  {option.label}
                </button>
              ))}
            </div>
          </>,
          document.body
        )
      : null;

  if (size === 'sm') {
    return (
      <>
        <button
          ref={triggerRef}
          onClick={() => setOpen((prev) => !prev)}
          disabled={disabled}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50"
        >
          <Download className="h-3.5 w-3.5" />
          {exporting ? t('buttons.exporting') : t('buttons.export')}
          <ChevronDown className="h-3 w-3" />
        </button>
        {dropdownMenu}
      </>
    );
  }

  return (
    <>
      <button
        ref={triggerRef}
        onClick={() => setOpen((prev) => !prev)}
        disabled={disabled}
        className="flex items-center gap-2 px-4 py-2 bg-surface-card border border-border-strong text-text-secondary rounded-md hover:bg-surface-inset transition-colors"
      >
        <Download className="h-4 w-4" />
        {t('buttons.export')}
        <ChevronDown className="h-3.5 w-3.5" />
      </button>
      {dropdownMenu}
    </>
  );
};
