import { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import {
  Download,
  ChevronDown,
  BookOpen,
  FileText,
  Loader2,
  Check,
} from 'lucide-react';
import { Button, useToast } from '@buurman/ui';

/** PDF languages, in menu order. The active UI language is pinned to the top at render. */
const SUPPORTED_LANGUAGES: { code: string; label: string }[] = [
  { code: 'en', label: 'English' },
  { code: 'nl', label: 'Nederlands' },
  { code: 'de', label: 'Deutsch' },
  { code: 'es', label: 'Español' },
  { code: 'fr', label: 'Français' },
  { code: 'pt', label: 'Português' },
  { code: 'it', label: 'Italiano' },
  { code: 'sv', label: 'Svenska' },
  { code: 'fi', label: 'Suomi' },
  { code: 'el', label: 'Ελληνικά' },
  { code: 'pl', label: 'Polski' },
  { code: 'da', label: 'Dansk' },
  { code: 'nb', label: 'Norsk' },
];

type DocType = 'booklet' | 'summary';

interface DocumentDownloadMenuProps {
  /** Full multi-page dossier in the given language. */
  onDownloadBooklet: (lang: string) => Promise<void>;
  /** One-page summary card in the given language. */
  onDownloadSummary: (lang: string) => Promise<void>;
}

/**
 * One unified entry point for both PDF outputs (full dossier + summary card) in any of the 13
 * supported languages. Replaces the two stand-alone download buttons: a single secondary button
 * opens a popover where the user picks the document type (segmented) and the language (list, with
 * their current UI language pinned and badged). Reused by the property/contract/contact detail pages.
 */
export const DocumentDownloadMenu = ({
  onDownloadBooklet,
  onDownloadSummary,
}: DocumentDownloadMenuProps) => {
  const { t, i18n } = useTranslation('common');
  const { showToast } = useToast();
  const [open, setOpen] = useState(false);
  const [docType, setDocType] = useState<DocType>('booklet');
  const [downloadingLang, setDownloadingLang] = useState<string | null>(null);

  const currentLang = i18n.language.split('-')[0];
  const triggerRef = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; right: number } | null>(null);

  useEffect(() => {
    if (!open || !triggerRef.current) {
      return;
    }
    const rect = triggerRef.current.getBoundingClientRect();
    setPos({
      top: rect.bottom + window.scrollY + 8,
      right: window.innerWidth - rect.right,
    });
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  // Active language first, then the rest in canonical order.
  const orderedLangs = (() => {
    const current = SUPPORTED_LANGUAGES.find((l) => l.code === currentLang);
    const rest = SUPPORTED_LANGUAGES.filter((l) => l.code !== currentLang);
    return current ? [current, ...rest] : SUPPORTED_LANGUAGES;
  })();

  const handleDownload = useCallback(
    async (lang: string) => {
      setDownloadingLang(lang);
      try {
        await (docType === 'booklet'
          ? onDownloadBooklet(lang)
          : onDownloadSummary(lang));
        setOpen(false);
      } catch {
        showToast(t('documentDownload.downloadFailed'), 'error');
      } finally {
        setDownloadingLang(null);
      }
    },
    [docType, onDownloadBooklet, onDownloadSummary, showToast, t]
  );

  const types: { key: DocType; icon: typeof BookOpen }[] = [
    { key: 'booklet', icon: BookOpen },
    { key: 'summary', icon: FileText },
  ];

  const popover =
    open && pos
      ? createPortal(
          <>
            <div
              className="fixed inset-0 z-[9998]"
              onClick={() => setOpen(false)}
              aria-hidden="true"
            />
            <div
              role="menu"
              aria-label={t('documentDownload.title')}
              className="fixed w-80 bg-surface-card rounded-xl shadow-xl border border-border-default z-[9999] overflow-hidden animate-in fade-in zoom-in-95 duration-100"
              style={{ top: pos.top, right: pos.right }}
            >
              {/* Document-type chooser */}
              <div className="p-3 pb-2">
                <div className="grid grid-cols-2 gap-1 p-1 bg-surface-inset rounded-lg">
                  {types.map(({ key, icon: Icon }) => {
                    const active = docType === key;
                    return (
                      <button
                        key={key}
                        type="button"
                        onClick={() => setDocType(key)}
                        aria-pressed={active}
                        className={`flex items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                          active
                            ? 'bg-surface-card text-text-primary shadow-sm'
                            : 'text-text-secondary hover:text-text-primary'
                        }`}
                      >
                        <Icon className="h-4 w-4" />
                        {t(`documentDownload.${key}.label`)}
                      </button>
                    );
                  })}
                </div>
                <p className="text-xs text-text-secondary mt-2 px-1">
                  {t(`documentDownload.${docType}.description`)}
                </p>
              </div>

              {/* Language list */}
              <div className="border-t border-border-subtle pt-2 pb-2">
                <div className="px-4 pb-1 text-[11px] font-semibold uppercase tracking-wide text-text-muted">
                  {t('documentDownload.language')}
                </div>
                <div className="max-h-72 overflow-y-auto px-2">
                  {orderedLangs.map(({ code, label }) => {
                    const isCurrent = code === currentLang;
                    const isDownloading = downloadingLang === code;
                    return (
                      <button
                        key={code}
                        type="button"
                        role="menuitem"
                        onClick={() => handleDownload(code)}
                        disabled={downloadingLang !== null}
                        className={`w-full flex items-center justify-between gap-2 px-3 py-2 rounded-lg text-sm transition-colors disabled:opacity-60 ${
                          isCurrent
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
                        {isDownloading ? (
                          <Loader2 className="h-4 w-4 animate-spin text-text-muted" />
                        ) : (
                          isCurrent && <Check className="h-4 w-4 text-primary-500" />
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </>,
          document.body
        )
      : null;

  return (
    <div ref={triggerRef}>
      <Button
        variant="secondary"
        leftIcon={<Download />}
        rightIcon={
          <ChevronDown
            className={`transition-transform duration-150 ${open ? 'rotate-180' : ''}`}
          />
        }
        aria-haspopup="menu"
        aria-expanded={open}
        disabled={downloadingLang !== null}
        onClick={() => setOpen((v) => !v)}
      >
        {t('documentDownload.button')}
      </Button>
      {popover}
    </div>
  );
};

DocumentDownloadMenu.displayName = 'DocumentDownloadMenu';
