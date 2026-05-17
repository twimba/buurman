import { LayoutDashboard, Home, Users, DollarSign, Menu } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { LucideIcon } from 'lucide-react';
import { useMobileNav } from '@/context/MobileNavContext';

interface Tab {
  to?: string;
  label: string;
  icon: LucideIcon;
  onClick?: () => void;
}

/**
 * iPhone-only bottom tab bar with 5 destinations: Dashboard / Properties /
 * Contacts / Payments / More. "More" opens the existing mobile drawer (via
 * MobileNavContext) which still hosts everything else.
 *
 * Mounted only at viewports `<md`. `md:hidden`. Respects safe-area-inset-bottom.
 */
export const BottomTabBar = () => {
  const { t } = useTranslation('navigation');
  const { open: openDrawer } = useMobileNav();

  const tabs: Tab[] = [
    { to: '/dashboard', label: t('sidebar.dashboard'), icon: LayoutDashboard },
    { to: '/properties', label: t('sidebar.properties'), icon: Home },
    { to: '/contacts', label: t('sidebar.contacts'), icon: Users },
    { to: '/payments', label: t('sidebar.payments'), icon: DollarSign },
    { onClick: openDrawer, label: t('sidebar.more', 'More'), icon: Menu },
  ];

  return (
    <nav
      role="navigation"
      aria-label={t('accessibility.bottomNav', 'Primary')}
      className="md:hidden fixed inset-x-0 bottom-0 z-40 bg-surface-card/95 backdrop-blur-xl border-t border-border-default flex items-stretch justify-around"
      style={{ paddingBottom: 'var(--safe-bottom, 0px)' }}
    >
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const content = (isActive: boolean) => (
          <span
            className={`flex flex-col items-center justify-center gap-0.5 min-h-touch px-2 py-2 flex-1 text-[11px] leading-none ${
              isActive
                ? 'text-primary-600 dark:text-primary-300 font-semibold'
                : 'text-text-secondary'
            }`}
          >
            <Icon className="h-5 w-5" />
            <span>{tab.label}</span>
          </span>
        );

        if (tab.to) {
          return (
            <NavLink
              key={tab.label}
              to={tab.to}
              className="flex-1 inline-flex items-stretch focus-ring"
            >
              {({ isActive }) => content(isActive)}
            </NavLink>
          );
        }
        return (
          <button
            key={tab.label}
            type="button"
            onClick={tab.onClick}
            className="flex-1 inline-flex items-stretch focus-ring"
          >
            {content(false)}
          </button>
        );
      })}
    </nav>
  );
};

BottomTabBar.displayName = 'BottomTabBar';
