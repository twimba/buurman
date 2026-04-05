import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('settings');
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
    TEAM_ADMIN: t('teamMembers.roles.admin'),
    TEAM_EDITOR: t('teamMembers.roles.editor'),
    TEAM_VIEWER: t('teamMembers.roles.viewer'),
  };

  const roleDescriptions: Record<Role, string> = {
    TEAM_ADMIN: t('teamMembers.roles.adminDescription'),
    TEAM_EDITOR: t('teamMembers.roles.editorDescription'),
    TEAM_VIEWER: t('teamMembers.roles.viewerDescription'),
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
                {t('teamMembers.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('teamMembers.memberCount', { count: members?.length ?? 0 })}
              </p>
            </div>
            {canManageMembers && (
              <button
                onClick={() => setShowInviteModal(true)}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2"
              >
                <UserPlus className="h-4 w-4" />
                {t('teamMembers.inviteMember')}
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
                          {t('teamMembers.you')}
                        </span>
                      )}
                      {member.isOwner && (
                        <span className="px-2 py-0.5 bg-warning-bg text-warning-text text-xs font-semibold rounded flex items-center gap-1">
                          <Crown className="h-3 w-3" />
                          {t('teamMembers.owner')}
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-text-secondary">
                      {member.email}
                    </p>
                    <p className="text-xs text-text-secondary mt-1">
                      {t('teamMembers.joined', {
                        date: formatDate(member.joinedAt),
                      })}
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
                        title={t('teamMembers.changeRole')}
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
                          title={t('teamMembers.transferOwnership')}
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
                        title={t('teamMembers.removeMember')}
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
                    {t('teamMembers.pendingInvitations.title')}
                  </h2>
                  <p className="text-sm text-text-secondary mt-1">
                    {t('teamMembers.pendingInvitations.count', {
                      count: pendingInvitations.length,
                    })}
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
                            {t('teamMembers.pendingInvitations.invitedBy', {
                              name: inv.inviterName,
                            })}
                          </span>
                        </div>
                        <p className="text-xs text-text-muted mt-1">
                          {t('teamMembers.pendingInvitations.sent', {
                            date: formatDate(inv.invitedAt),
                          })}{' '}
                          &middot;{' '}
                          {t('teamMembers.pendingInvitations.expires', {
                            date: formatDate(inv.expiresAt),
                          })}
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
                      {t('teamMembers.pendingInvitations.resend')}
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
          <span className="font-medium">{t('teamMembers.roles.label')}</span>
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
                {t('teamMembers.inviteModal.title')}
              </h3>
              <p className="text-sm text-text-secondary mt-1">
                {t('teamMembers.inviteModal.subtitle')}
              </p>
            </div>

            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('teamMembers.inviteModal.emailAddress')}
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
                  {t('teamMembers.inviteModal.role')}
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
                {t('common:buttons.cancel')}
              </button>
              <button
                onClick={handleInviteMember}
                disabled={createInvitationMutation.isPending || !inviteEmail}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {createInvitationMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                {t('teamMembers.inviteModal.sendInvitation')}
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
                {t('teamMembers.roleModal.title')}
              </h3>
              <p className="text-sm text-text-secondary mt-1">
                {t('teamMembers.roleModal.subtitle', {
                  name: selectedMember.name,
                })}
              </p>
            </div>

            <div className="p-6">
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('teamMembers.roleModal.newRole')}
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
                {t('common:buttons.cancel')}
              </button>
              <button
                onClick={handleUpdateRole}
                disabled={updateRoleMutation.isPending}
                className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {updateRoleMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                {t('teamMembers.roleModal.updateRole')}
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
                {t('teamMembers.transferModal.title')}
              </h3>
            </div>

            <div className="p-6">
              <p
                className="text-text-secondary"
                dangerouslySetInnerHTML={{
                  __html: t('teamMembers.transferModal.message', {
                    name: selectedMember.name,
                  }),
                }}
              />
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowTransferModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                {t('common:buttons.cancel')}
              </button>
              <button
                onClick={handleTransferOwnership}
                disabled={transferOwnershipMutation.isPending}
                className="px-4 py-2 bg-warning-text text-white rounded-lg hover:opacity-90 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {transferOwnershipMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                {t('teamMembers.transferModal.confirm')}
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
                {t('teamMembers.removeModal.title')}
              </h3>
            </div>

            <div className="p-6">
              <p
                className="text-text-secondary"
                dangerouslySetInnerHTML={{
                  __html: t('teamMembers.removeModal.message', {
                    name: selectedMember.name,
                  }),
                }}
              />
            </div>

            <div className="p-6 border-t border-border-default flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowRemoveModal(false);
                  setSelectedMemberId(null);
                }}
                className="px-4 py-2 border border-border-strong text-text-secondary rounded-lg hover:bg-surface-inset transition-colors"
              >
                {t('common:buttons.cancel')}
              </button>
              <button
                onClick={handleRemoveMember}
                disabled={removeMemberMutation.isPending}
                className="px-4 py-2 bg-error-text text-white rounded-lg hover:opacity-90 transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {removeMemberMutation.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                {t('teamMembers.removeModal.confirm')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
