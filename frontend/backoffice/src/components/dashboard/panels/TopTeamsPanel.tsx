import { Link } from 'react-router-dom';

import { dl } from '../../../lib/deeplinks';
import { useTopTeams } from '../../../hooks/dashboard';
import { PanelShell } from '../PanelShell';

export const TopTeamsPanel = () => {
  const { data, isLoading, isError } = useTopTeams();
  const teams = data?.teams ?? [];

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
        <ol className="-mx-2 space-y-0.5">
          {teams.map((team, i) => (
            <li key={team.identifier}>
              <Link
                to={team.deeplink}
                className="focus-ring flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-surface-page"
              >
                <span className="w-4 shrink-0 text-xs tabular-nums text-text-muted">
                  {i + 1}
                </span>
                <span className="min-w-0 flex-1 truncate text-sm text-text-primary">
                  {team.name}
                </span>
                <span className="shrink-0 text-xs tabular-nums text-text-secondary">
                  {team.activityScore.toLocaleString()}
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </PanelShell>
  );
};
