import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Home,
  Users,
  FileText,
  DollarSign,
  Receipt,
  BarChart3,
  ClipboardList,
  Settings,
  LogOut,
  Menu,
  X,
} from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Properties', href: '/properties', icon: Home },
  { name: 'Tenants', href: '/tenants', icon: Users },
  { name: 'Contracts', href: '/contracts', icon: FileText },
  { name: 'Payments', href: '/payments', icon: DollarSign },
  { name: 'Expenses', href: '/expenses', icon: Receipt },
  { name: 'Reports', href: '/reports', icon: BarChart3 },
  { name: 'Audit Log', href: '/audit-log', icon: ClipboardList },
];

export const Sidebar = () => {
  const [isOpen, setIsOpen] = useState(true);
  const { logout } = useAuth();

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-white shadow-md hover:bg-gray-100"
      >
        {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </button>

      {/* Sidebar */}
      <aside
        className={`
          fixed top-0 left-0 h-full bg-white border-r border-gray-200 z-40
          transition-all duration-300 ease-in-out
          ${isOpen ? 'w-64' : 'w-0 lg:w-20'}
          ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center justify-center h-16 border-b border-gray-200 px-4">
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
                        transition-colors duration-200
                        ${
                          isActive
                            ? 'bg-blue-50 text-blue-600 font-medium'
                            : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
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
            </ul>
          </nav>

          {/* Footer Actions */}
          <div className="border-t border-gray-200 p-2 space-y-1">
            <NavLink
              to="/settings"
              className={({ isActive }) =>
                `
                  flex items-center gap-3 px-3 py-2.5 rounded-lg
                  transition-colors duration-200
                  ${
                    isActive
                      ? 'bg-blue-50 text-blue-600 font-medium'
                      : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
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
                text-red-600 hover:bg-red-50
                transition-colors duration-200
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
          className="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-30"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  );
};
