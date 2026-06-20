import { PullToRefresh, DataList, StatusBadge } from '@buurman/ui';

export function PaymentList() {
  return (
    <PullToRefresh onRefresh={() => Promise.resolve()}>
      <ul className="space-y-3 p-3">
        {[
          {
            street: 'Maple Street 14',
            tenant: 'Sanne de Vries',
            amount: '€1.250,00',
            status: 'Overdue' as const,
          },
          {
            street: 'Kerkstraat 8',
            tenant: 'Joris Bakker',
            amount: '€1.100,00',
            status: 'Paid' as const,
          },
          {
            street: 'Lindelaan 22',
            tenant: 'Fatima El Amrani',
            amount: '€980,00',
            status: 'Due' as const,
          },
        ].map((p) => (
          <li
            key={p.street}
            className="bg-surface-card border border-border-default p-4"
          >
            <DataList
              title={p.street}
              trailing={
                <StatusBadge
                  label={p.status}
                  color={
                    p.status === 'Overdue'
                      ? 'orange'
                      : p.status === 'Paid'
                        ? 'green'
                        : 'gray'
                  }
                />
              }
              items={[
                { label: 'Tenant', value: p.tenant },
                {
                  label: 'Amount',
                  value: (
                    <span className="font-semibold text-text-primary">
                      {p.amount}
                    </span>
                  ),
                  align: 'right',
                },
              ]}
            />
          </li>
        ))}
      </ul>
    </PullToRefresh>
  );
}
