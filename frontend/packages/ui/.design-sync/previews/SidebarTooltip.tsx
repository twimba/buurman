import { SidebarTooltip } from '@buurman/ui';
import { Home, Building2, Users, Receipt } from 'lucide-react';

function SidebarIcon({ children }: { children: React.ReactNode }) {
  return (
    <div className="h-11 w-11 flex items-center justify-center rounded-lg text-text-secondary hover:bg-surface-inset hover:text-text-primary transition-colors">
      {children}
    </div>
  );
}

export function CollapsedRail() {
  return (
    <div className="inline-flex flex-col gap-1 p-2 bg-surface-card border border-border-default rounded-xl">
      <SidebarTooltip label="Dashboard" show>
        <SidebarIcon>
          <Home className="h-5 w-5" />
        </SidebarIcon>
      </SidebarTooltip>
      <SidebarTooltip label="Properties" show>
        <SidebarIcon>
          <Building2 className="h-5 w-5" />
        </SidebarIcon>
      </SidebarTooltip>
      <SidebarTooltip label="Tenants" show>
        <SidebarIcon>
          <Users className="h-5 w-5" />
        </SidebarIcon>
      </SidebarTooltip>
      <SidebarTooltip label="Payments" show>
        <SidebarIcon>
          <Receipt className="h-5 w-5" />
        </SidebarIcon>
      </SidebarTooltip>
    </div>
  );
}

export function TooltipAppearance() {
  return (
    <div className="flex items-center gap-2 p-6 bg-surface-card border border-border-default rounded-xl">
      <SidebarTooltip label="Properties" show>
        <SidebarIcon>
          <Building2 className="h-5 w-5" />
        </SidebarIcon>
      </SidebarTooltip>
      {/* Static render of the tooltip bubble (the real component reveals this on hover only). */}
      <span className="px-2.5 py-1.5 text-xs font-medium text-text-inverse bg-neutral-700 rounded-md whitespace-nowrap shadow-lg">
        Properties
      </span>
    </div>
  );
}

export function Disabled() {
  return (
    <div className="inline-flex p-2 bg-surface-card border border-border-default rounded-xl">
      <SidebarTooltip label="Hidden when expanded" show={false}>
        <SidebarIcon>
          <Home className="h-5 w-5" />
        </SidebarIcon>
      </SidebarTooltip>
    </div>
  );
}
