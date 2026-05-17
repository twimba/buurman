import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Home,
  Users,
  FileText,
  DollarSign,
  Receipt,
  Folder,
  Image,
  BarChart3,
  CreditCard,
  Calendar,
  Bell,
  ClipboardList,
  Download,
  Settings,
  LogOut,
  Menu,
  X,
  Shield,
  ChevronDown,
  ChevronsLeft,
  ChevronsRight,
  Scale,
  TrendingUp,
  FileCode,
} from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { SidebarTooltip } from '@buurman/ui';
import { useMobileNav } from '@/context/MobileNavContext';
import { useAuth } from '@/context/AuthContext';
import { useTeam } from '@/context/TeamContext';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import { TeamSwitcher } from './TeamSwitcher';
import type { LucideIcon } from 'lucide-react';

interface NavItem {
  name: string;
  href: string;
  icon: LucideIcon;
  featureFlag?: string;
  children?: NavChild[];
}

interface NavChild {
  name: string;
  href: string;
  icon: LucideIcon;
  featureFlag?: string;
}

const navLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-3 px-3 py-2.5 rounded-lg
  transition-all duration-200
  ${collapsed ? 'lg:justify-center' : ''}
  ${
    isActive
      ? 'bg-primary-50 dark:bg-primary-500/15 text-primary-700 dark:text-primary-300 font-semibold'
      : 'text-text-secondary hover:bg-surface-inset hover:text-text-primary'
  }
`;

interface SidebarProps {
  collapsed: boolean;
  onToggleCollapse: () => void;
  /** Controlled mobile-drawer state. If omitted, the Sidebar manages its own state. */
  mobileOpen?: boolean;
  onMobileOpenChange?: (next: boolean) => void;
}

export const Sidebar = ({
  collapsed: userCollapsed,
  onToggleCollapse,
  mobileOpen: controlledMobileOpen,
  onMobileOpenChange,
}: SidebarProps) => {
  const [uncontrolledMobileOpen, setUncontrolledMobileOpen] = useState(false);
  const isControlled = controlledMobileOpen !== undefined;
  const mobileOpen = isControlled
    ? controlledMobileOpen
    : uncontrolledMobileOpen;
  const setMobileOpen = useCallback(
    (next: boolean) => {
      if (isControlled) {
        onMobileOpenChange?.(next);
      } else {
        setUncontrolledMobileOpen(next);
      }
    },
    [isControlled, onMobileOpenChange]
  );
  const { logout } = useAuth();
  const { teams, canEditTeamSettings } = useTeam();
  const { isEnabled } = useFeatureFlags();
  const location = useLocation();
  const { t } = useTranslation('navigation');

  const navigation: NavItem[] = useMemo(
    () => [
      {
        name: t('sidebar.dashboard'),
        href: '/dashboard',
        icon: LayoutDashboard,
      },
      { name: t('sidebar.properties'), href: '/properties', icon: Home },
      { name: t('sidebar.contacts'), href: '/contacts', icon: Users },
      {
        name: t('sidebar.contracts'),
        href: '/contracts',
        icon: FileText,
        children: [
          {
            name: t('sidebar.rentRegulations'),
            href: '/rent-regulations',
            icon: Scale,
          },
          {
            name: t('sidebar.rentAdjustments'),
            href: '/rent-increases/apply',
            icon: TrendingUp,
          },
          {
            name: t('sidebar.paymentInstructions'),
            href: '/admin/payment-instructions',
            icon: CreditCard,
          },
        ],
      },
      { name: t('sidebar.payments'), href: '/payments', icon: DollarSign },
      { name: t('sidebar.expenses'), href: '/expenses', icon: Receipt },
      { name: t('sidebar.documents'), href: '/documents', icon: Folder },
      { name: t('sidebar.photos'), href: '/photos', icon: Image },
      {
        name: t('sidebar.reports'),
        href: '/reports',
        icon: BarChart3,
        featureFlag: FeatureFlags.REPORTS,
      },
    ],
    [t]
  );

  const administrationNavigation = useMemo(
    () => [
      {
        name: t('admin.preferences'),
        href: '/admin/preferences',
        icon: Settings,
      },
      {
        name: t('admin.teamMembers'),
        href: '/admin/team-members',
        icon: Users,
      },
      {
        name: t('admin.calendarFeeds'),
        href: '/admin/calendar-feeds',
        icon: Calendar,
      },
      {
        name: t('admin.notifications'),
        href: '/admin/notifications',
        icon: Bell,
      },
      {
        name: t('admin.dataExport'),
        href: '/admin/data-export',
        icon: Download,
      },
      { name: t('admin.billing'), href: '/admin/billing', icon: Receipt },
      {
        name: t('admin.activityLog'),
        href: '/admin/activity-log',
        icon: ClipboardList,
      },
      {
        name: t('admin.apiDocs'),
        href: '/admin/api-docs',
        icon: FileCode,
        featureFlag: FeatureFlags.SWAGGER,
      },
    ],
    [t]
  );

  const isOnAdminPage = location.pathname.startsWith('/admin');
  const [isAdminOpen, setIsAdminOpen] = useState(isOnAdminPage);

  // Track which parent groups are expanded
  const isChildActive = (item: NavItem) =>
    item.children?.some((child) => location.pathname.startsWith(child.href)) ??
    false;

  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(() => {
    const initial = new Set<string>();
    for (const item of navigation) {
      if (item.children && isChildActive(item)) {
        initial.add(item.href);
      }
    }
    return initial;
  });

  const toggleGroup = (href: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(href)) {
        next.delete(href);
      } else {
        next.add(href);
      }
      return next;
    });
  };

  const visibleNavigation = navigation.filter(
    (item) => !item.featureFlag || isEnabled(item.featureFlag)
  );

  // Viewport detection. Three states matter:
  //   `phone`  (<md, <768)        — off-canvas drawer + floating hamburger
  //   `rail`   (md → lg, 768–1023) — persistent 64-px icon-only rail (if flag enabled)
  //   `desktop`(lg+, ≥1024)       — full expanded sidebar (user-collapsible to 20)
  const [isPhoneViewport, setIsPhoneViewport] = useState(() =>
    typeof window !== 'undefined'
      ? window.matchMedia('(max-width: 767px)').matches
      : false
  );
  const [isRailViewport, setIsRailViewport] = useState(() =>
    typeof window !== 'undefined'
      ? window.matchMedia('(min-width: 768px) and (max-width: 1023px)').matches
      : false
  );
  useEffect(() => {
    const mqPhone = window.matchMedia('(max-width: 767px)');
    const mqRail = window.matchMedia(
      '(min-width: 768px) and (max-width: 1023px)'
    );
    const onPhone = (e: MediaQueryListEvent) => setIsPhoneViewport(e.matches);
    const onRail = (e: MediaQueryListEvent) => setIsRailViewport(e.matches);
    mqPhone.addEventListener('change', onPhone);
    mqRail.addEventListener('change', onRail);
    return () => {
      mqPhone.removeEventListener('change', onPhone);
      mqRail.removeEventListener('change', onRail);
    };
  }, []);

  // Feature flag: localStorage 'buurman.mobile.sidebarRail' ∈ { 'true','false' }.
  // Defaults to enabled. Read once at mount.
  const [railEnabled] = useState(() => {
    try {
      const v = localStorage.getItem('buurman.mobile.sidebarRail');
      return v !== 'false';
    } catch {
      return true;
    }
  });
  const railModeActive = isRailViewport && railEnabled;

  // Effective collapsed: rail mode forces collapsed visuals regardless of the
  // user's `collapsed` preference (which only applies at lg+).
  // Shadowing the prop as `collapsed` makes the rest of the JSX
  // automatically render in rail-collapsed style at md→lg.
  const collapsed = railModeActive || userCollapsed;

  // Drawer inert: only when phone-viewport AND closed. Rail and desktop always
  // remain in tab order.
  const shouldBeInert = isPhoneViewport && !mobileOpen;

  // Whether a page-level header (ListPageHeader) is providing its own menu trigger.
  const { hasOwnMenuButton } = useMobileNav();

  // Close drawer on Escape (mobile only)
  useEffect(() => {
    if (!mobileOpen) {
      return;
    }
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setMobileOpen(false);
      }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [mobileOpen, setMobileOpen]);

  return (
    <>
      {/* Floating mobile menu button — phone only (`<md`). At md→lg the rail
          provides persistent nav; at lg+ the sidebar is always visible.
          Also hidden when a page-level header provides its own trigger
          (ListPageHeader + MobileMenuButton). Always min-h/w 44px touch target. */}
      {!hasOwnMenuButton && (
        <button
          type="button"
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label={
            mobileOpen
              ? t('accessibility.closeMenu', 'Close menu')
              : t('accessibility.openMenu', 'Open menu')
          }
          aria-expanded={mobileOpen}
          aria-controls="primary-sidebar"
          className={`${railEnabled ? 'md:hidden' : 'lg:hidden'} fixed left-4 z-50 inline-flex items-center justify-center min-h-11 min-w-11 p-2 rounded-lg bg-surface-card/95 shadow-md backdrop-blur-sm hover:bg-surface-inset focus-ring`}
          style={{ top: 'calc(var(--env-banner-height, 0px) + 1rem)' }}
        >
          {mobileOpen ? (
            <X className="h-6 w-6" />
          ) : (
            <Menu className="h-6 w-6" />
          )}
        </button>
      )}

      {/* Backdrop scrim — closes drawer on tap, only visible on mobile when open */}
      {mobileOpen && (
        <div
          className="lg:hidden fixed inset-0 z-30 bg-black/40"
          onClick={() => setMobileOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar — `inert` when closed on mobile removes children from tab order */}
      <aside
        id="primary-sidebar"
        aria-label={t('accessibility.primaryNav', 'Primary navigation')}
        {...(shouldBeInert ? { inert: '' as unknown as boolean } : {})}
        aria-hidden={shouldBeInert || undefined}
        className={`
          fixed left-0 bg-surface-card/95 backdrop-blur-xl border-r border-border-default z-40
          transition-all duration-300 ease-in-out
          ${mobileOpen ? 'w-64 translate-x-0' : '-translate-x-full'}
          ${railEnabled ? 'md:translate-x-0 md:w-16' : ''}
          ${collapsed ? 'lg:w-20' : 'lg:w-64'}
          lg:translate-x-0
        `}
        style={{
          top: 'var(--env-banner-height, 0px)',
          height: 'calc(100dvh - var(--env-banner-height, 0px))',
          paddingBottom: 'env(safe-area-inset-bottom, 0px)',
          paddingLeft: 'env(safe-area-inset-left, 0px)',
        }}
      >
        <div className="flex flex-col h-full overflow-hidden">
          {/* Logo */}
          <div
            className="flex items-center h-16 border-b border-border-default px-3"
            style={{ justifyContent: 'space-between' }}
          >
            <div style={{ minWidth: 0 }}>
              {/* Mobile: always show full logo */}
              <div className="lg:hidden">
                <img
                  src="/assets/logo/logo_horizontal.png"
                  alt="Buurman"
                  className="h-10 w-auto dark:hidden"
                />
                <img
                  src="/assets/logo/logo_horizontal_dark.png"
                  alt="Buurman"
                  className="h-10 w-auto hidden dark:block"
                />
              </div>
              {/* Desktop: depends on collapsed */}
              <div className="hidden lg:block">
                {collapsed ? (
                  <>
                    <img
                      src="/assets/logo/logo_square.png"
                      alt="Buurman"
                      className="h-10 w-10 rounded-lg dark:hidden"
                    />
                    <img
                      src="/assets/logo/logo_square_dark.png"
                      alt="Buurman"
                      className="h-10 w-10 rounded-lg hidden dark:block"
                    />
                  </>
                ) : (
                  <>
                    <img
                      src="/assets/logo/logo_horizontal.png"
                      alt="Buurman"
                      className="h-10 w-auto dark:hidden"
                    />
                    <img
                      src="/assets/logo/logo_horizontal_dark.png"
                      alt="Buurman"
                      className="h-10 w-auto hidden dark:block"
                    />
                  </>
                )}
              </div>
            </div>
            <button
              onClick={onToggleCollapse}
              className="hidden lg:flex flex-shrink-0 p-1 rounded-md text-text-muted hover:text-text-secondary hover:bg-surface-inset transition-colors"
              title={
                collapsed
                  ? t('common:sidebar.expand')
                  : t('common:sidebar.collapse')
              }
            >
              {collapsed ? (
                <ChevronsRight className="h-4 w-4" />
              ) : (
                <ChevronsLeft className="h-4 w-4" />
              )}
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto py-4 px-2">
            <ul className="space-y-1">
              {visibleNavigation.map((item) => (
                <li key={item.name}>
                  {item.children ? (
                    <NavGroup
                      item={item}
                      collapsed={collapsed}
                      isExpanded={expandedGroups.has(item.href)}
                      onToggle={() => toggleGroup(item.href)}
                      onMobileClose={() => setMobileOpen(false)}
                    />
                  ) : (
                    <SidebarTooltip label={item.name} show={collapsed}>
                      <NavLink
                        to={item.href}
                        className={({ isActive }) =>
                          navLinkClass(isActive, collapsed)
                        }
                        onClick={() => setMobileOpen(false)}
                      >
                        <item.icon className="h-5 w-5 flex-shrink-0" />
                        <span
                          className={`truncate ${collapsed ? 'lg:hidden' : ''}`}
                        >
                          {item.name}
                        </span>
                      </NavLink>
                    </SidebarTooltip>
                  )}
                </li>
              ))}
            </ul>

            {/* Administration Group */}
            {canEditTeamSettings && (
              <div className="mt-4">
                {/* Group Header - only shown when sidebar is expanded (desktop) or open (mobile) */}
                <div className={collapsed ? 'lg:hidden' : ''}>
                  <button
                    onClick={() => setIsAdminOpen(!isAdminOpen)}
                    className="w-full flex items-center justify-between px-3 py-2 mb-1 rounded-lg text-xs font-semibold uppercase tracking-wider text-text-muted hover:text-text-secondary hover:bg-surface-inset transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      {t('admin.title')}
                      <Shield className="h-3.5 w-3.5 text-accent-600" />
                    </span>
                    <ChevronDown
                      className={`h-4 w-4 transition-transform duration-200 ${isAdminOpen ? 'rotate-180' : ''}`}
                    />
                  </button>
                </div>

                {/* Divider when sidebar is collapsed (desktop only) */}
                <div
                  className={`mx-3 my-2 border-t border-border-default hidden ${collapsed ? 'lg:block' : ''}`}
                />

                {/* Admin Items */}
                {(isAdminOpen || collapsed) && (
                  <ul className="space-y-1">
                    {administrationNavigation
                      .filter(
                        (item) =>
                          !item.featureFlag || isEnabled(item.featureFlag)
                      )
                      .map((item) => (
                        <li key={item.name}>
                          <SidebarTooltip label={item.name} show={collapsed}>
                            <NavLink
                              to={item.href}
                              className={({ isActive }) =>
                                navLinkClass(isActive, collapsed)
                              }
                              onClick={() => setMobileOpen(false)}
                            >
                              <item.icon className="h-5 w-5 flex-shrink-0" />
                              <span
                                className={`truncate ${collapsed ? 'lg:hidden' : ''}`}
                              >
                                {item.name}
                              </span>
                            </NavLink>
                          </SidebarTooltip>
                        </li>
                      ))}
                  </ul>
                )}
              </div>
            )}
          </nav>

          {/* Footer Actions */}
          <div className="border-t border-border-default p-2 space-y-1">
            {/* Team Switcher - only show when user has multiple teams and sidebar is expanded */}
            {!collapsed && teams.length > 1 && (
              <div className="px-1 py-2 mb-1">
                <TeamSwitcher />
              </div>
            )}
            <SidebarTooltip label={t('settings')} show={collapsed}>
              <NavLink
                to="/settings"
                className={({ isActive }) => navLinkClass(isActive, collapsed)}
                onClick={() => setMobileOpen(false)}
              >
                <Settings className="h-5 w-5 flex-shrink-0" />
                <span className={`truncate ${collapsed ? 'lg:hidden' : ''}`}>
                  {t('settings')}
                </span>
              </NavLink>
            </SidebarTooltip>
            <SidebarTooltip label={t('signOut')} show={collapsed}>
              <button
                onClick={logout}
                className={`
                  w-full flex items-center gap-3 px-3 py-2.5 rounded-lg
                  text-error-text hover:bg-error-bg
                  transition-all duration-200
                  ${collapsed ? 'lg:justify-center' : ''}
                `}
              >
                <LogOut className="h-5 w-5 flex-shrink-0" />
                <span className={`truncate ${collapsed ? 'lg:hidden' : ''}`}>
                  {t('signOut')}
                </span>
              </button>
            </SidebarTooltip>
          </div>
        </div>
      </aside>

      {/* Overlay for mobile */}
      {mobileOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black/40 backdrop-blur-sm z-30"
          onClick={() => setMobileOpen(false)}
        />
      )}
    </>
  );
};

Sidebar.displayName = 'Sidebar';

// ── Collapsible nav group (parent + children) ───────────────────────

function NavGroup({
  item,
  collapsed,
  isExpanded,
  onToggle,
  onMobileClose,
}: {
  item: NavItem;
  collapsed: boolean;
  isExpanded: boolean;
  onToggle: () => void;
  onMobileClose: () => void;
}) {
  const location = useLocation();
  const isParentActive = location.pathname === item.href;
  const isAnyChildActive =
    item.children?.some((child) => location.pathname.startsWith(child.href)) ??
    false;
  const isGroupActive = isParentActive || isAnyChildActive;

  // When collapsed, just show the parent link with tooltip
  if (collapsed) {
    return (
      <SidebarTooltip label={item.name} show>
        <NavLink
          to={item.href}
          className={({ isActive }) => navLinkClass(isActive, collapsed)}
          onClick={onMobileClose}
        >
          <item.icon className="h-5 w-5 flex-shrink-0" />
        </NavLink>
      </SidebarTooltip>
    );
  }

  return (
    <div>
      {/* Parent row: link + chevron toggle */}
      <div className="flex items-center">
        <NavLink
          to={item.href}
          className={() =>
            `flex-1 flex items-center gap-3 px-3 py-2.5 rounded-l-lg transition-all duration-200 ${
              isParentActive
                ? 'bg-primary-50 dark:bg-primary-500/15 text-primary-700 dark:text-primary-300 font-semibold'
                : isGroupActive
                  ? 'text-primary-700 dark:text-primary-300 font-medium'
                  : 'text-text-secondary hover:bg-surface-inset hover:text-text-primary'
            }`
          }
          onClick={onMobileClose}
        >
          <item.icon className="h-5 w-5 flex-shrink-0" />
          <span className="truncate">{item.name}</span>
        </NavLink>
        <button
          onClick={onToggle}
          className={`p-2 rounded-r-lg transition-colors ${
            isGroupActive
              ? 'text-primary-700 dark:text-primary-300'
              : 'text-text-muted hover:text-text-secondary'
          } hover:bg-surface-inset`}
        >
          <ChevronDown
            className={`h-3.5 w-3.5 transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}
          />
        </button>
      </div>

      {/* Children */}
      {isExpanded && item.children && (
        <ul className="mt-0.5 space-y-0.5 pl-4">
          {item.children.map((child) => (
            <li key={child.name}>
              <NavLink
                to={child.href}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-primary-50 dark:bg-primary-500/15 text-primary-700 dark:text-primary-300 font-semibold'
                      : 'text-text-secondary hover:bg-surface-inset hover:text-text-secondary'
                  }`
                }
                onClick={onMobileClose}
              >
                <child.icon className="h-4 w-4 flex-shrink-0" />
                <span className="truncate text-sm">{child.name}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
