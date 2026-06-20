import { DataList, StatusBadge } from '@buurman/ui';
import { Home } from 'lucide-react';

export function PaymentCard() {
  return (
    <div className="max-w-sm rounded-lg border border-border-default bg-surface-card p-4">
      <DataList
        title="Sanne de Vries"
        trailing={<StatusBadge label="Paid" color="green" dot />}
        items={[
          { label: 'Period', value: 'June 2026' },
          { label: 'Amount', value: '€ 1.450,00', align: 'right' },
          { label: 'Due date', value: '1 Jun 2026', align: 'right' },
          { label: 'Method', value: 'SEPA direct debit', align: 'right' },
        ]}
      />
    </div>
  );
}

export function PropertyCard() {
  return (
    <div className="max-w-sm rounded-lg border border-border-default bg-surface-card p-4">
      <DataList
        leading={
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-100 text-primary-600">
            <Home className="h-5 w-5" />
          </div>
        }
        title="Keizersgracht 124"
        trailing={<StatusBadge label="Occupied" color="green" dot />}
        items={[
          { label: 'City', value: 'Amsterdam', align: 'right' },
          { label: 'Type', value: 'Apartment', align: 'right' },
          { label: 'Monthly rent', value: '€ 1.850,00', align: 'right' },
          { label: 'Lease ends', value: '31 Dec 2026', align: 'right' },
        ]}
      />
    </div>
  );
}

export function ContractSummary() {
  return (
    <div className="max-w-sm rounded-lg border border-border-default bg-surface-card p-4">
      <DataList
        title="Lease — Witte de Withstraat 7B"
        trailing={<StatusBadge label="Active" color="blue" />}
        items={[
          { label: 'Tenant', value: 'Mark Jansen', align: 'right' },
          { label: 'Start date', value: '1 Mar 2024', align: 'right' },
          { label: 'Term', value: '24 months', align: 'right' },
          { label: 'Deposit', value: '€ 2.550,00', align: 'right' },
        ]}
      />
    </div>
  );
}
