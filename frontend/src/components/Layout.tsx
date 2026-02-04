import { Sidebar } from './Sidebar';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout = ({ children }: LayoutProps) => {
  return (
    <div className="flex h-screen overflow-hidden bg-[#f8f9fc] dark:bg-[#0c0d14]">
      <Sidebar />
      <main className="flex-1 overflow-auto lg:ml-64">
        <div className="p-4 lg:p-8">{children}</div>
      </main>
    </div>
  );
};
