import { Users, CheckCircle, Loader2, Shield } from 'lucide-react';
import {
  usePendingInvitations,
  useAcceptInvitation,
} from '@/hooks/useTeamHooks';
import { useFormatDate } from '@/hooks/useFormatDate';

const formatRole = (role: string) => {
  switch (role) {
    case 'TEAM_ADMIN':
      return 'Admin';
    case 'TEAM_EDITOR':
      return 'Editor';
    case 'TEAM_VIEWER':
      return 'Viewer';
    default:
      return role;
  }
};

const roleBadgeClass = (role: string) => {
  switch (role) {
    case 'TEAM_ADMIN':
      return 'bg-primary-50 text-primary-700';
    case 'TEAM_EDITOR':
      return 'bg-info-bg text-info-text';
    default:
      return 'bg-surface-inset text-text-secondary';
  }
};

export const PendingInvitationsPanel = () => {
  const { data: invitations, isLoading } = usePendingInvitations();
  const acceptMutation = useAcceptInvitation();
  const { formatDate } = useFormatDate();

  if (isLoading || !invitations || invitations.length === 0) {
    return null;
  }

  return (
    <div className="bg-info-bg rounded-lg shadow-sm border border-info-border p-6">
      <div className="flex items-center gap-3 mb-4">
        <div className="p-2 bg-info-bg rounded-lg">
          <Users className="h-5 w-5 text-primary-500" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-text-primary">
            Pending Team Invitations
          </h2>
          <p className="text-sm text-text-secondary">
            You have been invited to join{' '}
            {invitations.length === 1
              ? 'a team'
              : `${invitations.length} teams`}
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {invitations.map((inv) => (
          <div
            key={inv.token}
            className="flex items-center justify-between bg-surface-card rounded-lg border border-border-default px-4 py-3"
          >
            <div className="flex items-center gap-3 min-w-0 flex-1">
              <div className="flex-shrink-0 h-10 w-10 bg-surface-inset rounded-full flex items-center justify-center">
                <Shield className="h-5 w-5 text-primary-500" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-text-primary truncate">
                    {inv.teamName}
                  </span>
                  <span
                    className={`flex-shrink-0 px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass(inv.role)}`}
                  >
                    {formatRole(inv.role)}
                  </span>
                </div>
                <p className="text-xs text-text-secondary">
                  Invited by {inv.inviterName} &middot; Expires{' '}
                  {formatDate(inv.expiresAt)}
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
              Accept
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
