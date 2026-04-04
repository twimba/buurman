import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { AuthenticatedBroadcastBanner } from './common/BroadcastBanner';
import { OnboardingWizard } from './onboarding/OnboardingWizard';
import { useOnboardingStatus } from '@/hooks/useOnboarding';

const STORAGE_KEY = 'buurman-sidebar-collapsed';

interface LayoutProps {
  children?: React.ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });
  const [wizardDismissed, setWizardDismissed] = useState(false);

  const { data: onboarding } = useOnboardingStatus();
  const showWizard = onboarding && !onboarding.completed && !wizardDismissed;

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
    <div
      className="flex overflow-hidden bg-surface-page"
      style={{
        height: 'calc(100vh - var(--env-banner-height, 0px))',
        marginTop: 'var(--env-banner-height, 0px)',
      }}
    >
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[100] focus:rounded-md focus:bg-surface-card focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-text-primary focus:shadow-lg focus-ring"
      >
        Skip to main content
      </a>
      <Sidebar collapsed={collapsed} onToggleCollapse={toggleCollapsed} />
      <main
        id="main-content"
        className={`flex-1 overflow-auto transition-[margin] duration-300 ease-in-out ${collapsed ? 'lg:ml-20' : 'lg:ml-64'}`}
      >
        <div className="max-w-screen-2xl mx-auto px-4 py-6 md:px-6 md:py-8 lg:px-8">
          <AuthenticatedBroadcastBanner />
          {children ?? <Outlet />}
        </div>
      </main>

      {showWizard && (
        <OnboardingWizard
          onComplete={() => setWizardDismissed(true)}
          currentCountryCode={onboarding.currentCountryCode}
          currentCurrency={onboarding.currentCurrency}
        />
      )}
    </div>
  );
};

Layout.displayName = 'Layout';
