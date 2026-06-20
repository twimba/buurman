import { ModalWrapper, Button, FormField, Input } from '@buurman/ui';

export function FormModal() {
  return (
    <ModalWrapper
      open={true}
      onClose={() => {}}
      title="Adjust rent"
      subtitle="Kerkstraat 8 · Amsterdam"
      size="lg"
      footer={
        <>
          <Button variant="secondary" onClick={() => {}}>
            Cancel
          </Button>
          <Button onClick={() => {}}>Save changes</Button>
        </>
      }
    >
      <div className="space-y-4">
        <FormField label="New monthly rent" htmlFor="rent">
          <Input id="rent" defaultValue="1.155,00" />
        </FormField>
        <FormField label="Effective date" htmlFor="date">
          <Input id="date" type="date" defaultValue="2026-07-01" />
        </FormField>
        <p className="text-sm text-text-secondary">
          The tenant will be notified by email once the adjustment is applied.
        </p>
      </div>
    </ModalWrapper>
  );
}
