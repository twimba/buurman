import {
  useState,
  useRef,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { Download, ChevronDown, Loader2 } from 'lucide-react';

export interface ExportOption {
  label: string;
  onExport: () => void | Promise<void>;
  /** Optional custom icon rendered next to the label. Defaults to a Download icon. */
  icon?: ReactNode;
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

  if (options.length === 0) {
    return null;
  }

  const showDropdown = options.length > 1;
  const isBusy = disabled || exporting;

  const renderIcon = (icon: ReactNode | undefined, smallSize: boolean) => {
    if (icon) {
      return icon;
    }
    return smallSize ? (
      <Download className="h-3.5 w-3.5" />
    ) : (
      <Download className="h-4 w-4" />
    );
  };

  const spinnerSm = <Loader2 className="h-3.5 w-3.5 animate-spin" />;
  const spinnerMd = <Loader2 className="h-4 w-4 animate-spin" />;

  if (!showDropdown) {
    const firstOption = options[0];
    if (size === 'sm') {
      return (
        <button
          onClick={() => firstOption.onExport()}
          disabled={isBusy}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {exporting ? spinnerSm : renderIcon(firstOption.icon, true)}
          {exporting ? t('buttons.exporting') : firstOption.label}
        </button>
      );
    }
    return (
      <button
        onClick={() => firstOption.onExport()}
        disabled={isBusy}
        className="flex items-center gap-2 px-4 py-2 bg-surface-card border border-border-strong text-text-secondary rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {exporting ? spinnerMd : renderIcon(firstOption.icon, false)}
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
                width: size === 'sm' ? '10rem' : '12rem',
                borderColor:
                  size === 'sm'
                    ? 'var(--color-border-default)'
                    : 'var(--color-border-strong)',
              }}
            >
              {options.map((option) => (
                <button
                  key={option.label}
                  disabled={disabled || exporting}
                  onClick={() => {
                    setOpen(false);
                    option.onExport();
                  }}
                  className={`flex w-full items-center gap-2 text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${
                    size === 'sm' ? 'px-3 py-2 text-xs' : 'px-4 py-2 text-sm'
                  }`}
                >
                  {renderIcon(option.icon, true)}
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
          onClick={() => !isBusy && setOpen((prev) => !prev)}
          disabled={isBusy}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {exporting ? spinnerSm : <Download className="h-3.5 w-3.5" />}
          {exporting ? t('buttons.exporting') : t('buttons.export')}
          {!exporting && <ChevronDown className="h-3 w-3" />}
        </button>
        {dropdownMenu}
      </>
    );
  }

  return (
    <>
      <button
        ref={triggerRef}
        onClick={() => !isBusy && setOpen((prev) => !prev)}
        disabled={isBusy}
        className="flex items-center gap-2 px-4 py-2 bg-surface-card border border-border-strong text-text-secondary rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {exporting ? spinnerMd : <Download className="h-4 w-4" />}
        {exporting ? t('buttons.exporting') : t('buttons.export')}
        {!exporting && <ChevronDown className="h-3.5 w-3.5" />}
      </button>
      {dropdownMenu}
    </>
  );
};
