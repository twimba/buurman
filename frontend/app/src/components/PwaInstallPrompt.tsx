import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Download, X } from 'lucide-react';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';

const DISMISSED_STORAGE_KEY = 'buurman.pwa.installDismissedAt';
// Re-show window: don't nag again for 14 days after dismissal.
const REDISPLAY_AFTER_MS = 1000 * 60 * 60 * 24 * 14;

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/**
 * Captures the browser's `beforeinstallprompt` event (Chrome/Edge/Brave on
 * Android + desktop) and surfaces a custom in-app install CTA — a one-line
 * banner above the BottomTabBar on phone, a small card top-right on desktop.
 *
 * Also listens for `appinstalled` and fires telemetry. iOS Safari doesn't
 * support these events; on iOS the component is a no-op (no banner shown).
 *
 * Mounts once at app root. Banner respects a 14-day cool-down after
 * dismissal via localStorage.
 */
export const PwaInstallPrompt = () => {
  const { t } = useTranslation('common');
  const promptRef = useRef<BeforeInstallPromptEvent | null>(null);
  const [show, setShow] = useState(false);

  useEffect(() => {
    const handleBeforeInstall = (e: Event) => {
      // Stash the event — calling prompt() later requires it.
      e.preventDefault();
      promptRef.current = e as BeforeInstallPromptEvent;
      trackEvent(AnalyticsEvent.PWA_INSTALL_PROMPT_AVAILABLE);

      // Check cool-down before showing.
      try {
        const dismissedAt = localStorage.getItem(DISMISSED_STORAGE_KEY);
        if (
          dismissedAt &&
          Date.now() - Number(dismissedAt) < REDISPLAY_AFTER_MS
        ) {
          return;
        }
      } catch {
        // localStorage unavailable (private mode); show anyway.
      }
      setShow(true);
      trackEvent(AnalyticsEvent.PWA_INSTALL_PROMPT_SHOWN);
    };

    const handleInstalled = () => {
      setShow(false);
      promptRef.current = null;
      trackEvent(AnalyticsEvent.PWA_INSTALLED);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    window.addEventListener('appinstalled', handleInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
      window.removeEventListener('appinstalled', handleInstalled);
    };
  }, []);

  const handleInstall = useCallback(async () => {
    const prompt = promptRef.current;
    if (!prompt) {
      return;
    }
    await prompt.prompt();
    const { outcome } = await prompt.userChoice;
    if (outcome === 'accepted') {
      trackEvent(AnalyticsEvent.PWA_INSTALL_ACCEPTED);
    } else {
      trackEvent(AnalyticsEvent.PWA_INSTALL_DISMISSED);
    }
    setShow(false);
    promptRef.current = null;
  }, []);

  const handleDismiss = useCallback(() => {
    try {
      localStorage.setItem(DISMISSED_STORAGE_KEY, String(Date.now()));
    } catch {
      // ignore
    }
    setShow(false);
    trackEvent(AnalyticsEvent.PWA_INSTALL_DISMISSED);
  }, []);

  if (!show) {
    return null;
  }

  return (
    <div
      role="region"
      aria-label="App install prompt"
      className="fixed inset-x-0 z-40 md:left-auto md:right-4 md:max-w-sm flex items-center gap-3 px-4 py-3 bg-surface-card border border-border-default shadow-lg md:rounded-lg"
      style={{
        // Phone: above the BottomTabBar + safe area. Desktop: 1rem from top.
        bottom: 'calc(var(--bottomnav-h, 0px) + var(--safe-bottom, 0px))',
        top: 'auto',
      }}
    >
      <Download className="h-5 w-5 text-primary-500 flex-shrink-0" />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-text-primary">
          {t('pwa.installTitle', { defaultValue: 'Install Buurman' })}
        </p>
        <p className="text-xs text-text-secondary truncate">
          {t('pwa.installSubtitle', {
            defaultValue: 'Faster access, full-screen, works offline',
          })}
        </p>
      </div>
      <button
        type="button"
        onClick={handleInstall}
        className="min-h-touch px-3 py-1.5 rounded-md bg-primary-500 text-white text-sm font-medium hover:bg-primary-600 focus-ring active:scale-[0.97]"
      >
        {t('pwa.installAction', { defaultValue: 'Install' })}
      </button>
      <button
        type="button"
        onClick={handleDismiss}
        aria-label={t('buttons.dismiss', { defaultValue: 'Dismiss' })}
        className="min-h-touch min-w-touch -m-2 p-2 inline-flex items-center justify-center text-text-muted"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
};

PwaInstallPrompt.displayName = 'PwaInstallPrompt';
