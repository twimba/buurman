import { useProductEntities } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

export const ProductEntitiesPanel = () => {
  const { data, isLoading, isError } = useProductEntities();

  return (
    <PanelShell
      title="Product entities today"
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
    >
      <div className="grid grid-cols-3 gap-3">
        {(data?.entities ?? []).map((entity) => (
          <div
            key={entity.label}
            className="rounded-md bg-surface-page px-3 py-2.5"
          >
            <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
              {entity.label}
            </p>
            <p className="mt-1 text-2xl font-bold leading-none text-text-primary tabular-nums">
              {entity.today}
            </p>
            <p className="mt-1 text-xs text-text-secondary tabular-nums">
              {entity.total.toLocaleString()} total
            </p>
          </div>
        ))}
      </div>
    </PanelShell>
  );
};
