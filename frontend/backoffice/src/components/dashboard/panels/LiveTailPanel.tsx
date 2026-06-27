import { ExternalLink } from 'lucide-react';

import { PanelShell } from '../PanelShell';

// In production the backoffice runs on a fleet of pods; a per-process in-memory tail would only
// ever show one pod's logs. Aggregated, fleet-wide live tail lives in BetterStack — link out to it
// rather than reimplement log aggregation in the dashboard.
const BETTERSTACK_LIVE_TAIL_URL =
  'https://telemetry.betterstack.com/team/t505111/tail?s=l1735995';

export const LiveTailPanel = () => (
  <PanelShell title="Live tail">
    <div className="flex h-full flex-col justify-center gap-3 py-2">
      <p className="text-xs text-text-secondary">
        Logs are aggregated across the whole fleet in BetterStack. Open the live
        tail there for real-time, cluster-wide log streaming.
      </p>
      <a
        href={BETTERSTACK_LIVE_TAIL_URL}
        target="_blank"
        rel="noreferrer noopener"
        className="focus-ring inline-flex w-fit items-center gap-1.5 rounded-md border border-border-default px-2.5 py-1.5 text-xs font-medium text-text-secondary hover:text-text-primary"
      >
        <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
        Open live tail in BetterStack
      </a>
    </div>
  </PanelShell>
);
