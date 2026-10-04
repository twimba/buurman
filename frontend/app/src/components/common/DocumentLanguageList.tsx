import { useTranslation } from 'react-i18next';
import { Check, Loader2 } from 'lucide-react';
import {
  orderDocumentLanguages,
  type DocumentLanguageCode,
} from './documentLanguages';

interface DocumentLanguageListProps {
  /** Base code of the active UI language; pinned to the top and badged "Your language". */
  uiLanguage: string;
  /** Highlighted + checked entry; defaults to the UI language (the booklet menu's behaviour). */
  selected?: DocumentLanguageCode;
  /** Entry showing a spinner instead of the check while its action runs. */
  pendingCode?: string | null;
  disabled?: boolean;
  /** `action`: each entry triggers something (menuitem). `select`: it picks a value (menuitemradio). */
  mode?: 'action' | 'select';
  /** Top divider separating the list from content above it in the same popover. */
  divided?: boolean;
  onSelect: (code: DocumentLanguageCode) => void;
}

/**
 * The document-language list shared by every PDF output (booklets, summary cards, lease
 * agreement): the 13 PDF languages in canonical order, the user's UI language pinned first.
 */
export const DocumentLanguageList = ({
  uiLanguage,
  selected,
  pendingCode = null,
  disabled = false,
  mode = 'action',
  divided = true,
  onSelect,
}: DocumentLanguageListProps) => {
  const { t } = useTranslation('common');
  const highlighted = selected ?? uiLanguage;

  return (
    <div
      className={`${divided ? 'border-t border-border-subtle ' : ''}pt-2 pb-2`}
    >
      <div className="px-4 pb-1 text-[11px] font-semibold uppercase tracking-wide text-text-muted">
        {t('documentDownload.language')}
      </div>
      <div className="max-h-72 overflow-y-auto px-2">
        {orderDocumentLanguages(uiLanguage).map(({ code, label }) => {
          const isCurrent = code === uiLanguage;
          const isSelected = code === highlighted;
          const isPending = pendingCode === code;
          return (
            <button
              key={code}
              type="button"
              role={mode === 'select' ? 'menuitemradio' : 'menuitem'}
              aria-checked={mode === 'select' ? isSelected : undefined}
              onClick={() => onSelect(code)}
              disabled={disabled}
              className={`w-full flex items-center justify-between gap-2 px-3 py-2 rounded-lg text-sm transition-colors disabled:opacity-60 ${
                isSelected
                  ? 'text-primary-600 dark:text-primary-300 font-medium hover:bg-primary-50 dark:hover:bg-primary-500/10'
                  : 'text-text-primary hover:bg-surface-inset'
              }`}
            >
              <span className="flex items-center gap-2">
                {label}
                {isCurrent && (
                  <span className="text-[10px] font-normal uppercase tracking-wide text-text-muted">
                    {t('documentDownload.yourLanguage')}
                  </span>
                )}
              </span>
              {isPending ? (
                <Loader2 className="h-4 w-4 animate-spin text-text-muted" />
              ) : (
                isSelected && <Check className="h-4 w-4 text-primary-500" />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};

DocumentLanguageList.displayName = 'DocumentLanguageList';
