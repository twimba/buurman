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
      return 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300';
    case 'TEAM_EDITOR':
      return 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300';
    default:
      return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]';
  }
};

export const PendingInvitationsPanel = () => {
  const { data: invitations, isLoading } = usePendingInvitations();
  const acceptMutation = useAcceptInvitation();
  const { formatDate } = useFormatDate();

  if (isLoading || !invitations || invitations.length === 0) return null;

  return (
    <div className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-[#14161f] dark:to-[#1a1d2e] rounded-xl shadow-sm border border-blue-200 dark:border-[#2a2e3f] p-6">
      <div className="flex items-center gap-3 mb-4">
        <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
          <Users className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Pending Team Invitations
          </h2>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
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
            className="flex items-center justify-between bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] px-4 py-3"
          >
            <div className="flex items-center gap-3 min-w-0 flex-1">
              <div className="flex-shrink-0 h-10 w-10 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-full flex items-center justify-center">
                <Shield className="h-5 w-5 text-[#5c7cfa]" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                    {inv.teamName}
                  </span>
                  <span
                    className={`flex-shrink-0 px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass(inv.role)}`}
                  >
                    {formatRole(inv.role)}
                  </span>
                </div>
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
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
              className="flex-shrink-0 inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] hover:bg-[#4c6ef5] rounded-lg transition-colors disabled:opacity-50"
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
