import { useQueryClient } from '@tanstack/react-query';
import { Check, LayoutGrid, Pause, Play, RefreshCw } from 'lucide-react';

import { useDashboardContext } from '../../context/DashboardContext';
import { dashboardQueryKey } from '../../hooks/dashboard';

/** Mission-control topbar: title, global refresh, Pause, and the layout-edit toggle. */
export const DashboardTopbar = () => {
  const queryClient = useQueryClient();
  const { paused, togglePaused, editing, toggleEditing } =
    useDashboardContext();

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: dashboardQueryKey });
  };

  return (
    <div className="flex items-start justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold text-text-primary">
          Mission Control
        </h1>
        <p className="mt-1 text-sm text-text-secondary">
          Platform health, growth and support — all in one view.
        </p>
      </div>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={toggleEditing}
          aria-pressed={editing}
          title={editing ? 'Finish editing layout' : 'Customize layout'}
          className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary"
        >
          {editing ? (
            <>
              <Check className="h-4 w-4" aria-hidden="true" /> Done
            </>
          ) : (
            <>
              <LayoutGrid className="h-4 w-4" aria-hidden="true" /> Customize
            </>
          )}
        </button>
        <button
          type="button"
          onClick={togglePaused}
          aria-pressed={paused}
          title={paused ? 'Resume auto-refresh' : 'Pause auto-refresh'}
          className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary"
        >
          {paused ? (
            <>
              <Play className="h-4 w-4" aria-hidden="true" /> Resume
            </>
          ) : (
            <>
              <Pause className="h-4 w-4" aria-hidden="true" /> Pause
            </>
          )}
        </button>
        <button
          type="button"
          onClick={refreshAll}
          className="focus-ring inline-flex items-center gap-1.5 rounded-lg border border-border-default bg-surface-card px-3 py-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text-primary"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" /> Refresh
        </button>
      </div>
    </div>
  );
};
