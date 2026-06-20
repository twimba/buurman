import {
  Building2,
  Contact,
  FileText,
  User,
  Users2,
  type LucideIcon,
} from 'lucide-react';

import { useProductEntities } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

const ICONS: Record<string, LucideIcon> = {
  Properties: Building2,
  Contracts: FileText,
  Contacts: Contact,
  Users: User,
  Teams: Users2,
};

const EntityRow = ({
  label,
  last7Days,
  total,
}: {
  label: string;
  last7Days: number;
  total: number;
}) => {
  const Icon = ICONS[label] ?? Building2;
  const hasDelta = last7Days > 0;

  return (
    <div className="flex items-center gap-3 rounded-md border border-border-subtle bg-surface-card px-3 py-2.5 transition-colors hover:border-border-default hover:bg-surface-page">
      <span
        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-md ${
          hasDelta
            ? 'bg-primary-50 text-primary-600'
            : 'bg-surface-page text-text-muted'
        }`}
      >
        <Icon className="h-4 w-4" aria-hidden="true" strokeWidth={2} />
      </span>

      <div className="min-w-0 flex-1">
        <p className="truncate text-[11px] font-medium uppercase tracking-wider text-text-muted">
          {label}
        </p>
        <p className="mt-0.5 text-xl font-bold leading-none text-text-primary tabular-nums">
          {total.toLocaleString()}
        </p>
      </div>

      <span
        className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold tabular-nums ${
          hasDelta ? 'bg-primary-50 text-primary-700' : 'text-text-disabled'
        }`}
        aria-label={
          hasDelta
            ? `${last7Days} added in the last 7 days`
            : 'No change in the last 7 days'
        }
      >
        {hasDelta ? `+${last7Days.toLocaleString()}` : '—'}
      </span>
    </div>
  );
};

export const ProductEntitiesPanel = () => {
  const { data, isLoading, isError } = useProductEntities();
  const entities = data?.entities ?? [];

  return (
    <PanelShell
      title="Product entities · last 7 days"
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
    >
      {entities.length === 0 ? (
        <p className="py-4 text-center text-xs text-text-secondary">
          No entities yet.
        </p>
      ) : (
        <div className="@container">
          <div className="grid grid-cols-1 gap-2 @[28rem]:grid-cols-2">
            {entities.map((entity) => (
              <EntityRow
                key={entity.label}
                label={entity.label}
                last7Days={entity.last7Days}
                total={entity.total}
              />
            ))}
          </div>
        </div>
      )}
    </PanelShell>
  );
};
