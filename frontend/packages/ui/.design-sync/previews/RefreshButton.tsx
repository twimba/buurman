import { RefreshButton } from '@buurman/ui';

export function States() {
  return (
    <div className="flex items-center gap-6">
      <div className="flex flex-col items-center gap-2">
        <RefreshButton onClick={() => {}} />
        <span className="text-sm text-text-secondary">Idle</span>
      </div>
      <div className="flex flex-col items-center gap-2">
        <RefreshButton onClick={() => {}} isRefreshing />
        <span className="text-sm text-text-secondary">Refreshing</span>
      </div>
    </div>
  );
}

export function InToolbar() {
  return (
    <div className="flex w-96 items-center justify-between rounded-lg border border-border-default bg-surface-card px-4 py-3">
      <span className="text-sm font-medium text-text-primary">
        Open invoices
      </span>
      <RefreshButton onClick={() => {}} />
    </div>
  );
}
