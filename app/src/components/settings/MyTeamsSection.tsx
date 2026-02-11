import {
  Shield,
  Users,
  Crown,
  CheckCircle,
  Clock,
  Loader2,
  Star,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import {
  usePendingInvitations,
  useAcceptInvitation,
} from '@/hooks/useTeamHooks';
import { useFormatDate } from '@/hooks/useFormatDate';

const formatRole = (role: string) => {
  switch (role) {
    case 'TEAM_ADMIN':
      return 'Administrator';
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

export const MyTeamsSection = () => {
  const { teams } = useTeam();
  const { data: pendingInvitations = [] } = usePendingInvitations();
  const acceptMutation = useAcceptInvitation();
  const { formatDate } = useFormatDate();

  return (
    <div className="space-y-6">
      {/* Active Teams */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          My Teams
        </h3>
        <div className="space-y-3">
          {teams.map((team) => (
            <div
              key={team.identifier}
              className={`bg-white dark:bg-[#14161f] rounded-xl border p-5 ${
                team.isActive
                  ? 'border-[#5c7cfa] dark:border-[#5c7cfa]/50 ring-1 ring-[#5c7cfa]/20'
                  : 'border-[#e2e6f0] dark:border-[#2a2e3f]'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0 h-10 w-10 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-full flex items-center justify-center">
                    <Shield className="h-5 w-5 text-[#5c7cfa]" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                        {team.name}
                      </span>
                      {team.isActive && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider bg-[#5c7cfa]/10 text-[#5c7cfa] dark:text-[#91a7ff]">
                          Active
                        </span>
                      )}
                      {team.isDefault && (
                        <Star className="h-3.5 w-3.5 text-amber-500 fill-amber-500" />
                      )}
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass(team.role)}`}
                      >
                        {team.isOwner && <Crown className="h-3 w-3" />}
                        {formatRole(team.role)}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Users className="h-3.5 w-3.5" />
                        {team.memberCount}{' '}
                        {team.memberCount === 1 ? 'member' : 'members'}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center">
                  <CheckCircle className="h-5 w-5 text-green-500" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Pending Invitations */}
      {pendingInvitations.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            Pending Invitations
          </h3>
          <div className="space-y-3">
            {pendingInvitations.map((inv) => (
              <div
                key={inv.token}
                className="bg-white dark:bg-[#14161f] rounded-xl border border-amber-200 dark:border-amber-900/50 p-5"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <div className="flex-shrink-0 h-10 w-10 bg-amber-50 dark:bg-amber-900/20 rounded-full flex items-center justify-center">
                      <Clock className="h-5 w-5 text-amber-600 dark:text-amber-400" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                          {inv.teamName}
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300">
                          Pending
                        </span>
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${roleBadgeClass(inv.role)}`}
                        >
                          {formatRole(inv.role)}
                        </span>
                        <span>Invited by {inv.inviterName}</span>
                      </div>
                      <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-1">
                        Expires {formatDate(inv.expiresAt)}
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
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
