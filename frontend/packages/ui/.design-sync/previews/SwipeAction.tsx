import { SwipeAction, DataList, StatusBadge } from '@buurman/ui';
import { CheckCircle, Trash2 } from 'lucide-react';

export function PaymentRow() {
  return (
    <SwipeAction
      onClick={() => {}}
      leftActions={[
        {
          label: 'Mark Paid',
          icon: CheckCircle,
          tone: 'success',
          onAction: () => {},
        },
        {
          label: 'Delete',
          icon: Trash2,
          tone: 'danger',
          onAction: () => {},
        },
      ]}
    >
      <div className="bg-surface-card border border-border-default p-4 min-h-touch">
        <DataList
          title="Maple Street 14"
          trailing={<StatusBadge label="Overdue" color="orange" />}
          items={[
            { label: 'Due date', value: '1 Dec 2026' },
            { label: 'Contact', value: 'Sanne de Vries' },
            {
              label: 'Amount',
              value: (
                <span className="font-semibold text-text-primary">
                  €1.250,00
                </span>
              ),
              align: 'right',
            },
          ]}
        />
      </div>
    </SwipeAction>
  );
}
