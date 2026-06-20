import { EmptyState, Button } from '@buurman/ui';
import { Users, FileSpreadsheet, Plus } from 'lucide-react';

export function Section() {
  return (
    <EmptyState
      variant="section"
      icon={<Users className="h-10 w-10" />}
      title="No tenants yet"
      description="Add your first tenant to start tracking leases and payments."
      actions={<Button leftIcon={<Plus />}>Add tenant</Button>}
    />
  );
}

export function Page() {
  return (
    <EmptyState
      variant="page"
      icon={<FileSpreadsheet className="h-12 w-12" />}
      title="No import history"
      description="Imports you run will appear here with their status and results."
    />
  );
}

export function Inline() {
  return (
    <EmptyState
      variant="inline"
      title="No results"
      description="Try adjusting your filters."
    />
  );
}
