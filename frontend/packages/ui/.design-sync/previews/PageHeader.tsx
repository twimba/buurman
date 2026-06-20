// PageHeader calls useNavigate(), so it must render inside a Router whose
// context comes from the SAME bundle instance. react-router-dom is merged onto
// the global via cfg.extraEntries, so importing MemoryRouter from '@buurman/ui'
// yields the bundle's own router — its context matches PageHeader's useNavigate.
import { PageHeader, Button, StatusBadge, MemoryRouter } from '@buurman/ui';
import { Pencil, Trash2, Building2 } from 'lucide-react';

export function DetailHeader() {
  return (
    <MemoryRouter>
      <div className="bg-surface-page px-4">
        <PageHeader
          title="Prinsengracht 263"
          subtitle="#PROP-7F2A"
          description="Amsterdam · 4 units"
          backTo="/properties"
          badge={<StatusBadge label="Occupied" color="green" />}
          actions={
            <>
              <Button variant="secondary" leftIcon={<Pencil />}>
                Edit
              </Button>
              <Button variant="danger" leftIcon={<Trash2 />}>
                Delete
              </Button>
            </>
          }
        />
      </div>
    </MemoryRouter>
  );
}

export function WithAvatar() {
  return (
    <MemoryRouter>
      <div className="bg-surface-page px-4">
        <PageHeader
          title="Jan de Vries"
          subtitle="Tenant since Mar 2022"
          backTo="/tenants"
          avatar={
            <div className="h-10 w-10 rounded-full bg-primary-500 text-white flex items-center justify-center font-semibold">
              JV
            </div>
          }
          badge={<StatusBadge label="Current" color="blue" />}
          actions={<Button variant="secondary">Message</Button>}
        />
      </div>
    </MemoryRouter>
  );
}

export function Minimal() {
  return (
    <MemoryRouter>
      <div className="bg-surface-page px-4">
        <PageHeader
          title="Lease agreement"
          backTo="/contracts"
          avatar={<Building2 className="h-6 w-6 text-primary-500" />}
        />
      </div>
    </MemoryRouter>
  );
}
