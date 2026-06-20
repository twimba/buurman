import { Link } from 'react-router-dom';

import { dl } from '../../../lib/deeplinks';
import { useTopTeams } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

// Podium tiers (gold / silver / bronze) for the top 3, indigo for the rest.
const BAR_GRADIENT = [
  'linear-gradient(90deg, rgba(245,158,11,0.32), rgba(245,158,11,0.06))',
  'linear-gradient(90deg, rgba(100,116,139,0.30), rgba(100,116,139,0.06))',
  'linear-gradient(90deg, rgba(234,88,12,0.28), rgba(234,88,12,0.06))',
];
const INDIGO_BAR =
  'linear-gradient(90deg, rgba(99,102,241,0.22), rgba(99,102,241,0.04))';

const BADGE: { background: string; color: string }[] = [
  { background: '#fef3c7', color: '#b45309' },
  { background: '#f1f5f9', color: '#475569' },
  { background: '#ffedd5', color: '#c2410c' },
];
const BADGE_DEFAULT = { background: '#eceff5', color: '#64748b' };

export const TopTeamsPanel = () => {
  const { data, isLoading, isError } = useTopTeams();
  const teams = data?.teams ?? [];
  const max = teams.reduce((m, t) => Math.max(m, t.activityScore), 0);

  return (
    <PanelShell
      title="Top teams"
      deeplink={dl.teams()}
      status={data?.status}
      previewCta={data?.previewCta}
      isLoading={isLoading}
      isError={isError}
    >
      {teams.length === 0 ? (
        <p className="py-4 text-center text-xs text-text-secondary">
          No teams yet.
        </p>
      ) : (
        <ol className="space-y-1.5">
          {teams.map((team, i) => {
            const pct =
              max > 0 ? Math.max(5, (team.activityScore / max) * 100) : 0;
            const badge = BADGE[i] ?? BADGE_DEFAULT;
            return (
              <li key={team.identifier}>
                <Link
                  to={team.deeplink}
                  className="focus-ring relative block overflow-hidden rounded-lg bg-surface-page"
                  title={`${team.name} — ${team.activityScore.toLocaleString()}`}
                >
                  {/* relative data-bar (animates in from the left) */}
                  <span
                    aria-hidden="true"
                    className="absolute inset-y-0 left-0 origin-left rounded-lg motion-safe:animate-mc-grow-x"
                    style={{
                      width: `${pct}%`,
                      background: BAR_GRADIENT[i] ?? INDIGO_BAR,
                    }}
                  />
                  <span className="relative z-10 flex items-center gap-2.5 px-2.5 py-2">
                    <span
                      className="flex h-5 w-5 shrink-0 items-center justify-center rounded-md text-[11px] font-bold tabular-nums"
                      style={badge}
                    >
                      {i + 1}
                    </span>
                    <span
                      className={`min-w-0 flex-1 truncate text-sm text-text-primary ${
                        i < 3 ? 'font-semibold' : 'font-medium'
                      }`}
                    >
                      {team.name}
                    </span>
                    <span className="shrink-0 text-xs font-semibold tabular-nums text-text-secondary">
                      {team.activityScore.toLocaleString()}
                    </span>
                  </span>
                </Link>
              </li>
            );
          })}
        </ol>
      )}
    </PanelShell>
  );
};
