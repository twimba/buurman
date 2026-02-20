import { useState } from 'react';
import { Sidebar } from './Sidebar';

const STORAGE_KEY = 'buurman-sidebar-collapsed';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });

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
      className="flex overflow-hidden bg-[#f8f9fc] dark:bg-[#0c0d14]"
      style={{
        height: 'calc(100vh - var(--env-banner-height, 0px))',
        marginTop: 'var(--env-banner-height, 0px)',
      }}
    >
      <Sidebar collapsed={collapsed} onToggleCollapse={toggleCollapsed} />
      <main
        className={`flex-1 overflow-auto transition-all duration-300 ease-in-out ${collapsed ? 'lg:ml-20' : 'lg:ml-64'}`}
      >
        <div className="p-4 lg:p-8">{children}</div>
      </main>
    </div>
  );
};
