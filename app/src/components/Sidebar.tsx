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
  Settings,
  LogOut,
  Menu,
  X,
  Shield,
  ChevronDown,
  ChevronsLeft,
  ChevronsRight,
} from 'lucide-react';
import { useState } from 'react';
import { SidebarTooltip } from '@buurman/ui';
import { useAuth } from '@/contexts/AuthContext';
import { useTeam } from '@/context/TeamContext';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import { TeamSwitcher } from './TeamSwitcher';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Properties', href: '/properties', icon: Home },
  { name: 'Tenants', href: '/tenants', icon: Users },
  { name: 'Contracts', href: '/contracts', icon: FileText },
  {
    name: 'Payment Instructions',
    href: '/payment-instructions',
    icon: CreditCard,
    indent: true,
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
  { name: 'Team Members', href: '/admin/team-members', icon: Users },
  { name: 'Preferences', href: '/admin/preferences', icon: Settings },
  { name: 'Calendar Feeds', href: '/admin/calendar-feeds', icon: Calendar },
  { name: 'Notifications', href: '/admin/notifications', icon: Bell },
  { name: 'Subscription & Billing', href: '/admin/billing', icon: Receipt },
  { name: 'Activity Log', href: '/admin/activity-log', icon: ClipboardList },
];

const navLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-3 px-3 py-2.5 rounded-lg
  transition-all duration-200
  ${collapsed ? 'lg:justify-center' : ''}
  ${
    isActive
      ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 text-[#5c7cfa] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]'
      : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]'
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

  const visibleNavigation = navigation.filter(
    (item) => !('featureFlag' in item) || isEnabled(item.featureFlag!)
  );

  const isOnAdminPage = location.pathname.startsWith('/admin');
  const [isAdminOpen, setIsAdminOpen] = useState(isOnAdminPage);

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => setMobileOpen(!mobileOpen)}
        className="lg:hidden fixed left-4 z-50 p-2 rounded-lg bg-white/95 dark:bg-[#14161f]/95 shadow-md backdrop-blur-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
        style={{ top: 'calc(var(--env-banner-height, 0px) + 1rem)' }}
      >
        {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </button>

      {/* Sidebar */}
      <aside
        className={`
          fixed left-0 bg-white/95 dark:bg-[#0c0d14]/95 backdrop-blur-xl border-r border-[#e2e6f0] dark:border-[#2a2e3f] z-40
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
            className="flex items-center h-16 border-b border-[#e2e6f0] dark:border-[#2a2e3f] px-3"
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
              className="hidden lg:flex flex-shrink-0 p-1 rounded-md text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:hover:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
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
                <li
                  key={item.name}
                  className={
                    'indent' in item && item.indent && !collapsed
                      ? 'lg:pl-4'
                      : ''
                  }
                >
                  <SidebarTooltip label={item.name} show={collapsed}>
                    <NavLink
                      to={item.href}
                      className={({ isActive }) =>
                        navLinkClass(isActive, collapsed)
                      }
                      onClick={() => setMobileOpen(false)}
                    >
                      <item.icon
                        className={`flex-shrink-0 ${'indent' in item && item.indent ? 'h-4 w-4' : 'h-5 w-5'}`}
                      />
                      {/* Mobile: always show text. Desktop: hide when collapsed */}
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

            {/* Administration Group */}
            {canEditTeamSettings && (
              <div className="mt-4">
                {/* Group Header - only shown when sidebar is expanded (desktop) or open (mobile) */}
                <div className={collapsed ? 'lg:hidden' : ''}>
                  <button
                    onClick={() => setIsAdminOpen(!isAdminOpen)}
                    className="w-full flex items-center justify-between px-3 py-2 mb-1 rounded-lg text-xs font-semibold uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:hover:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
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
                  className={`mx-3 my-2 border-t border-[#e2e6f0] dark:border-[#2a2e3f] hidden ${collapsed ? 'lg:block' : ''}`}
                />

                {/* Admin Items */}
                {(isAdminOpen || collapsed) && (
                  <ul className="space-y-1">
                    {administrationNavigation.map((item) => (
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
          <div className="border-t border-[#e2e6f0] dark:border-[#2a2e3f] p-2 space-y-1">
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
                  text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20
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
