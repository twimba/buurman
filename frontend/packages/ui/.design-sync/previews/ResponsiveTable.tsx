import { ResponsiveTable, StatusBadge, DataList } from '@buurman/ui';
import type { ResponsiveTableColumn } from '@buurman/ui';

interface ExpenseRow {
  id: string;
  date: string;
  property: string;
  category: string;
  amount: string;
  status: 'paid' | 'open';
}

const expenses: ExpenseRow[] = [
  { id: 'e1', date: '12 Jun 2026', property: 'Keizersgracht 124', category: 'Maintenance', amount: '€ 320,00', status: 'paid' },
  { id: 'e2', date: '08 Jun 2026', property: 'Oudegracht 210', category: 'Insurance', amount: '€ 145,50', status: 'paid' },
  { id: 'e3', date: '03 Jun 2026', property: 'Witte de Withstraat 7B', category: 'Cleaning', amount: '€ 90,00', status: 'open' },
  { id: 'e4', date: '28 May 2026', property: 'Vughterstraat 45', category: 'Repairs', amount: '€ 1.240,00', status: 'open' },
];

const columns: ResponsiveTableColumn<ExpenseRow>[] = [
  { key: 'date', header: 'Date', cell: (r) => r.date },
  { key: 'property', header: 'Property', cell: (r) => r.property, sticky: true },
  { key: 'category', header: 'Category', cell: (r) => r.category, hideBelow: 'lg' },
  { key: 'amount', header: 'Amount', cell: (r) => r.amount, align: 'right' },
  {
    key: 'status',
    header: 'Status',
    align: 'right',
    cell: (r) =>
      r.status === 'paid' ? (
        <StatusBadge label="Paid" color="green" dot />
      ) : (
        <StatusBadge label="Open" color="amber" dot />
      ),
  },
];

export function Expenses() {
  return (
    <div className="max-w-3xl">
      <ResponsiveTable
        rows={expenses}
        columns={columns}
        rowKey={(r) => r.id}
        mobileRow={(r) => (
          <DataList
            title={r.property}
            trailing={
              r.status === 'paid' ? (
                <StatusBadge label="Paid" color="green" dot />
              ) : (
                <StatusBadge label="Open" color="amber" dot />
              )
            }
            items={[
              { label: 'Date', value: r.date, align: 'right' },
              { label: 'Category', value: r.category, align: 'right' },
              { label: 'Amount', value: r.amount, align: 'right' },
            ]}
          />
        )}
      />
    </div>
  );
}

interface ContractRow {
  id: string;
  tenant: string;
  property: string;
  end: string;
  rent: string;
  status: 'active' | 'expiring' | 'ended';
}

const contracts: ContractRow[] = [
  { id: 'c1', tenant: 'Sanne de Vries', property: 'Keizersgracht 124', end: '31 Dec 2026', rent: '€ 1.850', status: 'active' },
  { id: 'c2', tenant: 'Mark Jansen', property: 'Witte de Withstraat 7B', end: '31 Aug 2026', rent: '€ 1.275', status: 'expiring' },
  { id: 'c3', tenant: 'Lotte Visser', property: 'Oudegracht 210', end: '14 Feb 2027', rent: '€ 1.620', status: 'active' },
  { id: 'c4', tenant: 'Pieter Bakker', property: 'Vughterstraat 45', end: '01 May 2026', rent: '€ 1.100', status: 'ended' },
];

const contractColumns: ResponsiveTableColumn<ContractRow>[] = [
  { key: 'tenant', header: 'Tenant', cell: (r) => r.tenant, sticky: true },
  { key: 'property', header: 'Property', cell: (r) => r.property },
  { key: 'end', header: 'End date', cell: (r) => r.end, hideBelow: 'lg' },
  { key: 'rent', header: 'Rent', cell: (r) => r.rent, align: 'right' },
  {
    key: 'status',
    header: 'Status',
    align: 'right',
    cell: (r) => {
      if (r.status === 'active') {
        return <StatusBadge label="Active" color="green" dot />;
      }
      if (r.status === 'expiring') {
        return <StatusBadge label="Expiring" color="amber" dot />;
      }
      return <StatusBadge label="Ended" color="gray" dot />;
    },
  },
];

export function Contracts() {
  return (
    <div className="max-w-3xl">
      <ResponsiveTable
        rows={contracts}
        columns={contractColumns}
        rowKey={(r) => r.id}
        mobileRow={(r) => (
          <DataList
            title={r.tenant}
            items={[
              { label: 'Property', value: r.property, align: 'right' },
              { label: 'Rent', value: r.rent, align: 'right' },
            ]}
          />
        )}
      />
    </div>
  );
}
