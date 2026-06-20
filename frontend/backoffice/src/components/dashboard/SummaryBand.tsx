import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  ShieldCheck,
} from 'lucide-react';

import {
  useActionQueue,
  useCostWatch,
  useProductEntities,
  useStatusStrip,
} from '../../hooks/dashboard';
import { formatEurMinor } from '../../lib/money';

/**
 * The dashboard headline: a single "is the platform OK?" verdict plus the 3 numbers that matter
 * most, all derived from already-cached panel data (no extra requests).
 */
export const SummaryBand = () => {
  const { data: strip } = useStatusStrip();
  const { data: queue } = useActionQueue();
  const { data: entities } = useProductEntities();
  const { data: cost } = useCostWatch();

  const severities = [
    ...(strip?.pillars ?? []).map((p) => p.severity),
    ...(queue?.items ?? []).map((i) => i.severity),
  ];
  const crit = severities.filter((s) => s === 'crit').length;
  const warn = severities.filter((s) => s === 'warn').length;
  const verdictSev = crit > 0 ? 'crit' : warn > 0 ? 'warn' : 'ok';
  const verdictText =
    crit > 0
      ? `${crit} critical`
      : warn > 0
        ? `${warn} warning${warn === 1 ? '' : 's'}`
        : 'All systems normal';

  const actionCount = queue?.items?.length ?? 0;
  const newThisWeek = (entities?.entities ?? []).reduce(
    (sum, e) => sum + e.last7Days,
    0
  );
  const mom = cost?.momChangePct;

  return (
    <div
      className="flex flex-wrap items-stretch gap-x-6 gap-y-3 rounded-xl border border-border-subtle px-5 py-3.5"
      style={{
        background: 'var(--hero-gradient)',
        boxShadow: 'var(--shadow-inner-top)',
      }}
    >
      {/* Verdict */}
      <div className="flex items-center gap-2.5 pr-6">
        <span
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
          style={{
            background: `var(--severity-${verdictSev}-bg)`,
            color: `var(--severity-${verdictSev})`,
          }}
        >
          {verdictSev === 'ok' ? (
            <ShieldCheck className="h-5 w-5" aria-hidden="true" />
          ) : (
            <AlertTriangle className="h-5 w-5" aria-hidden="true" />
          )}
        </span>
        <div>
          <p className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-text-muted">
            System health
          </p>
          <p
            className="text-[18px] font-bold leading-tight tracking-[-0.02em]"
            style={{ color: `var(--severity-${verdictSev})` }}
          >
            {verdictText}
          </p>
        </div>
      </div>

      <Divider />
      <Metric
        label="Action items"
        value={actionCount.toLocaleString()}
        sub={actionCount === 0 ? 'nothing waiting' : 'need attention'}
      />
      <Divider />
      <Metric
        label="New this week"
        value={`+${newThisWeek.toLocaleString()}`}
        sub="entities · 7d"
        accent
      />
      <Divider />
      <Link to="/costs" className="focus-ring rounded">
        <Metric
          label="Cost run-rate"
          value={formatEurMinor(cost?.totalMonthlyEurMinor ?? 0)}
          sub={
            mom !== undefined && mom !== null ? (
              <span
                className={`inline-flex items-center gap-0.5 tabular-nums ${
                  mom > 0 ? 'text-red-600' : 'text-emerald-600'
                }`}
              >
                {mom > 0 ? (
                  <ArrowUpRight className="h-3 w-3" aria-hidden="true" />
                ) : (
                  <ArrowDownRight className="h-3 w-3" aria-hidden="true" />
                )}
                {Math.abs(mom).toFixed(1)}% MoM
              </span>
            ) : (
              '/mo'
            )
          }
        />
      </Link>
    </div>
  );
};

const Divider = () => (
  <span
    className="hidden w-px self-stretch bg-border-subtle sm:block"
    aria-hidden="true"
  />
);

const Metric = ({
  label,
  value,
  sub,
  accent,
}: {
  label: string;
  value: string;
  sub: React.ReactNode;
  accent?: boolean;
}) => (
  <div className="flex flex-col justify-center">
    <p className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-text-muted">
      {label}
    </p>
    <p
      className={`text-[22px] font-bold leading-none tracking-[-0.025em] tabular-nums ${
        accent ? 'text-primary-600' : 'text-text-primary'
      }`}
    >
      {value}
    </p>
    <p className="mt-1 text-[11px] leading-[14px] text-text-muted">{sub}</p>
  </div>
);
