import { DataTable, StatusBadge } from '@buurman/ui';
import type { ColumnDef } from '@buurman/ui';

interface PaymentRow {
  id: string;
  period: string;
  tenant: string;
  amount: string;
  status: 'paid' | 'pending' | 'overdue';
}

const payments: PaymentRow[] = [
  { id: '1', period: 'Jun 2026', tenant: 'Sanne de Vries', amount: '€ 1.450,00', status: 'paid' },
  { id: '2', period: 'Jun 2026', tenant: 'Mark Jansen', amount: '€ 1.275,00', status: 'pending' },
  { id: '3', period: 'Jun 2026', tenant: 'Fatima El Amrani', amount: '€ 1.620,00', status: 'overdue' },
  { id: '4', period: 'May 2026', tenant: 'Pieter Bakker', amount: '€ 1.100,00', status: 'paid' },
  { id: '5', period: 'May 2026', tenant: 'Lotte Visser', amount: '€ 1.850,00', status: 'paid' },
];

const statusColor: Record<PaymentRow['status'], 'green' | 'amber' | 'red'> = {
  paid: 'green',
  pending: 'amber',
  overdue: 'red',
};

const statusLabel: Record<PaymentRow['status'], string> = {
  paid: 'Paid',
  pending: 'Pending',
  overdue: 'Overdue',
};

const columns: ColumnDef<PaymentRow>[] = [
  { id: 'period', header: 'Period', accessor: (r) => r.period, sortable: true },
  { id: 'tenant', header: 'Tenant', accessor: (r) => r.tenant },
  { id: 'amount', header: 'Amount', accessor: (r) => r.amount, align: 'right' },
  {
    id: 'status',
    header: 'Status',
    align: 'right',
    cell: (r) => <StatusBadge label={statusLabel[r.status]} color={statusColor[r.status]} dot />,
  },
];

export function Payments() {
  return (
    <div className="max-w-2xl">
      <DataTable
        columns={columns}
        data={payments}
        rowKey={(r) => r.id}
        sort={{ columnId: 'period', direction: 'desc' }}
        onSortChange={() => {}}
        aria-label="Rent payments"
      />
    </div>
  );
}

interface PropertyRow {
  id: string;
  address: string;
  city: string;
  rent: string;
  occupancy: 'occupied' | 'vacant';
}

const properties: PropertyRow[] = [
  { id: 'p1', address: 'Keizersgracht 124', city: 'Amsterdam', rent: '€ 1.850', occupancy: 'occupied' },
  { id: 'p2', address: 'Witte de Withstraat 7B', city: 'Rotterdam', rent: '€ 1.275', occupancy: 'occupied' },
  { id: 'p3', address: 'Oudegracht 210', city: 'Utrecht', rent: '€ 1.620', occupancy: 'vacant' },
  { id: 'p4', address: 'Vughterstraat 45', city: "'s-Hertogenbosch", rent: '€ 1.100', occupancy: 'occupied' },
];

const propertyColumns: ColumnDef<PropertyRow>[] = [
  { id: 'address', header: 'Address', accessor: (r) => r.address },
  { id: 'city', header: 'City', accessor: (r) => r.city },
  { id: 'rent', header: 'Monthly rent', accessor: (r) => r.rent, align: 'right' },
  {
    id: 'occupancy',
    header: 'Status',
    align: 'right',
    cell: (r) =>
      r.occupancy === 'occupied' ? (
        <StatusBadge label="Occupied" color="green" dot />
      ) : (
        <StatusBadge label="Vacant" color="gray" dot />
      ),
  },
];

export function Properties() {
  return (
    <div className="max-w-2xl">
      <DataTable
        columns={propertyColumns}
        data={properties}
        rowKey={(r) => r.id}
        aria-label="Properties"
      />
    </div>
  );
}
