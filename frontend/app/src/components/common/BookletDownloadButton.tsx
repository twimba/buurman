import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { Download, X, Loader2 } from 'lucide-react';
import { Button, useToast } from '@buurman/ui';

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

interface BookletDownloadButtonProps {
  onDownload: (lang: string) => Promise<void>;
}

export const BookletDownloadButton = ({
  onDownload,
}: BookletDownloadButtonProps) => {
  const { t, i18n } = useTranslation('common');
  const { showToast } = useToast();
  const [showPopover, setShowPopover] = useState(false);
  const [downloadingLang, setDownloadingLang] = useState<string | null>(null);

  const currentLang = i18n.language.split('-')[0];
  const triggerRef = useRef<HTMLDivElement>(null);
  const [popoverPos, setPopoverPos] = useState<{ top: number; right: number } | null>(null);

  useEffect(() => {
    if (!showPopover || !triggerRef.current) {
      return;
    }
    const rect = triggerRef.current.getBoundingClientRect();
    setPopoverPos({
      top: rect.bottom + window.scrollY + 8,
      right: window.innerWidth - rect.right,
    });
  }, [showPopover]);

  const handleDownload = async (lang: string) => {
    setDownloadingLang(lang);
    try {
      await onDownload(lang);
      setShowPopover(false);
    } catch {
      showToast(t('bookletDownload.downloadFailed'), 'error');
    } finally {
      setDownloadingLang(null);
    }
  };

  const popover =
    showPopover && popoverPos
      ? createPortal(
          <>
            <div
              className="fixed inset-0 z-[9998]"
              onClick={() => setShowPopover(false)}
            />
            <div
              className="fixed w-72 bg-surface-card rounded-lg shadow-xl border border-border-default p-4 z-[9999]"
              style={{ top: popoverPos.top, right: popoverPos.right }}
            >
              <div className="flex items-center justify-between mb-3">
                <h4 className="font-medium text-sm text-text-primary">
                  {t('bookletDownload.title')}
                </h4>
                <button
                  onClick={() => setShowPopover(false)}
                  className="p-1 rounded hover:bg-surface-inset"
                >
                  <X className="h-4 w-4 text-text-muted" />
                </button>
              </div>
              <p className="text-xs text-text-secondary mb-3">
                {t('bookletDownload.description')}
              </p>
              <div className="space-y-0.5">
                {SUPPORTED_LANGUAGES.map(({ code, label }) => {
                  const isCurrentLang = code === currentLang;
                  const isDownloading = downloadingLang === code;
                  return (
                    <button
                      key={code}
                      onClick={() => handleDownload(code)}
                      disabled={downloadingLang !== null}
                      className={`w-full text-left px-3 py-2 rounded-lg text-sm flex items-center justify-between transition-colors disabled:opacity-50 ${
                        isCurrentLang
                          ? 'bg-primary-50 dark:bg-primary-500/10 text-primary-600 dark:text-primary-300 font-medium hover:bg-primary-100 dark:hover:bg-primary-500/20'
                          : 'text-text-primary hover:bg-surface-inset'
                      }`}
                    >
                      <span>{label}</span>
                      <span className="flex items-center gap-1">
                        {isDownloading && (
                          <Loader2 className="h-3 w-3 animate-spin" />
                        )}
                        {isCurrentLang && !isDownloading && (
                          <span className="text-xs text-text-muted font-normal">
                            {t('bookletDownload.yourLanguage')}
                          </span>
                        )}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </>,
          document.body
        )
      : null;

  return (
    <div ref={triggerRef}>
      <Button
        variant="primary"
        leftIcon={<Download />}
        onClick={() => setShowPopover((v) => !v)}
      >
        {t('bookletDownload.button')}
      </Button>
      {popover}
    </div>
  );
};
