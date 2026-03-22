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
import { useState } from 'react';
import { SidebarTooltip } from '@buurman/ui';
import { useAuth } from '@/contexts/AuthContext';
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

const navigation: NavItem[] = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Properties', href: '/properties', icon: Home },
  { name: 'Contacts', href: '/contacts', icon: Users },
  {
    name: 'Contracts',
    href: '/contracts',
    icon: FileText,
    children: [
      { name: 'Rent Regulations', href: '/rent-regulations', icon: Scale },
      {
        name: 'Rent Adjustments',
        href: '/rent-increases/apply',
        icon: TrendingUp,
      },
      {
        name: 'Payment Instructions',
        href: '/payment-instructions',
        icon: CreditCard,
      },
    ],
  },
  { name: 'Payments', href: '/payments', icon: DollarSign },
  { name: 'Expenses', href: '/expenses', icon: Receipt },
  { name: 'Documents', href: '/documents', icon: Folder },
  { name: 'Photos', href: '/photos', icon: Image },
  {
    name: 'Reports',
    href: '/reports',
    icon: BarChart3,
    featureFlag: FeatureFlags.REPORTS,
  },
];

const administrationNavigation = [
  { name: 'Preferences', href: '/admin/preferences', icon: Settings },
  { name: 'Team Members', href: '/admin/team-members', icon: Users },
  { name: 'Calendar Feeds', href: '/admin/calendar-feeds', icon: Calendar },
  { name: 'Notifications', href: '/admin/notifications', icon: Bell },
  { name: 'Data Export', href: '/admin/data-export', icon: Download },
  { name: 'Subscription & Billing', href: '/admin/billing', icon: Receipt },
  { name: 'Activity Log', href: '/admin/activity-log', icon: ClipboardList },
  {
    name: 'API Documentation',
    href: '/admin/api-docs',
    icon: FileCode,
    featureFlag: FeatureFlags.SWAGGER,
  },
];

const navLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-3 px-3 py-2.5 rounded-lg
  transition-all duration-200
  ${collapsed ? 'lg:justify-center' : ''}
  ${
    isActive
      ? 'bg-primary-50 text-primary-700 font-semibold'
      : 'text-text-secondary hover:bg-neutral-50 hover:text-text-primary'
  }
`;

interface SidebarProps {
  collapsed: boolean;
  onToggleCollapse: () => void;
}

export const Sidebar = ({ collapsed, onToggleCollapse }: SidebarProps) => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { logout } = useAuth();
  const { teams, canEditTeamSettings } = useTeam();
  const { isEnabled } = useFeatureFlags();
  const location = useLocation();

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
        initial.add(item.name);
      }
    }
    return initial;
  });

  const toggleGroup = (name: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(name)) {
        next.delete(name);
      } else {
        next.add(name);
      }
      return next;
    });
  };

  const visibleNavigation = navigation.filter(
    (item) => !item.featureFlag || isEnabled(item.featureFlag)
  );

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => setMobileOpen(!mobileOpen)}
        className="lg:hidden fixed left-4 z-50 p-2 rounded-lg bg-surface-card/95 shadow-md backdrop-blur-sm hover:bg-surface-inset"
        style={{ top: 'calc(var(--env-banner-height, 0px) + 1rem)' }}
      >
        {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </button>

      {/* Sidebar */}
      <aside
        className={`
          fixed left-0 bg-surface-card/95 backdrop-blur-xl border-r border-border-default z-40
          transition-all duration-300 ease-in-out
          ${mobileOpen ? 'w-64 translate-x-0' : '-translate-x-full'}
          ${collapsed ? 'lg:w-20' : 'lg:w-64'}
          lg:translate-x-0
        `}
        style={{
          top: 'var(--env-banner-height, 0px)',
          height: 'calc(100vh - var(--env-banner-height, 0px))',
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
                  className="h-10 w-auto"
                />
              </div>
              {/* Desktop: depends on collapsed */}
              <div className="hidden lg:block">
                {collapsed ? (
                  <img
                    src="/assets/logo/logo_square.png"
                    alt="Buurman"
                    className="h-10 w-10 rounded-lg"
                  />
                ) : (
                  <img
                    src="/assets/logo/logo_horizontal.png"
                    alt="Buurman"
                    className="h-10 w-auto"
                  />
                )}
              </div>
            </div>
            <button
              onClick={onToggleCollapse}
              className="hidden lg:flex flex-shrink-0 p-1 rounded-md text-text-muted hover:text-text-secondary hover:bg-surface-inset transition-colors"
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
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
                      isExpanded={expandedGroups.has(item.name)}
                      onToggle={() => toggleGroup(item.name)}
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
                      Administration
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
            <SidebarTooltip label="Settings" show={collapsed}>
              <NavLink
                to="/settings"
                className={({ isActive }) => navLinkClass(isActive, collapsed)}
                onClick={() => setMobileOpen(false)}
              >
                <Settings className="h-5 w-5 flex-shrink-0" />
                <span className={`truncate ${collapsed ? 'lg:hidden' : ''}`}>
                  Settings
                </span>
              </NavLink>
            </SidebarTooltip>
            <SidebarTooltip label="Logout" show={collapsed}>
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
                  Logout
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
                ? 'bg-primary-50 text-primary-700 font-semibold'
                : isGroupActive
                  ? 'text-primary-700 font-medium'
                  : 'text-text-secondary hover:bg-neutral-50 hover:text-text-primary'
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
              ? 'text-primary-700'
              : 'text-text-muted hover:text-text-secondary'
          } hover:bg-neutral-50`}
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
                      ? 'bg-primary-50 text-primary-700 font-semibold'
                      : 'text-text-secondary hover:bg-neutral-50 hover:text-text-secondary'
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
