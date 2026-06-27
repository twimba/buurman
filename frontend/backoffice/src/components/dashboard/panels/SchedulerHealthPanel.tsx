import { Link } from 'react-router-dom';

import { dl } from '../../../lib/deeplinks';
import { useSchedulerHealth } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

const tileColor = (state: string): string => {
  switch (state) {
    case 'NORMAL':
      return 'bg-success';
    case 'PAUSED':
      return 'bg-warning';
    case 'ERROR':
    case 'BLOCKED':
      return 'bg-error';
    default:
      return 'bg-neutral-300';
  }
};

// A non-color glyph for failing tiles so state is never conveyed by color alone.
const tileGlyph = (state: string): string => {
  switch (state) {
    case 'ERROR':
    case 'BLOCKED':
      return '!';
    case 'PAUSED':
      return '‖';
    default:
      return '';
  }
};

export const SchedulerHealthPanel = () => {
  const { data, isLoading, isError, refetch } = useSchedulerHealth();

  return (
    <PanelShell
      title="Scheduler health"
      deeplink={dl.scheduler()}
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
      onRetry={refetch}
    >
      <div className="mb-3 flex items-center gap-3 text-xs">
        <span className="text-text-secondary">
          <span className="font-bold text-text-primary tabular-nums">
            {data?.normal ?? 0}
          </span>{' '}
          ok
        </span>
        {(data?.error ?? 0) + (data?.blocked ?? 0) > 0 && (
          <span className="font-semibold text-error-text tabular-nums">
            {(data?.error ?? 0) + (data?.blocked ?? 0)} failing
          </span>
        )}
        {(data?.paused ?? 0) > 0 && (
          <span className="text-warning-text tabular-nums">
            {data?.paused} paused
          </span>
        )}
      </div>
      <div className="flex flex-wrap gap-1">
        {(data?.jobs ?? []).map((job) => (
          <Link
            key={`${job.jobGroup}.${job.jobName}`}
            to={job.deeplink}
            title={`${job.jobName} — ${job.state}`}
            aria-label={`${job.jobName} is ${job.state}`}
            className={`focus-ring flex h-4 w-4 items-center justify-center rounded-sm text-[9px] font-bold leading-none text-white ${tileColor(job.state)}`}
          >
            {tileGlyph(job.state)}
          </Link>
        ))}
      </div>
    </PanelShell>
  );
};
