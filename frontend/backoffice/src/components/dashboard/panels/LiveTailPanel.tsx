import type { LogLine } from '../../../generated/models';
import { useLiveTail } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

const levelClass = (level: string): string => {
  switch (level) {
    case 'ERROR':
      return 'text-red-600';
    case 'WARN':
      return 'text-amber-600';
    case 'INFO':
      return 'text-emerald-600';
    default:
      return 'text-text-muted';
  }
};

const time = (iso: string): string => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleTimeString();
};

const Line = ({ line }: { line: LogLine }) => (
  <div className="flex gap-2 whitespace-nowrap py-0.5">
    <span className="shrink-0 text-text-muted tabular-nums">
      {time(line.timestamp)}
    </span>
    <span className={`w-10 shrink-0 font-semibold ${levelClass(line.level)}`}>
      {line.level}
    </span>
    <span className="shrink-0 text-text-secondary">{line.logger}</span>
    <span className="truncate text-text-primary">{line.message}</span>
  </div>
);

export const LiveTailPanel = () => {
  const { data, isLoading, isError } = useLiveTail();
  const lines = data?.lines ?? [];

  return (
    <PanelShell
      title="Live tail"
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
    >
      {lines.length === 0 ? (
        <p className="py-4 text-center text-xs text-text-secondary">
          No recent log activity.
        </p>
      ) : (
        <div className="max-h-48 overflow-auto font-mono text-[11px] leading-relaxed">
          {lines.map((line, i) => (
            <Line key={`${line.timestamp}-${i}`} line={line} />
          ))}
        </div>
      )}
    </PanelShell>
  );
};
