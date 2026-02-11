import type { ReactNode } from 'react';

interface SidebarTooltipProps {
  label: string;
  show: boolean;
  children: ReactNode;
}

export const SidebarTooltip = ({ label, show, children }: SidebarTooltipProps) => {
  if (!show) return <>{children}</>;

  return (
    <div style={{ position: 'relative' }} className="group">
      {children}
      <div
        style={{
          position: 'absolute',
          left: '100%',
          top: '50%',
          transform: 'translateY(-50%)',
          marginLeft: '0.5rem',
          pointerEvents: 'none',
          zIndex: 50,
        }}
        className="px-2.5 py-1.5 text-xs font-medium text-white bg-[#1a1d2e] dark:bg-[#eef0f6] dark:text-[#1a1d2e] rounded-md whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-150 shadow-lg"
      >
        {label}
      </div>
    </div>
  );
};
