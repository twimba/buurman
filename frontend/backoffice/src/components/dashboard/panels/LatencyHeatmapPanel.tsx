import { useLatencyHeatmap } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

export const LatencyHeatmapPanel = () => {
  const { data, isLoading, isError } = useLatencyHeatmap();
  return (
    <PanelShell
      title="Latency heatmap"
      status={data?.status}
      previewCta={data?.previewCta}
      docsLink={data?.docsLink}
      isLoading={isLoading}
      isError={isError}
    >
      {null}
    </PanelShell>
  );
};
