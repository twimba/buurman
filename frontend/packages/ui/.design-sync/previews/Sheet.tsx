import { Sheet, Button, DataList, StatusBadge } from '@buurman/ui';

export function BottomSheet() {
  return (
    <Sheet
      open={true}
      onClose={() => {}}
      forceSheet
      title="Payment details"
      description="Rent · December 2026"
      footer={
        <div className="flex w-full justify-end gap-2">
          <Button variant="secondary" onClick={() => {}}>
            Close
          </Button>
          <Button onClick={() => {}}>Mark paid</Button>
        </div>
      }
    >
      <DataList
        title="Maple Street 14"
        trailing={<StatusBadge label="Overdue" color="orange" />}
        items={[
          { label: 'Tenant', value: 'Sanne de Vries' },
          { label: 'Due date', value: '1 Dec 2026' },
          {
            label: 'Amount',
            value: (
              <span className="font-semibold text-text-primary">€1.250,00</span>
            ),
            align: 'right',
          },
        ]}
      />
    </Sheet>
  );
}
