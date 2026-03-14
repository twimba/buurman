import { useState } from 'react';
import {
  Users,
  UserPlus,
  Mail,
  Trash2,
  Edit3,
  Shield,
  Crown,
  Loader2,
  Clock,
  RefreshCw,
} from 'lucide-react';
import {
  useCurrentTeam,
  useTeamMembers,
  useTeamPendingInvitations,
  useCreateInvitation,
  useResendInvitation,
  useRemoveMember,
  useUpdateMemberRole,
  useTransferOwnership,
} from '../../hooks/useTeamHooks';
import { useTeam } from '../../context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';

type Role = 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';

export const TeamSettingsSection = () => {
  const { formatDate } = useFormatDate();
  const { canManageMembers } = useTeam();
  const { data: team, isLoading: teamLoading } = useCurrentTeam();
  const { data: members, isLoading: membersLoading } = useTeamMembers(
    team?.identifier
  );

  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showRemoveModal, setShowRemoveModal] = useState(false);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<Role>('TEAM_VIEWER');
  const [newRole, setNewRole] = useState<Role>('TEAM_VIEWER');

  const { data: pendingInvitations } = useTeamPendingInvitations(
    team?.identifier
  );

  const createInvitationMutation = useCreateInvitation(team?.identifier ?? '');
  const resendInvitationMutation = useResendInvitation(team?.identifier ?? '');
  const removeMemberMutation = useRemoveMember(team?.identifier ?? '');
  const updateRoleMutation = useUpdateMemberRole(team?.identifier ?? '');
  const transferOwnershipMutation = useTransferOwnership(
    team?.identifier ?? ''
  );

  const roleLabels: Record<Role, string> = {
    TEAM_ADMIN: 'Admin',
    TEAM_EDITOR: 'Editor',
    TEAM_VIEWER: 'Viewer',
  };

  const roleDescriptions: Record<Role, string> = {
    TEAM_ADMIN: 'Full access to all features and settings',
    TEAM_EDITOR: 'Can create and edit properties, tenants, and contracts',
    TEAM_VIEWER: 'Read-only access to all data',
  };

  const roleColors: Record<Role, string> = {
    TEAM_ADMIN: 'bg-info-bg text-info-text',
    TEAM_EDITOR: 'bg-info-bg text-info-text',
    TEAM_VIEWER: 'bg-surface-inset text-text-primary',
  };

  const selectedMember = members?.find(
    (m) => m.userIdentifier === selectedMemberId
  );

  const handleInviteMember = () => {
    if (!inviteEmail.trim()) {
      return;
    }
    createInvitationMutation.mutate(
      { email: inviteEmail, role: inviteRole },
      {
        onSuccess: () => {
          setShowInviteModal(false);
          setInviteEmail('');
          setInviteRole('TEAM_VIEWER');
        },
      }
    );
  };

  const handleRemoveMember = () => {
    if (!selectedMemberId) {
      return;
    }
    removeMemberMutation.mutate(selectedMemberId, {
      onSuccess: () => {
        setShowRemoveModal(false);
        setSelectedMemberId(null);
      },
    });
  };

  const handleUpdateRole = () => {
    if (!selectedMemberId) {
      return;
    }
    updateRoleMutation.mutate(
      { memberId: selectedMemberId, data: { role: newRole } },
      {
        onSuccess: () => {
          setShowRoleModal(false);
          setSelectedMemberId(null);
        },
      }
    );
  };

  const handleTransferOwnership = () => {
    if (!selectedMemberId) {
      return;
    }
    transferOwnershipMutation.mutate(selectedMemberId, {
      onSuccess: () => {
        setShowTransferModal(false);
        setSelectedMemberId(null);
      },
    });
  };

  if (teamLoading || membersLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500 dark:text-primary-300" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Team Members Card */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
        <div className="p-6 border-b border-border-default">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                Team Members
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {members?.length ?? 0} member{members?.length !== 1 ? 's' : ''}
              </p>
            </div>
            {canManageMembers && (
              <button
                onClick={() => setShowInviteModal(true)}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2"
              >
                <UserPlus className="h-4 w-4" />
                Invite Member
              </button>
            )}
          </div>
        </div>

        <div className="divide-y divide-border-default">
          {members?.map((member) => (
            <div
              key={member.userIdentifier}
              className="p-6 hover:bg-surface-inset"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-full bg-info-bg flex items-center justify-center">
                    <Users className="h-6 w-6 text-primary-500 dark:text-primary-300" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-text-primary">
                        {member.name}
                      </p>
                      {member.isCurrentUser && (
                        <span className="px-2 py-0.5 bg-success-bg text-success-text text-xs font-semibold rounded">
                          You
                        </span>
                      )}
                      {member.isOwner && (
                        <span className="px-2 py-0.5 bg-warning-bg text-warning-text text-xs font-semibold rounded flex items-center gap-1">
                          <Crown className="h-3 w-3" />
                          Owner
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-text-secondary">
                      {member.email}
                    </p>
                    <p className="text-xs text-text-secondary mt-1">
                      Joined {formatDate(member.joinedAt)}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <span
                      className={`px-3 py-1 text-xs font-semibold rounded ${roleColors[member.role as Role]}`}
                    >
                      {roleLabels[member.role as Role]}
                    </span>
                    <p className="text-xs text-text-secondary mt-1">
                      {roleDescriptions[member.role as Role]}
                    </p>
                  </div>
                  {canManageMembers && !member.isCurrentUser && (
                    <div className="flex gap-1">
                      <button
                        onClick={() => {
                          setSelectedMemberId(member.userIdentifier);
                          setNewRole(member.role as Role);
                          setShowRoleModal(true);
                        }}
                        className="p-2 text-primary-500 hover:bg-primary-50 rounded-lg transition-colors"
                        title="Change role"
                      >
                        <Edit3 className="h-4 w-4" />
                      </button>
                      {!member.isOwner && (
                        <button
                          onClick={() => {
                            setSelectedMemberId(member.userIdentifier);
                            setShowTransferModal(true);
                          }}
                          className="p-2 text-warning-text hover:bg-warning-bg rounded-lg transition-colors"
                          title="Transfer ownership"
                        >
                          <Crown className="h-4 w-4" />
                        </button>
                      )}
                      <button
                        onClick={() => {
                          setSelectedMemberId(member.userIdentifier);
                          setShowRemoveModal(true);
                        }}
                        className="p-2 text-error-text hover:bg-error-bg rounded-lg transition-colors"
                        title="Remove member"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Pending Invitations Card */}
      {canManageMembers &&
        pendingInvitations &&
        pendingInvitations.length > 0 && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
            <div className="p-6 border-b border-border-default">
              <div className="flex items-center gap-2">
                <Clock className="h-5 w-5 text-warning-text" />
                <div>
                  <h2 className="text-xl font-semibold text-text-primary">
                    Pending Invitations
                  </h2>
                  <p className="text-sm text-text-secondary mt-1">
                    {pendingInvitations.length} pending invitation
                    {pendingInvitations.length !== 1 ? 's' : ''}
                  </p>
                </div>
              </div>
            </div>

            <div className="divide-y divide-border-default">
              {pendingInvitations.map((inv) => (
                <div key={inv.token} className="p-6 hover:bg-surface-inset">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="h-12 w-12 rounded-full bg-warning-bg flex items-center justify-center">
                        <Mail className="h-6 w-6 text-warning-text" />
                      </div>
                      <div>
                        <p className="font-semibold text-text-primary">
                          {inv.email}
                        </p>
                        <div className="flex items-center gap-2 mt-1">
                          <span
                            className={`px-3 py-0.5 text-xs font-semibold rounded ${roleColors[inv.role as Role]}`}
                          >
                            {roleLabels[inv.role as Role]}
                          </span>
                          <span className="text-xs text-text-secondary">
                            Invited by {inv.inviterName}
                          </span>
                        </div>
                        <p className="text-xs text-text-muted mt-1">
                          Sent {formatDate(inv.invitedAt)} &middot; Expires{' '}
                          {formatDate(inv.expiresAt)}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => resendInvitationMutation.mutate(inv.token)}
                      disabled={resendInvitationMutation.isPending}
                      className="flex-shrink-0 inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-primary-500 border border-primary-500/30 hover:bg-primary-500/5 rounded-lg transition-colors disabled:opacity-50"
                      title="Resend invitation"
                    >
                      {resendInvitationMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <RefreshCw className="h-4 w-4" />
                      )}
                      Resend
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

      {/* Roles Reference — compact informational banner */}
      <div className="bg-surface-page rounded-lg border border-border-default px-4 py-3">
        <div className="flex items-center gap-2 text-xs text-text-secondary">
          <Shield className="h-3.5 w-3.5 shrink-0" />
          <span className="font-medium">Roles:</span>
          {Object.entries(roleLabels).map(([role, label]) => (
            <span key={role} className="inline-flex items-center gap-1">
              <span
                className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${roleColors[role as Role]}`}
              >
                {label}
              </span>
              <span className="text-text-muted">
                {roleDescriptions[role as Role]}
              </span>
              {role !== 'TEAM_VIEWER' && (
                <span className="text-text-disabled mx-1">&middot;</span>
              )}
            </span>
          ))}
        </div>
      </div>

      {/* Invite Member Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-border-default">
              <h3 className="text-xl font-semibold text-text-primary">
                Invite Team Member
              </h3>
              <p className="text-sm text-text-secondary mt-1">
                Send an invitation to join your team
              </p>
            </div>

            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
                  <input
                    type="email"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    placeholder="member@example.com"
                    className="w-full pl-10 pr-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Role
                </label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value as Role)}
                  className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  {Object.entries(roleLabels).map(([role, label]) => (
                    <option key={role} value={role}>
                      {label} - {roleDescriptions[role as Role]}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => setShowInviteModal(false)}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleInviteMember}
                disabled={createInvitationMutation.isPending || !inviteEmail}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {createInvitationMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Send Invitation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Change Role Modal */}
      {showRoleModal && selectedMember && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-border-default">
              <h3 className="text-xl font-semibold text-text-primary">
                Change Member Role
              </h3>
              <p className="text-sm text-text-secondary mt-1">
                Update role for {selectedMember.name}
              </p>
            </div>

            <div className="p-6">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                New Role
              </label>
              <select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as Role)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                {Object.entries(roleLabels).map(([role, label]) => (
                  <option key={role} value={role}>
                    {label} - {roleDescriptions[role as Role]}
                  </option>
                ))}
              </select>
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowRoleModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleUpdateRole}
                disabled={updateRoleMutation.isPending}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {updateRoleMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Update Role
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Transfer Ownership Modal */}
      {showTransferModal && selectedMember && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-border-default">
              <h3 className="text-xl font-semibold text-text-primary">
                Transfer Ownership
              </h3>
            </div>

            <div className="p-6">
              <p className="text-text-secondary">
                Are you sure you want to transfer team ownership to{' '}
                <span className="font-semibold">{selectedMember.name}</span>?
                This action cannot be undone by you. The new owner will have
                full control of the team.
              </p>
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowTransferModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleTransferOwnership}
                disabled={transferOwnershipMutation.isPending}
                className="px-4 py-2 bg-warning-text text-white rounded-lg hover:opacity-90 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {transferOwnershipMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Transfer Ownership
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Remove Member Modal */}
      {showRemoveModal && selectedMember && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6 border-b border-border-default">
              <h3 className="text-xl font-semibold text-text-primary">
                Remove Team Member
              </h3>
            </div>

            <div className="p-6">
              <p className="text-text-secondary">
                Are you sure you want to remove{' '}
                <span className="font-semibold">{selectedMember.name}</span>{' '}
                from your team? They will lose access to all team data.
              </p>
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowRemoveModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleRemoveMember}
                disabled={removeMemberMutation.isPending}
                className="px-4 py-2 bg-error-text text-white rounded-lg hover:opacity-90 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {removeMemberMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Remove Member
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
