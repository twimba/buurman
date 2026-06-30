import {
  Shield,
  Users,
  Crown,
  CheckCircle,
  Clock,
  Loader2,
  Star,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useTeam } from '@/context/TeamContext';
import {
  usePendingInvitations,
  useAcceptInvitation,
} from '@/hooks/useTeamHooks';
import { useFormatDate } from '@/hooks/useFormatDate';

// formatRole moved inside component

const roleBadgeClass = (role: string) => {
  switch (role) {
    case 'TEAM_ADMIN':
      return 'bg-info-bg text-info-text';
    case 'TEAM_EDITOR':
      return 'bg-info-bg text-info-text';
    default:
      return 'bg-surface-inset text-text-secondary';
  }
};

export const MyTeamsSection = () => {
  const { t } = useTranslation('settings');
  const { teams } = useTeam();

  const formatRole = (role: string) => {
    switch (role) {
      case 'TEAM_ADMIN':
        return t('myTeams.roles.admin');
      case 'TEAM_EDITOR':
        return t('myTeams.roles.editor');
      case 'TEAM_VIEWER':
        return t('myTeams.roles.viewer');
      default:
        return role;
    }
  };
  const { data: pendingInvitations = [] } = usePendingInvitations();
  const acceptMutation = useAcceptInvitation();
  const { formatDate } = useFormatDate();

  return (
    <div className="space-y-6">
      {/* Active Teams */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('myTeams.title')}
        </h3>
        <div className="space-y-3">
          {teams.map((team) => (
            <div
              key={team.identifier}
              className={`bg-surface-card rounded-lg border p-5 ${
                team.isActive
                  ? 'border-primary-500 ring-1 ring-primary-500/20'
                  : 'border-border-default'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0 h-10 w-10 bg-surface-inset rounded-full flex items-center justify-center">
                    <Shield className="h-5 w-5 text-primary-500" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-text-primary">
                        {team.name}
                      </span>
                      {team.isActive && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider bg-primary-500/10 text-primary-500 dark:text-primary-300">
                          {t('myTeams.active')}
                        </span>
                      )}
                      {team.isDefault && (
                        <Star className="h-3.5 w-3.5 text-amber-500 fill-amber-500" />
                      )}
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-sm text-text-secondary">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass(team.role)}`}
                      >
                        {team.isOwner && <Crown className="h-3 w-3" />}
                        {formatRole(team.role)}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Users className="h-3.5 w-3.5" />
                        {t('myTeams.member', { count: team.memberCount })}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center">
                  <CheckCircle className="h-5 w-5 text-success-text" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Pending Invitations */}
      {pendingInvitations.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            {t('myTeams.pendingInvitations')}
          </h3>
          <div className="space-y-3">
            {pendingInvitations.map((inv) => (
              <div
                key={inv.token}
                className="bg-surface-card rounded-lg border border-warning-border p-5"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <div className="flex-shrink-0 h-10 w-10 bg-warning-bg rounded-full flex items-center justify-center">
                      <Clock className="h-5 w-5 text-warning-text" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-text-primary">
                          {inv.teamName}
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider bg-warning-bg text-warning-text">
                          {t('myTeams.pending')}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-sm text-text-secondary">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass(inv.role)}`}
                        >
                          {formatRole(inv.role)}
                        </span>
                        <span>
                          {t('myTeams.invitedBy', { name: inv.inviterName })}
                        </span>
                      </div>
                      <p className="text-xs text-text-muted mt-1">
                        {t('myTeams.expires', {
                          date: formatDate(inv.expiresAt ?? ''),
                        })}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() =>
                      acceptMutation.mutate(inv.token, {
                        onSuccess: () => {
                          window.location.reload();
                        },
                      })
                    }
                    disabled={acceptMutation.isPending}
                    className="flex-shrink-0 inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-lg transition-colors disabled:opacity-50"
                  >
                    {acceptMutation.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <CheckCircle className="h-4 w-4" />
                    )}
                    {t('myTeams.accept')}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
