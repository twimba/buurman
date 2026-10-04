import { useState, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { Download, ChevronDown, BookOpen, FileText } from 'lucide-react';
import { Button, useToast } from '@buurman/ui';
import { DocumentLanguageList } from './DocumentLanguageList';
import { baseLanguage } from './documentLanguages';
import { useAnchoredPopover } from './useAnchoredPopover';

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
  const { open, setOpen, triggerRef, popoverRef, popoverStyle } = useAnchoredPopover();
  const [docType, setDocType] = useState<DocType>('booklet');
  const [downloadingLang, setDownloadingLang] = useState<string | null>(null);

  const currentLang = baseLanguage(i18n.language);

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
    [docType, onDownloadBooklet, onDownloadSummary, setOpen, showToast, t]
  );

  const types: { key: DocType; icon: typeof BookOpen }[] = [
    { key: 'booklet', icon: BookOpen },
    { key: 'summary', icon: FileText },
  ];

  const popover =
    open
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
              className="fixed w-80 bg-surface-card rounded-xl shadow-xl border border-border-default z-[9999] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-100"
              ref={popoverRef}
              style={popoverStyle}
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

              <DocumentLanguageList
                uiLanguage={currentLang}
                pendingCode={downloadingLang}
                disabled={downloadingLang !== null}
                onSelect={handleDownload}
              />
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
