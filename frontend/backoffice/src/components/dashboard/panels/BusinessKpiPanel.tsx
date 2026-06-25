import { useBusiness } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

export const BusinessKpiPanel = () => {
  const { data, isLoading, isError } = useBusiness();
  return (
    <PanelShell
      title="Business · MTD"
      status={data?.status}
      previewCta={data?.previewCta}
      docsLink={data?.docsLink}
      isLoading={isLoading}
      isError={isError}
    >
      <p className="text-xs text-text-muted">
        Billing metrics are not available yet.
      </p>
    </PanelShell>
  );
};
