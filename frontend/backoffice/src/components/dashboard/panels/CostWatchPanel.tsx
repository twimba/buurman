import { useCostWatch } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

export const CostWatchPanel = () => {
  const { data, isLoading, isError } = useCostWatch();
  return (
    <PanelShell
      title="Cost watch"
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
