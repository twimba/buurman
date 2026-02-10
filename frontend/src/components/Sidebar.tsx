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
} from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useTeam } from '@/context/TeamContext';
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
  { name: 'Reports', href: '/reports', icon: BarChart3 },
];

const administrationNavigation = [
  { name: 'Team Members', href: '/admin/team-members', icon: Users },
  { name: 'Preferences', href: '/admin/preferences', icon: Settings },
  { name: 'Calendar Feeds', href: '/admin/calendar-feeds', icon: Calendar },
  { name: 'Notifications', href: '/admin/notifications', icon: Bell },
  { name: 'Subscription & Billing', href: '/admin/billing', icon: Receipt },
  { name: 'Activity Log', href: '/admin/activity-log', icon: ClipboardList },
];

const navLinkClass = (isActive: boolean, isOpen: boolean) => `
  flex items-center gap-3 px-3 py-2.5 rounded-lg
  transition-all duration-200
  ${
    isActive
      ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 text-[#5c7cfa] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]'
      : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]'
  }
  ${!isOpen && 'lg:justify-center'}
`;

export const Sidebar = () => {
  const [isOpen, setIsOpen] = useState(true);
  const { logout } = useAuth();
  const { teams, canEditTeamSettings } = useTeam();
  const location = useLocation();

  const isOnAdminPage = location.pathname.startsWith('/admin');
  const [isAdminOpen, setIsAdminOpen] = useState(isOnAdminPage);

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-white/95 dark:bg-[#14161f]/95 shadow-md backdrop-blur-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
      >
        {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </button>

      {/* Sidebar */}
      <aside
        className={`
          fixed top-0 left-0 h-full bg-white/95 dark:bg-[#0c0d14]/95 backdrop-blur-xl border-r border-[#e2e6f0] dark:border-[#2a2e3f] z-40
          transition-all duration-300 ease-in-out
          ${isOpen ? 'w-64' : 'w-0 lg:w-20'}
          ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center justify-center h-16 border-b border-[#e2e6f0] dark:border-[#2a2e3f] px-4">
            {isOpen ? (
              <img
                src="/assets/logo/logo_horizontal.png"
                alt="Buurman"
                className="h-10 w-auto"
              />
            ) : (
              <img
                src="/assets/logo/logo_square.png"
                alt="Buurman"
                className="h-10 w-10 rounded-lg"
              />
            )}
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto py-4 px-2">
            <ul className="space-y-1">
              {navigation.map((item) => (
                <li key={item.name} className={'indent' in item && item.indent && isOpen ? 'pl-4' : ''}>
                  <NavLink
                    to={item.href}
                    className={({ isActive }) => navLinkClass(isActive, isOpen)}
                    title={!isOpen ? item.name : undefined}
                  >
                    <item.icon className={`flex-shrink-0 ${'indent' in item && item.indent ? 'h-4 w-4' : 'h-5 w-5'}`} />
                    {isOpen && <span className="truncate">{item.name}</span>}
                  </NavLink>
                </li>
              ))}
            </ul>

            {/* Administration Group */}
            {canEditTeamSettings && (
              <div className="mt-4">
                {/* Group Header - only shown when sidebar is expanded */}
                {isOpen && (
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
                )}

                {/* Divider when sidebar is collapsed */}
                {!isOpen && (
                  <div className="hidden lg:block mx-3 my-2 border-t border-[#e2e6f0] dark:border-[#2a2e3f]" />
                )}

                {/* Admin Items */}
                {(isAdminOpen || !isOpen) && (
                  <ul className="space-y-1">
                    {administrationNavigation.map((item) => (
                      <li key={item.name}>
                        <NavLink
                          to={item.href}
                          className={({ isActive }) =>
                            navLinkClass(isActive, isOpen)
                          }
                          title={!isOpen ? item.name : undefined}
                        >
                          <item.icon className="h-5 w-5 flex-shrink-0" />
                          {isOpen && (
                            <span className="truncate">{item.name}</span>
                          )}
                        </NavLink>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </nav>

          {/* Footer Actions */}
          <div className="border-t border-[#e2e6f0] dark:border-[#2a2e3f] p-2 space-y-1">
            {/* Team Switcher - only show when user has multiple teams */}
            {isOpen && teams.length > 1 && (
              <div className="px-1 py-2 mb-1">
                <TeamSwitcher />
              </div>
            )}
            <NavLink
              to="/settings"
              className={({ isActive }) => navLinkClass(isActive, isOpen)}
              title={!isOpen ? 'Settings' : undefined}
            >
              <Settings className="h-5 w-5 flex-shrink-0" />
              {isOpen && <span className="truncate">Settings</span>}
            </NavLink>
            <button
              onClick={logout}
              className={`
                w-full flex items-center gap-3 px-3 py-2.5 rounded-lg
                text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20
                transition-all duration-200
                ${!isOpen && 'lg:justify-center'}
              `}
              title={!isOpen ? 'Logout' : undefined}
            >
              <LogOut className="h-5 w-5 flex-shrink-0" />
              {isOpen && <span className="truncate">Logout</span>}
            </button>
          </div>
        </div>
      </aside>

      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black/40 backdrop-blur-sm z-30"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  );
};
