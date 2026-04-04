import type { ReactNode } from 'react';

interface SidebarTooltipProps {
  label: string;
  show: boolean;
  children: ReactNode;
}

export const SidebarTooltip = ({
  label,
  show,
  children,
}: SidebarTooltipProps) => {
  if (!show) {
    return <>{children}</>;
  }

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
        className="px-2.5 py-1.5 text-xs font-medium text-text-inverse bg-neutral-700 rounded-md whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-150 shadow-lg"
      >
        {label}
      </div>
    </div>
  );
};
