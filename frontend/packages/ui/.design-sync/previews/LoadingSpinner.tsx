import { LoadingSpinner } from '@buurman/ui';

export function Default() {
  return (
    <div className="rounded-lg border border-border-default bg-surface-card">
      <LoadingSpinner />
    </div>
  );
}

export function WithMessage() {
  return (
    <div className="rounded-lg border border-border-default bg-surface-card">
      <LoadingSpinner message="Loading properties…" />
    </div>
  );
}
