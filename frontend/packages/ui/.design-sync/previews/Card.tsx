import { Card, Button } from '@buurman/ui';

export function Basic() {
  return (
    <Card>
      <h3 className="text-base font-semibold text-text-primary">
        Maple Street 14
      </h3>
      <p className="mt-1 text-sm text-text-secondary">
        Two-bedroom apartment · 78 m² · Rotterdam
      </p>
    </Card>
  );
}

export function WithActions() {
  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-base font-semibold text-text-primary">
            Lease renewal
          </h3>
          <p className="mt-1 text-sm text-text-secondary">
            Expires in 14 days — review the proposed terms.
          </p>
        </div>
        <Button size="sm">Review</Button>
      </div>
    </Card>
  );
}

export function Padding() {
  return (
    <div className="flex flex-col gap-3">
      <Card padding="sm">
        <span className="text-sm text-text-secondary">Padding: sm</span>
      </Card>
      <Card padding="md">
        <span className="text-sm text-text-secondary">Padding: md</span>
      </Card>
      <Card padding="lg">
        <span className="text-sm text-text-secondary">Padding: lg</span>
      </Card>
    </div>
  );
}
