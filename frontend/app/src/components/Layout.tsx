import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { BottomTabBar } from './BottomTabBar';
import { AuthenticatedBroadcastBanner } from './common/BroadcastBanner';
import { OnboardingWizard } from './onboarding/OnboardingWizard';
import { PwaInstallPrompt } from './PwaInstallPrompt';
import { useOnboardingStatus } from '@/hooks/useOnboarding';
import { MobileNavContext } from '@/context/MobileNavContext';
import { useKeyboardInset } from '@/hooks/useKeyboardInset';

const STORAGE_KEY = 'buurman-sidebar-collapsed';

interface LayoutProps {
  children?: React.ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  const { t } = useTranslation('common');
  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });
  const [wizardDismissed, setWizardDismissed] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [hasOwnMenuButton, setHasOwnMenuButton] = useState(false);
  const [hasOwnBottomFooter, setHasOwnBottomFooter] = useState(false);

  // Rail-mode opt-out (localStorage 'buurman.mobile.sidebarRail' = 'false'). Default on.
  // Read once at mount; used to skip the md:ml-16 margin reservation.
  const [railEnabled] = useState(() => {
    try {
      return localStorage.getItem('buurman.mobile.sidebarRail') !== 'false';
    } catch {
      return true;
    }
  });

  const isKeyboardOpen = useKeyboardInset();

  const { data: onboarding } = useOnboardingStatus();
  const showWizard = onboarding && !onboarding.completed && !wizardDismissed;

  const openMobileNav = useCallback(() => setMobileNavOpen(true), []);
  const closeMobileNav = useCallback(() => setMobileNavOpen(false), []);
  const mobileNavValue = useMemo(
    () => ({
      open: openMobileNav,
      close: closeMobileNav,
      isOpen: mobileNavOpen,
      hasOwnMenuButton,
      setHasOwnMenuButton,
      isKeyboardOpen,
      hasOwnBottomFooter,
      setHasOwnBottomFooter,
    }),
    [
      openMobileNav,
      closeMobileNav,
      mobileNavOpen,
      hasOwnMenuButton,
      isKeyboardOpen,
      hasOwnBottomFooter,
    ]
  );

  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    try {
      localStorage.setItem(STORAGE_KEY, String(next));
    } catch {
      /* noop */
    }
  };

  return (
    <MobileNavContext.Provider value={mobileNavValue}>
      <div
        className="flex overflow-hidden bg-surface-page"
        style={{
          height: 'calc(100dvh - var(--env-banner-height, 0px))',
          marginTop: 'var(--env-banner-height, 0px)',
        }}
      >
        {/* Global a11y live region for useAnnounce(). Visually hidden. */}
        <div
          id="a11y-live"
          role="status"
          aria-live="polite"
          aria-atomic="true"
          className="sr-only"
        />
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[100] focus:rounded-md focus:bg-surface-card focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-text-primary focus:shadow-lg focus-ring"
        >
          {t('accessibility.skipToContent')}
        </a>
        <Sidebar
          collapsed={collapsed}
          onToggleCollapse={toggleCollapsed}
          mobileOpen={mobileNavOpen}
          onMobileOpenChange={setMobileNavOpen}
        />
        <main
          id="main-content"
          className={`flex-1 overflow-auto overscroll-y-contain transition-[margin] duration-300 ease-in-out ${railEnabled ? 'md:ml-16' : ''} ${collapsed ? 'lg:ml-20' : 'lg:ml-64'}`}
        >
          <div className="max-w-screen-2xl mx-auto px-4 py-6 md:px-6 md:py-8 lg:px-8 pl-safe pr-safe pb-[calc(var(--bottomnav-h)+var(--safe-bottom,0px)+var(--kbd-inset,0px))] md:pb-0">
            <AuthenticatedBroadcastBanner />
            {children ?? <Outlet />}
          </div>
        </main>
        <BottomTabBar />
        <PwaInstallPrompt />

        {showWizard && (
          <OnboardingWizard
            onComplete={() => setWizardDismissed(true)}
            currentCountryCode={onboarding.currentCountryCode}
            currentCurrency={onboarding.currentCurrency}
          />
        )}
      </div>
    </MobileNavContext.Provider>
  );
};

Layout.displayName = 'Layout';
