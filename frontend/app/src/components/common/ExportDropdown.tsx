import { useState, useRef, useEffect } from 'react';
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
  const ref = useRef<HTMLDivElement>(null);
  const { isEnabled } = useFeatureFlags();
  const { t } = useTranslation('common');

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const showDropdown =
    isEnabled(FeatureFlags.EXCEL_EXPORT) && options.length > 1;

  if (size === 'sm') {
    const smButtonClass =
      'inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50';

    if (!showDropdown) {
      const firstOption = options[0];
      return (
        <button
          onClick={() => firstOption.onExport()}
          disabled={disabled}
          className={smButtonClass}
        >
          <Download className="h-3.5 w-3.5" />
          {exporting ? t('buttons.exporting') : firstOption.label}
        </button>
      );
    }

    return (
      <div className="relative" ref={ref}>
        <button
          onClick={() => setOpen((prev) => !prev)}
          disabled={disabled}
          className={smButtonClass}
        >
          <Download className="h-3.5 w-3.5" />
          {exporting ? t('buttons.exporting') : t('buttons.export')}
          <ChevronDown className="h-3 w-3" />
        </button>
        {open && (
          <div className="absolute right-0 mt-1 w-32 bg-surface-card border border-border-default rounded-md shadow-lg z-10">
            {options.map((option) => (
              <button
                key={option.label}
                onClick={() => {
                  setOpen(false);
                  option.onExport();
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-xs text-text-secondary hover:bg-surface-inset transition-colors"
              >
                <Download className="h-3.5 w-3.5" />
                {option.label}
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  // md size
  const mdButtonClass =
    'flex items-center gap-2 px-4 py-2 bg-surface-card border border-border-strong text-text-secondary rounded-md hover:bg-surface-inset transition-colors';

  if (!showDropdown) {
    const firstOption = options[0];
    return (
      <button
        onClick={() => firstOption.onExport()}
        disabled={disabled}
        className={mdButtonClass}
      >
        <Download className="h-4 w-4" />
        {exporting ? t('buttons.exporting') : firstOption.label}
      </button>
    );
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((prev) => !prev)}
        disabled={disabled}
        className={mdButtonClass}
      >
        <Download className="h-4 w-4" />
        {t('buttons.export')}
        <ChevronDown className="h-3.5 w-3.5" />
      </button>
      {open && (
        <div className="absolute right-0 mt-1 w-36 bg-surface-card border border-border-strong rounded-md shadow-lg z-10">
          {options.map((option) => (
            <button
              key={option.label}
              onClick={() => {
                setOpen(false);
                option.onExport();
              }}
              className="flex w-full items-center gap-2 px-4 py-2 text-sm text-text-secondary hover:bg-surface-inset transition-colors"
            >
              <Download className="h-3.5 w-3.5" />
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
