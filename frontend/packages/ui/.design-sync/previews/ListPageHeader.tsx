import { ListPageHeader } from '@buurman/ui';
import { Building2, Plus, Download, Filter, Users } from 'lucide-react';

export function WithPrimaryAction() {
  return (
    <ListPageHeader
      title="Properties"
      subtitle="42 properties across 6 cities"
      icon={Building2}
      primaryAction={{ label: 'Add property', icon: Plus }}
    />
  );
}

export function WithSecondaryActions() {
  return (
    <ListPageHeader
      title="Tenants"
      subtitle="128 active leases"
      icon={Users}
      actions={[
        { label: 'Filter', icon: Filter },
        { label: 'Export', icon: Download },
      ]}
      primaryAction={{ label: 'New tenant', icon: Plus }}
    />
  );
}

export function TitleOnly() {
  return <ListPageHeader title="Maintenance requests" icon={Building2} />;
}
