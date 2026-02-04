import { NavLink } from 'react-router-dom';
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
  ClipboardList,
  Settings,
  LogOut,
  Menu,
  X,
  Shield,
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
  { name: 'Payments', href: '/payments', icon: DollarSign },
  { name: 'Expenses', href: '/expenses', icon: Receipt },
  { name: 'Documents', href: '/documents', icon: Folder },
  { name: 'Photos', href: '/photos', icon: Image },
  { name: 'Reports', href: '/reports', icon: BarChart3 },
];

const adminNavigation = [
  { name: 'Activity Log', href: '/audit-log', icon: ClipboardList },
];

export const Sidebar = () => {
  const [isOpen, setIsOpen] = useState(true);
  const { logout } = useAuth();
  const { canEditTeamSettings } = useTeam();

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
                <li key={item.name}>
                  <NavLink
                    to={item.href}
                    className={({ isActive }) =>
                      `
                        flex items-center gap-3 px-3 py-2.5 rounded-lg
                        transition-all duration-200
                        ${
                          isActive
                            ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 text-[#5c7cfa] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]'
                            : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]'
                        }
                        ${!isOpen && 'lg:justify-center'}
                      `
                    }
                    title={!isOpen ? item.name : undefined}
                  >
                    <item.icon className="h-5 w-5 flex-shrink-0" />
                    {isOpen && <span className="truncate">{item.name}</span>}
                  </NavLink>
                </li>
              ))}
              {canEditTeamSettings &&
                adminNavigation.map((item) => (
                  <li key={item.name}>
                    <NavLink
                      to={item.href}
                      className={({ isActive }) =>
                        `
                        flex items-center gap-3 px-3 py-2.5 rounded-lg
                        transition-all duration-200
                        ${
                          isActive
                            ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 text-[#5c7cfa] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]'
                            : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]'
                        }
                        ${!isOpen && 'lg:justify-center'}
                      `
                      }
                      title={!isOpen ? item.name : undefined}
                    >
                      <item.icon className="h-5 w-5 flex-shrink-0" />
                      {isOpen && (
                        <span className="truncate flex items-center gap-2">
                          {item.name}
                          <Shield className="h-3.5 w-3.5 text-accent-600" />
                        </span>
                      )}
                    </NavLink>
                  </li>
                ))}
            </ul>
          </nav>

          {/* Footer Actions */}
          <div className="border-t border-[#e2e6f0] dark:border-[#2a2e3f] p-2 space-y-1">
            {/* Team Switcher */}
            {isOpen && (
              <div className="px-1 py-2 mb-1">
                <TeamSwitcher />
              </div>
            )}
            <NavLink
              to="/settings"
              className={({ isActive }) =>
                `
                  flex items-center gap-3 px-3 py-2.5 rounded-lg
                  transition-all duration-200
                  ${
                    isActive
                      ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 text-[#5c7cfa] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]'
                      : 'text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]'
                  }
                  ${!isOpen && 'lg:justify-center'}
                `
              }
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
