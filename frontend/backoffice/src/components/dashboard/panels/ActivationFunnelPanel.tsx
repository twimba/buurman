import { useFunnel } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

export const ActivationFunnelPanel = () => {
  const { data, isLoading, isError } = useFunnel();

  return (
    <PanelShell
      title="Activation funnel"
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
    >
      <div className="space-y-2.5">
        {(data?.stages ?? []).map((stage) => (
          <div key={stage.label}>
            <div className="flex items-baseline justify-between text-xs">
              <span className="text-text-secondary">{stage.label}</span>
              <span className="font-semibold text-text-primary tabular-nums">
                {stage.count.toLocaleString()}
                <span className="ml-1 font-normal text-text-muted">
                  {stage.pctOfTop}%
                </span>
              </span>
            </div>
            <div className="mt-1 h-2 overflow-hidden rounded-full bg-surface-page">
              <div
                className="h-full rounded-full bg-primary-500"
                style={{
                  width: `${Math.min(100, Math.max(0, stage.pctOfTop))}%`,
                }}
              />
            </div>
          </div>
        ))}
      </div>
    </PanelShell>
  );
};
